package com.xiemingxin.nandu.game

import com.xiemingxin.nandu.ai.WorldAction
import com.xiemingxin.nandu.ai.WorldTurnPlan
import kotlin.math.max

/**
 * Stage 7：势力战略脑。
 *
 * 这里不让大模型直接“想一个动作”，而是先由权威世界状态生成少量可执行战略候选。
 * 便宜模型只需要在候选之间做取舍；模型不可用时，本地也能按相同评分直接选择。
 *
 * 设计参考的是成熟回合制策略游戏常见的 Threat -> Candidate -> Score -> Order pipeline，
 * 但实现完全基于《南渡》自己的城池、道路、军团、补给和战争规则。
 */
enum class StrategicIntent(val label: String) {
    DEFEND("固守要地"),
    RESUPPLY("整顿粮道"),
    CONTINUE_OPERATION("延续既定战役"),
    PRESS_ADVANTAGE("乘势进攻"),
    RAID("袭扰薄弱边城"),
    EXPAND("推进战线"),
    CONSOLIDATE("休整集结")
}

data class FactionThreatAssessment(
    val factionId: String,
    val threatenedCityIds: List<String>,
    val lowSupplyArmyIds: List<String>,
    val activeOperationArmyIds: List<String>,
    val totalTroops: Int,
    val frontierEnemyTroops: Int
)

data class FactionStrategyCandidate(
    val id: String,
    val factionId: String,
    val intent: StrategicIntent,
    val score: Int,
    val summary: String,
    val actions: List<WorldAction>
)

object FactionStrategyPlanner {
    private const val MAX_CANDIDATES_PER_FACTION = 3

    fun assess(state: GameState, factionId: String): FactionThreatAssessment {
        val ownCities = state.cities.filter { it.owner == factionId }
        val ownArmies = state.armies.filter {
            it.ownerFactionId == factionId && it.statusCode != ArmyStatus.DISBANDED && it.troops > 0
        }
        val enemyArmies = state.armies.filter {
            it.ownerFactionId != factionId && it.statusCode != ArmyStatus.DISBANDED && it.troops > 0
        }

        val threatened = ownCities.filter { city ->
            val neighbors = MapData.neighborsOf(city.id)
            enemyArmies.any { enemy ->
                enemy.currentCityId == city.id || enemy.currentCityId in neighbors ||
                    (enemy.targetCityId == city.id && enemy.statusCode in setOf(ArmyStatus.MARCHING, ArmyStatus.ENGAGEMENT_PENDING))
            }
        }.map { it.id }

        val frontierEnemyTroops = enemyArmies
            .filter { enemy -> ownCities.any { enemy.currentCityId in MapData.neighborsOf(it.id) } }
            .sumOf { it.troops }

        return FactionThreatAssessment(
            factionId = factionId,
            threatenedCityIds = threatened,
            lowSupplyArmyIds = ownArmies.filter { it.supplyLevel < 40 }.map { it.id },
            activeOperationArmyIds = ownArmies.filter {
                it.statusCode in setOf(ArmyStatus.MARCHING, ArmyStatus.ENGAGEMENT_PENDING) && it.targetCityId.isNotBlank()
            }.map { it.id },
            totalTroops = ownArmies.sumOf { it.troops },
            frontierEnemyTroops = frontierEnemyTroops
        )
    }

    /** 供 LLM 挑选的候选。只给少量高质量方案，省 token，也降低小模型胡来的空间。 */
    fun candidates(state: GameState, factionId: String): List<FactionStrategyCandidate> {
        val playerFactionId = playerFactionId(state)
        if (factionId == playerFactionId) return emptyList()
        val faction = state.factions.firstOrNull { it.id == factionId } ?: return emptyList()
        if (faction.isDestroyed) return emptyList()

        val ownArmies = state.armies.filter {
            it.ownerFactionId == factionId && it.statusCode != ArmyStatus.DISBANDED && it.troops > 0
        }
        if (ownArmies.isEmpty()) return emptyList()

        val threat = assess(state, factionId)
        val result = mutableListOf<FactionStrategyCandidate>()

        buildDefenseCandidate(state, factionId, ownArmies, threat)?.let(result::add)
        buildResupplyCandidate(state, factionId, ownArmies, threat)?.let(result::add)
        buildContinuityCandidate(state, factionId, ownArmies)?.let(result::add)
        buildOpportunityCandidate(state, factionId, ownArmies)?.let(result::add)
        result += buildConsolidateCandidate(state, factionId, ownArmies, threat)

        return result
            .distinctBy { it.id }
            .sortedWith(compareByDescending<FactionStrategyCandidate> { it.score }.thenBy { it.id })
            .take(MAX_CANDIDATES_PER_FACTION)
    }

    fun chooseBest(state: GameState, factionId: String): FactionStrategyCandidate? =
        candidates(state, factionId).maxWithOrNull(compareBy<FactionStrategyCandidate> { it.score }.thenByDescending { it.id })

