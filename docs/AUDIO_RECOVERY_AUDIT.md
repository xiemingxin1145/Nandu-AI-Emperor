# AUDIO-RECOVERY-001 — V1.6.2 已验收正式 BGM 溯源与恢复调查

> 独立辅助任务，不修改 `integration/v1.6.2-preacceptance`，不碰地图代码，不碰
> STAB-007 UI。基线：`release/v1.6.1`。

---

## 结论先行（怕你没耐心看完全部证据链）

**8 个正式槽位，0 个可以从 git 仓库历史里直接恢复二进制。原因不是"曾经验收
通过又被误删"，而是这批音乐从一开始就没有真正的二进制文件提交进过这个仓库
——受限于"当前 GitHub 连接器无法直接上传本地大体积二进制音乐文件"这个
技术限制（这句话是仓库自己的文档写的，不是我猜的）。**

好消息：溯源过程中挖出了一份此前没人提起过的"验收映射文档"
（`docs/audio/BGM_V161_MAPPING.md`），里面精确记录了每个槽位对应的**原始
音源文件名**（比如 `beneath_the_imperial_gate.mp3`）和处理规格。这不是
"查无此音"，是"知道要找哪几个文件，只是文件本体不在这个仓库里"。

另一个重要发现：仓库里确实有 15 首"曾经真实播放过"的旧 BGM 完整二进制
（`git` 历史里可以 100% 无损恢复），但它们跟当前 Registry 的 8 个正式槽位
几乎完全不是一回事——这 15 首经过反复溯源确认是从"零版权占位音效"开始，
中间经历过好几轮"AI 生成提示词片段→CC0 开源素材→自主编曲"的来回替换，
最终被作者本人在 2026-08-20 晚上以"有歌词/人声/爆音/提示语"为由主动清空，
不属于"已验收"，**不建议恢复**。

---

## 第一阶段：确认正式 8 个槽位（以 Registry 当前代码为准，不凭记忆）

来源：`app/src/main/java/com/xiemingxin/nandu/game/AudioResourceRegistry.kt`
里的 `object Bgm`（第 8~19 行注释明确写着"已验收并接入的正式场景音乐
（V1.6.1）"），交叉核对 `bgmForScene`/`bgmForTab`/`bgmForPalace` 三个路由
函数和 `MainActivity.kt` 的 `GameAudioController` 场景判断逻辑。

| 场景 | 正式文件名 | Registry 常量 | 是否循环（设计意图） | 预期用途 | 当前是否真的接进播放路径 |
|---|---|---|---|---|---|
| 主菜单 | `bgm_main_menu.ogg` | `Bgm.mainMenu` | 循环 | 主菜单主题 | 是（`bgmForScene("main_menu")`） |
| 垂拱殿入殿 | `bgm_chuigong_hall_entry.ogg` | `Bgm.chuigongEntry` | **不循环**（一次性演出） | 升朝/入殿演出 | **否——全仓搜索找不到任何调用点，这个常量目前完全没接进任何播放逻辑，即使拿到文件现在也不会响** |
| 垂拱殿常驻 | `bgm_chuigong_hall_loop.ogg` | `Bgm.chuigongLoop` | 循环 | 垂拱殿/朝议/宫殿默认 | 是 |
| 后苑 | `bgm_garden_loop.ogg` | `Bgm.garden` | 循环 | 后苑/御花园 | 部分——`bgmForScene("garden")` 函数分支存在，但 `MainActivity.kt` 的场景判断逻辑目前不会产出 `"garden"` 这个 scene key，同样是"接了一半" |
| 御书房 | `bgm_study_loop.ogg` | `Bgm.study` | 循环 | 御书房/政事堂静思 | 是 |
| 天下地图 | `bgm_worldmap_loop.ogg` | `Bgm.worldMap` | 循环 | 天下地图 | 是，也是默认兜底曲 |
| 临安城市 | `bgm_linan_loop.ogg` | `Bgm.linan` | 循环 | 城市场景 | 是 |
| 军营 | `bgm_military_camp_loop.ogg` | `Bgm.militaryCamp` | 循环 | 军营/军务 | 是 |

**发现1（文档与代码不同步）**：`app/src/main/assets/audio/bgm/README_BGM.md`
（2026-08-20 "清仓重建"当天写的）"二、第一批已验收文件名"只列了 **7 首**，
并且在"四、序章音频规则"里明确写"不允许恢复旧 `bgm_main_menu.ogg`"。但
2026-08-21 上午 10:29 的三个 commit（`43f4588`/`3412c6b`/`21dd6b7`，都在
PR #50 里）把 `bgm_main_menu.ogg` 从"pending 待定"正式提升成了第 8 个槽位
——**代码已经更新，`README_BGM.md` 没有同步**，现在是过期状态。这不是
本任务的范围（本任务不改代码/文档），但值得记录，避免后续接手的人被
过期文档误导。

