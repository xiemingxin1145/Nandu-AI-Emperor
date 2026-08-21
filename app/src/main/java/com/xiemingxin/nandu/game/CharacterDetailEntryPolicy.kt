package com.xiemingxin.nandu.game

/**
 * 人物详情入口策略。
 *
 * 只负责“哪些人物可以点开、隐藏人物是否只给线索、详情字段从哪读”。
 * 不改人物调度、旅行、宫殿任务或朝堂 AI。
 */
data class CharacterDetailSnapshot(
    val officerId: String,
    val displayName: String,
    val isHiddenHint: Boolean,
    val faction: String,
    val identity: String,
    val currentRole: String,
    val cityName: String,
    val statusHint: String,
    val loyaltyLabel: String,
    val ambitionLabel: String,
    val force: Int?,
    val command: Int?,
    val strategy: Int?,
    val politics: Int?,
    val skills: List<String>,
    val armyName: String?,
    val bio: String
)

object CharacterDetailEntryPolicy {
    private val revealedStatuses = setOf(
        OfficerStatus.IN_COURT,
        OfficerStatus.DEPLOYED,
        OfficerStatus.DISMISSED,
        OfficerStatus.DECEASED
    )

    private val leadStatuses = setOf(
        OfficerStatus.HIDDEN,
        OfficerStatus.SOLDIER,
        OfficerStatus.WANDERING
    )

    fun usesHiddenHint(officer: Officer, gameState: GameState): Boolean {
        val isLeadPending = officer.id in gameState.talentLeads && officer.status in leadStatuses
        val isFullyRevealed = officer.status in revealedStatuses || isLeadPending
        return !isFullyRevealed && officer.status == OfficerStatus.HIDDEN
    }

    /** 国政页“朝廷已登记人才”可点开的人物。不泄露隐藏/未入局名单。 */
    fun registeredForStateScreen(state: GameState): List<Officer> =
        state.officers.filter {
            it.status == OfficerStatus.IN_COURT || it.status == OfficerStatus.DEPLOYED
        }

    fun snapshot(officer: Officer, gameState: GameState): CharacterDetailSnapshot {
        val cityName = gameState.cities.find { it.id == officer.currentCityId }?.name
            ?: officer.currentCityId.ifBlank { "行踪未明" }
        if (usesHiddenHint(officer, gameState)) {
            return CharacterDetailSnapshot(
                officerId = officer.id,
                displayName = "？？？",
                isHiddenHint = true,
                faction = "",
                identity = "尚未被发现",
                currentRole = "",
                cityName = cityName,
                statusHint = CharacterStateSource.statusHint(gameState, officer),
                loyaltyLabel = "",
                ambitionLabel = "",
                force = null,
                command = null,
                strategy = null,
                politics = null,
                skills = emptyList(),
                armyName = null,
                bio = ""
            )
        }
        val profile = officer.profile()
        val identity = listOf(profile.rank, profile.origin)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
            .ifBlank { "身份未载" }
        return CharacterDetailSnapshot(
            officerId = officer.id,
            displayName = officer.name.ifBlank { "无名" },
            isHiddenHint = false,
            faction = officer.faction.ifBlank { "未载" },
            identity = identity,
            currentRole = AppointmentSystem.currentRole(gameState, officer.id),
            cityName = cityName,
            statusHint = CharacterStateSource.statusHint(gameState, officer),
            loyaltyLabel = OfficerIntel.loyaltyLabel(officer.loyalty),
            ambitionLabel = OfficerIntel.ambitionLabel(profile.ambition),
            force = officer.force,
            command = officer.command,
            strategy = officer.strategy,
            politics = officer.politics,
            skills = profile.skills,
            armyName = CharacterStateSource.armyOf(gameState, officer.id)?.name,
            bio = officer.bio
        )
    }
}
