package com.xiemingxin.nandu.game

/**
 * 世界地图大区规划占坑。
 * 现在先不把所有城一次塞进地图，避免主图拥挤。
 * 后续按大区逐步扩展：先显示区域、再加核心城、最后接入势力与路线。
 */
data class WorldRegionPlan(
    val id: String,
    val name: String,
    val power: String,
    val phase: Int,
    val role: String,
    val coreCities: List<String>,
    val notes: String
)

object WorldRegionPlans {
    /**
     * phase 1：当前主战区，必须可玩。
     * phase 2：下一阶段扩展，先占坑。
     * phase 3：后期世界地图纵深，先规划不落地。
     */
    val all: List<WorldRegionPlan> = listOf(
        WorldRegionPlan(
            id = "song_jiangnan",
            name = "江南行在区",
            power = "南宋",
            phase = 1,
            role = "朝廷核心、钱粮根本、水军与漕运中心",
            coreCities = listOf("临安", "建康", "镇江", "平江", "明州", "绍兴"),
            notes = "当前版本核心后方。后续适合做城市经营、漕运、商业、皇宫事件。"
        ),
        WorldRegionPlan(
            id = "song_huai",
            name = "江淮防线区",
            power = "宋金争夺",
            phase = 1,
            role = "前线缓冲、淮河天险、攻防拉锯",
            coreCities = listOf("扬州", "楚州", "庐州", "寿春", "泗州", "濠州"),
            notes = "当前版本战略压力区。适合加入关隘、渡口、断粮、围城战。"
        ),
        WorldRegionPlan(
            id = "jingxiang",
            name = "荆襄山河区",
            power = "南宋",
            phase = 1,
            role = "北伐跳板、山地防线、汉水粮道",
            coreCities = listOf("襄阳", "鄂州", "江陵", "随州", "郢州"),
            notes = "当前版本第二前线。适合强化山道、关隘、步骑克制。"
        ),
        WorldRegionPlan(
            id = "jin_zhongyuan",
            name = "中原旧都区",
            power = "金国",
            phase = 1,
            role = "收复目标、东京西京、政治象征",
            coreCities = listOf("开封", "洛阳", "应天", "郑州", "许州", "陈州"),
            notes = "当前版本主要进攻目标。光复东京、西京归宋等成就都在这里。"
        ),
        WorldRegionPlan(
            id = "jin_hebei",
            name = "河北燕云区",
            power = "金国",
            phase = 2,
            role = "金国纵深、燕京屏障、骑兵腹地",
            coreCities = listOf("燕京", "中山", "真定", "河间", "大名", "云中"),
            notes = "下一阶段扩展。先作为世界地图北方纵深，不急着全接战斗。"
        ),
        WorldRegionPlan(
            id = "xixia_hexilog",
            name = "西夏河陇区",
            power = "西夏",
            phase = 2,
            role = "第三势力、贸易与边患、牵制金宋",
            coreCities = listOf("兴庆府", "灵州", "夏州", "凉州", "兰州", "会州"),
            notes = "先占坑为第三势力。后续可做联夏制金、买马、边贸、河西走廊。"
        ),
        WorldRegionPlan(
            id = "dali_yunnan",
            name = "大理西南区",
            power = "大理",
            phase = 3,
            role = "后期外交与资源区，不是前期主战场",
            coreCities = listOf("大理", "善阐", "姚州", "永昌"),
            notes = "后期世界地图扩展。前期只规划，不加入主线压力。"
        ),
        WorldRegionPlan(
            id = "tubo_west",
            name = "吐蕃西域区",
            power = "诸部",
            phase = 3,
            role = "边疆、商道、马政与外交区",
            coreCities = listOf("河湟", "西宁", "于阗", "龟兹"),
            notes = "世界地图远期边疆。可做马匹、商道、宗教与边患事件。"
        ),
        WorldRegionPlan(
            id = "sea_east",
            name = "东海海贸区",
            power = "海贸诸势力",
            phase = 3,
            role = "海贸、水军、海外收益",
            coreCities = listOf("明州", "泉州", "广州", "琉球", "高丽海路"),
            notes = "远期扩展海贸与水军。适合连接港口建筑和财政系统。"
        )
    )

    fun byPhase(phase: Int): List<WorldRegionPlan> = all.filter { it.phase == phase }
    fun byPower(power: String): List<WorldRegionPlan> = all.filter { it.power == power }
}
