# 南渡无悔 · AI原生南宋皇帝沙盒 — 多模型可插拔AI引擎 + 御笔下诏系统

> 你写圣旨，AI理解；群臣反应，地图变化；历史反噬，天下推进。

## 项目名称

**南渡无悔 · AI皇帝引擎**  
> 你写圣旨，AI理解；群臣反应，地图变化；历史反噬，天下推进。

## 当前状态（V1.4.x）

本项目已经从早期 Mock AI + 静态地图阶段，推进到 **AI 引擎中枢 + 宫殿主页 + 城池经营 + 地图战局 + 音频接线 + 朝堂 NPC 奏议** 的综合原型阶段。

当前重点能力：

- **AI 圣旨解析**：玩家输入自由文本圣旨，AI 返回统一 `EdictResult` JSON。
- **本地规则裁决**：所有 AI 命令必须经过 Kotlin 白名单与 `GameRuleEngine`，AI 不允许直接改数值。
- **多模型入口**：保留 Claude / OpenAI / Gemini / OpenRouter / 自定义 OpenAI-compatible / Mock 的 Provider 架构。
- **AI 引擎中枢**：用户可在 App 内配置 Provider、API Key、模型名、自定义 Base URL，并进行连接测试。
- **宫殿模拟器入口**：皇宫主页已升级为垂拱殿、文德殿、枢密院、政事堂、御书房、皇城司、后苑内廷、太庙等宫殿入口。
- **朝堂 NPC 奏议**：岳飞、赵鼎、秦桧、李纲、韩世忠等人物通过 `npcResponses` 参与奏对。
- **地图与城池系统**：包含山河图、城池内景、酒楼走访、城防营建、募兵、攻城等玩法。
- **音频系统**：真实接线的 BGM / SFX / 环境音系统已接入，素材替换清单见 `docs/AUDIO_TASKS.md`。

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
| Claude | 已实现 | 推荐，圣旨理解与群臣奏议效果最好 |
| OpenAI | 接入中 | 走 OpenAI-compatible 聊天补全协议 |
| Gemini | 预留 | 可通过自定义 OpenAI-compatible 中转接入 |
| OpenRouter | 接入中 | 可路由多模型，适合测试不同模型 |
| 自定义 API | 接入中 | 面向第三方 OpenAI-compatible 中转站 |
| Mock 离线 | 已实现 | 无需 Key，用于离线试玩和开发测试 |

> 实际可用性以 App 内“AI 引擎中枢”的连接测试结果为准。

## 统一 JSON 协议（EdictResult）

所有模型必须返回以下结构：

```json
{
  "summary": "一句话摘要",
  "commands": [
    {
      "type": "dispatch_army",
      "officerId": "..."
    }
  ],
  "npcResponses": [
    {
      "officerId": "yue_fei",
      "attitude": "support",
      "text": "..."
    }
  ],
  "riskTags": ["grain_pressure"],
  "confidence": 0.9,
  "clarificationNeeded": false,
  "clarificationHint": ""
}
```

## 命令白名单

支持的命令类型（AI 只能使用这些，否则丢弃）：

- `dispatch_army`
- `assign_officer`
- `repair_city`
- `raise_grain`
- `suppress_officer`
- `reward_officer`
- `punish_officer`
- `move_capital`

> **安全机制**：AI 无法直接修改数值，所有命令最终由本地 `GameRuleEngine` 裁决。

## NPC 角色原则

所有 AI 角色必须符合南宋朝堂语境与古代思维：

- 不说现代网络词、现代组织管理话术、现代经济学口吻。
- 说话围绕君臣名分、社稷安危、粮道、军心、民力、边防、朝局、祖宗法度。
- 岳飞偏忠烈主战；赵鼎偏稳重理财、先问钱粮；秦桧偏主和、借“民力/社稷”表达风险；李纲偏刚烈守城、重国耻；韩世忠偏豪勇直爽、军伍口吻。
- `npcResponses.text` 应像奏对，不像聊天机器人解释。

