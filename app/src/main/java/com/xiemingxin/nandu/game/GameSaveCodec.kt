package com.xiemingxin.nandu.game


import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.Base64

object GameSaveCodec {
    private const val PREFIX = "NANDU_SAVE_V1:"
    // V1.7 天下战略：Faction/City/Officer 补齐扩展字段序列化，存档结构升级到 V3。
    // 旧存档（V1/V2）读取时所有新增字段均走 optXxx 默认值兜底，不会崩溃，只是那些旧存档里
    // 本就没有记录过的扩展数值（如城池人口、势力关系等）会被重置为初始默认值。
    private const val SAVE_VERSION = 7

    fun export(state: GameState): String {
        val root = JSONObject()
            .put("saveVersion", SAVE_VERSION)
            .put("turn", state.turn)
            .put("era", state.era)
            .put("calendar", state.calendar.toJson())
            .put("season", state.season.name)
            .put("weather", state.weather.name)
            .put("gold", state.gold)
            .put("grain", state.grain)
            .put("troopMorale", state.troopMorale)
            .put("courtStability", state.courtStability)
            .put("jinThreat", state.jinThreat)
            .put("warFactionPower", state.warFactionPower)
            .put("peaceFactionPower", state.peaceFactionPower)
            .put("factions", JSONArray(state.factions.map { it.toJson() }))
            .put("armies", JSONArray(state.armies.map { it.toJson() }))
            .put("officers", JSONArray(state.officers.map { it.toJson() }))
            .put("cities", JSONArray(state.cities.map { it.toJson() }))
            .put("chronicle", JSONArray(state.chronicle.map { it.toJson() }))
            .put("prestige", state.prestige)
            .put("cityActionPoints", state.cityActionPoints)
            .put("talentLeads", JSONArray(state.talentLeads.toList()))
            .put("rumors", JSONArray(state.rumors.map { it.toJson() }))
            // Stage 3 任职体系
            .put("cityGovernors", JSONObject().apply { state.cityGovernors.forEach { (k, v) -> put(k, v) } })
            .put("cityGarrisons", JSONObject().apply { state.cityGarrisons.forEach { (k, v) -> put(k, v) } })
        val encoded = Base64.getEncoder().encodeToString(root.toString().toByteArray(StandardCharsets.UTF_8))
        return PREFIX + encoded
    }

    fun import(code: String): Result<GameState> = runCatching {
        val trimmed = code.trim()
        require(trimmed.startsWith(PREFIX)) { "不是南渡无悔存档码" }
        val json = String(Base64.getDecoder().decode(trimmed.removePrefix(PREFIX)), StandardCharsets.UTF_8)
        val root = JSONObject(json)
        val ignoredVersion = root.optInt("saveVersion", 1)
        GameState(
            turn = root.optInt("turn", 1),
            era = root.optString("era", "建炎元年"),
            calendar = root.optJSONObject("calendar")?.toCalendar() ?: GameCalendar(),
            season = enumValueOf(root.optString("season", Season.SPRING.name)),
            weather = enumValueOf(root.optString("weather", WeatherType.RAIN.name)),
            gold = root.optInt("gold", 50000),
            grain = root.optInt("grain", 200000),
            troopMorale = root.optInt("troopMorale", 60),
            courtStability = root.optInt("courtStability", 50),
            jinThreat = root.optInt("jinThreat", 70),
            warFactionPower = root.optInt("warFactionPower", 50),
            peaceFactionPower = root.optInt("peaceFactionPower", 50),
            factions = root.optJSONArray("factions").toListOrDefault(InitialData.factions) { it.toFaction() },
            armies = root.optJSONArray("armies").toListOrDefault(InitialData.armies) { it.toArmy() },
            officers = root.optJSONArray("officers").toListOrDefault(InitialData.officers) { it.toOfficer() },
            cities = root.optJSONArray("cities").toListOrDefault(InitialData.cities) { it.toCity() },
            chronicle = root.optJSONArray("chronicle").toListOrDefault(emptyList()) { it.toChronicle() },
            prestige = root.optInt("prestige", 30),
            cityActionPoints = root.optInt("cityActionPoints", TavernSystem.MAX_ACTION_POINTS),
            talentLeads = root.optJSONArray("talentLeads").toStringList().toSet(),
            rumors = root.optJSONArray("rumors").toListOrDefault(emptyList()) { it.toRumor() },
            // Stage 3 任职体系（旧存档默认空Map，向后兼容）
            cityGovernors = root.optJSONObject("cityGovernors")?.let { obj ->
                obj.keys().asSequence().associateWith { obj.optString(it, "") }.filterValues { it.isNotBlank() }
            } ?: emptyMap(),
            cityGarrisons = root.optJSONObject("cityGarrisons")?.let { obj ->
                obj.keys().asSequence().associateWith { obj.optString(it, "") }.filterValues { it.isNotBlank() }
            } ?: emptyMap()
        )
    }