---

## 第二阶段：全仓溯源

### 已搜索范围

- 全部 64 个远程分支（含 14 个音频相关分支：`audio-bgm-self-composed-v1`、
  `audio-claude-task-brief-v1`、`audio-missing-fills-v1`、
  `audio-routing-sfx-v1`、`audio-second-batch-v1`、
  `audio-settings-controls-v1`、`audio-settings-persistence-v1`（及
  clean 版）、`audio-sfx-triggers-v1`、`audio/nandu-bgm-batch2-20260820`、
  `fix/prologue-audio-timeline-20260820`、`hotfix-audio-placeholder-loop`、
  `integration/audio-cleanroom-v1`、`opensource-audio-framework-pass`）
- `git log --all` 全部 commit 历史（按文件路径 `--follow` 追踪逐次内容变化）
- GitHub 已合并/未合并 PR（关键字搜索 "approved"/"bgm"/"audio"）
- `app/src/main/assets/audio/bgm/` 目录及其历史版本
- 804 个 GitHub Actions Artifacts 的名称与创建时间列表
- 相关 docs（`README_BGM.md`、新发现的 `docs/audio/BGM_V161_MAPPING.md`）

### 关键 commit 时间线（这才是整个调查的核心）

```
e281d79  V1.4.1 音频系统接线 + 零版权占位音效           ← 15首旧BGM最早的共同起点之一
ba87976  replace generated prompt clips with vetted CC0 assets
b5c43de  同上（又一轮替换）
f9dae34  同上
2ce4e69  同上
47287f0  replace CC0 placeholders with self-composed BGM tracks   ← 转向"自主编曲"
c7ad778  又换回 vetted CC0 assets
c319e22  restore 15 self-composed BGM tracks on current main      ← 最终定格版本
─────────────────────────────────────────────────────────
2026-08-20 22:21  b791e46  quarantine legacy BGM（代码注释首次点名"有歌词/人声/
                            爆音/提示语"问题，隔离 crisis/event_sad/map/
                            main_menu/court）
2026-08-20 22:27  2325194  audio: remove legacy BGM library from repair branch
                            （15首旧文件从资产树物理删除）
2026-08-20 ~22:30 7a6b471  docs(audio): establish clean BGM whitelist and
                            import rules（README_BGM.md 首个版本，7首名单）
2026-08-20 ~22:35 f21d2f4  audio: rebuild BGM registry around certified scene
                            tracks（当前7个新场景常量首次出现在代码里，
                            此时还没有对应的.ogg文件）
─────────────────────────────────────────────────────────
2026-08-21 10:29:03  43f4588  feat(audio): restore approved BGM scene routes
                               （main_menu 从 pending 提升为第8个正式槽位）
2026-08-21 10:29:19  3412c6b  test(audio): whitelist approved V1.6.1 BGM set
2026-08-21 10:29:50  21dd6b7  docs(audio): record V1.6.1 approved BGM mapping
                               （新建 docs/audio/BGM_V161_MAPPING.md）
```

### `docs/audio/BGM_V161_MAPPING.md`（21dd6b7 新建，全文摘录）

这是这次调查最重要的单一发现，直接原文引用：

> 本批使用用户重新提供并确认过的音乐源，避免旧的生成提示词/占位语音再次
> 进入 BGM 通道。

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

> `MusicLab_12446064_825173564.mp3` 与当前 `bgm_main_menu.ogg` 音频内容
> 高度一致（特征相关约 0.999），因此不重复占用运行时槽位。
>
> 处理规格：OGG Vorbis / 48 kHz / stereo；循环曲做 3 秒等功率首尾交叉衔接；
> 整体峰值约 -3 dBFS 以下，游戏内再由 BGM 音量控制衰减。
>
> **说明：当前 GitHub 连接器无法直接上传本地大体积二进制音乐文件；本映射
> 与路由先进入仓库，测试 APK 会把已验收的本地 OGG 注入并用同一稳定 RC
> 签名重新签名。后续获得二进制上传通道后，应按本表将完全相同的 OGG 入库，
> 使 CI 构建可完全复现。**

**这最后一段话直接回答了整个任务的核心问题**：你记忆中"V1.6.1 测试安装包
里真实播放过的正式 BGM"，就是这批音乐——但它们进入那个特定测试 APK 的
方式，是"本地注入已验收的 OGG + 用稳定 RC 签名重新签名"，**不是**通过
"提交到 git 仓库 → GitHub Actions 从仓库构建"这条常规路径。所以：

