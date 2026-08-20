package com.xiemingxin.nandu.game

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

// ──────────────────────────────────────────────────────────────────────────────
// Stage 5 战斗结果（完整字段，供 UI 直接使用，不再重新计算）
// ──────────────────────────────────────────────────────────────────────────────
data class BattleOutcome(
    val battleType: String,               // FIELD 野战 / SIEGE 攻城
    val attackerFactionId: String,
    val defenderFactionId: String,
    val attackerArmyId: String,
    val targetCityId: String,
    val attackerWins: Boolean,
    val attackerLosses: Int,
    val defenderLosses: Int,
    val attackerRemaining: Int,
    val defenderRemaining: Int,
    val attackerMoraleAfter: Int,
    val defenderMoraleAfter: Int,
    val advantage: Int,                   // 0-100，攻方综合优势
    val cityCaptured: Boolean,
    val modifiers: List<String>,          // 主要影响因素列表（给UI显示）
    val report: String,                   // 完整文字战报
    // 旧字段兼容（供现有代码继续用）
    val successRate: Int = advantage,
    val newControlState: String = ""
)

// ──────────────────────────────────────────────────────────────────────────────
// Stage 5 战斗解算引擎（势力中立 + seeded确定性随机 + BattleUnitCatalog克制）
// ──────────────────────────────────────────────────────────────────────────────
object BattleResolver {

    // ─── 公共工具 ─────────────────────────────────────────────────────────────

    /** armyType → BattleUnitCatalog primaryUnit（fallback到faction默认） */
    fun resolveUnit(army: Army): BattleUnitDef? {
        if (army.primaryUnitId.isNotBlank())
            return BattleUnitCatalog.byId(army.primaryUnitId)
        // armyType 映射
        val id = when {
            army.armyType.contains("naval")    -> if (army.ownerFactionId == "jin") null else "song_navy"
            army.armyType.contains("cavalry")  -> if (army.ownerFactionId == "jin") "jin_heavy_cavalry" else "song_cavalry"
            army.armyType.contains("elite")    -> if (army.ownerFactionId == "jin") "jin_iron_pagoda" else "song_beiwei_elite"
            army.armyType.contains("mountain") -> "song_crossbowman"
            army.armyType.contains("frontier") -> if (army.ownerFactionId == "jin") "jin_infantry" else "song_infantry"
            else -> if (army.ownerFactionId == "jin") "jin_infantry" else "song_infantry"
        } ?: return null
        return BattleUnitCatalog.byId(id)
    }

    /** 补给战力系数 */
    private fun supplyFactor(supply: Int): Double = when {
        supply >= 80 -> 1.00
        supply >= 50 -> 0.88
        supply >= 25 -> 0.72
        else         -> 0.52
    }

    /** 地形对进攻方的修正（守方已含城防加成） */
    private fun terrainAttackFactor(terrain: String, armyType: String): Double = when (terrain) {
        "pass"     -> 0.72   // 关隘，攻方大幅受限
        "mountain" -> 0.82   // 山地
        "river"    -> if (armyType.contains("naval")) 1.05 else 0.90
        "coast"    -> if (armyType.contains("naval")) 1.05 else 0.92
        else       -> 1.00
    }

    /** 地形对守方的修正 */
    private fun terrainDefenseFactor(terrain: String): Double = when (terrain) {
        "pass"     -> 1.30
        "mountain" -> 1.18
        "river"    -> 1.10
        "coast"    -> 1.05
        else       -> 1.00
    }

    private fun seasonFactor(season: Season, isAttacker: Boolean): Double = when (season) {
        Season.AUTUMN -> if (isAttacker) 1.12 else 0.95
        Season.WINTER -> if (isAttacker) 0.80 else 1.05
        Season.SUMMER -> if (isAttacker) 1.02 else 1.0
        Season.SPRING -> 1.0
    }

    private fun weatherFactor(weather: WeatherType, armyType: String, isAttacker: Boolean): Double = when (weather) {
        WeatherType.RAIN  -> if (armyType.contains("cavalry")) 0.85 else 0.92
        WeatherType.STORM -> if (isAttacker) 0.72 else 0.85
        WeatherType.SNOW  -> if (isAttacker) 0.78 else 0.90
        WeatherType.FOG   -> if (isAttacker) 1.05 else 0.95   // 雾利攻
        WeatherType.WIND  -> 0.96
        WeatherType.CLEAR -> 1.0
    }

    // ─── 战力计算 ─────────────────────────────────────────────────────────────

