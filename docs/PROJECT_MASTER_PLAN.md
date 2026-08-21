# 《南渡无悔》项目总纲与持续执行路线

> 本文档是项目唯一主路线图。任何 AI / 开发者接手前必须先读根目录 `AGENTS.md`，再读本文件。
>
> 核心规则：**做完一项，更新一项；任何任务没有写回本文件，就视为没有完整交接。**

最后更新：2026-08-21  
当前可测试版本：**V1.6.1 RC / versionCode 28**  
当前稳定基线：`release/v1.6.1`  
当前主集成线：`fix/stab-005-video-player-compat`（PR #58，基于 STAB-004）  
当前最终集成线：`integration/v1.6.2-preacceptance`（PR #59；STAB-001～006、COURT-001、ROSTER-001 审计和正式入口修复已接入）
并行已验证分支：`feat/court-001-crowd-wiring`（PR #54，已并入预验收线，尚未合 main）
并行审计分支：`docs/roster-001-expansion-audit`（Claude，ROSTER-001 审计文档已并入预验收线）
当前里程碑：**V1.6.2 稳定化与去 Demo 化**  
当前执行任务：**STAB-007 正式入口 Smoke Test（IN_PROGRESS；等待正式 BGM 与真机验收）**
当前里程碑进度：**6 / 8**  
当前总进度：**6 / 37**

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
- 唯一资源阻塞：此前人工验收通过的正式 BGM **0/8**；仓库和当前项目文件均无法访问原始 OGG/MP3。
- 自动验收实测：**Android Build #230 PASS；Debug APK #741 PASS；140 个 JVM 测试全部执行，0 失败、0 异常、0 跳过。**
- APK 实测：**朝堂 54/54；应天府专属图存在；H.264/yuv420p 视频 51/51，内嵌音轨 0/51；序章旁白 6/6；正式 BGM 0/8；固定签名一致。**
- 完成真实手机全入口验收和正式 BGM 恢复之前不得标 `DONE`，里程碑保持 **6/8**。

## STAB-008 — V1.6.2 真机验收与发布收口

状态：`TODO`

工作：版本号递增、固定签名、两套 CI、APK 拆包、音视频资源核对、真机测试、总纲更新、合 main 前形成验收记录。

完成标准：V1.6.2 成为稳定基线，不再带明显 Demo 入口和空白页。

---

# 五、V1.7.0 — 动态历史核心

目标：从历史场景演示升级为真正的历史条件系统。

## HIST-001 — 历史事件统一数据模型
状态：`TODO`  
建立 `HistoricalEventDefinition`：id、时间窗口、required/suppress 条件、参与者、地点、触发分数、选择、worldMutations、replacementEventIds、historicalNote。

## HIST-002 — 历史事件 Trigger Engine
状态：`TODO`  
每旬依据当前世界判断候选，支持满足、延迟、抑制、变体、替代、永不发生。

## HIST-003 — 人物动态履历与官职系统
状态：`TODO`  
支持当前/历任官职、任命来源、日期、机构、军职文职并存、罢免贬谪召回、玩家改史。

## HIST-004 — 战役由世界态势“长出来”
状态：`TODO`  
顺昌作为第一套标准样板：真实敌军、真实城池/军团、守将由世界决定，可提前增援/撤守，可根本不发生，战后改写世界。

## HIST-005 — 历史偏离与替代事件
状态：`TODO`  
例如金国提前衰弱 -> 北伐/议和；岳飞提前重用 -> 不同战区；秦桧未崛起 -> 主和替代人物；提前收复中原 -> 治理与财政新问题。

## HIST-006 — 历史偏离度与世界记忆
状态：`TODO`  
记录偏离锚点、原因、责任者，并让 AI 理解当前世界已经不同于史书。

## HIST-007 — 动态军报/奏疏入口
状态：`TODO`  
战役通过军报、枢密院急奏、将领奏折、地图预警、AI 主动进言进入玩家视野，不再主页固定按钮。

