package com.xiemingxin.nandu.ai

import com.xiemingxin.nandu.game.ArmyStatus
import com.xiemingxin.nandu.game.CharacterStateSource
import com.xiemingxin.nandu.game.FactionStrategyPlanner
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.MapData
import kotlinx.serialization.Serializable

/**
 * AI World Engine 协议。
 *
 * Stage 7 在 Stage 6 的“AI 只能提出动作”基础上再收紧一层：
 * 本地规则先生成少量经过地图、补给、兵力校验的战略候选，便宜模型主要负责挑选候选与生成角色奏言。
 */
@Serializable
data class WorldTurnPlan(
    val strategySummary: String = "",
    val selectedStrategyIds: List<String> = emptyList(),
    val actions: List<WorldAction> = emptyList(),
    val npcInitiatives: List<NpcInitiative> = emptyList()
)

@Serializable
data class WorldAction(
    val type: String,
    val factionId: String = "",
    val armyId: String = "",
    val targetCityId: String = "",
    val reason: String = ""
) {
    companion object {
        val ALLOWED_TYPES = setOf(
            "move_army",
            "attack_city",
            "resupply_army",
            "hold_army"
        )

        fun isValid(type: String): Boolean = type in ALLOWED_TYPES
    }
}

@Serializable
data class NpcInitiative(
    val officerId: String,
    val kind: String = "memorial", // memorial / warning / request / advice
    val text: String
)

/** 只有支持世界推演的 provider 才实现此接口；官方不支持时自动退回本地战略脑。 */
interface WorldPlanningProvider {
    suspend fun planWorldTurn(context: WorldTurnContext): Result<WorldTurnPlan>
}

data class WorldTurnContext(
    val turn: Int,
    val era: String,
    val playerFactionId: String,
    val factions: List<WorldFactionContext>,
    val cities: List<WorldCityContext>,
    val armies: List<WorldArmyContext>,
    val officers: List<WorldOfficerContext>,
    val strategyCandidates: List<WorldStrategyCandidateContext> = emptyList()
)

data class WorldStrategyCandidateContext(
    val id: String,
    val factionId: String,
    val intent: String,
    val score: Int,
    val summary: String,
    val actions: List<WorldAction>
)

data class WorldFactionContext(
    val id: String,
    val name: String,
    val relationToPlayer: Int,
    val cityCount: Int,
    val armyCount: Int,
    val isDestroyed: Boolean
)

data class WorldCityContext(
    val id: String,
    val name: String,
    val owner: String,
    val troops: Int,
    val defense: Int,
    val grain: Int,
    val terrain: String,
    val controlState: String,
    val neighbors: List<String>
)

data class WorldArmyContext(
    val id: String,
    val name: String,
    val owner: String,
    val commanderId: String,
    val commanderName: String,
    val currentCityId: String,
    val troops: Int,
    val morale: Int,
    val supply: Int,
    val status: String,
    val targetCityId: String
)

data class WorldOfficerContext(
    val id: String,
    val name: String,
    val courtFaction: String,
    val currentCityId: String,
    val status: String,
    val command: Int,
    val strategy: Int,
    val politics: Int,
    val loyalty: Int,
    val skills: List<String>
)

object WorldContextFactory {
    fun fromState(state: GameState): WorldTurnContext {
        val playerFactionId = state.factions.firstOrNull { it.isPlayable }?.id ?: "song"
        val playerFaction = state.factions.firstOrNull { it.id == playerFactionId }

        val factionContexts = state.factions.map { faction ->
            WorldFactionContext(
                id = faction.id,
                name = faction.name,
                relationToPlayer = if (faction.id == playerFactionId) 100
                else faction.relationWith(playerFactionId).takeIf { it != 0 }
                    ?: playerFaction?.relationWith(faction.id)
                    ?: 0,
                cityCount = state.cities.count { it.owner == faction.id },
                armyCount = state.armies.count {
                    it.ownerFactionId == faction.id && it.statusCode != ArmyStatus.DISBANDED
                },
                isDestroyed = faction.isDestroyed
            )
        }

        val cityContexts = state.cities.map { city ->
            WorldCityContext(
                id = city.id,
                name = city.name,
                owner = city.owner,
                troops = city.troops,
                defense = city.defense,
                grain = city.grain,
                terrain = city.terrain,
                controlState = city.controlState,
                neighbors = MapData.neighborsOf(city.id).toList()
            )
        }

        val armyContexts = state.armies
            .filter { it.statusCode != ArmyStatus.DISBANDED }
            .map { army ->
                val commander = state.officers.firstOrNull { it.id == army.commanderId }
                WorldArmyContext(
                    id = army.id,
                    name = army.name,
                    owner = army.ownerFactionId,
                    commanderId = army.commanderId,
                    commanderName = commander?.name ?: "",
                    currentCityId = army.currentCityId,
                    troops = army.troops,
                    morale = army.morale,
                    supply = army.supplyLevel,
                    status = army.statusCode.name,
                    targetCityId = army.targetCityId
                )
            }

        // 世界模型只能看到“已经进入当前世界视野”的人物。
        // 可见性统一交给 CharacterStateSource，避免不同AI入口各写一套状态判断。
        val officerContexts = state.officers
            .filter { CharacterStateSource.visibleToWorldAi(state, it) }
            .map { officer ->
                WorldOfficerContext(
                    id = officer.id,
                    name = officer.name,
                    courtFaction = officer.faction,
                    currentCityId = officer.currentCityId,
                    status = officer.status.name,
                    command = officer.command,
                    strategy = officer.strategy,
                    politics = officer.politics,
                    loyalty = officer.loyalty,
                    skills = officer.skills.take(5)
                )
            }

        val strategyCandidates = state.factions
            .asSequence()
            .filter { it.id != playerFactionId && !it.isDestroyed }
            .flatMap { FactionStrategyPlanner.candidates(state, it.id).asSequence() }
            .map { candidate ->
                WorldStrategyCandidateContext(
                    id = candidate.id,
                    factionId = candidate.factionId,
                    intent = candidate.intent.name,
                    score = candidate.score,
                    summary = candidate.summary,
                    actions = candidate.actions
                )
            }
            .toList()

        // 兼容 Stage 6 的 OpenAI-compatible 提示模板：候选战略压缩进 era 简报，
        // 不额外增加一次模型调用。模型仍输出原 WorldAction JSON，但会优先照着候选动作选择。
        val candidateBrief = strategyCandidates.joinToString("；") { c ->
            val actions = c.actions.joinToString("+") { a ->
                "${a.type}(${a.armyId}${if (a.targetCityId.isNotBlank()) "->${a.targetCityId}" else ""})"
            }
            "${c.id}[${c.intent},分${c.score}]:${c.summary}=>$actions"
        }
        val eraWithStrategy = buildString {
            append("${state.calendar.displayText()} / ${state.season.label} / ${state.weather.label}")
            if (candidateBrief.isNotBlank()) {
                append("\nStage7本地军议候选（优先从中择策，不要另造不合理行动）：")
                append(candidateBrief)
            }
        }

        return WorldTurnContext(
            turn = state.turn,
            era = eraWithStrategy,
            playerFactionId = playerFactionId,
            factions = factionContexts,
            cities = cityContexts,
            armies = armyContexts,
            officers = officerContexts,
            strategyCandidates = strategyCandidates
        )
    }
}
