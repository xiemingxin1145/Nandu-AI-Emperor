# 《南渡无悔》项目总纲与持续执行路线

> 本文档是项目唯一主路线图。任何 AI / 开发者接手前必须先读根目录 `AGENTS.md`，再读本文件。
>
> 核心规则：**做完一项，更新一项；任何任务没有写回本文件和当前执行看板，就视为没有完整交接。**
>
> 长期版本与 37 项主线只以本文件为准；当前 P0/P1、负责人、证据、缺陷和唯一 `NEXT` 只以 `docs/PROJECT_EXECUTION_BOARD.md` 为准。

最后更新：2026-08-21  
当前可测试版本：**V1.6.1 RC / versionCode 28**  
当前稳定基线：`release/v1.6.1`  
当前主集成线：`fix/stab-005-video-player-compat`（PR #58，基于 STAB-004）  
当前最终集成线：`integration/v1.6.2-preacceptance`（PR #59；STAB-001～006、COURT-001、ROSTER-001 审计和正式入口修复已接入）
并行已验证分支：`feat/court-001-crowd-wiring`（PR #54，已并入预验收线，尚未合 main）
并行审计分支：`docs/roster-001-expansion-audit`（Claude，ROSTER-001 审计文档已并入预验收线）
当前里程碑：**V1.6.2 稳定化与去 Demo 化**  
当前执行任务：**STAB-007 正式入口 Smoke Test（IN_PROGRESS；8/8 BGM 候选已恢复，等待人工试听与真机验收）**
当前下一执行项：**OPENING-P0-001 — 六幕序章、穿越身份与开局 30 分钟真机复现**
当前里程碑进度：**6 / 8**  
当前总进度：**6 / 37**

当前项目执行中枢：`docs/PROJECT_EXECUTION_BOARD.md`
当前 AI 协作协议：`docs/AI_COLLABORATION_PROTOCOL.md`
开局发布验收门：`docs/OPENING_30MIN_RELEASE_GATE.md`
独立真实山河原型：Draft PR #68 / `feat/map-real-geography-v2` / APK #760；尚未合入当前正式集成线，等待用户手机裁决。

---

# 一、项目总纲

## 1. 游戏定位

《南渡无悔》不是历史剧情播放器，而是一款 **AI 原生的南宋皇帝动态历史策略沙盒**。

玩家从 1127 年建炎开局，以赵构身份执掌南宋。历史只提供初始世界、人物、矛盾与趋势；之后的天下必须允许被玩家和 AI 势力共同改变。

目标体验：

- 玩家下旨，世界真实响应；
- 官员拥有真实位置、官职、目标、关系、记忆和利益；
- 金国、西夏、大理、高丽、草原、地方集团会自主行动；
- 战争来自真实调兵、补给、攻城、撤退，而不是固定按钮；
- 历史事件可以发生，也可以提前、延迟、变形、被替代或永不发生；
- AI 负责思考、表达与候选计划，本地规则负责合法性和世界状态裁决；
- 玩家每一旬都应感到：**天下自己在动，而我正在改变它。**

## 2. 核心设计原则

### 2.1 历史锚点，不是历史铁轨

历史事件应使用：

`时间窗口 + 世界条件 + 人物条件 + 战争条件 + 抑制条件 + 替代事件`

禁止：

`到了某年某月 -> 强制播放固定剧情`

例如顺昌之战只有在金军仍有南侵能力、对应战区真实形成、城池仍有战略意义、人物与军团条件合理时才可能出现；如果金国提前衰弱，则应转为北伐、议和、收复等替代机会。

### 2.2 世界状态是唯一真相源

UI 不得硬编码人物官职、所在地、兵力、战役年份、城市归属或固定胜负。

### 2.3 玩家决策必须回写世界

禁止只改 Compose 局部变量或显示一句“准奏”。任何正式决策必须写回 `GameState`，产生即时效果、延迟效果、人物/势力反应和记录。

