# COURT_NPC_EXISTING_ASSET_AUDIT.md

扫描路径：`app/src/main/assets/images/characters/`  
基线：`fix/prologue-audio-timeline-20260820`  
范围：**非历史核心名人**素材（历史半身/头像仅作对照，不计入 KEEP 池）

---

## 非历史 / 通用素材审计

| 文件 | 用途 | 性别 | 年龄感 | 服饰 | 官职适配 | 朝堂 | 详情 | 结论 |
|---|---|---|---|---|---|---|---|---|
| bureau_clerk.webp | 给事中/书吏占位 | 男 | 中年 | 模糊文官 | 属官 | 勉强 | 弱 | **LIMITED_USE** |
| generic_scholar.webp | 文臣通用 | 男 | 中年 | 通用儒服 | 文官 | 可应急 | 弱 | **LIMITED_USE** |
| generic_general.webp | 武将通用 | 男 | 中年 | 甲胄感 | 武臣 | 限军务 | 弱 | **LIMITED_USE** |
| generic_merchant.webp | 商人 | 男 | 中年 | 民服 | 非朝官 | 否 | 市井 | **NOT_FOR_COURT** |
| generic_spy.webp | 密探 | 男 | 青壮 | 便服 | 皇城/情报 | 否 | 特殊 | **NOT_FOR_COURT** |
| eunuch_pressman.webp | 内侍 | 男(阉) | 中年 | 内侍服 | 内侍 | 可 | 弱 | **LIMITED_USE** |
| npc_eunuch_steward.webp | 内侍管事 | 男(阉) | 中年 | 内侍 | 内侍 | 可 | 弱 | **LIMITED_USE** |
| palace_attendant.webp | 宫人/侍从 | 偏女/不明 | 年轻 | 宫装 | 内廷 | 远景 | 弱 | **LIMITED_USE** |
| female_officer.webp | 女官 | 女 | 中青 | 女官服 | 后宫线 | 非垂拱文班 | 可 | **LIMITED_USE** |
| imperial_doctor.webp | 太医 | 男 | 中老 | 医官 | 太医 | 特殊 | 可 | **KEEP** |
| senior_nanny.webp | 乳母/老妪 | 女 | 老 | 民/内 | 后宫 | 否 | 后宫 | **NOT_FOR_COURT** |
| sea_merchant.webp | 海商 | 男 | 中年 | 商贾 | 市舶 | 否 | 城内 | **NOT_FOR_COURT** |
| jin_envoy.webp | 金使 | 男 | 中年 | 金人服饰 | 使节 | 外交场景 | 可 | **KEEP**（非宋官） |
| goryeo_envoy.webp | 高丽使 | 男 | 中年 | 使团 | 使节 | 外交 | 可 | **KEEP** |
| dali_envoy.webp | 大理使 | 男 | 中年 | 使团 | 使节 | 外交 | 可 | **KEEP** |
| xixia_envoy.webp | 西夏使 | 男 | 中年 | 使团 | 使节 | 外交 | 可 | **KEEP** |
| placeholder_portrait.webp | 占位 | — | — | — | — | 否 | 否 | **REPLACE_LATER** |
| placeholder_halfbody.webp | 占位 | — | — | — | — | 否 | 否 | **REPLACE_LATER** |
| officer_young_guard_general_full_01.webp | 年轻禁军将 | 男 | 青年 | 武将 | 禁军 | 军务 | 可 | **LIMITED_USE** |
| officer_veteran_frontier_general_full_01.webp | 老边将 | 男 | 老 | 武将 | 边军 | 军务 | 可 | **LIMITED_USE** |
| officer_wartime_strategist_full_01.webp | 谋士型 | 男 | 中年 | 文/策 | 幕僚 | 有限 | 可 | **LIMITED_USE** |
| officer_jin_frontier_commander_full_01.webp | 金边将 | 男 | 中年 | 金军 | 敌军 | 否（敌） | 可 | **NOT_FOR_COURT** |

历史人物半身/头像（岳飞、李纲、秦桧等）**不纳入普通 NPC 池**，由历史 Canon 与定装管理。

### 统计

| 结论 | 数量（上表） |
|---|---|
| KEEP | 5（太医 + 四使节） |
| LIMITED_USE | 11 |
| REPLACE_LATER | 2（placeholder） |
| NOT_FOR_COURT | 5 |

**结论：现有通用朝官严重不足。** 朝堂无法靠 1 个 `bureau_clerk` + 若干 generic 支撑“百官列班”。必须新建 36 人模板库。