    /**
     * 无模型 / 超时 / 坏 JSON 时直接使用。每个非玩家势力各自有脑子，而不是把所有敌军混成一团。
     */
    fun heuristicWorldPlan(state: GameState): WorldTurnPlan {
        val player = playerFactionId(state)
        val selected = state.factions
            .asSequence()
            .filter { it.id != player && !it.isDestroyed }
            .mapNotNull { chooseBest(state, it.id) }
            .toList()

        return WorldTurnPlan(
            strategySummary = selected.joinToString("；") { "${it.factionId}:${it.summary}" }
                .ifBlank { "诸势力暂以守成为主。" },
            selectedStrategyIds = selected.map { it.id },
            actions = selected.flatMap { it.actions }.take(4),
            npcInitiatives = emptyList()
        )
    }

    private fun buildDefenseCandidate(
        state: GameState,
        factionId: String,
        ownArmies: List<Army>,
        threat: FactionThreatAssessment
    ): FactionStrategyCandidate? {
        if (threat.threatenedCityIds.isEmpty()) return null
        val targetCity = threat.threatenedCityIds
            .mapNotNull { id -> state.cities.firstOrNull { it.id == id } }
            .maxByOrNull { city ->
                val enemyNear = state.armies.filter { army ->
                    army.ownerFactionId != factionId &&
                        (army.currentCityId == city.id || army.currentCityId in MapData.neighborsOf(city.id))
                }.sumOf { it.troops }
                enemyNear - city.troops + if (city.isCapital) 12000 else 0
            } ?: return null

        val army = ownArmies
            .filter { it.statusCode in setOf(ArmyStatus.GARRISONED, ArmyStatus.STANDBY) }
            .maxByOrNull { it.troops + it.morale * 80 + it.supplyLevel * 60 }
            ?: ownArmies.maxByOrNull { it.troops }
            ?: return null

        val action = if (army.currentCityId == targetCity.id) {
            WorldAction("hold_army", factionId, army.id, targetCity.id, "敌军逼近，固守要地")
        } else {
            WorldAction("move_army", factionId, army.id, targetCity.id, "回援受威胁城池")
        }
        val capitalBonus = if (targetCity.isCapital) 45 else 0
        val score = 165 + capitalBonus + (threat.frontierEnemyTroops / 2500).coerceAtMost(35)
        return FactionStrategyCandidate(
            id = "$factionId:defend:${targetCity.id}:${army.id}",
            factionId = factionId,
            intent = StrategicIntent.DEFEND,
            score = score,
            summary = "敌军迫近${targetCity.name}，优先回援固守",
            actions = listOf(action)
        )
    }

    private fun buildResupplyCandidate(
        state: GameState,
        factionId: String,
        ownArmies: List<Army>,
        threat: FactionThreatAssessment
    ): FactionStrategyCandidate? {
        val low = ownArmies
            .filter {
                it.id in threat.lowSupplyArmyIds &&
                    it.statusCode in setOf(ArmyStatus.GARRISONED, ArmyStatus.STANDBY)
            }
            .sortedBy { it.supplyLevel }
        if (low.isEmpty()) return null

        val worst = low.first()
        val actions = low.take(2).map {
            WorldAction("resupply_army", factionId, it.id, reason = "补给${it.supplyLevel}%，先稳粮道")
        }
        val score = 150 + (40 - worst.supplyLevel) * 3 + if (worst.supplyLevel < 20) 45 else 0
        return FactionStrategyCandidate(
            id = "$factionId:resupply:${worst.id}",
            factionId = factionId,
            intent = StrategicIntent.RESUPPLY,
            score = score,
            summary = "${worst.name}粮道吃紧，停止冒进并补充军粮",
            actions = actions
        )
    }

    private fun buildContinuityCandidate(
        state: GameState,
        factionId: String,
        ownArmies: List<Army>
    ): FactionStrategyCandidate? {
        val active = ownArmies
            .filter { it.statusCode in setOf(ArmyStatus.MARCHING, ArmyStatus.ENGAGEMENT_PENDING) && it.targetCityId.isNotBlank() }
            .maxByOrNull { it.troops + it.supplyLevel * 100 }
            ?: return null

        val target = state.cities.firstOrNull { it.id == active.targetCityId }
        if (target == null || target.owner == factionId) return null

        if (active.supplyLevel < 30) {
            return FactionStrategyCandidate(
                id = "$factionId:continue-cautious:${active.id}",
                factionId = factionId,
                intent = StrategicIntent.CONTINUE_OPERATION,
                score = 125,
                summary = "既定战役尚未结束，但${active.name}粮秣不足，暂缓强攻",
                actions = listOf(WorldAction("hold_army", factionId, active.id, target.id, "战役目标不变，先保存实力"))
            )
        }

        val action: WorldAction
        val intent: StrategicIntent
        val score: Int
        if (active.statusCode == ArmyStatus.ENGAGEMENT_PENDING) {
            val ratio = attackRatio(state, active, target)
            if (ratio >= 0.82) {
                action = WorldAction("attack_city", factionId, active.id, target.id, "既定战役已到决战时机")
                intent = StrategicIntent.PRESS_ADVANTAGE
                score = 160 + ((ratio - 0.82) * 40).toInt().coerceIn(0, 30)
            } else {
                action = WorldAction("hold_army", factionId, active.id, target.id, "目标未变，敌强则不强攻")
                intent = StrategicIntent.CONTINUE_OPERATION
                score = 132
            }
        } else {
            // 行军推进由 WorldAiTurnExecutor 每旬真实走一个道路节点；这里保持原计划，防止 AI 每旬改目标。
            action = WorldAction("hold_army", factionId, active.id, target.id, "沿既定路线继续战役")
            intent = StrategicIntent.CONTINUE_OPERATION
            score = 148 + (active.supplyLevel / 10)
        }

        return FactionStrategyCandidate(
            id = "$factionId:continue:${active.id}:${target.id}",
            factionId = factionId,
            intent = intent,
            score = score,
            summary = "维持${active.name}对${target.name}的既定战役目标",
            actions = listOf(action)
        )
    }

