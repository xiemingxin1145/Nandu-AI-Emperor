package com.xiemingxin.nandu.game

/**
 * V1.1 统一人物状态源（活朝堂 / living-world-court-v1）。
 *
 * 背景：曾出现“岳飞一边可被征辟，一边已站在朝会参加列班”这类同一人物
 * 同时存在两种矛盾状态的问题。根源不是数据里有两份岳飞，而是不同 UI/系统
 * 各自判断“这人现在算不算在朝”，标准不一致。
 *
 * 这个文件不新增任何人物数据，只把“在场判断”收拢成一份，所有系统
 * （朝会、招募、军务、人物详情、Agent）都应该调用这里，而不是自己写判断。
 *
 * 历史 Canon v1.1：正式玩法开局为建炎元年六月己未朔（约 1127-07-11），
 * 行在南京应天府，因此开局京城必须是 yingtianfu，而不是后来的临安。
 */
object CharacterStateSource {

    /** 建炎元年正式开局的行在城市 id。 */
    const val CAPITAL_CITY_ID = "yingtianfu"

    /** 允许被征辟的状态：尚未发现/在野/军中小卒。已入朝、外任、俘虏、未到时代、罢黜、已故都不可征辟。 */
    private val recruitableStatuses = setOf(
        OfficerStatus.HIDDEN,
        OfficerStatus.SOLDIER,
        OfficerStatus.WANDERING
    )

    fun isAlive(officer: Officer): Boolean = officer.status != OfficerStatus.DECEASED

    fun isDismissed(officer: Officer): Boolean = officer.status == OfficerStatus.DISMISSED

    fun isDeployed(officer: Officer): Boolean = officer.status == OfficerStatus.DEPLOYED

    fun isRecruitable(officer: Officer): Boolean = officer.status in recruitableStatuses

    /** 是否正在奉诏赶路（还没抵达目的地，途中不能算“在朝”）。 */
    fun isTraveling(officer: Officer): Boolean =
        officer.travelDestinationCityId != null && officer.travelArrivalTurn != null

    fun isInCapital(officer: Officer): Boolean = officer.currentCityId == CAPITAL_CITY_ID

    /**
     * 是否真的“肉身站在朝廷”，可以出现在实体朝会场景。
     * 必须同时满足：status==IN_COURT、人在当前行在、不在赶路途中。
     * 这是判断“能不能出现在朝会列班”的唯一标准；CharacterAppearanceSystem
     * 的 canAppearInPalace 也复用这个判断，不重复造逻辑。
     */
    fun isAtCourt(officer: Officer): Boolean =
        officer.status == OfficerStatus.IN_COURT && isInCapital(officer) && !isTraveling(officer)

    /**
     * 人物是否可进入世界 AI 上下文（WorldContextFactory / 战略脑）。
     *
     * 单一规则源：禁止在各处散写 `status != HIDDEN`。
     * 世界 AI 可以知道公开敌军、公开城池、公开战争与公开官员，
     * 但不能凭空知道尚未进入本局视野或未被发现的人物。
     *
     * - NOT_YET_RELEVANT / HIDDEN（无线索）→ 不泄露
     * - WANDERING / SOLDIER → 仅当 talentLeads 已发现时进入
     * - CAPTIVE → 可作为世界事实/外交情报（带 CAPTIVE 状态），不是宋廷可调用人物
     * - IN_COURT / IN_CAPITAL / DEPLOYED / DISMISSED → 正常进入
     * - DECEASED → 不作为可行动角色进入上下文
     */
    fun visibleToWorldAi(state: GameState, officer: Officer): Boolean {
        return when (officer.status) {
            OfficerStatus.NOT_YET_RELEVANT -> false
            OfficerStatus.HIDDEN -> state.talentLeads.contains(officer.id)
            OfficerStatus.WANDERING, OfficerStatus.SOLDIER ->
                state.talentLeads.contains(officer.id)
            OfficerStatus.DECEASED -> false
            OfficerStatus.CAPTIVE -> true
            OfficerStatus.IN_COURT,
            OfficerStatus.IN_CAPITAL,
            OfficerStatus.DEPLOYED,
            OfficerStatus.DISMISSED -> true
        }
    }

    /** 某人当前所统军团（若有）。army<->officer 的唯一真实关联仍是 Army.commanderId，这里只是统一取用入口。 */
    fun armyOf(state: GameState, officerId: String): Army? =
        state.armies.firstOrNull { it.commanderId == officerId }

    /** 给 UI 用的人物“当前动向”一句话摘要。 */
    fun statusHint(state: GameState, officer: Officer): String {
        if (!isAlive(officer)) return "已故"
        if (isTraveling(officer)) {
            val destName = state.cities.find { it.id == officer.travelDestinationCityId }?.name
                ?: officer.travelDestinationCityId.orEmpty()
            val remain = ((officer.travelArrivalTurn ?: state.turn) - state.turn).coerceAtLeast(0)
            return if (remain <= 0) "已抵达$destName，候旨入朝" else "奉诏回京途中，约${remain}旬后抵达$destName"
        }
        return when (officer.status) {
            OfficerStatus.IN_COURT -> if (isInCapital(officer)) "在行在候命" else "在朝（异地记录，需核查）"
            OfficerStatus.DEPLOYED -> "外任/领军在外"
            OfficerStatus.SOLDIER -> "军中服役，未入朝籍"
            OfficerStatus.WANDERING -> "流落在野"
            OfficerStatus.HIDDEN -> "尚未被发现"
            OfficerStatus.DISMISSED -> "已罢黜"
            OfficerStatus.DECEASED -> "已故"
            OfficerStatus.CAPTIVE -> "羁留敌营，未归"
            OfficerStatus.IN_CAPITAL -> "在行在任军职，仅军务场合出席"
            OfficerStatus.NOT_YET_RELEVANT -> "此时尚未进入本局视野"
        }
    }
}
