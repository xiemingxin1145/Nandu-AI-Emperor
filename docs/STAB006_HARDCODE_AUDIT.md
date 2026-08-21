# STAB-006 — 全仓历史硬编码审计清单

> 本文档是 STAB-006"输出审计清单"这项完成标准的交付物。
>
> 范围说明：本分支上已经完成了"声明式触发门控"（`EventDirector` 强制执行
> JSON 里声明的 `required_npc_alive`/`city_owner`/`blocked_flags`/`trigger_event`
> 等条件）和"效果回写"（`StoryEventEffectApplier` 让声明的效果真正写进
> `GameState`，兼容 camelCase/snake_case，处理嵌套 city 效果）——这两块解决的
> 是"剧情事件系统"层面的"固定历史结果"和"UI 假数据"问题，本次审计**不重做**，
> 只记录为已完成项。本文档聚焦剩余的：分散在具体屏幕/系统文件里的固定年份、
> 固定官职、固定人物位置、固定城市归属。

---

## 一、已修复（本次，高危正式流程）

### 1. `GameCalendar` 永不切换年号 — 已修复

**位置**：`app/src/main/java/com/xiemingxin/nandu/game/GameRuleEngine.kt`

**问题**：`advance()` 每次跨年都硬编码 `"建炎${chineseYear(nextYear)}年"`。建炎
年号历史上只用了四年（1127-1130），绍兴元年起于1131年，但游戏里无论玩家推进
多少年，界面上永远显示"建炎N年"——如果玩家玩到游戏内第十五年，会显示"建炎
十五年"这种史书上不存在的年号。这是正式流程里持续暴露给玩家的历史穿帮
（日历显示是几乎每一屏都会看到的信息）。

**修复**：`GameCalendar` 新增 `companion object` 里的 `eraNameFor(gameYear)`，
第 1~4 年用"建炎"，第 5 年起改用"绍兴"纪年（重新从"元年"计数）。同时把原本
只支持 1-10 的中文数字转换扩展成支持 1-99（原实现超过10会退化成"绍兴11年"
这种阿拉伯数字与中文混搭的写法，不符合传统纪年习惯——绍兴年号历史上一路用到
三十二年，必须能正确显示到两位数）。

**已知边界**：只处理"建炎→绍兴"这一次切换。当前游戏设计范围内高宗一直在位，
不模拟绍兴之后的禅位改元（隆兴、乾道等）——如果未来游戏支持模拟到孝宗朝，
需要再扩展 `eraNameFor`。

**测试**：新增 `GameCalendarEraTest.kt`，6 个用例，覆盖开局纪年、建炎四年边界、
第五年切换、远期年份（对应顺昌之战候选窗口附近）确实已切绍兴、`eraNameFor`
纯函数与真实 `advance()` 推进结果一致。

### 2. `IntroScreen.kt` 硬编码"临安行在" — 已修复

**位置**：`app/src/main/java/com/xiemingxin/nandu/ui/screens/IntroScreen.kt`

**问题**：开场画面（游戏最早、几乎每个玩家都会看到的一屏）的 `contentDescription`
写死"临安行在"，与首都已经修正为应天府的历史设定（Canon v1.1）矛盾。

**修复**：改成不含具体地名的"皇宫"。图片本身暂不换（换成应天府风格的宫殿图
需要新美术，超出本任务范围，且不影响 `contentDescription` 只是无障碍文本，
不是玩家肉眼可见的主要内容）。

### 3. `MockProvider.kt` 硬编码"垂拱殿已议定圣旨" — 已修复

**位置**：`app/src/main/java/com/xiemingxin/nandu/ai/MockProvider.kt`

**问题**：`EdictResult.summary` 这个字段会被 `EmperorMainScreen.kt` 原文直接
展示给玩家（`CourtStageHeader` 的 subtitle），不经过任何时代性过滤。硬编码
"垂拱殿"这三个字，在应天府开局阶段（乃至任何还没有独立宫殿名设定的时间点）
都构成穿帮。

**修复**：改成不含具体宫殿名的"朝廷已议定圣旨"。

---

## 二、发现但判定非高危、本次未修（附理由）

### 4. `MockProvider.kt` 硬编码 `"zhao_ding"` 作为默认发言人（7 处）

**位置**：同上文件，`parseEdict()` 内多处 `NpcResponse("zhao_ding", ...)`。

**问题**：赵鼎现在的状态是 `NOT_YET_RELEVANT`（Canon v1.1），但 Mock 模式下
的圣旨解析逻辑仍然把他当作"永远在场、可以代表朝廷发言的财政大臣"角色硬编码
使用。

