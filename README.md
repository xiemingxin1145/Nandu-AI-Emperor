# 南渡无悔 · AI皇帝引擎

> 你写圣旨，AI理解；群臣反应，地图变化；历史反噬，天下推进。

## 架构概览

```
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
```

## AI Provider 支持

| Provider | 状态 | 说明 |
|---------|------|------|
| Claude | ✅ 已实现 | 推荐，圣旨理解最准 |
| OpenAI | 🔧 骨架已建 | 待填充 |
| Gemini | 🔧 骨架已建 | 待填充 |
| OpenRouter | 🔧 骨架已建 | 可路由任意模型 |
| 自定义API | 🔧 骨架已建 | 兼容OpenAI格式 |
| Mock离线 | ✅ 已实现 | 无需Key，用于测试 |

## 统一JSON协议

所有模型必须返回：
```json
{
  "summary": "一句话摘要",
  "commands": [{"type": "dispatch_army", "officerId": "...", ...}],
  "npcResponses": [{"officerId": "yue_fei", "attitude": "support", "text": "..."}],
  "riskTags": ["grain_pressure"],
  "confidence": 0.9,
  "clarificationNeeded": false
}
```

## 命令白名单
`dispatch_army` / `assign_officer` / `repair_city` / `raise_grain` / `suppress_officer` / `reward_officer` / `punish_officer` / `move_capital`

AI乱编的命令一律丢弃，最终数值由本地GameRuleEngine裁决。

## 构建

GitHub Actions自动构建Debug APK，见Actions页面下载。

## 版本路线
- **V0.2** 当前：12城静态地图 + 御笔下诏 + Mock AI
- **V0.3** 接入真Claude API解析JSON
- **V0.4** 5人NPC群臣回应完善
- **V0.5** 城池地图实时联动
- **V0.6** 金军自动推进
- **V1.0** AI原生南宋皇帝沙盒完整体验
