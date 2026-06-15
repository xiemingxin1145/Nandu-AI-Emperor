# NPC 派系与长期记忆骨架

本模块用于让朝臣不只是一次性发言，而是根据皇帝此前裁断逐渐形成长期态度。

## 当前接入

- `CourtFactionMemorySystem`
- `CourtFactionSnapshot`
- `OfficerMemoryNote`
- `PalaceTasksScreen` 中的“朝中派系风向”面板
- 朝会发言卡中的“记忆”提示

## 当前派系

- 主战诸臣：岳飞、韩世忠、李纲
- 主和文臣：秦桧
- 财政执政：赵鼎
- 后苑内廷：皇后、太后、内侍
- 皇城司耳目：皇城司勾当官

## 记忆来源

这一版暂不新增存档字段，而是从已有 `GameState` 派生：

- `storyFlags`
- `warFactionPower`
- `peaceFactionPower`
- 官员忠诚、野心、功绩
- 钱粮、军心、金国威胁等状态

## 体验效果

玩家在宫殿待办中点击“入殿听奏”后，可以看到：

1. 当前朝中哪个派系声势最重。
2. 主战、主和、财政、内廷、皇城司各自状态。
3. 重要人物发言时带有长期记忆提示。

例：

- 如果多次选择主战，岳飞、李纲会显示“蒙恩可战”。
- 如果多次暂缓兵议，岳飞、李纲可能显示“前议受抑”。
- 如果重用主战，秦桧可能显示“暗自不满”。
- 如果整顿政事堂，赵鼎可能显示“理财受信”。
- 如果开启皇后建议线，后苑会显示“中宫可问”。

## 后续计划

下一阶段可把派系记忆从派生式升级为持久式：

```kotlin
data class OfficerRelation(
    val fromOfficerId: String,
    val toOfficerId: String,
    val trust: Int,
    val rivalry: Int,
    val factionTie: Int
)

data class PersistentFactionState(
    val factionId: String,
    val power: Int,
    val resentment: Int,
    val confidence: Int,
    val lastTouchedTurn: Int
)
```

并继续补：

- 人物互相结党 / 反目
- 被冷落、重用、敲打后的长期反馈
- 派系待办和派系反扑
- 后宫好感、内廷稳定、外朝观感
- AI 奏议引用这些记忆生成更贴合局势的台词
