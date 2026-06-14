# 南渡无悔 V0.6.7 Compose 音频钩子

本次新增：

```text
app/src/main/java/com/xiemingxin/nandu/ui/components/GameAudioEffects.kt
```

## 用法

页面播放背景音乐：

```kotlin
PlayBgmEffect(AudioResourceRegistry.Bgm.court, sceneKey = "court")
```

播放一次音效：

```kotlin
PlaySfxEffect(AudioResourceRegistry.Ui.stampEdict, triggerKey = edictResult.id)
```

预加载常用音效：

```kotlin
PreloadSfxEffect(
    listOf(
        AudioResourceRegistry.Ui.click,
        AudioResourceRegistry.Ui.confirm,
        AudioResourceRegistry.Ui.warning
    )
)
```

## 安全边界

- 页面销毁时自动释放 `GameAudioPlayer`。
- BGM 页面离开时自动停止。
- 音频资源缺失时不崩溃。
- 目前尚未接入导航与页面，等资源上传稳定后再逐页接入。

## 下一步

1. 朝议页接入 `bgm_court.ogg`
2. 地图页接入 `bgm_map.ogg`
3. 战斗页接入 `bgm_battle.ogg`
4. 圣旨确认接入 `ui_stamp_edict.ogg`
5. 按钮点击接入 `ui_click.ogg`
