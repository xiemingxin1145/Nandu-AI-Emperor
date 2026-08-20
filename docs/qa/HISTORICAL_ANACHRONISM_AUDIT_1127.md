# 1127 开局历史穿帮审查

**时代锚点**：建炎元年六月（约 1127-07），行在 **南京应天府**，非临安。  
**审查原则**：内部兼容 ID 可保留；**玩家可见文案/地图标签/事件叙述**不得把开局写成临安时代。

---

## 总表（高信号项）

| 位置 | 内容 | 判定 | 说明 |
|------|------|------|------|
| InitialData 宋势力 capitalCityId | yingtianfu | **正确** | 与 Canon 一致 |
| InitialData 城 yingtianfu | 南京应天府，isCapital=true | **正确** | |
| InitialData 城 linan | 显示名「杭州」，isCapital=false | **正确** | 1127 尚未升临安府 |
| InitialData 城 shaoxing | 显示名「越州」 | **正确** | 尚未改绍兴府 |
| MapData 节点 linan | 名「临安」，isCapital=true | **历史错误** | 与 InitialData 矛盾，地图易误导 |
| MapData 节点 yingtianfu | 应天府 | **正确** | 但应明确为当前行在 |
| ArtResourceRegistry 城 | linan → 「临安」 | **需改文案** | 开局展示应「杭州」 |
| PrologueScreen 第六幕 | 「百官已经候在**垂拱殿**」 | **需改文案** | 应天行在不宜直接用临安宫名 |
| PalaceIds.CHUIGONG | 内部 id | **兼容 ID** | 可保留；UI 展示名开局应避免写死「垂拱殿」 |
| CouncilConsequenceSystem | 结果句「垂拱殿定主战方略…」 | **需改文案** | 玩家可见 |
| random_events_v1.json | 大量「临安街头/茶肆/押送临安」 | **历史错误（开局池风险）** | 须按年代/行在门闸 |
| WorldRegionPlan | 江南行在区核心城写「临安」「绍兴」 | **需改文案** | 1127 应用杭州、越州 |
| army_song_linan | 军队 id | **兼容 ID** | 驻地应为应天；玩家显示名须是御营/行在军 |
| 序章第三幕 | 即位于南京应天府，改元建炎 | **正确** | |
| CharacterStateSource.CAPITAL_CITY_ID | yingtianfu | **正确** | |

## 垂拱殿专项

建炎元年应天府行在更常见鸿庆宫等礼制空间。建议内部继续 chuigong id；开局 UI 映射为「行在正殿」；迁都临安后再显示「垂拱殿」。序章第六幕口白必须先改。

## 优先修复

1. P0 序章第六幕殿名
2. P0 MapData 首都与 linan 显示名
3. P0 随机事件临安门闸
4. P1 朝会结果字符串
5. P1 ArtRegistry / WorldRegionPlan 显示名