## HIST-008 — 动态历史回归测试
状态：`TODO`  
至少覆盖历史近似路线、金国提前战败、刘锜不在/已死、顺昌已失守、玩家提前重兵部署五类分支。

---

# 六、V1.8.0 — 活世界与人物社会

## WORLD-001 — World Roster 正式并入主循环
状态：`TODO`  
数百/上千人物按 CORE / ACTIVE / BACKGROUND 分层计算。

## WORLD-002 — 人物位置与旅行时间统一
状态：`TODO`  
召见、赴任、出征、回京全部真实旅行，禁止瞬移。

## WORLD-003 — 人物目标/计划/记忆长期化
状态：`TODO`  
人物自主升迁、结交、争斗、请战、请辞、求援、隐瞒、投机。

## WORLD-004 — 派系与关系网络深化
状态：`TODO`  
主战/主和之外加入地域、军头、文官、宗室、外戚、士族、商人等网络。

## WORLD-005 — 多国/地方势力持续 AI 回合
状态：`TODO`  
宋、金、西夏、大理、高丽、草原、地方豪强、叛军持续行动。

## WORLD-006 — NPC 主动事件密度
状态：`TODO`  
玩家不操作时仍持续收到奏疏、军报、弹劾、举荐、求援、外交、传闻。

## WORLD-007 — 信息可见性与谣言
状态：`TODO`  
玩家、人物、国家均非全知；情报有延迟、误差和欺骗。

## WORLD-008 — 活世界性能预算
状态：`TODO`  
低成本模型可玩：限制每旬模型调用，本地规则承担大部分模拟。

---

# 七、V1.9.0 — 战略深度与国家治理

## STRAT-001 — 财政闭环
状态：`TODO`  
税、商贸、军费、赈济、建设、腐败、物价压力。

## STRAT-002 — 粮草与补给线深化
状态：`TODO`  
军队依赖真实补给线，补给可被切断。

## STRAT-003 — 城市人口/民心/灾害
状态：`TODO`  
战争、征税、灾荒、流民影响城市和兵源。

## STRAT-004 — 外交与条约
状态：`TODO`  
停战、和议、岁币、互市、结盟、使节、俘虏交换。

## STRAT-005 — 情报/谍战体系
状态：`TODO`  
皇城司、边探、敌谍、假情报、反间。

## STRAT-006 — 官僚执行力
状态：`TODO`  
圣旨不是瞬时魔法，官员能力、忠诚、派系影响执行。

## STRAT-007 — 战争政治后果
状态：`TODO`  
大胜、大败、弃城、北伐、议和持续重塑朝局与人物关系。

---

# 八、V2.0.0 — 完整长期体验

## POLISH-001 — 新手引导与信息层级
状态：`TODO`

## POLISH-002 — UI/交互统一与空状态清零
状态：`TODO`

## POLISH-003 — 全量音视频兼容与资源 QA
状态：`TODO`

## POLISH-004 — 存档兼容与迁移系统
状态：`TODO`

## POLISH-005 — 长局稳定性 / 性能 / AI 成本测试
状态：`TODO`

## POLISH-006 — 正式发布流程与生产签名
状态：`TODO`

---

# 九、并行辅助任务（不计入 37 项主线）

这些任务允许与 V1.6.2 主线并行，但不得抢同一核心文件。若并行任务也修改本总纲，集成时必须人工保留双方记录。

## COURT-001 — 朝会列班视觉接线
状态：`DONE`（Claude，独立分支已验证；已并入 `integration/v1.6.2-preacceptance`）
目标：利用现有普通官员素材与群像图提升大殿“百官列班”观感，不伪造正式 Officer 状态，不改主战役逻辑。

