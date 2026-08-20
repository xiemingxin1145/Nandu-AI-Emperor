# VIDEO_USAGE_PLAN.md

《南渡无悔》现有 51 条 V3 视频 — 游戏使用规划与质量分级  
Branch: `art/video-batch4-living-world`  
Baseline: `fix/prologue-audio-timeline-20260820`  
Date: 2026-08-20  

**本文件不改二进制、不改 Kotlin。** 仅供后续集成与清洗使用。

---

## 0. 分级定义

| 标签 | 含义 |
|---|---|
| **KEEP** | 内容可用，优先接入；技术上可后续洗成 H.264 + 无音轨 |
| **REPLACE_LATER** | 内容勉强或风格漂移，短期可应急，中期应重做画面 |
| **SPECIAL_EVENT** | 一次性剧情/过场，不当循环背景 |
| **DEBUG_ONLY** | 仅天命绘卷/验收用，不进主流程 |
| **UNUSED** | 当前玩法无落点 |

技术默认假设（历史 README）：多数为 **H.265 + AAC、1280×720**。  
**所有接入表面必须静音播放**；音轨清理见 `VIDEO_REPLACEMENT_MAP.json`。

---

## 1. 全量分级总表（51）

### 1.1 intro（3）

| id | path | 分级 | 用途 | 场景 | 触发建议 | loop | 背景 | 弹窗CG | 人物详情 | 战斗 | fallback | 优先级 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| V01_splash_logo | assets/videos/intro/V01_splash_logo.mp4 | KEEP | 水墨片头 logo | 冷启动 / 开屏 | App 启动后 1 次 | oneshot | 否 | 可 | 否 | 否 | 静态标题字 | P1 |
| V02_menu_bg_loop | assets/videos/intro/V02_menu_bg_loop.mp4 | KEEP | 主菜单动态背景 | 主菜单 | showIntro && !prologue | **loop** | **是** | 否 | 否 | 否 | 主菜单静态图 | **P0** |
| V03_intro_cinematic | assets/videos/intro/V03_intro_cinematic.mp4 | KEEP | 靖康之难过场 | 序章第 2 幕 | 序章 ACT_2 | oneshot | 否 | **是** | 否 | 否 | event_jingkang 静态 CG | **P0** |

备注：V03 已知 **音轨污染风险**，集成必须 mute；清洗后仍 KEEP。

### 1.2 seasons（4）

| id | path | 分级 | 用途 | 场景 | 触发建议 | loop | 背景 | 弹窗CG | 人物 | 战斗 | fallback | 优先级 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| V04_season_spring | assets/videos/seasons/V04_season_spring.mp4 | KEEP | 春 | 地图/日历换季 | season==SPRING 入场一次或淡入 | oneshot/可慢循环 | 可 | 可 | 否 | 否 | season_spring_bg.webp | P1 |
| V05_season_summer | assets/videos/seasons/V05_season_summer.mp4 | KEEP | 夏 | 同上 | season==SUMMER | oneshot | 可 | 可 | 否 | 否 | season_summer_bg.webp | P1 |
| V06_season_autumn | assets/videos/seasons/V06_season_autumn.mp4 | KEEP | 秋 | 同上 | season==AUTUMN | oneshot | 可 | 可 | 否 | 否 | season_autumn_bg.webp | P1 |
| V07_season_winter | assets/videos/seasons/V07_season_winter.mp4 | KEEP | 冬 | 同上 | season==WINTER | oneshot | 可 | 可 | 否 | 否 | season_winter_bg.webp | P1 |

### 1.3 battle（6）

| id | path | 分级 | 用途 | 场景 | 触发建议 | loop | 背景 | 弹窗CG | 人物 | 战斗 | fallback | 优先级 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| V08_battle_field_clash | assets/videos/battle/V08_battle_field_clash.mp4 | KEEP | 普通野战 | 战报弹窗 | battleType FIELD | oneshot | 否 | **是** | 否 | **是** | battle_field.webp | **P0** |
| V09_battle_siege_assault | assets/videos/battle/V09_battle_siege_assault.mp4 | KEEP | 攻城 | 战报弹窗 | battleType SIEGE | oneshot | 否 | **是** | 否 | **是** | battle_siege.webp | **P0** |
| V10_battle_naval_clash | assets/videos/battle/V10_battle_naval_clash.mp4 | KEEP | 水战 | 战报弹窗 | NAVAL / RIVER_NAVAL | oneshot | 否 | **是** | 否 | **是** | 水战静态 | **P0** |
| V11_battle_mountain_pass | assets/videos/battle/V11_battle_mountain_pass.mp4 | KEEP | 山地关隘 | 战报弹窗 | MOUNTAIN / PASS | oneshot | 否 | **是** | 否 | **是** | 山地静态 | P1 |
| V12_battle_victory | assets/videos/battle/V12_battle_victory.mp4 | KEEP | 胜利演出 | 战报结尾 / 结局 | attackerWins | oneshot | 否 | **是** | 否 | **是** | 胜利静态字 | **P0** |
| V13_battle_defeat | assets/videos/battle/V13_battle_defeat.mp4 | KEEP | 失败演出 | 战报结尾 / 结局 | !attackerWins | oneshot | 否 | **是** | 否 | **是** | 失败静态字 | **P0** |