### 2.4 正式入口不能是空壳

禁止：

- “待办 1 件”但进去空白；
- 有按钮但没有世界后果；
- 测试场景永久挂在主页；
- 播放失败一律谎报“编码不兼容”。

### 2.5 AI 不直接改世界

AI 可生成建议、奏议、计划、外交措辞和候选行动；最终数值、移动、任免、战斗与资源变化必须经过本地规则验证。

### 2.6 源素材可以高质量，发布产物必须兼容

美术/视频源文件可保留高质量原始格式，但进入 Android APK 前必须经过确定性的发布转换与 QA。不得把“开发机能播”当成“目标手机一定能播”。

### 2.7 先做可玩基线，再做大系统

当前已发布/可测版本已经是 **V1.6.1 / versionCode 28**，不得为了叙述方便倒退为 V1.6.0。**V1.6.2 是第一版真正可玩的稳定化基线**；STAB-007 下优先清理序幕、玩家身份、音视频、朝议卡死、人物时空、授权执行、地图验收和存档 P0。完整动态历史重构进入 V1.7，人物池与活世界进入 V1.8。

发现新问题时必须先进入执行看板，标明严重度、负责人、复现证据与验收条件；不得绕过当前 `NEXT` 自行扩展新玩法。

---

# 二、当前已有基础

- [x] Android / Compose 框架
- [x] 自由文本圣旨 -> AI/Mock -> 本地白名单裁决
- [x] OpenAI-compatible 自定义模型/中转入口
- [x] 城市、地图、基础内政
- [x] Stage 4 军团 / 行军 / 补给
- [x] Stage 5 战斗 / 攻城 / 占领
- [x] Stage 6 AI World Engine
- [x] Stage 7 Faction Brain
- [x] Stage 8 Character Agent
- [x] Living World Canon 基础
- [x] 世界 AI 信息边界基础
- [x] 宫殿与待办骨架
- [x] V3 美术资产库
- [x] 序章 6 幕 + 手动快进/回看
- [x] BGM / 环境声 / SFX / 旁白分层框架
- [x] V1.6.1 起固定 Android 测试签名与版本策略 CI
- [x] 八宫殿待办“有数字但内容空白”布局/映射修复
- [x] 朝会群像与应天府视觉接线已在独立 PR 验证通过
- [x] Media3 统一视频播放层 + 构建时 Android 视频转码/QA

当前仍明显未成熟：宫殿待办持久化/完成/逾期/连锁机制、全仓历史硬编码、动态历史替代事件、大规模人物/势力主循环、经济外交情报联动、长期世界事件密度，以及最终真机全入口验收。

---

# 三、版本路线总览

| 版本 | 主题 | 任务数 | 当前进度 | 完成目标 |
| --- | --- | ---: | ---: | --- |
| V1.6.2 | 稳定化 / 去 Demo 化 | 8 | **6/8** | 所有入口可用，历史不再明显穿帮 |
| V1.7.0 | 动态历史核心 | 8 | 0/8 | 历史由条件生成，可被玩家改变 |
| V1.8.0 | 活世界与人物社会 | 8 | 0/8 | 人物、派系、国家持续自主行动 |
| V1.9.0 | 战略深度与国家治理 | 7 | 0/7 | 战争、财政、外交、地方形成闭环 |
| V2.0.0 | 完整体验与长期可玩 | 6 | 0/6 | 新玩家可连续玩数小时而非 Demo |

当前规划总任务：**37 项**  
当前完成：**6 / 37**

---

# 四、V1.6.2 — 稳定化与去 Demo 化

目标：先把现在“一脚一个坑”的地方封住。本里程碑共 8 项，**已完成 6 项，剩余 2 项**。

## STAB-001 — 顺昌测试入口退出正式流程

状态：`DONE`

