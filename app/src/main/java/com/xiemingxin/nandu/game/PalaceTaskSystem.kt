package com.xiemingxin.nandu.game

/**
 * V1.6.2 宫殿待办派生系统。
 *
 * 待办目前从 GameState 派生，以兼容既有存档；但每一条正式待办必须能解释为当前世界
 * 状态的结果，不能仅因 turn % N 到点就伪造“有事待闻”。持久化、逾期和连锁后果仍留
 * 给后续版本深化。
 */
data class PalaceTask(
    val id: String,
    val palaceId: String,
    val title: String,
    val description: String,
    val severity: TaskSeverity,
    val source: TaskSource,
    val relatedOfficerIds: List<String> = emptyList(),
    val relatedCityIds: List<String> = emptyList(),
    val recommendedTab: Int = 1,
    val edictDraft: String = ""
)

enum class TaskSeverity(val label: String) {
    LOW("寻常"), MEDIUM("要务"), HIGH("急务"), URGENT("火急")
}

enum class TaskSource(val label: String) {
    COURT("朝议"), WAR_REPORT("军报"), FISCAL("钱粮"), TALENT("人才"), RUMOR("密折"),
    DIPLOMACY("外交"), TRADE("外贸"), PALACE("内廷"), RITUAL("礼制")
}

object PalaceIds {
    const val CHUIGONG = "chuigongdian"
    const val WENDE = "wendedian"
    const val SHUMI = "shumiyuan"
    const val ZHENGSHI = "zhengshitang"
    const val YUSHU = "yushufang"
    const val HUANGCHENG = "huangchengsi"
    const val HOUYUAN = "houyuan"
    const val TAIMIAO = "taimiao"
}

data class PalaceInfo(
    val id: String,
    val name: String,
    val subtitle: String,
    val icon: String,
    val defaultTab: Int,
    val backgroundPath: String = "images/buildings/building_imperial_palace_01.webp"
)

object PalaceRegistry {
    val palaces = listOf(
        PalaceInfo(PalaceIds.CHUIGONG, "应天行在·内殿", "AI圣旨 / 群臣奏议", "📜", 1),
        PalaceInfo(PalaceIds.WENDE, "文班公署", "任官招贤 / 文臣事务", "🎓", 3),
        PalaceInfo(PalaceIds.SHUMI, "枢密院", "军令战报 / 调兵设防", "⚔", 4),
        PalaceInfo(PalaceIds.ZHENGSHI, "政事堂", "钱粮民心 / 财政民政 / 外贸", "⚖", 3),
        PalaceInfo(PalaceIds.YUSHU, "御前便阁", "密折起居 / 外交情报", "🕯", 1),
        PalaceInfo(PalaceIds.HUANGCHENG, "皇城司", "侦缉暗线 / 情报验证", "🕵", 2),
        PalaceInfo(PalaceIds.HOUYUAN, "行在内廷", "宫廷事件 / 内廷建议", "🏮", 1),
        PalaceInfo(PalaceIds.TAIMIAO, "礼制事务", "正统国运 / 史评功业", "🐉", 3)
    )

    fun byId(id: String): PalaceInfo = palaces.firstOrNull { it.id == id } ?: palaces.first()
}

object PalaceTaskSystem {

