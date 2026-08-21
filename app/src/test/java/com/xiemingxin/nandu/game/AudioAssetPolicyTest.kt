package com.xiemingxin.nandu.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Prevents polluted/legacy or non-authoritative BGM from silently returning. */
class AudioAssetPolicyTest {
    private val approved = setOf(
        "bgm_main_menu.ogg",
        "bgm_chuigong_hall_entry.ogg",
        "bgm_chuigong_hall_loop.ogg",
        "bgm_garden_loop.ogg",
        "bgm_study_loop.ogg",
        "bgm_worldmap_loop.ogg",
        "bgm_linan_loop.ogg",
        "bgm_military_camp_loop.ogg"
    )

    /**
     * Authoritative V1.6.1 user-approved archive hashes.
     * Source of truth: Nandu-Audio-Approved-V1.6.1.zip.
     * Independently cross-checked against the previously accepted V1.6.1 RC APK:
     * all 8 files are byte-for-byte identical.
     */
    private val authoritativeSha256 = mapOf(
        "bgm_main_menu.ogg" to "44f00d3353d14a71e61c133a0661621b5950d0929d69cb4d1098cd6e7cf9de3e",
        "bgm_chuigong_hall_entry.ogg" to "a9e3bd2ce670da41d38ebf6454f784a5a3b25eea764c9dd531c9fa78b8dc229c",
        "bgm_chuigong_hall_loop.ogg" to "b32447006530504059442b5a9c463940787f025d4704ef437a381dd11dd7b823",
        "bgm_garden_loop.ogg" to "d03b75d64cfc1919aa11902ff055bfe593595508a4f54b12dde049be68097b88",
        "bgm_study_loop.ogg" to "018c1de2caa94adc632be07c429b196ffb0c04faa681cebbee9b1f025f6d14b3",
        "bgm_worldmap_loop.ogg" to "586ffdfbb6cb0689f4076ec39938e70d4161c509a7381372f8f7a94660784472",
        "bgm_linan_loop.ogg" to "a74c5973e848ebd4832f4bc6ceb962be5c5494d6b151da02bdcdb24f5f89ac71",
        "bgm_military_camp_loop.ogg" to "7ca793c5c1b174b6954babdd97026357947742501219d5eea16ab76e867fb02a"
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
        "bgm_map.ogg",
        "bgm_market.ogg",
        "bgm_military.ogg",
        "bgm_palace_hall.ogg",
        "bgm_ritual.ogg",
        "bgm_victory.ogg"
    )

    @Test
    fun `production BGM directory contains only approved track names`() {
        val bgmDir = locateBgmDir() ?: return
        val oggFiles = bgmDir.listFiles().orEmpty()
            .filter { it.isFile && it.extension.equals("ogg", ignoreCase = true) }
            .map { it.name }
            .toSet()

        val forbidden = oggFiles intersect bannedLegacy
        assertTrue("Legacy BGM must never return to production assets: $forbidden", forbidden.isEmpty())

        val unreviewed = oggFiles - approved
        assertTrue("Every production BGM must be explicitly reviewed first: $unreviewed", unreviewed.isEmpty())
    }

    @Test
    fun `all eight authoritative BGM files are present and byte exact`() {
        val bgmDir = locateBgmDir()
        assertNotNull("Production BGM directory must exist for release validation", bgmDir)
        bgmDir!!

        val actualNames = bgmDir.listFiles().orEmpty()
            .filter { it.isFile && it.extension.equals("ogg", ignoreCase = true) }
            .map { it.name }
            .toSet()
        assertEquals("Release must contain exactly the 8 authoritative BGM files", approved, actualNames)

        authoritativeSha256.forEach { (name, expected) ->
            val file = File(bgmDir, name)
            assertTrue("Missing authoritative BGM: $name", file.isFile)
            val actual = file.sha256()
            assertEquals("BGM bytes differ from approved V1.6.1 archive: $name", expected, actual)
        }
    }

    @Test
    fun `approved names never overlap legacy blacklist`() {
        assertFalse("Approved BGM whitelist must not reuse banned legacy filenames", approved.any { it in bannedLegacy })
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

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
