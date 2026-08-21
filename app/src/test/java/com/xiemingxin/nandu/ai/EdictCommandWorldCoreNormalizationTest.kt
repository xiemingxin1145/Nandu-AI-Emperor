package com.xiemingxin.nandu.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class EdictCommandWorldCoreNormalizationTest {

    @Test
    fun legacyMilitaryAssignmentBecomesGarrisonAppointment() {
        val command = EdictCommand(
            type = "assign_officer",
            officerId = "zong_ze",
            cityId = "kaifeng",
            role = "东京留守"
        )
        assertEquals("appoint_garrison", command.type)
    }

    @Test
    fun legacyCivilAssignmentBecomesGovernorAppointment() {
        val command = EdictCommand(
            type = "assign_officer",
            officerId = "zhao_ding",
            cityId = "kaifeng",
            role = "开封府主官"
        )
        assertEquals("appoint_governor", command.type)
    }

    @Test
    fun ambiguousAssignmentStaysUnchangedForClarification() {
        val command = EdictCommand(
            type = "assign_officer",
            officerId = "li_gang",
            cityId = "kaifeng",
            role = "差遣"
        )
        assertEquals("assign_officer", command.type)
    }

    @Test
    fun assignmentWithoutCityNeverGuessesDestination() {
        val command = EdictCommand(
            type = "assign_officer",
            officerId = "zong_ze",
            role = "东京留守"
        )
        assertEquals("assign_officer", command.type)
    }
}
