package com.xiemingxin.nandu.story

import com.xiemingxin.nandu.game.City
import com.xiemingxin.nandu.game.GameState
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * V0.7.0 剧情事件效果结算器。
 * 只处理 GameState 当前已有字段；暂未持久化的将领成长效果会进入 pendingEffects。
 */
data class StoryEventEffectResult(
    val newState: GameState,
    val outcomes: List<String>,
    val flags: Set<String>,
    val pendingEffects: Map<String, Int>
)

object StoryEventEffectApplier {
    fun applyChoice(state: GameState, event: StoryEvent, choiceId: String): StoryEventEffectResult {
        val choice = event.choices.firstOrNull { it.id == choiceId }
            ?: return StoryEventEffectResult(
                newState = state,
                outcomes = listOf("【剧情】选项不存在：$choiceId"),
                flags = emptySet(),
                pendingEffects = emptyMap()
            )

        var next = state
        val outcomes = mutableListOf<String>()
        val pending = mutableMapOf<String, Int>()

        for ((key, value) in choice.effects) {
            val amount = value.intValueOrNull() ?: continue
            when (key) {
                "gold" -> {
                    next = next.copy(gold = (next.gold + amount).coerceAtLeast(0))
                    outcomes.add(formatEffect("国库", amount))
                }
                "grain" -> {
                    next = next.copy(grain = (next.grain + amount).coerceAtLeast(0))
                    outcomes.add(formatEffect("粮草", amount))
                }
                "troopMorale" -> {
                    next = next.copy(troopMorale = (next.troopMorale + amount).coerceIn(0, 100))
                    outcomes.add(formatEffect("军心", amount))
                }
                "courtStability" -> {
                    next = next.copy(courtStability = (next.courtStability + amount).coerceIn(0, 100))
                    outcomes.add(formatEffect("朝局稳定", amount))
                }
                "jinThreat" -> {
                    next = next.copy(jinThreat = (next.jinThreat + amount).coerceIn(0, 100))
                    outcomes.add(formatEffect("金国威胁", amount))
                }
                "warFactionPower" -> {
                    next = next.copy(warFactionPower = (next.warFactionPower + amount).coerceIn(0, 100))
                    outcomes.add(formatEffect("主战派", amount))
                }
                "peaceFactionPower" -> {
                    next = next.copy(peaceFactionPower = (next.peaceFactionPower + amount).coerceIn(0, 100))
                    outcomes.add(formatEffect("主和派", amount))
                }
                "popularSupport" -> {
                    next = next.copy(cities = next.cities.map { it.adjustPopularSupport(amount) })
                    outcomes.add(formatEffect("各地民心", amount))
                }
                else -> {
                    if (key.startsWith("cityDefense_")) {
                        val cityId = key.removePrefix("cityDefense_")
                        next = next.copy(cities = next.cities.map { city ->
                            if (city.id == cityId) city.copy(defense = (city.defense + amount).coerceIn(0, 100)) else city
                        })
                        outcomes.add(formatEffect("${cityId}城防", amount))
                    } else {
                        pending[key] = amount
                    }
                }
            }
        }

        if (pending.isNotEmpty()) {
            outcomes.add("【待接入】${pending.keys.joinToString("、")} 已记录，等待将领成长/特殊系统接入。")
        }

        return StoryEventEffectResult(
            newState = next,
            outcomes = outcomes,
            flags = choice.flags.toSet(),
            pendingEffects = pending
        )
    }

    private fun City.adjustPopularSupport(amount: Int): City {
        return copy(popularSupport = (popularSupport + amount).coerceIn(0, 100))
    }

    private fun JsonElement.intValueOrNull(): Int? = runCatching { jsonPrimitive.intOrNull }.getOrNull()

    private fun formatEffect(label: String, amount: Int): String {
        val sign = if (amount >= 0) "+" else ""
        return "【剧情影响】$label $sign$amount"
    }
}
