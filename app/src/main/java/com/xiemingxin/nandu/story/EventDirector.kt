package com.xiemingxin.nandu.story

import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.Season
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * V1.0 剧情事件导演。
 * - candidates()：筛选所有可触发事件（原有逻辑保留）
 * - selectForTurn()：按分池加权随机选出本旬事件列表
 * - chainCandidates()：连锁事件查找
 */
object EventDirector {

    // ─── 原有接口（保留，向下兼容）───────────────────────────────────────

    fun candidates(
        state: GameState,
        events: List<StoryEvent>,
        firedEventIds: Set<String> = emptySet(),
        flags: Set<String> = emptySet()
    ): List<StoryEvent> {
        return events
            .filter { it.repeatable || it.eventId !in firedEventIds }
            .filter { matchesTurn(state, it.trigger) }
            .filter { matchesRequiredCity(state, it.trigger) }
            .filter { matchesCondition(state, it.trigger, flags) }
            .sortedWith(compareBy<StoryEvent> { typePriority(it.type) }.thenBy { it.eventId })
    }

    fun firstCandidate(
        state: GameState,
        events: List<StoryEvent>,
        firedEventIds: Set<String> = emptySet(),
        flags: Set<String> = emptySet()
    ): StoryEvent? = candidates(state, events, firedEventIds, flags).firstOrNull()

    fun flagsFromChoice(event: StoryEvent, choiceId: String): Set<String> {
        return event.choices.firstOrNull { it.id == choiceId }?.flags?.toSet().orEmpty()
    }

    // ─── V1.0 新增：分池加权随机选取 ─────────────────────────────────────

    /**
     * 每旬从多个事件池中按权重随机选出最多 [maxPerTurn] 个事件。
     * 主线/金国/城池危机 → 优先级最高，直接取头部；
     * 朝堂/军事随机 → 各取1个（加权随机）；
     * 人才发现/传闻 → 概率触发。
     */
    fun selectForTurn(
        state: GameState,
        events: List<StoryEvent>,
        firedEventIds: Set<String> = emptySet(),
        flags: Set<String> = emptySet(),
        rng: kotlin.random.Random = kotlin.random.Random.Default,
        maxPerTurn: Int = 3
    ): List<StoryEvent> {
        val pool = candidates(state, events, firedEventIds, flags)
        if (pool.isEmpty()) return emptyList()

        val highPriorityTypes = setOf("main_story", "jin_event", "city_crisis")
        val mainPool     = pool.filter { it.type in highPriorityTypes }
        val courtPool    = pool.filter { it.type == "random_court" }
        val militaryPool = pool.filter { it.type == "random_military" }
        val talentPool   = pool.filter { it.type == "talent_discovery" }
        val rumorPool    = pool.filter { it.type == "folk_rumor" }
        val sidePool     = pool.filter { it.type == "side_story" || it.type == "diplomacy_event" }

        val selected = mutableListOf<StoryEvent>()

        // 主线/危机：不随机，直接取优先级最高的一个
        mainPool.firstOrNull()?.let { selected += it }

        // 朝堂：加权随机取1个
        weightedPick(courtPool, state, rng)?.let { if (it !in selected) selected += it }

        // 军事：加权随机取1个
        weightedPick(militaryPool, state, rng)?.let { if (it !in selected) selected += it }

        // 人才发现：30% 概率触发
        if (talentPool.isNotEmpty() && rng.nextInt(100) < 30) {
            weightedPick(talentPool, state, rng)?.let { if (it !in selected) selected += it }
        }

        // 野史传闻：40% 概率触发
        if (rumorPool.isNotEmpty() && rng.nextInt(100) < 40) {
            weightedPick(rumorPool, state, rng)?.let { if (it !in selected) selected += it }
        }

        // 外交/支线：20% 概率触发
        if (sidePool.isNotEmpty() && rng.nextInt(100) < 20) {
            weightedPick(sidePool, state, rng)?.let { if (it !in selected) selected += it }
        }

        return selected.distinctBy { it.eventId }.take(maxPerTurn)
    }

    /**
     * 连锁事件查找：在父事件 chain_next 中找出尚未触发的第一个后续事件。
     */
    fun chainCandidates(
        event: StoryEvent,
        allEvents: List<StoryEvent>,
        firedEventIds: Set<String>
    ): List<StoryEvent> {
        if (event.chainNext.isEmpty()) return emptyList()
        return allEvents
            .filter { it.eventId in event.chainNext }
            .filterNot { it.eventId in firedEventIds }
    }