### 1.4 units（15）

全部 **KEEP**，用途：兵种出场 / 军务图鉴 / 征兵成功闪过。  
触发：选中兵种、招募完成、军务页预览。  
loop=否；背景=否；弹窗CG=可；人物详情=否；战斗=可作附加层。  
fallback：对应兵种图标/静态立绘。优先级：**P1**（背嵬/铁浮屠/神臂弓可抬到 P0 展示向）。

| id | path | 兵种 |
|---|---|---|
| V14_unit_song_infantry | assets/videos/units/V14_unit_song_infantry.mp4 | 宋军步兵 |
| V15_unit_song_archer | assets/videos/units/V15_unit_song_archer.mp4 | 宋军弓手 |
| V16_unit_song_crossbow | assets/videos/units/V16_unit_song_crossbow.mp4 | 宋军弩手 |
| V17_unit_divine_arm | assets/videos/units/V17_unit_divine_arm.mp4 | 神臂弓 |
| V18_unit_song_cavalry | assets/videos/units/V18_unit_song_cavalry.mp4 | 宋军骑兵 |
| V19_unit_song_navy | assets/videos/units/V19_unit_song_navy.mp4 | 宋军水军 |
| V20_unit_beiwei | assets/videos/units/V20_unit_beiwei.mp4 | 背嵬军 |
| V21_unit_shengjie | assets/videos/units/V21_unit_shengjie.mp4 | 胜捷军 |
| V22_unit_tabai | assets/videos/units/V22_unit_tabai.mp4 | 踏白军 |
| V23_unit_scout | assets/videos/units/V23_unit_scout.mp4 | 探马 |
| V24_unit_jin_infantry | assets/videos/units/V24_unit_jin_infantry.mp4 | 金军步兵 |
| V25_unit_jin_horse_archer | assets/videos/units/V25_unit_jin_horse_archer.mp4 | 金军马弓手 |
| V26_unit_jin_heavy | assets/videos/units/V26_unit_jin_heavy.mp4 | 金军重装 |
| V27_unit_jin_guaizi | assets/videos/units/V27_unit_jin_guaizi.mp4 | 拐子马 |
| V28_unit_iron_pagoda | assets/videos/units/V28_unit_iron_pagoda.mp4 | 铁浮屠 |

### 1.5 skills（6）

全部 **KEEP**。用途：武将技能演出。  
触发：战报含该将、技能释放、人物详情“演武”。  
loop=否；弹窗CG=是；人物详情=可；战斗=是。优先级 **P1**。

| id | path | 武将 |
|---|---|---|
| V29_skill_yue_fei | assets/videos/skills/V29_skill_yue_fei.mp4 | 岳飞 |
| V30_skill_han_shizhong | assets/videos/skills/V30_skill_han_shizhong.mp4 | 韩世忠 |
| V31_skill_li_gang | assets/videos/skills/V31_skill_li_gang.mp4 | 李纲 |
| V32_skill_wu_jie | assets/videos/skills/V32_skill_wu_jie.mp4 | 吴玠 |
| V33_skill_qin_hui | assets/videos/skills/V33_skill_qin_hui.mp4 | 秦桧 |
| V34_skill_zong_ze | assets/videos/skills/V34_skill_zong_ze.mp4 | 宗泽 |

### 1.6 char_live（10）

全部 **KEEP**。用途：人物详情动态立绘（呼吸/眨眼）。  
触发：OfficerList / CharacterDetail 展开且 status 允许显示肖像。  
**loop=是**；背景=否；弹窗CG=否；**人物详情=是**；战斗=否。  
fallback：VisualAssetV3 / ArtResourceRegistry 静态半身。优先级 **P0**。

