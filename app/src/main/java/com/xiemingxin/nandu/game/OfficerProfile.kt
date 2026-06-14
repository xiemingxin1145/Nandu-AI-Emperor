package com.xiemingxin.nandu.game

/**
 * V0.6.1 人物底层：身份、出身、魅力、野心、名望、经验、技能、可统兵上限。
 * 先用 deterministic profile 覆盖历史人物和早期 NPC，后续 V0.6.2 再接随机 NPC 生成器。
 */
data class OfficerProfile(
    val rank: String,
    val origin: String,
    val charm: Int,
    val ambition: Int,
    val fame: Int,
    val experience: Int,
    val skills: List<String>
)

object OfficerProfiles {
    fun of(officer: Officer): OfficerProfile {
        val base = when (officer.id) {
            "yue_fei" -> OfficerProfile("军中小卒", "军户", 72, 18, 5, 6, listOf("步战", "骑战", "严军", "反击"))
            "han_shizhong" -> OfficerProfile("低阶武官", "军户", 68, 28, 18, 24, listOf("水战", "江防", "突袭"))
            "wu_jie" -> OfficerProfile("边地武人", "军户", 58, 25, 10, 18, listOf("山地战", "守关", "防御"))
            "liu_qi" -> OfficerProfile("军中小卒", "军户", 55, 20, 4, 8, listOf("守城", "步战", "硬抗"))
            "zong_ze" -> OfficerProfile("流落未仕", "士族", 76, 12, 30, 45, listOf("守城", "军政", "安民"))
            "li_gang" -> OfficerProfile("朝臣", "士族", 80, 10, 60, 55, listOf("主战", "整饬", "朝议"))
            "zhao_ding" -> OfficerProfile("朝臣", "士族", 82, 18, 35, 40, listOf("筹粮", "安民", "政务"))
            "qin_hui" -> OfficerProfile("寒门文士", "寒门", 84, 86, 8, 20, listOf("外交", "党争", "议和"))
            "zhang_jun" -> OfficerProfile("朝臣", "士族", 65, 38, 25, 30, listOf("军政", "筹粮", "整军"))
            "zhang_俊" -> OfficerProfile("低阶武官", "军户", 60, 72, 14, 22, listOf("步战", "统兵"))
            else -> fallback(officer)
        }
        return when (officer.status) {
            OfficerStatus.HIDDEN -> base.copy(rank = base.rank, fame = base.fame.coerceAtMost(8), experience = base.experience.coerceAtMost(20))
            OfficerStatus.SOLDIER -> base.copy(rank = "军中小卒", fame = base.fame.coerceAtMost(12))
            OfficerStatus.WANDERING -> base.copy(rank = "在野士人", fame = base.fame.coerceAtMost(25))
            OfficerStatus.IN_COURT -> promoteToCourt(base)
            OfficerStatus.DEPLOYED -> promoteToDeployed(base)
            OfficerStatus.DISMISSED -> base.copy(rank = "罢黜")
            OfficerStatus.DECEASED -> base.copy(rank = "已故")
        }
    }

    private fun fallback(officer: Officer): OfficerProfile {
        val seed = officer.id.hashCode() and Int.MAX_VALUE
        val origin = listOf("军户", "寒门", "士族", "豪强", "义军", "归正人")[seed % 6]
        val rank = when (officer.status) {
            OfficerStatus.SOLDIER -> "军中小卒"
            OfficerStatus.WANDERING -> "在野士人"
            OfficerStatus.HIDDEN -> "未登记人才"
            OfficerStatus.DEPLOYED -> "外任将吏"
            else -> "御前名册"
        }
        val skills = when {
            officer.force >= 75 -> listOf("步战", "冲阵")
            officer.politics >= 75 -> listOf("政务", "安民")
            officer.strategy >= 75 -> listOf("谋略", "伏击")
            else -> listOf("守备")
        }
        return OfficerProfile(
            rank = rank,
            origin = origin,
            charm = 35 + seed % 55,
            ambition = 15 + (seed / 3) % 75,
            fame = 3 + (seed / 7) % 30,
            experience = 5 + (seed / 11) % 35,
            skills = skills
        )
    }

    private fun promoteToCourt(profile: OfficerProfile): OfficerProfile {
        val rank = when (profile.rank) {
            "军中小卒" -> "新拔军校"
            "低阶武官" -> "御前武官"
            "边地武人" -> "边军将校"
            "寒门文士" -> "新进文臣"
            "流落未仕", "在野士人" -> "新召幕僚"
            else -> profile.rank
        }
        return profile.copy(rank = rank, fame = (profile.fame + 10).coerceAtMost(100))
    }

    private fun promoteToDeployed(profile: OfficerProfile): OfficerProfile {
        val rank = when (profile.rank) {
            "军中小卒", "新拔军校" -> "偏将"
            "低阶武官", "御前武官" -> "统领"
            "边地武人", "边军将校" -> "守将"
            "朝臣", "新进文臣", "新召幕僚" -> "外任官"
            else -> profile.rank
        }
        return profile.copy(rank = rank, fame = (profile.fame + 18).coerceAtMost(100), experience = (profile.experience + 10).coerceAtMost(100))
    }
}

fun Officer.profile(): OfficerProfile = OfficerProfiles.of(this)

fun Officer.commandLimit(): Int {
    val p = profile()
    val rankBase = when (p.rank) {
        "军中小卒" -> 800
        "新拔军校" -> 1800
        "低阶武官" -> 2500
        "御前武官" -> 5000
        "边地武人" -> 3500
        "边军将校" -> 7000
        "偏将" -> 12000
        "统领" -> 18000
        "守将" -> 22000
        "外任官" -> 4000
        "朝臣", "新进文臣", "新召幕僚" -> 1500
        else -> 1000
    }
    val commandBonus = command * 90
    val fameBonus = p.fame * 80
    val expBonus = p.experience * 55
    val skillBonus = when {
        p.skills.any { it in listOf("水战", "山地战", "守城", "步战", "骑战", "统兵", "严军") } -> 2500
        else -> 0
    }
    val civilianPenalty = if (p.skills.any { it in listOf("政务", "筹粮", "安民", "外交", "党争") } && force < 45) 3500 else 0
    return (rankBase + commandBonus + fameBonus + expBonus + skillBonus - civilianPenalty).coerceIn(0, 90000)
}

fun Officer.skillText(): String = profile().skills.joinToString(" / ")
