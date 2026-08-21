# 《南渡无悔》Audio Cleanroom Integration Status

更新时间：2026-08-20

## 当前工作分支

`integration/audio-cleanroom-v1`

基于：`fix/prologue-audio-timeline-20260820`

## 已完成

- 旧 15 首 BGM 已从当前修复资产树移除。
- `AudioResourceRegistry` 已改成新白名单命名。
- 序章禁止旧 BGM 自动进入。
- 序章视频内嵌音轨强制静音。
- 通用 `AssetVideoSurface` 强制所有生成视频静音，`muted=false` 也不能绕过。
- 顶层 `GameAudioController` 已按具体 `palaceId` 路由 BGM，不再把所有宫殿统一当作 `council`。
- 新增 `AudioAssetPolicyTest`：CI 阻止旧 BGM 或未验收 BGM 重返正式目录。

## 已人工试听通过、等待二进制入库的 7 首

1. `bgm_chuigong_hall_entry.ogg`
2. `bgm_chuigong_hall_loop.ogg`
3. `bgm_garden_loop.ogg`
4. `bgm_study_loop.ogg`
5. `bgm_worldmap_loop.ogg`
6. `bgm_linan_loop.ogg`
7. `bgm_military_camp_loop.ogg`

## 仍为 pending 的音乐槽位

- 主菜单主题
- 靖康序章危机曲
- 靖康/南渡悲怆曲
- 真正战斗曲
- 胜利曲
- 战败/战后余烬
- 外交
- 市集
- 宗庙/祭祀
- 水战专曲

这些用途宁可暂时无 BGM，也不得拿旧曲或不匹配曲目顶包。

## 序章当前音频策略

序章暂时采用：

`正式 M4A 旁白 + 低音量环境声 + 静音视频/CG`

在专门序章 BGM 经真机试听通过前，不恢复任何旧序章配乐。

## 三线协作边界

- Claude：`feature/living-world-court-v1` — 人物唯一状态源、时间线、动态朝堂。
- Grok：`art/video-batch4-living-world` — 视频/动态视觉资产，不改玩法逻辑。
- ChatGPT：`integration/audio-cleanroom-v1` — 音频白名单、视频静音、场景路由、CI 与最终集成。

最终合并顺序建议：

1. 审查 Claude 逻辑与测试。
2. 合并 Claude 到集成分支并修冲突。
3. 审查 Grok manifest/视频编码/音轨。
4. 合并 Grok 资产。
5. 接入新 BGM 二进制。
6. 全量 unit tests + Debug APK。
7. 解包 APK 验证 assets 实际存在。
8. 手机真机验收序章、朝堂、人物、声音、视频。