完成：
- `HistoricalBattleAvailability.forShunchang(state)` 建立最小候选门控；
- 检查历史窗口、金国南侵能力、宋金敌对、顺昌代理区域归属、刘锜存活/位置/旅行状态；
- `PalaceHallScreen` 不满足条件时不显示入口；
- `MainActivity` 二次校验防绕过；
- 1127 开局、金国衰弱/灭亡、人物不在场等回归测试已覆盖。

记录：
- Claude 分支：`fix/stab-001-shunchang-gating`
- 原 PR：#52（已由 #53 承接并关闭）
- 核心提交：`d5ff4232d83f6cfe8c030bf4a18abbd61849664a`
- 集成验证：Android Build #214 PASS；Debug APK #728 PASS。

遗留：`GameCalendar` 年号切换、独立顺昌地图节点留给后续动态历史/地图任务。

## STAB-002 — 清理战役 UI 硬编码人物与年份

状态：`DONE`

完成：
- 日期来自实时 `GameCalendar`；
- 参战者从真实战区军团、驻防任命、人物状态生成；
- 官职/职责来自当前 rank、Army、Appointment；
- 死亡、异地、在途、俘虏、未登场人物不得凭空上镜；
- 兵力、粮草、士气、敌情从 `GameState` 派生；
- 删除旧顺昌页面约 960 行固定“建炎四年 / 刘锜岳飞韩世忠 / 18000 对十万”Demo 路径；
- 新动态军情页无真实主帅时显示明确空状态。

记录：
- 独立分支：`fix/stab-002-dynamic-battle-roster`，原 PR #51（已由 #53 承接并关闭）
- 集成分支：`integration/v1.6.2-stab-001-002`，PR #53
- 关键提交：`5617c5c`、`e1fcc6a`、`7afcdc6`
- 回归测试：6 组；Android Build #214 PASS；Debug APK #728 PASS。

## STAB-003 — 战役选择真实回写 GameState

状态：`DONE`

完成：
- 新增 `BattleDirectiveSystem`，正式支持 `HOLD / REINFORCE / DELIBERATE`；
- **固守**：真实扣除寿春粮草、提升真实城防与战区军团士气；
- **驰援**：只从当前真实宋军中选择可调军团，检查出发城粮草与地图可达性，调用 `ArmyMovementSystem` 生成真实路线并进入 `MARCHING`，不瞬移、不凭空造兵；
- **再议**：不伪造调军，明确写入军心、朝局与金军主动权代价；
- 同一旬只允许一次正式顺昌军令，防止反复点击刷数值；
- 每个成功军令写入 `ChronicleEntry`；
- 页面新增正式“御前军令”栏，UI 只发选择，所有数值统一由规则系统修改；
- 通过 `GameSaveCodec.export -> import` 回归测试确认军团路线、粮草变化和军令记录存档后仍存在。

记录：
- 分支：`fix/stab-003-persistent-battle-directives`
- PR：#55 → `integration/v1.6.2-stab-001-002`（Draft，不合 main）
- 关键提交：`049e673`、`e9ac886`、`793d774`、`cdbd8c2`、`569e172`、`9390edb`
- 新增回归测试：7 组
- 首轮 CI 抓到 Compose `weight` 内部 import 编译错误，已修复；
- 最终验证：**Android Build #217 PASS；Build Nandu Debug APK #730 PASS；unit tests PASS；APK 构建 PASS；固定 V1.6.1 测试签名校验 PASS。**

遗留：战役结果本身仍由 Stage 5 战争系统继续深化；本任务只负责皇帝战前军令真实落地。

## STAB-004 — 修复宫殿待办空白/空壳页面

状态：`DONE`