    /** 计算军团实战战力 */
    private fun armyPower(
        army: Army,
        commander: Officer?,
        enemyUnit: BattleUnitDef?,
        terrain: String,
        season: Season,
        weather: WeatherType,
        isAttacker: Boolean,
        modifiers: MutableList<String>
    ): Double {
        var power = army.troops.toDouble()

        // 士气
        val moraleFactor = 0.6 + army.morale / 100.0 * 0.6
        power *= moraleFactor
        if (army.morale < 40) modifiers.add("${army.name}士气低迷(-)")

        // 主帅统率
        val cmd = commander?.command ?: 60
        val cmdFactor = 0.75 + cmd / 100.0 * 0.50
        power *= cmdFactor
        if (cmd >= 85) modifiers.add("${commander?.name ?: "主帅"}统率出众(+)")

        // 主帅谋略（轻度修正）
        val str = commander?.strategy ?: 55
        val strFactor = 0.95 + str / 100.0 * 0.10
        power *= strFactor

        // 补给
        val supF = supplyFactor(army.supplyLevel)
        power *= supF
        if (army.supplyLevel < 50) modifiers.add("${army.name}补给不足(-)")
        if (army.supplyLevel < 25) modifiers.add("${army.name}粮道断绝，战力大减(--)")

        // 兵种克制
        val myUnit = resolveUnit(army)
        if (myUnit != null && enemyUnit != null) {
            val cf = BattleUnitCatalog.counterFactor(myUnit, enemyUnit)
            power *= cf
            when {
                cf > 1.0 -> modifiers.add("${myUnit.name}克制${enemyUnit.name}(+25%)")
                cf < 1.0 -> modifiers.add("${myUnit.name}被${enemyUnit.name}克制(-15%)")
            }
        }

        // 地形
        val terrainF = if (isAttacker) terrainAttackFactor(terrain, army.armyType)
                       else terrainDefenseFactor(terrain)
        power *= terrainF

        // 季节
        power *= seasonFactor(season, isAttacker)

        // 天气
        power *= weatherFactor(weather, army.armyType, isAttacker)

        return power.coerceAtLeast(1.0)
    }

    // ─── 野战 ─────────────────────────────────────────────────────────────────

    /**
     * 野战解算：攻击方 Army vs 守方 Army列表
     * seed 保证相同输入相同结果
     */
    fun resolveFieldBattle(
        attackerArmy: Army,
        attackerCommander: Officer?,
        defenderArmies: List<Army>,
        defenderCommanders: Map<String, Officer?>,  // armyId → officer
        city: City,
        state: GameState,
        seed: Long
    ): BattleOutcome {
        val rng = Random(seed)
        val mods = mutableListOf<String>()

        // 防守方聚合战力
        val defenderEnemyUnit = resolveUnit(attackerArmy)   // 防守方看攻方兵种
        val attackerEnemyUnit = defenderArmies.mapNotNull { resolveUnit(it) }.maxByOrNull { it.attack }

        val attackPowerBase = armyPower(attackerArmy, attackerCommander,
            attackerEnemyUnit, city.terrain, state.season, state.weather, true, mods)

        var defendPowerBase = 0.0
        defenderArmies.forEach { da ->
            val dc = defenderCommanders[da.id]
            defendPowerBase += armyPower(da, dc, defenderEnemyUnit,
                city.terrain, state.season, state.weather, false, mods)
        }
        if (defendPowerBase < 1.0) defendPowerBase = 1.0

        // ±8% 随机波动
        val atkPower = attackPowerBase * (0.92 + rng.nextDouble() * 0.16)
        val defPower = defendPowerBase * (0.92 + rng.nextDouble() * 0.16)

        val total = atkPower + defPower
        val atkRatio = atkPower / total
        val advantage = (atkRatio * 100).roundToInt().coerceIn(5, 95)
        val attackerWins = rng.nextDouble() < atkRatio

        // 伤亡计算
        val intensity = 1.0 - abs(atkRatio - 0.5) * 2.0  // 势均力敌伤亡大
        val atkLossRate = if (attackerWins) (0.06 + intensity * 0.14) else (0.12 + intensity * 0.22)
        val defLossRate = if (attackerWins) (0.15 + intensity * 0.22) else (0.06 + intensity * 0.10)

        val atkLoss = (attackerArmy.troops * atkLossRate * (0.85 + rng.nextDouble() * 0.30)).roundToInt()
            .coerceIn(0, attackerArmy.troops)
        val totalDefTroops = defenderArmies.sumOf { it.troops }.coerceAtLeast(1)
        val defLoss = (totalDefTroops * defLossRate * (0.85 + rng.nextDouble() * 0.30)).roundToInt()
            .coerceIn(0, totalDefTroops)

        val atkRemain = attackerArmy.troops - atkLoss
        val defRemain = totalDefTroops - defLoss
        val atkMorale = (attackerArmy.morale + (if (attackerWins) 8 else -12)).coerceIn(0, 100)
        val avgDefMorale = defenderArmies.map { it.morale }.average().roundToInt()
        val defMorale = (avgDefMorale + (if (attackerWins) -15 else 5)).coerceIn(0, 100)

        if (!attackerWins) mods.add("守方以逸待劳，挡住攻势")
        if (attackerArmy.troops > totalDefTroops * 1.5) mods.add("兵力大幅占优(+)")

        val report = buildFieldReport(attackerArmy, attackerCommander, defenderArmies,
            atkLoss, defLoss, atkRemain, defRemain, advantage, attackerWins, mods, city, state)

        return BattleOutcome(
            battleType = "FIELD",
            attackerFactionId = attackerArmy.ownerFactionId,
            defenderFactionId = defenderArmies.firstOrNull()?.ownerFactionId ?: city.owner,
            attackerArmyId = attackerArmy.id,
            targetCityId = city.id,
            attackerWins = attackerWins,
            attackerLosses = atkLoss,
            defenderLosses = defLoss,
            attackerRemaining = atkRemain,
            defenderRemaining = defRemain,
            attackerMoraleAfter = atkMorale,
            defenderMoraleAfter = defMorale,
            advantage = advantage,
            cityCaptured = false,  // 野战不直接占城
            modifiers = mods,
            report = report
        )
    }

