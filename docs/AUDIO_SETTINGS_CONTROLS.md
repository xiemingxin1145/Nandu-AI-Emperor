# 音频设置控制

本次目标：给测试版接入运行态音频控制。

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

- 设置保存在 `NanduApp` 的运行态 Compose state。
- 关闭音频后 BGM 与 SFX 的传入 volume 都为 0。
- BGM 与 SFX 分开传入 `PlayBgmEffect` / `PlaySfxEffect`。
- 先不改底层播放器，不改 AI/存档/游戏逻辑。

## 后续方向

1. 接入 SharedPreferences 或 DataStore，保存音频设置。
2. 增加“测试音效”按钮。
3. 增加“默认 / 安静 / 影院感”预设。
4. 后续等 Claude 导入真实音频后再做真机音量校准。
