package com.xiemingxin.nandu.game

import com.xiemingxin.nandu.ai.EdictCommand

/** Turns an explicit, player-written continuing order into a bounded local authorization. */
object ImperialMandatePolicy {
    private val delegationLanguage = Regex("授权|委以|委任|命.{1,8}(?:经营|镇守|统领|负责|整军)|便宜从事|奉旨而行|自行(?:募|修|调|筹)|军费.{0,8}(?:限|以内)")
    private val forbiddenAttack = Regex("(?:不得|不许|禁止|不可|毋得|不准).{0,12}(?:主动(?:进攻|交战)|开战|发动.{0,5}(?:大战|北伐)|北伐|交战)")
    private val explicitAttack = Regex("(?:准|许|允许|可以|可).{0,8}(?:主动(?:进攻|交战)|开战|北伐|攻城)")
    private val budgetPattern = Regex("(?:军费|钱粮预算|用钱|预算|经费)(?:以|不得超过|不超过|至多|最多|上限|为|在)?\\s*([零〇一二两三四五六七八九十百千万亿0-9,，]+)\\s*(?:贯|钱|以内|为限|之内)?")
    private val grainPattern = Regex("(?:粮草|粮食|军粮)(?:以|不得超过|不超过|至多|最多|上限|为|在)?\\s*([零〇一二两三四五六七八九十百千万亿0-9,，]+)\\s*(?:石|以内|为限|之内)")

