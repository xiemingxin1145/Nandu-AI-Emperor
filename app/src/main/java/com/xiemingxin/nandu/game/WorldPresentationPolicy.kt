package com.xiemingxin.nandu.game

import com.xiemingxin.nandu.ai.EdictCommand
import com.xiemingxin.nandu.ai.EdictResult

/** UI-only decisions: the existing local rule engine remains the sole executor. */
data class ImperialDecision(
    val selectedOfficerIds: Set<String> = emptySet(),
    val synthesizeOpinions: Boolean = false,
    val amendmentRequested: Boolean = false
) {
    fun toggleOfficer(officerId: String): ImperialDecision = copy(
        selectedOfficerIds = if (officerId in selectedOfficerIds) {
            selectedOfficerIds - officerId
        } else {
            selectedOfficerIds + officerId
        },
        synthesizeOpinions = false
    )

    fun synthesize(result: EdictResult): ImperialDecision = copy(
        selectedOfficerIds = result.npcResponses.map { it.officerId }.toSet(),
        synthesizeOpinions = true
    )

    fun requestAmendment(): ImperialDecision = copy(amendmentRequested = true)

    fun canExecute(result: EdictResult, hasLongTermMandate: Boolean = false): Boolean =
        !result.clarificationNeeded && (result.commands.isNotEmpty() || hasLongTermMandate) &&
            (result.npcResponses.isEmpty() || selectedOfficerIds.isNotEmpty())
}

enum class WorldTurnActionKind(val label: String) {
    MARCH("行军"),
    ATTACK("交战"),
    RESUPPLY("补给"),
    RETREAT("退兵"),
    CITY_CAPTURE("城池易手"),
    ARMY_DESTROYED("军团覆灭"),
    RECRUIT("募兵"),
    REPAIR_DEFENSE("修缮城防"),
    ASSIGN_COMMANDER("任将")
}

data class WorldTurnAction(
    val kind: WorldTurnActionKind,
    val factionId: String,
    val factionName: String,
    val armyName: String = "",
    val originCityId: String = "",
    val targetCityId: String = "",
    val originCityName: String = "",
    val targetCityName: String = "",
    val routeNodeIds: List<String> = emptyList(),
    val detail: String
)

data class SeasonalTransition(
    val from: Season,
    val to: Season,
    val videoPath: String,
    val fallbackImagePath: String
)

data class WorldTurnReplay(
    val turn: Int,
    val dateLabel: String,
    val actions: List<WorldTurnAction>,
    val reports: List<String>,
    val seasonalTransition: SeasonalTransition? = null
)

object WorldPresentationPolicy {
    /**
     * 天下纪要属于皇帝视角，不是开发控制台。
     * 网络异常、JSON、HTTP、DNS、类名、内部 fallback 标签都不允许泄漏到玩家面前。
     */
    fun humanizeReport(state: GameState, report: String): String {
        val trimmed = report.trim()
        if (looksLikeTechnicalAiFailure(trimmed)) {
            return "驿报一度受阻，枢密院已依既定军政方略继续处置，本旬天下推演未中断。"
        }

        val names = buildMap {
            state.officers.forEach { put(it.id, it.name) }
            state.cities.forEach { put(it.id, it.name) }
            state.armies.forEach { put(it.id, it.name) }
            state.factions.forEach { put(it.id, it.name) }
        }
        var readable = trimmed
            .replace("【AI世界推演】", "【枢密院议定】")
            .replace("【本地战略脑】", "【枢密院议定】")

        names.entries.sortedByDescending { it.key.length }.forEach { (id, name) ->
            readable = readable.replace(Regex("(?<![A-Za-z0-9_])${Regex.escape(id)}(?![A-Za-z0-9_])"), name)
        }
        return readable.replace(Regex("\\b[a-z][a-z0-9]*(?:_[a-z0-9]+)+\\b"), "相关事项")
    }

    private fun looksLikeTechnicalAiFailure(report: String): Boolean {
        val lower = report.lowercase()
        return report.startsWith("【AI自动降级】") ||
            lower.contains("unable to resolve host") ||
            lower.contains("unknownhost") ||
            lower.contains("expected start of the object") ||
            lower.contains("expected eof after parsing") ||
            lower.contains("unexpected json token") ||
            lower.contains("serialization") ||
            lower.contains("json input:") ||
            lower.contains("http 4") ||
            lower.contains("http 5") ||
            lower.contains("api错误") ||
            lower.contains("chat/completions")
    }

