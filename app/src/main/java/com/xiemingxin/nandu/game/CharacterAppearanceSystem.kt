package com.xiemingxin.nandu.game

/**
 * V2.2 人物登场 / 实体在场门槛。
 *
 * 核心原则：
 * 1. “玩家知道这个人”不等于“这个人此刻肉身站在宫殿里”。
 * 2. 可征辟、在野、军中服役、外放/领军的人，只能通过传闻、军报、奏折被提及，
 *    不能同时出现在实体朝会。
 * 3. 朝会人物必须从同一份 Officer.status 得出，不允许招募系统和朝会系统各自造一份岳飞。
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
    private val hiddenTalents = setOf("yue_fei", "wu_jie", "liu_qi")
    private val politicalShadow = setOf("qin_hui")
    private val innerPalace = setOf("empress", "dowager", "eunuch")

    fun infoFor(state: GameState, characterId: String, fallbackName: String = characterId): CharacterAppearanceInfo {
        val officer = state.officers.firstOrNull { it.id == characterId }
        val visibility = visibilityFor(state, characterId)
        val name = officer?.name ?: fallbackName
        val allowed = allowedPalacesFor(characterId, visibility)
        val portrait = if (visibility.ordinal >= CharacterVisibility.SEEN.ordinal) {
            ArtResourceRegistry.portraitForOfficer(characterId)
        } else {
            ArtResourceRegistry.Fallback.portrait
        }
        return CharacterAppearanceInfo(
            characterId = characterId,
            displayName = if (visibility == CharacterVisibility.HIDDEN) "未闻其名" else name,
            visibility = visibility,
            allowedPalaces = allowed,
            portraitPath = portrait,
            silhouettePath = ArtResourceRegistry.Fallback.portrait,
            revealHint = revealHintFor(state, characterId, name, visibility)
        )
    }

    fun visibilityFor(state: GameState, characterId: String): CharacterVisibility {
        if (characterId in innerPalace) return CharacterVisibility.CORE
        if (characterId == "bureau_clerk") return CharacterVisibility.SEEN

        val officer = state.officers.firstOrNull { it.id == characterId }
        if (officer != null) {
            return when (officer.status) {
                // 核心人物只有在真的“在朝”时才是 CORE；外放后仍然只是已知人物。
                OfficerStatus.IN_COURT -> if (characterId in alwaysCore) CharacterVisibility.CORE else CharacterVisibility.COURT
                OfficerStatus.DEPLOYED -> CharacterVisibility.SEEN
                OfficerStatus.SOLDIER, OfficerStatus.WANDERING ->
                    if (state.talentLeads.contains(characterId)) CharacterVisibility.RUMORED else CharacterVisibility.HIDDEN
                OfficerStatus.HIDDEN ->
                    if (state.talentLeads.contains(characterId)) CharacterVisibility.RUMORED
                    else if (characterId in politicalShadow && state.turn >= 3) CharacterVisibility.RUMORED
                    else CharacterVisibility.HIDDEN
                OfficerStatus.DISMISSED, OfficerStatus.DECEASED -> CharacterVisibility.SEEN
            }
        }

        // 没有 Officer 实体记录的人，不因为名字著名就凭空在朝。
        return CharacterVisibility.HIDDEN
    }

    /**
     * 判断人物能否“肉身”出现在某一宫殿。
     * 这里故意比 visibility 更严格：SEEN / RUMORED 只代表玩家知道他，不代表他人在京城。
     */
    fun canAppearInPalace(state: GameState, characterId: String, palaceId: String): Boolean {
        if (characterId in innerPalace) return palaceId == PalaceIds.HOUYUAN
        if (characterId == "bureau_clerk") {
            return palaceId == PalaceIds.HUANGCHENG || palaceId == PalaceIds.YUSHU
        }

        val officer = state.officers.firstOrNull { it.id == characterId } ?: return false

        // 统一人物状态源（CharacterStateSource.isAtCourt）：
        // 必须 status==IN_COURT，人在京城，且不在"奉诏回京途中"，才算真的肉身在朝。
        // DEPLOYED 可在军报/奏折中出现，但不能瞬移进垂拱殿；
        // SOLDIER/WANDERING/HIDDEN 仍属待发现/待征辟，更不能一边征辟一边上朝；
        // 正在赶路的人也不行——诏令已发不等于人已经到了。
        if (!CharacterStateSource.isAtCourt(officer)) return false

        val allowed = allowedPalacesFor(characterId, visibilityFor(state, characterId))
        return palaceId in allowed
    }

    /**
     * 朝会过滤。若当前场景里所有预写名臣都不在京，不再把他们“兜底复活”回来，
     * 而是使用匿名当值官员承接信息，避免时间线/位置穿帮。
     */
    fun filterCouncilLines(state: GameState, palaceId: String, lines: List<CouncilLine>): List<CouncilLine> {
        val physicallyPresent = lines.filter { line -> canAppearInPalace(state, line.speakerId, palaceId) }
        if (physicallyPresent.isNotEmpty()) return physicallyPresent

        return listOf(
            CouncilLine(
                speakerId = "duty_official",
                speakerName = dutyOfficialName(palaceId),
                role = dutyOfficialRole(palaceId),
                attitude = "concerned",
                text = "诸司奏牍已经汇齐，请官家裁断。外任诸臣另以军报、奏札陈情，不敢虚列殿班。"
            )
        )
    }

    fun visibleOfficerIds(state: GameState): Set<String> = state.officers
        .filter { visibilityFor(state, it.id) != CharacterVisibility.HIDDEN }
        .map { it.id }
        .toSet()

    private fun allowedPalacesFor(characterId: String, visibility: CharacterVisibility): Set<String> {
        if (visibility == CharacterVisibility.HIDDEN) return emptySet()
        return when (characterId) {
            "yue_fei", "han_shizhong", "wu_jie", "liu_qi" ->
                setOf(PalaceIds.SHUMI, PalaceIds.CHUIGONG, PalaceIds.WENDE, PalaceIds.TAIMIAO)
            "li_gang" -> setOf(PalaceIds.CHUIGONG, PalaceIds.TAIMIAO, PalaceIds.WENDE, PalaceIds.SHUMI)
            "zhao_ding" -> setOf(PalaceIds.CHUIGONG, PalaceIds.ZHENGSHI, PalaceIds.WENDE, PalaceIds.YUSHU)
            "qin_hui" -> setOf(PalaceIds.CHUIGONG, PalaceIds.YUSHU, PalaceIds.HUANGCHENG, PalaceIds.ZHENGSHI)
            "empress", "dowager", "eunuch" -> setOf(PalaceIds.HOUYUAN)
            "bureau_clerk" -> setOf(PalaceIds.HUANGCHENG, PalaceIds.YUSHU)
            else -> setOf(PalaceIds.CHUIGONG, PalaceIds.WENDE)
        }
    }

    private fun dutyOfficialName(palaceId: String): String = when (palaceId) {
        PalaceIds.SHUMI -> "枢密院承旨"
        PalaceIds.ZHENGSHI -> "政事堂堂吏"
        PalaceIds.WENDE -> "中书舍人"
        PalaceIds.YUSHU -> "御前承旨"
        PalaceIds.HUANGCHENG -> "皇城司勾当官"
        PalaceIds.TAIMIAO -> "太常寺官"
        else -> "给事中"
    }

    private fun dutyOfficialRole(palaceId: String): String = when (palaceId) {
        PalaceIds.SHUMI -> "军务当值"
        PalaceIds.ZHENGSHI -> "政务当值"
        PalaceIds.WENDE -> "文班当值"
        PalaceIds.YUSHU -> "御前奏事"
        PalaceIds.HUANGCHENG -> "密奏当值"
        PalaceIds.TAIMIAO -> "礼官"
        else -> "殿中当值"
    }

    private fun revealHintFor(
        state: GameState,
        characterId: String,
        name: String,
        visibility: CharacterVisibility
    ): String = when (visibility) {
        CharacterVisibility.HIDDEN -> when (characterId) {
            "yue_fei" -> "军中似有可用武人，需通过军报、举荐或地方线索发现。"
            "qin_hui" -> "此人尚未完全入局，后续将从御书房、皇城司或主和议题中浮出水面。"
            else -> "此人尚未登场，需等待传闻、举荐、召见或剧情事件。"
        }
        CharacterVisibility.RUMORED -> "$name 已有线索，但尚未入朝；只能征辟、召见或继续追查，不能直接列班奏对。"
        CharacterVisibility.SEEN -> "$name 已为朝廷所知，但目前不在殿班；若在外领军/任职，只能通过军报、奏札或奉诏回京参与朝议。"
        CharacterVisibility.COURT -> "$name 已入朝，可依官职进入相关朝会、派系与角色线。"
        CharacterVisibility.CORE -> "$name 当前在朝且属于核心人物，可稳定出现在相应宫殿。"
    }
}