    private fun buildOpportunityCandidate(
        state: GameState,
        factionId: String,
        ownArmies: List<Army>
    ): FactionStrategyCandidate? {
        data class Opportunity(val army: Army, val city: City, val ratio: Double, val score: Int)

        val opportunities = ownArmies
            .filter {
                it.statusCode in setOf(ArmyStatus.GARRISONED, ArmyStatus.STANDBY, ArmyStatus.ENGAGEMENT_PENDING) &&
                    it.supplyLevel >= 45 && it.morale >= 40
            }
            .flatMap { army ->
                val neighborIds = MapData.neighborsOf(army.currentCityId)
                state.cities
                    .filter { city -> city.id in neighborIds && city.owner.isNotBlank() && city.owner != factionId }
                    .map { city ->
                        val ratio = attackRatio(state, army, city)
                        val value = (city.grain / 1500) + (if (city.controlState == "CONTESTED") 35 else 0) +
                            (if (city.controlState == "FRONTLINE") 15 else 0)
                        Opportunity(army, city, ratio, (ratio * 100).toInt() + value)
                    }
            }

        val best = opportunities.maxWithOrNull(compareBy<Opportunity> { it.score }.thenByDescending { it.city.id })
            ?: return null
        if (best.ratio < 0.68) return null

        val intent = when {
            best.ratio >= 1.05 -> StrategicIntent.EXPAND
            best.city.controlState == "CONTESTED" -> StrategicIntent.PRESS_ADVANTAGE
            else -> StrategicIntent.RAID
        }
        val actionType = if (
            best.army.statusCode == ArmyStatus.ENGAGEMENT_PENDING && best.army.targetCityId == best.city.id
        ) "attack_city" else "move_army"

        val score = when (intent) {
            StrategicIntent.EXPAND -> 145
            StrategicIntent.PRESS_ADVANTAGE -> 152
            else -> 124
        } + ((best.ratio - 0.68) * 35).toInt().coerceIn(0, 35)

        return FactionStrategyCandidate(
            id = "$factionId:${intent.name.lowercase()}:${best.army.id}:${best.city.id}",
            factionId = factionId,
            intent = intent,
            score = score,
            summary = "${best.city.name}防线相对薄弱，以${best.army.name}施压",
            actions = listOf(WorldAction(actionType, factionId, best.army.id, best.city.id, "把握局部兵力优势"))
        )
    }

    private fun buildConsolidateCandidate(
        state: GameState,
        factionId: String,
        ownArmies: List<Army>,
        threat: FactionThreatAssessment
    ): FactionStrategyCandidate {
        val weakest = ownArmies.minByOrNull { it.morale + it.supplyLevel } ?: ownArmies.first()
        val avgSupply = ownArmies.sumOf { it.supplyLevel } / max(1, ownArmies.size)
        val avgMorale = ownArmies.sumOf { it.morale } / max(1, ownArmies.size)
        val score = 80 + (50 - avgSupply).coerceAtLeast(0) * 2 + (45 - avgMorale).coerceAtLeast(0) * 2 +
            if (threat.threatenedCityIds.isEmpty()) 8 else 0
        return FactionStrategyCandidate(
            id = "$factionId:consolidate",
            factionId = factionId,
            intent = StrategicIntent.CONSOLIDATE,
            score = score,
            summary = "暂无必胜战机，整顿军伍并保存机动力量",
            actions = listOf(WorldAction("hold_army", factionId, weakest.id, reason = "避免无意义消耗"))
        )
    }

    private fun attackRatio(state: GameState, army: Army, target: City): Double {
        val attackPower = army.troops.toDouble() *
            (0.55 + army.morale / 100.0 * 0.45) *
            (0.50 + army.supplyLevel / 100.0 * 0.50)
        val fieldDefenders = state.armies
            .filter {
                it.ownerFactionId == target.owner && it.currentCityId == target.id && it.statusCode != ArmyStatus.DISBANDED
            }
            .sumOf { it.troops }
        val defendPower = target.troops * (1.0 + target.defense / 100.0) + fieldDefenders
        return attackPower / max(1.0, defendPower)
    }

    private fun playerFactionId(state: GameState): String =
        state.factions.firstOrNull { it.isPlayable }?.id ?: "song"
}