完成：
- 修复 `PalaceTasksScreen` 的装饰框布局：装饰图/遮罩改为 `matchParentSize()`，不再参与测量把真实待办内容挤出屏幕；
- 八宫殿“待办 N 件”与实际列表统一来自 `PalaceTaskSystem.generate()`，新增回归测试逐宫核对数量一致；
- 无待办时显示居中的明确空状态“本旬暂无待办 / 不是加载失败”；
- 移除 `turn % 4 == 1` 的机械内廷假事件，改为由金军压力、国库压力、朝局不稳等真实状态触发；
- 修正多个“按钮能点但专属后果未命中”的 choice id 映射；
- 朝议 `restore/south`、内廷 `advice`、礼制 `restore_oath` 与后果系统对齐并保留兼容别名；
- 政事堂 `audit`、人才 `field` 补上真实世界后果；
- 外贸待办提供真实 `trade` 选择并进入外交/贸易状态系统；
- 列表使用稳定 key，降低重组时状态错位风险；
- 新增 `PalaceTaskSystemRegressionTest`，覆盖八宫殿 badge/list 一致性、安静状态下内廷为空、状态驱动内廷事件、任务内容非空、贸易选择、所有专属宫殿选择不掉进通用占位结果。

记录：
- 分支：`fix/stab-004-palace-task-empty-states`
- PR：#56 → `fix/stab-003-persistent-battle-directives`（Draft，不合 main）
- 关键提交：`6425108`、`4893c48`、`0a91be8`、`6f9ed93`、`5cb32a5`
- 最终验证：**Android Build #218 PASS；Build Nandu Debug APK #731 PASS；unit tests PASS；APK 构建 PASS；固定测试签名校验 PASS。**
- 真机专项：本轮尚未逐宫真机点击验收，留给 STAB-007/STAB-008 统一做入口 Smoke Test 与发布验收。

遗留：`PalaceTask` 仍是由 GameState 每旬派生，未正式持久化完成/逾期/连锁状态；这属于后续世界/任务深化，不再作为“空白页”问题阻塞 V1.6.2。

## STAB-005 — 视频播放器兼容性修复

状态：`DONE`

完成：
- 通用 `AssetVideoSurface` 从 `MediaPlayer + TextureView` 迁移到 Media3 `ExoPlayer + PlayerView`；
- 序章第二幕删除独立 `VideoView + file:///android_asset/...` 老播放链，全游戏正式 asset 视频统一走同一个 Media3 入口；
- 使用 `asset:///...` 数据源，画面采用 ZOOM 裁切；
- 视频继续强制 `volume = 0`，生成视频内嵌声音不进入游戏混音；
- 新增 `AssetVideoFailureKind`：资源不存在 / 数据源 / 格式 / 解码器 / 播放器，停止把所有异常统一误报为“编码不兼容”；
- 静态 CG 永远先渲染在视频底层，播放失败时视频层退出，页面不会因视频失败黑屏；
- 全仓真实审计发现：**51 / 51 条 V3 MP4 源视频全部为 HEVC/H.265**，这是部分 Android 设备报“编码不可用”的直接根因；
- 新增 `scripts/prepare_android_video_assets.sh`：构建 APK 前自动将非 H.264 视频转为 **H.264 Main / level 4.0 / yuv420p / faststart**，并删除视频内嵌音轨；
- 新增 `scripts/check_video_assets.sh`：使用 ffprobe 对全部 MP4 校验容器可解析、H.264、yuv420p、1080p-class 以内、有效时长、AAC-or-silent；
- 两套 Android CI 都加入 ffmpeg/ffprobe 准备、发布转码、转后 QA；Ubuntu 24.04 runner 无 ffprobe 时会自动安装 ffmpeg；
- 最终 CI APK 进一步拆包实测：**assets/videos 下 51 条 MP4 = 51 条 h264 / 51 条 yuv420p / 51 条无内嵌音轨 / 0 条异常**。