    fun seasonalTransition(before: GameState, after: GameState): SeasonalTransition? {
        if (before.season == after.season) return null
        val id = when (after.season) {
            Season.SPRING -> "spring"
            Season.SUMMER -> "summer"
            Season.AUTUMN -> "autumn"
            Season.WINTER -> "winter"
        }
        val clip = VideoResourceRegistry.find(id) ?: return null
        return SeasonalTransition(before.season, after.season, clip.path, "ui_textures/season_${id}_bg.webp")
    }

    /** Every marker is derived from actual before/after state, never from an AI proposal. */
    fun replay(before: GameState, after: GameState, reports: List<String> = emptyList()): WorldTurnReplay {
        val beforeCities = before.cities.associateBy { it.id }
        val afterCities = after.cities.associateBy { it.id }
        val beforeArmies = before.armies.associateBy { it.id }
        val afterArmies = after.armies.associateBy { it.id }
        val factionNames = (before.factions + after.factions).associate { it.id to it.name }
        val newMandateRecords = after.mandateExecutionLog.drop(before.mandateExecutionLog.size)
        val actions = mutableListOf<WorldTurnAction>()

        fun cityName(id: String): String =
            afterCities[id]?.name ?: beforeCities[id]?.name ?: MapData.nodeMap[id]?.name ?: "当地"

        before.armies.forEach { previous ->
            val current = afterArmies[previous.id]
            val faction = factionNames[previous.ownerFactionId] ?: "所属军府"
            if (current == null || current.statusCode == ArmyStatus.DISBANDED) {
                if (previous.statusCode != ArmyStatus.DISBANDED) {
                    actions += WorldTurnAction(
                        WorldTurnActionKind.ARMY_DESTROYED, previous.ownerFactionId, faction,
                        armyName = previous.name,
                        originCityId = previous.currentCityId,
                        originCityName = cityName(previous.currentCityId),
                        detail = "${previous.name}于${cityName(previous.currentCityId)}失去建制"
                    )
                }
                return@forEach
            }

            if (current.currentCityId != previous.currentCityId) {
                val retreat = previous.statusCode == ArmyStatus.ENGAGEMENT_PENDING &&
                    current.statusCode == ArmyStatus.GARRISONED
                val kind = if (retreat) WorldTurnActionKind.RETREAT else WorldTurnActionKind.MARCH
                actions += WorldTurnAction(
                    kind, current.ownerFactionId, faction,
                    armyName = current.name,
                    originCityId = previous.currentCityId,
                    targetCityId = current.currentCityId,
                    originCityName = cityName(previous.currentCityId),
                    targetCityName = cityName(current.currentCityId),
                    routeNodeIds = actualRoute(previous, current),
                    detail = "${current.name}${kind.label}：${cityName(previous.currentCityId)} → ${cityName(current.currentCityId)}"
                )
            }

            if (current.lastBattleTurn != previous.lastBattleTurn && current.lastBattleTurn >= before.turn) {
                val target = current.targetCityId.ifBlank { current.currentCityId }
                actions += WorldTurnAction(
                    WorldTurnActionKind.ATTACK, current.ownerFactionId, faction,
                    armyName = current.name,
                    targetCityId = target,
                    targetCityName = cityName(target),
                    detail = "${current.name}于${cityName(target)}交战"
                )
            }

            if (current.supplyLevel > previous.supplyLevel && current.lastSuppliedTurn != previous.lastSuppliedTurn) {
                actions += WorldTurnAction(
                    WorldTurnActionKind.RESUPPLY, current.ownerFactionId, faction,
                    armyName = current.name,
                    targetCityId = current.currentCityId,
                    targetCityName = cityName(current.currentCityId),
                    detail = "${current.name}于${cityName(current.currentCityId)}完成补给"
                )
            }

            if (current.commanderId != previous.commanderId && current.commanderId.isNotBlank()) {
                val commander = after.officers.firstOrNull { it.id == current.commanderId }?.name ?: "新任主帅"
                val record = newMandateRecords.firstOrNull {
                    it.success && it.actionKind == MandateActionKind.ASSIGN_COMMANDER &&
                        it.responsibleOfficerId == current.commanderId
                }
                actions += WorldTurnAction(
                    WorldTurnActionKind.ASSIGN_COMMANDER, current.ownerFactionId, faction,
                    armyName = current.name, targetCityId = current.currentCityId,
                    targetCityName = cityName(current.currentCityId),
                    detail = record?.description?.let { humanizeReport(after, it) }
                        ?: "${commander}于${cityName(current.currentCityId)}接掌${current.name}"
                )
            }
        }

        after.armies.forEach { army ->
            val previous = beforeArmies[army.id]
            val gained = army.troops - (previous?.troops ?: 0)
            if (gained <= 0 || army.statusCode == ArmyStatus.DISBANDED) return@forEach
            val cityBefore = beforeCities[army.currentCityId] ?: return@forEach
            val cityAfter = afterCities[army.currentCityId] ?: return@forEach
            if (cityAfter.troops >= cityBefore.troops && cityAfter.population >= cityBefore.population) return@forEach
            val record = newMandateRecords.firstOrNull {
                it.success && it.actionKind == MandateActionKind.RECRUIT &&
                    it.responsibleOfficerId == army.commanderId
            }
            val faction = factionNames[army.ownerFactionId] ?: "所属军府"
            actions += WorldTurnAction(
                WorldTurnActionKind.RECRUIT, army.ownerFactionId, faction,
                armyName = army.name, targetCityId = army.currentCityId,
                targetCityName = cityAfter.name,
                detail = record?.description?.let { humanizeReport(after, it) }
                    ?: "${army.name}于${cityAfter.name}实际募兵${gained}人"
            )
        }

        after.cities.forEach { city ->
            val previous = beforeCities[city.id] ?: return@forEach
            if (previous.owner != city.owner) {
                val faction = factionNames[city.owner] ?: "所属势力"
                actions += WorldTurnAction(
                    WorldTurnActionKind.CITY_CAPTURE, city.owner, faction,
                    targetCityId = city.id,
                    targetCityName = city.name,
                    detail = "${city.name}易手，现归${faction}控制"
                )
            }
            if (city.owner == previous.owner && city.defense > previous.defense &&
                (after.gold < before.gold || city.gold < previous.gold)) {
                val faction = factionNames[city.owner] ?: "所属势力"
                val record = newMandateRecords.firstOrNull {
                    it.success && it.actionKind == MandateActionKind.REPAIR_DEFENSE &&
                        after.officers.firstOrNull { officer -> officer.id == it.responsibleOfficerId }?.currentCityId == city.id
                }
                actions += WorldTurnAction(
                    WorldTurnActionKind.REPAIR_DEFENSE, city.owner, faction,
                    targetCityId = city.id, targetCityName = city.name,
                    detail = record?.description?.let { humanizeReport(after, it) }
                        ?: "${city.name}实际加固城防${city.defense - previous.defense}点"
                )
            }
        }

        return WorldTurnReplay(after.turn, after.calendar.displayText(), actions, reports, seasonalTransition(before, after))
    }

