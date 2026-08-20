# 《南渡》第三方音频来源与授权

本文件用于追踪正式打包进 APK 的外部音频。原则：来源可追溯、许可证明确、优先 CC0。

## A. Super Bash Folds 开放音频包

- 上游仓库：https://github.com/blancmathis/Super_Bash_Folds
- 目录：`public/assets/audio/open/`
- 该目录附带 Kenney 音效/音乐许可证文本，开放素材为 CC0 路线。
- 同步脚本固定 SHA-256，避免上游内容无声变化。

### 当前使用

| 上游文件 | 用途 | SHA-256 |
|---|---|---|
| `music/menu-loop.ogg` | BGM 安全回退 | `3028433d60fa198ac919a934aef07256e1bf81eeb585f25426fd57f2a76cee7d` |
| `music/battle-loop.ogg` | 军事/危机/败北 BGM | `5323dfe7f172e6b85870790f60027a533bd87bfd233e6c99cab0caf923f68f3f` |
| `sfx/menu-back.ogg` | 返回/关闭 | `61581c58194e3f19f531072edabbc344204c7e0a2887b8ededce4357bcf09195` |
| `sfx/menu-confirm.ogg` | 确认/选择/印章占位 | `33b17a9a9a2397c62b285c52c33a907fdffb476909c99e42dde603f6a7a8b12c` |
| `sfx/menu-move.ogg` | 点击/切换/打开面板 | `ad09146e4ea33b931b2f5dfb4051a4f1fe4a36f1a48c42c5e9269c292ae21214` |

这些文件由 `scripts/sync_vetted_open_audio.sh` 下载并校验哈希。

## B. Open Lo-Fi

- 上游仓库：https://github.com/btahir/open-lofi
- 授权：CC0 1.0 Universal / Public Domain
- README 明确说明可用于 apps、videos、streams、games、podcasts，无需署名。
- 全量包：https://github.com/btahir/open-lofi/releases/latest/download/openlofi.zip

### 当前选择的 Asian & Zen Lo-Fi 曲目

| 曲目 | 《南渡》场景 |
|---|---|
| Temple at Dawn | 主菜单 |
| Bamboo Shadow Waltz | 朝堂/皇宫 |
| Lanterns in Slow Motion | 朝议/外交 |
| Moon Through Bamboo | 山河地图 |
| Teacup Morning Fog | 城市/市集 |
| Bells Before Sunrise | 太庙/礼仪 |
| Paper Lantern Rain | 悲情事件 |
| Misty Steam Quiet Dreams | 胜利/舒缓结算 |

同步时 MP3 会由 GitHub Actions 使用 FFmpeg 转为 OGG Vorbis，文件名映射到现有 `AudioResourceRegistry`，因此业务代码不需要改路径。

如果 OpenLoFi Release 临时下载失败，构建分支仍保留上面的 CC0 `menu-loop.ogg` 作为纯音乐安全回退，不会恢复旧的生成提示语音。

## C. 语音通道政策

`assets/audio/voice/` 当前刻意保持为空。

原因：此前 Demo 素材中出现了“南渡无悔、界面切换、古风温柔典雅……”一类生成提示词/报数被误当音频播放的问题。以后人物语音必须满足：

1. 明确属于某个人物/事件；
2. 只由 Voice API 显式触发；
3. 默认不循环；
4. 不得由 BGM、UI、Ambience 路由间接触发；
5. 文件需要单独记录来源、生成方式和商用权利。

## D. 为什么不直接搬“公开仓库里的全部音乐”

GitHub 公开只代表可读，不代表可复制或可商用。本项目只自动同步授权足够清晰的 CC0 素材；CC-BY 素材必须记录作者并在游戏内增加致谢后才允许进入正式包，授权不明素材一律不自动导入。
