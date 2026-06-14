package com.xiemingxin.nandu.game

import com.xiemingxin.nandu.ai.EdictCommand
import com.xiemingxin.nandu.ai.EdictResult
import kotlin.math.ceil
import kotlin.math.sqrt

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
    val status: OfficerStatus = OfficerStatus.IN_COURT,
    // V0.8 人物面板扩展（全带默认值，兼容旧数据）
    val charm: Int = 50,                  // 魅力（招募/稳定军心）
    val ambition: Int = 50,               // 野心（隐藏，影响叛变）
    val rankLevel: Int = 0,               // 官阶 0小卒→5方面大将
    val merit: Int = 0,                   // 军功值
    val origin: String = "",              // 出身：寒门/军户/士族/豪强/义军/归正人
    val skills: List<String> = emptyList(), // 技能标签
    val bio: String = ""                  // 简介
)

enum class OfficerStatus { HIDDEN, SOLDIER, WANDERING, IN_COURT, DEPLOYED, DISMISSED, DECEASED }

data class City(
    val id: String,
    val name: String,
    val owner: String = "song",
    val troops: Int,
    val defense: Int,
    val grain: Int,
    val gold: Int,
    val popularSupport: Int = 80,
    val controlState: String = "STABLE",
    // V0.8 地理与经营扩展（全部带默认值，兼容旧存档）
    val route: String = "",            // 所属路，如 两浙西路
    val cityLevel: String = "州",       // 府/州/军/监/关
    val terrain: String = "plain",     // plain平原 / river水network / mountain山地 / pass关隘 / coast沿海
    val population: Int = 100000,      // 人口
    val commerce: Int = 50,            // 商业度
    val agriculture: Int = 50,         // 农业度
    val isCapital: Boolean = false,    // 是否都城
    val isWaterNode: Boolean = false,  // 是否水运节点
    val isPass: Boolean = false,       // 是否关隘
    val x: Int = 0,                    // 地图坐标X
    val y: Int = 0                     // 地图坐标Y
)

data class Faction(
    val id: String,
    val name: String,
    val shortName: String,
    val rulerName: String,
    val capitalCityId: String,
    val stance: String,
    val isPlayable: Boolean = false
)

data class Army(
    val id: String,
    val name: String,
    val ownerFactionId: String,
    val commanderId: String,
    val homeCityId: String,
    val currentCityId: String,
    val troops: Int,
    val morale: Int,
    val armyType: String,
    val supplyCityId: String,
    val status: String = "驻防",
    val targetCityId: String = "",
    val routeFromCityId: String = "",
    val marchDaysTotal: Int = 0,
    val marchDaysRemaining: Int = 0
)

enum class Season(val label: String, val effectText: String) {
    SPRING("春", "整军、募兵、修城略有加成"),
    SUMMER("夏", "水军占优，暴雨和疫病风险上升"),
    AUTUMN("秋", "秋高马肥，北伐与金军南侵都更频繁"),
    WINTER("冬", "行军变慢，粮草消耗上升，北方有冰渡风险")
}

enum class WeatherType(val label: String, val effectText: String) {
    CLEAR("晴", "行军与弓弩发挥稳定"),
    RAIN("雨", "火攻下降，水军略强，行军稍缓"),
    STORM("暴雨", "道路泥泞，移动和攻城受阻"),
    FOG("雾", "伏击与突围机会增加，侦察下降"),
    SNOW("雪", "山地行军困难，士气与补给受压"),
    WIND("大风", "火攻增强，水战风险上升")
}

