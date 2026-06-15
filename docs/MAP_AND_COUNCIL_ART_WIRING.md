# 山河地图与朝会人物美术接线

本次接线目标：让已入库的完整资源包继续在核心玩法页生效。

## 已接入

### 山河地图

`MapScreen` 现在使用：

```kotlin
ArtResourceRegistry.mapBackground(activeLayer)
```

对应：

- 军事：`images/map/song_world_military.webp`
- 经济：`images/map/song_world_parchment.webp`
- 外交：`images/map/song_world_political.webp`
- 外贸：`images/map/song_world_trade.webp`

Canvas 仍负责叠加：

- 城池节点
- 道路/海路/贸易路线
- 军团旗标
- 经济 halo
- 选中光环

这样山河页从“纯程序底色”推进为“地图底图 + 策略叠加层”。

### 朝会 NPC 卡牌

`PalaceTasksScreen` 的 `CouncilLineCard` 现在使用：

```kotlin
ArtResourceRegistry.portraitForOfficer(line.speakerId)
```

会显示：

- 岳飞、秦桧、赵鼎、李纲、韩世忠等核心人物卡
- 内侍、皇城司、皇后、太后等宫廷人物
- 金使、西夏使者、大理使者、海商等外交人物
- 未命中时使用 `images/characters/silhouettes/silhouette_unknown.webp`

## 效果

- 朝会不再只是文字卡。
- 人物奏议开始有头像与身份感。
- 地图图层不再只是纯色 Canvas。
- 资源包从“已入库”继续变成“已被核心 UI 消费”。

## 下一步

1. 城池 icon 真实图片替代 Canvas 矩形。
2. 朝会卡框接入 `images/ui/npc_card_frame.webp`。
3. 事件 CG 接入角色线与重大事件。
4. 山河装饰资源接入军旗、贸易路线、前线警戒。
