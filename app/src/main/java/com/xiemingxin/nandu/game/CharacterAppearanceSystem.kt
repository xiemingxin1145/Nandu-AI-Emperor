package com.xiemingxin.nandu.game

/**
 * V2.1 人物登场门槛系统。
 *
 * 目的：人物不能一开局全部摆出来。
 * 未登场的人只显示线索、剪影或传闻；入朝后才进入朝会、派系和角色线。
 */
enum class CharacterVisibility(val label: String) {
    HIDDEN("未登场"),
    RUMORED("传闻中"),
    SEEN("已露面"),
    COURT("已入朝"),
    CORE("核心人物")
}

data class CharacterAppearanceInfo(
    val characterId: String,
    val displayName: String,
    val visibility: CharacterVisibility,
    val allowedPalaces: Set<String>,
    val portraitPath: String,
    val silhouettePath: String,
    val revealHint: String
)

object CharacterAppearanceSystem {
    private val alwaysCore = setOf("li_gang", "zhao_ding")
    private val earlyMilitary = setOf("han_shizhong")
    private val hiddenTalents = setOf("yue_fei", "wu_jie", "liu_qi")
    private val politicalShadow = setOf("qin_hui")
    private val innerPalace = setOf("empress", "dowager", "eunuch")

    fun infoFor(state: GameState, characterId: String, fallbackName: String = characterId): CharacterAppearanceInfo {
        val officer = state.officers.firstOrNull { it.id == characterId }
        val visibility = visibilityFor(state, characterId)
        val name = officer?.name ?: fallbackName
        val allowed = allowedPalacesFor(characterId, visibility)
        val portrait = if (visibility.ordinal >= CharacterVisibility.SEEN.ordinal) {
            "images/characters/${characterId}.webp"
        } else {
            "images/characters/silhouette_${characterId}.webp"
        }
        return CharacterAppearanceInfo(
            characterId = characterId,
            displayName = if (visibility == CharacterVisibility.HIDDEN) "未闻其名" else name,
            visibility = visibility,
            allowedPalaces = allowed,
            portraitPath = portrait,
            silhouettePath = "images/characters/silhouette_unknown.webp",
            revealHint = revealHintFor(state, characterId, name, visibility)
        )
    }

    fun visibilityFor(state: GameState, characterId: String): CharacterVisibility {
        if (characterId in innerPalace) return CharacterVisibility.CORE
        if (characterId in alwaysCore) return CharacterVisibility.CORE
        val officer = state.officers.firstOrNull { it.id == characterId }
        if (officer != null) {
            return when (officer.status) {
                OfficerStatus.IN_COURT -> CharacterVisibility.COURT
                OfficerStatus.DEPLOYED -> CharacterVisibility.SEEN
                OfficerStatus.SOLDIER, OfficerStatus.WANDERING -> if (state.talentLeads.contains(characterId)) CharacterVisibility.RUMORED else CharacterVisibility.HIDDEN
                OfficerStatus.HIDDEN -> if (state.talentLeads.contains(characterId)) CharacterVisibility.RUMORED else if (characterId in politicalShadow && state.turn >= 3) CharacterVisibility.RUMORED else CharacterVisibility.HIDDEN
                OfficerStatus.DISMISSED, OfficerStatus.DECEASED -> CharacterVisibility.SEEN
            }
        }
        return when (characterId) {
            "bureau_clerk" -> CharacterVisibility.SEEN
            else -> CharacterVisibility.HIDDEN
        }
    }

    fun canAppearInPalace(state: GameState, characterId: String, palaceId: String): Boolean {
        val info = infoFor(state, characterId)
        if (info.visibility == CharacterVisibility.HIDDEN) return false
        return palaceId in info.allowedPalaces || info.visibility == CharacterVisibility.CORE
    }

    fun filterCouncilLines(state: GameState, palaceId: String, lines: List<CouncilLine>): List<CouncilLine> =
        lines.filter { line -> canAppearInPalace(state, line.speakerId, palaceId) }
            .ifEmpty { lines.filterNot { it.speakerId in hiddenTalents || it.speakerId in politicalShadow } }

    fun visibleOfficerIds(state: GameState): Set<String> = state.officers
        .filter { visibilityFor(state, it.id) != CharacterVisibility.HIDDEN }
        .map { it.id }
        .toSet()

    private fun allowedPalacesFor(characterId: String, visibility: CharacterVisibility): Set<String> {
        if (visibility == CharacterVisibility.HIDDEN) return emptySet()
        return when (characterId) {
            "yue_fei", "han_shizhong", "wu_jie", "liu_qi" -> setOf(PalaceIds.SHUMI, PalaceIds.CHUIGONG, PalaceIds.WENDE, PalaceIds.TAIMIAO)
            "li_gang" -> setOf(PalaceIds.CHUIGONG, PalaceIds.TAIMIAO, PalaceIds.WENDE, PalaceIds.SHUMI)
            "zhao_ding" -> setOf(PalaceIds.CHUIGONG, PalaceIds.ZHENGSHI, PalaceIds.WENDE, PalaceIds.YUSHU)
            "qin_hui" -> if (visibility.ordinal >= CharacterVisibility.RUMORED.ordinal) setOf(PalaceIds.CHUIGONG, PalaceIds.YUSHU, PalaceIds.HUANGCHENG, PalaceIds.ZHENGSHI) else emptySet()
            "empress", "dowager", "eunuch" -> setOf(PalaceIds.HOUYUAN)
            "bureau_clerk" -> setOf(PalaceIds.HUANGCHENG, PalaceIds.YUSHU)
            else -> setOf(PalaceIds.CHUIGONG, PalaceIds.WENDE)
        }
    }

    private fun revealHintFor(state: GameState, characterId: String, name: String, visibility: CharacterVisibility): String = when (visibility) {
        CharacterVisibility.HIDDEN -> when (characterId) {
            "yue_fei" -> "军中似有低阶武人可用，需通过文德殿寻访或城中传闻发现。"
            "qin_hui" -> "此人尚未完全入局，后续将从御书房、皇城司或主和议题中浮出水面。"
            else -> "此人尚未登场，需等待传闻、召见或剧情事件。"
        }
        CharacterVisibility.RUMORED -> "$name 已有线索，可通过召见、考校或相关宫殿事件正式登场。"
        CharacterVisibility.SEEN -> "$name 已露面，但未必能参与所有朝会。"
        CharacterVisibility.COURT -> "$name 已入朝，可进入朝会、派系与角色线。"
        CharacterVisibility.CORE -> "$name 是当前核心人物，可稳定出现在相关宫殿。"
    }
}