    // ─── 权重计算 ──────────────────────────────────────────────────────────

    private fun weightedPick(
        pool: List<StoryEvent>,
        state: GameState,
        rng: kotlin.random.Random
    ): StoryEvent? {
        if (pool.isEmpty()) return null
        val weights = pool.map { dynamicWeight(it, state) }
        val total = weights.sum().coerceAtLeast(1)
        var rand = rng.nextInt(total)
        for (i in pool.indices) {
            rand -= weights[i]
            if (rand < 0) return pool[i]
        }
        return pool.last()
    }

    /**
     * 动态权重：base weight + 局势加成。
     * 局势越紧张，对应事件池权重越高，让随机更贴合当前处境。
     */
    private fun dynamicWeight(event: StoryEvent, state: GameState): Int {
        var w = event.weight.coerceAtLeast(1)
        when (event.type) {
            "jin_event"       -> if (state.jinThreat > 60) w += 40
            "random_court"    -> if (state.courtStability < 40) w += 30
            "random_military" -> if (state.troopMorale < 40) w += 30
            "folk_rumor"      -> if (state.jinThreat > 50) w += 10
            "city_crisis"     -> if (state.cities.any { it.popularSupport < 40 }) w += 30
        }
        return w
    }

    // ─── 触发条件匹配（原有逻辑）─────────────────────────────────────────

    private fun matchesTurn(state: GameState, trigger: JsonObject): Boolean {
        val min = trigger.int("turn_min") ?: Int.MIN_VALUE
        val max = trigger.int("turn_max") ?: Int.MAX_VALUE
        return state.turn in min..max
    }

    private fun matchesRequiredCity(state: GameState, trigger: JsonObject): Boolean {
        val required = trigger.string("required_city") ?: return true
        return state.cities.any { it.id == required }
    }

    private fun matchesCondition(state: GameState, trigger: JsonObject, flags: Set<String>): Boolean {
        val condition = trigger.string("condition") ?: return true
        return evaluateCondition(state, condition, flags)
    }

    private fun evaluateCondition(state: GameState, condition: String, flags: Set<String>): Boolean {
        val normalized = condition.trim()
        return when {
            normalized == "has_battle_event == true" -> "has_battle_event" in flags
            normalized == "season == summer" -> state.season == Season.SUMMER
            normalized == "season == autumn" -> state.season == Season.AUTUMN
            normalized == "season == winter" -> state.season == Season.WINTER
            normalized.contains(">=") -> compare(state, normalized, ">=") { a, b -> a >= b }
            normalized.contains("<=") -> compare(state, normalized, "<=") { a, b -> a <= b }
            normalized.contains(">")  -> compare(state, normalized, ">")  { a, b -> a > b }
            normalized.contains("<")  -> compare(state, normalized, "<")  { a, b -> a < b }
            normalized.contains("==") -> compare(state, normalized, "==") { a, b -> a == b }
            else -> normalized in flags
        }
    }

    private fun compare(state: GameState, expression: String, op: String, block: (Int, Int) -> Boolean): Boolean {
        val parts = expression.split(op, limit = 2).map { it.trim() }
        if (parts.size != 2) return false
        val left = valueOf(state, parts[0]) ?: return false
        val right = valueOf(state, parts[1]) ?: parts[1].toIntOrNull() ?: return false
        return block(left, right)
    }

    private fun valueOf(state: GameState, key: String): Int? = when (key) {
        "turn"             -> state.turn
        "gold"             -> state.gold
        "grain"            -> state.grain
        "troopMorale"      -> state.troopMorale
        "courtStability"   -> state.courtStability
        "jinThreat"        -> state.jinThreat
        "warFactionPower"  -> state.warFactionPower
        "peaceFactionPower"-> state.peaceFactionPower
        "popularSupport"   -> state.cities.map { it.popularSupport }.average().toInt()
        else               -> null
    }

    private fun typePriority(type: String): Int = when (type) {
        "main_story"       -> 0
        "jin_event"        -> 1
        "city_crisis"      -> 2
        "talent_discovery" -> 3
        "random_military"  -> 4
        "random_court"     -> 5
        "folk_rumor"       -> 6
        "side_story"       -> 7
        else               -> 9
    }

    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNullSafe()
    private fun JsonElement.contentOrNullSafe(): String? = runCatching { jsonPrimitive.content }.getOrNull()
    @Suppress("unused")
    private fun JsonObject.boolean(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull
}
