package com.xiemingxin.nandu.game

object OfficerIntel {
    fun loyaltyLabel(value: Int): String = when {
        value >= 85 -> "忠直"
        value >= 65 -> "可信"
        value >= 45 -> "观望"
        value >= 25 -> "不稳"
        else -> "低信"
    }

    fun ambitionLabel(value: Int): String = when {
        value >= 80 -> "强烈进取"
        value >= 60 -> "颇有进取"
        value >= 40 -> "求进"
        value >= 20 -> "安分"
        else -> "淡泊"
    }

    fun fameLabel(value: Int): String = when {
        value >= 80 -> "名动天下"
        value >= 55 -> "名望甚高"
        value >= 30 -> "小有声望"
        value >= 12 -> "略有名气"
        else -> "无名"
    }

    fun experienceLabel(value: Int): String = when {
        value >= 80 -> "百战老成"
        value >= 55 -> "历练深厚"
        value >= 30 -> "略经阵仗"
        value >= 12 -> "经验尚浅"
        else -> "未经大用"
    }

    fun trustBrief(loyalty: Int, ambition: Int): String = when {
        loyalty < 30 && ambition > 65 -> "高度留意"
        loyalty < 45 && ambition > 55 -> "需要观察"
        loyalty > 80 && ambition < 35 -> "可托重任"
        else -> "正常观察"
    }
}