完成：
- `CourtOfficerRow` 增加 6 张殿内群像背景层，按当前旬数稳定选图；
- 8 张无脸背影列班素材按旬数稳定洗牌取 6 张作为纯装饰，不可点击、不映射具体 Officer；
- 前景正式人物仍只来自 `canAppearInPalace` 的真实状态过滤；
- 接入 `feature/living-world-court-v1` 已存在的 54 张普通官员/朝会素材与 `ArtResourceRegistry.CourtNpc` 注册；
- 应天府“缺图”确认实际是接线路径错误：已有 `images/cities/city_yingtianfu.webp` 未被注册表读取，已移到正确路径并接入；
- `MapData.kt` 中首都标记从临安纠正为应天府，和 `InitialData.kt` 对齐；
- `CityVisualRegistry.kt` 同步把应天府设为 CAPITAL、临安降为普通重镇；
- 主殿中写死“临安行在”的文字/内容描述改为中性历史表述，避免 1127 开局穿帮。

记录：
- 分支：`feat/court-001-crowd-wiring`
- PR：#54 → `release/v1.6.1`（独立 Draft PR，未合并）
- 关键提交：`4faa3e506838e3de36c65ff5d4b15e8d9d367a94`（另含 cherry-pick 素材提交）
- Claude 本地 JVM：76 个测试通过；
- CI：**Android Build #215 / run 32445288756 PASS**；
- 真机专项：尚未验收，待 STAB-007/STAB-008 与主集成线一起验收。

注意：COURT-001 曾在总纲中额外插入第二条同名 `IN_PROGRESS` 记录；预验收集成时已手工消除重复，并保留 STAB 主线和本处完整 DONE 记录。最终合入 STAB-006 时仍需手工合并总纲，禁止覆盖另一方进度。

## ART-001 — 首都与核心城池背景修复
状态：`IN_PROGRESS`  
应天府专属城池图接线已由 COURT-001 完成；后续仅继续补真正缺失的高频核心城池，不再把应天府当作缺素材任务。

## ROSTER-001 — 正式人物池扩容规划
状态：`DONE`（Claude，PR #57；审计文档已并入预验收线）
交付：`docs/ROSTER_EXPANSION_AUDIT.md`，盘点当前 12 名正式 Officer、6 名有立绘无实体人物、金国将领缺口，并提出 35 名候选人物的分层规划。仅整合文档，未把候选人物写入正式 Officer 数据；`V162PreacceptanceRegressionTest` 固定验证正式人物仍为 12 人。

## V162-INTEGRATION-PREP — V1.6.2 集成与 Smoke Test 准备
状态：`DONE`（Codex，PR #59；预验收准备完成，不代表 STAB-007 已完成）
范围：以 STAB-005 为基线整合 COURT-001 与 ROSTER-001 审计；建立 `docs/V162_SMOKE_TEST_MATRIX.md`、`docs/V162_PREACCEPTANCE_RISKS.md`、独立预验收 JVM 测试与源资源 / APK 审计脚本。
边界：不修改 STAB-006 历史事件核心，不修改版本号和签名，不把 STAB-007 提前标 DONE，不合 main。
新增发现：正式剧情 CG 曾残留 `VideoView`，已统一迁移到 Media3；8 首正式 BGM 未入仓库、主菜单设置被提前返回挡住、亡国判定仍固定杭州，详见风险文档。
完成条件：独立 Draft PR、两套 CI 通过、54 张朝堂图片与应天图进入 APK、51 条 V3 视频为 H.264/yuv420p/无内嵌音轨、固定测试签名保持不变。
完成记录：
- Draft PR：#59 → `fix/stab-005-video-player-compat`；集成提交：`659e7845129338f4ebcfdd5406ea83162281a05c`。
- 两套验证：**Android Build #226 PASS；Build Nandu Debug APK #738 PASS；全量 unit tests PASS；APK 构建 PASS；固定 V1.6.1 测试签名 PASS。**
- APK 实测：**朝堂素材 54/54 SHA-256 一致；应天府专属图存在；V3 视频 H.264 51/51、yuv420p 51/51、内嵌音轨 0/51；序章旁白 6/6。**
- 明确发布阻塞：正式已审核 BGM **0/8**，不得把当前 CI APK 当成完整有音乐的 V1.6.2 交付。
- 当时识别的主菜单设置、首都判定、军务返回和 Demo 文案已由后续最终集成修复；系统返回手势仍需真机验证。
- Claude 的 STAB-006 PR #60 已修正为 STAB-005 基线，并已合流当前最终集成线。
- 当前正式状态以 STAB-006 `DONE`、STAB-007 `IN_PROGRESS`、V1.6.2 **6/8** 为准；预验收时期的旧交接状态不再有效。

