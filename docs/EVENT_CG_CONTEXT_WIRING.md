# 事件 CG 上屏与上下文匹配

本次目标：让事件卡不再只依赖 `eventId` 精确命中 CG，而是能根据事件标题、描述、类型和 artHint 自动选择更合适的事件图。

## 修改文件

```text
app/src/main/java/com/xiemingxin/nandu/ui/components/StoryEventCard.kt
```

## 核心逻辑

新增：

```kotlin
private fun eventCgPath(event: StoryEvent): String
```

优先级：

1. 先尝试 `ArtResourceRegistry.eventImage(event.eventId)`。
2. 如果命中默认图，则进入上下文匹配。
3. 根据标题、描述、artHint、事件类型、章节名选择 CG。

## 已覆盖方向

```text
岳飞 / 请战 / 北伐 → event_yuefei_petition.webp
风波亭 / 秦桧 / 冤案 → event_fengboting_crisis.webp
皇后 / 后苑 / 内廷 → event_empress_secret.webp
御书房 / 密折 / 夜议 → event_yushufang_night.webp
太庙 / 誓师 / 礼制 → event_taimiao_oath.webp
金使 / 金国 / 完颜 → event_jin_envoy.webp
海贸 / 市舶 / 南海 / 海商 → event_sea_trade_boom.webp
临安 / 繁华 / 民生 → event_linan_prosperity.webp
江淮 / 军务 / 前线 → event_jianghuai_battle.webp
西夏 / 马市 → event_xixia_horse_market.webp
大理 / 西南 → event_dali_trade.webp
皇城司 / 内侦 / 暗线 → event_secret_police.webp
```

## 事件类型兜底

```text
jin_event / diplomacy_event → 金使来朝
random_military / city_crisis → 江淮军务
talent_discovery → 岳飞请战
random_court → 御书房夜议
city_event → 临安繁华
folk_rumor → 皇城司密奏
```

## 效果

动态事件、JSON 数据事件、随机事件即使没有稳定 eventId，也更容易显示对应 CG，不会全部落到默认御书房图。

## 后续方向

1. 给事件系统增加标准 `artKey` 字段。
2. 把圣旨执行结果页也接入事件 CG。
3. 将事件 CG 与具体城池、人物、外交状态联动。
