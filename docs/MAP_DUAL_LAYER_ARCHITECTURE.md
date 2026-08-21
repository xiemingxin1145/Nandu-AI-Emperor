# 双层天下地图架构预研（MAP-PROTOTYPE-001）

**分支**：`prototype/map-globe-v1`
**性质**：技术预研 + 视觉原型，**不**替换正式山河页，**不**接入朝议 / AI 执行器。
**日期**：2026-08-21

---

## 1. 目标体验

| 层 | 名称 | 用途 |
|----|------|------|
| L0 | **寰宇图** | 天下推演开场、外交、外贸、海路、季节、大事件、宏观势力 |
| L1 | **平铺战略图** | 城池/军团/道路/补给/战区操作（现有山河能力的主场） |

---

## 2. 技术方案对比

### 方案 A：真 3D 球体（Filament / SceneView）
难度高、低端机差、与 Compose 互操作差。**不适合 V1.7 MVP**。

### 方案 B：Canvas/OpenGL 伪 3D mesh
中高难度，可作中期演进，**不是最小可行**。

### 方案 C：2.5D 伪球面 + Compose Canvas 展开（**推荐 MVP**）
纯 Compose；性能好；点击命中简单；直接复用 16000×10000 世界坐标；可升级为真 3D 只换 L0 后端。

---

## 3. 推荐路线

选定 **方案 C**。L0 伪球面 → flatten 动画 → L1 现有 MapScreen 摄像机模型。不重写地图系统，不引入重型 3D 引擎。

---

## 4. 数据复用

MapData.nodes 坐标、四图层底图、城池 icons、道路类型均复用。原型在 `prototype/mapglobe` 独立包。

---

## 5. 现有素材盘点（只读）

- 底图：`song_world_{parchment,military,political,strategy,trade,blank_overlay}.webp`
- icons：首都/关隘/港口/前线等
- decorations：军旗、雾、前线、商路光
- 山河：程序绘制为主，非独立大图块
- 季节战略层：未完成接线
- 海路：MapData SEA + trade 节点已有

L1 素材够用；L0 MVP 无需新美术。

---

## 6. 分阶段

- **V1.7**：调试挂载原型；正式可 L0 过场后跳转 MapScreen
- **V1.8**：L0 读势力摘要/大事件；flatten 对齐初始摄像机；性能分档
- **V1.9+**：可选真 3D 仅换 L0 渲染后端

## 7. 正式接入预告改动（本分支不改）

导航入口、新建 WorldGlobeScreen（或迁出 prototype）、MapScreen 仅收初始 camera。不碰朝议与 AI 执行器。
