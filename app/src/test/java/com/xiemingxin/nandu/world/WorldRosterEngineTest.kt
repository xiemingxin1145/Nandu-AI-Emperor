package com.xiemingxin.nandu.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldRosterEngineTest {
    @Test
    fun `generated person is deterministic for same seed`() {
        val spec = WorldRosterEngine.GeneratedPersonSpec(
            factionId = "southern_song",
            regionId = "linan",
            role = PersonRole.SCHOLAR,
            seed = 42
        )
        assertEquals(
            WorldRosterEngine.generateBackground(spec),
            WorldRosterEngine.generateBackground(spec)
        )
    }

    @Test
    fun `nonce creates distinct generated people`() {
        val base = WorldRosterEngine.GeneratedPersonSpec(
            factionId = "southern_song",
            regionId = "jiankang",
            role = PersonRole.LOCAL_OFFICIAL,
            seed = 7
        )
        val a = WorldRosterEngine.generateBackground(base)
        val b = WorldRosterEngine.generateBackground(base.copy(nonce = 1))
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun `background person can be promoted without changing identity`() {
        val spec = WorldRosterEngine.GeneratedPersonSpec(seed = 9)
        val person = WorldRosterEngine.generateBackground(spec)
        val roster = WorldRosterEngine.emptyDefault().copy(people = mapOf(person.id to person))
        val promoted = WorldRosterEngine.promote(roster, person.id, AgentTier.ACTIVE)
        assertEquals(person.id, promoted.people.getValue(person.id).id)
        assertEquals(AgentTier.ACTIVE, promoted.people.getValue(person.id).tier)
    }

    @Test
    fun `default world contains multiple states and internal blocs`() {
        val factions = WorldFactionCatalog.asMap()
        assertTrue("southern_song" in factions)
        assertTrue("jin" in factions)
        assertTrue("western_xia" in factions)
        assertTrue("dali" in factions)
        assertTrue("goryeo" in factions)
        assertEquals("southern_song", factions.getValue("song_war_bloc").parentFactionId)
    }

    @Test
    fun `search filters faction role tier and tag`() {
        val a = WorldPersonRecord(
            id = "a",
            displayName = "甲",
            origin = PersonOrigin.HISTORICAL,
            factionId = "southern_song",
            role = PersonRole.MILITARY_OFFICER,
            tier = AgentTier.CORE,
            tags = setOf("主战")
        )
        val b = WorldPersonRecord(
            id = "b",
            displayName = "乙",
            origin = PersonOrigin.HISTORICAL,
            factionId = "jin",
            role = PersonRole.MILITARY_OFFICER,
            tier = AgentTier.CORE,
            tags = setOf("主战")
        )
        val roster = WorldRoster(people = mapOf(a.id to a, b.id to b))
        val found = WorldRosterEngine.search(
            roster = roster,
            factionId = "southern_song",
            role = PersonRole.MILITARY_OFFICER,
            tier = AgentTier.CORE,
            tag = "主战"
        )
        assertEquals(listOf(a), found)
    }
}