data class GameCalendar(
    val eraName: String = "建炎元年",
    val year: Int = 1,
    val month: Int = 1,
    val tenDay: Int = 1
) {
    fun monthName(): String = listOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月"
    ).getOrElse(month - 1) { "正月" }

    fun tenDayName(): String = when (tenDay) {
        1 -> "上旬"
        2 -> "中旬"
        else -> "下旬"
    }

    fun season(): Season = when (month) {
        1, 2, 3 -> Season.SPRING
        4, 5, 6 -> Season.SUMMER
        7, 8, 9 -> Season.AUTUMN
        else -> Season.WINTER
    }

    fun displayText(): String = "$eraName ${monthName()}${tenDayName()}"

    fun advance(): GameCalendar {
        return if (tenDay < 3) {
            copy(tenDay = tenDay + 1)
        } else if (month < 12) {
            copy(month = month + 1, tenDay = 1)
        } else {
            val nextYear = year + 1
            GameCalendar("建炎${chineseYear(nextYear)}年", nextYear, 1, 1)
        }
    }

    private fun chineseYear(value: Int): String = when (value) {
        1 -> "元"
        2 -> "二"
        3 -> "三"
        4 -> "四"
        5 -> "五"
        6 -> "六"
        7 -> "七"
        8 -> "八"
        9 -> "九"
        10 -> "十"
        else -> value.toString()
    }
}

object WeatherSystem {
    fun generate(calendar: GameCalendar, turn: Int): WeatherType {
        val candidates = when (calendar.season()) {
            Season.SPRING -> listOf(WeatherType.RAIN, WeatherType.CLEAR, WeatherType.FOG, WeatherType.CLEAR, WeatherType.RAIN, WeatherType.STORM)
            Season.SUMMER -> listOf(WeatherType.CLEAR, WeatherType.RAIN, WeatherType.STORM, WeatherType.STORM, WeatherType.WIND, WeatherType.CLEAR)
            Season.AUTUMN -> listOf(WeatherType.CLEAR, WeatherType.CLEAR, WeatherType.WIND, WeatherType.FOG, WeatherType.RAIN, WeatherType.CLEAR)
            Season.WINTER -> listOf(WeatherType.SNOW, WeatherType.CLEAR, WeatherType.WIND, WeatherType.FOG, WeatherType.SNOW, WeatherType.CLEAR)
        }
        val seed = calendar.year * 37 + calendar.month * 11 + calendar.tenDay * 7 + turn * 13
        return candidates[(seed and Int.MAX_VALUE) % candidates.size]
    }
}

data class ChronicleEntry(
    val turn: Int,
    val era: String,
    val edictText: String,
    val summary: String,
    val outcomes: List<String>,
    val season: Season = Season.SPRING,
    val weather: WeatherType = WeatherType.RAIN
)

data class GameState(
    val turn: Int = 1,
    val era: String = "建炎元年",
    val calendar: GameCalendar = GameCalendar(),
    val season: Season = Season.SPRING,
    val weather: WeatherType = WeatherType.RAIN,
    val gold: Int = 50000,
    val grain: Int = 200000,
    val troopMorale: Int = 60,
    val courtStability: Int = 50,
    val jinThreat: Int = 70,
    val warFactionPower: Int = 50,
    val peaceFactionPower: Int = 50,
    val factions: List<Faction> = InitialData.factions,
    val armies: List<Army> = InitialData.armies,
    val officers: List<Officer> = InitialData.officers,
    val cities: List<City> = InitialData.cities,
    val chronicle: List<ChronicleEntry> = emptyList(),
    val firedEventIds: Set<String> = emptySet(),
    val storyFlags: Set<String> = emptySet()
)

object GameRuleEngine {

    data class ExecutionResult(
        val newState: GameState,
        val outcomes: List<String>,
        val rejectedCommands: List<String>
    )