    fun generate(state: GameState): List<PalaceTask> {
        val tasks = mutableListOf<PalaceTask>()
        val songCities = state.cities.filter { it.owner == "song" }
        val frontline = songCities
            .filter { it.controlState == "FRONTLINE" || it.controlState == "CONTESTED" }
            .sortedBy { it.defense }
        val weakestFront = frontline.firstOrNull()
        val lowGrainCity = songCities.sortedBy { it.grain }.firstOrNull()
        val hiddenTalent = state.officers.firstOrNull {
            (it.status == OfficerStatus.HIDDEN || it.status == OfficerStatus.WANDERING) && !state.talentLeads.contains(it.id)
        }
        val riskyOfficer = state.officers
            .filter { it.ambition >= 70 || it.loyalty <= 45 }
            .filter { it.status !in setOf(OfficerStatus.CAPTIVE, OfficerStatus.NOT_YET_RELEVANT, OfficerStatus.DECEASED) }
            .maxByOrNull { it.ambition }

        val openingCourtIds = state.officers
            .filter { CharacterAppearanceSystem.canAppearInPalace(state, it.id, PalaceIds.CHUIGONG) }
            .map { it.id }

        tasks += PalaceTask(
            id = "court_${state.turn}",
            palaceId = PalaceIds.CHUIGONG,
            title = if (state.courtStability < 45) "主战主和争执不下" else "本旬朝议待断",
            description = if (state.courtStability < 45) {
                "朝局摇动，诸臣各执一词。可在行在内殿下旨定调，或留中再议。"
            } else {
                "应天行在群臣候奏，待官家定本旬军政轻重。"
            },
            severity = if (state.courtStability < 45) TaskSeverity.HIGH else TaskSeverity.MEDIUM,
            source = TaskSource.COURT,
            relatedOfficerIds = openingCourtIds,
            recommendedTab = 1,
            edictDraft = "传朕旨意：今日朝议先定军政轻重。李纲议边防，黄潜善、汪伯彦陈南幸利害，诸司各以实情具奏。"
        )

        if (state.jinThreat >= 75 || weakestFront != null) {
            val city = weakestFront ?: songCities.firstOrNull()
            tasks += PalaceTask(
                id = "war_${state.turn}_${city?.id ?: "front"}",
                palaceId = PalaceIds.SHUMI,
                title = if (state.jinThreat >= 85) "金军压境，边报火急" else "中原防线需整备",
                description = city?.let { "${it.name}防御${it.defense}、守军${it.troops}，需议调兵、修城或筹粮。" }
                    ?: "金国威胁升高，枢密院请定防线。",
                severity = if (state.jinThreat >= 85) TaskSeverity.URGENT else TaskSeverity.HIGH,
                source = TaskSource.WAR_REPORT,
                relatedOfficerIds = state.officers
                    .filter { it.status in setOf(OfficerStatus.IN_CAPITAL, OfficerStatus.IN_COURT, OfficerStatus.DEPLOYED) }
                    .filter { it.command >= 75 }
                    .map { it.id }
                    .take(4),
                relatedCityIds = city?.let { listOf(it.id) } ?: emptyList(),
                recommendedTab = 4,
                edictDraft = city?.let { "传朕旨意：枢密院即核${it.name}兵粮城防，命现有可用将领整军待命，粮道未稳不得轻进。" }.orEmpty()
            )
        }

        if (state.grain < 160000 || state.gold < 40000 || lowGrainCity != null) {
            tasks += PalaceTask(
                id = "fiscal_${state.turn}",
                palaceId = PalaceIds.ZHENGSHI,
                title = if (state.grain < 120000) "府库粮储吃紧" else "钱粮调度待议",
                description = "国库${state.gold}贯，粮草${state.grain}石。政事堂请议转运、屯田、赈济与军粮。",
                severity = if (state.grain < 120000) TaskSeverity.HIGH else TaskSeverity.MEDIUM,
                source = TaskSource.FISCAL,
                relatedOfficerIds = openingCourtIds.take(3),
                relatedCityIds = lowGrainCity?.let { listOf(it.id) } ?: emptyList(),
                recommendedTab = 3,
                edictDraft = "传朕旨意：有司核天下钱粮，先保军粮与民食，转运、屯田、赈济分轻重具奏。"
            )
        }

        WorldStrategySystem.diplomacyBriefs(state).forEachIndexed { index, brief ->
            val isTrade = brief.relatedRouteIds.any { it.contains("quanzhou") || it.contains("guangzhou") || it.contains("mingzhou") }
            tasks += PalaceTask(
                id = "foreign_${state.turn}_$index",
                palaceId = if (isTrade) PalaceIds.ZHENGSHI else PalaceIds.YUSHU,
                title = brief.title,
                description = brief.description,
                severity = brief.severity,
                source = if (isTrade) TaskSource.TRADE else TaskSource.DIPLOMACY,
                relatedCityIds = brief.relatedRouteIds.mapNotNull { routeId ->
                    when {
                        routeId.contains("quanzhou") -> "quanzhou"
                        routeId.contains("mingzhou") -> "mingzhou"
                        routeId.contains("guangzhou") -> "guangzhou"
                        routeId.contains("xixia") -> "xingqing"
                        routeId.contains("dali") -> "dali"
                        else -> null
                    }
                },
                recommendedTab = if (isTrade) 3 else 1,
                edictDraft = brief.edictDraft
            )
        }

        if (hiddenTalent != null || state.talentLeads.isNotEmpty()) {
            tasks += PalaceTask(
                id = "talent_${state.turn}_${hiddenTalent?.id ?: state.talentLeads.firstOrNull().orEmpty()}",
                palaceId = PalaceIds.WENDE,
                title = if (hiddenTalent != null) "在野人才可访" else "人才线索待召见",
                description = hiddenTalent?.let { "${it.currentCityId}一带或有${it.origin}出身之才，可议访求。" }
                    ?: "已有在野人才线索，宜召见、考校、授官。",
                severity = TaskSeverity.MEDIUM,
                source = TaskSource.TALENT,
                relatedOfficerIds = hiddenTalent?.let { listOf(it.id) } ?: state.talentLeads.take(2),
                recommendedTab = 3,
                edictDraft = hiddenTalent?.let { "传朕旨意：遣使访求${it.currentCityId}在野之才，得其人者，先召见考校，再议授官。" }
                    ?: "传朕旨意：整理在野人才线索，择其可用者召至行在考校。"
            )
        }

        if (state.rumors.isNotEmpty()) {
            val rumor = state.rumors.last()
            tasks += PalaceTask(
                id = "rumor_${state.turn}_${state.rumors.size}",
                palaceId = PalaceIds.YUSHU,
                title = "坊间传闻入密折",
                description = rumor.text.take(60),
                severity = TaskSeverity.MEDIUM,
                source = TaskSource.RUMOR,
                relatedCityIds = listOf(rumor.sourceCityId).filter { it.isNotBlank() },
                recommendedTab = 1,
                edictDraft = "传朕旨意：御前将近日传闻整理成密折，分真假缓急，凡涉军粮边防者先奏。"
            )
        }

        if (riskyOfficer != null) {
            tasks += PalaceTask(
                id = "intel_${state.turn}_${riskyOfficer.id}",
                palaceId = PalaceIds.HUANGCHENG,
                title = "朝臣动向需留意",
                description = "${riskyOfficer.name}忠诚${riskyOfficer.loyalty}、野心${riskyOfficer.ambition}。皇城司请密察其往来。",
                severity = if (riskyOfficer.loyalty <= 35) TaskSeverity.HIGH else TaskSeverity.MEDIUM,
                source = TaskSource.RUMOR,
                relatedOfficerIds = listOf(riskyOfficer.id),
                recommendedTab = 1,
                edictDraft = "传朕旨意：皇城司谨慎核察朝臣往来，不得妄兴风波，凡有实据再具密奏。"
            )
        }

        if (state.prestige < 40 || state.turn % 3 == 0) {
            tasks += PalaceTask(
                id = "ritual_${state.turn}",
                palaceId = PalaceIds.TAIMIAO,
                title = "礼制正统待议",
                description = "名望${state.prestige}，国势未稳。可议告慰祖宗、安抚军民、整肃正统。",
                severity = if (state.prestige < 30) TaskSeverity.HIGH else TaskSeverity.LOW,
                source = TaskSource.RITUAL,
                relatedOfficerIds = openingCourtIds.take(2),
                recommendedTab = 3,
                edictDraft = "传朕旨意：礼官议定告慰祖宗之礼，以安军民之心；钱粮从简，不得扰民。"
            )
        }

        // STAB-004：内廷不再按 turn % 4 机械制造“有事待闻”。
        // 只有当前世界确实存在内廷能感知的压力时才生成，且文案直接展示触发数据。
        val innerPalaceTask = when {
            state.jinThreat >= 85 -> PalaceTask(
                id = "inner_war_pressure_${state.turn}",
                palaceId = PalaceIds.HOUYUAN,
                title = "军报频至，内廷人心不安",
                description = "金军威胁已达${state.jinThreat}。行在内廷因前线急报接连入宫而人心浮动，需决定节用、安抚或听取近事。",
                severity = TaskSeverity.MEDIUM,
                source = TaskSource.PALACE,
                recommendedTab = 1,
                edictDraft = "传朕旨意：军国虽急，内廷各安其职，不得因边报自乱，更不得干预外朝军政。"
            )
            state.gold < 40000 -> PalaceTask(
                id = "inner_budget_${state.turn}",
                palaceId = PalaceIds.HOUYUAN,
                title = "行在内廷请议减省",
                description = "国库仅余${state.gold}贯。内廷请官家裁定宫中用度是否继续裁减，以免与军粮民食争用。",
                severity = TaskSeverity.MEDIUM,
                source = TaskSource.PALACE,
                recommendedTab = 1,
                edictDraft = "传朕旨意：内廷诸费从简，先保军粮与民食，不得借行在草创妄增供给。"
            )
            state.courtStability < 40 -> PalaceTask(
                id = "inner_court_unrest_${state.turn}",
                palaceId = PalaceIds.HOUYUAN,
                title = "朝局震荡波及宫中",
                description = "朝局稳定仅${state.courtStability}。外朝争议已传入宫禁，内廷请示是否安抚众人并严禁私议军政。",
                severity = TaskSeverity.MEDIUM,
                source = TaskSource.PALACE,
                recommendedTab = 1,
                edictDraft = "传朕旨意：宫中各安职守，外朝争议不得传抄煽动；确有近事可密陈朕前。"
            )
            else -> null
        }
        innerPalaceTask?.let(tasks::add)

        return tasks
            .distinctBy { it.id }
            .sortedWith(compareByDescending<PalaceTask> { it.severity.ordinal }.thenBy { it.palaceId })
            .take(12)
    }

    fun tasksForPalace(state: GameState, palaceId: String): List<PalaceTask> =
        generate(state).filter { it.palaceId == palaceId }

    fun countByPalace(state: GameState): Map<String, Int> =
        generate(state).groupingBy { it.palaceId }.eachCount()
}