- git 历史里找不到这 7 个新场景文件名的二进制，**不是因为丢了，是因为
  它们从来没有以这种形式存在过**（我对全部 64 个分支、全部 commit 历史
  做过精确文件名搜索，`bgm_chuigong_hall_entry.ogg` 等 7 个名字在任何
  commit 里都从未作为真实 blob 出现过——只有代码里的路径字符串，从
  `f21d2f4` 那个"重建注册表"的 commit 起就只是占位声明）；
- GitHub Actions 产生的 804 个 Artifacts，全部是从当前仓库（BGM 目录
  为空）自动构建的，源头没有文件，构建产物自然也不可能有——**不需要
  逐个拆包验证，这是必然的因果关系**，我抽查了体积最大的一批
  `nandu-v1.6.1-debug-apk` 想确认这一点，但下载被沙盒网络白名单拦住
  （重定向到 `blob.core.windows.net`），改用逻辑推导确认了结论；
- 你记得"听到过"，是因为那个特定的、被手动重新签名的测试 APK 确实包含
  了这批音乐——**但那个 APK 本身不是这个仓库/这次调查能触达的对象**，
  它是一次性的本地产物。

### 15 首旧 BGM 的完整"身份证"

按 `2325194`（2026-08-20 22:27，被删除前的最后状态）逐一取值：

| 文件名 | 清仓前最终 blob SHA | 首次引入 commit | 是否经历"占位→CC0→自主编曲"反复替换 | 是否被 `b791e46` 注释点名"有问题" |
|---|---|---|---|---|
| `bgm_main_menu.ogg` | `0d01f1f7...` | `e281d79`（零版权占位起点） | 是（9 次内容变更） | **是** |
| `bgm_battle.ogg` | `c56eb22c...` | `e281d79` | 是（6 次变更） | 否（未点名，但同批清空） |
| `bgm_court.ogg` | `c9c25733...` | `e281d79` | 是（9 次变更） | **是** |
| `bgm_defeat.ogg` | `f5e57fc0...` | `e281d79` | 是（6 次变更） | 否（未点名，但同批清空） |
| `bgm_event_sad.ogg` | `05aa2ff9...` | `e281d79` | 是（9 次变更） | **是** |
| `bgm_map.ogg` | `be8ed14b...` | `e281d79` | 是（9 次变更） | **是** |
| `bgm_victory.ogg` | `c883ae1a...` | `e281d79` | 是（9 次变更） | 否（未点名，但同批清空） |
| `bgm_crisis.ogg` | `7be870b5...` | `acd15a9`（"add missing audio"） | 否，走的是"replace synth placeholder with real audio"这条独立线 | **是**（点名了但来源线不同，见下） |
| `bgm_city.ogg` | `b472c3b9...` | `fe39f81` | 否，"real recordings"线 | 否 |
| `bgm_court_council.ogg` | `7bcf4b73...` | `7ae93ad` | 否，"real recordings"线 | 否 |
| `bgm_diplomacy.ogg` | `fd2cff7d...` | `ba2b4ad`（"second audio batch"） | 否 | 否 |
| `bgm_market.ogg` | `e42d5c2d...` | `b466edc` | 否 | 否 |
| `bgm_military.ogg` | `7e01cdbd...` | `82ecaa8` | 否，"real recordings"线 | 否 |
| `bgm_palace_hall.ogg` | `947dc511...` | `ba89036` | 否，"real recordings"线 | 否 |
| `bgm_ritual.ogg` | `aa6c5939...` | `bb68181` | 否 | 否 |

**这里必须说清楚一个细节**：15 首里其实是两条不同血统——`main_menu`/
`battle`/`court`/`defeat`/`event_sad`/`map`/`victory` 这 7 首经历过
"零版权占位→AI 生成提示词片段→CC0 开源素材→自主编曲"来回好几轮替换，
`b791e46` 的注释明确点名隔离的正是这条线里的几个（main_menu/court/
event_sad/map，crisis 虽然血统线不同但也被一并点名）；而 `city`/
`court_council`/`diplomacy`/`market`/`military`/`palace_hall`/`ritual`
这 8 首走的是"add missing audio"→"replace synth placeholder with real
audio"/"second audio batch"这条描述为"真实录音"的独立血统线，**没有被
`b791e46` 的注释直接点名**，但仍然在 `2325194` 那次"一刀切"式清仓里被
一并整体删除——这可能是保守的批量处理（不逐首甄别，为了避免任何一首
漏网直接全清），也可能这 8 首本身也确实有问题只是注释没有逐一列举。
**我没有找到能证明这 8 首本身"过关"或者"不过关"的独立证据，两种可能
都成立，不能替你下判断。**

---

## 第三阶段：身份证汇总表