    fun executeEdict(state: GameState, edictResult: EdictResult, edictText: String): ExecutionResult {
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
                        rejected.add("【调兵失败】兵力不足、城池不存在、将领未被朝廷登记、正在行军或无统兵资格。")
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
                "move_capital" -> rejected.add("【迁都暂缓】迁都系统尚未开放。")
            }
        }

        val nextCalendar = currentState.calendar.advance()
        val nextSeason = nextCalendar.season()
        val nextWeather = WeatherSystem.generate(nextCalendar, currentState.turn + 1)
        val seasonalThreat = if (nextSeason == Season.AUTUMN) 4 else 2
        val weatherMoralePenalty = when (nextWeather) {
            WeatherType.STORM, WeatherType.SNOW -> 2
            else -> 0
        }
        val grainCost = if (nextSeason == Season.WINTER) 7000 else 5000

        currentState = currentState.copy(
            turn = currentState.turn + 1,
            era = nextCalendar.eraName,
            calendar = nextCalendar,
            season = nextSeason,
            weather = nextWeather,
            jinThreat = (currentState.jinThreat + seasonalThreat).coerceAtMost(100),
            gold = (currentState.gold - 3000).coerceAtLeast(0),
            grain = (currentState.grain - grainCost).coerceAtLeast(0),
            troopMorale = (currentState.troopMorale - weatherMoralePenalty).coerceIn(0, 100)
        )

        val march = advanceMarchingArmies(currentState, days = 10)
        currentState = march.first

        outcomes.add("【天时】时序推进至${nextCalendar.displayText()}，${nextSeason.label}，天气${nextWeather.label}；${nextWeather.effectText}。")
        outcomes.addAll(march.second)

        val entry = ChronicleEntry(
            turn = state.turn,
            era = state.calendar.displayText(),
            edictText = edictText,
            summary = edictResult.summary,
            outcomes = outcomes + rejected,
            season = state.season,
            weather = state.weather
        )
        currentState = currentState.copy(chronicle = currentState.chronicle + entry)
        return ExecutionResult(currentState, outcomes, rejected)
    }

    private fun executeDispatchArmy(state: GameState, cmd: EdictCommand): Pair<GameState, String?> {
        val officer = state.officers.find { it.id == cmd.officerId } ?: return state to null
        if (officer.status != OfficerStatus.IN_COURT && officer.status != OfficerStatus.DEPLOYED) return state to null
        val fromCity = state.cities.find { it.id == cmd.fromCityId } ?: return state to null
        val toCity = state.cities.find { it.id == cmd.toCityId } ?: return state to null
        val existingArmy = state.armies.find { it.commanderId == officer.id }
        if (existingArmy?.status?.contains("进军") == true) return state to null

        val cityAvailable = (fromCity.troops - 5000).coerceAtLeast(0)
        val officerLimit = officer.commandLimit()
        val actualTroops = cmd.troops.coerceAtMost(cityAvailable).coerceAtMost(officerLimit)
        if (actualTroops <= 0) return state to null

        val armyType = existingArmy?.armyType ?: "field_army"
        val marchDays = estimateMarchDays(cmd.fromCityId, cmd.toCityId, armyType, state.season, state.weather)
        val weatherDelay = when (state.weather) {
            WeatherType.STORM -> "暴雨泥泞，行军迟缓，"
            WeatherType.SNOW -> "风雪阻道，粮队艰难，"
            WeatherType.FOG -> "雾气弥漫，斥候难明，"
            WeatherType.RAIN -> "道路湿滑，"
            else -> ""
        }
        val capNote = if (cmd.troops > actualTroops) {
            "原拟${cmd.troops}兵，因${officer.name}当前可统${officerLimit}兵、${fromCity.name}需留守，实发${actualTroops}兵。"
        } else ""

        val newCities = state.cities.map { city ->
            if (city.id == cmd.fromCityId) city.copy(troops = city.troops - actualTroops) else city
        }
        val newOfficers = state.officers.map {
            if (it.id == cmd.officerId) it.copy(currentCityId = cmd.fromCityId, status = OfficerStatus.DEPLOYED) else it
        }
        val newArmies = if (existingArmy != null) {
            state.armies.map { army ->
                if (army.id == existingArmy.id) army.copy(
                    currentCityId = cmd.fromCityId,
                    targetCityId = cmd.toCityId,
                    routeFromCityId = cmd.fromCityId,
                    troops = actualTroops,
                    morale = (army.morale + 3).coerceAtMost(100),
                    supplyCityId = cmd.fromCityId,
                    status = "进军·余${marchDays}天",
                    marchDaysTotal = marchDays,
                    marchDaysRemaining = marchDays
                ) else army
            }
        } else {
            state.armies + Army(
                id = "army_${officer.id}_${state.turn}",
                name = "${officer.name}部",
                ownerFactionId = fromCity.owner,
                commanderId = officer.id,
                homeCityId = cmd.fromCityId,
                currentCityId = cmd.fromCityId,
                troops = actualTroops,
                morale = (state.troopMorale + officer.loyalty / 10).coerceIn(30, 100),
                armyType = armyType,
                supplyCityId = cmd.fromCityId,
                status = "进军·余${marchDays}天",
                targetCityId = cmd.toCityId,
                routeFromCityId = cmd.fromCityId,
                marchDaysTotal = marchDays,
                marchDaysRemaining = marchDays
            )
        }

        val newState = state.copy(
            cities = newCities,
            officers = newOfficers,
            armies = newArmies,
            troopMorale = (state.troopMorale + 5).coerceAtMost(100),
            jinThreat = (state.jinThreat + 5).coerceAtMost(100)
        )
        return newState to "【调兵】${weatherDelay}${officer.name}率${actualTroops}兵马由${fromCity.name}启程赴${toCity.name}，预计${marchDays}日抵达，军心+5。$capNote"
    }

    private fun estimateMarchDays(fromCityId: String, toCityId: String, armyType: String, season: Season, weather: WeatherType): Int {
        val from = MapData.nodeMap[fromCityId] ?: return 30
        val to = MapData.nodeMap[toCityId] ?: return 30
        val dx = (from.worldX - to.worldX).toDouble()
        val dy = (from.worldY - to.worldY).toDouble()
        val mapDistance = sqrt(dx * dx + dy * dy)
        val routeKm = mapDistance / 6.5
        val baseSpeed = when {
            armyType.contains("naval") -> 45.0
            armyType.contains("cavalry") -> 36.0
            armyType.contains("mountain") -> 21.0
            else -> 26.0
        }
        val seasonFactor = when (season) {
            Season.SPRING -> 1.0
            Season.SUMMER -> 1.05
            Season.AUTUMN -> 0.95
            Season.WINTER -> 1.25
        }
        val weatherFactor = when (weather) {
            WeatherType.CLEAR -> 1.0
            WeatherType.RAIN -> 1.15
            WeatherType.STORM -> 1.55
            WeatherType.FOG -> 1.10
            WeatherType.SNOW -> 1.40
            WeatherType.WIND -> 1.05
        }
        val days = ceil(routeKm / baseSpeed * seasonFactor * weatherFactor).toInt()
        return days.coerceIn(3, 120)
    }

    private fun advanceMarchingArmies(state: GameState, days: Int): Pair<GameState, List<String>> {
        var cities = state.cities
        var officers = state.officers
        val outcomes = mutableListOf<String>()
        val newArmies = state.armies.map { army ->
            if (!army.status.contains("进军") || army.targetCityId.isBlank()) {
                army
            } else {
                val remaining = (army.marchDaysRemaining - days).coerceAtLeast(0)
                if (remaining == 0) {
                    val targetCity = state.cities.find { it.id == army.targetCityId }
                    val commander = state.officers.find { it.id == army.commanderId }
                    cities = cities.map {
                        if (it.id == army.targetCityId) it.copy(troops = it.troops + army.troops) else it
                    }
                    officers = officers.map {
                        if (it.id == army.commanderId) it.copy(currentCityId = army.targetCityId, status = OfficerStatus.DEPLOYED) else it
                    }
                    outcomes.add("【军情】${commander?.name ?: army.name}所部抵达${targetCity?.name ?: army.targetCityId}，${army.troops}兵入城驻防。")
                    army.copy(
                        currentCityId = army.targetCityId,
                        supplyCityId = army.targetCityId,
                        status = "驻防",
                        targetCityId = "",
                        routeFromCityId = "",
                        marchDaysTotal = 0,
                        marchDaysRemaining = 0
                    )
                } else {
                    army.copy(status = "进军·余${remaining}天", marchDaysRemaining = remaining)
                }
            }
        }
        return state.copy(cities = cities, officers = officers, armies = newArmies) to outcomes
    }

    private fun executeAssignOfficer(state: GameState, cmd: EdictCommand): Pair<GameState, String> {
        if (cmd.role == "search" || cmd.officerId.isBlank()) return executeTalentSearch(state, cmd)
        val officer = state.officers.find { it.id == cmd.officerId } ?: return state to "【任命失败】未找到此人。"
        val city = state.cities.find { it.id == cmd.cityId } ?: return state to "【任命失败】未找到城池。"
        if (officer.status == OfficerStatus.HIDDEN || officer.status == OfficerStatus.SOLDIER || officer.status == OfficerStatus.WANDERING) {
            return executeTalentSearch(state, cmd)
        }
        val newOfficers = state.officers.map {
            if (it.id == cmd.officerId) it.copy(currentCityId = cmd.cityId, status = OfficerStatus.DEPLOYED) else it
        }
        val newArmies = if (state.armies.any { it.commanderId == officer.id }) {
            state.armies.map {
                if (it.commanderId == officer.id) it.copy(currentCityId = cmd.cityId, supplyCityId = cmd.cityId, status = "驻防", targetCityId = "", routeFromCityId = "", marchDaysTotal = 0, marchDaysRemaining = 0) else it
            }
        } else state.armies
        return state.copy(officers = newOfficers, armies = newArmies) to "【任命】${officer.name}奉命赴${city.name}，职掌${cmd.role.ifEmpty { "守备" }}。"
    }

    private fun executeTalentSearch(state: GameState, cmd: EdictCommand): Pair<GameState, String> {
        val cost = (cmd.amount.takeIf { it > 0 } ?: 3000).coerceIn(1000, 20000)
        if (state.gold < cost) return state to "【寻访失败】国库不足，无法派出访才使。"
        val hiddenStatuses = setOf(OfficerStatus.HIDDEN, OfficerStatus.SOLDIER, OfficerStatus.WANDERING)
        val targetPool = state.officers.filter { officer ->
            officer.status in hiddenStatuses &&
                (cmd.officerId.isBlank() || officer.id == cmd.officerId) &&
                (cmd.cityId.isBlank() || officer.currentCityId == cmd.cityId)
        }.ifEmpty {
            state.officers.filter { officer -> officer.status in hiddenStatuses && (cmd.officerId.isBlank() || officer.id == cmd.officerId) }
        }
        if (targetPool.isEmpty()) {
            return state.copy(gold = state.gold - cost) to "【寻访】使者遍访军营乡里，未得可用之才，耗资${cost}贯。"
        }
        val seed = state.turn * 37 + cmd.cityId.hashCode() * 3 + cmd.officerId.hashCode() * 7 + state.calendar.month * 11
        val roll = (seed and Int.MAX_VALUE) % 100
        val threshold = if (cmd.officerId.isNotBlank()) 82 else 48
        val chosen = targetPool[(seed and Int.MAX_VALUE) % targetPool.size]
        if (roll > threshold) {
            val hintCity = state.cities.find { it.id == chosen.currentCityId }?.name ?: chosen.currentCityId
            return state.copy(gold = state.gold - cost) to "【寻访】访才使得线索：$hintCity 一带似有可造之才，但此番未能请至御前，耗资${cost}贯。"
        }
        val newOfficers = state.officers.map {
            if (it.id == chosen.id) it.copy(status = OfficerStatus.IN_COURT, faction = when (it.id) {
                "yue_fei", "liu_qi" -> "新锐武将"
                "han_shizhong", "wu_jie" -> "武将派"
                "qin_hui" -> "文臣派"
                else -> it.faction
            }) else it
        }
        val cityName = state.cities.find { it.id == chosen.currentCityId }?.name ?: chosen.currentCityId
        val origin = when (chosen.status) {
            OfficerStatus.SOLDIER -> "军中低阶之人"
            OfficerStatus.WANDERING -> "流落未仕之士"
            else -> "民间未登记之才"
        }
        return state.copy(gold = state.gold - cost, officers = newOfficers) to "【寻访得才】访才使在${cityName}寻得${origin}：${chosen.name}。此人已录入御前名册，可听候任用，耗资${cost}贯。"
    }

    private fun executeRepairCity(state: GameState, cmd: EdictCommand): Pair<GameState, String> {
        val city = state.cities.find { it.id == cmd.cityId } ?: return state to "【修城失败】未找到城池。"
        val cost = 8000
        if (state.gold < cost) return state to "【修城失败】国库不足，需${cost}贯，现有${state.gold}贯。"
        val seasonBonus = if (state.season == Season.SPRING) 5 else 0
        val newCities = state.cities.map {
            if (it.id == cmd.cityId) it.copy(defense = (it.defense + 15 + seasonBonus).coerceAtMost(100)) else it
        }
        return state.copy(cities = newCities, gold = state.gold - cost) to "【修城】${city.name}城防加固，城防+${15 + seasonBonus}，耗资${cost}贯。"
    }

    private fun executeRaiseGrain(state: GameState, cmd: EdictCommand): Pair<GameState, String> {
        val officer = state.officers.find { it.id == cmd.officerId }
        val amount = cmd.amount.coerceIn(10000, 500000)
        val skillBonus = officer?.profile()?.skills?.let { SkillEffects.domainModifier(it, "grain") } ?: 0f
        val seasonBonus = if (state.season == Season.AUTUMN) amount / 10 else 0
        val skillGain = (amount * skillBonus).toInt() / 2
        val stabilityPenalty = if (amount > 100000) -8 else -3
        return state.copy(grain = state.grain + amount / 2 + seasonBonus + skillGain, courtStability = (state.courtStability + stabilityPenalty).coerceIn(0, 100)) to "【筹粮】${officer?.name ?: "户部"}奉旨筹粮，首旬得粮${amount / 2 + seasonBonus + skillGain}石，朝堂稳定${stabilityPenalty}。"
    }

    private fun executeSuppressOfficer(state: GameState, cmd: EdictCommand): Pair<GameState, String> {
        val officer = state.officers.find { it.id == cmd.officerId } ?: return state to "【处置失败】未找到此人。"
        val isPeaceFaction = officer.faction == "主和派"
        val stabilityChange = if (cmd.severity == "severe") -15 else -8
        val factionShift = if (isPeaceFaction && cmd.severity != "light") 10 else 0
        val intrigueBonus = SkillEffects.domainModifier(officer.profile().skills, "court_intrigue")
        val adjustedFactionShift = (factionShift * (1f + intrigueBonus)).toInt()
        val newOfficers = state.officers.map {
            if (it.id == cmd.officerId && cmd.severity == "severe") it.copy(status = OfficerStatus.DISMISSED) else it
        }
        return state.copy(officers = newOfficers, courtStability = (state.courtStability + stabilityChange).coerceIn(0, 100), warFactionPower = (state.warFactionPower + adjustedFactionShift).coerceIn(0, 100), peaceFactionPower = (state.peaceFactionPower - adjustedFactionShift).coerceIn(0, 100)) to "【处置】${officer.name}遭御前压制，朝堂稳定${stabilityChange}，主战派声势+${adjustedFactionShift}。"
    }

    private fun executeRewardOfficer(state: GameState, cmd: EdictCommand): Pair<GameState, String> {
        val officer = state.officers.find { it.id == cmd.officerId } ?: return state to "【赏赐失败】未找到武将。"
        val cost = 5000
        if (state.gold < cost) return state to "【赏赐失败】国库不足。"
        return state.copy(gold = state.gold - cost, troopMorale = (state.troopMorale + 3).coerceAtMost(100)) to "【赏赐】${officer.name}受赏，军心+3，耗资${cost}贯。"
    }

    private fun executePunishOfficer(state: GameState, cmd: EdictCommand): Pair<GameState, String> {
        val officer = state.officers.find { it.id == cmd.officerId } ?: return state to "【惩处失败】未找到此人。"
        val newOfficers = state.officers.map { if (it.id == cmd.officerId) it.copy(status = OfficerStatus.DISMISSED) else it }
        return state.copy(officers = newOfficers, courtStability = (state.courtStability - 10).coerceIn(0, 100)) to "【惩处】${officer.name}被罢黜，朝堂稳定-10。"
    }
}
