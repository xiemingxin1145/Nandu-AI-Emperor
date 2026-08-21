# 南渡无悔 · AI原生南宋皇帝沙盒 — 多模型可插拔AI引擎 + 御笔下诏系统

> 你写圣旨，AI理解；群臣反应，地图变化；历史反噬，天下推进。

> **AI / 开发者接手项目请先阅读：** `AGENTS.md` → `docs/PROJECT_MASTER_PLAN.md` → `docs/RELEASE_POLICY.md`。  
> 项目总进度、当前下一任务、剩余任务数与交接记录以 `docs/PROJECT_MASTER_PLAN.md` 为唯一准绳。每完成一项必须同步更新总纲。

## 项目名称

**南渡无悔 · AI皇帝引擎**  
> 你写圣旨，AI理解；群臣反应，地图变化；历史反噬，天下推进。

## 当前状态（V1.6.1 RC）

当前处于 **V1.6.2 稳定化 / 去 Demo 化** 前置阶段。已有 AI 引擎、军队/战争、Faction Brain、Character Agent、Living World、宫殿、美术与音频等基础，但历史战役、人物履历、宫殿待办、视频播放和正式入口仍存在结构性问题。

后续不再以本 README 的旧版本表为开发依据；完整路线、任务编号、完成标准和下一步统一见：

- `AGENTS.md` — AI/开发者强制接手规则；
- `docs/PROJECT_MASTER_PLAN.md` — 唯一项目总纲与任务状态；
- `docs/RELEASE_POLICY.md` — Android 版本号、签名、CI 与 APK 交付硬规则。

## 当前重点能力

- **AI 圣旨解析**：玩家输入自由文本圣旨，AI 返回统一 `EdictResult` JSON。
- **本地规则裁决**：所有 AI 命令必须经过 Kotlin 白名单与 `GameRuleEngine`，AI 不允许直接改数值。
- **多模型入口**：保留 Claude / OpenAI / Gemini / OpenRouter / 自定义 OpenAI-compatible / Mock 的 Provider 架构。
- **AI 引擎中枢**：用户可在 App 内配置 Provider、API Key、模型名、自定义 Base URL，并进行连接测试。
- **Living World**：人物位置、历史 Canon、世界 AI 信息边界已有基础实现。
- **Stage 4-8**：军队/行军/补给、战争、AI World Engine、Faction Brain、Character Agent 已有初版。
- **地图与城池系统**：包含山河图、城池内景、城防营建、募兵、攻城等玩法。
- **宫殿与朝议**：八宫殿和待办框架已接入，V1.6.2 将优先清理空壳与演示逻辑。
- **音频系统**：BGM / SFX / 环境声 / 序章旁白分层接入；正式音频映射见 `docs/audio/`。
- **V3 美术资产**：角色、CG、UI 纹理和视频已打包进入 Android assets。

## 架构概览

玩家写圣旨（自由文本）
    ↓
AiProvider（可插拔：Claude / OpenAI / Gemini / OpenRouter / 自定义 / Mock）
    ↓
统一 EdictResult JSON
    ↓
Kotlin 校验 + 命令白名单过滤
    ↓
GameRuleEngine（本地规则裁决，AI 不能直接改数值）
    ↓
GameState 更新 + 起居注记录 + NPC 奏议展示

- **核心流程**：玩家输入圣旨 → AI 解析为结构化 JSON → 本地校验与白名单过滤 → `GameRuleEngine` 执行业务规则 → 更新 `GameState` 并记录起居注。

## AI Provider 支持

| Provider | 状态 | 说明 |
| --- | --- | --- |
| Claude | 已实现 | 可用于圣旨理解与群臣奏议 |
| OpenAI-compatible | 已实现基础 | 支持自定义 Base URL / 中转站 |
| Gemini | 可通过兼容入口接入 | 以实际接口兼容性为准 |
| OpenRouter | 可通过兼容入口接入 | 适合低成本模型测试 |
| Mock 离线 | 已实现 | 无需 Key，用于离线试玩和开发测试 |

> 实际可用性以 App 内“AI 引擎中枢”的连接测试结果为准。

## 统一 JSON 协议（EdictResult）

所有模型必须返回统一结构；AI 只生成候选命令和文本，本地规则层负责最终裁决。

## 命令白名单与安全原则

AI 无法直接修改世界数值。所有命令最终由本地规则引擎校验、执行并写回 `GameState`。

历史系统同样遵循这一原则：历史事件只能根据当前世界状态成为候选，不能因为现实历史“后来发生过”就无条件强制发生。

## 构建与部署

- 通过 GitHub Actions 自动跑单元测试和 Debug APK 构建；
- `release/**` 分支必须遵守 `docs/RELEASE_POLICY.md`；
- V1.6.1 起固定测试签名，后续测试 APK 不允许随意换证书；
- 每次交付 APK 前必须核对版本号、签名、资源打包和关键入口。

## 项目路线

路线图不再复制到 README，避免出现多个互相冲突的版本。请只维护：

**`docs/PROJECT_MASTER_PLAN.md`**

其中当前规划为：

- V1.6.2：稳定化 / 去 Demo 化；
- V1.7.0：动态历史核心；
- V1.8.0：活世界与人物社会；
- V1.9.0：战略深度与国家治理；
- V2.0.0：完整长期体验。

---

## 美术资产库 `assets/`

V3 美术资产包含角色、背景、CG、UI 纹理和视频。资源是否真正可用于正式流程，以当前代码接线、设备兼容性和 `PROJECT_MASTER_PLAN` 对应任务验收为准；“资源存在”不等于“玩法完成”。

---

**项目核心一句话：**  
《南渡无悔》要做的不是“按时间播放南宋历史”，而是建立一个以 1127 为起点、由玩家决策与 AI 势力共同推进、可以真正改写历史的活世界。
