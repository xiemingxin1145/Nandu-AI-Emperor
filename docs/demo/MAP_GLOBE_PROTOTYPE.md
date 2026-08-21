# 双层地图原型挂载说明（MAP-PROTOTYPE-001）

## 代码位置

```
app/src/main/java/com/xiemingxin/nandu/prototype/mapglobe/
  GlobeProjection.kt          # 伪球面投影与采样城市
  GlobeMapPrototypeScreen.kt  # 可交互 Demo UI
```

正式山河页 `ui/screens/MapScreen.kt` **未被修改**。

## 真机/调试临时挂载（集成负责人操作）

在**你自己的调试分支**上，任选一处入口调用：

```kotlin
import com.xiemingxin.nandu.prototype.mapglobe.GlobeMapPrototypeScreen

// 示例：全屏覆盖
GlobeMapPrototypeScreen(onExit = { /* pop */ })
```

不要在 `integration/v1.6.2-preacceptance` 上直接长驻此入口，除非评审通过。

## 手势

| 模式 | 操作 |
|------|------|
| 寰宇图 | 单指拖拽旋转；点击城点选中 |
| 平铺 | 双指缩放 + 拖拽平移（与现有山河同构） |
| 按钮 | 「展开山河」/「收回寰宇」 |

## 验收看点

1. 球面阶段能分出宋/金/西夏色带与城点  
2. 展开动画连续，不是硬切  
3. 展开后点击命中仍可用  
4. 不出现对 MapScreen 的替换  