    // ─── 攻城 ─────────────────────────────────────────────────────────────────

    /**
     * 攻城解算：攻击方 Army vs 城池守军(City.troops) + 正式守将
     */
    fun resolveSiege(
        attackerArmy: Army,
        attackerCommander: Officer?,
        city: City,
        garrisonOfficer: Officer?,   // cityGarrisons 对应的守将（可为null）
        state: GameState,
        seed: Long
    ): BattleOutcome {
        val rng = Random(seed)
        val mods = mutableListOf<String>()

        // 守将加成
        val garCmd = garrisonOfficer?.command ?: 50
        val garStr = garrisonOfficer?.strategy ?: 50
        if (garrisonOfficer != null && garCmd >= 75) mods.add("${garrisonOfficer.name}坐镇，守城严整(+)")

        // 守方战力 = 城兵 × 城防加成 × 守将 × 民心 × 地形
        var defendPower = city.troops.toDouble()
        val defenseBonus = 1.0 + city.defense / 100.0 * 0.85
        defendPower *= defenseBonus
        defendPower *= (0.85 + garCmd / 100.0 * 0.35)     // 守将统率
        defendPower *= (0.92 + garStr / 100.0 * 0.15)     // 守将谋略
        defendPower *= (0.80 + city.popularSupport / 100.0 * 0.40)  // 民心
        defendPower *= terrainDefenseFactor(city.terrain)
        if (city.defense >= 80) mods.add("城高壕深，固若金汤(+)")
        if (city.popularSupport >= 80) mods.add("民心所向，军民同仇(+)")

        // 攻方战力
        val attackerEnemyUnit = BattleUnitCatalog.byId("song_infantry")  // 守城方通用
        val attackPowerBase = armyPower(attackerArmy, attackerCommander,
            attackerEnemyUnit, city.terrain, state.season, state.weather, true, mods)

        // ±8% 波动
        val atkPower = attackPowerBase * (0.92 + rng.nextDouble() * 0.16)
        val defPower = (defendPower * (0.92 + rng.nextDouble() * 0.16)).coerceAtLeast(1.0)

        val total = atkPower + defPower
        val atkRatio = atkPower / total
        val advantage = (atkRatio * 100).roundToInt().coerceIn(5, 95)
        val attackerWins = rng.nextDouble() < atkRatio

        // 攻城伤亡（攻城方较高）
        val intensity = 1.0 - abs(atkRatio - 0.5) * 2.0
        val atkLossRate = if (attackerWins) (0.08 + intensity * 0.16) else (0.16 + intensity * 0.24)
        val defLossRate = if (attackerWins) (0.20 + intensity * 0.30) else (0.08 + intensity * 0.12)

        val atkLoss = (attackerArmy.troops * atkLossRate * (0.85 + rng.nextDouble() * 0.30)).roundToInt()
            .coerceIn(0, attackerArmy.troops)
        val defLoss = (city.troops * defLossRate * (0.85 + rng.nextDouble() * 0.30)).roundToInt()
            .coerceIn(0, city.troops)

        val atkRemain = attackerArmy.troops - atkLoss
        val defRemain = city.troops - defLoss
        val atkMorale = (attackerArmy.morale + (if (attackerWins) 10 else -10)).coerceIn(0, 100)
        val cityCaptured = attackerWins

        val report = buildSiegeReport(attackerArmy, attackerCommander, city, garrisonOfficer,
            atkLoss, defLoss, atkRemain, defRemain, advantage, attackerWins, cityCaptured, mods, state)

        return BattleOutcome(
            battleType = "SIEGE",
            attackerFactionId = attackerArmy.ownerFactionId,
            defenderFactionId = city.owner,
            attackerArmyId = attackerArmy.id,
            targetCityId = city.id,
            attackerWins = attackerWins,
            attackerLosses = atkLoss,
            defenderLosses = defLoss,
            attackerRemaining = atkRemain,
            defenderRemaining = defRemain,
            attackerMoraleAfter = atkMorale,
            defenderMoraleAfter = 0,   // 城池无独立士气
            advantage = advantage,
            cityCaptured = cityCaptured,
            modifiers = mods,
            report = report,
            newControlState = if (cityCaptured) "FRONTLINE" else city.controlState
        )
    }

