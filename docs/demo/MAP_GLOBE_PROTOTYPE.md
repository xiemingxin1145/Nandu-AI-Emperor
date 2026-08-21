# 双层地图原型挂载说明（MAP-PROTOTYPE-001）

## 代码位置

```
app/src/main/java/com/xiemingxin/nandu/prototype/mapglobe/
  GlobeProjection.kt          # 伪球面投影与采样城市
  GlobeMapPrototypeScreen.kt  # 可交互 Demo UI
```

正式山河页 `ui/screens/MapScreen.kt` **未被修改**。

## 真机独立挂载（MAP-PROTOTYPE-INTEGRATION-001）

在独立分支 `feat/map-globe-device-prototype` 的底部导航进入「山河」，点击右上角「寰宇试览」。正式 `MapScreen.kt` 保持不变，试览结束后点击「返回正式山河图」或使用 Android 系统返回。

集成入口必须传入同一份真实世界状态：

```kotlin
import com.xiemingxin.nandu.prototype.mapglobe.GlobeMapPrototypeScreen

GlobeMapPrototypeScreen(gameState = currentGameState, onExit = { closePreview() })
```

不要在 `integration/v1.6.2-preacceptance` 上直接长驻此入口，除非评审通过。

## 手势

| 模式 | 操作 |
|------|------|
| 寰宇图 | 单指拖拽旋转；双指缩放；点击真实城点选中 |
| 平铺 | 双指缩放 + 拖拽平移（与现有山河同构） |
| 按钮 | 「展开山河」/「收回寰宇」 |

## 验收看点

1. 球面阶段能分出宋/金/西夏色带与真实城点，归属直接读取当前 `GameState`
2. 展开动画连续，不是硬切
3. 展开后点击命中仍可用
4. 不出现对 MapScreen 的替换
