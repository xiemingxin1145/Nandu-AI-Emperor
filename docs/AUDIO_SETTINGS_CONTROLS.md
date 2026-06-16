# 音频设置控制

本次目标：给测试版接入可持久化的音频控制。

## 修改范围

```text
app/src/main/java/com/xiemingxin/nandu/MainActivity.kt
app/src/main/java/com/xiemingxin/nandu/ui/screens/SettingsScreen.kt
```

## 新增设置

```text
开启音频：总开关
BGM 音量：0% - 100%
音效音量：0% - 100%
```

## 当前实现

- 设置初始值从 `SharedPreferences` 读取。
- 设置变化后立刻写入 `SharedPreferences`。
- 重启 App 后仍会记住音频开关、BGM 音量、SFX 音量。
- 关闭音频后 BGM 与 SFX 的传入 volume 都为 0。
- BGM 与 SFX 分开传入 `PlayBgmEffect` / `PlaySfxEffect`。
- 先不改底层播放器，不改 AI/存档/游戏逻辑。

## 存储键

```text
SharedPreferences: nandu_audio_settings
audio_enabled
bgm_volume
sfx_volume
```

## 后续方向

1. 增加“测试音效”按钮。
2. 增加“默认 / 安静 / 影院感”预设。
3. 后续等 Claude 导入真实音频后再做真机音量校准。
4. 若设置项继续变多，可迁移到 DataStore。
