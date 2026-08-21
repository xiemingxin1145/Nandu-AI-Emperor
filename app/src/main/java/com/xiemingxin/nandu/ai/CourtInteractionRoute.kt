package com.xiemingxin.nandu.ai

/**
 * 朝堂输入入口给 AI 层的轻量路由提示。
 *
 * 这是 UI -> provider 的一次性信号：问政、闲聊、下旨由玩家先选，模型不再负责猜。
 * 不写入存档，也不写进玩家圣旨正文；每次 parseEdict 只消费一次。
 */
object CourtInteractionRoute {
    private val allowed = setOf("CHAT", "CONSULT", "ORDER")

    @Volatile
    private var pendingMode: String? = null

    fun select(mode: String) {
        pendingMode = mode.uppercase().takeIf { it in allowed }
    }

    fun consume(): String? {
        val current = pendingMode
        pendingMode = null
        return current
    }

    fun clear() {
        pendingMode = null
    }
}
