package com.xiemingxin.nandu.game

import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.Base64

object GameSaveCodec {
    private const val PREFIX = "NANDU_SAVE_V1:"
    private const val SAVE_VERSION = 2

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
            rumors = root.optJSONArray("rumors").toListOrDefault(emptyList()) { it.toRumor() }
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

    private fun JSONObject.toFaction() = Faction(
        id = optString("id"),
        name = optString("name"),
        shortName = optString("shortName"),
        rulerName = optString("rulerName"),
        capitalCityId = optString("capitalCityId"),
        stance = optString("stance"),
        isPlayable = optBoolean("isPlayable", false)
    )

    private fun Officer.toJson() = JSONObject()
        .put("id", id).put("name", name).put("faction", faction)
        .put("command", command).put("force", force).put("strategy", strategy)
        .put("politics", politics).put("loyalty", loyalty)
        .put("currentCityId", currentCityId).put("status", status.name)

    private fun JSONObject.toOfficer() = Officer(
        id = optString("id"),
        name = optString("name"),
        faction = optString("faction"),
        command = optInt("command", 50),
        force = optInt("force", 50),
        strategy = optInt("strategy", 50),
        politics = optInt("politics", 50),
        loyalty = optInt("loyalty", 50),
        currentCityId = optString("currentCityId"),
        status = enumValueOf(optString("status", OfficerStatus.IN_COURT.name))
    )

    private fun City.toJson() = JSONObject()
        .put("id", id).put("name", name).put("owner", owner)
        .put("troops", troops).put("defense", defense).put("grain", grain)
        .put("gold", gold).put("popularSupport", popularSupport)
        .put("controlState", controlState)

    private fun JSONObject.toCity() = City(
        id = optString("id"),
        name = optString("name"),
        owner = optString("owner", "song"),
        troops = optInt("troops", 0),
        defense = optInt("defense", 50),
        grain = optInt("grain", 0),
        gold = optInt("gold", 0),
        popularSupport = optInt("popularSupport", 80),
        controlState = optString("controlState", "STABLE")
    )

    private fun Army.toJson() = JSONObject()
        .put("id", id).put("name", name).put("ownerFactionId", ownerFactionId)
        .put("commanderId", commanderId).put("homeCityId", homeCityId)
        .put("currentCityId", currentCityId).put("troops", troops)
        .put("morale", morale).put("armyType", armyType).put("supplyCityId", supplyCityId)
        .put("status", status).put("targetCityId", targetCityId)
        .put("routeFromCityId", routeFromCityId).put("marchDaysTotal", marchDaysTotal)
        .put("marchDaysRemaining", marchDaysRemaining)

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
        marchDaysRemaining = optInt("marchDaysRemaining", 0)
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
}
