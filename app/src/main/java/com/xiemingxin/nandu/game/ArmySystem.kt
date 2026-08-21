package com.xiemingxin.nandu.game

/**
 * Stage 4 军团管理系统
 *
 * 负责：组建、解散、换帅
 * 兵力Invariant：City.troops + sum(Army.troops) = 总兵力（不重复）
 */
object ArmySystem {

    sealed class ArmyResult {
        data class Success(val message: String, val newState: GameState) : ArmyResult()
        data class Failure(val reason: String) : ArmyResult()
    }

    private const val MIN_GARRISON = 2000  // 城池必须保留最低守军

    // ─── 组建军团 ─────────────────────────────────────────────
    fun formArmy(
        state: GameState,
        fromCityId: String,
        commanderId: String,
        troops: Int,
        armyType: String
    ): ArmyResult {
        val city = state.cities.find { it.id == fromCityId }
            ?: return ArmyResult.Failure("【组建失败】找不到出发城池：$fromCityId")
        if (city.owner != "song")
            return ArmyResult.Failure("【组建失败】${city.name}不在宋廷控制之下。")

        val commander = state.officers.find { it.id == commanderId }
            ?: return ArmyResult.Failure("【组建失败】找不到此将领。")
        if (commander.status in setOf(OfficerStatus.HIDDEN, OfficerStatus.WANDERING, OfficerStatus.SOLDIER))
            return ArmyResult.Failure("【组建失败】${commander.name}尚未入朝，无法担任统帅。须先征辟入御前。")
        if (commander.status == OfficerStatus.DECEASED)
            return ArmyResult.Failure("【组建失败】${commander.name}已殁。")

        // 检查是否已统领其他军团
        val existingCommand = state.armies.find {
            it.commanderId == commanderId &&
            it.statusCode != ArmyStatus.DISBANDED
        }
        if (existingCommand != null)
            return ArmyResult.Failure("【组建失败】${commander.name}已统领「${existingCommand.name}」，同一主帅不得重复带兵。须先解除旧职或换帅。")

        // 统兵上限
        val cmdLimit = commander.commandLimit()
        val available = city.troops - MIN_GARRISON
        if (available <= 0)
            return ArmyResult.Failure("【组建失败】${city.name}可调兵力不足（须保留守军${MIN_GARRISON}）。")

        val actualTroops = troops.coerceAtMost(available).coerceAtMost(cmdLimit)
        if (actualTroops <= 0)
            return ArmyResult.Failure("【组建失败】${commander.name}当前可统${cmdLimit}兵，城池可调${available}兵，无法组建。")

        // 扣除城池兵力
        val newCities = state.cities.map {
            if (it.id == fromCityId) it.copy(troops = it.troops - actualTroops) else it
        }
        val newOfficers = state.officers.map {
            if (it.id == commanderId) it.copy(currentCityId = fromCityId, status = OfficerStatus.DEPLOYED) else it
        }
        val armyId = "army_${commanderId}_${state.turn}"
        val newArmy = Army(
            id = armyId,
            name = "${commander.name}部",
            ownerFactionId = "song",
            commanderId = commanderId,
            homeCityId = fromCityId,
            currentCityId = fromCityId,
            troops = actualTroops,
            morale = (state.troopMorale + commander.loyalty / 10).coerceIn(40, 100),
            armyType = armyType,
            supplyCityId = fromCityId,
            statusCode = ArmyStatus.GARRISONED,
            status = ArmyStatus.GARRISONED.label,
            supplyLevel = 100,
            createdTurn = state.turn
        )
        val note = if (actualTroops < troops)
            "（原拟${troops}兵，受统兵上限/城池兵力限制，实发${actualTroops}兵）" else ""
        return ArmyResult.Success(
            "【组建】${commander.name}部于${city.name}正式成军，兵${actualTroops}，粮足，待命出征。$note",
            state.copy(cities = newCities, officers = newOfficers, armies = state.armies + newArmy)
        )
    }

