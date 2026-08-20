package com.xiemingxin.nandu.agent

import kotlin.math.sqrt

/**
 * Lightweight Utility-AI primitive for autonomous historical characters.
 *
 * Design inspiration: public MIT Utility-AI / GOAP projects, but this implementation is
 * original Kotlin code tailored for Nandu. It deliberately has no third-party runtime
 * dependency so cheap-model / offline fallback behavior remains deterministic.
 *
 * Metrics are normalized to 0..1. The engine ranks prevalidated behavior candidates;
 * it never mutates authoritative GameState by itself.
 */
enum class UtilityCurve {
    LINEAR,
    QUADRATIC,
    SQRT,
    STEP
}

data class UtilityFactor(
    val metric: String,
    val weight: Double = 1.0,
    val curve: UtilityCurve = UtilityCurve.LINEAR,
    val invert: Boolean = false,
    /** Used by STEP: values >= threshold score 1, otherwise 0. */
    val threshold: Double = 0.5
)

data class UtilityOption<T>(
    val id: String,
    val payload: T,
    val baseScore: Double = 0.0,
    val factors: List<UtilityFactor> = emptyList(),
    /** Small continuity bonus to stop an agent changing its mind every turn. */
    val continuityBonus: Double = 0.08
)

data class RankedUtilityOption<T>(
    val option: UtilityOption<T>,
    val score: Double,
    val factorScores: Map<String, Double>
)

object UtilityDecisionEngine {

    fun <T> choose(
        metrics: Map<String, Double>,
        options: List<UtilityOption<T>>,
        currentOptionId: String? = null
    ): RankedUtilityOption<T>? = rank(metrics, options, currentOptionId).firstOrNull()

    fun <T> rank(
        metrics: Map<String, Double>,
        options: List<UtilityOption<T>>,
        currentOptionId: String? = null
    ): List<RankedUtilityOption<T>> {
        return options.map { option ->
            val details = linkedMapOf<String, Double>()
            var total = option.baseScore

            option.factors.forEachIndexed { index, factor ->
                val raw = metrics[factor.metric]?.coerceIn(0.0, 1.0) ?: 0.0
                val normalized = if (factor.invert) 1.0 - raw else raw
                val curved = applyCurve(normalized, factor)
                val contribution = curved * factor.weight
                details["${factor.metric}#$index"] = contribution
                total += contribution
            }

            if (currentOptionId != null && option.id == currentOptionId) {
                total += option.continuityBonus
                details["continuity"] = option.continuityBonus
            }

            RankedUtilityOption(
                option = option,
                score = total,
                factorScores = details
            )
        }.sortedWith(
            compareByDescending<RankedUtilityOption<T>> { it.score }
                .thenBy { it.option.id }
        )
    }

    private fun applyCurve(value: Double, factor: UtilityFactor): Double = when (factor.curve) {
        UtilityCurve.LINEAR -> value
        UtilityCurve.QUADRATIC -> value * value
        UtilityCurve.SQRT -> sqrt(value)
        UtilityCurve.STEP -> if (value >= factor.threshold.coerceIn(0.0, 1.0)) 1.0 else 0.0
    }
}
