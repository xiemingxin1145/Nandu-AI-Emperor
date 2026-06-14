package com.xiemingxin.nandu.game

import com.xiemingxin.nandu.ai.EdictCommand
import com.xiemingxin.nandu.ai.EdictResult

// ══════════════════════════════════════════════
//  数据模型
// ══════════════════════════════════════════════

data class Officer(
    val id: String,
    val name: String,
    val faction: String,
    val command: Int,
    val force: Int,
    val strategy: Int,
    val politics: Int,
    val loyalty: Int,
    val currentCityId: String,
    val status: OfficerStatus = OfficerStatus.IN_COURT
)

enum class OfficerStatus { IN_COURT, DEPLOYED, DISMISSED, DECEASED }

data class City(
    val id: String,
    val name: String,
    val owner: String = "song",
    val troops: Int,
    val defense: Int,
    val grain: Int,
    val gold: Int,
    val popularSupport: Int = 80,
    val controlState: String = "STABLE"
)

data class ChronicleEntry(
    val turn: Int,
    val era: String,
    val edictText: String,
    val summary: String,
    val outcomes: List<String>
)

// ══════════════════════════════════════════════
//  游戏状态
// ══════════════════════════════════════════════

data class GameState(
    val turn: Int = 1,
    val era: String = "建炎元年",
    val gold: Int = 50000,
    val grain: Int = 200000,
    val troopMorale: Int = 60,
    val courtStability: Int = 50,
    val jinThreat: Int = 70,
    val warFactionPower: Int = 50,
    val peaceFactionPower: Int = 50,
    val officers: List<Officer> = InitialData.officers,
    val cities: List<City> = InitialData.cities,
    val chronicle: List<ChronicleEntry> = emptyList()
)

// ══════════════════════════════════════════════
//  GameRuleEngine — 本地规则裁决
//  AI不能直接改国运，必须过这里
// ══════════════════════════════════════════════

object GameRuleEngine {

    data class ExecutionResult(
        val newState: GameState,
        val outcomes: List<String>,
        val rejectedCommands: List<String>
    )

    fun executeEdict(
        state: GameState,
        edictResult: EdictResult,
        edictText: String
    ): ExecutionResult {
        var currentState = state
        val outcomes = mutableListOf<String>()
        val rejected = mutableListOf<String>()

        for (command in edictResult.commands) {
            if (!EdictCommand.isValid(command.type)) {
                rejected.add("【命令拒绝】未知命令：${command.type}")
                continue
            }

            when (command.type) {
                "dispatch_army" -> {
                    val result = executeDispatchArmy(currentState, command)
                    if (result.second != null) {
                        currentState = result.first
                        outcomes.add(result.second!!)
                    } else {
                        rejected.add("【调兵失败】兵力不足、城池不存在或将领不可用。")
                    }
                }
                "assign_officer" -> {
                    val result = executeAssignOfficer(currentState, command)
                    currentState = result.first
                    outcomes.add(result.second)
                }
                "repair_city" -> {
                    val result = executeRepairCity(currentState, command)
                    currentState = result.first
                    outcomes.add(result.second)
                }
                "raise_grain" -> {
                    val result = executeRaiseGrain(currentState, command)
                    currentState = result.first
                    outcomes.add(result.second)
                }
                "suppress_officer" -> {
                    val result = executeSuppressOfficer(currentState, command)
                    currentState = result.first
                    outcomes.add(result.second)
                }
                "reward_officer" -> {
                    val result = executeRewardOfficer(currentState, command)
                    currentState = result.first
                    outcomes.add(result.second)
                }
                "punish_officer" -> {
                    val result = executePunishOfficer(currentState, command)
                    currentState = result.first
                    outcomes.add(result.second)
                }
                "move_capital" -> {
                    rejected.add("【迁都暂缓】迁都系统尚未开放。")
                }
            }
        }

        currentState = currentState.copy(
            turn = currentState.turn + 1,
            jinThreat = (currentState.jinThreat + 2).coerceAtMost(100),
            gold = (currentState.gold - 3000).coerceAtLeast(0)
        )

        val entry = ChronicleEntry(
            turn = state.turn,
            era = state.era,
            edictText = edictText,
            summary = edictResult.summary,
            outcomes = outcomes + rejected
        )
        currentState = currentState.copy(
            chronicle = currentState.chronicle + entry
        )

        return ExecutionResult(currentState, outcomes, rejected)
    }

