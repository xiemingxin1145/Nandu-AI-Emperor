# 事件CG映射表（给ArtResourceRegistry用）

豆包已生成10张事件CG，已上传到 `app/src/main/assets/images/events/`，
文件名按 event_id 命名，可直接在 eventImages 映射里补充：

| event_id | CG文件路径 |
|----------|-----------|
| jianyan_first_court_shock | events/event_jianyan_first_court_shock.webp |
| discover_yue_fei_soldier | events/event_discover_yue_fei_soldier.webp |
| han_shizhong_requests_battle | events/event_han_shizhong_requests_battle.webp |
| zong_ze_guards_bianjing | events/event_zong_ze_guards_bianjing.webp |
| li_gang_dismissed | events/event_li_gang_dismissed.webp |
| jin_army_crosses_huai | events/event_jin_army_crosses_huai.webp |
| jin_general_wanyan_zongbi_moves | events/event_jin_general_wanyan_zongbi_moves.webp |
| qin_hui_returns_from_jin | events/event_qin_hui_returns_from_jin.webp |
| yangzhou_panic_flee | events/event_yangzhou_panic_flee.webp |
| city_siege_jiankang | events/event_city_siege_jiankang.webp |

## 建议接法

ArtResourceRegistry.eventImages 里，key 直接用 event_id，
这样 StoryEventCard 显示时可以直接：
```
ArtResourceRegistry.eventImage(event.eventId)
```
找不到时自动回退 placeholder_event.webp。

## 城池背景（已全部对齐现有命名）

新增/更新的城池背景，路径与现有 cityBackgrounds 映射完全一致：
chengdu / chuzhou / ezhou / jiankang / kaifeng / linan / xiangyang / yangzhou / yingtianfu / zhenjiang

其中 chuzhou / yingtianfu / zhenjiang 是新补的，建议在 cityBackgrounds 里补这三个key。
