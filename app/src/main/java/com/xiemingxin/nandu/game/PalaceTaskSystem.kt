package com.xiemingxin.nandu.game

/**
 * V1.5 宫殿待办系统骨架。
 *
 * 设计原则：
 * - 暂不改存档结构，先从 GameState 派生每旬待办，避免破坏旧存档。
 * - 每个宫殿都有可显示的待办和推荐处理入口。
 * - 后续版本再把 PalaceTask 持久化进 GameState，并加入完成/逾期/连锁后果。
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
    LOW("寻常"),
    MEDIUM("要务"),
    HIGH("急务"),
    URGENT("火急")
}

enum class TaskSource(val label: String) {
    COURT("朝议"),
    WAR_REPORT("军报"),
    FISCAL("钱粮"),
    TALENT("人才"),
    RUMOR("密折"),
    PALACE("内廷"),
    RITUAL("礼制")
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
        PalaceInfo(PalaceIds.CHUIGONG, "垂拱殿", "AI圣旨 / 群臣奏议", "📜", 1),
        PalaceInfo(PalaceIds.WENDE, "文德殿", "任官招贤 / 文臣事务", "🎓", 3),
        PalaceInfo(PalaceIds.SHUMI, "枢密院", "军令战报 / 调兵设防", "⚔", 4),
        PalaceInfo(PalaceIds.ZHENGSHI, "政事堂", "钱粮民心 / 财政民政", "⚖", 3),
        PalaceInfo(PalaceIds.YUSHU, "御书房", "密折起居 / 风险复盘", "🕯", 1),
        PalaceInfo(PalaceIds.HUANGCHENG, "皇城司", "侦缉暗线 / 情报验证", "🕵", 2),
        PalaceInfo(PalaceIds.HOUYUAN, "后苑内廷", "宫廷事件 / 内廷建议", "🏮", 1),
        PalaceInfo(PalaceIds.TAIMIAO, "太庙", "正统国运 / 史评功业", "🐉", 3)
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
            .maxByOrNull { it.ambition }

        tasks += PalaceTask(
            id = "court_${state.turn}",
            palaceId = PalaceIds.CHUIGONG,
            title = if (state.courtStability < 45) "主战主和争执不下" else "本旬朝议待断",
            description = if (state.courtStability < 45) {
                "朝局摇动，诸臣各执一词。可在垂拱殿下旨定调，或留中再议。"
            } else {
                "群臣列班，待陛下以圣旨定本旬军政轻重。"
            },
            severity = if (state.courtStability < 45) TaskSeverity.HIGH else TaskSeverity.MEDIUM,
            source = TaskSource.COURT,
            relatedOfficerIds = listOf("li_gang", "zhao_ding", "qin_hui"),
            recommendedTab = 1,
            edictDraft = "传朕旨意：今日朝议先定军政轻重，主战主和各陈利害，赵鼎核钱粮，李纲议边防，秦桧不得以空言误国。"
        )

        if (state.jinThreat >= 75 || weakestFront != null) {
            val city = weakestFront ?: songCities.firstOrNull()
            tasks += PalaceTask(
                id = "war_${state.turn}_${city?.id ?: "front"}",
                palaceId = PalaceIds.SHUMI,
                title = if (state.jinThreat >= 85) "金军压境，边报火急" else "江淮防线需整备",
                description = city?.let { "${it.name}防御${it.defense}、守军${it.troops}，需议调兵、修城或筹粮。" } ?: "金国威胁升高，枢密院请定防线。",
                severity = if (state.jinThreat >= 85) TaskSeverity.URGENT else TaskSeverity.HIGH,
                source = TaskSource.WAR_REPORT,
                relatedOfficerIds = listOf("yue_fei", "han_shizhong"),
                relatedCityIds = city?.let { listOf(it.id) } ?: emptyList(),
                recommendedTab = 4,
                edictDraft = city?.let { "传朕旨意：枢密院即核${it.name}兵粮城防，命可用将领整军待命，粮道未稳不得轻进。" }.orEmpty()
            )
        }

        if (state.grain < 160000 || state.gold < 40000 || lowGrainCity != null) {
            tasks += PalaceTask(
                id = "fiscal_${state.turn}",
                palaceId = PalaceIds.ZHENGSHI,
                title = if (state.grain < 120000) "府库粮储吃紧" else "钱粮调度待议",
                description = "国库${state.gold}贯，粮草${state.grain}石。政事堂请议漕运、屯田、赈济与军粮。",
                severity = if (state.grain < 120000) TaskSeverity.HIGH else TaskSeverity.MEDIUM,
                source = TaskSource.FISCAL,
                relatedOfficerIds = listOf("zhao_ding"),
                relatedCityIds = lowGrainCity?.let { listOf(it.id) } ?: emptyList(),
                recommendedTab = 3,
                edictDraft = "传朕旨意：赵鼎会同转运司核天下钱粮，先保军粮与民食，漕运、屯田、赈济分轻重具奏。"
            )
        }

        if (hiddenTalent != null || state.talentLeads.isNotEmpty()) {
            tasks += PalaceTask(
                id = "talent_${state.turn}_${hiddenTalent?.id ?: state.talentLeads.firstOrNull().orEmpty()}",
                palaceId = PalaceIds.WENDE,
                title = if (hiddenTalent != null) "在野人才可访" else "人才线索待召见",
                description = hiddenTalent?.let { "${it.currentCityId}一带或有${it.origin}出身之才，文德殿可议访求。" }
                    ?: "已有在野人才线索，宜召见、考校、授官。",
                severity = TaskSeverity.MEDIUM,
                source = TaskSource.TALENT,
                relatedOfficerIds = hiddenTalent?.let { listOf(it.id) } ?: state.talentLeads.take(2),
                recommendedTab = 3,
                edictDraft = hiddenTalent?.let { "传朕旨意：文德殿遣使访求${it.currentCityId}在野之才，得其人者，先召见考校，再议授官。" }
                    ?: "传朕旨意：文德殿整理在野人才线索，择其可用者入朝召见。"
            )
        }

        if (state.rumors.isNotEmpty()) {
            val rumor = state.rumors.last()
            tasks += PalaceTask(
                id = "rumor_${state.turn}_${state.rumors.size}",
                palaceId = PalaceIds.YUSHU,
                title = "酒楼传闻入密折",
                description = rumor.text.take(60),
                severity = TaskSeverity.MEDIUM,
                source = TaskSource.RUMOR,
                relatedCityIds = listOf(rumor.sourceCityId).filter { it.isNotBlank() },
                recommendedTab = 1,
                edictDraft = "传朕旨意：御书房将近日传闻整理成密折，分真假缓急，凡涉军粮边防者先奏。"
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
                id = "temple_${state.turn}",
                palaceId = PalaceIds.TAIMIAO,
                title = "太庙礼制待修",
                description = "名望${state.prestige}，国势未稳。可议祭告祖宗、安抚民心、整肃正统。",
                severity = if (state.prestige < 30) TaskSeverity.HIGH else TaskSeverity.LOW,
                source = TaskSource.RITUAL,
                relatedOfficerIds = listOf("li_gang", "zhao_ding"),
                recommendedTab = 3,
                edictDraft = "传朕旨意：礼官择日祭告太庙，以安军民之心；钱粮从简，不得扰民。"
            )
        }

        if (state.turn % 4 == 1) {
            tasks += PalaceTask(
                id = "inner_${state.turn}",
                palaceId = PalaceIds.HOUYUAN,
                title = "内廷有事待闻",
                description = "后苑内廷呈上宫中近况，事关皇帝声望与朝臣观感。",
                severity = TaskSeverity.LOW,
                source = TaskSource.PALACE,
                recommendedTab = 1,
                edictDraft = "传朕旨意：内廷诸事以节俭安静为先，不得干预外朝军政。"
            )
        }

        return tasks
            .distinctBy { it.id }
            .sortedWith(compareByDescending<PalaceTask> { it.severity.ordinal }.thenBy { it.palaceId })
            .take(10)
    }

    fun tasksForPalace(state: GameState, palaceId: String): List<PalaceTask> =
        generate(state).filter { it.palaceId == palaceId }

    fun countByPalace(state: GameState): Map<String, Int> =
        generate(state).groupingBy { it.palaceId }.eachCount()
}
