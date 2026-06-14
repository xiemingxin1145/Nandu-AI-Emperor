package com.xiemingxin.nandu.story

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

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
    @SerialName("related_cities") val relatedCities: List<String> = emptyList()
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

object StoryEventLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    const val JIANYAN_01_PATH = "story/story_events_jianyan_01.json"

    fun loadJianyan01(context: Context): List<StoryEvent> = load(context, JIANYAN_01_PATH)

    fun load(context: Context, assetPath: String): List<StoryEvent> {
        return try {
            val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            json.decodeFromString(text)
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
