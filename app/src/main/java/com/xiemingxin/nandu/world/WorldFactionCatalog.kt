package com.xiemingxin.nandu.world

/**
 * Initial faction directory for the 12th-century East-Asian sandbox.
 * These are data seeds, not hard-coded enemies/allies. Diplomacy and internal blocs are resolved by
 * authoritative game systems later.
 */
object WorldFactionCatalog {
    val defaults: List<WorldFactionRecord> = listOf(
        WorldFactionRecord(
            id = "southern_song",
            displayName = "南宋",
            kind = FactionKind.STATE,
            tags = setOf("song", "player_candidate"),
            description = "以临安朝廷为核心的南宋政权。"
        ),
        WorldFactionRecord(
            id = "jin",
            displayName = "金",
            kind = FactionKind.STATE,
            tags = setOf("jurchen", "north"),
            description = "女真建立的北方强国，内部可继续拆分宗室、贵族、汉官与前线军镇。"
        ),
        WorldFactionRecord(
            id = "western_xia",
            displayName = "西夏",
            kind = FactionKind.STATE,
            tags = setOf("tangut", "northwest"),
            description = "西北政权，可独立外交、贸易与战争。"
        ),
        WorldFactionRecord(
            id = "dali",
            displayName = "大理",
            kind = FactionKind.STATE,
            tags = setOf("southwest"),
            description = "西南政权，可参与贸易、宗教、边境与外交事件。"
        ),
        WorldFactionRecord(
            id = "goryeo",
            displayName = "高丽",
            kind = FactionKind.STATE,
            tags = setOf("northeast", "maritime"),
            description = "东北亚国家，可参与使节、海贸与区域外交。"
        ),
        WorldFactionRecord(
            id = "steppe_confederacies",
            displayName = "草原诸部",
            kind = FactionKind.NOMADIC,
            tags = setOf("steppe", "dynamic"),
            description = "不是单一国家；后续可按剧本拆分为多个部族、联盟与新兴政权。"
        ),
        WorldFactionRecord(
            id = "song_war_bloc",
            displayName = "主战派",
            kind = FactionKind.COURT_BLOC,
            parentFactionId = "southern_song",
            tags = setOf("court", "war"),
            description = "朝廷内部倾向恢复失地、强化边军的政治网络。"
        ),
        WorldFactionRecord(
            id = "song_peace_bloc",
            displayName = "主和派",
            kind = FactionKind.COURT_BLOC,
            parentFactionId = "southern_song",
            tags = setOf("court", "peace"),
            description = "朝廷内部更重视停战、财政恢复与政局稳定的政治网络。"
        )
    )

    fun asMap(): Map<String, WorldFactionRecord> = defaults.associateBy { it.id }
}
