# 地图真实城池图标叠层

本次目标：在不破坏 `MapScreen` 现有 Canvas 拖动、缩放、点击逻辑的前提下，把资源包中的真实城池 icon 叠到地图节点上。

## 实现方式

新增 `MapIconOverlay`：

```kotlin
MapIconOverlay(
    cameraX = cameraX,
    cameraY = cameraY,
    zoom = zoom,
    selectedId = selectedId,
    activeLayer = activeLayer,
    cityMap = cityMap
)
```

它位于 Canvas 之上，但不绑定点击事件。

### 保留的旧逻辑

Canvas 仍负责：

- 拖动
- 缩放
- 点击选城
- 选中光环
- 城池名
- 道路/海路/贸易线
- 军团路线/军旗

### 新增的美术层

`MapIconOverlay` 负责：

- 根据世界坐标转屏幕坐标
- 根据 `CityVisualRegistry.visualFor(city, node)` 获取 iconKey
- 使用 `ArtResourceRegistry.mapIcon(visual.iconKey)` 读取真实 WebP
- 选中节点时叠加 `images/map/decorations/selected_ring.webp`

## 资源路径

```text
images/map/icons/city_capital_song.webp
images/map/icons/city_capital_jin.webp
images/map/icons/city_capital_xixia.webp
images/map/icons/city_capital_dali.webp
images/map/icons/city_metropolis.webp
images/map/icons/city_fortress.webp
images/map/icons/city_port.webp
images/map/icons/city_old_capital.webp
images/map/icons/city_county.webp
images/map/icons/city_pass.webp
images/map/icons/city_strategic.webp
images/map/icons/city_trade.webp
images/map/icons/city_frontline.webp
images/map/icons/city_water.webp
```

## 城池详情面板

`CityDetailPanel` 也开始显示对应城池 icon，让详情面板与地图节点视觉统一。

## 风险控制

这次没有把点击逻辑迁移到 Compose 图片层，避免图标覆盖后影响地图手势。

后续如果要让图标本身可点击，需要重新设计 hit test 和手势优先级。
