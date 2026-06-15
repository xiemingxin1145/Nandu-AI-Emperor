# 南渡无悔 · 音频素材任务单（V1.4.1）

> 给何老师 / 素材负责人。**代码侧音频系统已全部接好线**，每个槽位都有路径、有触发逻辑。
> 你只需要把真素材按下表的**文件名**丢进对应目录覆盖即可，**不用改任何代码**，下次构建自动生效。
> 现在仓库里的是 Claude 程序合成的**零版权占位音效**（能听出是什么，但不耐听），替换后体验立刻上一个台阶。

---

## 一、铁律：授权

游戏要上架/商用，素材授权必须干净。优先级：

1. **CC0（公共领域）** —— 首选，随便用、不用署名。Freesound / Pixabay / OpenGameArt 都能按 CC0 筛。
2. **CC BY** —— 可用，但**必须在游戏里署名**（做个"音频致谢"页）。
3. **CC BY-NC** —— ❌ 不可商用，直接放弃。
4. **GPL/CC BY-SA** —— 有传染性，慎用，别碰。

> ⚠️ 不要看到"免费"就拖进来。Pixabay 是它自家 License（不等于 CC0），OpenGameArt 授权混杂，**每个文件都要单独看 license**。下载时顺手把作者、来源链接、授权类型记到 `docs/AUDIO_CREDITS.md`。

---

## 二、格式要求

- 统一 **`.ogg`**（Vorbis），文件名与下表**完全一致**，丢进 `app/src/main/assets/audio/对应子目录/`。
- 采样率 44.1kHz，单声道或立体声均可。
- **音效（UI/SFX）**：剪到只剩有效声音，去头去尾静音，峰值归一化到约 -3dB。
- **环境音（ambience）**：做成**无缝循环**（首尾能接上），3–8 秒即可，游戏里会自动 loop。
- **BGM**：建议 60–120 秒无缝循环，音量比音效低（游戏里已压到 0.7）。

---

## 三、槽位清单与替换指引

### UI 音效 `assets/audio/ui/`（短、干脆，<0.3s）
| 文件 | 用途 | 找什么 |
|---|---|---|
| ui_click.ogg | 通用点击 | 木质/玉石轻击 "嗒" |
| ui_confirm.ogg | 确认/准奏 | 上行短音、磬声 |
| ui_cancel.ogg | 取消/驳回 | 下行短音 |
| ui_open_panel.ogg | 打开面板 | 卷轴展开/抽屉 |
| ui_close_panel.ogg | 关闭面板 | 卷轴收起 |
| ui_warning.ogg | 警告/危局 | 低沉提示音 |
| ui_stamp_edict.ogg | **朱批落印** | 印章盖纸"咚"，很有仪式感，建议自己录 |

### 战场 / 城务音效 `assets/audio/sfx/`
| 文件 | 用途 | 找什么 |
|---|---|---|
| sfx_drum_war.ogg | **攻城战鼓**（已接攻城触发） | 中式大战鼓连击 |
| sfx_battle_start.ogg | **开战号角**（已接攻城触发） | 牛角号/螺号长鸣 |
| sfx_sword_clash.ogg | 刀剑相击 | 金属兵器碰撞 |
| sfx_arrow_volley.ogg | 箭雨齐发 | 多箭破空 whoosh |
| sfx_horse_run.ogg | 马蹄奔驰 | 群马哒哒 |
| sfx_march.ogg | 行军 | 整齐脚步+鼓点 |
| sfx_city_fire.ogg | 城池火攻 | 大火轰鸣噼啪 |
| sfx_naval_wave.ogg | 水战江浪 | 江河波涛 |
| sfx_court_murmur.ogg | 朝堂私语 | 殿内群臣低语 |
| sfx_report_arrive.ogg | **军报/走访见闻到达**（已接酒楼走访） | 清脆铃/木鱼"叮" |

### 环境音 `assets/audio/ambience/`（无缝循环）
| 文件 | 用途 | 找什么 |
|---|---|---|
| amb_rain.ogg | 雨天 | 中雨 |
| amb_storm.ogg | 暴雨 | 雷雨 |
| amb_snow_wind.ogg | 冬季风雪 | 寒风呼啸 |
| amb_river.ogg | 水乡/沿江城 | 流水 |
| amb_city_day.ogg | **城内白日**（已接城内场景） | 市井人声底噪 |
| amb_city_night.ogg | 城内夜 | 夜市稀声/虫鸣 |
| amb_camp_night.ogg | 军营夜 | 营火+夜风 |
| amb_tavern.ogg | **酒楼**（已接酒楼面板） | 茶肆人声+杯盏碰撞 |
| amb_market.ogg | 市集 | 集市喧闹 |

### BGM `assets/audio/bgm/`（**最该尽快换真配乐**，合成占位最不耐听）
| 文件 | 场景 | 风格建议 |
|---|---|---|
| bgm_main_menu.ogg | 开局/标题 | 苍凉笛箫，南渡悲怆 |
| bgm_court.ogg | 皇宫/朝议/国政（已接） | 庄重宫廷，古筝编钟 |
| bgm_map.ogg | 山河图/城内（已接） | 舒缓行旅，山河辽阔 |
| bgm_battle.ogg | 战斗 | 急促战鼓+弦乐紧张 |
| bgm_event_sad.ogg | 悲情事件 | 二胡哀婉 |
| bgm_victory.ogg | 胜利结局（已接） | 恢弘大气 |
| bgm_defeat.ogg | 亡国结局（已接） | 低沉挽歌 |

---

## 四、中文人声 `assets/audio/voice/` —— **公开素材找不到，必须 TTS 或自录**

南宋古战场叫阵、旁白、酒楼吆喝，公开库里没有对味的。两条路：① AI 配音（如各家 TTS，选浑厚男声/古风音色）后期加混响处理；② 自己录。版权最干净，味道也最准。

| 文件 | 用途 | 推荐台词（可改） |
|---|---|---|
| voice_battle_cry.ogg | **叫阵**（槽位已留，待接战斗UI） | "金贼！可敢与某一战——！" / "大宋将士，随我杀！" |
| voice_edict_received.ogg | 接旨 | "臣，领旨——" |
| voice_military_report.ogg | 军报 | "八百里加急——！" |
| voice_court_response.ogg | 群臣应答 | "陛下圣明" / "臣以为不可" |
| voice_enemy_warning.ogg | 敌情预警 | "报——金军过河了！" |

> TTS 出来的干声建议过一道轻混响 + 战场环境垫底，更有临场感。叫阵可以多录几条随机播放，避免重复。

---

## 五、第一版优先级（先换这 8 个，性价比最高）

按 GPT 给的判断，先把最常听到的换掉：
1. `ui_click.ogg`（每次点击都响）
2. `amb_tavern.ogg`（酒楼氛围灵魂）
3. `amb_city_day.ogg`（城内底噪）
4. `sfx_drum_war.ogg` + `sfx_battle_start.ogg`（攻城最燃）
5. `bgm_court.ogg`（皇宫主场景）
6. `bgm_map.ogg`（山河主场景）
7. `ui_stamp_edict.ogg`（朱批仪式感）
8. `voice_battle_cry.ogg`（叫阵，自己 TTS）

其余可以慢慢补。系统对缺文件是**静默处理**，换一个生效一个，不会等齐。
