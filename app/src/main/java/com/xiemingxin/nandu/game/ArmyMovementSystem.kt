package com.xiemingxin.nandu.game

import kotlin.math.ceil

/**
 * Stage 4 军团行军系统
 *
 * 核心职责：
 *  1. BFS路径规划（沿地图道路图，不再直线瞬移）
 *  2. 逐节点/逐旬推进行军
 *  3. 敌境检测（遇敌城进入 ENGAGEMENT_PENDING，不攻城）
 *  4. 行军时间计算（道路类型 × 军团类型 × 季节 × 天气）
 */
object ArmyMovementSystem {

    // ─── 道路基础天数/节点 ────────────────────────────────────
    private const val BASE_DAYS_PER_NODE = 8   // 标准陆路一个节点耗时（天）

    private fun roadCostFactor(type: RoadType, armyType: String): Double = when (type) {
        RoadType.LAND    -> if (armyType.contains("cavalry")) 0.7 else 1.0
        RoadType.RIVER   -> if (armyType.contains("naval"))   0.5 else 1.4
        RoadType.CANAL   -> if (armyType.contains("naval"))   0.6 else 1.2
        RoadType.MOUNTAIN-> if (armyType.contains("mountain"))0.8 else 1.8
        RoadType.SEA     -> if (armyType.contains("naval"))   0.6 else 3.0  // 步军不能走海路
        RoadType.PASS    -> 1.5
    }

    private fun seasonFactor(season: Season): Double = when (season) {
        Season.SPRING -> 1.0
        Season.SUMMER -> 1.05
        Season.AUTUMN -> 0.95
        Season.WINTER -> 1.30
    }

    private fun weatherFactor(weather: WeatherType): Double = when (weather) {
        WeatherType.CLEAR -> 1.0
        WeatherType.RAIN  -> 1.15
        WeatherType.STORM -> 1.55
        WeatherType.FOG   -> 1.10
        WeatherType.SNOW  -> 1.45
        WeatherType.WIND  -> 1.08
    }

    private fun commandFactor(command: Int): Double =
        1.0 - ((command - 50).coerceIn(0, 50) / 500.0)   // 统率每+10 行军快2%，上限10%

    /**
     * BFS寻路：返回从 start 到 target 的路线节点列表（含起终点）
     * 步军不能走纯海路节点，骑兵不推荐山路，水军优先水路。
     * 找不到路径返回 null。
     */
    fun findRoute(
        startId: String,
        targetId: String,
        armyType: String
    ): List<String>? {
        if (startId == targetId) return listOf(startId)
        // BFS（加权最短路），使用 Dijkstra（轻量实现）
        val dist = mutableMapOf(startId to 0.0)
        val prev = mutableMapOf<String, String>()
        val queue = ArrayDeque<String>()
        queue.add(startId)
        val visited = mutableSetOf<String>()

        // 简单BFS（不考虑权重）→ 改为Dijkstra
        val pq = java.util.PriorityQueue<Pair<Double, String>>(compareBy { it.first })
        pq.add(0.0 to startId)

        while (pq.isNotEmpty()) {
            val (cost, node) = pq.poll()
            if (node in visited) continue
            visited.add(node)
            if (node == targetId) break
            val neighbors = MapData.neighborsOf(node)
            for (neighbor in neighbors) {
                val roadType = MapData.roadType(node, neighbor) ?: RoadType.LAND
                // 步军不能走纯海路
                if (roadType == RoadType.SEA && !armyType.contains("naval")) continue
                val edgeCost = BASE_DAYS_PER_NODE * roadCostFactor(roadType, armyType)
                val newCost = cost + edgeCost
                if (newCost < (dist[neighbor] ?: Double.MAX_VALUE)) {
                    dist[neighbor] = newCost
                    prev[neighbor] = node
                    pq.add(newCost to neighbor)
                }
            }
        }

        if (targetId !in prev && targetId != startId) return null

        // 重建路径
        val path = mutableListOf<String>()
        var cur: String? = targetId
        while (cur != null) {
            path.add(0, cur)
            cur = prev[cur]
        }
        return if (path.first() == startId) path else null
    }

