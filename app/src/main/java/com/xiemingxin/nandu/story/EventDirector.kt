package com.xiemingxin.nandu.story

import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.Season
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * V0.6.9 剧情事件导演。
 * 先负责“每旬筛出可触发事件”，不直接改 GameRuleEngine。
 */
object EventDirector {
    fun candidates(
        state: GameState,
        events: List<StoryEvent>,
        firedEventIds: Set<String> = emptySet(),
        flags: Set<String> = emptySet()
    ): List<StoryEvent> {
        return events
            .filterNot { it.eventId in firedEventIds }
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
            normalized.contains(">") -> compare(state, normalized, ">") { a, b -> a > b }
            normalized.contains("<") -> compare(state, normalized, "<") { a, b -> a < b }
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
        "turn" -> state.turn
        "gold" -> state.gold
        "grain" -> state.grain
        "troopMorale" -> state.troopMorale
        "courtStability" -> state.courtStability
        "jinThreat" -> state.jinThreat
        "warFactionPower" -> state.warFactionPower
        "peaceFactionPower" -> state.peaceFactionPower
        "popularSupport" -> state.cities.map { it.popularSupport }.average().toInt()
        else -> null
    }

    private fun typePriority(type: String): Int = when (type) {
        "main_story" -> 0
        "jin_event" -> 1
        "city_crisis" -> 2
        "talent_discovery" -> 3
        "random_military" -> 4
        "random_court" -> 5
        "folk_rumor" -> 6
        "side_story" -> 7
        else -> 9
    }

    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNullSafe()
    private fun JsonElement.contentOrNullSafe(): String? = runCatching { jsonPrimitive.content }.getOrNull()
    @Suppress("unused")
    private fun JsonObject.boolean(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull
}