    fun draft(state: GameState, edictText: String, selectedOfficerIds: Set<String> = emptySet()): ImperialMandate? {
        val text = edictText.trim()
        if (text.isBlank() || !delegationLanguage.containsMatchIn(text)) return null

        val available = state.officers.filter {
            it.status !in setOf(OfficerStatus.HIDDEN, OfficerStatus.WANDERING, OfficerStatus.SOLDIER,
                OfficerStatus.DECEASED, OfficerStatus.CAPTIVE, OfficerStatus.NOT_YET_RELEVANT)
        }
        val responsible = available.filter { text.contains(it.name) }.minByOrNull { text.indexOf(it.name) }
            ?: available.singleOrNull { it.id in selectedOfficerIds }
            ?: return null

        val autonomy = when {
            text.contains("御前亲断") || text.contains("不得自行") -> MandateAutonomyLevel.IMPERIAL_DECREE
            text.contains("便宜从事") || text.contains("自主处置") || text.contains("自行决断") -> MandateAutonomyLevel.DISCRETIONARY
            else -> MandateAutonomyLevel.BY_THE_BOOK
        }
        val allowed = buildSet {
            if (Regex("募兵|募义勇|募.{0,3}军|征募|补军|补员|招募").containsMatchIn(text)) add(MandateActionKind.RECRUIT)
            if (Regex("补给|调粮|筹粮|粮道|粮草|补粮").containsMatchIn(text)) add(MandateActionKind.RESUPPLY)
            if (Regex("修城|修防|城防|筑城|修缮|固城").containsMatchIn(text)) add(MandateActionKind.REPAIR_DEFENSE)
            if (Regex("任将|任命将|委任将|选将|任帅|换帅").containsMatchIn(text)) add(MandateActionKind.ASSIGN_COMMANDER)
            if (Regex("调兵|调军|调度.{0,5}军|调动.{0,5}军|移防|驻防|行军").containsMatchIn(text)) add(MandateActionKind.REPOSITION_ARMY)
            if (explicitAttack.containsMatchIn(text) && !forbiddenAttack.containsMatchIn(text)) add(MandateActionKind.INITIATE_BATTLE)
        }
        if (allowed.isEmpty() && autonomy != MandateAutonomyLevel.IMPERIAL_DECREE) return null

        val territory = linkedSetOf<String>()
        state.cities.filter { text.contains(it.name) }.forEach { territory += it.id }
        if (text.contains("东京")) state.cities.firstOrNull { it.id == "kaifeng" }?.let { territory += it.id }
        if (Regex("河北|北线|中原|两河").containsMatchIn(text)) {
            state.cities.filter {
                it.route.contains("河北") || it.route.contains("河东") ||
                    (it.controlState in setOf("FRONTLINE", "CONTESTED") && it.y <= 5100)
            }.forEach { territory += it.id }
        }
        if (text.contains("江淮")) state.cities.filter { it.route.contains("淮南") }.forEach { territory += it.id }
        // The minister may prepare at their real location; never teleport into an enemy-held objective.
        state.cities.firstOrNull { it.id == responsible.currentCityId && it.owner == "song" }?.let { territory += it.id }
        if (territory.isEmpty()) territory += responsible.currentCityId

        val explicitBudget = budgetPattern.find(text)?.groupValues?.getOrNull(1)?.let(::parseAmount)
        val gold = (explicitBudget ?: state.gold.coerceAtMost(10_000)).coerceAtLeast(0)
        val grain = grainPattern.find(text)?.groupValues?.getOrNull(1)?.let(::parseAmount)
            ?: state.grain.coerceAtMost((gold.toLong() * 2).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        val restrictOtherArmies = Regex("(?:不得|禁止|不许|毋得).{0,8}(?:擅调|擅动|调动).{0,8}(?:守军|军团|军队|其他)")
            .containsMatchIn(text)
        val prohibited = if (text.contains("江淮") && restrictOtherArmies) {
            state.armies.filter { army ->
                army.ownerFactionId == "song" && army.commanderId != responsible.id &&
                    state.cities.firstOrNull { it.id == army.currentCityId }?.route?.contains("淮南") == true
            }.map { it.id }.toSet()
        } else emptySet()

        return ImperialMandate(
            id = "mandate_${responsible.id}_${state.turn}_${state.imperialMandates.size + 1}",
            issuedTurn = state.turn,
            goal = text,
            responsibleOfficerId = responsible.id,
            regionCityIds = territory,
            autonomyLevel = autonomy,
            allowedActions = allowed,
            budgetGold = gold,
            budgetGrain = grain.coerceAtLeast(0),
            allowMoveOtherArmies = !restrictOtherArmies &&
                Regex("调度所属军|调度辖区|调动其他|统辖诸军|节制诸军").containsMatchIn(text),
            prohibitedArmyIds = prohibited
        )
    }

    fun describeTerritory(state: GameState, mandate: ImperialMandate): String =
        mandate.regionCityIds.mapNotNull { id -> state.cities.firstOrNull { it.id == id }?.name }
            .take(6).joinToString("、").ifBlank { "负责人当前驻地" }

    fun describeRestrictions(state: GameState, mandate: ImperialMandate): String {
        val restrictions = mutableListOf<String>()
        if (MandateActionKind.INITIATE_BATTLE !in mandate.allowedActions) restrictions += "不得主动交战"
        if (!mandate.allowMoveOtherArmies) restrictions += "不得擅调其他军团"
        mandate.prohibitedArmyIds.mapNotNull { id -> state.armies.firstOrNull { it.id == id }?.name }
            .forEach { restrictions += "不得调动$it" }
        return restrictions.joinToString("；").ifBlank { "依既定地域、预算与军规办事" }
    }

    /** A direct imperial military order withdraws conflicting automatic authority immediately. */
    fun prioritizeManualCommands(state: GameState, commands: List<EdictCommand>): Pair<GameState, List<String>> {
        val directTypes = setOf("dispatch_army", "move_army", "attack_city", "retreat_army", "change_army_commander", "disband_army")
        val affectedOfficers = commands.filter { it.type in directTypes }.mapNotNull { command ->
            state.officers.firstOrNull { it.id == command.officerId }?.id
                ?: state.armies.firstOrNull { it.id == command.officerId }?.commanderId?.takeIf { it.isNotBlank() }
        }.toSet()
        if (affectedOfficers.isEmpty()) return state to emptyList()
        val overridden = state.imperialMandates.filter { it.isActive && it.responsibleOfficerId in affectedOfficers }
        if (overridden.isEmpty()) return state to emptyList()
        val result = state.copy(imperialMandates = state.imperialMandates.map {
            if (it in overridden) it.copy(isActive = false) else it
        })
        return result to overridden.map { mandate ->
            val name = state.officers.firstOrNull { it.id == mandate.responsibleOfficerId }?.name ?: "受命之臣"
            "【御前亲断】${name}原自动授权已由皇帝亲令覆盖。"
        }
    }

    internal fun parseAmount(raw: String): Int? {
        val value = raw.replace(",", "").replace("，", "")
        value.toIntOrNull()?.let { return it }
        val digits = mapOf('零' to 0, '〇' to 0, '一' to 1, '二' to 2, '两' to 2, '三' to 3,
            '四' to 4, '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9)
        var total = 0L
        var section = 0L
        var number = 0L
        for (char in value) {
            when (char) {
                in digits.keys -> number = digits.getValue(char).toLong()
                '十', '百', '千' -> {
                    val unit = when (char) { '十' -> 10L; '百' -> 100L; else -> 1_000L }
                    section += (if (number == 0L) 1L else number) * unit
                    number = 0
                }
                '万', '亿' -> {
                    val unit = if (char == '万') 10_000L else 100_000_000L
                    total += (section + number).coerceAtLeast(1L) * unit
                    section = 0
                    number = 0
                }
                else -> return null
            }
        }
        return (total + section + number).takeIf { it in 1..Int.MAX_VALUE.toLong() }?.toInt()
    }
}
