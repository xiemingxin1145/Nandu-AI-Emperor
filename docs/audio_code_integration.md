# 南渡无悔 V0.6.6 音频代码接入说明

本次新增的是音频代码骨架，不上传音频本体，避免和资源导入并发冲突。

## 新增文件

- `app/src/main/java/com/xiemingxin/nandu/game/AudioResourceRegistry.kt`
- `app/src/main/java/com/xiemingxin/nandu/audio/GameAudioPlayer.kt`

## 资源目录约定

音频资源应放在：

```text
app/src/main/assets/audio/bgm/
app/src/main/assets/audio/sfx/
app/src/main/assets/audio/ambience/
app/src/main/assets/audio/voice/
app/src/main/assets/audio/ui/
```

## 代码能力

`AudioResourceRegistry` 统一登记音频路径：

```kotlin
AudioResourceRegistry.Bgm.court
AudioResourceRegistry.Ui.click
AudioResourceRegistry.Sfx.march
AudioResourceRegistry.Ambience.rain
AudioResourceRegistry.Voice.militaryReport
```

`GameAudioPlayer` 负责安全播放：

```kotlin
val audio = GameAudioPlayer(context)
audio.playSfx(AudioResourceRegistry.Ui.click)
audio.playBgm(AudioResourceRegistry.Bgm.court)
```

## 安全策略

- 音频缺失时静默失败，不闪退。
- 不依赖第三方库，不需要修改 Gradle。
- 使用 Android 原生 `MediaPlayer` 和 `SoundPool`。
- 会把 assets 音频临时复制到 cache 再播放，规避 APK 压缩资源导致的 `openFd` 问题。

## 下一步接入点

等 Claude 上传音频资源后，可以逐步接入：

1. 主菜单播放 `bgm_main_menu.ogg`
2. 朝议页播放 `bgm_court.ogg`
3. 山河地图播放 `bgm_map.ogg`
4. 战斗页播放 `bgm_battle.ogg`
5. 按钮点击播放 `ui_click.ogg`
6. 圣旨盖章播放 `ui_stamp_edict.ogg`
7. 军报抵达播放 `sfx_report_arrive.ogg`

## 当前不做

暂不改 `MainActivity`、导航、页面生命周期，等资源上传并通过 Actions 后再接入 UI 层。
