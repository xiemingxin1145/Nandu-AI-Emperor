# docs/ 统一文档索引

> 本文件是 `docs/` 目录的唯一入口。新接手的 AI / 开发者先读根目录 `START_HERE.md` 与 `AGENTS.md`，再按本索引找专项文档。
>
> 建立于 REPO-GOVERNANCE-001（2026-08-21）。新增文档时请在对应分类下补一行说明。

## 当前版本与发布

| 文档 | 用途 |
| --- | --- |
| `RELEASE_POLICY.md` | Android 版本号递增、固定测试签名、构建交付硬规则；做任何 APK 前必读 |
| `OPEN_SOURCE_PASS_STATUS.md` | 开源音频/框架验收路线的状态标记（CC0 素材落盘、voice 目录留空原因） |

## Roadmap / 项目总纲

| 文档 | 用途 |
| --- | --- |
| `PROJECT_MASTER_PLAN.md` | **项目唯一主路线图**：里程碑、37 项任务状态、完成标准与交接规则；进度只以本文件为准 |
| `GAMEPLAY_GAPS_ROADMAP.md` | 玩法缺口路线图：已有能力盘点 + P0/P1/P2 缺口表（结论：优先补玩法闭环，不优先堆美术） |
| `REPO_GOVERNANCE_001_REPORT.md` | 2026-08-21 仓库治理报告：23 个 PR 审计归档、治理一致性检查、仓库卫生清单 |

## AI / 世界模拟

| 文档 | 用途 |
| --- | --- |
| `AI_WORLD_ARCHITECTURE.md` | AI 原生世界架构总纲：权威世界状态、代码负责现实 / AI 负责灵魂 |
| `AGENT_INTEGRATION_PLAN.md` | 游戏内 AI 军师 / Agent 三层结构（观察-计划-执行）融合方案 |
| `OPEN_SOURCE_REFERENCES.md` | 开源架构参考清单（AI Town 等），只吸收思想、不整库搬运 |

## 朝议 / 皇帝系统

| 文档 | 用途 |
| --- | --- |
| `COURT_COUNCIL_AND_PALACE_EVENTS.md` | 大殿朝会与后苑内廷事件设计：任务卡升级为角色上奏 + 皇帝裁断 |
| `COUNCIL_UI_FRAME_WIRING.md` | 朝会/宫殿待办 UI 框体资源接线说明 |
| `NPC_FACTION_MEMORY.md` | NPC 派系与长期记忆骨架：朝臣态度随裁断累积 |
| `DIPLOMACY_TRADE_STATE.md` | 外交与外贸持久化骨架（西夏/大理/高丽/海贸，暂编码于 Rumor 列表） |

## 军事

> 军事/战役专项文档目前集中在总纲的 STAB / HIST 任务章节与 PR 审计记录中（STAB-001~006、HIST-004），docs/ 暂无独立军事文档。战役门控实现见 PR #53/#60。

## 地图

| 文档 | 用途 |
| --- | --- |
| `MAP_LAYERS_AND_SCREEN_SPLIT.md` | 山河图层与页面职责拆分：解决山河/军务/国政内容重叠 |
| `MAP_REAL_CITY_ICONS.md` | 真实城池图标叠层（MapIconOverlay）实现说明 |
| `MAP_VISUAL_AND_APPEARANCE_GATING.md` | 地图视觉基础（底图/城池/道路）与人物登场门槛 |
| `MAP_AND_COUNCIL_ART_WIRING.md` | 山河地图与朝会人物美术接线记录 |
| `CITY_ICONS_UI_FRAME_WIRING.md` | 城池图标与 UI 框体纳入统一资源注册表 |
| `WORLD_MAP_EXPANSION_PLAN.md` | 世界地图分阶段扩展规划（不一次塞满全中国） |

## 历史事件

| 文档 | 用途 |
| --- | --- |
| `NANDU_EVENT_EDITOR_SPEC.md` | 事件编辑器与数据包规范：条件触发→选择→数值/地图/历史线变化 |
| `STORY_DESIGN_V1.md` | 剧情企划 V1.0：主菜单、序章、建炎元年节点设计 |
| `story_phase_table.md` | 剧情阶段表：各章回合节奏、核心事件与结束条件 |
| `story_import_report.md` | 建炎初期剧情事件库（20 事件）导入报告 |

## 人物

> docs/ 暂无独立人物文档。人物池扩容审计在 Draft PR #57 的 `ROSTER_EXPANSION_AUDIT.md`（未合入）；人物状态原则见总纲 2.2 与 `AGENTS.md` 设计原则。

## 美术

| 文档 | 用途 |
| --- | --- |
| `ASSET_WIRING_V1.md` | 资源接线 v1：让已导入资源包真正被 UI 命中 |
| `PROP_ART_BRIEF_V1.md` | 道具美术批次 V1：18 件道具规范与命名约定 |
| `art_cg_requirements.md` | 事件 CG 美术需求清单（P0/P1 分级） |
| `art_upload_report.md` | 美术资产上传报告（V0.6.1 批次记录） |
| `event_cg_mapping.md` | 事件 CG 映射表（event_id → 文件路径） |
| `EVENT_CG_CONTEXT_WIRING.md` | 事件 CG 上屏与上下文匹配逻辑 |
| `seven_factions_art_import_report.md` | 七大势力美术压缩导入包说明 |
| `VISUAL_ASSET_LICENSES.md` | 视觉资产授权说明（AI 生成批次记录） |

## 音频

| 文档 | 用途 |
| --- | --- |
| `AUDIO_TASKS.md` | 音频素材任务单：真素材替换清单 + 授权铁律（CC0 优先） |
| `AUDIO_ASSET_SHOPPING_LIST.md` | 音频素材采购/寻找清单（按目录与文件名约定） |
| `AUDIO_LICENSES.md` | 音频素材来源与授权总记录 |
| `THIRD_PARTY_AUDIO.md` | 第三方音频来源追踪（SHA-256 固定，防上游漂移） |
| `AUDIO_SETTINGS_CONTROLS.md` | 音频设置控制（总开关/音量持久化） |
| `AUDIO_ASSET_AUDIT_20260820.md` | 音频资产排雷审计（APK 解包 + ffprobe） |
| `AUDIO_CLEANROOM_STATUS.md` | Audio Cleanroom 集成状态：旧 BGM 清仓、视频静音、宫殿路由 |
| `audio_code_integration.md` | 音频代码骨架接入说明（V0.6.6） |
| `audio_compose_hooks.md` | Compose 音频钩子用法（PlayBgmEffect / PlaySfxEffect） |
| `audio/BGM_V161_MAPPING.md` | V1.6.1 八个 BGM 槽位与源文件映射（含大文件上传限制记录） |

## QA / 测试

| 文档 | 用途 |
| --- | --- |
| `VIDEO_COMPAT_AUDIT_V3.md` | V3 视频兼容审计：51 视频全 HEVC，编码风险与对策 |
| `build_fix_log.md` | 构建修复日志：CI 连环失败的根因记录（历史档案） |

## 原型 / 未来设计

> 双层天下地图架构（MAP_DUAL_LAYER_ARCHITECTURE）与寰宇原型说明在 Draft PR #65 分支（未合入）；World Roster 底座设计在 Draft PR #38 分支（未合入）。合入后请迁移至本分类。
