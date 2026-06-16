# Claude 音频素材导入任务书

目标：给《南渡无悔》补齐测试版 BGM、UI 音效、事件音效、环境声。

## 重要原则

1. 优先使用 CC0 / Public Domain / 明确可免费用于游戏 Demo 的资源。
2. 可使用 CC-BY，但必须记录作者、来源、链接、许可证。
3. 不要使用热门影视、游戏、短视频、流行古风歌曲原曲。
4. 每个音频统一转成 `.ogg`。
5. 文件名必须严格匹配 `AudioResourceRegistry.kt`。
6. 缺文件时 App 应当仍能运行，但目标是尽量补齐第一批核心音频。

## 优先来源

```text
OpenGameArt
Freesound
Pixabay Sound Effects / Music
Kenney audio assets
其他明确 CC0 / Public Domain 音频库
```

## 第一优先级：必须先补

```text
app/src/main/assets/audio/bgm/bgm_main_menu.ogg
app/src/main/assets/audio/bgm/bgm_palace_hall.ogg
app/src/main/assets/audio/bgm/bgm_court.ogg
app/src/main/assets/audio/bgm/bgm_court_council.ogg
app/src/main/assets/audio/bgm/bgm_map.ogg
app/src/main/assets/audio/bgm/bgm_city.ogg
app/src/main/assets/audio/bgm/bgm_military.ogg
app/src/main/assets/audio/bgm/bgm_crisis.ogg
```

风格要求：

```text
主菜单：古琴、箫、低鼓，皇权感
皇宫大厅：庄重、宽阔、轻鼓
垂拱殿：肃穆、压迫、不吵
朝会裁断：低沉、紧张、适合读文字
山河地图：大地图、战略、空旷
城池经营：市井、轻乐、人间烟火
军务：战鼓、号角、紧迫
危机：低音、悬疑、压迫
```

## 第二优先级：UI 音效

```text
app/src/main/assets/audio/ui/ui_click.ogg
app/src/main/assets/audio/ui/ui_confirm.ogg
app/src/main/assets/audio/ui/ui_cancel.ogg
app/src/main/assets/audio/ui/ui_open_panel.ogg
app/src/main/assets/audio/ui/ui_close_panel.ogg
app/src/main/assets/audio/ui/ui_switch_tab.ogg
app/src/main/assets/audio/ui/ui_warning.ogg
app/src/main/assets/audio/ui/ui_stamp_edict.ogg
app/src/main/assets/audio/ui/ui_brush_write.ogg
app/src/main/assets/audio/ui/ui_scroll_open.ogg
app/src/main/assets/audio/ui/ui_scroll_close.ogg
```

风格要求：

```text
点击：短、轻、不刺耳
确认：干净、有落定感
取消：低一点、短一点
打开/关闭面板：纸张或木匣感
警告：短促，但不要现代电子警报
盖印：印章落纸、朱批感
毛笔：短刷刷声
卷轴：展开/收起纸张声
```

## 第三优先级：事件与环境音

```text
app/src/main/assets/audio/sfx/sfx_court_murmur.ogg
app/src/main/assets/audio/sfx/sfx_report_arrive.ogg
app/src/main/assets/audio/sfx/sfx_drum_war.ogg
app/src/main/assets/audio/sfx/sfx_march.ogg
app/src/main/assets/audio/sfx/sfx_horse_run.ogg
app/src/main/assets/audio/sfx/sfx_naval_wave.ogg
app/src/main/assets/audio/sfx/sfx_bell.ogg
app/src/main/assets/audio/sfx/sfx_gong.ogg
app/src/main/assets/audio/sfx/sfx_page_turn.ogg
app/src/main/assets/audio/sfx/sfx_coin.ogg
app/src/main/assets/audio/sfx/sfx_build.ogg
app/src/main/assets/audio/sfx/sfx_recruit.ogg
```

```text
app/src/main/assets/audio/ambience/amb_rain.ogg
app/src/main/assets/audio/ambience/amb_storm.ogg
app/src/main/assets/audio/ambience/amb_snow_wind.ogg
app/src/main/assets/audio/ambience/amb_city_day.ogg
app/src/main/assets/audio/ambience/amb_market.ogg
app/src/main/assets/audio/ambience/amb_palace_murmur.ogg
app/src/main/assets/audio/ambience/amb_harbor.ogg
app/src/main/assets/audio/ambience/amb_frontier_wind.ogg
```

## 音量建议

```text
BGM：-12dB 到 -18dB，循环不突兀
UI 音效：-6dB 到 -10dB，短促清楚
环境声：-18dB 到 -24dB，不能盖过 BGM
战鼓/急报：-8dB 到 -12dB，突出但不炸耳
```

## 循环建议

BGM 尽量 45 秒到 2 分钟。

```text
bgm_* 文件应尽量可循环
amb_* 文件应尽量可循环
ui_* 文件必须短，最好 0.1s 到 1.2s
sfx_* 文件按事件 0.3s 到 4s
```

## 交付要求

Claude/Cursor 完成后，请提交：

1. 音频文件放入正确 assets 目录。
2. 新增 `docs/AUDIO_LICENSES.md`。
3. `AUDIO_LICENSES.md` 至少包含：

```text
文件名
来源站点
原始链接
作者名
许可证
是否需要署名
备注
```

4. 跑 Android Build 和 Debug APK。
5. 截图或说明 App 没有因为缺音频而崩溃。

## 不要做

```text
不要提交 mp3/wav 大文件，优先 ogg
不要用不明授权的热门音乐
不要改 AudioResourceRegistry 的路径，除非和我确认
不要一次塞几百 MB 音频
```

## 第一批目标大小

```text
BGM 总体最好控制在 20MB 到 60MB
SFX + UI + Ambience 控制在 10MB 到 40MB
第一版总音频包尽量不超过 100MB
```
