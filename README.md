# 南渡无悔 · AI原生南宋皇帝沙盒 — 多模型可插拔AI引擎 + 御笔下诅系统

> 你写圣旨，AI理解；群臣反应，地图变化；历史反噬，天下推进。

## 项目名称
**南渡无悔 · AI皇帝引擎**  
> 你写圣旨，AI理解；群臣反应，地图变化；历史反噬，天下推进。

## 架构概览

玩家写圣旨（自由文本）
    ↓
AiProvider（可插拔：Claude / OpenAI / Gemini / OpenRouter / 自定义 / Mock）
    ↓
统一EdictResult JSON
    ↓
Kotlin校验 + 命令白名单过滤
    ↓
GameRuleEngine（本地规则裁决，AI不能直接改数值）
    ↓
GameState更新 + 起居注记录

- **核心流程**：玩家输入圣旨 → AI 解析为结构化 JSON → 本地校验与白名单过滤 → `GameRuleEngine` 执行业务规则 → 更新 `GameState` 并记录起居注。

## AI Provider 支持

| Provider | 状态 | 说明 |
| --- | --- | --- |
| Claude | 已实现 | 推荐，圣旨理解最准 |
| OpenAI | 骨架已建 | 待填充 |
| Gemini | 骨架已建 | 待填充 |
| OpenRouter | 骨架已建 | 可路由任意模型 |
| 自定义API | 骨架已建 | 兼容OpenAI格式 |
| Mock离线 | 已实现 | 无需Key，用于测试 |

## 统一JSON协议（EdictResult）

所有模型必须返回以下结构：

```json
{
  "summary": "一句话摘要",
  "commands": [
    {
      "type": "dispatch_army",
      "officerId": "...",
      ...
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
  "clarificationNeeded": false
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

## 关键类与流程组件

- **`GameState`** ：游戏状态核心，记录城池、军队、官员、粮草等信息。
- **`GameRuleEngine`** ：本地规则引擎，负责校验命令合法性、执行业务逻辑、防止 AI 越权。
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
| **V0.2** | 当前 | 12城静态地图 + 御笔下诅 + Mock AI |
| **V0.3** | 计划 | 接入真 Claude API 解析 JSON |
| **V0.4** | 计划 | 5人 NPC 群臣回应完善 |
| **V0.5** | 计划 | 城池地图实时联动 |
| **V0.6** | 计划 | 金军自动推进 |
| **V1.0** | 目标 | AI 原生南宋皇帝沙盒完整体验 |

## 集成点（供扩展）

- **AI 集成**：通过 `AiProvider` 接口替换或扩展模型（支持 OpenAI 兼容格式）。
- **事件系统**：`npcResponses` 与 `riskTags` 可触发事件（如粮草危机、官员态度变化）。
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
这是一个基于 AI 驱动的南宋皇帝模拟引擎，核心在于安全地将 AI 解析的圣旨转化为可控的游戏命令，通过 `GameRuleEngine` 本地执行业务逻辑，确保数值安全。支持多种 AI 提供者，采用统一 JSON 协议，具备清晰的扩展点（如事件、音频、规则）。当前处于 V0.2，Mock AI + 静态地图阶段，未来将逐步实现动态地图、NPC 互动与历史推进。
