# 朝会 UI 框体接线

本次目标：让宫殿待办与朝会奏议开始使用完整资源包中的 UI 框体资源。

## 接入范围

修改：

```text
app/src/main/java/com/xiemingxin/nandu/ui/screens/PalaceTasksScreen.kt
```

新增通用 `FramedPanel`，通过：

```kotlin
ArtResourceRegistry.uiImage(id)
```

读取 UI WebP 框体。

## 已使用资源

```text
images/ui/palace_tab.webp
images/ui/dialog_frame.webp
images/ui/edict_bar.webp
images/ui/faction_tag.webp
images/ui/npc_card_frame.webp
images/ui/edict_scroll.webp
images/ui/choice_button.webp
```

## 已接入组件

- 宫殿顶部标题栏
- 宫殿待办卡
- 空待办提示卡
- 朝会议题头部
- 朝中派系风向
- NPC 奏议卡
- 陛下裁断标题
- 裁断选择卡

## 设计原则

本 PR 只替换视觉框体，不改变：

- 朝会生成逻辑
- 角色发言逻辑
- 派系记忆逻辑
- 选择后果逻辑
- 圣旨草稿逻辑

## 后续方向

1. 进一步把 `edict_scroll.webp` 用到圣旨草稿确认页。
2. 把 `war_badge.webp`、`trade_badge.webp`、`ritual_badge.webp` 接入任务类型标签。
3. 把 `relation_badge.webp` 接入角色关系/信任系统。
4. 结合真机截图微调透明度与文字可读性。
