# AUDIO_LICENSES.md

本文件记录《南渡无悔》游戏中所有音频素材的来源、授权和使用说明。

## 音频生成说明

本批音频（PR #18，`audio-missing-fills-v1`）为 **Claude AI 使用 FFmpeg lavfi 合成生成**的占位音效。

- 生成工具：FFmpeg 6.1.1（libvorbis 编码）
- 生成方式：正弦波公式合成 + 滤波噪声
- 授权状态：**原创合成，无版权限制，可自由用于任何用途**
- 用途：游戏 Demo 测试阶段占位，后续可替换为真实录音/授权音频

---

## BGM（背景音乐）

| 文件名 | 来源 | 作者 | 许可证 | 是否署名 | 备注 |
|---|---|---|---|---|---|
| bgm_palace_hall.ogg | FFmpeg lavfi 合成 | Claude AI | 原创，无限制 | 否 | 低频三和弦，110/165/220 Hz，皇宫大厅 |
| bgm_court_council.ogg | FFmpeg lavfi 合成 | Claude AI | 原创，无限制 | 否 | 小三和弦，98/117/147 Hz，朝会裁断 |
| bgm_crisis.ogg | FFmpeg lavfi 合成 | Claude AI | 原创，无限制 | 否 | 不协和音程，130/184 Hz，危机张力 |
| bgm_city.ogg | FFmpeg lavfi 合成 | Claude AI | 原创，无限制 | 否 | 大三和弦，261/329/392 Hz，市井轻快 |
| bgm_military.ogg | FFmpeg lavfi 合成 | Claude AI | 原创，无限制 | 否 | 低频调制 87 Hz，战鼓节奏感 |
| bgm_main_menu.ogg | 已有（前批） | — | — | — | 主菜单 |
| bgm_court.ogg | 已有（前批） | — | — | — | 垂拱殿 |
| bgm_map.ogg | 已有（前批） | — | — | — | 山河大图 |
| bgm_battle.ogg | 已有（前批） | — | — | — | 战斗 |
| bgm_crisis.ogg | 已有（前批） | — | — | — | （见新增） |
| bgm_defeat.ogg | 已有（前批） | — | — | — | 败北 |
| bgm_victory.ogg | 已有（前批） | — | — | — | 胜利 |
| bgm_event_sad.ogg | 已有（前批） | — | — | — | 悲痛事件 |

---

## UI 音效

| 文件名 | 来源 | 作者 | 许可证 | 是否署名 | 备注 |
|---|---|---|---|---|---|
| ui_switch_tab.ogg | FFmpeg lavfi 合成 | Claude AI | 原创，无限制 | 否 | 1200/1600 Hz 短促切换 |
| ui_brush_write.ogg | FFmpeg lavfi 合成 | Claude AI | 原创，无限制 | 否 | 粉色噪声低通，毛笔感 |
| ui_scroll_open.ogg | FFmpeg lavfi 合成 | Claude AI | 原创，无限制 | 否 | 白噪声带通 3000 Hz，纸张展开 |
| ui_scroll_close.ogg | FFmpeg lavfi 合成 | Claude AI | 原创，无限制 | 否 | 白噪声带通 2500 Hz，纸张收起 |
| ui_click.ogg | 已有（前批） | — | — | — | 点击 |
| ui_confirm.ogg | 已有（前批） | — | — | — | 确认 |
| ui_cancel.ogg | 已有（前批） | — | — | — | 取消 |
| ui_open_panel.ogg | 已有（前批） | — | — | — | 打开面板 |
| ui_close_panel.ogg | 已有（前批） | — | — | — | 关闭面板 |
| ui_warning.ogg | 已有（前批） | — | — | — | 警告 |
| ui_stamp_edict.ogg | 已有（前批） | — | — | — | 盖印圣旨 |

---

## SFX（事件音效）

