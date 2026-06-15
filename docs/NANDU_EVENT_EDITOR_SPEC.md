# 《南渡无悔》事件编辑器与数据包规范

## 目标

本规范定义一套适合《南渡无悔》的可编辑数据骨架，让剧情、城池、武将、兵种、战役、结局、AI 上下文都能从数据文件读取，而不是继续写死在 Kotlin 代码里。

核心目标：

```text
条件触发 → 玩家选择 → 数值变化 → 地图变化 → 战役变化 → 历史线变化 → AI 解释与反馈
```

这套系统参考公开事件编辑器、叙事编辑器、地图编辑器的思路，但数据结构完全自有，不直接依赖商业游戏资源。

## 推荐目录结构

```text
app/src/main/assets/data/
  events/
    history_events.json
    city_events.json
    officer_events.json
    battle_events.json
    policy_events.json
  cities/
    cities_song_core.json
    cities_jin_core.json
    cities_xixia_phase2.json
  officers/
    officers_song.json
    officers_jin.json
  battles/
    campaigns.json
  endings/
    endings.json
  ai/
    npc_personas.json
    ai_context_rules.json
```

后续也可以把这些表交给 Google Sheet、CastleDB、Tiled、LDtk 或其他编辑器维护，再导出 JSON。

---

## 一、事件触发器规范

### 事件基础结构

```json
{
  "id": "linan_food_crisis_001",
  "title": "临安粮荒",
  "type": "city_event",
  "priority": 50,
  "once": true,
  "trigger": {},
  "description": "临安城中米价腾贵，百姓聚于市口，请求朝廷开仓。",
  "choices": []
}
```

字段说明：

| 字段 | 说明 |
|---|---|
| id | 全局唯一 ID |
| title | 弹窗标题 |
| type | history_event / city_event / officer_event / battle_event / policy_event / ending_event |
| priority | 事件优先级，越高越先触发 |
| once | 是否只触发一次 |
| trigger | 触发条件 |
| description | 正文描述 |
| choices | 玩家选择 |

### trigger 条件

```json
{
  "year_gte": 1128,
  "year_lte": 1142,
  "month_gte": 1,
  "month_lte": 12,
  "faction": "song",
  "city_owner": { "linan": "song" },
  "city_food_lte": { "linan": 30000 },
  "city_support_lte": { "linan": 60 },
  "gold_gte": 10000,
  "grain_gte": 50000,
  "jin_threat_gte": 70,
  "court_stability_lte": 50,
  "required_flags": ["jingkang_aftermath"],
  "blocked_flags": ["linan_food_crisis_solved"],
  "required_officers": ["yue_fei"],
  "required_achievements": []
}
```

第一版不需要一次支持所有字段，可以先实现：

```text
year_gte / year_lte
city_owner
gold_gte / grain_gte
jin_threat_gte / jin_threat_lte
court_stability_lte
required_flags / blocked_flags
```

### choice 选择结构

```json
{
  "id": "open_granary",
  "text": "开仓赈灾",
  "ai_hint": "民生优先，赵鼎与李纲会赞成。",
  "effects": {},
  "add_flags": ["linan_food_crisis_solved"],
  "remove_flags": [],
  "next_event": null
}
```

### effects 效果结构

```json
{
  "gold": -3000,
  "grain": -20000,
  "court_stability": 5,
  "jin_threat": 0,
  "city": {
    "linan": {
      "popularSupport": 15,
      "commerce": -5,
      "grain": -20000
    }
  },
  "officer_relation": {
    "zhao_ding": 5,
    "qin_hui": -2
  },
  "army": {
    "yue_army": {
      "morale": 5,
      "troops": 0
    }
  }
}
```

---

## 二、历史事件示例

```json
{
  "id": "jingkang_aftermath_001",
  "title": "靖康余烬",
  "type": "history_event",
  "priority": 100,
  "once": true,
  "trigger": {
    "year_gte": 1127,
    "required_flags": [],
    "blocked_flags": ["jingkang_aftermath_done"]
  },
  "description": "汴京陷落，宗室南渡，天下震动。江南士民望向行在，问大宋是否还有中兴之日。",
  "choices": [
    {
      "id": "stabilize_jiangnan",
      "text": "拥立行在，稳住江南",
      "ai_hint": "稳健保守，提升朝局稳定。",
      "effects": {
        "court_stability": 15,
        "gold": 3000,
        "grain": 10000
      },
      "add_flags": ["jingkang_aftermath_done", "jiangnan_first"]
    },
    {
      "id": "call_for_recovery",
      "text": "号召勤王，图谋恢复",
      "ai_hint": "主战派赞成，金国压力上升。",
      "effects": {
        "troopMorale": 15,
        "jin_threat": 10,
        "court_stability": -5
      },
      "add_flags": ["jingkang_aftermath_done", "recovery_first"]
    }
  ]
}
```

---

## 三、城池数据包规范

```json
{
  "id": "linan",
  "name": "临安",
  "region": "江南行在区",
  "route": "两浙西路",
  "owner": "song",
  "control_state": "CAPITAL",
  "terrain": "river",
  "x": 6200,
  "y": 7600,
  "defense": 88,
  "population": 900000,
  "commerce": 95,
  "agriculture": 70,
  "grain": 80000,
  "gold": 60000,
  "troops": 45000,
  "features": ["capital", "river", "commerce", "palace"],
  "links": ["shaoxing", "pingjiang", "jiankang"]
}
```