记录：
- 分支：`fix/stab-005-video-player-compat`
- PR：#58 → `fix/stab-004-palace-task-empty-states`（Draft，不合 main）
- 关键提交包含 Media3 迁移、序章统一接线、视频 QA、构建转码与 CI 接线；STAB-005 CI 验证头：`0dc0f9a01ba30a559ca037bfb3957217b41dfb4e`
- 第一轮 CI：发现 Ubuntu 24.04 runner 默认无 ffprobe，已修；
- 第二轮 CI：真实发现 51 / 51 源视频均为 HEVC；
- 第三轮 CI：51 条构建时转码成功，转后 QA 全 PASS；
- 最终验证：**Android Build #225 PASS；Build Nandu Debug APK #737 PASS；unit tests PASS；Media3/Compose 编译 PASS；APK 构建 PASS；固定 V1.6.1 测试签名校验 PASS；artifact 上传 PASS。**
- Debug APK artifact：run #737 / artifact `nandu-v1.6.1-debug-apk`，artifact id `9434713143`。
- APK 拆包复核：51 条打包视频全部 H.264/yuv420p/无内嵌音轨。

遗留：无远程工具可替代玩家手机做完整硬件/系统组合真机播放验收；序章、战场、战役样例的实际点击播放留给 STAB-007 / STAB-008。代码与发布产物层的兼容根因已修复。

## STAB-006 — 全仓历史硬编码审计

状态：`DONE`

审计并处理：固定年份、固定官职、固定位置、固定城市归属、固定历史结果、正式流程中的 UI 假数据。

完成标准：输出审计清单，高危正式流程硬编码清零。

完成记录：

- 日期：2026-08-21
- 分支：`fix/stab-006-historical-hardcode-audit`（以 `fix/stab-005-video-player-compat`
  为正式 PR 基线，只包含 STAB-006 自身新增提交）
- 提交：
  - `3245825`/`284ae4d`/`8ea6412`/`b929b26`/`df096c2` —— 本分支既有工作：让
    `EventDirector` 强制执行 JSON 声明的触发条件（`required_npc_alive`/
    `city_owner`/`blocked_flags`/`trigger_event` 等），让 `StoryEventEffectApplier`
    把声明的效果真正写回 `GameState`（兼容 camelCase/snake_case，处理嵌套
    city 效果），及对应回归测试。这部分不属于本次改动，仅记录。
  - `7502462` —— 本次新增：修了三处高危正式流程穿帮
    （`GameCalendar.advance()` 年号永不从"建炎"切到"绍兴" / `IntroScreen.kt`
    硬编码"临安行在" / `MockProvider.kt` 硬编码"垂拱殿已议定圣旨"摘要），
    新增 `docs/STAB006_HARDCODE_AUDIT.md` 审计清单，新增 `GameCalendarEraTest.kt`。
  - `d168239` —— 把本任务标 IN_PROGRESS 并记录分支/提交，避免总纲失真。
- PR：#60 → `fix/stab-005-video-player-compat`，Draft，已合入 #59 最终集成线，未合并 main。
- CI：`android-build.yml` 与 `android-debug-apk.yml` 均触发并全部 `success`——
  版本/签名策略检查、视频兼容性检查（STAB-005 成果）、unit tests、Debug APK
  构建、签名指纹校验、APK 上传，每一步都真实跑过，没有 skip（除了只在失败时
  触发的诊断步骤）。
- 测试结果：`GameCalendarEraTest.kt` 6 个用例本地真实 JVM 跑通；整个
  `game`+`agent` 测试套件（含本分支已有的 `BattleDirectiveSystemTest`/
  `BattleOpportunitySystemTest`/`BattleScenePresentationSystemTest`/
  `PalaceTaskSystemRegressionTest` 等）112 个测试全部通过。CI 上的完整
  `testDebugUnitTest` 同样 `success`。
