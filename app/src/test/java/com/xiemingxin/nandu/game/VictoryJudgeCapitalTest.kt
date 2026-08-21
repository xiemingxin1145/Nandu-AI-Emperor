package com.xiemingxin.nandu.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VictoryJudgeCapitalTest {
    @Test
    fun losingHangzhouDoesNotEndGameWhileYingtianRemainsTheCapital() {
        val state = GameState().let { opening ->
            opening.copy(cities = opening.cities.map { city ->
                if (city.id == "linan") city.copy(owner = "jin") else city
            })
        }

        assertEquals(GameEnding.ONGOING, VictoryJudge.judgeDefeat(state))
    }

    @Test
    fun losingRealOpeningCapitalEndsGameEvenWhenHangzhouIsSafe() {
        val state = GameState().let { opening ->
            opening.copy(cities = opening.cities.map { city ->
                if (city.id == "yingtianfu") city.copy(owner = "jin") else city
            })
        }

        assertEquals(GameEnding.CAPITAL_LOST, VictoryJudge.judgeDefeat(state))
    }

    @Test
    fun relocatedCapitalIsResolvedFromCurrentFactionAndCityState() {
        val opening = GameState()
        val relocated = opening.copy(
            factions = opening.factions.map { faction ->
                if (faction.id == "song") faction.copy(capitalCityId = "linan") else faction
            },
            cities = opening.cities.map { city ->
                when (city.id) {
                    "yingtianfu" -> city.copy(owner = "jin", isCapital = false)
                    "linan" -> city.copy(isCapital = true)
                    else -> city
                }
            }
        )

        assertEquals(GameEnding.ONGOING, VictoryJudge.judgeDefeat(relocated))

        val fallen = relocated.copy(cities = relocated.cities.map { city ->
            if (city.id == "linan") city.copy(owner = "jin") else city
        })
        assertEquals(GameEnding.CAPITAL_LOST, VictoryJudge.judgeDefeat(fallen))
    }

    @Test
    fun currentCapitalMarkerWinsIfFactionRecordHasNotCaughtUp() {
        val opening = GameState()
        val relocated = opening.copy(cities = opening.cities.map { city ->
            when (city.id) {
                "yingtianfu" -> city.copy(owner = "jin", isCapital = false)
                "linan" -> city.copy(isCapital = true)
                else -> city
            }
        })

        assertEquals(GameEnding.ONGOING, VictoryJudge.judgeDefeat(relocated))
    }

    @Test
    fun capitalLostEndingDoesNotNameAHardcodedFutureCapital() {
        assertFalse(GameEnding.CAPITAL_LOST.desc.contains("临安"))
    }
}
