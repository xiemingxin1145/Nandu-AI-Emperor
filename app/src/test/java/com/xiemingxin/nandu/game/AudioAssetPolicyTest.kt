package com.xiemingxin.nandu.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Prevents the old polluted BGM library from silently returning.
 *
 * The game intentionally allows approved files to be temporarily absent while assets are being imported,
 * but any OGG that does exist in the production BGM directory must be on the human-reviewed whitelist.
 */
class AudioAssetPolicyTest {

    private val approved = setOf(
        "bgm_chuigong_hall_entry.ogg",
        "bgm_chuigong_hall_loop.ogg",
        "bgm_garden_loop.ogg",
        "bgm_study_loop.ogg",
        "bgm_worldmap_loop.ogg",
        "bgm_linan_loop.ogg",
        "bgm_military_camp_loop.ogg"
    )

    private val bannedLegacy = setOf(
        "bgm_battle.ogg",
        "bgm_city.ogg",
        "bgm_court.ogg",
        "bgm_court_council.ogg",
        "bgm_crisis.ogg",
        "bgm_defeat.ogg",
        "bgm_diplomacy.ogg",
        "bgm_event_sad.ogg",
        "bgm_main_menu.ogg",
        "bgm_map.ogg",
        "bgm_market.ogg",
        "bgm_military.ogg",
        "bgm_palace_hall.ogg",
        "bgm_ritual.ogg",
        "bgm_victory.ogg"
    )

    @Test
    fun `production BGM directory contains only human approved tracks`() {
        val bgmDir = locateBgmDir()
        if (bgmDir == null) {
            // A temporarily empty BGM directory is valid during the clean rebuild.
            return
        }

        val oggFiles = bgmDir.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension.equals("ogg", ignoreCase = true) }
            .map { it.name }
            .toSet()

        val forbidden = oggFiles intersect bannedLegacy
        assertTrue(
            "Legacy BGM must never return to the production asset tree: $forbidden",
            forbidden.isEmpty()
        )

        val unreviewed = oggFiles - approved
        assertTrue(
            "Every production BGM must be explicitly human-reviewed first: $unreviewed",
            unreviewed.isEmpty()
        )
    }

    @Test
    fun `approved names never overlap legacy blacklist`() {
        assertFalse(
            "Approved BGM whitelist must not reuse a banned legacy filename",
            approved.any { it in bannedLegacy }
        )
    }

    private fun locateBgmDir(): File? {
        val userDir = File(System.getProperty("user.dir"))
        val candidates = listOf(
            File(userDir, "src/main/assets/audio/bgm"),
            File(userDir, "app/src/main/assets/audio/bgm"),
            File(userDir.parentFile ?: userDir, "app/src/main/assets/audio/bgm")
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
    }
}