    // ─── 通用募兵/补员（DELEGATION-001：支持任意势力，不局限于宋）─────────────
    /**
     * 跟 [formArmy] 的核心机制一致（城池 troops 转移给军团），但：
     * 1. 支持任意 factionId，供被授权的宋方负责人和金/西夏等 AI 势力共用；
     * 2. 若 commanderId 已经统领同势力的军团，走"补员"（troops 累加到既有军团），
     *    不强行拆分成两支番号；
     * 3. 不在这里扣钱粮——预算是否够、扣哪个"钱包"（玩家中央国库 or 该城池地方财政）
     *    由调用方（DelegatedActionValidator / FactionStrategyPlanner）先决定，
     *    这个函数只负责"人从哪来、兵到哪去"这个军事动作本身，职责单一。
     */
    fun recruitOrReinforce(
        state: GameState,
        factionId: String,
        cityId: String,
        commanderId: String,
        troops: Int,
        armyType: String
    ): ArmyResult {
        val city = state.cities.find { it.id == cityId }
            ?: return ArmyResult.Failure("【募兵失败】找不到城池：$cityId")
        if (city.owner != factionId)
            return ArmyResult.Failure("【募兵失败】${city.name}不在该势力控制之下。")

        val commander = state.officers.find { it.id == commanderId }
            ?: return ArmyResult.Failure("【募兵失败】找不到负责募兵的人物。")
        if (commander.status == OfficerStatus.DECEASED)
            return ArmyResult.Failure("【募兵失败】${commander.name}已殁。")

        val available = city.troops - MIN_GARRISON
        if (available <= 0)
            return ArmyResult.Failure("【募兵失败】${city.name}可调兵力不足（须保留守军$MIN_GARRISON）。")

        val existing = state.armies.find {
            it.commanderId == commanderId && it.ownerFactionId == factionId && it.statusCode != ArmyStatus.DISBANDED
        }

        if (existing != null) {
            // 补员：既有军团受统兵上限约束，不能无限吃兵。
            val cmdLimit = commander.commandLimit()
            val headroom = (cmdLimit - existing.troops).coerceAtLeast(0)
            val actual = troops.coerceAtMost(available).coerceAtMost(headroom)
            if (actual <= 0)
                return ArmyResult.Failure("【募兵失败】${commander.name}部已近统兵上限，无法再补员。")
            val newCities = state.cities.map { if (it.id == cityId) it.copy(troops = it.troops - actual) else it }
            val newArmies = state.armies.map { if (it.id == existing.id) it.copy(troops = it.troops + actual) else it }
            return ArmyResult.Success(
                "【募兵】${commander.name}部于${city.name}补员${actual}人。",
                state.copy(cities = newCities, armies = newArmies)
            )
        }

        // 新组建：与 formArmy 相同的统兵上限/主帅唯一性检查，只是不锁死 "song"。
        val alreadyCommanding = state.armies.find {
            it.commanderId == commanderId && it.statusCode != ArmyStatus.DISBANDED
        }
        if (alreadyCommanding != null)
            return ArmyResult.Failure("【募兵失败】${commander.name}已统领「${alreadyCommanding.name}」，同一主帅不得重复带兵。")

        val cmdLimit = commander.commandLimit()
        val actualTroops = troops.coerceAtMost(available).coerceAtMost(cmdLimit)
        if (actualTroops <= 0)
            return ArmyResult.Failure("【募兵失败】${commander.name}当前可统${cmdLimit}兵，${city.name}可调${available}兵，无法成军。")

        val newCities = state.cities.map { if (it.id == cityId) it.copy(troops = it.troops - actualTroops) else it }
        val newOfficers = state.officers.map {
            if (it.id == commanderId) it.copy(currentCityId = cityId, status = OfficerStatus.DEPLOYED) else it
        }
        val armyId = "army_${commanderId}_${state.turn}"
        val newArmy = Army(
            id = armyId,
            name = "${commander.name}部",
            ownerFactionId = factionId,
            commanderId = commanderId,
            homeCityId = cityId,
            currentCityId = cityId,
            troops = actualTroops,
            morale = (state.troopMorale + commander.loyalty / 10).coerceIn(40, 100),
            armyType = armyType,
            supplyCityId = cityId,
            statusCode = ArmyStatus.GARRISONED,
            status = ArmyStatus.GARRISONED.label,
            supplyLevel = 100,
            createdTurn = state.turn
        )
        return ArmyResult.Success(
            "【募兵】${commander.name}部于${city.name}募兵成军，兵$actualTroops。",
            state.copy(cities = newCities, officers = newOfficers, armies = state.armies + newArmy)
        )
    }