## V162-FINAL-INTEGRATION-STAB007 — V1.6.2 最终集成与正式入口验收
状态：`DONE`（本轮最终集成与自动验收交付已完成；正式主任务 STAB-007 仍为 `IN_PROGRESS`，不得混淆）
范围：完整接收 STAB-006，并保留 COURT-001、ROSTER-001 审计、49 项入口矩阵、54 张朝堂素材、应天府图、统一 Media3 和 APK 视频审计。
新增修复：主菜单设置正确进入/返回；亡国只依据真实行在；军务左上与 Android 返回手势真实回退；天命绘卷可用系统返回；正式主菜单删除 Demo 占位。
回归守卫：5 组真实首都亡国测试、6 组纯 JVM 导航测试、49 行状态矩阵校验、8 个 BGM 槽位与 APK 数量检查。
正式验收：**PASS 0 / BLOCKED 1 / DEVICE_REQUIRED 48**；BGM **0/8**；不改版本、不改签名、不合 main、不启动 STAB-008。
实际验证：**Android Build #230 PASS；Debug APK #741 PASS；140/140 JVM 测试通过且 0 跳过；APK 视频 51/51、朝堂 54/54、应天府图 1/1、旁白 6/6、BGM 0/8。**

## ART-HOTFIX-001 — 地图现有美术接线修复
状态：`IN_PROGRESS`（STAB-007 前低风险视觉修复；当前双 CI 待验证，不改变正式里程碑进度）
审计依据：Claude `ART-001` Draft PR #61；只读取审计结果，不合并其旧基线历史，不新增图片、不增加玩法。
修复范围：15 个重点城市 `mapIconPath` 改回真实 `images/map/icons/`；动态城市/宋金西夏大理首都回退统一走现有图标注册表；正式 `MapScreen` 真正消费 `mapIconPath`。
现有资源：**地图图标 16 张 / 注册别名 24 条；地图装饰 10 张，其中 5 张已按真实状态安全接入；地图底图 6 张；正式城市背景 31 张。**
安全接线：选中框、真实前线预警、真实商路节点、真实宋军旗和金军旗；鄂州、扬州等特殊命名城池背景改走正式注册表，应天府专属图和 1127 首都状态继续保持正确。
暂缓接线：雾层、山脉标签、河流标签、区域牌、路线箭头。这些图片实际为半透明方框标记，缺少成熟的全屏雾层、世界坐标锚点或真实路线方向槽位，硬接会遮挡地图或制造虚假状态。
剩余风险：开局 36 座实际城池中 18 座尚无正式专属背景，继续使用既有通用背景；不混用 `images/cities/` 旧候选图，不生成新图。
保护边界：STAB-007 保持 `IN_PROGRESS`；49 项矩阵保持 **PASS 0 / BLOCKED 1 / DEVICE_REQUIRED 48**；V1.6.2 保持 **6/8**，总进度 **6/37**；正式 BGM 仍为 **0/8 BLOCKED**。

---

# 十、任务更新规则

每完成一项任务，执行者必须：