    private fun GameCalendar.toJson() = JSONObject()
        .put("eraName", eraName)
        .put("year", year)
        .put("month", month)
        .put("tenDay", tenDay)

    private fun JSONObject.toCalendar() = GameCalendar(
        eraName = optString("eraName", "建炎元年"),
        year = optInt("year", 1),
        month = optInt("month", 1),
        tenDay = optInt("tenDay", 1)
    )

    private fun Faction.toJson() = JSONObject()
        .put("id", id).put("name", name).put("shortName", shortName)
        .put("rulerName", rulerName).put("capitalCityId", capitalCityId)
        .put("stance", stance).put("isPlayable", isPlayable)
        .put("colorArgb", colorArgb).put("gold", gold).put("grain", grain)
        .put("prestige", prestige).put("isAI", isAI).put("isDestroyed", isDestroyed)
        .put("relations", JSONObject().apply { relations.forEach { (k, v) -> put(k, v) } })

    private fun JSONObject.toFaction(): Faction {
        val isPlayableValue = optBoolean("isPlayable", false)
        return Faction(
            id = optString("id"),
            name = optString("name"),
            shortName = optString("shortName"),
            rulerName = optString("rulerName"),
            capitalCityId = optString("capitalCityId"),
            stance = optString("stance"),
            isPlayable = isPlayableValue,
            colorArgb = optLong("colorArgb", 0xFF8C8C8CL),
            gold = optInt("gold", 0),
            grain = optInt("grain", 0),
            prestige = optInt("prestige", 0),
            isAI = optBoolean("isAI", !isPlayableValue),
            isDestroyed = optBoolean("isDestroyed", false),
            relations = optJSONObject("relations")?.let { rel ->
                rel.keys().asSequence().associateWith { rel.optInt(it, 0) }
            } ?: emptyMap()
        )
    }

    private fun Officer.toJson(): JSONObject {
        val obj = JSONObject()
            .put("id", id).put("name", name).put("faction", faction)
            .put("command", command).put("force", force).put("strategy", strategy)
            .put("politics", politics).put("loyalty", loyalty)
            .put("currentCityId", currentCityId).put("status", status.name)
            .put("charm", charm).put("ambition", ambition).put("rankLevel", rankLevel)
            .put("merit", merit).put("origin", origin)
            .put("skills", JSONArray(skills)).put("bio", bio)
        // V1.0 活朝堂：赶路状态是可选字段，人不在途中时不写入，读取端按"没有=未在赶路"处理。
        travelDestinationCityId?.let { obj.put("travelDestinationCityId", it) }
        travelArrivalTurn?.let { obj.put("travelArrivalTurn", it) }
        // V1.1 历史 Canon：预定状态迁移，同样只在有值时写入。
        scheduledStatus?.let { obj.put("scheduledStatus", it.name) }
        scheduledCityId?.let { obj.put("scheduledCityId", it) }
        scheduledTurn?.let { obj.put("scheduledTurn", it) }
        return obj
    }

