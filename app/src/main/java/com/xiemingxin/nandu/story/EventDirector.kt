package com.xiemingxin.nandu.story

import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.OfficerStatus
import com.xiemingxin.nandu.game.Season
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * 剧情事件导演。
 *
 * STAB-006 原则：JSON 中声明的触发条件必须真正执行。历史文本可以提供候选剧情，
 * 但不得因为导演忽略 required_npc_alive / city_owner / blocked_flags / trigger_event 等字段，
 * 让已经死亡的人、已经易手的城市或没有发生过的前置事件继续按史书硬播。
 */
object EventDirector {

    fun candidates(
        state: GameState,
        events: List<StoryEvent>,
        firedEventIds: Set<String> = emptySet(),
        flags: Set<String> = emptySet()
    ): List<StoryEvent> {
        val activeFlags = flags + state.storyFlags
        return events
            .filter { it.repeatable || it.eventId !in firedEventIds }
            .filter { matchesTurn(state, it.trigger) }
            .filter { matchesRequiredCity(state, it.trigger) }
            .filter { matchesDeclaredGuards(state, it.trigger, activeFlags) }
            .filter { matchesCondition(state, it.trigger, activeFlags) }
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

    /**
     * 每旬从多个事件池中按权重随机选出最多 [maxPerTurn] 个事件。
     * event.trigger.random_chance 在进入事件池前先执行；类型级概率随后执行。
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
            .filter { matchesRandomChance(it.trigger, rng) }
        if (pool.isEmpty()) return emptyList()

        val highPriorityTypes = setOf("main_story", "jin_event", "city_crisis")
        val mainPool     = pool.filter { it.type in highPriorityTypes }
        val courtPool    = pool.filter { it.type == "random_court" }
        val militaryPool = pool.filter { it.type == "random_military" }
        val talentPool   = pool.filter { it.type == "talent_discovery" }
        val rumorPool    = pool.filter { it.type == "folk_rumor" }
        val sidePool     = pool.filter { it.type == "side_story" || it.type == "diplomacy_event" }

        val selected = mutableListOf<StoryEvent>()
        mainPool.firstOrNull()?.let { selected += it }
        weightedPick(courtPool, state, rng)?.let { if (it !in selected) selected += it }
        weightedPick(militaryPool, state, rng)?.let { if (it !in selected) selected += it }

        if (talentPool.isNotEmpty() && rng.nextInt(100) < 30) {
            weightedPick(talentPool, state, rng)?.let { if (it !in selected) selected += it }
        }
        if (rumorPool.isNotEmpty() && rng.nextInt(100) < 40) {
            weightedPick(rumorPool, state, rng)?.let { if (it !in selected) selected += it }
        }
        if (sidePool.isNotEmpty() && rng.nextInt(100) < 20) {
            weightedPick(sidePool, state, rng)?.let { if (it !in selected) selected += it }
        }

        return selected.distinctBy { it.eventId }.take(maxPerTurn)
    }

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

    private fun matchesTurn(state: GameState, trigger: JsonObject): Boolean {
        val min = trigger.int("turn_min") ?: Int.MIN_VALUE
        val max = trigger.int("turn_max") ?: Int.MAX_VALUE
        return state.turn in min..max
    }

    private fun matchesRequiredCity(state: GameState, trigger: JsonObject): Boolean {
        val required = trigger.string("required_city") ?: return true
        return state.cities.any { it.id == required }
    }

    /** Execute every structured guard currently present in the shipped event packs. */
    private fun matchesDeclaredGuards(state: GameState, trigger: JsonObject, flags: Set<String>): Boolean {
        val requiredNpcAlive = trigger.string("required_npc_alive")
        if (requiredNpcAlive != null) {
            val officer = state.officers.firstOrNull { it.id == requiredNpcAlive } ?: return false
            if (officer.status == OfficerStatus.DECEASED) return false
        }

        val triggerEvent = trigger.string("trigger_event")
        if (triggerEvent != null && triggerEvent !in flags) return false

        val blockedFlags = trigger.strings("blocked_flags")
        if (blockedFlags.any { it in flags }) return false

        val requiredFlags = trigger.strings("required_flags")
        if (requiredFlags.any { it !in flags }) return false

        trigger.objectValue("city_owner")?.forEach { (cityId, ownerValue) ->
            val owner = runCatching { ownerValue.jsonPrimitive.content }.getOrNull() ?: return false
            if (state.cities.none { it.id == cityId && it.owner == owner }) return false
        }

        val requiredCityOwner = trigger.string("required_city_owner")
        val requiredCity = trigger.string("required_city")
        if (requiredCityOwner != null && requiredCity != null) {
            if (state.cities.none { it.id == requiredCity && it.owner == requiredCityOwner }) return false
        }

        if (!atLeast(state.jinThreat, trigger.int("jin_threat_gte"))) return false
        if (!atMost(state.jinThreat, trigger.int("jin_threat_lte"))) return false
        if (!atLeast(state.courtStability, trigger.int("court_stability_gte"))) return false
        if (!atMost(state.courtStability, trigger.int("court_stability_lte"))) return false
        if (!atLeast(state.gold, trigger.int("gold_gte"))) return false
        if (!atMost(state.gold, trigger.int("gold_lte"))) return false
        if (!atLeast(state.grain, trigger.int("grain_gte"))) return false
        if (!atMost(state.grain, trigger.int("grain_lte"))) return false
        if (!atLeast(state.warFactionPower, trigger.int("war_faction_power_gte"))) return false
        if (!atLeast(state.peaceFactionPower, trigger.int("peace_faction_power_gte"))) return false

        return true
    }

    private fun matchesRandomChance(trigger: JsonObject, rng: kotlin.random.Random): Boolean {
        val chance = trigger.double("random_chance") ?: return true
        return rng.nextDouble() < chance.coerceIn(0.0, 1.0)
    }

    private fun atLeast(actual: Int, minimum: Int?): Boolean = minimum == null || actual >= minimum
    private fun atMost(actual: Int, maximum: Int?): Boolean = maximum == null || actual <= maximum

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
        "turn"              -> state.turn
        "gold"              -> state.gold
        "grain"             -> state.grain
        "troopMorale"       -> state.troopMorale
        "courtStability"    -> state.courtStability
        "jinThreat"         -> state.jinThreat
        "warFactionPower"   -> state.warFactionPower
        "peaceFactionPower" -> state.peaceFactionPower
        "popularSupport"    -> state.cities.map { it.popularSupport }.takeIf { it.isNotEmpty() }?.average()?.toInt()
        else -> null
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
    private fun JsonObject.double(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull
    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNullSafe()
    private fun JsonObject.strings(key: String): List<String> =
        (this[key] as? JsonArray)?.mapNotNull { element -> runCatching { element.jsonPrimitive.content }.getOrNull() }.orEmpty()
    private fun JsonObject.objectValue(key: String): JsonObject? = this[key] as? JsonObject
    private fun JsonElement.contentOrNullSafe(): String? = runCatching { jsonPrimitive.content }.getOrNull()
    @Suppress("unused")
    private fun JsonObject.boolean(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull
}
