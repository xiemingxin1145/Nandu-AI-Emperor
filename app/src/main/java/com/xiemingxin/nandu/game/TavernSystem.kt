package com.xiemingxin.nandu.game

import kotlin.random.Random

/**
 * V1.4.0 城中走访 / 酒楼情报系统（纯逻辑，无 Android 依赖，可单元测试 / Python 对照）。
 *
 * 设计：皇帝（或其耳目）每旬有若干「城中行动力」，可在城内酒楼、市集、书院等处走访，
 * 换取传闻情报、名望、在野人才线索。情报不是花架子——军情可提前预警金军动向，
 * 民情可提示赈灾时机，人才线索揭示 HIDDEN / WANDERING 的名将贤臣所在。
 */

/** 一条传闻情报 */
data class Rumor(
    val id: String,
    val text: String,
    val category: String,      // military军情 / civil民情 / trade商情 / talent人才 / legend奇遇
    val sourceCityId: String,
    val turn: Int,
    val talentOfficerId: String = ""  // 若为人才线索，指向被发现的 officer
)

/** 城中走访动作 */
enum class CityVisitAction(
    val label: String,
    val desc: String,
    val goldCost: Int
) {
    INQUIRE("打听消息", "在酒楼茶肆混迹市井，听三教九流闲谈", 200),
    FEAST("宴请宾客", "设宴款待名士游侠，破费但长名望、易得贤才线索", 1500),
    BEFRIEND("结交游侠", "结纳江湖豪客、镖师细作，换取边地与人才消息", 600),
    INVESTIGATE("探查传闻", "顺着风声深挖一桩传闻，多得军情商情实据", 400)
}

/** 一次走访的结算结果 */
data class VisitResult(
    val narrative: String,           // 走访见闻叙述
    val prestigeDelta: Int,          // 名望变化
    val goldDelta: Int,              // 金钱变化（含花费，负为支出）
    val rumor: Rumor? = null,        // 获得的传闻（可能为空）
    val talentLeadId: String = "",   // 发现的在野人才 officerId（可能为空）
    val success: Boolean = true      // 行动力不足等失败情形
)

object TavernSystem {

    const val MAX_ACTION_POINTS = 3

    /** 判断一城是否属于前线（邻接金占或自身为前线/争夺态） */
    private fun isFrontline(city: City, allCities: List<City>): Boolean {
        if (city.controlState == "FRONTLINE" || city.controlState == "CONTESTED") return true
        // 简化：北方/被金军威胁城（owner song 但靠近金） — 用 grain/troops 低 + 边境判断不易，
        // 这里以是否有金占城存在 + 该城驻军吃紧近似
        val anyJin = allCities.any { it.owner == "jin" }
        return anyJin && city.troops < 12000
    }

    /**
     * 结算一次城中走访。纯函数：相同 seed + 输入产出一致，便于测试。
     *
     * @param city          走访所在城
     * @param action        走访动作
     * @param cityOfficers  当前在该城、且为 HIDDEN / WANDERING 状态的可发掘人才
     * @param allCities     全部城池（用于生成军情/商情文案）
     * @param turn          当前回合（旬）
     * @param seed          随机种子
     */
    fun resolveVisit(
        city: City,
        action: CityVisitAction,
        cityOfficers: List<Officer>,
        allCities: List<City>,
        turn: Int,
        seed: Long
    ): VisitResult {
        val rng = Random(seed)
        val frontline = isFrontline(city, allCities)

        // 各动作对「类别」的权重（military/civil/trade/talent/legend）
        val weights: IntArray = when (action) {
            CityVisitAction.INQUIRE    -> intArrayOf(if (frontline) 35 else 20, 35, 20, 10, 5)
            CityVisitAction.FEAST      -> intArrayOf(10, 15, 15, 45, 15)
            CityVisitAction.BEFRIEND   -> intArrayOf(25, 15, 15, 30, 15)
            CityVisitAction.INVESTIGATE-> intArrayOf(if (frontline) 45 else 30, 20, 35, 8, 7)
        }
        val category = pickWeighted(rng, CATEGORIES, weights)

        // 名望增量：宴请最高，结交次之
        val basePrestige = when (action) {
            CityVisitAction.FEAST -> 2 + rng.nextInt(4)      // +2~5
            CityVisitAction.BEFRIEND -> 1 + rng.nextInt(3)   // +1~3
            CityVisitAction.INVESTIGATE -> rng.nextInt(2)    // +0~1
            CityVisitAction.INQUIRE -> rng.nextInt(2)        // +0~1
        }

        // 是否发现在野人才线索：与动作、是否有可发掘人才、名望挂钩
        val talentChance = when (action) {
            CityVisitAction.FEAST -> 60
            CityVisitAction.BEFRIEND -> 45
            CityVisitAction.INQUIRE -> 20
            CityVisitAction.INVESTIGATE -> 15
        }
        val foundTalent = cityOfficers.isNotEmpty() &&
            (category == "talent" || rng.nextInt(100) < talentChance)

        if (foundTalent) {
            val officer = cityOfficers[rng.nextInt(cityOfficers.size)]
            val rumor = Rumor(
                id = "rumor_talent_${officer.id}_$turn",
                text = "茶肆中有人提起：${city.name}近来藏龙卧虎，有一位${officer.origin.ifBlank { "草莽" }}出身、" +
                    "唤作「${officer.name}」者，${talentTease(officer)}。若得明主征辟，必成大器。",
                category = "talent",
                sourceCityId = city.id,
                turn = turn,
                talentOfficerId = officer.id
            )
            return VisitResult(
                narrative = "你在${city.name}${action.label}，席间偶得一桩贤才线索。",
                prestigeDelta = basePrestige + 1,
                goldDelta = -action.goldCost,
                rumor = rumor,
                talentLeadId = officer.id
            )
        }

        // 普通传闻
        val rumor = buildRumor(category, city, allCities, turn, rng)
        return VisitResult(
            narrative = "你在${city.name}${action.label}，${rumorFlavour(category)}",
            prestigeDelta = basePrestige,
            goldDelta = -action.goldCost,
            rumor = rumor
        )
    }

