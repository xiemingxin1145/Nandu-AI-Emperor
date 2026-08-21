package com.xiemingxin.nandu.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BattleDirectiveSystemTest {

    private fun songFaction() = Faction(
        id = "song", name = "大宋", shortName = "宋", rulerName = "赵构",
        capitalCityId = "yingtianfu", stance = "行在草创", isPlayable = true,
        isAI = false, relations = mapOf("jin" to -80)
    )

    private fun jinFaction() = Faction(
        id = "jin", name = "金国", shortName = "金", rulerName = "完颜宗弼",
        capitalCityId = "kaifeng", stance = "南侵", isAI = true,
        relations = mapOf("song" to -80)
    )

    private fun city(
        id: String,
        name: String,
        owner: String,
        grain: Int = 30000,
        troops: Int = 12000,
        defense: Int = 60
    ) = City(
        id = id, name = name, owner = owner, troops = troops, defense = defense,
        grain = grain, gold = 10000, controlState = if (owner == "song") "FRONTLINE" else "FALLEN"
    )

    private fun officer(id: String, name: String, cityId: String, command: Int = 85) = Officer(
        id = id, name = name, faction = "宋廷", command = command, force = 80,
        strategy = 75, politics = 55, loyalty = 90, currentCityId = cityId,
        status = OfficerStatus.DEPLOYED, rankLevel = 3
    )

    private fun army(
        id: String,
        name: String,
        commanderId: String,
        cityId: String,
        troops: Int = 12000,
        morale: Int = 65
    ) = Army(
        id = id, name = name, ownerFactionId = "song", commanderId = commanderId,
        homeCityId = cityId, currentCityId = cityId, troops = troops, morale = morale,
        armyType = "field_army", supplyCityId = cityId,
        statusCode = ArmyStatus.GARRISONED, status = ArmyStatus.GARRISONED.label,
        supplyLevel = 90
    )

    private fun readyState(withReinforcement: Boolean = true): GameState {
        val liuQi = officer("liu_qi", "刘锜", "shouchun", command = 92)
        val local = army("army_liu", "淮西守军", "liu_qi", "shouchun", morale = 64)
        val supportOfficer = officer("support_general", "王援", "yingtianfu", command = 82)
        val supportArmy = army("army_support", "御营援军", "support_general", "yingtianfu", troops = 10000)
        return GameState(
            turn = 40,
            calendar = GameCalendar("绍兴十年", 14, 5, 1),
            season = Season.SUMMER,
            weather = WeatherType.CLEAR,
            troopMorale = 66,
            courtStability = 55,
            jinThreat = 60,
            factions = listOf(songFaction(), jinFaction()),
            cities = listOf(
                city("shouchun", "寿春", "song", grain = 30000, defense = 62),
                city("yingtianfu", "应天府", "song", grain = 50000, defense = 70),
                city("kaifeng", "开封", "jin", grain = 80000, troops = 50000, defense = 90)
            ),
            officers = if (withReinforcement) listOf(liuQi, supportOfficer) else listOf(liuQi),
            armies = if (withReinforcement) listOf(local, supportArmy) else listOf(local)
        )
    }

    @Test
    fun holdConsumesRealGrainAndChangesRealDefenseAndMorale() {
        val before = readyState()
        val result = BattleDirectiveSystem.applyShunchang(before, ShunchangDirective.HOLD)

        assertTrue(result.message, result.success)
        val beforeCity = before.cities.first { it.id == "shouchun" }
        val afterCity = result.newState.cities.first { it.id == "shouchun" }
        assertEquals(beforeCity.grain - 1200, afterCity.grain)
        assertEquals(beforeCity.defense + 8, afterCity.defense)
        assertEquals(70, result.newState.armies.first { it.id == "army_liu" }.morale)
        assertEquals(before.troopMorale + 2, result.newState.troopMorale)
        assertTrue(result.newState.chronicle.last().summary.contains("固守顺昌"))
        assertEquals(before.cities.sumOf { it.troops } + before.armies.sumOf { it.troops },
            result.newState.cities.sumOf { it.troops } + result.newState.armies.sumOf { it.troops })
    }

    @Test
    fun reinforceCreatesRealMarchRouteAndDoesNotTeleport() {
        val before = readyState()
        val result = BattleDirectiveSystem.applyShunchang(before, ShunchangDirective.REINFORCE)

        assertTrue(result.message, result.success)
        assertEquals("army_support", result.affectedArmyId)
        val routed = result.newState.armies.first { it.id == "army_support" }
        assertEquals(ArmyStatus.MARCHING, routed.statusCode)
        assertEquals("shouchun", routed.targetCityId)
        assertEquals("yingtianfu", routed.currentCityId)
        assertTrue(routed.routeNodeIds.first() == "yingtianfu")
        assertTrue(routed.routeNodeIds.last() == "shouchun")
        assertTrue(routed.marchDaysTotal > 0)
        assertEquals(49000, result.newState.cities.first { it.id == "yingtianfu" }.grain)
        assertTrue(result.newState.chronicle.last().summary.contains("调军驰援"))
    }

    @Test
    fun deliberateHasPersistentStrategicCostWithoutFakeTroopMovement() {
        val before = readyState()
        val result = BattleDirectiveSystem.applyShunchang(before, ShunchangDirective.DELIBERATE)

        assertTrue(result.success)
        assertEquals(before.troopMorale - 2, result.newState.troopMorale)
        assertEquals(before.courtStability + 1, result.newState.courtStability)
        assertEquals(before.jinThreat + 2, result.newState.jinThreat)
        assertEquals(before.armies, result.newState.armies)
        assertTrue(result.newState.chronicle.last().summary.contains("暂缓再议"))
    }

    @Test
    fun oneFormalDirectivePerTurnPreventsClickFarming() {
        val first = BattleDirectiveSystem.applyShunchang(readyState(), ShunchangDirective.HOLD)
        assertTrue(first.success)
        val second = BattleDirectiveSystem.applyShunchang(first.newState, ShunchangDirective.HOLD)
        assertFalse(second.success)
        assertEquals(first.newState, second.newState)
    }

    @Test
    fun noReinforcementArmyMeansFailureInsteadOfFabricatedHelp() {
        val before = readyState(withReinforcement = false)
        val result = BattleDirectiveSystem.applyShunchang(before, ShunchangDirective.REINFORCE)
        assertFalse(result.success)
        assertEquals(before, result.newState)
        assertTrue(result.message.contains("不会凭空生成援军"))
    }

    @Test
    fun unavailableBattleRejectsDirectiveWithoutWorldMutation() {
        val before = readyState().copy(calendar = GameCalendar("建炎元年", 1, 6, 1))
        val result = BattleDirectiveSystem.applyShunchang(before, ShunchangDirective.HOLD)
        assertFalse(result.success)
        assertEquals(before, result.newState)
    }

    @Test
    fun directiveSurvivesSaveExportAndImport() {
        val applied = BattleDirectiveSystem.applyShunchang(readyState(), ShunchangDirective.REINFORCE)
        assertTrue(applied.success)

        val saveCode = GameSaveCodec.export(applied.newState)
        val loaded = GameSaveCodec.import(saveCode).getOrThrow()

        val loadedArmy = loaded.armies.first { it.id == "army_support" }
        assertEquals(ArmyStatus.MARCHING, loadedArmy.statusCode)
        assertEquals("shouchun", loadedArmy.targetCityId)
        assertEquals(applied.newState.cities.first { it.id == "yingtianfu" }.grain,
            loaded.cities.first { it.id == "yingtianfu" }.grain)
        assertTrue(loaded.chronicle.any { it.summary == "【顺昌军令】调军驰援" })
        assertNotEquals(readyState().chronicle, loaded.chronicle)
    }
}
