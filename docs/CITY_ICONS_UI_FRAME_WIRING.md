# 城池图标与 UI 框体接线 v1

本次目标：把完整资源包中的地图 icon 与 UI 框体纳入统一资源注册表，作为后续地图节点和界面框体替换的稳定入口。

## 已完成

修改：

```text
app/src/main/java/com/xiemingxin/nandu/game/ArtResourceRegistry.kt
```

新增：

```kotlin
fun mapIcon(iconKey: String): String
fun uiImage(id: String): String
```

### 地图 icon 已注册

资源目录：

```text
images/map/icons/
```

已覆盖：

- 宋都城
- 金都城
- 西夏都城
- 大理都城
- 巨城
- 重镇
- 金军重镇
- 江防重镇
- 港市
- 旧都
- 州县
- 关隘
- 战略节点
- 商贸节点
- 前线城
- 水路城

### UI 框体已注册

资源目录：

```text
images/ui/
```

已覆盖：

- 奏议框
- 圣旨落笔
- 危急标签
- 派系标签
- 御前裁断按钮
- 宫殿页签
- 山河面板
- 城池面板
- 人物卡框
- 圣旨卷轴
- 好感信任
- 商税外贸
- 军务战报
- 礼制名望
- 未登场标签

## 设计原则

后续 UI 不再直接写死资源路径，而是统一从：

```kotlin
ArtResourceRegistry.mapIcon(iconKey)
ArtResourceRegistry.uiImage(id)
```

取图。

这样后面替换图片时，只要保持文件名或修改注册表，不用到处改界面代码。

## 下一步

1. 在 `MapScreen` 中把 Canvas 方块城池替换为 `AssetImage` 图标叠层。
2. 在 `CityDetailPanel` 中显示 `CityVisualInfo.mapIconPath` 或 `ArtResourceRegistry.mapIcon(visual.iconKey)`。
3. 在朝会卡中接入 `npc_card_frame.webp`。
4. 在圣旨/奏议/选择按钮中接入 `dialog_frame.webp`、`edict_bar.webp`、`choice_button.webp`。

## 当前范围

本 PR 做的是稳定注册和接线路径，不大重写地图渲染，避免移动端地图交互再次失稳。