    /**
     * 计算给定路线的总行军天数
     */
    fun calcRouteDays(
        routeNodeIds: List<String>,
        armyType: String,
        season: Season,
        weather: WeatherType,
        commandStat: Int
    ): Int {
        if (routeNodeIds.size <= 1) return 0
        var totalDays = 0.0
        for (i in 0 until routeNodeIds.size - 1) {
            val roadType = MapData.roadType(routeNodeIds[i], routeNodeIds[i + 1]) ?: RoadType.LAND
            val segDays = BASE_DAYS_PER_NODE *
                roadCostFactor(roadType, armyType) *
                seasonFactor(season) *
                weatherFactor(weather) *
                commandFactor(commandStat)
            totalDays += segDays
        }
        return ceil(totalDays).toInt().coerceIn(3, 150)
    }

    /**
     * 每旬（10天）推进所有行军中的宋方军团。
     * 返回新GameState和军情报告列表。
     *
     * 关键原则：
     *  - 抵达己方城池：Army.currentCityId更新，状态→GARRISONED，不加兵回城池
     *  - 抵达敌方城池前：状态→ENGAGEMENT_PENDING，停在最后一个己方/中立节点
     *  - 补给和士气由 ArmySupplySystem 单独处理
     */
    fun tickAllArmies(
        state: GameState,
        tickDays: Int = 10
    ): Pair<GameState, List<String>> {
        var officers = state.officers
        val reports = mutableListOf<String>()

        val newArmies = state.armies.map { army ->
            if (army.ownerFactionId != "song") return@map army  // 只tick宋方（金军暂不处理）
            if (army.statusCode != ArmyStatus.MARCHING) return@map army
            if (army.routeNodeIds.isEmpty() || army.routeIndex >= army.routeNodeIds.size - 1) {
                // 路线空或已在终点，转驻扎
                return@map army.copy(statusCode = ArmyStatus.GARRISONED, status = ArmyStatus.GARRISONED.label)
            }

            // 推进路线节点（简化：每10天移动约1-2个节点）
            var remainingDays = tickDays
            var idx = army.routeIndex
            var currentNode = army.routeNodeIds[idx]
            var hitEnemy = false

            while (remainingDays > 0 && idx < army.routeNodeIds.size - 1) {
                val nextNode = army.routeNodeIds[idx + 1]
                val roadType = MapData.roadType(currentNode, nextNode) ?: RoadType.LAND
                val commander = state.officers.find { it.id == army.commanderId }
                val cmdStat = commander?.command ?: 60
                val segDays = ceil(
                    BASE_DAYS_PER_NODE *
                    roadCostFactor(roadType, army.armyType) *
                    seasonFactor(state.season) *
                    weatherFactor(state.weather) *
                    commandFactor(cmdStat)
                ).toInt()

                if (remainingDays >= segDays) {
                    // 检查下一节点是否是敌方城池
                    val nextCity = state.cities.find { it.id == nextNode }
                    if (nextCity != null && nextCity.owner != "song" && nextCity.owner != "") {
                        // 敌境！停在当前节点，进入待战
                        hitEnemy = true
                        break
                    }
                    remainingDays -= segDays
                    idx++
                    currentNode = nextNode
                } else {
                    break  // 这旬走不完这一段，停在当前节点
                }
            }

            val arrivedAtTarget = idx == army.routeNodeIds.size - 1
            val newNode = army.routeNodeIds[idx]
            val commander = state.officers.find { it.id == army.commanderId }
            val cmdName = commander?.name ?: army.name

            when {
                hitEnemy -> {
                    val enemyNode = army.routeNodeIds[idx + 1]
                    val enemyCity = state.cities.find { it.id == enemyNode }
                    reports.add("【军情】${cmdName}部进抵${state.cities.find { it.id == newNode }?.name ?: newNode}，前方${enemyCity?.name ?: enemyNode}为敌控区域，全军列阵待旨。")
                    // 更新武将位置
                    officers = officers.map {
                        if (it.id == army.commanderId) it.copy(currentCityId = newNode) else it
                    }
                    army.copy(
                        currentCityId = newNode,
                        routeIndex = idx,
                        statusCode = ArmyStatus.ENGAGEMENT_PENDING,
                        status = ArmyStatus.ENGAGEMENT_PENDING.label,
                        marchDaysRemaining = 0
                    )
                }
                arrivedAtTarget -> {
                    val targetCityName = state.cities.find { it.id == newNode }?.name ?: newNode
                    reports.add("【军情】${cmdName}部（${army.troops}兵）抵达${targetCityName}，就地驻扎，听候调遣。")
                    officers = officers.map {
                        if (it.id == army.commanderId) it.copy(currentCityId = newNode, status = OfficerStatus.DEPLOYED) else it
                    }
                    // 注意：不加兵回 city.troops！Army保持独立存在
                    army.copy(
                        currentCityId = newNode,
                        routeIndex = idx,
                        statusCode = ArmyStatus.GARRISONED,
                        status = ArmyStatus.GARRISONED.label,
                        targetCityId = "",
                        marchDaysRemaining = 0,
                        marchDaysTotal = 0,
                        supplyCityId = newNode
                    )
                }
                else -> {
                    // 仍在行军途中
                    val remainSeg = army.routeNodeIds.size - 1 - idx
                    val approxRemainingDays = remainSeg * BASE_DAYS_PER_NODE  // 粗估
                    army.copy(
                        currentCityId = newNode,
                        routeIndex = idx,
                        marchDaysRemaining = approxRemainingDays,
                        status = "行军·余约${approxRemainingDays}日"
                    )
                }
            }
        }

        return state.copy(armies = newArmies, officers = officers) to reports
    }

