package com.xiemingxin.nandu.game

/**
 * V0.6.2 技能权重表。
 * 先把技能效果集中管理，后续战斗、内政、党争、寻访都从这里取系数，避免散落在各处。
 */
data class SkillEffect(
    val skill: String,
    val domain: String,
    val modifier: Float,
    val description: String
)

object SkillEffects {
    private val table = listOf(
        SkillEffect("水战", "river_battle", 0.20f, "水路与江防战斗胜率提升20%"),
        SkillEffect("江防", "river_defense", 0.18f, "长江、淮水防线守备提升18%"),
        SkillEffect("山地战", "mountain_battle", 0.22f, "川陕、关隘、山地作战提升22%"),
        SkillEffect("守关", "pass_defense", 0.20f, "关隘守备与迟滞敌军提升20%"),
        SkillEffect("守城", "city_defense", 0.18f, "守城战与城防组织提升18%"),
        SkillEffect("步战", "field_battle", 0.12f, "平原步军战斗提升12%"),
        SkillEffect("骑战", "field_battle", 0.15f, "机动、追击、野战冲击提升15%"),
        SkillEffect("严军", "morale", 0.12f, "军纪、军心、溃散抗性提升12%"),
        SkillEffect("反击", "counter", 0.10f, "防守反击与逆风战提升10%"),
        SkillEffect("突袭", "ambush", 0.15f, "奇袭、夜袭、渡江突击提升15%"),
        SkillEffect("筹粮", "grain", 0.18f, "筹粮、转运、军需收益提升18%"),
        SkillEffect("安民", "popular_support", 0.15f, "民心恢复与灾后安抚提升15%"),
        SkillEffect("政务", "governance", 0.15f, "钱粮、修城、行政效率提升15%"),
        SkillEffect("军政", "army_admin", 0.12f, "整军、屯驻、编制恢复提升12%"),
        SkillEffect("整饬", "court_order", 0.10f, "朝堂秩序、军纪整肃提升10%"),
        SkillEffect("外交", "diplomacy", 0.14f, "议和、交涉、离间相关判定提升14%"),
        SkillEffect("党争", "court_intrigue", 0.20f, "结党、构陷、朝堂内斗能力提升20%"),
        SkillEffect("议和", "peace_talk", 0.16f, "和议与缓兵谈判提升16%"),
        SkillEffect("谋略", "strategy", 0.15f, "伏击、诱敌、识破敌计提升15%"),
        SkillEffect("伏击", "ambush", 0.18f, "埋伏和地形杀伤提升18%"),
        SkillEffect("统兵", "command", 0.10f, "可统兵上限和军团稳定提升10%"),
        SkillEffect("冲阵", "shock", 0.12f, "先锋突破与士气冲击提升12%"),
        SkillEffect("守备", "city_defense", 0.08f, "常规守备提升8%")
    ).associateBy { it.skill }

    fun of(skill: String): SkillEffect? = table[skill]

    fun domainModifier(skills: List<String>, domain: String): Float {
        return skills.mapNotNull { table[it] }
            .filter { it.domain == domain }
            .sumOf { (it.modifier * 100).toInt() }
            .toFloat() / 100f
    }

    fun shortSummary(skills: List<String>): String {
        val effects = skills.mapNotNull { table[it] }.take(3)
        if (effects.isEmpty()) return "暂无明确技能加成"
        return effects.joinToString("；") { "${it.skill}+${(it.modifier * 100).toInt()}%" }
    }
}
