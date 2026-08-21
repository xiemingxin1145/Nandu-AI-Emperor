package com.xiemingxin.nandu.story

import com.xiemingxin.nandu.game.AppointmentSystem
import com.xiemingxin.nandu.game.City
import com.xiemingxin.nandu.game.GameState
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * 剧情事件效果结算器。
 *
 * STAB-006：正式事件包里已经声明的基础世界效果必须真的写回 GameState。
 * 同时兼容旧 Jianyan 包的 camelCase 与 data/events 包的 snake_case；嵌套 city 效果
 * 也不能因为不是顶层整数而被静默跳过。
 *
 * 尚没有统一规则入口的人物升迁/直接增兵等效果继续进入 pendingEffects，避免剧情 JSON
 * 绕过 Appointment/Army 等正式系统直接改世界。
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

        fun applyScalar(key: String, amount: Int): Boolean {
            when (key) {
                "gold" -> {
                    next = next.copy(gold = (next.gold + amount).coerceAtLeast(0))
                    outcomes += formatEffect("国库", amount)
                }
                "grain" -> {
                    next = next.copy(grain = (next.grain + amount).coerceAtLeast(0))
                    outcomes += formatEffect("粮草", amount)
                }
                "troopMorale", "troop_morale" -> {
                    next = next.copy(troopMorale = (next.troopMorale + amount).coerceIn(0, 100))
                    outcomes += formatEffect("军心", amount)
                }
                "courtStability", "court_stability" -> {
                    next = next.copy(courtStability = (next.courtStability + amount).coerceIn(0, 100))
                    outcomes += formatEffect("朝局稳定", amount)
                }
                "jinThreat", "jin_threat" -> {
                    next = next.copy(jinThreat = (next.jinThreat + amount).coerceIn(0, 100))
                    outcomes += formatEffect("金国威胁", amount)
                }
                "warFactionPower", "war_faction_power" -> {
                    next = next.copy(warFactionPower = (next.warFactionPower + amount).coerceIn(0, 100))
                    outcomes += formatEffect("主战派", amount)
                }
                "peaceFactionPower", "peace_faction_power" -> {
                    next = next.copy(peaceFactionPower = (next.peaceFactionPower + amount).coerceIn(0, 100))
                    outcomes += formatEffect("主和派", amount)
                }
                "popularSupport", "popular_support" -> {
                    next = next.copy(cities = next.cities.map { it.adjustPopularSupport(amount) })
                    outcomes += formatEffect("各地民心", amount)
                }
                else -> {
                    if (key.startsWith("cityDefense_")) {
                        val cityId = key.removePrefix("cityDefense_")
                        next = next.copy(cities = next.cities.map { city ->
                            if (city.id == cityId) city.copy(defense = (city.defense + amount).coerceIn(0, 100)) else city
                        })
                        outcomes += formatEffect("${cityName(next, cityId)}城防", amount)
                    } else {
                        return false
                    }
                }
            }
            return true
        }

        for ((key, value) in choice.effects) {
            if (key == "city") {
                val cityEffects = value as? JsonObject
                if (cityEffects != null) {
                    val cityOutcomeStart = outcomes.size
                    next = next.copy(cities = next.cities.map { city ->
                        val changes = cityEffects[city.id] as? JsonObject ?: return@map city
                        applyCityObject(city, changes, outcomes)
                    })
                    if (outcomes.size == cityOutcomeStart && cityEffects.isNotEmpty()) {
                        outcomes += "【剧情提示】城市效果没有命中当前世界中的城池。"
                    }
                }
                continue
            }

            val amount = value.intValueOrNull() ?: continue
            if (!applyScalar(key, amount)) {
                // DELEGATION-001：人物忠诚度现在有正式入口了，不用再攒着不生效。
                if (key.endsWith("_loyalty")) {
                    val officerId = key.removeSuffix("_loyalty")
                    when (val result = AppointmentSystem.adjustLoyalty(next, officerId, amount)) {
                        is AppointmentSystem.AppointResult.Success -> {
                            next = result.newState
                            outcomes += "【剧情影响】${result.message}"
                        }
                        is AppointmentSystem.AppointResult.Failure -> {
                            // 人物不存在/已故：不写回世界状态，也不把内部 key 抛给玩家。
                            pending[key] = amount
                        }
                    }
                    continue
                }
                // Officer rank/troops and other special effects without an authoritative
                // system entry point stay pending until routed. Never silently pretend they applied.
                pending[key] = amount
            }
        }

        if (pending.isNotEmpty()) {
            // DELEGATION-001：绝不把 zong_ze_loyalty 这类内部 key 原样丢给玩家看。
            outcomes += "【剧情】部分效果仍等待正式规则接入，暂未施行（不影响本次已生效的部分）。"
        }

        return StoryEventEffectResult(
            newState = next,
            outcomes = outcomes,
            flags = choice.flags.toSet(),
            pendingEffects = pending
        )
    }

    private fun applyCityObject(city: City, changes: JsonObject, outcomes: MutableList<String>): City {
        var next = city

        fun apply(label: String, key: String, transform: (City, Int) -> City) {
            val amount = changes[key]?.intValueOrNull() ?: return
            next = transform(next, amount)
            outcomes += formatEffect("${city.name}$label", amount)
        }

        apply("钱粮", "gold") { current, amount -> current.copy(gold = (current.gold + amount).coerceAtLeast(0)) }
        apply("粮储", "grain") { current, amount -> current.copy(grain = (current.grain + amount).coerceAtLeast(0)) }
        apply("城防", "defense") { current, amount -> current.copy(defense = (current.defense + amount).coerceIn(0, 100)) }
        apply("民心", "popularSupport") { current, amount -> current.copy(popularSupport = (current.popularSupport + amount).coerceIn(0, 100)) }
        apply("民心", "popular_support") { current, amount -> current.copy(popularSupport = (current.popularSupport + amount).coerceIn(0, 100)) }
        apply("商业", "commerce") { current, amount -> current.copy(commerce = (current.commerce + amount).coerceIn(0, 100)) }

        return next
    }

    private fun City.adjustPopularSupport(amount: Int): City =
        copy(popularSupport = (popularSupport + amount).coerceIn(0, 100))

    private fun JsonElement.intValueOrNull(): Int? = runCatching { jsonPrimitive.intOrNull }.getOrNull()

    private fun cityName(state: GameState, cityId: String): String =
        state.cities.firstOrNull { it.id == cityId }?.name ?: cityId

    private fun formatEffect(label: String, amount: Int): String {
        val sign = if (amount >= 0) "+" else ""
        return "【剧情影响】$label $sign$amount"
    }
}
