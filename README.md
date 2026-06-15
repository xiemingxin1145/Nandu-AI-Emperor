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
- **音频/多媒体**：当前未提及，但 `npcResponses` 中的 `text` 可扩展为语音合成输入点。
- **规则扩展**：`GameRuleEngine` 支持新增业务规则，控制命令执行条件。

## 古风音效系统集成 (已完成 - 专为吴王添加沉浸式体验)

已为游戏完整集成了《古风音效管理器》，支持战场、朝堂、勾栏听曲、大气碧碴等多种场景的分类音效和背景音乐。

### 核心功能
- **SoundCategory** 枚举：BATTLEFIELD / COURT / GOULAN / EPIC / UI / AMBIENT
- **随机播放** `playRandom(category)` 
- **事件触发** `playForGameEvent(eventType, riskTags)` — 在 GameRuleEngine 执行命令后自动播放对应音效
- **背景音乐** `startAmbient(category, fileName)` + 循环
- **音量控制** 和资源释放

### 集成方式 (建议在 GameRuleEngine.kt 中调用)

```kotlin
// 在 GameRuleEngine 执行命令后
soundManager.playForGameEvent(command.type, result.riskTags)

// 或在 npcResponses 处理后
if (npc.attitude == "oppose") soundManager.playRandom(SoundCategory.COURT)

// 背景音乐示例（在主界面或地图屏开始时）
soundManager.startAmbient(SoundCategory.COURT, "guzheng_ambient.ogg")
```

### 音效资产放置结构
```
app/src/main/assets/sounds/
  battlefield/
    war_drum1.ogg
    cavalry_gallop.ogg
    sword_clash.ogg
    ...
  court/
    court_bell.ogg
    scroll_unroll.ogg
    guzheng_ambient.ogg
  goulan/
    pipa_solo.ogg
    erhu_melody.ogg
  epic/
    grand_drums.ogg
    imperial_fanfare.ogg
  ui/
    wood_click.ogg
  ambient/
    city_ambient.ogg
```

### 免费古风音效来源推荐 (几百种任选，建议精选50-100个高质量放入assets)

1. **Pixabay Chinese Culture** (340+个免费古风/中国风格音效)：  
   https://pixabay.com/sound-effects/search/chinese%20culture/  
   包含古风转场、古筝、琴琶、传统乐器过场音乐等。

2. **Freesound.org** (大量CC0免费下载)：  
   搜索关键词：
   - 战场："chinese war drum" "ancient battle horn" "sword clash" "cavalry charge" "battlefield ambient" "arrow impact"
   - 朝堂："chinese bell" "bianzhong" "court music" "ancient palace footsteps" "scroll sound" "ink brush"
   - 勾栏/乐器："pipa" "guzheng" "erhu" "traditional chinese music" "pipa solo" "soft guzheng"
   - 大气碧碴："epic chinese orchestral" "suona" "grand chinese drums" "imperial fanfare" "majestic traditional"

3. **启动包推荐文件名示例** (可直接搜索下载后改名放入对应文件夹)：
   - **战场** : war_drum_loop.ogg, cavalry_gallop.ogg, sword_clash1.ogg, arrow_whoosh.ogg, battle_horn_suona.ogg, siege_fire_crackle.ogg
   - **朝堂** : court_bell_chime.ogg, scroll_unroll_paper.ogg, palace_stone_footsteps.ogg, guzheng_court_ambient.ogg, woodblock_announce.ogg, ink_brush_writing.ogg
   - **勾栏听曲** : pipa_rapid_solo.ogg, erhu_melodic.ogg, guzheng_traditional_piece.ogg, soft_pipa_dance_music.ogg
   - **大气碧碴** : epic_grand_drums_gongs.ogg, imperial_suona_fanfare.ogg, majestic_chinese_orchestra.ogg, heroic_guzheng_epic.ogg

**提示**：下载后用 Audacity 或在线工具转为 .ogg 格式，体积更小。多个文件可以随机播放增加变化感。

### 初始化示例

```kotlin
val soundManager = SoundManager.createAndLoadDefault(this)  // this = Context
// 在地图或主界面开始时
soundManager.startAmbient(SoundCategory.COURT, "guzheng_court_ambient.ogg")
// 在命令执行后
soundManager.playForGameEvent("dispatch_army")
```

**已将 SoundManager.kt 推送到仓库** 。直接在项目中使用即可为游戏添加沉浸式古风音效，让朝堂决策、战场指挥、群臣对话都更有南宋风情。

如果需要调整分类、增加更多音效或与具体UI绑定，随时告诉我继续完善。

---

**总结**：  
这是一个基于 AI 驱动的南宋皇帝模拟引擎，核心在于安全地将 AI 解析的圣旨转化为可控的游戏命令，通过 `GameRuleEngine` 本地执行业务逻辑，确保数值安全。支持多种 AI 提供者，采用统一 JSON 协议，具备清晰的扩展点（如事件、音频、规则）。当前处于 V0.2，Mock AI + 静态地图阶段，未来将逐步实现动态地图、NPC 互动与历史推进。