    private fun executeDispatchArmy(
        state: GameState,
        cmd: EdictCommand
    ): Pair<GameState, String?> {
        val officer = state.officers.find { it.id == cmd.officerId } ?: return state to null
        val fromCity = state.cities.find { it.id == cmd.fromCityId } ?: return state to null
        val toCity = state.cities.find { it.id == cmd.toCityId } ?: return state to null

        val actualTroops = cmd.troops.coerceAtMost(fromCity.troops - 5000)
        if (actualTroops <= 0) return state to null

        val newCities = state.cities.map { city ->
            when (city.id) {
                cmd.fromCityId -> city.copy(troops = city.troops - actualTroops)
                cmd.toCityId -> city.copy(troops = city.troops + actualTroops)
                else -> city
            }
        }

        val newOfficers = state.officers.map {
            if (it.id == cmd.officerId) it.copy(currentCityId = cmd.toCityId, status = OfficerStatus.DEPLOYED)
            else it
        }

        val newState = state.copy(
            cities = newCities,
            officers = newOfficers,
            troopMorale = (state.troopMorale + 5).coerceAtMost(100),
            jinThreat = (state.jinThreat + 5).coerceAtMost(100)
        )

        return newState to "【调兵】${officer.name}率${actualTroops}兵马由${fromCity.name}驰往${toCity.name}，军心+5，金国警觉上升。"
    }

    private fun executeAssignOfficer(
        state: GameState,
        cmd: EdictCommand
    ): Pair<GameState, String> {
        val officer = state.officers.find { it.id == cmd.officerId }
            ?: return state to "【任命失败】未找到武将。"
        val city = state.cities.find { it.id == cmd.cityId }
            ?: return state to "【任命失败】未找到城池。"

        val newOfficers = state.officers.map {
            if (it.id == cmd.officerId) it.copy(currentCityId = cmd.cityId, status = OfficerStatus.DEPLOYED)
            else it
        }

        return state.copy(officers = newOfficers) to
            "【任命】${officer.name}奉命赴${city.name}，职掌${cmd.role.ifEmpty { "守备" }}。"
    }

    private fun executeRepairCity(
        state: GameState,
        cmd: EdictCommand
    ): Pair<GameState, String> {
        val city = state.cities.find { it.id == cmd.cityId }
            ?: return state to "【修城失败】未找到城池。"

        val cost = 8000
        if (state.gold < cost) {
            return state to "【修城失败】国库不足，需${cost}贯，现有${state.gold}贯。"
        }

        val newCities = state.cities.map {
            if (it.id == cmd.cityId) it.copy(defense = (it.defense + 15).coerceAtMost(100))
            else it
        }

        return state.copy(cities = newCities, gold = state.gold - cost) to
            "【修城】${city.name}城防加固，城防+15，耗资${cost}贯。"
    }

    private fun executeRaiseGrain(
        state: GameState,
        cmd: EdictCommand
    ): Pair<GameState, String> {
        val officer = state.officers.find { it.id == cmd.officerId }
        val amount = cmd.amount.coerceIn(10000, 500000)
        val stabilityPenalty = if (amount > 100000) -8 else -3

        return state.copy(
            grain = state.grain + amount / 2,
            courtStability = (state.courtStability + stabilityPenalty).coerceIn(0, 100)
        ) to "【筹粮】${officer?.name ?: "户部"}奉旨筹粮，首旬得粮${amount / 2}石，朝堂稳定${stabilityPenalty}。"
    }

    private fun executeSuppressOfficer(
        state: GameState,
        cmd: EdictCommand
    ): Pair<GameState, String> {
        val officer = state.officers.find { it.id == cmd.officerId }
            ?: return state to "【处置失败】未找到此人。"

        val isPeaceFaction = officer.faction == "主和派"
        val stabilityChange = if (cmd.severity == "severe") -15 else -8
        val factionShift = if (isPeaceFaction && cmd.severity != "light") 10 else 0

        val newOfficers = state.officers.map {
            if (it.id == cmd.officerId && cmd.severity == "severe") it.copy(status = OfficerStatus.DISMISSED)
            else it
        }

        return state.copy(
            officers = newOfficers,
            courtStability = (state.courtStability + stabilityChange).coerceIn(0, 100),
            warFactionPower = (state.warFactionPower + factionShift).coerceIn(0, 100),
            peaceFactionPower = (state.peaceFactionPower - factionShift).coerceIn(0, 100)
        ) to "【处置】${officer.name}遭御前压制，朝堂稳定${stabilityChange}，主战派声势+${factionShift}。"
    }

    private fun executeRewardOfficer(
        state: GameState,
        cmd: EdictCommand
    ): Pair<GameState, String> {
        val officer = state.officers.find { it.id == cmd.officerId }
            ?: return state to "【赏赐失败】未找到武将。"
        val cost = 5000
        if (state.gold < cost) return state to "【赏赐失败】国库不足。"

        return state.copy(
            gold = state.gold - cost,
            troopMorale = (state.troopMorale + 3).coerceAtMost(100)
        ) to "【赏赐】${officer.name}受赏，军心+3，耗资${cost}贯。"
    }

    private fun executePunishOfficer(
        state: GameState,
        cmd: EdictCommand
    ): Pair<GameState, String> {
        val officer = state.officers.find { it.id == cmd.officerId }
            ?: return state to "【惩处失败】未找到此人。"

        val newOfficers = state.officers.map {
            if (it.id == cmd.officerId) it.copy(status = OfficerStatus.DISMISSED)
            else it
        }

        return state.copy(
            officers = newOfficers,
            courtStability = (state.courtStability - 10).coerceIn(0, 100)
        ) to "【惩处】${officer.name}被罢黜，朝堂稳定-10。"
    }
}