- 遗留问题（已写入 `docs/STAB006_HARDCODE_AUDIT.md`，非高危，本次未修）：
  1. `MockProvider.kt` 硬编码"zhao_ding"作为默认发言人（多处）——下游
     `CourtDebatePanel` 已用 `CharacterAppearanceSystem` 过滤 HIDDEN 人物，
     实际不会展示给玩家；真正修复需要给 `parseEdict()` 加 `GameState` 参数，
     牵连 `AiProvider` 接口和其它 Provider 实现类，留给专门任务。
  2. `VoiceProfiles.kt` 里的 `HistoricalEvent`（`fixedHistory`/
     `intervenableEvents`/`potentialFuture`）是完全没有被任何代码引用的死
     代码，像是给 V1.7 `HIST-001` 准备的设计草稿，启动 `HIST-001` 时应先看
     一眼再决定复用还是清理。
  3. `Faction("jin").rulerName` 与金国唯一有立绘的人物（完颜宗弼）不一致，
     `ROSTER-001`（`docs/ROSTER_EXPANSION_AUDIT.md`）已记录，交叉引用。

## STAB-007 — 正式入口 Smoke Test

状态：`IN_PROGRESS`（`DEVICE_REQUIRED`；正式 BGM 另有 `BLOCKED`）

逐一检查主菜单、序章、皇宫、朝议、地图、国政、军务、人物、城池内景、八宫殿、设置/AI、战役/CG/视频。

完成标准：每个入口能进入、能返回、有真实内容或明确空状态、无崩溃、无明显错误历史信息。

当前记录：
- 最终集成 Draft PR：#59；STAB-006 PR #60 已完整合流，保留 COURT-001 和 ROSTER-001 审计成果。
- 已修主菜单设置路由、真实首都亡国判定、军务返回、Android 分层返回、天命绘卷返回和主菜单 Demo 文案。
- `docs/V162_SMOKE_TEST_MATRIX.md` 共 49 项：**PASS 0 / BLOCKED 1 / DEVICE_REQUIRED 48**；源码、JVM 回归和 APK 拆包不冒充真实手机点击。
- 音频当前状态：8 首用户来源的 BGM 候选已恢复到正式注册路径，格式和 SHA-256 清单可自动校验；它们并非旧版逐字节归档，仍须由玩家真机试听确认，`MEDIA-03` 在确认前保持 `BLOCKED`。
- 自动验收实测：**Android Build #240 PASS；Debug APK #750 PASS；163 个 JVM 测试全部执行，0 失败、0 异常、0 跳过。**
- APK 实测：**朝堂 54/54；应天府专属图存在；H.264/yuv420p 视频 51/51，内嵌音轨 0/51；四季视频 4/4；四季静态 CG 4/4；序章旁白 6/6；BGM 候选 8/8（真机试听待确认）；固定签名一致。**
- 完成真实手机全入口验收和 8 首 BGM 人工试听确认之前不得标 `DONE`，里程碑保持 **6/8**。
- 新增执行中枢与开局 30 分钟发布门：当前先执行 `OPENING-P0-001`，再按真实复现处理朝议卡死、人物时空、音频、授权和存档；专项子任务不增加 37 项主线计数。
- 用户真机报告属于待复现事实，旧 JVM 回归不能证明手机没有卡死；独立真实地图 PR #68 / APK #760 只有在用户认可后才允许选择性…5210 tokens truncated…cceptance` 承接，不直接合 main。

## 2026-08-21 — V1.6.2 预验收集成准备
- COURT-001 两个提交与 ROSTER-001 审计文档整合到独立预验收分支；
- 手工清理 COURT-001 重复、互相矛盾的总纲任务记录，保留 STAB-001～005 全部历史；
- 发现正式剧情 CG 仍在使用 VideoView，已迁移至统一 Media3 播放器并添加静态 CG 保底；
- 建立正式入口 Smoke Test 矩阵、发布风险报告、54 张朝堂素材 SHA 校验、应天首都与实际 APK 视频审计；
- 记录正式 BGM 8/8 缺失、主菜单设置不可达、杭州被错误当成亡国判定首都等真实阻塞；
- Draft PR #59 经 Android Build #226 / Debug APK #738 验证通过，APK 内 54 张朝堂素材、51 条静音 H.264 视频与 6 条旁白全部实测存在。
- 明确记录正式 BGM 0/8、主菜单设置与首都亡国判定等发布阻塞，不允许用 CI 全绿冒充完整真机验收。
- 此条为预验收阶段历史记录；当前进度已由后续 STAB-006 完成与最终集成记录更新，不应重新作为当前状态。

