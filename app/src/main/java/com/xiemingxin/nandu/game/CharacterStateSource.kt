package com.xiemingxin.nandu.game

/**
 * V1.0 统一人物状态源（活朝堂 / living-world-court-v1）。
 *
 * 背景：曾出现"岳飞一边可被征辟，一边已站在垂拱殿参加朝会"这类同一人物
 * 同时存在两种矛盾状态的问题。根源不是数据里有两份岳飞（Officer 列表本身
 * 每个 id 只有一条记录），而是不同 UI/系统各自用土办法判断"这人现在算不算
 * 在朝"，标准不一致（有的只看 status==IN_COURT，有的干脆硬编码人名列表，
 * 完全不看 status）。
 *
 * 这个文件不新增任何人物数据，只是把"在场判断"收拢成一份，所有系统
 * （朝会、招募、军务、人物详情、Agent）都应该调用这里，而不是自己写判断。
 */
object CharacterStateSource {

    /** 京城（临安）城市id，人物只有物理上在这座城，才谈得上"肉身在朝"。 */
    const val CAPITAL_CITY_ID = "linan"

    /** 允许被征辟的状态：尚未发现/在野/军中小卒。已入朝、外任、罢黜、已故都不可征辟。 */
    private val recruitableStatuses = setOf(
        OfficerStatus.HIDDEN,
        OfficerStatus.SOLDIER,
        OfficerStatus.WANDERING
    )

    fun isAlive(officer: Officer): Boolean = officer.status != OfficerStatus.DECEASED

    fun isDismissed(officer: Officer): Boolean = officer.status == OfficerStatus.DISMISSED

    fun isDeployed(officer: Officer): Boolean = officer.status == OfficerStatus.DEPLOYED

    fun isRecruitable(officer: Officer): Boolean = officer.status in recruitableStatuses

    /** 是否正在奉诏赶路（还没抵达目的地，途中不能算"在朝"）。 */
    fun isTraveling(officer: Officer): Boolean =
        officer.travelDestinationCityId != null && officer.travelArrivalTurn != null

    fun isInCapital(officer: Officer): Boolean = officer.currentCityId == CAPITAL_CITY_ID

    /**
     * 是否真的"肉身站在朝廷"，可以出现在垂拱殿等实体朝会场景。
     * 必须同时满足：status==IN_COURT、人在京城、不在赶路途中。
     * 这是判断"能不能出现在朝会列班"的唯一标准；CharacterAppearanceSystem
     * 的 canAppearInPalace 也复用这个判断，不重复造逻辑。
     */
    fun isAtCourt(officer: Officer): Boolean =
        officer.status == OfficerStatus.IN_COURT && isInCapital(officer) && !isTraveling(officer)

    /** 某人当前所统军团（若有）。army<->officer 的唯一真实关联仍是 Army.commanderId，这里只是统一取用入口。 */
    fun armyOf(state: GameState, officerId: String): Army? =
        state.armies.firstOrNull { it.commanderId == officerId }

    /** 给UI用的人物"当前动向"一句话摘要：赶路中 / 在朝 / 外任 / 未登场等。 */
    fun statusHint(state: GameState, officer: Officer): String {
        if (!isAlive(officer)) return "已故"
        if (isTraveling(officer)) {
            val destName = state.cities.find { it.id == officer.travelDestinationCityId }?.name
                ?: officer.travelDestinationCityId.orEmpty()
            val remain = ((officer.travelArrivalTurn ?: state.turn) - state.turn).coerceAtLeast(0)
            return if (remain <= 0) "已抵达$destName，候旨入朝" else "奉诏回京途中，约${remain}旬后抵达$destName"
        }
        return when (officer.status) {
            OfficerStatus.IN_COURT -> if (isInCapital(officer)) "在朝候命" else "在朝（异地记录，需核查）"
            OfficerStatus.DEPLOYED -> "外任/领军在外"
            OfficerStatus.SOLDIER -> "军中服役，未入朝籍"
            OfficerStatus.WANDERING -> "流落在野"
            OfficerStatus.HIDDEN -> "尚未被发现"
            OfficerStatus.DISMISSED -> "已罢黜"
            OfficerStatus.DECEASED -> "已故"
        }
    }
}
