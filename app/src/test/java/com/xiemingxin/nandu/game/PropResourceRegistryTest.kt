package com.xiemingxin.nandu.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PropResourceRegistryTest {

    @Test
    fun `all props have unique stable asset paths`() {
        val props = PropResourceRegistry.all.values.toList()
        assertEquals(props.size, props.map { it.id }.toSet().size)
        assertEquals(props.size, props.map { it.imagePath }.toSet().size)
        assertTrue(props.all { it.imagePath.startsWith("images/props/v1/prop_") })
        assertTrue(props.all { it.imagePath.endsWith(".webp") })
    }

    @Test
    fun `core officers expose signature prop sets`() {
        val yueFei = PropResourceRegistry.signaturePropsForOfficer("yue_fei").map { it.id }
        val qinHui = PropResourceRegistry.signaturePropsForOfficer("qin_hui").map { it.id }
        val emperor = PropResourceRegistry.signaturePropsForOfficer("zhao_gou").map { it.id }

        assertTrue("tiger_tally" in yueFei)
        assertTrue("officer_sword" in yueFei)
        assertTrue("secret_memorial" in qinHui)
        assertTrue("imperial_seal" in emperor)
    }

    @Test
    fun `edict event shows seal and edict instead of military props`() {
        val props = PropResourceRegistry.propsForEvent(
            eventId = "imperial_appointment",
            type = "history_event",
            title = "御前下诏",
            description = "官家命中书舍人草诏，正式任命新帅。",
            artHint = "案上诏书与御玺"
        ).map { it.id }

        assertTrue("imperial_edict" in props)
        assertTrue("imperial_seal" in props)
        assertTrue("tiger_tally" !in props)
    }

    @Test
    fun `military crisis surfaces command and intelligence objects`() {
        val props = PropResourceRegistry.propsForEvent(
            eventId = "jin_army_crosses_huai",
            type = "jin_event",
            title = "金军压境",
            description = "前线急报送达，枢密院连夜军议。",
            artHint = "军机舆图"
        ).map { it.id }

        assertTrue("tiger_tally" in props)
        assertTrue("campaign_map" in props)
        assertTrue("military_report" in props)
    }

    @Test
    fun `secret intelligence event surfaces sealed memorial`() {
        val props = PropResourceRegistry.propsForEvent(
            eventId = "secret_police_report",
            type = "folk_rumor",
            title = "皇城司密报",
            description = "一封密奏送入御书房。",
            artHint = "封缄文书"
        ).map { it.id }

        assertEquals("secret_memorial", props.first())
        assertTrue("dispatch_box" in props)
    }

    @Test
    fun `golden tablet recall event gets highest priority`() {
        val props = PropResourceRegistry.propsForEvent(
            eventId = "yuefei_recall",
            type = "history_event",
            title = "十二道金牌",
            description = "急诏催岳飞班师。",
            artHint = "金字牌与急递匣"
        ).map { it.id }

        assertEquals("golden_tablet", props.first())
        assertTrue("imperial_edict" in props)
        assertTrue(props.size <= 3)
    }
}