## 2026-08-21 — STAB-006 合流与 STAB-007 正式验收启动
- 核实 Claude PR #60 已改回 `fix/stab-005-video-player-compat` 正确基线，只包含 STAB-006 自身变更，并完整合流 Draft PR #59。
- 保留人物生死/前置事件/旗标/城市归属/概率门控、世界效果真实回写、建炎至绍兴年号切换及全部历史回归。
- 修复主菜单设置提前返回、1127 年错误把杭州当作首都亡国、军务空返回、系统返回分层、绘卷返回和 Demo 文案。
- 新增真实首都与纯导航 JVM 回归，并把 49 项矩阵全部标记正式结果：**PASS 0 / BLOCKED 1 / DEVICE_REQUIRED 48**。
- 集成提交经 **Android Build #230 / Debug APK #741** 完整验证，JUnit XML 证实 **140/140** 全部执行、零失败、零异常、零跳过；APK 审计证实视频 51/51、朝堂 54/54、应天图 1/1、旁白 6/6、BGM 0/8。
- 真实项目文件中未找到此前验收过的 8 首 OGG/MP3，正式 BGM **0/8 BLOCKED**；禁止空文件、旧污染音乐或视频音轨替代。
- STAB-006 保持 `DONE`；STAB-007 保持 `IN_PROGRESS`；V1.6.2 当前进度 **6/8**，总进度 **6/37**；STAB-008 仍为 `TODO`。

## 2026-08-21 — ART-HOTFIX-001 地图现有美术接线
- 根据 Claude PR #61 的只读美术审计，修复 15 条重点城市地图图标路径和 1 条动态回退规则；正式地图首次直接消费 `CityVisualRegistry.mapIconPath`。
- 校验现有地图图标 **16/16**、注册别名 **24 条**、地图装饰 **10 张**、安全接入装饰 **5 张**、地图底图 **6 张**、正式城市背景 **31 张**；不新增图片。
- 前线预警、商路标记、宋金军旗仅由真实城池、地图图层和军团状态触发；鄂州、扬州等特殊文件名背景恢复正式接线，应天府仍为开局首都。
- 新增 7 组地图专项 JVM 回归；**Android Build #232 / Debug APK #743** 实际验证 **147/147** 测试零失败、零异常、零跳过；APK 地图图标 **16/16**、装饰 **10/10**、底图 **6/6**，原有视频、朝堂美术和序章旁白全部保持正确。
- STAB-007 仍为 `IN_PROGRESS`，49 项矩阵与 BGM **0/8 BLOCKED** 不变；V1.6.2 继续保持 **6/8**，不进入 STAB-008。

## 2026-08-21 — WORLD-UX-001 皇帝裁决与天下推演可视化
- 朝议改为臣议单选、多选、综合诸议；朱批前明确展示采纳对象、最终圣意和中文命令；澄清时双重禁止执行，修改圣意保留既有对话上下文。
- 天下推演只根据真实 `GameState` 前后差异展示各势力行军、交战、补给、退兵、城池易手和军团覆灭；地图路线只使用真实道路，支持跳过和本旬纪要回看。
- 修复独立世界回合的历法、季节、天气同步；既有春夏秋冬 **4/4** 静音 Media3 视频与 **4/4** 静态 CG 已进入正式换季演出。
- 保留并行追加的 **8/8** 用户来源 BGM 候选、SHA-256 清单、音频隔离与 CI ffprobe；这些候选仍须真实手机试听，不得冒充已批准正式配乐。
- 新增 **16** 组纯 JVM 回归；**Android Build #240 / Debug APK #750** 证实 **163/163** 测试零失败、零异常、零跳过，并逐项核对 APK 季节视频、季节 CG、BGM、51 条兼容视频、54 张朝堂美术及 6 条旁白。
- 未修改 Claude `DELEGATION-001` 底层授权执行器；STAB-007 保持 `IN_PROGRESS`，49 项矩阵仍为 **PASS 0 / BLOCKED 1 / DEVICE_REQUIRED 48**，V1.6.2 保持 **6/8**，不进入 STAB-008。

