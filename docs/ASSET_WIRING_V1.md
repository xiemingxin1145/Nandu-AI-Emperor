# 资源接线 v1

本次接线目标：让 Claude 已导入的完整资源包真正被 UI 命中，而不是只存在于仓库里。

## 已完成

### 1. 资源注册表路径对齐

修改：

```text
app/src/main/java/com/xiemingxin/nandu/game/ArtResourceRegistry.kt
```

将旧路径：

```text
images/cities/...
images/characters/portrait_xxx.webp
images/characters/halfbody_xxx.webp
images/maps/...
```

对齐为真实资源包路径：

```text
images/city/...
images/characters/xxx.webp
images/map/...
images/palace/...
images/ui/...
```

### 2. 皇宫大厅接线

修改：

```text
app/src/main/java/com/xiemingxin/nandu/ui/screens/PalaceHallScreen.kt
```

接入：

- `images/palace/chuigongdian.webp`
- `images/palace/linan_street.webp`
- 八宫殿背景
- `ArtResourceRegistry.halfbodyForOfficer("zhao_gou")`
- `ArtResourceRegistry.palaceBackground(palaceId)`

### 3. 已可命中的资源类型

- 核心人物卡牌
- 城池背景
- 宫殿背景
- 事件 CG 槽位
- 地图底图路径
- UI 组件路径

## 下一步

1. 山河页接入真实地图底图。
2. 朝会页接入人物卡框与角色头像。
3. 城池详情页继续扩大城市背景命中率。
4. 事件系统接入事件 CG 槽位。
5. UI 逐步替换为统一金边墨底视觉。

## 说明

本 PR 只做路径接线和皇宫大厅第一步换图，不重构玩法逻辑。