1. 将状态从 `NEXT/IN_PROGRESS` 改为 `DONE`；
2. 写明日期、分支、commit/PR、测试、APK/真机结果、遗留问题；
3. 更新里程碑完成/剩余数量与总进度；
4. 将下一项 `TODO` 改为 `NEXT`；
5. 用户改变路线时，先修改总纲再动代码；
6. 未通过测试/CI 的任务禁止标 `DONE`；
7. 正式集成前检查是否与其他 AI 并行分支冲突；
8. 并行任务修改过总纲时，集成者必须手工合并记录，禁止整文件覆盖导致另一条工作的进度消失。

禁止：做完代码不更新路线图；为了显得快把未测试任务标 DONE；重复实现已完成系统；把测试 Demo 当正式玩法继续堆功能。

---

# 十一、交接模板

```text
【南渡项目交接】
完成任务 ID：
当前版本：
工作分支：
Commit / PR：
完成内容：
测试结果：
APK/真机结果：
遗留问题：
本总纲是否已更新：是/否
下一任务 ID：
下一任务第一步：
```

---

# 十二、变更日志

## 2026-08-21 — 建立统一主路线图
- 将后续开发拆为 V1.6.2 / V1.7 / V1.8 / V1.9 / V2.0，共 37 项；
- 确立“历史锚点而非铁轨”“世界状态唯一真相源”“玩家决策必须回写世界”。

## 2026-08-21 — STAB-001 / STAB-002 完成
- 顺昌入口改为动态门控；
- 战役日期、人物、官职、兵力改为实时状态；
- 移除固定顺昌 Demo 正式路径；
- 集成 PR #53 通过 Android Build #214 / Debug #728。

## 2026-08-21 — STAB-003 完成
- 顺昌三类御前军令正式回写 GameState；
- 驰援复用真实行军系统，不瞬移、不造兵；
- 军令进入 Chronicle 并通过存档往返测试；
- PR #55 通过 Android Build #217 / Debug #730；
- V1.6.2 进度推进至 3/8，下一项 STAB-004。

## 2026-08-21 — STAB-004 完成
- 修复宫殿装饰框参与测量导致真实待办被挤出屏幕；
- 八宫殿 badge/list 一致性和明确空状态加入回归测试；
- 清除机械内廷假待办，按真实世界压力生成；
- 修复多组宫殿 choice id 与后果系统错位，并补财政审计/人才试用/外贸真实后果；
- PR #56 通过 Android Build #218 / Debug #731；
- V1.6.2 进度推进至 4/8，下一项 STAB-005。

## 2026-08-21 — STAB-005 完成
- 正式视频播放统一迁移 Media3/ExoPlayer，序章不再保留独立 VideoView；
- 视频失败按资源/数据源/格式/解码器/播放器分类，静态 CG 永远保底；
- 审计确认 51/51 条 V3 源 MP4 为 HEVC/H.265；
- Android 构建前自动转 H.264 Main/yuv420p/无内嵌音轨，再由 ffprobe 全量 QA；
- Android Build #225 / Debug APK #737 全绿；
- #737 APK 拆包再验：51/51 条打包视频均 h264+yuv420p、无内嵌音轨；
- V1.6.2 进度推进至 5/8，下一项 STAB-006。

## 2026-08-21 — COURT-001 独立完成
- 朝会群像/侧翼列班接线完成，真实 Officer 仍按状态过滤；
- 应天府既有城池图接线路径修复，首都数据/地图/视觉分级统一；
- PR #54 / Android Build #215 验证通过；
- 已由 `integration/v1.6.2-preacceptance` 承接，不直接合 main。

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
- 新增 7 组地图专项 JVM 回归；全部测试声明数从 140 增至 **147**；等待本轮双 CI 和 APK 地图资源验证。
- STAB-007 仍为 `IN_PROGRESS`，49 项矩阵与 BGM **0/8 BLOCKED** 不变；V1.6.2 继续保持 **6/8**，不进入 STAB-008。