## 关键类与流程组件

- **`GameState`**：游戏状态核心，记录城池、军队、官员、粮草等信息。
- **`GameRuleEngine`**：本地规则引擎，负责校验命令合法性、执行业务逻辑、防止 AI 越权。
- **Edict 处理流程**：
  1. 玩家输入圣旨（自由文本）
  2. 调用 `AiProvider` 解析为 `EdictResult` JSON
  3. Kotlin 层校验 JSON 结构
  4. 过滤非白名单命令
  5. `GameRuleEngine` 执行业务规则
  6. 更新 `GameState`
  7. 记录起居注（历史日志）

## 构建与部署

- **构建方式**：通过 GitHub Actions 自动构建 Debug APK。
- 下载路径：查看项目 Actions 页面。

## 版本路线图

| 版本 | 状态 | 功能 |
|------|------|------|
| **V1.4.1** | 已完成 | 真实音频系统接线、BGM/SFX/环境音、同类音效随机播放 |
| **V1.4.2** | PR 中 | AI 引擎中枢、本机配置保存、八宫殿主页、垂拱殿 NPC 奏议卡牌 |
| **V1.5** | 下一步 | 每个宫殿独立背景图槽位、固定 NPC 站位、奏议气泡、宫殿待办系统 |
| **V1.6** | 计划 | 文德殿任官招贤、枢密院军报、政事堂钱粮、御书房密折、皇城司暗线 |
| **V2.0** | 目标 | 完整 AI 原生南宋皇帝单机模拟器体验 |

## 集成点（供扩展）

- **AI 集成**：通过 `AiProvider` 接口替换或扩展模型（支持 OpenAI-compatible 格式）。
- **事件系统**：`npcResponses` 与 `riskTags` 可触发事件（如粮草危机、官员态度变化）。
- **宫殿系统**：每个宫殿应有独立背景图槽位、待办入口、专属 NPC/事件。
- **音频/多媒体**：已接入（见下文音频系统）；`npcResponses` 中的 `text` 可进一步扩展为语音合成输入点。
- **规则扩展**：`GameRuleEngine` 支持新增业务规则，控制命令执行条件。

## 音频系统（V1.4.1，已接线）

游戏音频由 `audio/GameAudioPlayer.kt` 驱动（SoundPool 播短音效 + 双路 MediaPlayer：BGM 与环境音并行），所有槽位在 `game/AudioResourceRegistry.kt` 统一登记，Compose 钩子见 `ui/components/GameAudioEffects.kt`。已接入的触发点：

- **BGM 随场景切换**：皇宫/朝议/国政（court）、山河/军务/城内（map）、开局（main_menu）、胜利/亡国结局。
- **攻城战**：战鼓 + 号角齐鸣。
- **城内**：市井环境音，进酒楼切人声喧闹；走访得情报时提示音。
- 同一音效支持多变体随机播放（`playSfxVariant`），同名加 `_2`/`_3` 后缀即自动进随机池。

当前 `assets/audio/` 内是程序合成的**零版权占位音效**（缺文件静默不崩，换一个生效一个）。真素材替换清单、授权要求、免费来源、中文叫阵 TTS 指引，统一见 **`docs/AUDIO_TASKS.md`**。

> 注：早前另一支线引入的 `com.nanduaiemperor.audio.SoundManager` 因放在不参与编译的 `src/main/kotlin/` 目录、包名与项目（`com.xiemingxin.nandu`）不符、且无任何素材与接线，已移除；其有价值的素材来源链接已并入 `docs/AUDIO_TASKS.md`。

---

**总结**：  
这是一个基于 AI 驱动的南宋皇帝模拟引擎。核心不是让 AI 直接改游戏，而是让 AI 把玩家圣旨理解成可校验、可白名单过滤、可由本地规则执行的游戏命令；同时用符合古代朝堂思维的 NPC 奏议，让玩家获得“坐殿治国”的沉浸感。