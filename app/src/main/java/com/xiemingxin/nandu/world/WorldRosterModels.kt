package com.xiemingxin.nandu.world

/**
 * Stage 11 preparation: scalable world roster data that can hold both historical and generated people.
 *
 * This layer is intentionally independent from CharacterAgentState/GameState so Stage 8 can continue
 * on its own branch without merge conflicts. A later integration pass can activate selected entries
 * into full character agents.
 */
enum class PersonOrigin {
    HISTORICAL,
    GENERATED
}

enum class AgentTier {
    CORE,
    ACTIVE,
    BACKGROUND
}

enum class PersonStatus {
    UNKNOWN,
    UNDISCOVERED,
    AVAILABLE,
    IN_SERVICE,
    CAPTIVE,
    EXILED,
    RETIRED,
    DECEASED
}

enum class PersonRole {
    EMPEROR,
    IMPERIAL_CLAN,
    CIVIL_OFFICIAL,
    MILITARY_OFFICER,
    LOCAL_OFFICIAL,
    ENVOY,
    SCHOLAR,
    MERCHANT,
    LANDOWNER,
    MONK_OR_DAOIST,
    CLAN_LEADER,
    BANDIT_OR_REBEL,
    ARTISAN,
    COMMONER,
    OTHER
}

data class WorldPersonRecord(
    val id: String,
    val displayName: String,
    val origin: PersonOrigin,
    val factionId: String? = null,
    val homeRegionId: String? = null,
    val role: PersonRole = PersonRole.OTHER,
    val tier: AgentTier = AgentTier.BACKGROUND,
    val status: PersonStatus = PersonStatus.UNKNOWN,
    val birthYear: Int? = null,
    val deathYear: Int? = null,
    val biography: String = "",
    val tags: Set<String> = emptySet(),
    val familyId: String? = null,
    val sourceNote: String? = null
)

enum class FactionKind {
    STATE,
    COURT_BLOC,
    ARMY_COMMAND,
    LOCAL_POWER,
    CLAN,
    MERCHANT_NETWORK,
    RELIGIOUS,
    REBEL,
    NOMADIC,
    OTHER
}

data class WorldFactionRecord(
    val id: String,
    val displayName: String,
    val kind: FactionKind,
    val parentFactionId: String? = null,
    val capitalRegionId: String? = null,
    val tags: Set<String> = emptySet(),
    val description: String = ""
)

data class WorldRoster(
    val people: Map<String, WorldPersonRecord> = emptyMap(),
    val factions: Map<String, WorldFactionRecord> = emptyMap()
) {
    fun person(id: String): WorldPersonRecord? = people[id]
    fun faction(id: String): WorldFactionRecord? = factions[id]

    fun peopleByFaction(factionId: String): List<WorldPersonRecord> =
        people.values.filter { it.factionId == factionId }

    fun peopleByTier(tier: AgentTier): List<WorldPersonRecord> =
        people.values.filter { it.tier == tier }
}
