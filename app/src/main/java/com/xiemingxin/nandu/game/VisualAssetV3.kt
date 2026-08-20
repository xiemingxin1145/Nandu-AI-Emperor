package com.xiemingxin.nandu.game

/**
 * Visual Integration V3
 *
 * Keeps the newly restored user art batch separate from the legacy registry so we can
 * safely switch featured art without disturbing old save/data mappings. UI should ask
 * this resolver first, then fall back to ArtResourceRegistry.
 */
object VisualAssetV3 {
    data class FeaturedCharacterArt(
        val portrait: String,
        val halfbody: String
    )

    private val featuredCharacters: Map<String, FeaturedCharacterArt> = mapOf(
        "yue_fei" to FeaturedCharacterArt(
            portrait = "images/characters/batch1/portrait_yue_fei_batch1.webp",
            halfbody = "images/characters/batch1/halfbody_yue_fei_batch1.webp"
        ),
        "han_shizhong" to FeaturedCharacterArt(
            portrait = "images/characters/batch1/portrait_han_shizhong_batch1.webp",
            halfbody = "images/characters/batch1/halfbody_han_shizhong_batch1.webp"
        ),
        // V2 corrects Qin Hui's headwear and is the preferred version.
        "qin_hui" to FeaturedCharacterArt(
            portrait = "images/characters/batch2/portrait_qin_hui_batch2.webp",
            halfbody = "images/characters/batch2/halfbody_qin_hui_batch2.webp"
        ),
        "liu_qi" to FeaturedCharacterArt(
            portrait = "images/characters/batch2/portrait_liu_qi_batch2.webp",
            halfbody = "images/characters/batch2/halfbody_liu_qi_batch2.webp"
        ),
        "wu_jie" to FeaturedCharacterArt(
            portrait = "images/characters/batch2/portrait_wu_jie_batch2.webp",
            halfbody = "images/characters/batch2/halfbody_wu_jie_batch2.webp"
        ),
        "zhao_ding" to FeaturedCharacterArt(
            portrait = "images/characters/batch2/portrait_zhao_ding_batch2.webp",
            halfbody = "images/characters/batch2/halfbody_zhao_ding_batch2.webp"
        ),
        "zhao_gou" to FeaturedCharacterArt(
            portrait = "images/characters/batch2/portrait_zhao_gou_batch2.webp",
            halfbody = "images/characters/batch2/halfbody_zhao_gou_batch2.webp"
        ),
        "wanyan_zongbi" to FeaturedCharacterArt(
            portrait = "images/characters/batch2/portrait_wanyan_zongbi_batch2.webp",
            halfbody = "images/characters/batch2/halfbody_wanyan_zongbi_batch2.webp"
        )
    )

    const val SHUNCHANG_WALL = "images/locations/batch2/bg_shunchang_wall_batch2.webp"
    const val CHUIGONG_HALL = "images/palace/batch1/chuigongdian_batch1.webp"

    private const val EVENT_DEPARTURE = "images/events/batch1/event_army_departure_batch1.webp"
    private const val EVENT_EDICT = "images/events/batch1/event_imperial_edict_batch1.webp"
    private const val EVENT_JIN_APPROACH = "images/events/batch1/event_jin_approach_batch1.webp"
    private const val EVENT_SHUNCHANG = "images/events/batch1/event_shunchang_prewar_batch1.webp"
    private const val EVENT_YANCHENG = "images/events/batch2/event_yancheng_victory_batch2.webp"

    fun portraitForOfficer(officerId: String): String =
        featuredCharacters[officerId]?.portrait ?: ArtResourceRegistry.portraitForOfficer(officerId)

    fun halfbodyForOfficer(officerId: String): String =
        featuredCharacters[officerId]?.halfbody ?: ArtResourceRegistry.halfbodyForOfficer(officerId)

    fun isFeaturedOfficer(officerId: String): Boolean = officerId in featuredCharacters

    /**
     * Returns a V3 event CG when the event text clearly matches the new batch.
     * Null means the caller should fall back to ArtResourceRegistry's broader matcher.
     */
    fun eventImageFor(
        eventId: String,
        type: String,
        title: String,
        description: String,
        artHint: String
    ): String? {
        val text = "$eventId $type $title $description $artHint"
        return when {
            listOf("郾城", "郾城大捷", "yancheng", "大捷").any(text::contains) -> EVENT_YANCHENG
            listOf("顺昌", "shunchang").any(text::contains) -> EVENT_SHUNCHANG
            listOf("圣旨", "下诏", "诏书", "诏令", "imperial_edict").any(text::contains) -> EVENT_EDICT
            listOf("出征", "出师", "拔营", "departure").any(text::contains) -> EVENT_DEPARTURE
            listOf("金军压境", "金兵压境", "渡淮", "金军来袭", "jin_approach").any(text::contains) -> EVENT_JIN_APPROACH
            type == "jin_event" && listOf("金军", "金兵", "完颜", "攻城").any(text::contains) -> EVENT_JIN_APPROACH
            else -> null
        }
    }
}