| 文件名 | 状态 | 理由 |
|---|---|---|
| `bgm_chuigong_hall_entry.ogg` | **MISSING**（仓库从未有过二进制） | 已知原始音源：`beneath_the_imperial_gate.mp3` 前42秒，需要用户提供原始文件 |
| `bgm_chuigong_hall_loop.ogg` | **MISSING** | 已知原始音源：`beneath_the_imperial_gate.mp3` |
| `bgm_garden_loop.ogg` | **MISSING** | 已知原始音源：`candlelight_over_the_yangtze.mp3` |
| `bgm_study_loop.ogg` | **MISSING** | 已知原始音源："同曲中段制作的安静版本"（即 `candlelight_over_the_yangtze.mp3` 的二创剪辑，具体版本文件未见记录） |
| `bgm_worldmap_loop.ogg` | **MISSING** | 已知原始音源：`MusicLab_12446237_650103557.mp3` |
| `bgm_linan_loop.ogg` | **MISSING** | 已知原始音源：`MusicLab_12446230_188062609.mp3` |
| `bgm_military_camp_loop.ogg` | **MISSING** | 已知原始音源：`MusicLab_12446226_776533420.mp3` |
| `bgm_main_menu.ogg`（新版，映射文档记录的"用户提供"版本） | **MISSING** | 映射文档只写"用户提供 bgm_main_menu.ogg"，没有更具体的原始文件名/来源线索 |
| `bgm_main_menu.ogg`（git 历史里那个旧 blob，`0d01f1f7...`） | **UNVERIFIED，且有明确质量风险证据，不建议恢复** | 血统复杂（占位→CC0→自主编曲反复替换），被 `b791e46` 注释点名"有歌词/人声/爆音/提示语"问题；技术上可无损从 git 恢复，但没有任何独立验收证据，且有明确的反面证据 |
| 其余 14 首旧 BGM | **UNVERIFIED（7首被点名有问题）/ RECOVERABLE-BUT-UNVETTED（8首"真实录音"线，技术上可无损恢复，但同样没有独立验收证据）** | 全部可以从 `2325194^` 这个 commit 完整无损取出二进制，但**没有一首**能找到独立的"人工验收通过"记录 |

**没有任何一首满足"能够证明是原验收版本"这个标准，所以按任务规则，
没有任何一首可以标记 RECOVERABLE。**

---

## 第四阶段：是否创建恢复分支

**没有创建 `fix/audio-recovery-v162`。**

原因：这个分支是为"确实从 git 历史找到了可以直接恢复的原始二进制"这种
场景准备的。经过完整溯源，8 个正式槽位对应的二进制**从未进入过本仓库**，
不存在"恢复"这个动作的对象；15 首旧 BGM 虽然技术上可以无损恢复二进制，
但没有一首拿得到"人工验收通过"的独立证据，反而有明确证据显示至少 7 首
存在被点名的质量问题——按任务规则不允许标记 RECOVERABLE，也就不满足
"新建恢复分支"的前提条件。

本任务只产出这一份调查文档，走的是"第五阶段：找不到二进制，不生成音乐，
只出文档"这条路径。

---

## 第五阶段：下一步需要什么

**唯一已知、明确可行的找回路径，需要你提供以下之一：**

1. **那个曾经真实播放过 BGM 的 V1.6.1 测试 APK 本身**（如果你本地还留着
   安装包文件）——它是一次性本地签名产物，不在 GitHub 仓库/Actions 能
   触达的范围内，只有你手上可能还有。有了它，可以直接从 APK（本质是个
   zip 包）里 extract 出 `assets/audio/bgm/*.ogg`，逐一 ffprobe 验证。
2. **`docs/audio/BGM_V161_MAPPING.md` 里点名的原始 mp3 源文件**——
   `beneath_the_imperial_gate.mp3`、`candlelight_over_the_yangtze.mp3`、
   `MusicLab_12446237_650103557.mp3`、`MusicLab_12446230_188062609.mp3`、
   `MusicLab_12446226_776533420.mp3`，以及"用户提供的 bgm_main_menu.ogg"
   本体。如果你手上还有这几个文件（或者知道 MusicLab 那几首的原始下载
   来源/授权凭证），可以按映射表里写的处理规格（OGG Vorbis / 48kHz /
   stereo / 循环曲 3 秒交叉衔接）重新处理一遍——这不是"重新生成"，是
   "重新处理已经选定、已经验收过的原始素材"，符合任务规则。
3. 如果两者都拿不到，这 8 个槽位目前只能保持 MISSING，STAB-007 验收时
   这一项会是明确的已知阻塞，不建议用旧 15 首里任何一首顶替（无论是否
   被点名，都没有独立验收证据）。

**不建议做的事（任务已经列过，这里按调查结果重申一遍其必要性）**：不要
把 git 历史里那 15 首旧文件的任何一首重新放回 `assets/audio/bgm/`——
即使是"real recordings"那 8 首看起来没被直接点名，也没有独立证据证明
它们过关，贸然恢复等于把"清仓"这个决定重新推翻，而这次调查没有找到
任何支持推翻这个决定的新证据。
