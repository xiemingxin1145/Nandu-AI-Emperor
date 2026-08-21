# V1.6.1 BGM 接入映射

本批使用用户重新提供并确认过的音乐源，避免旧的生成提示词/占位语音再次进入 BGM 通道。

| 运行时文件 | 来源/用途 |
|---|---|
| `bgm_main_menu.ogg` | 用户提供 `bgm_main_menu.ogg`，主菜单 |
| `bgm_chuigong_hall_entry.ogg` | `beneath_the_imperial_gate.mp3` 前 42 秒，升朝/入殿一次性演出 |
| `bgm_chuigong_hall_loop.ogg` | `beneath_the_imperial_gate.mp3`，垂拱殿/朝议 |
| `bgm_garden_loop.ogg` | `candlelight_over_the_yangtze.mp3`，后苑 |
| `bgm_study_loop.ogg` | 同曲中段制作的安静版本，御书房/政事 |
| `bgm_worldmap_loop.ogg` | `MusicLab_12446237_650103557.mp3`，天下地图/序章前段 |
| `bgm_linan_loop.ogg` | `MusicLab_12446230_188062609.mp3`，城市 |
| `bgm_military_camp_loop.ogg` | `MusicLab_12446226_776533420.mp3`，军务/军营 |

`MusicLab_12446064_825173564.mp3` 与当前 `bgm_main_menu.ogg` 音频内容高度一致（特征相关约 0.999），因此不重复占用运行时槽位。

处理规格：OGG Vorbis / 48 kHz / stereo；循环曲做 3 秒等功率首尾交叉衔接；整体峰值约 -3 dBFS 以下，游戏内再由 BGM 音量控制衰减。

说明：当前 GitHub 连接器无法直接上传本地大体积二进制音乐文件；本映射与路由先进入仓库，测试 APK 会把已验收的本地 OGG 注入并用同一稳定 RC 签名重新签名。后续获得二进制上传通道后，应按本表将完全相同的 OGG 入库，使 CI 构建可完全复现。