## 2026-08-21 — DELEGATION-INTEGRATION-001 皇帝授权制正式集成
- 只提取 Claude Draft PR #64 的 12 个真实后端/测试文件，不整包 merge 其早于 WORLD-UX-001 的历史；保留 #59 既有朝议、地图、四季、序幕音频隔离和 8 首待试听 BGM 候选。
- 长期圣旨进入真实 `ImperialMandate`：可预览负责人、地域、自治档位、准许事项、钱粮预算和禁战底线；朱批后持续生效，可随时撤销，直接皇帝军令优先。
- 宋方授权募兵/修防与模型实际提交的任将、调兵、补给、交战均由本地 validator 裁决；金方沿同一权威军事规则经营，不能凭空增兵、隔空募兵、透支钱粮或绕过禁战边界。
- 修复宗泽开局没有直属军团导致授权无动作的问题；允许在本人真实所在、受宋廷控制的城池募兵成军，并取消被新圣旨覆盖的旧开封瞬移脚本。
- 实际募兵同步扣当地守军/人口与中央钱粮，修城扣钱并修改城防，执行记录真实写入 `GameState`、存档和地图回放；没有真实世界变化的记录不得制造虚假地图演出。
- 不依赖模型额外提议：本地自治可按圣旨主动募兵、修防、补充当地真实粮草、同城接掌无人统领的己方军团，并沿真实道路增援己方前线；绝不为了行动效果自动进入金方城池或发起未授权战争。
- **Android Build #247 / Debug APK #757** 双 CI 实证 **197/197** JVM 回归全部运行、0 失败、0 异常、0 跳过；实际 APK 拆包确认视频 **51/51 H.264/yuv420p/无音轨**、朝堂图 **54/54**、地图图标 **16/16**、四季视频/CG 各 **4/4**、旁白 **6/6**、BGM 候选 **8/8**；固定测试签名和 V1.6.1/versionCode 28 未改变。
- STAB-007 仍保持 `IN_PROGRESS`；49 项矩阵继续 **PASS 0 / BLOCKED 1 / DEVICE_REQUIRED 48**，BGM 待玩家真机试听，V1.6.2 仍为 **6/8**；不合 PR #65、不合 main、不进入 STAB-008。

## 2026-08-21 — GOVERNANCE-001 项目执行中枢与开局发布门

- 正式建立当前任务执行看板、AI 分工交接协议、30 项开局导演验收和可由双 CI 执行的项目治理一致性守卫。
- 区分唯一长期 37 项主路线与当前 P0/P1 执行队列，明确 `OPENING-P0-001` 是唯一下一执行项；用户反馈需按 APK、设备、模型和操作步骤复现，不能用旧测试掩盖。
- 明确真实地图 Draft PR #68 / APK #760 仍是独立待批准原型；它的 213 项测试不得误写为 #59 当前 197 项正式集成回归。
- 修正原发布文档与实际流程冲突：同版本开发构建允许保持 V1.6.1 / versionCode 28，但真正 V1.6.2 正式升级只能在 STAB-008 经批准递增版本，并始终保留固定开发签名。
- Claude 专注历史/人物/状态机与规则，Work 专注集成/复现/QA，豆包仅交付经批准的素材；所有 AI 必须通过同一看板、总纲和受保护文件边界交接。
- 本次只整理仓库治理，不改玩法、人物、资源、音频、地图集成或版本；STAB-007 保持 `IN_PROGRESS`，V1.6.2 保持 **6/8**，总进度保持 **6/37**。