    private fun JSONObject.toOfficer(): Officer {
        val id = optString("id")
        val fallback = InitialData.officers.find { it.id == id }
        return Officer(
            id = id,
            name = optString("name"),
            faction = optString("faction"),
            command = optInt("command", 50),
            force = optInt("force", 50),
            strategy = optInt("strategy", 50),
            politics = optInt("politics", 50),
            loyalty = optInt("loyalty", 50),
            currentCityId = optString("currentCityId"),
            status = enumValueOf(optString("status", OfficerStatus.IN_COURT.name)),
            charm = optInt("charm", fallback?.charm ?: 50),
            ambition = optInt("ambition", fallback?.ambition ?: 50),
            rankLevel = optInt("rankLevel", fallback?.rankLevel ?: 0),
            merit = optInt("merit", fallback?.merit ?: 0),
            origin = optString("origin", fallback?.origin ?: ""),
            skills = optJSONArray("skills")?.toStringList() ?: fallback?.skills ?: emptyList(),
            bio = optString("bio", fallback?.bio ?: ""),
            travelDestinationCityId = if (has("travelDestinationCityId")) optString("travelDestinationCityId") else null,
            travelArrivalTurn = if (has("travelArrivalTurn")) optInt("travelArrivalTurn") else null,
            scheduledStatus = if (has("scheduledStatus")) enumValueOf(optString("scheduledStatus")) else null,
            scheduledCityId = if (has("scheduledCityId")) optString("scheduledCityId") else null,
            scheduledTurn = if (has("scheduledTurn")) optInt("scheduledTurn") else null
        )
    }

    private fun City.toJson() = JSONObject()
        .put("id", id).put("name", name).put("owner", owner)
        .put("troops", troops).put("defense", defense).put("grain", grain)
        .put("gold", gold).put("popularSupport", popularSupport)
        .put("controlState", controlState)
        .put("route", route).put("cityLevel", cityLevel).put("terrain", terrain)
        .put("population", population).put("commerce", commerce).put("agriculture", agriculture)
        .put("isCapital", isCapital).put("isWaterNode", isWaterNode).put("isPass", isPass)
        .put("x", x).put("y", y)
        .put("buildings", JSONObject().apply { buildings.forEach { (k, v) -> put(k, v) } })

    private fun JSONObject.toCity(): City {
        // 城池的"原始默认值"取自 InitialData，兼容旧存档：旧存档没记录过的扩展字段
        // （人口/商业/农业/地理属性等）会回落到该城市的初始设定，而不是笼统的硬编码默认值。
        val id = optString("id")
        val fallback = InitialData.cities.find { it.id == id }
        return City(
            id = id,
            name = optString("name"),
            owner = optString("owner", fallback?.owner ?: "song"),
            troops = optInt("troops", fallback?.troops ?: 0),
            defense = optInt("defense", fallback?.defense ?: 50),
            grain = optInt("grain", fallback?.grain ?: 0),
            gold = optInt("gold", fallback?.gold ?: 0),
            popularSupport = optInt("popularSupport", fallback?.popularSupport ?: 80),
            controlState = optString("controlState", fallback?.controlState ?: "STABLE"),
            route = optString("route", fallback?.route ?: ""),
            cityLevel = optString("cityLevel", fallback?.cityLevel ?: "州"),
            terrain = optString("terrain", fallback?.terrain ?: "plain"),
            population = optInt("population", fallback?.population ?: 100000),
            commerce = optInt("commerce", fallback?.commerce ?: 50),
            agriculture = optInt("agriculture", fallback?.agriculture ?: 50),
            isCapital = optBoolean("isCapital", fallback?.isCapital ?: false),
            isWaterNode = optBoolean("isWaterNode", fallback?.isWaterNode ?: false),
            isPass = optBoolean("isPass", fallback?.isPass ?: false),
            x = optInt("x", fallback?.x ?: 0),
            y = optInt("y", fallback?.y ?: 0),
            buildings = optJSONObject("buildings")?.let { b ->
                b.keys().asSequence().associateWith { b.optInt(it, 0) }
            } ?: fallback?.buildings ?: emptyMap()
        )
    }

