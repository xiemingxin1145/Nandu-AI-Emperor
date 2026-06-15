# 地图视觉基础与人物登场门槛

本模块回应两个核心问题：

1. 地图不能只是程序点位，应该是“底图 + 城池 + 道路 + 信息面板”的 SLG 结构。
2. 人物不能一开局全部摆出来，必须按历史阶段、传闻、召见、入朝逐步登场。

## 一、地图视觉基础

新增：

- `CityVisualRegistry`
- `CityVisualInfo`
- `CityVisualTier`
- `MapBackgroundRegistry`

### 地图底图槽位

当前预留四套底图路径：

```text
images/map/song_world_parchment.webp
images/map/song_world_political.webp
images/map/song_world_trade.webp
images/map/song_world_military.webp
```

后续只要把真实图片放进这些路径，山河页即可逐步替换程序底图。

### 城池视觉等级

城池不再只有点位，先登记视觉身份：

- 都城
- 巨城
- 重镇
- 港市
- 关隘
- 州县
- 战略节点

每类都有 `mapScale`，后续可用于地图城池图标大小和面板样式。

### 第一批重点城池

已登记：

- 临安：皇都 / 行在 / 繁华
- 建康：江防重镇
- 襄阳：北伐跳板
- 汴京：旧都
- 洛阳：旧都
- 燕京：燕云重镇
- 泉州：市舶司 / 香料海贸
- 明州：船材 / 高丽海路
- 广州：南海商路
- 成都：西南财赋
- 兴庆府：西夏都城
- 大理：西南国都
- 合肥：江淮前线
- 寿春：淮上要地

## 二、人物登场门槛

新增：

- `CharacterAppearanceSystem`
- `CharacterVisibility`
- `CharacterAppearanceInfo`

### 登场状态

人物分为：

- 未登场
- 传闻中
- 已露面
- 已入朝
- 核心人物

### 规则

人物不再一开局全部出现在朝会里。

例：

- 李纲、赵鼎：核心人物，开局可出现在朝会。
- 韩世忠：早期军事人物，可出现在枢密院与军事议题。
- 岳飞：未正式入朝前，需通过人才线索、召见、考校逐步登场。
- 秦桧：前期可作为暗线或传闻人物，不应一开始稳定占满朝会。
- 皇后、太后、内侍：只稳定出现在后苑内廷。

### 朝会接入

`CourtCouncilSystem.sceneForTask(...)` 现在会通过：

```kotlin
CharacterAppearanceSystem.filterCouncilLines(...)
```

过滤未登场人物。

这意味着：

- 未登场人物不会硬塞进朝会。
- 有线索但未入朝的人，可显示为“传闻人物”。
- 已入朝或核心人物才可进入完整奏对。

## 三、后续计划

### 下一步地图

- 在 `MapScreen` 中接入真实底图资源。
- 让 `CityVisualTier.mapScale` 影响城池图标大小。
- 让 `CityVisualInfo.tags` 显示在城池详情面板。
- 给港市、关隘、旧都、都城使用不同城池 icon。

### 下一步人物线

- 新增 `CharacterStoryState`。
- 给岳飞、秦桧、赵鼎、皇后建立故事阶段。
- 让好感、信任、危机、专属结局进入系统。
- 后续再接 CG 解锁和角色事件。

## 一句话

地图先有底图和城池身份，人物先有登场门槛；这样后续做角色线、CG、国是树、胜利路线才不会浮在空中。
