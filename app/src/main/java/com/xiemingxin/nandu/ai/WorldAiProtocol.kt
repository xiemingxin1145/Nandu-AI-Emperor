package com.xiemingxin.nandu.ai

import com.xiemingxin.nandu.game.ArmyStatus
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.MapData
import com.xiemingxin.nandu.game.OfficerStatus
import kotlinx.serialization.Serializable

/**
 * Stage 6 AI World Engine 协议。
 *
 * 设计目标：
 *  1. 一个便宜模型每旬只调用一次，统一决定非玩家势力行动 + 重要人物主动上奏；
 *  2. AI 只能“提出动作”，不能直接修改兵力/城池/钱粮；
 *  3. 所有动作交给本地规则引擎二次校验并执行，避免模型幻觉改世界状态；
 *  4. 协议尽量小，适合 mini / flash / deepseek-chat / qwen 等低成本模型。
 */
@Serializable
data class WorldTurnPlan(
    val strategySummary: String = "",
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
    val officers: List<WorldOfficerContext>
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

        // 为省 token，不把隐藏人物和死人交给世界模型。
        val officerContexts = state.officers
            .filter { it.status != OfficerStatus.HIDDEN && it.status != OfficerStatus.DECEASED }
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

        return WorldTurnContext(
            turn = state.turn,
            era = "${state.calendar.displayText()} / ${state.season.label} / ${state.weather.label}",
            playerFactionId = playerFactionId,
            factions = factionContexts,
            cities = cityContexts,
            armies = armyContexts,
            officers = officerContexts
        )
    }
}