**为什么判定非高危**：追踪了下游渲染路径——`EmperorMainScreen.kt` 的
`CourtDebatePanel` 已经用 `CharacterAppearanceSystem.canAppearInPalace` /
`visibilityFor(...) != HIDDEN` 对所有 `NpcResponse` 做了两层过滤。赵鼎现在
`NOT_YET_RELEVANT` 映射到 `HIDDEN` 可见度，两层过滤都过不去，**这些硬编码
生成的"赵鼎发言"实际上不会被展示给玩家**——下游系统已经有防护网接住了。

**为什么本次不修**：`parseEdict()` 函数签名里没有 `GameState` 参数（只有
`edictText` 和 `GameContext`），要让这里的发言人选择感知真实人物状态，需要
改 `AiProvider` 接口签名，这会牵连 `ClaudeProvider.kt`/`OtherProviders.kt`
等其它实现类——改动面明显超出"历史穿帮修复"应有的范围，且当前工作模式正在
做 V1.6.2 集成，`ai/` 目录下的接口级改动有较高的跨分支冲突风险。

**建议**：留给专门的"AiProvider 感知真实世界状态"任务，到时候一并处理，
不要只是换个硬编码的人名了事。

### 5. `VoiceProfiles.kt` 里的 `HistoricalEvent` 数据（`fixedHistory` /
   `intervenableEvents` / `potentialFuture`，约 15 条，含"绍兴和议""岳飞被害"
   "崖山海战"等）

**位置**：`app/src/main/java/com/xiemingxin/nandu/audio/VoiceProfiles.kt`

**问题**：这套数据结构定义了"历史惯性""蝴蝶效应阈值"等相当完整的概念，年份
用的是公历数字（1127、1140、1141……），风格上更像是提前为 V1.7 的
`HIST-001`（历史事件统一数据模型）准备的设计草稿。

**为什么判定非高危**：全仓搜索确认**没有任何其它代码引用 `HistoricalEvent`**
（包括 `canChangeHistory` 函数）——这是一段完全没有被接入任何系统的死代码，
玩家不可能看到，不构成正式流程穿帮。

**建议**：不建议现在删除（万一是有意留给 HIST-001 参考的设计稿），但也不建议
现在接入（会大幅扩大改动面，超出 STAB-006 范围）。启动 HIST-001 时应该先看
一眼这份草稿，决定是复用还是清理，不要在不知情的情况下重新发明一遍。

### 6. `Faction("jin").rulerName` 写"完颜宗望"，但金国唯一有立绘、唯一被
   Canon 研究文档提及的却是完颜宗弼

这一条 ROSTER-001 审计（`docs/ROSTER_EXPANSION_AUDIT.md`）已经记录过，此处
交叉引用，不重复展开。不算"硬编码穿帮"（斡离不 1127 年确实在世且是攻宋主力
之一，这个设定本身没错），而是"金国最高决策权后续应该动态过渡"这个功能缺失，
需要金国也有一套任免/继承逻辑才能正确表达，本次不处理。

---

## 三、审计方法与已排查、确认干净的范围

用以下模式在 `app/src/main/java/` 全仓搜索，逐条人工核对上下文（排除历史背景
说明性质的注释、`GameCalendar` 默认值、Canon 研究引用等合理用法）：

- `建炎[一二三四五六七八九十]年` / `绍兴[一二三四五六七八九十]年`（精确年号
  组合）：无命中。
- `建炎` / `绍兴`（宽泛搜索）：逐个文件核对，除本清单列出的问题外，其余命中
  均为合理用法（历史背景注释、地名"绍兴"本身、`GameCalendar` 相关代码、
  `WorldRegionPlan.kt` 的核心城市列表等）。
- 硬编码具体官职称号（`"东京留守"`/`"枢密使"`/`"尚书右仆射"`/`"节度使"` 等）
  出现在 `ui/` 目录：无命中——官职展示已经统一走 `AppointmentSystem.currentRole()`
  这类状态读取路径。
- 硬编码人物位置断言（"岳飞在……"这类字符串模式）：无命中。
- 硬编码城市归属判断（不通过 `City.owner` 读取）：无命中，检查过的
  `VictoryJudge.kt`/`MapData.kt` 里的 `ownerHint` 都是初始提示值，会随游戏
  进程被真实 `owner` 字段覆盖，不是穿帮。
- 这次分支自己新增的战役相关文件（`ShunchangBattleScreen.kt`、
  `DynamicBattleBriefingScreen.kt`、`ShunchangDirectivePanel.kt`）：单独检查，
  干净，没有硬编码年份/身份。

---

## 四、完成标准对照

- [x] 输出审计清单（本文档）。
- [x] 高危正式流程硬编码清零 —— 本次发现的 3 处玩家可见穿帮
  （日历年号 / IntroScreen 首屏描述 / Mock 圣旨解析摘要）已修复并有测试覆盖
  （日历部分）或人工核查（另两处为纯字符串替换，改动面极小）。
- [x] 非高危项已记录并说明为什么不在本次处理，避免"没人知道这个问题存在"。
