# 《南渡无悔》美术资产上传报告

**版本：** V0.6.1  
**更新时间：** 2026-06-14T14:54:03Z  

---

## 本次上传汇总

| 类别 | 数量 | 路径 | 状态 |
|------|------|------|------|
| UI功能图标 | 53张 WebP | `app/src/main/assets/images/ui/icons/` | ✅ 已完成 |
| Fallback占位图 | 6张 WebP | 各目录根路径 | ✅ 已完成 |
| 历史名人立绘 | 0张 | `characters/historical/` | ⏳ 待豆包生图 |
| NPC模板 | 0张 | `characters/templates/` | ⏳ 待豆包生图 |
| 城池背景 | 0张 | `cities/` | ⏳ 待豆包生图 |
| 事件CG | 0张 | `events/` | ⏳ 待豆包生图 |
| 战役背景 | 0张 | `battles/` | ⏳ 待豆包生图 |
| 地图底图 | 0张 | `maps/` | ⏳ 待豆包生图 |

---

## UI图标详情（53张）

### 外交类（2张）
- `diplomacy_declare_war.webp` 宣战
- `diplomacy_peace.webp` 议和

### 天气类（9张）
- `weather_spring/summer/autumn/winter/clear/rain/snow/fog/wind.webp`

### 属性类（6张）
- `stat_force/command/strategy/politics/charm/loyalty.webp`

### 战役类（4张）
- `battle_ambush/attack/breakout/defense.webp`

### 行动类（8张）
- `action_appoint/promote/build/punish/recruit/research/reward/train.webp`

### 状态类（6张）
- `status_popular_support/famine/epidemic/stability/prosperity/corruption.webp`

### 系统类（9张）
- `system_task/cancel/save/help/achievement/confirm/settings/load/back.webp`

### 资源类（5张）
- `resource_weapon/cloth/wood/iron/horse.webp`

### 势力类（4张）
- `faction_song/jin/rebel/volunteer.webp`

---

## Fallback占位图（6张）

| 文件名 | 尺寸 | 大小 | 路径 |
|--------|------|------|------|
| placeholder_portrait.webp | 512×512 | 5KB | characters/ |
| placeholder_halfbody.webp | 1024×1536 | 13KB | characters/ |
| placeholder_city.webp | 1024×768 | 7KB | cities/ |
| placeholder_event.webp | 1024×640 | 8KB | events/ |
| placeholder_battle.webp | 1024×640 | 7KB | battles/ |
| placeholder_map.webp | 1920×1080 | 14KB | maps/ |

> 占位图为深色金边风格，不影响游戏运行，图片缺失时自动显示。

---

## 待补充资源清单

### 优先级 P0（核心历史人物，必须尽快补）
- 赵构（4套半身+4头像）、岳飞、韩世忠、吴玠、刘锜、李纲、赵鼎、秦桧、张浚、宗泽
- 完颜宗弼等金国核心人物8-10人
- **格式要求：** 头像512×512 WebP透明底；半身像1024×1536 WebP透明底

### 优先级 P1（NPC模板）
- 南宋文臣20头像+10半身
- 南宋武将25头像+15半身
- 南宋士兵20头像+10半身（不需透明底）
- 南宋功能官员20头像+10半身
- 金军武将20头像+10半身
- 金国文官12头像+6半身
- 敌军军官16头像+8半身（不需透明底）
- 叛军草寇16头像+8半身（不需透明底）
- 中立人物24头像+12半身

### 优先级 P2（场景背景）
- 城池背景：临安、开封、襄阳、鄂州、建康、扬州、成都（1024×768 WebP）
- 事件CG：靖康之变、二帝北狩、岳飞被害、绍兴和议（1024×640 WebP）
- 战役背景：水战、野战、攻城、山地（1024×640 WebP）
- 地图底图：羊皮纸底图（1920×1080 WebP）

---

## 注意事项

1. **不要修改Kotlin核心文件** — GameRuleEngine.kt / ArtResourceRegistry.kt / OfficerProfile.kt
2. **命名规范** — 全小写英文，下划线分隔，无空格无中文
3. **格式统一** — 全部WebP，质量80-85
4. **透明底** — 人物立绘必须透明背景PNG→转WebP，城池/事件/战役背景不需要透明底

---

*本报告由晚晚自动生成，豆包美术资源到位后持续更新*