| id | path | 人物 |
|---|---|---|
| V35_char_yue_fei | assets/videos/char_live/V35_char_yue_fei.mp4 | 岳飞 |
| V36_char_han_shizhong | assets/videos/char_live/V36_char_han_shizhong.mp4 | 韩世忠 |
| V37_char_qin_hui | assets/videos/char_live/V37_char_qin_hui.mp4 | 秦桧 |
| V38_char_zhao_gou | assets/videos/char_live/V38_char_zhao_gou.mp4 | 赵构 |
| V39_char_li_gang | assets/videos/char_live/V39_char_li_gang.mp4 | 李纲 |
| V40_char_zhao_ding | assets/videos/char_live/V40_char_zhao_ding.mp4 | 赵鼎 |
| V41_char_zhang_jun | assets/videos/char_live/V41_char_zhang_jun.mp4 | 张浚 |
| V42_char_wu_jie | assets/videos/char_live/V42_char_wu_jie.mp4 | 吴玠 |
| V43_char_liu_qi | assets/videos/char_live/V43_char_liu_qi.mp4 | 刘锜 |
| V44_char_zong_ze | assets/videos/char_live/V44_char_zong_ze.mp4 | 宗泽 |

### 1.7 ui_effects（6）

| id | path | 分级 | 用途 | 场景 | 触发建议 | loop | 背景 | 弹窗 | 人物 | 战斗 | fallback | 优先级 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| V45_ui_edict_stamp | assets/videos/ui_effects/V45_ui_edict_stamp.mp4 | KEEP | 圣旨盖印 | 朝议确认圣旨 | edict confirmed | oneshot | 否 | **是** | 否 | 否 | 印章静态帧 | **P0** |
| V46_ui_city_capture | assets/videos/ui_effects/V46_ui_city_capture.mp4 | KEEP | 城池易手 | 战报/地图 | cityCaptured | oneshot | 否 | **是** | 否 | 否 | 旗帜静态 | **P0** |
| V47_ui_level_up | assets/videos/ui_effects/V47_ui_level_up.mp4 | KEEP | 升级 | 人物/建筑升级 | level up event | oneshot | 否 | 可 | 可 | 否 | 光效静态 | P2 |
| V48_ui_gold_reward | assets/videos/ui_effects/V48_ui_gold_reward.mp4 | KEEP | 钱粮赏赐 | 赏赐/收入 | gold gain | oneshot | 否 | 可 | 否 | 否 | 铜钱图标 | P2 |
| V49_ui_grain_reward | assets/videos/ui_effects/V49_ui_grain_reward.mp4 | KEEP | 粮草入库 | 调粮成功 | grain gain | oneshot | 否 | 可 | 否 | 否 | 粮袋图标 | P2 |
| V50_ui_morale_boost | assets/videos/ui_effects/V50_ui_morale_boost.mp4 | KEEP | 士气提升 | 军务鼓舞 | morale up | oneshot | 否 | 可 | 否 | 否 | 士气图标 | P2 |

### 1.8 cinematic（1）

| id | path | 分级 | 用途 | 场景 | 触发建议 | loop | 背景 | 弹窗 | 人物 | 战斗 | fallback | 优先级 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| NanduWuhui_Prewar_CG_V01 | assets/videos/cinematic/NanduWuhui_Prewar_CG_V01.mp4 | **SPECIAL_EVENT** | 战前叙事 / 临时军议 | 出征前、军议过渡 | 攻城/野战确认前；或军务“军议” | oneshot | 否 | **是** | 否 | 可作前置 | 战前静态 CG | **P0** |

---

## 2. 玩法覆盖矩阵（必须场景）

| 玩法 | 推荐视频 | 状态 |
|---|---|---|
| 主菜单 | V02_menu_bg_loop | **已有 · 可接** |
| 序章（靖康） | V03_intro_cinematic | **已有 · 可接（须静音）** |
| 序章（其余幕） | 静态 CG + 旁白；Batch4 可选补镜 | 部分缺口 |
| 垂拱殿 | **无专用循环** | 缺口 → Batch4 或静态殿图 |
| 人物详情 | V35–V44 char_live | **已有 · 可接** |
| 天下地图 | **无地图循环**；季节 V04–V07 可作换季叠层 | 缺口 |
| 临安城 | **无城市场景循环** | 缺口 |
| 军营 | **无营寨循环**；单位片可作点缀 | 缺口 |
| 普通野战 | V08 | **已有** |
| 攻城 | V09 | **已有** |
| 水战 | V10 | **已有** |
| 胜利 | V12 | **已有** |
| 失败 | V13 | **已有** |
| 兵种 | V14–V28 | **已有** |
| 武将技能 | V29–V34 | **已有** |
| 城池易手 | V46 | **已有** |
| 圣旨盖印 | V45 | **已有** |
| 季节变化 | V04–V07 | **已有** |
| 战前军议 | NanduWuhui_Prewar_CG_V01 | **可临时复用** |