    // ─── 解散军团 ─────────────────────────────────────────────
    fun disbandArmy(state: GameState, armyId: String): ArmyResult {
        val army = state.armies.find { it.id == armyId }
            ?: return ArmyResult.Failure("【解散失败】找不到该军团。")
        if (army.statusCode == ArmyStatus.MARCHING)
            return ArmyResult.Failure("【解散失败】${army.name}正在行军，不得就地解散。须先驻扎方可解散。")
        if (army.statusCode == ArmyStatus.ENGAGEMENT_PENDING)
            return ArmyResult.Failure("【解散失败】${army.name}正处敌前待战，不得临阵解散。")

        val city = state.cities.find { it.id == army.currentCityId }
            ?: return ArmyResult.Failure("【解散失败】${army.name}所在城池不存在。")
        if (city.owner != "song")
            return ArmyResult.Failure("【解散失败】${city.name}已非我方城池，不得在此解散。")

        // 兵力归还城池
        val newCities = state.cities.map {
            if (it.id == army.currentCityId) it.copy(troops = it.troops + army.troops) else it
        }
        // 移除军团，武将回待命
        val newArmies = state.armies.filter { it.id != armyId }
        val newOfficers = state.officers.map {
            if (it.id == army.commanderId) it.copy(status = OfficerStatus.IN_COURT) else it
        }
        val commander = state.officers.find { it.id == army.commanderId }
        return ArmyResult.Success(
            "【解散】${army.name}于${city.name}奉旨解散，${army.troops}兵编入城防，${commander?.name ?: "主帅"}卸任候命。",
            state.copy(cities = newCities, armies = newArmies, officers = newOfficers)
        )
    }

    // ─── 更换主帅 ─────────────────────────────────────────────
    fun changeCommander(
        state: GameState,
        armyId: String,
        newCommanderId: String
    ): ArmyResult {
        val army = state.armies.find { it.id == armyId }
            ?: return ArmyResult.Failure("【换帅失败】找不到该军团。")
        val newCommander = state.officers.find { it.id == newCommanderId }
            ?: return ArmyResult.Failure("【换帅失败】找不到新主帅。")
        if (newCommander.status in setOf(OfficerStatus.HIDDEN, OfficerStatus.WANDERING, OfficerStatus.SOLDIER, OfficerStatus.DECEASED))
            return ArmyResult.Failure("【换帅失败】${newCommander.name}不在可用之列。")

        // 新帅不能已在统帅另一军
        val alreadyCommands = state.armies.find {
            it.commanderId == newCommanderId && it.id != armyId && it.statusCode != ArmyStatus.DISBANDED
        }
        if (alreadyCommands != null)
            return ArmyResult.Failure("【换帅失败】${newCommander.name}已统领「${alreadyCommands.name}」，不可兼领。")

        // 位置限制：新帅须在同一节点
        if (newCommander.currentCityId != army.currentCityId)
            return ArmyResult.Failure("【换帅失败】${newCommander.name}在${state.cities.find{it.id==newCommander.currentCityId}?.name?:newCommander.currentCityId}，而军团在${state.cities.find{it.id==army.currentCityId}?.name?:army.currentCityId}，须同城方可接令。")

        val oldCmdId = army.commanderId
        val oldCommander = state.officers.find { it.id == oldCmdId }
        val newArmies = state.armies.map {
            if (it.id == armyId) it.copy(
                commanderId = newCommanderId,
                name = "${newCommander.name}部"
            ) else it
        }
        val newOfficers = state.officers.map {
            when (it.id) {
                newCommanderId -> it.copy(status = OfficerStatus.DEPLOYED, currentCityId = army.currentCityId)
                oldCmdId -> it.copy(status = OfficerStatus.IN_COURT)
                else -> it
            }
        }
        return ArmyResult.Success(
            "【换帅】${oldCommander?.name ?: "原主帅"}卸任，${newCommander.name}接掌兵权，统领${army.troops}军于${state.cities.find{it.id==army.currentCityId}?.name?:army.currentCityId}。",
            state.copy(armies = newArmies, officers = newOfficers)
        )
    }

    /**
     * 校验兵力Invariant：全局总兵力 = 城池兵力 + 军团兵力（不含金方）
     * 返回 null = 通过，否则返回错误描述。
     */
    fun checkTroopInvariant(before: GameState, after: GameState): String? {
        val songBefore = before.cities.filter { it.owner == "song" }.sumOf { it.troops } +
                         before.armies.filter { it.ownerFactionId == "song" }.sumOf { it.troops }
        val songAfter  = after.cities.filter { it.owner == "song" }.sumOf { it.troops } +
                         after.armies.filter { it.ownerFactionId == "song" }.sumOf { it.troops }
        return if (songBefore != songAfter)
            "⚠ 兵力Invariant违反：操作前${songBefore}，操作后${songAfter}，差${songAfter - songBefore}"
        else null
    }
}
