package com.xiemingxin.nandu.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterDetailEntryPolicyTest {

    @Test
    fun stateScreenOnlyListsCourtAndDeployedOfficers() {
        val state = GameState()
        val listed = CharacterDetailEntryPolicy.registeredForStateScreen(state)

        assertTrue(listed.isNotEmpty())
        assertTrue(listed.all { it.status == OfficerStatus.IN_COURT || it.status == OfficerStatus.DEPLOYED })
        assertTrue(listed.none { it.status == OfficerStatus.HIDDEN })
        assertTrue(listed.none { it.status == OfficerStatus.NOT_YET_RELEVANT })
        assertTrue(listed.none { it.status == OfficerStatus.CAPTIVE })
        assertTrue(listed.none { it.id == "yue_fei" })
        assertTrue(listed.any { it.id == "li_gang" })
        assertTrue(listed.any { it.id == "zong_ze" })
    }

    @Test
    fun hiddenOfficerWithoutLeadOnlyShowsHint() {
        val hidden = officer("shadow_guest", "影客", OfficerStatus.HIDDEN, "linan")
        val state = GameState().copy(officers = GameState().officers + hidden, talentLeads = emptySet())
        val snapshot = CharacterDetailEntryPolicy.snapshot(hidden, state)

        assertTrue(CharacterDetailEntryPolicy.usesHiddenHint(hidden, state))
        assertTrue(snapshot.isHiddenHint)
        assertEquals("？？？", snapshot.displayName)
        assertNull(snapshot.force)
        assertNull(snapshot.command)
        assertTrue(snapshot.skills.isEmpty())
        assertEquals("", snapshot.bio)
        assertTrue(snapshot.cityName.isNotBlank())
    }

    @Test
    fun talentLeadRevealsPendingOfficerInsteadOfHint() {
        val hidden = officer("shadow_guest", "影客", OfficerStatus.HIDDEN, "linan")
        val state = GameState().copy(
            officers = GameState().officers + hidden,
            talentLeads = setOf("shadow_guest")
        )
        val snapshot = CharacterDetailEntryPolicy.snapshot(hidden, state)

        assertFalse(CharacterDetailEntryPolicy.usesHiddenHint(hidden, state))
        assertFalse(snapshot.isHiddenHint)
        assertEquals("影客", snapshot.displayName)
        assertEquals(70, snapshot.force)
        assertEquals(80, snapshot.command)
    }

    @Test
    fun revealedOfficerSnapshotCarriesRequiredFields() {
        val state = GameState()
        val officer = state.officers.first { it.id == "li_gang" }
        val snapshot = CharacterDetailEntryPolicy.snapshot(officer, state)

        assertFalse(snapshot.isHiddenHint)
        assertEquals("李纲", snapshot.displayName)
        assertTrue(snapshot.faction.isNotBlank())
        assertTrue(snapshot.identity.isNotBlank())
        assertTrue(snapshot.currentRole.isNotBlank())
        assertTrue(snapshot.cityName.isNotBlank())
        assertNotEquals("xiangyang", snapshot.cityName)
        assertTrue(snapshot.statusHint.isNotBlank())
        assertTrue(snapshot.loyaltyLabel.isNotBlank())
        assertTrue(snapshot.ambitionLabel.isNotBlank())
        assertNotNull(snapshot.force)
        assertNotNull(snapshot.command)
        assertNotNull(snapshot.strategy)
        assertNotNull(snapshot.politics)
        assertTrue(snapshot.skills.isNotEmpty())
    }

    @Test
    fun differentOfficersProduceDifferentSnapshots() {
        val state = GameState()
        val liGang = CharacterDetailEntryPolicy.snapshot(state.officers.first { it.id == "li_gang" }, state)
        val zongZe = CharacterDetailEntryPolicy.snapshot(state.officers.first { it.id == "zong_ze" }, state)

        assertNotEquals(liGang.displayName, zongZe.displayName)
        assertNotEquals(liGang.officerId, zongZe.officerId)
        assertTrue(liGang.command != null && zongZe.command != null)
    }

    @Test
    fun missingCityFallsBackToCityIdWithoutCrashing() {
        val officer = officer("ghost_clerk", "无名吏", OfficerStatus.IN_COURT, "no_such_city")
        val snapshot = CharacterDetailEntryPolicy.snapshot(officer, GameState())

        assertFalse(snapshot.isHiddenHint)
        assertEquals("no_such_city", snapshot.cityName)
        assertEquals("无名吏", snapshot.displayName)
        assertEquals("宋廷", snapshot.faction)
    }

    @Test
    fun blankNameAndFactionDoNotCrash() {
        val officer = Officer(
            id = "blank_person",
            name = "",
            faction = "",
            command = 40,
            force = 40,
            strategy = 40,
            politics = 40,
            loyalty = 40,
            currentCityId = "yingtianfu",
            status = OfficerStatus.IN_COURT
        )
        val snapshot = CharacterDetailEntryPolicy.snapshot(officer, GameState())
        assertEquals("无名", snapshot.displayName)
        assertEquals("未载", snapshot.faction)
        assertTrue(snapshot.identity.isNotBlank())
    }

    private fun officer(
        id: String,
        name: String,
        status: OfficerStatus,
        cityId: String
    ) = Officer(
        id = id,
        name = name,
        faction = "宋廷",
        command = 80,
        force = 70,
        strategy = 60,
        politics = 50,
        loyalty = 70,
        currentCityId = cityId,
        status = status,
        charm = 50,
        ambition = 40,
        rankLevel = 2,
        origin = "寒门",
        skills = listOf("政务"),
        bio = "测试人物"
    )
}