    private fun Army.toJson() = JSONObject()
        .put("id", id).put("name", name).put("ownerFactionId", ownerFactionId)
        .put("commanderId", commanderId).put("homeCityId", homeCityId)
        .put("currentCityId", currentCityId).put("troops", troops)
        .put("morale", morale).put("armyType", armyType).put("supplyCityId", supplyCityId)
        .put("status", status).put("targetCityId", targetCityId)
        .put("routeFromCityId", routeFromCityId).put("marchDaysTotal", marchDaysTotal)
        .put("marchDaysRemaining", marchDaysRemaining)
        // Stage 4 新字段
        .put("statusCode", statusCode.name)
        .put("routeNodeIds", org.json.JSONArray(routeNodeIds))
        .put("routeIndex", routeIndex)
        .put("supplyLevel", supplyLevel)
        .put("lastSuppliedTurn", lastSuppliedTurn)
        .put("createdTurn", createdTurn)
        .put("lastBattleTurn", lastBattleTurn)
        .put("primaryUnitId", primaryUnitId)

    private fun JSONObject.toArmy() = Army(
        id = optString("id"),
        name = optString("name"),
        ownerFactionId = optString("ownerFactionId"),
        commanderId = optString("commanderId"),
        homeCityId = optString("homeCityId"),
        currentCityId = optString("currentCityId"),
        troops = optInt("troops", 0),
        morale = optInt("morale", 60),
        armyType = optString("armyType", "field_army"),
        supplyCityId = optString("supplyCityId"),
        status = optString("status", "驻防"),
        targetCityId = optString("targetCityId", ""),
        routeFromCityId = optString("routeFromCityId", ""),
        marchDaysTotal = optInt("marchDaysTotal", 0),
        marchDaysRemaining = optInt("marchDaysRemaining", 0),
        // Stage 4 新字段（旧存档向后兼容）
        statusCode = try {
            ArmyStatus.valueOf(optString("statusCode", "GARRISONED"))
        } catch (e: IllegalArgumentException) {
            // 旧存档按status字符串迁移
            when {
                optString("status","").contains("进军") || optString("status","").contains("行军") -> ArmyStatus.MARCHING
                optString("status","").contains("待战") -> ArmyStatus.ENGAGEMENT_PENDING
                else -> ArmyStatus.GARRISONED
            }
        },
        routeNodeIds = optJSONArray("routeNodeIds")?.let { arr ->
            (0 until arr.length()).map { arr.optString(it) }
        } ?: emptyList(),
        routeIndex = optInt("routeIndex", 0),
        supplyLevel = optInt("supplyLevel", 100),
        lastSuppliedTurn = optInt("lastSuppliedTurn", 0),
        createdTurn = optInt("createdTurn", 0),
        lastBattleTurn = optInt("lastBattleTurn", -1),
        primaryUnitId = optString("primaryUnitId", "")
    )

    private fun ChronicleEntry.toJson() = JSONObject()
        .put("turn", turn).put("era", era).put("edictText", edictText)
        .put("summary", summary).put("outcomes", JSONArray(outcomes))
        .put("season", season.name).put("weather", weather.name)

    private fun JSONObject.toChronicle() = ChronicleEntry(
        turn = optInt("turn", 1),
        era = optString("era"),
        edictText = optString("edictText"),
        summary = optString("summary"),
        outcomes = optJSONArray("outcomes").toStringList(),
        season = enumValueOf(optString("season", Season.SPRING.name)),
        weather = enumValueOf(optString("weather", WeatherType.RAIN.name))
    )

    private fun Rumor.toJson() = JSONObject()
        .put("id", id).put("text", text).put("category", category)
        .put("sourceCityId", sourceCityId).put("turn", turn)
        .put("talentOfficerId", talentOfficerId)

    private fun JSONObject.toRumor() = Rumor(
        id = optString("id"),
        text = optString("text"),
        category = optString("category"),
        sourceCityId = optString("sourceCityId"),
        turn = optInt("turn", 1),
        talentOfficerId = optString("talentOfficerId", "")
    )