    /**
     * 重新规划行军路线（玩家改变目标时）
     * 从当前节点重新计算路径，不瞬移。
     */
    fun rerouteArmy(
        state: GameState,
        armyId: String,
        newTargetId: String
    ): Pair<GameState, String> {
        val army = state.armies.find { it.id == armyId }
            ?: return state to "【改道失败】找不到该军团。"
        if (army.statusCode != ArmyStatus.MARCHING && army.statusCode != ArmyStatus.GARRISONED && army.statusCode != ArmyStatus.ENGAGEMENT_PENDING)
            return state to "【改道失败】军团当前状态不支持改道。"

        val fromNode = army.currentCityId
        val route = findRoute(fromNode, newTargetId, army.armyType)
            ?: return state to "【改道失败】从${state.cities.find{it.id==fromNode}?.name?:fromNode}至${state.cities.find{it.id==newTargetId}?.name?:newTargetId}无可用路线，请检查地图连通性。"

        val commander = state.officers.find { it.id == army.commanderId }
        val cmdStat = commander?.command ?: 60
        val totalDays = calcRouteDays(route, army.armyType, state.season, state.weather, cmdStat)
        val fromName = state.cities.find { it.id == fromNode }?.name ?: fromNode
        val toName = state.cities.find { it.id == newTargetId }?.name ?: newTargetId
        val routeDesc = route.mapNotNull { id -> state.cities.find { it.id == id }?.name ?: MapData.nodeMap[id]?.name }.joinToString("→")

        val newArmy = army.copy(
            targetCityId = newTargetId,
            routeNodeIds = route,
            routeIndex = 0,
            marchDaysTotal = totalDays,
            marchDaysRemaining = totalDays,
            statusCode = ArmyStatus.MARCHING,
            status = "行军·余约${totalDays}日"
        )
        val newArmies = state.armies.map { if (it.id == armyId) newArmy else it }
        return state.copy(armies = newArmies) to
            "【改道】${commander?.name ?: army.name}部改赴${toName}，路线：${routeDesc}，预计${totalDays}日。"
    }
}
