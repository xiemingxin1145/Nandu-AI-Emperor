package com.xiemingxin.nandu.ai

/**
 * 朝堂输入入口给 AI 层的轻量路由提示。
 *
 * 这是 UI -> provider 的一次性信号：问政、闲聊、下旨由玩家先选，模型不再负责猜。
 * 不写入存档，也不写进玩家圣旨正文；每次 OpenAI-compatible parseEdict 只消费一次。
 * 若当前 provider 并不消费该信号，10 秒后自动失效，避免切换模型时误吃上一轮模式。
 */
object CourtInteractionRoute {
    private val allowed = setOf("CHAT", "CONSULT", "ORDER")
    private const val TTL_MS = 10_000L

    @Volatile
    private var pendingMode: String? = null

    @Volatile
    private var pendingAtMs: Long = 0L

    fun select(mode: String) {
        pendingMode = mode.uppercase().takeIf { it in allowed }
        pendingAtMs = if (pendingMode == null) 0L else System.currentTimeMillis()
    }

    fun consume(): String? {
        val current = pendingMode
        val age = System.currentTimeMillis() - pendingAtMs
        pendingMode = null
        pendingAtMs = 0L
        return current?.takeIf { age in 0..TTL_MS }
    }

    fun clear() {
        pendingMode = null
        pendingAtMs = 0L
    }
}