    private inline fun <T> JSONArray?.toListOrDefault(default: List<T>, mapper: (JSONObject) -> T): List<T> {
        if (this == null) return default
        return (0 until length()).map { mapper(getJSONObject(it)) }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { optString(it) }
    }
    // ── Stage 8 AgentState 序列化 ─────────────────────────────────────────────

    private fun agentStatesToJson(
        states: Map<String, com.xiemingxin.nandu.agent.CharacterAgentState>
    ): org.json.JSONArray {
        val arr = org.json.JSONArray()
        states.forEach { (id, s) ->
            arr.put(org.json.JSONObject()
                .put("id", id)
                .put("longTermGoal", s.longTermGoal.name)
                .put("currentGoal", s.currentGoal.name)
                .put("goalPersistTurns", s.goalPersistTurns)
                .put("warBias", s.warBias)
                .put("loyaltyToEmperor", s.loyaltyToEmperor)
                .put("ambition", s.ambition)
                .put("riskTolerance", s.riskTolerance)
                .put("fearLevel", s.fearLevel)
                .put("emperorAttitude", s.emperorAttitude.name)
                .put("adviceAdoptedCount", s.adviceAdoptedCount)
                .put("adviceRejectedCount", s.adviceRejectedCount)
                .put("rewardCount", s.rewardCount)
                .put("punishCount", s.punishCount)
                .put("currentPlan", s.currentPlan.name)
                .put("compressedMemorySummary", s.compressedMemorySummary)
                .put("lastActiveTurn", s.lastActiveTurn)
                .put("inactive", s.inactive)
            )
        }
        return arr
    }

    private fun parseAgentStates(
        arr: org.json.JSONArray?
    ): Map<String, com.xiemingxin.nandu.agent.CharacterAgentState> {
        if (arr == null) return emptyMap()
        val result = mutableMapOf<String, com.xiemingxin.nandu.agent.CharacterAgentState>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optString("id").ifBlank { "" }
            if (id.isBlank()) continue
            fun goal(key: String, default: com.xiemingxin.nandu.agent.AgentGoal) =
                runCatching { com.xiemingxin.nandu.agent.AgentGoal.valueOf(obj.optString(key)) }.getOrDefault(default)
            result[id] = com.xiemingxin.nandu.agent.CharacterAgentState(
                officerId = id,
                longTermGoal = goal("longTermGoal", com.xiemingxin.nandu.agent.AgentGoal.UNDEFINED),
                currentGoal = goal("currentGoal", com.xiemingxin.nandu.agent.AgentGoal.UNDEFINED),
                goalPersistTurns = obj.optInt("goalPersistTurns", 0),
                warBias = obj.optInt("warBias", 50),
                loyaltyToEmperor = obj.optInt("loyaltyToEmperor", 70),
                ambition = obj.optInt("ambition", 40),
                riskTolerance = obj.optInt("riskTolerance", 50),
                fearLevel = obj.optInt("fearLevel", 20),
                emperorAttitude = runCatching { com.xiemingxin.nandu.agent.EmperorAttitude.valueOf(obj.optString("emperorAttitude")) }
                    .getOrDefault(com.xiemingxin.nandu.agent.EmperorAttitude.NEUTRAL),
                adviceAdoptedCount = obj.optInt("adviceAdoptedCount", 0),
                adviceRejectedCount = obj.optInt("adviceRejectedCount", 0),
                rewardCount = obj.optInt("rewardCount", 0),
                punishCount = obj.optInt("punishCount", 0),
                currentPlan = runCatching { com.xiemingxin.nandu.agent.AgentPlanType.valueOf(obj.optString("currentPlan")) }
                    .getOrDefault(com.xiemingxin.nandu.agent.AgentPlanType.OBSERVE),
                compressedMemorySummary = obj.optString("compressedMemorySummary", ""),
                lastActiveTurn = obj.optInt("lastActiveTurn", -1),
                inactive = obj.optBoolean("inactive", false)
            )
        }
        return result
    }

}