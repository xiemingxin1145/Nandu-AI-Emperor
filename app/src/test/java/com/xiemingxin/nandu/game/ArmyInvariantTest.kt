package com.xiemingxin.nandu.game

import org.junit.Test
import org.junit.Assert.*

/** Stage 4 兵力Invariant测试 */
class ArmyInvariantTest {

    private fun mockCity(id: String, troops: Int, owner: String = "song") =
        City(id, id, owner, troops = troops, defense = 50, grain = 50000, gold = 10000,
             controlState = "STABLE", popularSupport = 80)

    private fun mockOfficer(id: String, name: String = id, city: String = "linan") =
        Officer(id, name, "宋廷", command = 80, force = 70, strategy = 65,
                politics = 60, loyalty = 90, currentCityId = city,
                status = OfficerStatus.IN_COURT, charm = 65, ambition = 40,
                rankLevel = 3, merit = 0, origin = "将门",
                skills = emptyList(), bio = "")

    private fun baseState(cities: List<City>, officers: List<Officer>, armies: List<Army> = emptyList()) =
        GameState(
            cities = cities, officers = officers, armies = armies,
            factions = emptyList(), turn = 1, troopMorale = 70, courtStability = 60,
            jinThreat = 40, gold = 50000, grain = 100000, prestige = 50,
            season = Season.SPRING, weather = WeatherType.CLEAR,
            calendar = GameCalendar(eraName = "建炎元年", year = 1127, month = 1, tenDay = 1),
            cityGovernors = emptyMap(), cityGarrisons = emptyMap()
        )

    @Test
    fun `form army deducts from city not duplicated`() {
        val city = mockCity("linan", 20000)
        val officer = mockOfficer("yue_fei", city = "linan")
        val state = baseState(listOf(city), listOf(officer))

        val result = ArmySystem.formArmy(state, "linan", "yue_fei", 10000, "field_army")
        assertTrue("组建应成功", result is ArmySystem.ArmyResult.Success)
        val newState = (result as ArmySystem.ArmyResult.Success).newState

        val newCity = newState.cities.find { it.id == "linan" }!!
        val newArmy = newState.armies.find { it.commanderId == "yue_fei" }!!

        assertEquals("城池兵力应减少10000", 10000, newCity.troops)
        assertEquals("军团兵力应为10000", 10000, newArmy.troops)

        // 总兵力不变
        val inv = ArmySystem.checkTroopInvariant(state, newState)
        assertNull("兵力Invariant必须通过: $inv", inv)
    }

    @Test
    fun `army arrives at friendly city does not add troops back`() {
        val city = mockCity("linan", 5000)
        val target = mockCity("jiankang", 8000)
        val officer = mockOfficer("han_shizhong", city = "linan")
        val army = Army(
            id = "a1", name = "韩世忠部", ownerFactionId = "song",
            commanderId = "han_shizhong", homeCityId = "linan",
            currentCityId = "linan", troops = 12000, morale = 80,
            armyType = "field_army", supplyCityId = "linan",
            statusCode = ArmyStatus.MARCHING, status = "行军",
            targetCityId = "jiankang",
            routeNodeIds = listOf("linan", "jiankang"),
            routeIndex = 0, marchDaysRemaining = 5, supplyLevel = 90
        )
        val state = baseState(listOf(city, target), listOf(officer), listOf(army))

        val (newState, _) = ArmyMovementSystem.tickAllArmies(state, 10)
        val newTarget = newState.cities.find { it.id == "jiankang" }!!
        val newArmy = newState.armies.find { it.id == "a1" }!!

        assertEquals("抵达后城池兵力不得增加", 8000, newTarget.troops)
        assertEquals("军团兵力保持不变", 12000, newArmy.troops)
        assertEquals("军团状态变为驻扎", ArmyStatus.GARRISONED, newArmy.statusCode)
    }

    @Test
    fun `disband army returns troops to city`() {
        val city = mockCity("linan", 5000)
        val officer = mockOfficer("zhao_ding", city = "linan")
        val army = Army(
            id = "a2", name = "赵鼎部", ownerFactionId = "song",
            commanderId = "zhao_ding", homeCityId = "linan",
            currentCityId = "linan", troops = 8000, morale = 75,
            armyType = "field_army", supplyCityId = "linan",
            statusCode = ArmyStatus.GARRISONED, status = "驻扎",
            supplyLevel = 100
        )
        val state = baseState(listOf(city), listOf(officer), listOf(army))

        val result = ArmySystem.disbandArmy(state, "a2")
        assertTrue("解散应成功", result is ArmySystem.ArmyResult.Success)
        val newState = (result as ArmySystem.ArmyResult.Success).newState

        assertEquals("城池兵力应+8000", 13000, newState.cities.find { it.id == "linan" }!!.troops)
        assertTrue("军团应已移除", newState.armies.none { it.id == "a2" })

        val inv = ArmySystem.checkTroopInvariant(state, newState)
        assertNull("兵力Invariant必须通过: $inv", inv)
    }

    @Test
    fun `same commander cannot lead two armies`() {
        val city = mockCity("linan", 30000)
        val officer = mockOfficer("yue_fei", city = "linan")
        val existingArmy = Army(
            id = "existing", name = "岳飞部", ownerFactionId = "song",
            commanderId = "yue_fei", homeCityId = "linan",
            currentCityId = "linan", troops = 10000, morale = 90,
            armyType = "field_army", supplyCityId = "linan",
            statusCode = ArmyStatus.GARRISONED, supplyLevel = 100
        )
        val state = baseState(listOf(city), listOf(officer), listOf(existingArmy))

        val result = ArmySystem.formArmy(state, "linan", "yue_fei", 5000, "field_army")
        assertTrue("重复带兵应失败", result is ArmySystem.ArmyResult.Failure)
    }

    @Test
    fun `supply ticks only change supply level not troops`() {
        val city = mockCity("linan", 5000)
        val army = Army(
            id = "a3", name = "测试军", ownerFactionId = "song",
            commanderId = "x", homeCityId = "linan",
            currentCityId = "linan", troops = 10000, morale = 80,
            armyType = "field_army", supplyCityId = "linan",
            statusCode = ArmyStatus.GARRISONED, supplyLevel = 80
        )
        val officer = mockOfficer("x", city = "linan")
        val state = baseState(listOf(city), listOf(officer), listOf(army))

        val (newState, _) = ArmySupplySystem.tickAllSupply(state)
        val newArmy = newState.armies.find { it.id == "a3" }!!

        assertEquals("troops不得因补给变化", 10000, newArmy.troops)
        assertTrue("supplyLevel应变化", newArmy.supplyLevel != 80 || newState.cities.find { it.id == "linan" }!!.grain < city.grain)
    }
}
