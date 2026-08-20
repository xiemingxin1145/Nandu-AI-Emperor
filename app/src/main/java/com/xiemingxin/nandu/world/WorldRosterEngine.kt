package com.xiemingxin.nandu.world

import kotlin.random.Random

/**
 * Cheap local roster engine. Background people can exist by the thousands without an LLM call.
 * Only people promoted to CORE/ACTIVE are candidates for a full CharacterAgent later.
 */
object WorldRosterEngine {
    data class GeneratedPersonSpec(
        val factionId: String? = null,
        val regionId: String? = null,
        val role: PersonRole = PersonRole.OTHER,
        val birthYearRange: IntRange = 1080..1130,
        val tags: Set<String> = emptySet(),
        val seed: Int,
        val nonce: Int = 0
    )

    private val surnames = listOf(
        "赵", "张", "王", "李", "陈", "刘", "杨", "吴", "周", "徐", "孙", "朱", "胡", "郭", "何", "高", "陆", "沈"
    )
    private val givenA = listOf("文", "仲", "子", "景", "伯", "彦", "士", "世", "德", "宗", "正", "元", "承", "守", "安", "怀")
    private val givenB = listOf("远", "宁", "昭", "衡", "简", "弼", "恭", "肃", "谦", "达", "济", "璋", "川", "礼", "章", "信")

    fun emptyDefault(): WorldRoster = WorldRoster(factions = WorldFactionCatalog.asMap())

    fun addHistorical(roster: WorldRoster, person: WorldPersonRecord): WorldRoster {
        require(person.origin == PersonOrigin.HISTORICAL) { "Historical import must use HISTORICAL origin" }
        return roster.copy(people = roster.people + (person.id to person))
    }

    fun generateBackground(spec: GeneratedPersonSpec): WorldPersonRecord {
        val random = Random(spec.seed * 31 + spec.nonce)
        val surname = surnames[random.nextInt(surnames.size)]
        val a = givenA[random.nextInt(givenA.size)]
        val b = givenB[random.nextInt(givenB.size)]
        val name = surname + a + b
        val year = spec.birthYearRange.random(random)
        val faction = spec.factionId ?: "independent"
        val id = "gen_${faction}_${spec.regionId ?: "unknown"}_${spec.role.name.lowercase()}_${spec.seed}_${spec.nonce}"
        return WorldPersonRecord(
            id = id,
            displayName = name,
            origin = PersonOrigin.GENERATED,
            factionId = spec.factionId,
            homeRegionId = spec.regionId,
            role = spec.role,
            tier = AgentTier.BACKGROUND,
            status = PersonStatus.AVAILABLE,
            birthYear = year,
            tags = spec.tags + "generated"
        )
    }

    fun addGenerated(roster: WorldRoster, spec: GeneratedPersonSpec): WorldRoster {
        val person = generateBackground(spec)
        return roster.copy(people = roster.people + (person.id to person))
    }

    fun promote(roster: WorldRoster, personId: String, tier: AgentTier): WorldRoster {
        val person = roster.people[personId] ?: return roster
        val updated = person.copy(tier = tier)
        return roster.copy(people = roster.people + (personId to updated))
    }

    fun updateStatus(roster: WorldRoster, personId: String, status: PersonStatus): WorldRoster {
        val person = roster.people[personId] ?: return roster
        return roster.copy(people = roster.people + (personId to person.copy(status = status)))
    }

    fun search(
        roster: WorldRoster,
        query: String = "",
        factionId: String? = null,
        tier: AgentTier? = null,
        role: PersonRole? = null,
        tag: String? = null
    ): List<WorldPersonRecord> {
        val q = query.trim()
        return roster.people.values
            .asSequence()
            .filter { factionId == null || it.factionId == factionId }
            .filter { tier == null || it.tier == tier }
            .filter { role == null || it.role == role }
            .filter { tag == null || tag in it.tags }
            .filter {
                q.isEmpty() || it.displayName.contains(q, ignoreCase = true) ||
                    it.biography.contains(q, ignoreCase = true) ||
                    it.tags.any { t -> t.contains(q, ignoreCase = true) }
            }
            .sortedWith(compareBy<WorldPersonRecord> { it.tier.ordinal }.thenBy { it.displayName })
            .toList()
    }
}