    // ─── 战报生成 ─────────────────────────────────────────────────────────────

    private fun buildFieldReport(
        atk: Army, atkCmd: Officer?,
        defs: List<Army>, atkLoss: Int, defLoss: Int,
        atkRemain: Int, defRemain: Int, advantage: Int,
        win: Boolean, mods: List<String>, city: City, state: GameState
    ): String {
        val defTotal = defs.sumOf { it.troops }
        val defNames = defs.take(2).joinToString("、") { it.name }
        val head = if (win) "【野战捷报】${city.name}城外，王师得胜！"
                   else "【野战战报】${city.name}城外，攻势受阻。"
        return buildString {
            appendLine(head)
            appendLine("${atk.name}（${atk.troops / 1000}k）对阵${defNames}（${defTotal / 1000}k）")
            appendLine("统帅：${atkCmd?.name ?: "无"} 统率${atkCmd?.command ?: "-"} · 敌将各部联防")
            appendLine("综合优势：${advantage}%  天气：${state.weather.label}  地形：${terrainLabel(city.terrain)}")
            if (mods.isNotEmpty()) appendLine("主要因素：${mods.take(3).joinToString(" · ")}")
            appendLine("结果：${if (win) "敌军败退" else "攻势受阻"}")
            appendLine("我军折损：${atkLoss / 1000}千（余${atkRemain / 1000}k）  敌军折损：${defLoss / 1000}千（余${defRemain / 1000}k）")
        }.trim()
    }

    private fun buildSiegeReport(
        atk: Army, atkCmd: Officer?, city: City, garOfficer: Officer?,
        atkLoss: Int, defLoss: Int, atkRemain: Int, defRemain: Int,
        advantage: Int, win: Boolean, captured: Boolean, mods: List<String>, state: GameState
    ): String {
        val head = if (captured) "【捷报】${city.name}已克复！" else "【战报】${city.name}久攻不下，将士折损。"
        return buildString {
            appendLine(head)
            appendLine("${atk.name}（${atk.troops / 1000}k）攻${city.name}守军（${city.troops / 1000}k · 城防${city.defense}）")
            appendLine("统帅：${atkCmd?.name ?: "无"} 统率${atkCmd?.command ?: "-"}  守将：${garOfficer?.name ?: "无"}")
            appendLine("综合优势：${advantage}%  天气：${state.weather.label}  地形：${terrainLabel(city.terrain)}")
            if (mods.isNotEmpty()) appendLine("主要因素：${mods.take(3).joinToString(" · ")}")
            appendLine("结果：${if (captured) "${city.name}旗帜易主，我军入城" else "守军坚守，攻势受挫"}")
            appendLine("我军折损：${atkLoss / 1000}千（余${atkRemain / 1000}k）  守军折损：${defLoss / 1000}千（余${defRemain / 1000}k）")
        }.trim()
    }

    private fun terrainLabel(t: String) = when(t) {
        "pass" -> "关隘"; "mountain" -> "山地"; "river" -> "水网"
        "coast" -> "沿海"; else -> "平原"
    }
}
