package com.xiemingxin.nandu.story

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
data class StoryEvent(
    @SerialName("event_id") val eventId: String,
    val title: String,
    val chapter: String = "",
    val type: String = "",
    val trigger: JsonObject = JsonObject(emptyMap()),
    val description: String,
    val choices: List<StoryChoice> = emptyList(),
    @SerialName("npc_reactions") val npcReactions: List<StoryNpcReaction> = emptyList(),
    @SerialName("risk_tags") val riskTags: List<String> = emptyList(),
    @SerialName("art_hint") val artHint: String = "",
    @SerialName("related_characters") val relatedCharacters: List<String> = emptyList(),
    @SerialName("related_cities") val relatedCities: List<String> = emptyList(),
    // V1.0 随机事件扩展
    val weight: Int = 50,
    @SerialName("chain_next") val chainNext: List<String> = emptyList(),
    val repeatable: Boolean = false
)

@Serializable
data class StoryChoice(
    val id: String,
    val text: String,
    val effects: JsonObject = JsonObject(emptyMap()),
    val flags: List<String> = emptyList()
)

@Serializable
data class StoryNpcReaction(
    @SerialName("npc_id") val npcId: String,
    val stance: String = "neutral",
    val text: String
)

@Serializable
private data class NanduEventDef(
    val id: String,
    val title: String,
    val type: String = "history_event",
    val priority: Int = 50,
    val weight: Int = 50,
    val once: Boolean = true,
    val repeatable: Boolean = false,
    val trigger: JsonObject = JsonObject(emptyMap()),
    val description: String,
    val choices: List<NanduChoiceDef> = emptyList(),
    @SerialName("chain_next") val chainNext: List<String> = emptyList()
)

@Serializable
private data class NanduChoiceDef(
    val id: String,
    val text: String,
    @SerialName("ai_hint") val aiHint: String = "",
    val effects: JsonObject = JsonObject(emptyMap()),
    @SerialName("add_flags") val addFlags: List<String> = emptyList(),
    @SerialName("remove_flags") val removeFlags: List<String> = emptyList(),
    val flags: List<String> = emptyList()
)

object StoryEventLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    const val JIANYAN_01_PATH = "story/story_events_jianyan_01.json"
    const val NANDU_HISTORY_V1_PATH = "data/events/history_events_v1.json"
    const val RANDOM_EVENTS_V1_PATH = "data/events/random_events_v1.json"

    fun loadJianyan01(context: Context): List<StoryEvent> = load(context, JIANYAN_01_PATH)

    fun loadDefaultEvents(context: Context): List<StoryEvent> {
        return loadJianyan01(context) +
            loadNanduEventPack(context, NANDU_HISTORY_V1_PATH) +
            loadNanduEventPack(context, RANDOM_EVENTS_V1_PATH)
    }

    fun load(context: Context, assetPath: String): List<StoryEvent> {
        return try {
            val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            json.decodeFromString(text)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun loadNanduEventPack(context: Context, assetPath: String): List<StoryEvent> {
        return try {
            val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            json.decodeFromString<List<NanduEventDef>>(text).map { it.toStoryEvent() }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun NanduEventDef.toStoryEvent(): StoryEvent {
        return StoryEvent(
            eventId = id,
            title = title,
            chapter = "数据事件包",
            type = type,
            trigger = trigger.toCompatibleTrigger(priority),
            description = description,
            choices = choices.map { choice ->
                StoryChoice(
                    id = choice.id,
                    text = choice.text,
                    effects = choice.effects,
                    flags = (choice.flags + choice.addFlags).distinct()
                )
            },
            weight = weight,
            chainNext = chainNext,
            repeatable = repeatable
        )
    }

    private fun JsonObject.toCompatibleTrigger(priority: Int): JsonObject {
        val yearGte = int("year_gte")
        val yearLte = int("year_lte")
        val jinGte = int("jin_threat_gte")
        val jinLte = int("jin_threat_lte")
        val courtLte = int("court_stability_lte")
        val goldGte = int("gold_gte")
        val grainGte = int("grain_gte")
        val condition = when {
            jinGte != null -> "jinThreat >= $jinGte"
            jinLte != null -> "jinThreat <= $jinLte"
            courtLte != null -> "courtStability <= $courtLte"
            goldGte != null -> "gold >= $goldGte"
            grainGte != null -> "grain >= $grainGte"
            else -> null
        }
        return buildJsonObject {
            put("priority", JsonPrimitive(priority))
            yearGte?.let { put("turn_min", JsonPrimitive(yearToTurnMin(it))) }
            yearLte?.let { put("turn_max", JsonPrimitive(yearToTurnMax(it))) }
            condition?.let { put("condition", JsonPrimitive(it)) }
        }
    }

    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull

    private fun yearToTurnMin(year: Int): Int {
        val absolute = if (year >= 1000) year else 1126 + year
        return ((absolute - 1127).coerceAtLeast(0) * 36) + 1
    }

    private fun yearToTurnMax(year: Int): Int {
        val absolute = if (year >= 1000) year else 1126 + year
        return ((absolute - 1127).coerceAtLeast(0) * 36) + 36
    }
}