    private fun actualRoute(before: Army, after: Army): List<String> {
        val route = after.routeNodeIds.ifEmpty { before.routeNodeIds }
        val start = route.indexOf(before.currentCityId)
        val end = route.indexOf(after.currentCityId)
        val candidates = when {
            start >= 0 && end >= start -> route.subList(start, end + 1)
            after.currentCityId in MapData.neighborsOf(before.currentCityId) ->
                listOf(before.currentCityId, after.currentCityId)
            else -> emptyList()
        }
        return candidates.takeIf { nodes ->
            nodes.size >= 2 && nodes.zipWithNext().all { (from, to) -> to in MapData.neighborsOf(from) }
        }.orEmpty()
    }

    fun commandDescription(state: GameState, command: EdictCommand): String {
        val officer = state.officers.firstOrNull { it.id == command.officerId }?.name
        val from = state.cities.firstOrNull { it.id == command.fromCityId }?.name
        val to = state.cities.firstOrNull { it.id == command.toCityId }?.name
        val city = state.cities.firstOrNull { it.id == command.cityId }?.name
        val army = state.armies.firstOrNull { it.id == command.officerId }?.name
        val action = when (command.type) {
            "dispatch_army", "move_army" -> "调兵"
            "assign_officer", "appoint_governor", "appoint_garrison", "change_army_commander" -> "任命"
            "repair_city" -> "修缮城防"
            "raise_grain" -> "筹措粮草"
            "suppress_officer" -> "处置朝议"
            "reward_officer" -> "赏赐"
            "punish_officer", "dismiss_officer" -> "惩处免职"
            "attack_city" -> "进兵攻城"
            "retreat_army" -> "撤军"
            "form_army" -> "组建军团"
            "disband_army" -> "整编军团"
            "resupply_army" -> "补充军粮"
            "transfer_officer" -> "调任"
            "recruit_officer" -> "征辟人才"
            "move_capital" -> "迁移行在"
            else -> "军政处置"
        }
        return listOfNotNull(
            action,
            officer ?: army,
            city,
            if (from != null && to != null) "$from → $to" else from ?: to,
            command.troops.takeIf { it > 0 }?.let { "${it}兵" },
            command.amount.takeIf { it > 0 }?.let { "预算${it}" }
        ).joinToString(" · ")
    }
}