    private fun talentTease(o: Officer): String = when {
        o.force >= 90 -> "据说勇力绝伦，能于万军中取上将首级"
        o.strategy >= 88 -> "据说腹有韬略，论兵事如指诸掌"
        o.politics >= 85 -> "据说精于政事文书，议论时局颇有见地"
        o.command >= 88 -> "据说统驭有方，治军令行禁止"
        else -> "言谈不俗，似有真才"
    }

    private fun buildRumor(
        category: String,
        city: City,
        allCities: List<City>,
        turn: Int,
        rng: Random
    ): Rumor {
        val text = when (category) {
            "military" -> {
                val jinCities = allCities.filter { it.owner == "jin" }
                if (jinCities.isNotEmpty()) {
                    val c = jinCities[rng.nextInt(jinCities.size)]
                    "有客自北来，称金军近日于${c.name}一带集结粮草，约三两旬后或有南下之意。宜早修城备粮。"
                } else {
                    "边关传言，北地诸路渐有异动，戍卒往来频密，未知虚实，当令斥候详查。"
                }
            }
            "civil" -> {
                if (city.popularSupport < 55)
                    "市井怨声渐起，皆言${city.name}赋役偏重、米价腾贵，若不抚恤，恐生流民之患。"
                else
                    "城中父老称今岁年景尚可，惟盼朝廷少征丁口，使百姓得以休养。"
            }
            "trade" -> {
                if (city.commerce < 55)
                    "牙人言${city.name}商路不畅，盐铁绢帛皆滞销，若兴市集、通漕运，可活钱粮。"
                else
                    "有海商泊岸，称南货北销利厚，若设榷场抽税，岁入可观。"
            }
            "talent" -> "听闻近郊有隐士，韬光于山林，然机缘未至，难知其踪。可多遣人访求。"
            else -> {  // legend
                "酒客醉言，称城外古寺夜有异光，僧人讳莫如深。或是无稽之谈，或藏玄机。"
            }
        }
        return Rumor(
            id = "rumor_${category}_${city.id}_${turn}_${rng.nextInt(9999)}",
            text = text,
            category = category,
            sourceCityId = city.id,
            turn = turn
        )
    }

    private fun rumorFlavour(category: String): String = when (category) {
        "military" -> "竟探得一桩边关军情。"
        "civil" -> "听来一段市井民情。"
        "trade" -> "得了一条商路风声。"
        "talent" -> "隐约觅得一缕贤才踪迹。"
        else -> "听了一桩光怪陆离的奇闻。"
    }

    fun categoryLabel(category: String): String = when (category) {
        "military" -> "军情"
        "civil" -> "民情"
        "trade" -> "商情"
        "talent" -> "人才"
        "legend" -> "奇遇"
        else -> "传闻"
    }

    private val CATEGORIES = arrayOf("military", "civil", "trade", "talent", "legend")

    private fun pickWeighted(rng: Random, items: Array<String>, weights: IntArray): String {
        val total = weights.sum().coerceAtLeast(1)
        var r = rng.nextInt(total)
        for (i in items.indices) {
            if (r < weights[i]) return items[i]
            r -= weights[i]
        }
        return items.last()
    }
}