| 文件名 | 来源 | 作者 | 许可证 | 是否署名 | 备注 |
|---|---|---|---|---|---|
| sfx_bell.ogg | FFmpeg lavfi 合成 | Claude AI | 原创，无限制 | 否 | 880/1760 Hz 衰减，钟声 |
| sfx_gong.ogg | FFmpeg lavfi 合成 | Claude AI | 原创，无限制 | 否 | 87/110 Hz 低频衰减，铜锣 |
| sfx_page_turn.ogg | FFmpeg lavfi 合成 | Claude AI | 原创，无限制 | 否 | 白噪声 4000 Hz，翻书 |
| sfx_coin.ogg | FFmpeg lavfi 合成 | Claude AI | 原创，无限制 | 否 | 1320/1760 Hz 衰减，铜钱 |
| sfx_build.ogg | FFmpeg lavfi 合成 | Claude AI | 原创，无限制 | 否 | 150 Hz + 回响，建造 |
| sfx_recruit.ogg | FFmpeg lavfi 合成 | Claude AI | 原创，无限制 | 否 | 100/150 Hz 快速衰减，鼓声 |
| sfx_court_murmur.ogg | 已有（前批） | — | — | — | 朝堂低语 |
| sfx_drum_war.ogg | 已有（前批） | — | — | — | 战鼓 |
| sfx_horse_run.ogg | 已有（前批） | — | — | — | 奔马 |
| sfx_march.ogg | 已有（前批） | — | — | — | 行军 |
| sfx_naval_wave.ogg | 已有（前批） | — | — | — | 水军/海浪 |
| sfx_report_arrive.ogg | 已有（前批） | — | — | — | 急报 |
| sfx_arrow_volley.ogg | 已有（前批） | — | — | — | 箭雨 |
| sfx_battle_start.ogg | 已有（前批） | — | — | — | 战斗开始 |
| sfx_city_fire.ogg | 已有（前批） | — | — | — | 城池火灾 |
| sfx_sword_clash.ogg | 已有（前批） | — | — | — | 刀剑交击 |

---

## 环境声（Ambience）

| 文件名 | 来源 | 作者 | 许可证 | 是否署名 | 备注 |
|---|---|---|---|---|---|
| amb_palace_murmur.ogg | FFmpeg lavfi 合成 | Claude AI | 原创，无限制 | 否 | 棕色噪声低通 600 Hz，宫殿低语 |
| amb_harbor.ogg | FFmpeg lavfi 合成 | Claude AI | 原创，无限制 | 否 | 棕色噪声低通 400 Hz，港口水声 |
| amb_frontier_wind.ogg | FFmpeg lavfi 合成 | Claude AI | 原创，无限制 | 否 | 白噪声带通 1200 Hz，边疆风声 |
| amb_rain.ogg | 已有（前批） | — | — | — | 雨声 |
| amb_storm.ogg | 已有（前批） | — | — | — | 暴风雨 |
| amb_snow_wind.ogg | 已有（前批） | — | — | — | 雪风 |
| amb_city_day.ogg | 已有（前批） | — | — | — | 城市白天 |
| amb_market.ogg | 已有（前批） | — | — | — | 市集 |
| amb_camp_night.ogg | 已有（前批） | — | — | — | 营地夜晚 |
| amb_city_night.ogg | 已有（前批） | — | — | — | 城市夜晚 |
| amb_river.ogg | 已有（前批） | — | — | — | 河流 |
| amb_tavern.ogg | 已有（前批） | — | — | — | 酒肆 |

---

## 后续替换建议

如需替换为真实录音/高质量授权音频，推荐来源：

- **OpenGameArt.org**（CC0/CC-BY 音频包）
- **Freesound.org**（大量 CC0 古风音效）
- **Kenney.nl**（全 CC0 音效包，推荐 Interface Sounds 和 Music Jingles）
- **Pixabay**（免版税音乐，需注意 Demo 后商业授权）

替换时请更新本文件对应行的来源信息。