城池字段必须最终能映射到现有 `City` 与 `MapNode`：

```text
City：资源、兵力、民心、经营数值
MapNode：地图坐标、是否都城
CityRoad：城市连接和道路类型
```

新增城池不得只加名字，必须同时补：

```text
1. 坐标
2. 归属势力
3. 路线连接
4. 初始资源
5. 地形标签
6. 是否水运/关隘/都城
```

---

## 四、武将与官员数据包规范

```json
{
  "id": "yue_fei",
  "name": "岳飞",
  "faction": "song",
  "role": "general",
  "status": "IN_COURT",
  "portrait": "images/characters/halfbody_yue_fei.webp",
  "stats": {
    "command": 96,
    "force": 92,
    "intellect": 75,
    "politics": 45,
    "loyalty": 98
  },
  "personality": {
    "stance": "war",
    "tone": "沉稳、刚直、少废话",
    "risk": "中",
    "likes": ["北伐", "军纪", "收复旧土"],
    "dislikes": ["议和", "内耗", "克扣军粮"]
  },
  "ai_prompt_tags": ["主战", "忠臣", "军纪", "北伐"]
}
```

AI 官员对话必须读这些字段，而不是所有 NPC 共用一个口吻。

---

## 五、兵种数据包规范

```json
{
  "id": "song_spear",
  "name": "宋军枪兵",
  "faction": "song",
  "image": "images/units/song_spear.webp",
  "attack": 55,
  "defense": 70,
  "morale": 60,
  "cost_gold": 1000,
  "cost_grain": 2000,
  "counter": ["cavalry"],
  "weak_against": ["crossbow", "heavy_infantry"],
  "terrain_bonus": {
    "plain": 0,
    "river": -5,
    "mountain": 5,
    "pass": 10
  }
}
```

第一版继续保留现有 `BattleUnitCatalog`，后续可以迁移为 JSON 数据表。

---

## 六、战役包规范

```json
{
  "id": "yuefei_northern_expedition",
  "name": "岳飞北伐",
  "available_from": 1134,
  "required_generals": ["yue_fei"],
  "required_flags": ["war_party_rising"],
  "target_cities": ["yingchang", "zhuxianzhen", "kaifeng"],
  "entry_text": "岳飞上表，请以诸军北上，连取中原诸镇。",
  "win_effects": {
    "prestige": 50,
    "jin_threat": -30,
    "court_stability": 10,
    "add_flags": ["yuefei_campaign_success"]
  },
  "lose_effects": {
    "troops": -30000,
    "court_stability": -15,
    "add_flags": ["northern_expedition_failed"]
  }
}
```

战役包不直接改状态，必须经过规则引擎校验：

```text
战役触发 → 检查将领/粮草/路线/城市归属 → 战斗结算 → 写入结果
```

---

## 七、结局包规范

```json
{
  "id": "recover_kaifeng_ending",
  "title": "光复东京",
  "type": "milestone",
  "trigger": {
    "city_owner": { "kaifeng": "song" }
  },
  "ending_text": "东京复归，大宋社稷重振。百官请上尊号，天下士民皆言中兴有望。",
  "achievement": "recover_kaifeng",
  "does_end_game": false
}
```

注意：V1.2 已经确定方向——多数大功业是成就，不强制结束游戏。真正结束只用于亡国、禅位、主动结算等。

---

## 八、AI 读取规则

AI 不直接读整个 GameState 原始对象，而读整理后的上下文：

```json
{
  "date": "建炎三年春",
  "emperor": "赵构",
  "court": {
    "stability": 58,
    "war_party": 70,
    "peace_party": 35
  },
  "state": {
    "gold": 120000,
    "grain": 300000,
    "jin_threat": 80
  },
  "focus_city": "linan",
  "recent_flags": ["jingkang_aftermath_done", "jiangnan_first"],
  "available_events": ["linan_food_crisis_001"],
  "officer_persona": "yue_fei"
}
```

AI 的职责：

```text
1. 用角色口吻解释局势
2. 给玩家建议
3. 生成候选圣旨文本
4. 标记建议行动类型
```

AI 不允许：

```text
1. 直接修改城市归属
2. 直接增加金钱粮草
3. 绕过 GameRuleEngine
4. 伪造成就或结局
```

---

## 九、第一版实现计划

### V1.3.1：事件 JSON 读取器

新增：

```text
EventTriggerDef.kt
EventChoiceDef.kt
EventEffectDef.kt
JsonEventLoader.kt
EventTriggerMatcher.kt
EventEffectApplier.kt
```

先支持：

```text
year_gte/year_lte
required_flags/blocked_flags
city_owner
gold_gte/grain_gte
jin_threat_gte/jin_threat_lte
court_stability_lte
```

### V1.3.2：首批事件数据

先做 10 个事件：

```text
靖康余烬
临安粮荒
江淮告急
岳飞请战
秦桧议和
宗泽遗命
襄阳筑防
开封旧民来投
西夏来使
海贸商船入港
```

### V1.3.3：事件弹窗接入

现有剧情弹窗可以复用，但事件来源从 Kotlin 固定库逐步转向 JSON。

---

## 十、验收标准

第一版事件编辑器完成后，必须满足：

```text
1. 改 JSON 就能新增事件，不改 Kotlin 主逻辑
2. 事件可按年份、城市、旗标、国力触发
3. 玩家选择能改变资源、民心、朝局、flag
4. AI 官员能读取事件和角色人格进行解释
5. APK 自动构建通过
```
