package com.xiemingxin.nandu.world

/**
 * Small seed only. The important part is that this catalog is append-only and can grow to hundreds
 * of historical people without changing CharacterAgent code.
 */
object HistoricalPersonCatalog {
    val seeds: List<WorldPersonRecord> = listOf(
        WorldPersonRecord(
            id = "yue_fei",
            displayName = "岳飞",
            origin = PersonOrigin.HISTORICAL,
            factionId = "southern_song",
            role = PersonRole.MILITARY_OFFICER,
            tier = AgentTier.CORE,
            status = PersonStatus.IN_SERVICE,
            tags = setOf("主战", "将领", "北伐")
        ),
        WorldPersonRecord(
            id = "han_shizhong",
            displayName = "韩世忠",
            origin = PersonOrigin.HISTORICAL,
            factionId = "southern_song",
            role = PersonRole.MILITARY_OFFICER,
            tier = AgentTier.CORE,
            status = PersonStatus.IN_SERVICE,
            tags = setOf("将领", "江防")
        ),
        WorldPersonRecord(
            id = "qin_hui",
            displayName = "秦桧",
            origin = PersonOrigin.HISTORICAL,
            factionId = "southern_song",
            role = PersonRole.CIVIL_OFFICIAL,
            tier = AgentTier.CORE,
            status = PersonStatus.IN_SERVICE,
            tags = setOf("主和", "宰执")
        ),
        WorldPersonRecord(
            id = "zhao_ding",
            displayName = "赵鼎",
            origin = PersonOrigin.HISTORICAL,
            factionId = "southern_song",
            role = PersonRole.CIVIL_OFFICIAL,
            tier = AgentTier.CORE,
            status = PersonStatus.IN_SERVICE,
            tags = setOf("宰执", "政务")
        ),
        WorldPersonRecord(
            id = "wanyan_zongbi",
            displayName = "完颜宗弼",
            origin = PersonOrigin.HISTORICAL,
            factionId = "jin",
            role = PersonRole.MILITARY_OFFICER,
            tier = AgentTier.CORE,
            status = PersonStatus.IN_SERVICE,
            tags = setOf("金将", "前线统帅")
        )
    )

    fun asMap(): Map<String, WorldPersonRecord> = seeds.associateBy { it.id }
}
