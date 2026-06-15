package com.xiemingxin.nandu.game

/**
 * V1.6 天下战略 / 外交外贸骨架。
 *
 * 先做纯数据与派生摘要，不改存档，不破坏已有南宋-金国主循环。
 * 下一步再把外交关系、贸易路线收益、使臣事件接进 GameState 与宫殿待办。
 */
data class WorldPower(
    val id: String,
    val name: String,
    val shortName: String,
    val rulerTitle: String,
    val stanceToSong: String,
    val relation: Int,
    val militaryThreat: Int,
    val tradeValue: Int,
    val diplomaticStatus: String,
    val strategyHint: String
)

data class TradeRoute(
    val id: String,
    val name: String,
    val fromRegion: String,
    val toRegion: String,
    val goods: List<String>,
    val profit: Int,
    val risk: Int,
    val isOpen: Boolean,
    val strategyHint: String
)

data class ForeignPolicyBrief(
    val title: String,
    val description: String,
    val relatedPowerIds: List<String>,
    val relatedRouteIds: List<String>,
    val severity: TaskSeverity,
    val edictDraft: String
)

enum class MapLayerMode(val label: String, val desc: String) {
    MILITARY("军事", "城池、军团、前线、粮道"),
    ECONOMY("经济", "钱粮、商税、漕运、城池产出"),
    DIPLOMACY("外交", "金、西夏、大理、高丽、诸海商"),
    TRADE("外贸", "海路、马市、药材、香料、边市")
}

object WorldPowerIds {
    const val SONG = "song"
    const val JIN = "jin"
    const val XIXIA = "xixia"
    const val DALI = "dali"
    const val GORYEO = "goryeo"
    const val SEA_TRADE = "sea_trade"
}

object WorldStrategySystem {
    val powers = listOf(
        WorldPower(WorldPowerIds.SONG, "大宋", "宋", "大宋皇帝", "本国", 100, 0, 85, "行在临安", "稳钱粮、修防线、择机北伐。"),
        WorldPower(WorldPowerIds.JIN, "金国", "金", "金主", "主敌", -80, 95, 35, "兵锋南压", "金国是主线压力源，军事、议和、边境事件都围绕它展开。"),
        WorldPower(WorldPowerIds.XIXIA, "西夏", "夏", "夏主", "可拉拢亦可牵制", 10, 45, 62, "河陇观望", "先做马市、边贸、买马与牵制金国，不急直接参战。"),
        WorldPower(WorldPowerIds.DALI, "大理", "理", "大理国主", "西南友好观望", 25, 18, 55, "西南通商", "大理适合提供马匹、药材、矿产与西南事件。"),
        WorldPower(WorldPowerIds.GORYEO, "高丽", "丽", "高丽王", "海东观望", 15, 22, 48, "海东使节", "高丽可作为后期海贸、使臣与金国东北牵制事件来源。"),
        WorldPower(WorldPowerIds.SEA_TRADE, "海贸诸商", "舶", "市舶诸商", "逐利而来", 35, 8, 95, "港市繁盛", "泉州、明州、广州的外贸税可成为南宋财政根本。")
    )

    val tradeRoutes = listOf(
        TradeRoute("quanzhou_spice", "泉州香料海贸", "泉州", "南海诸国", listOf("香料", "珠宝", "番货", "铜钱"), 95, 42, true, "高利润、高风险，适合政事堂和市舶司玩法。"),
        TradeRoute("mingzhou_ship", "明州船材海路", "明州", "高丽海东", listOf("船材", "海商", "铜器", "海盐"), 68, 30, true, "可补海防与水军后勤。"),
        TradeRoute("guangzhou_south", "广州南海商路", "广州", "南海诸国", listOf("香药", "象牙", "沉香", "海外商税"), 88, 50, true, "利润厚，但容易引出走私、海盗、豪商干政事件。"),
        TradeRoute("xixia_horse", "西夏马市", "川陕", "西夏河陇", listOf("战马", "皮货", "青盐"), 70, 44, false, "开马市可补骑兵，亦可能触怒金国。"),
        TradeRoute("dali_southwest", "大理西南路", "成都", "大理", listOf("马匹", "药材", "铜矿", "茶"), 58, 28, true, "适合温和外交与西南边境安抚。")
    )

    fun powerById(id: String): WorldPower? = powers.firstOrNull { it.id == id }

    fun diplomacyBriefs(state: GameState): List<ForeignPolicyBrief> {
        val briefs = mutableListOf<ForeignPolicyBrief>()
        if (state.jinThreat >= 80) {
            briefs += ForeignPolicyBrief(
                title = "金国压境，需谋外援牵制",
                description = "金国威胁已高，御书房可议西夏马市或海东使节，以分其势。",
                relatedPowerIds = listOf(WorldPowerIds.JIN, WorldPowerIds.XIXIA, WorldPowerIds.GORYEO),
                relatedRouteIds = listOf("xixia_horse", "mingzhou_ship"),
                severity = TaskSeverity.HIGH,
                edictDraft = "传朕旨意：御书房会同政事堂议西夏马市与海东使节，务求牵制金人，不得轻启边衅。"
            )
        }
        val coastCommerce = state.cities.filter { it.terrain == "coast" || it.isWaterNode }.sumOf { it.commerce }
        if (coastCommerce >= 300) {
            briefs += ForeignPolicyBrief(
                title = "东南港市可开外贸",
                description = "泉州、明州、广州商税丰厚，可设市舶、修海防、严禁豪商乱政。",
                relatedPowerIds = listOf(WorldPowerIds.SEA_TRADE, WorldPowerIds.DALI),
                relatedRouteIds = listOf("quanzhou_spice", "mingzhou_ship", "guangzhou_south"),
                severity = TaskSeverity.MEDIUM,
                edictDraft = "传朕旨意：政事堂清点泉州、明州、广州商税，市舶之利入公库，沿海诸州兼修海防。"
            )
        }
        return briefs
    }
}