---

## 3. 统计

| 分级 | 数量 |
|---|---|
| KEEP | **50** |
| REPLACE_LATER（内容级） | **0**（内容暂全留；技术清洗走 replacement map） |
| SPECIAL_EVENT | **1**（Prewar CG） |
| DEBUG_ONLY | **0** |
| UNUSED | **0** |

说明：若把“必须技术清洗（HEVC/AAC）”算进 REPLACE_LATER，则 **51 条全部进入技术 REPLACE 队列**，但**内容分级仍为 KEEP/SPECIAL_EVENT**。见第 6 节 JSON。

---

## 4. Batch4 十四项必要性复审

| Batch4 id | 原需求 | 复审 | 临时复用 | 说明 |
|---|---|---|---|---|
| B4_01_chuigong_empty_loop | 垂拱殿循环 | **MUST_GENERATE** | 无 | 无殿内循环；静态殿图仅 fallback |
| B4_02_emperor_enter_hall | 皇帝入殿 | **OPTIONAL** | 无 | 可砍；进殿用淡入+静态即可 |
| B4_03_imperial_garden_loop | 御花园 | **MUST_GENERATE** | 无 | 后苑场景无动态 |
| B4_04_yushu_study_loop | 御书房 | **MUST_GENERATE** | 无 | 书房无动态 |
| B4_05_linan_city_loop | 临安城 | **MUST_GENERATE** | 无 | 城内无动态 |
| B4_06_world_map_loop | 天下地图 | **MUST_GENERATE** | 季节片仅叠层，不可替地图本体 | 地图需要地图质感 |
| B4_07_song_camp_loop | 宋军营寨 | **MUST_GENERATE** | 单位片不可当营寨背景 | 军务页需要 |
| B4_08_prewar_council | 战前军议 | **CAN_REUSE_EXISTING** | `NanduWuhui_Prewar_CG_V01` | 足够支撑 v1 军议过渡 |
| B4_09_become_zhaogou | 穿越成赵构 | **MUST_GENERATE** | V38 仅为立绘循环，非 POV 觉醒 | 序章身份锚点 |
| B4_10a_jingkang_fall | 靖康陷落 | **CAN_REUSE_EXISTING** | V03_intro_cinematic | 勿重复生成同质过场 |
| B4_10b_two_emperors_north | 二帝北狩 | **OPTIONAL** | 无强替代 | 静态 CG+旁白可撑 |
| B4_10c_south_crossing | 宋室南渡 | **OPTIONAL** | 无强替代 | 同上 |
| B4_10d_kangwang_enthronement | 康王即位 | **OPTIONAL** | 无 | 可用静态朝会图 |
| B4_10e_jiangnan_ruins | 江南残山 | **OPTIONAL** | 季节/山水静图 | 非阻断 |

### 复审汇总

| 结论 | 数量 | 条目 |
|---|---|---|
| **MUST_GENERATE** | **7** | 垂拱殿、御花园、御书房、临安城、天下地图、宋军营寨、穿越赵构 |
| **CAN_REUSE_EXISTING** | **2** | 战前军议←Prewar CG；靖康←V03 |
| **OPTIONAL** | **5** | 皇帝入殿、北狩、南渡、即位、残山剩水 |

**真正必须新生成：7**  
**可由旧 51 条复用顶上的原 Batch4 需求：2**  
**可砍/延后：5**

---

## 5. 集成优先级建议（给接线方）

**P0 立刻可接（已有文件）**  
V02 主菜单 · V03 序章（静音）· V08–V10/V12–V13 战斗 · V35–V44 人物 · V45 盖印 · V46 易手 · Prewar CG 军议

**P1**  
季节 V04–V07 · 山地 V11 · 兵种/技能全套

**P2**  
V47–V50 奖励类 UI · V01 片头

**阻断体验（需 Batch4 或长期静态）**  
垂拱殿/后苑/书房/临安/大地图/营寨循环背景；序章“我成了赵构”专用镜头

---

## 6. 相关文件

- `assets/videos/v3_inventory_manifest.json` — 51 条扫描
- `assets/videos/VIDEO_REPLACEMENT_MAP.json` — 技术清洗映射（本 Phase 新增）
- `assets/videos/batch4_living_world/manifest.json` — 新片队列（必要性已在本文复审）
