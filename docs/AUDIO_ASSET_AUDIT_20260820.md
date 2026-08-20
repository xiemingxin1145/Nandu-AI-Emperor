# 《南渡无悔》音频资产排雷审计 — 2026-08-20

本审计基于已成功构建的 Android APK 解包结果与 `ffprobe` 检查。

## 1. BGM

旧 15 首已在 cleanroom 分支退出运行时资产树，单独见 `AUDIO_CLEANROOM_STATUS.md`。

## 2. 环境声 Ambience

现有环境循环全部很短，且全部为单声道：

| 文件 | 时长 | 采样率 | 声道 |
|---|---:|---:|---:|
| amb_camp_night.ogg | 4.00s | 44.1k | mono |
| amb_city_day.ogg | 4.00s | 44.1k | mono |
| amb_city_night.ogg | 4.00s | 44.1k | mono |
| amb_frontier_wind.ogg | 4.01s | 24k | mono |
| amb_harbor.ogg | 4.10s | 24k | mono |
| amb_market.ogg | 4.00s | 44.1k | mono |
| amb_palace_murmur.ogg | 5.40s | 24k | mono |
| amb_rain.ogg | 4.00s | 44.1k | mono |
| amb_river.ogg | 4.00s | 44.1k | mono |
| amb_snow_wind.ogg | 4.00s | 44.1k | mono |
| amb_storm.ogg | 4.00s | 44.1k | mono |
| amb_tavern.ogg | 4.00s | 44.1k | mono |

结论：

- 可继续作为测试占位。
- 不建议作为长期正式无限循环环境声；4 秒循环很容易产生机械重复感。
- 正式替换目标：25–60 秒自然循环；场景层尽量立体声，天气层可保留 mono/轻立体声。
- 序章暂不增加更多环境声层，避免与旁白抢戏。

## 3. 战斗/SFX 重复文件

以下文件 SHA-256 完全一致，本质为同一段声音的兼容别名：

- `sfx_city_alarm.ogg` = `sfx_city_fire.ogg`
- `sfx_battle_start.ogg` = `sfx_encounter_start.ogg`
- `sfx_metal_clash.ogg` = `sfx_sword_clash.ogg`
- `sfx_arrow_volley.ogg` = `sfx_arrows.ogg`

当前可保留兼容，不影响功能；正式音效重制时应分别制作，避免不同事件听感完全一样。

## 4. UI 音效重复文件

以下为完全相同的音频内容：

### 组 A
- `ui_confirm.ogg`
- `ui_select.ogg`
- `ui_stamp_edict.ogg`
- `ui_unlock.ogg`
- `ui_warning.ogg`

### 组 B
- `ui_cancel.ogg`
- `ui_close_panel.ogg`
- `ui_scroll_close.ogg`

### 组 C
- `ui_brush_write.ogg`
- `ui_click.ogg`
- `ui_open_panel.ogg`
- `ui_scroll_open.ogg`
- `ui_switch_tab.ogg`

结论：这些明显属于旧占位阶段。当前不是阻塞问题，但正式版应重新区分：

- 普通点击
- 页签切换
- 卷轴开合
- 毛笔书写
- 玉玺盖印
- 警告
- 解锁/成就

## 5. 优先级

P0：旁白真机稳定、BGM 白名单、视频无音轨。

P1：25–60 秒正式环境循环（宫廷/御花园/书房/临安/军营/地图/雨雪夜）。

P2：战场一击 SFX 多变体（刀兵、箭雨、马蹄、战鼓、城门、火焰）。

P3：UI 音效去重与南宋纸墨/木石/玉玺质感重制。
