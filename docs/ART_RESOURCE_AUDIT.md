# ART-001 — 全仓美术资产盘点与孤儿资源审计

> 调查/规划文档，不改游戏代码，不改 `docs/PROJECT_MASTER_PLAN.md`，不新增人物实体，
> 不新增玩法。基线：`release/v1.6.1`。另外交叉核实了 PR #54（COURT-001，
> `feat/court-001-crowd-wiring` 分支）和 `docs/ROSTER_EXPANSION_AUDIT.md`（ROSTER-001）。

---

## 一、方法论（先说清楚这份账本是怎么算出来的）

1. 完整枚举 `app/src/main/assets/images/` 下全部文件（449个，40M，除3个
   `.json` 清单外全是 `.webp`）。
2. 全仓搜索 `app/src/main/java/` 里出现过的每一条 `.webp` 字符串（含
   `"$BASE/xxx.webp"` 这种模板插值形式），按文件名做子串匹配，判定某个
   文件"在代码里有没有引用痕迹"。**第一版用精确字符串匹配漏掉了大量
   `$BASE/` 模板插值引用，已经改成子串匹配重新跑了一遍**——过程中的这个
   教训本身也值得记录：简单 grep 精确匹配对这类模板字符串场景不可靠。
3. 反向核对：从代码里提取所有完整路径声明（如
   `"images/xxx/yyy.webp"`），检查对应文件是否真实存在，找"registry 已
   登记但文件缺失"。
4. 抽样人工复核每个类别至少 1-2 个判定结果，确认自动化方法没有系统性
   偏差（过程中确实抓到过几次误判，见下）。
5. **"是否被引用"和"是否被 UI 真正消费"是两层不同的判定**：一个文件的
   路径字符串出现在某个 Registry 的 map 里，只说明它"被注册"；还要再确认
   这个 Registry 的访问函数有没有被任意 UI 文件调用，才算"真正接进正式
   流程"。本文档对重点对象（12名正式 Officer、COURT-001 素材等）做了
   这一层核实，对海量的常规资源（比如 130+ 张 UI 图标）只做了"是否被
   引用"这一层，不逐一验证每个图标是不是真的显示在某个具体屏幕上——
   工作量不允许，价值也有限。

**局限说明**：字符串子串匹配无法覆盖"纯运行时拼接、代码里连片段字面量
都没有"的极端情况（比如完全由外部 JSON 数据驱动生成的文件名）；本仓库
目前没有发现这种情况，但如果未来出现类似设计，这份账本需要重新核对。

---

## 二、总体统计

- **总文件数：449**（40M，446 个 `.webp` + 3 个 `.json` 清单）
- **代码中有引用痕迹：约 272**
- **疑似孤儿（全仓无任何引用痕迹）：约 174**（约 39%——将近四成的图片
  从未被任何代码提及）
- **反向核对发现的"注册但文件缺失"：10 条**（1 条真缺文件 + 9 条路径
  拼写错误，见下）

**一句话结论，跟你们猜测的一致**：这个项目现在的问题主要不是"没有图"，
是"有图但没人告诉游戏这张图存在"。将近四成的美术资源从落地那天起就没
被任何代码碰过。

---

## 三、三个最有价值的发现（比孤儿清单本身更重要）

### 发现 1：地图城池等级图标有一个路径拼写错误，殃及几乎全地图

**位置**：`app/src/main/java/com/xiemingxin/nandu/game/CityVisualRegistry.kt`

真实文件都在 `app/src/main/assets/images/map/icons/`（比如
`city_capital_song.webp`），但 `CityVisualRegistry.kt` 里**所有**
`mapIconPath` 声明都写成了 `"images/map/city_xxx.webp"`，漏了 `icons/`
这一层目录。这个错误出现在两处：

1. `important` 静态列表的全部 8 条记录（临安、建康、襄阳、燕京、合肥、
   寿春、兴庆府、大理——注意合肥和寿春共用同一条错误路径）；
2. 更严重的是**动态回退逻辑本身也漏了这一层**：
   `mapIconPath = "images/map/city_${tier.name.lowercase()}.webp"`——
   这意味着**不在 important 列表里的所有其它城市，走的也是同一个错误
   路径拼接规则**。

也就是说，地图上代表城池等级的图标（都城/大城/重镇/港市/关隘/州县……）
**很可能全部加载失败，正在用占位色块顶替**。这跟你们之前吐槽"地图还是
太垃圾"很可能直接相关——不是没画图标，是图标画好了但代码路径写错了，
一张都显示不出来。

修复本身极简单（每条路径加上 `icons/`），但按任务要求本次**只记录不
改代码**。

### 发现 2：项目里确实存在"美术双轨系统"，是有意设计，但会导致同一人物在不同页面显示不同版本立绘

**位置**：`app/src/main/java/com/xiemingxin/nandu/game/VisualAssetV3.kt`

这不是意外的技术债重复，代码注释写得很清楚，是故意设计的"精选层"：
`VisualAssetV3.portraitForOfficer(id)` 优先查自己的 `featuredCharacters`
表（8人：岳飞、韩世忠、秦桧、刘锜、吴玠、赵鼎、赵构、完颜宗弼，用的是
"batch1/batch2"这批后期修正版立绘），查不到才回退到
`ArtResourceRegistry.portraitForOfficer(id)`（主表，"portrait_xxx.webp"
这批早期版本）。

问题是：**不同 UI 文件对这两套系统的调用方式不统一**——

- `EmperorMainScreen.kt` 的朝会卡片（`OfficerMiniCard`）直接调用
  `ArtResourceRegistry.portraitForOfficer(officer.id)`，完全不经过 V3；
- `OfficerListScreen.kt`（人物列表/详情）调用
  `VisualAssetV3.halfbodyForOfficer(officer.id)` /
  `VisualAssetV3.portraitForOfficer(officer.id)`。

结果：秦桧（举例）在朝会主视图卡片上是一个版本的头像，玩家点进人物
详情页看到的是另一个版本——两张脸对不上。这 8 个人受影响
（yue_fei/han_shizhong/qin_hui/liu_qi/wu_jie/zhao_ding/zhao_gou/
wanyan_zongbi）。代码注释里还特别标了一句"V2 corrects Qin Hui's
headwear and is the preferred version"，说明 V3 版本本来就是被认定为
"更好的版本"——那更应该让所有界面统一走它，而不是各用各的。

### 发现 3：ROSTER-001 说"完颜昌/完颜宗翰需要新画"，其实这两位已经有现成立绘，只是没人接

这个发现直接更新了 `docs/ROSTER_EXPANSION_AUDIT.md` 里的判断。全仓搜索
确认，以下三张金国核心人物立绘**存在但从未被任何代码引用**：

- `characters/portrait_wanyan_chang.webp`（完颜昌／挞懒——ROSTER-001
  建议的 CORE 级人物，放归秦桧的关键角色）
- `characters/portrait_wanyan_zonghan.webp`（完颜宗翰／粘罕——
  ROSTER-001 建议的 CORE 级人物）
- `characters/officer_wanyan_zongwang_full_01.webp`（完颜宗望／斡离不
  ——`Faction("jin").rulerName` 字段写的就是他，但此前一直没有对应立绘，
  ROSTER-001 审计时也没发现这张图存在）

也就是说，ROSTER-001 建议优先补的 4 个 CORE 级人物里，金国那 3 个
（完颜宗弼已经在用、完颜宗翰、完颜昌）美术成本其实是**零**——图已经在
仓库里躺着，只差数据实体和接线。这是这次审计里最值得马上告诉负责排期
的人的一条信息。

---

## 四、按类别统计（用户要求的九类）

| 类别 | 对应目录 | 总数 | 孤儿数 | 说明 |
|---|---|---:|---:|---|
| 正式人物立绘 | `characters/`（去除通用/使者/占位） | ~70 | ~26 | 含 12 名正式 Officer + 8 名"有画无实体"人物（6 名 ROSTER-001 已知 + 2 名本次新发现的完颜宗翰/完颜宗望，完颜昌已计入 ROSTER-001 名单）+ 大量 batch1/batch2 旧版重复 |
| 普通朝官／朝堂群像 | 见下方 COURT-001 专项 | 54（另计，不在 449 总数内） | 见专项 | 这批在 `feat/court-001-crowd-wiring` 分支，不在本次盘点的 449 张基数里 |
| 城池／城池内景 | `city/`（30）+ `cities/`（17） | 47 | 15 | `cities/`（复数）目录整体是孤儿候选池，含应天府专属图（COURT-001 已处理，见专项） |
| 皇宫／建筑 | `palace/`（13）+ `buildings/`（15） | 28 | 2 | `buildings/` 全部被 `BuildingCatalog.kt` 用上了，干净；`palace/` 里 `frontier_pass.webp`/`military_camp.webp`/`port_market.webp` 三张不在 `PalaceIds` 八大殿注册表里，可能是城池内景专用，值得确认 |
| 剧情 CG | `events/` 里 `cg_*` 开头（4张，序章用）+ V3 event 常量（5张 batch） | 9 | 0 | 序章 CG 已接好 |
| 历史事件图 | `events/` 里 `event_*` 开头（27张） | 27 | 11 | 见下方专项列表，11张有明确叙事意图但无事件消费 |
| 战役图 | `battles/` | 8 | 6 | 6张（含"campt_night"夜营、"mountain_pass"山口、"river_naval"水战等）没有任何战役场景使用 |
| UI／纹理／装饰 | `ui/`（82）+ `map/decorations/`（10）+ `map/icons/`（16） | 108 | 24 | 见下方地图装饰专项 |
| 其他未分类 | `tech_policy/`（18，哈希命名）+ `npc_portraits/`（5，哈希命名）+ `transparent_candidates/`（55） | 78 | 78 | 全部是待整理素材池，见下 |

（表格数字有少量重叠/取整，不追求跟总数 449 完全对平——比如 `characters/`
里也有几张算作"事件相关"的，分类本身有交叉，这里按更常用的归属口径放。）

---

## 五、特别核实清单（任务里点名要确认的对象）

### 5.1 COURT-001（PR #54）的 54 张资源现状

**不在本次 449 张基数统计内**——这批文件目前只存在于
`feat/court-001-crowd-wiring` 分支，还没合并进 `release/v1.6.1`。核实
分支上的实际状态：

- 6 张殿内群像全景（`court_crowd_v1/`）：全部已注册进
  `ArtResourceRegistry.CourtNpc.crowdScenes`，且已经真正接进
  `EmperorMainScreen.CourtOfficerRow` 的背景层（半透明衬底）。
- 8 张无脸背影姿态（`c_rank_*`）：已注册进 `rankAndFilePoses`，已经真正
  接进 `CourtOfficerRow` 的"侧翼列班"。
- 24 张按职位分类的具名占位官员（12职位 × portrait+halfbody）：已注册
  进 `officePortraits`/`officeHalfbodies`，**只有其中被 `officialBySeed`
  按旬数抽中的那一位会显示在"当值官员"卡片上，其余职位的图长期处于
  "注册了但这局游戏可能永远抽不到"的半闲置状态**——这不算孤儿（确实
  被消费），但利用率取决于运气，值得记录。
- 16 张具名通用官员（仅 portrait）：已注册进 `namedGenericIds`，目前
  **代码里没有任何调用点真正使用这批**（`namedGenericPortrait`/
  `namedGenericPortraitBySeed` 函数存在，但全仓搜索没有任何地方调用
  它们）——16 张里 16 张都是"注册了但没接线"状态。

### 5.2 12 名正式 Officer 立绘完整度

| id | 姓名 | portrait | halfbody | 是否在 V3 精选层（双版本风险） |
|---|---|---|---|---|
| yue_fei | 岳飞 | 有 | 有 | 是 |
| han_shizhong | 韩世忠 | 有 | 有 | 是 |
| li_gang | 李纲 | 有 | 有 | 否 |
| zong_ze | 宗泽 | 有 | 有 | 否 |
| zhao_ding | 赵鼎 | 有 | 有 | 是 |
| qin_hui | 秦桧 | 有 | 有 | 是 |
| wu_jie | 吴玠 | 有 | 有 | 是 |
| zhang_jun | 张浚 | 有 | 有 | 否 |
| liu_qi | 刘锜 | 有 | 有 | 是 |
| zhang_jun2 | 张俊 | 只有单图（portrait=halfbody 复用） | 同左 | 否 |
| huang_qianshan | 黄潜善 | 只有单图 | 同左 | 否 |
| wang_boyan | 汪伯彦 | 只有单图 | 同左 | 否 |

**结论**：12 人全部"有图"，没有真正意义上缺立绘的正式 Officer。真正的
问题是 6 人受"双版本不一致"影响（发现2），以及张俊/黄潜善/汪伯彦这三人
只有单张图两头用，人物详情页的半身立绘位置显示的其实是头像放大版，
观感会比其他人粗糙。

### 5.3 "有图无实体"的历史人物（ROSTER-001 已知 6 名 + 本次新增 2 名）

| 姓名 | 图片 | 是否被引用 | 备注 |
|---|---|---|---|
| 吴璘 | `characters/wu_lin.webp` | 否 | ROSTER-001 已记录 |
| 杨沂中 | `characters/yang_yizhong.webp` | 否 | ROSTER-001 已记录 |
| 吕颐浩 | `characters/lv_yihao.webp` | 否 | ROSTER-001 已记录 |
| 朱胜非 | `characters/zhu_shengfei.webp` | 否 | ROSTER-001 已记录 |
| 胡铨 | `characters/hu_quan.webp` | 否 | ROSTER-001 已记录 |
| 完颜宗弼 | `characters/portrait_wanyan_zongbi.webp` + V3 精选版 | **是**（唯一一个已经被真正使用的） | ROSTER-001 已记录 |
| 完颜昌（挞懒） | `characters/portrait_wanyan_chang.webp` | 否 | **本次新发现**，ROSTER-001 未记录此图存在 |
| 完颜宗翰（粘罕） | `characters/portrait_wanyan_zonghan.webp` | 否 | **本次新发现** |
| 完颜宗望（斡离不） | `characters/officer_wanyan_zongwang_full_01.webp` | 否 | **本次新发现**，且是 `Faction.rulerName` 指名的人物 |

### 5.4 应天府专属城市图

本次盘点确认（跟 COURT-001 时发现的一致）：`cities/city_yingtianfu.webp`
在 `release/v1.6.1` 分支上仍然放在错误目录（复数 `cities/`），未接入
`ArtResourceRegistry.cityBackgrounds`。COURT-001 分支（PR #54）已经把
它移到正确目录并接好，但**这个修复还没合并进 `release/v1.6.1`**。

另外发现两张此前没人提过的应天府场景图，已经注册但从未被任何 UI 调用：
`locations/bg_yingtian_court_day.webp`（应天府朝堂·日）、
`locations/bg_yingtian_corridor.webp`（应天府走廊）——`ArtResourceRegistry
.locationBackground()` 这个访问函数本身在全仓库找不到任何调用点。

### 5.5 V3 美术库利用率

`VisualAssetV3.kt` 里声明的资源：8 人精选立绘（全部被使用，通过前面
提到的两个 UI 文件）、1 张顺昌城墙图（`SHUNCHANG_WALL`，需另查是否被
`ShunchangBattleScreen.kt`/新版战役屏消费）、1 张垂拱殿图
（`CHUIGONG_HALL`）、5 张 batch 事件 CG（通过 `eventImageFor()` 关键词
匹配，被 `StoryEventCard.kt` 调用）。V3 库本身体量不大（约 15 张），
利用率相对全仓其它批次是最高的一批。

---

## 六、注册但文件缺失 / 路径错误（反向核对结果，共 10 条）

| 声明位置 | 声明路径 | 问题类型 |
|---|---|---|
| `CityVisualRegistry.kt`（9条，含1条动态回退规则） | `images/map/city_*.webp` | **路径错误**，真实文件在 `images/map/icons/` 下，见发现1 |
| `PrologueScreen.kt` | `images/locations/yangzhou_river.webp` | 文件真的不存在（这是个 `cgFallback` 兜底路径，不是主路径，触发概率低但仍是死路径） |

---

## 七、明显重复／旧版／测试遗留

- `characters/batch1/`（6张）、`characters/batch2/`（12张）：早期修正版
  立绘，被 `VisualAssetV3.kt` 专门引用，不是废弃孤儿，但跟主表
  `portrait_xxx.webp`/`halfbody_xxx.webp` 构成"同一人物两个版本同时在
  用"的局面（发现2）。
- `characters/portrait_yue_fei_faction.webp`、
  `characters/halfbody_liang_hongyu.webp`、
  `characters/portrait_liang_hongyu.webp` 等"seven factions"批次：与
  主表命名体系不同，梁红玉甚至有立绘但连 Officer 实体都没有（ROSTER-001
  漏掉了这个人，这次一并提示）。
- `cities/`（复数目录，17张）：与 `city/`（单数，30张）存在若干同名或
  近义重复（chengdu/chuzhou/ezhou/jiankang/kaifeng/linan/xiangyang/
  yangzhou 都两边各有一张风格不同的图），像是"候选替换池"，需要人工
  判断哪版更适合正式使用，不是简单删掉一批。
- `palace/batch1/chuigongdian_batch1.webp`：垂拱殿修正版，被 V3 的
  `CHUIGONG_HALL` 引用，同样是"新旧版本并存"模式。
- `transparent_candidates/`（55张）：目录名本身说明这是一批"透明背景
  处理候选"，大概率是对应正式图的去背景版本，等待审核后替换，全部孤儿
  是符合目录性质的正常状态，不算异常。

---

## 八、三份清单

### A. 已经接好，可正常使用

- 12 名正式 Officer 的 portrait/halfbody（`characters/portrait_*.webp`
  等主表）
- V3 精选层 8 人立绘（会话5.5）
- `buildings/`（15张）全部接好
- 序章 4 张 CG + V3 的 5 张事件 CG
- 25 张已被事件系统消费的 `events/event_*.webp`
- COURT-001 的 6 张群像 + 8 张背影姿态（PR #54 分支上）

### B. 素材已有，只差低风险接线

- **11 张有明确叙事意图的历史事件图**（`event_discover_yue_fei_soldier`
  "发现岳飞小兵"、`event_han_shizhong_requests_battle`"韩世忠请战"、
  `event_li_gang_dismissed`"李纲罢相"、`event_qin_hui_returns_from_jin`
  "秦桧归宋"、`event_zong_ze_guards_bianjing`"宗泽守汴京"等）——配合
  已有的历史事件 JSON 数据接上即可，不需要动 STAB-006 已经修好的门控/
  回写逻辑本身。
- **完颜昌、完颜宗翰、完颜宗望三张金国人物立绘**（发现3）——只差
  ROSTER-001 那边给他们建 Officer 实体，图不用等。
- **`locations/bg_yingtian_court_day.webp` / `bg_yingtian_corridor.webp`**
  ——已注册未使用，可以直接替换掉 `PalaceHallScreen.kt`/`IntroScreen.kt`
  里那些通用宫殿图，让应天府开局的视觉真正对上"应天"而不是泛用宫殿图。
- **`map/decorations/` 里除 `selected_ring` 外的 9 张**（雾遮罩、前线
  警示、河流/山脉标签、贸易路线光效、宋金军旗）——正是能直接提升地图
  "活地图"观感的现成素材，之前聊天里猜到了这批存在，这次确认属实。
- COURT-001 的 16 张具名通用官员 + 部分未被抽中的职位官员图。
- 6 张 ROSTER-001 已知的"有图无实体"人物 + 本次新增的完颜昌/宗翰/宗望。

### C. 需要 V1.7/V1.8 人物/事件系统成熟后再接

- 6 张战役图（`battles/`，山口/水战/夜营等场景，适合配合 HIST-004"战役
  由世界态势长出来"再统一设计怎么用，现在接了也只是又一批孤立展示图）
- `tech_policy/`（18张）、`npc_portraits/`（5张）——哈希命名，语义不明，
  需要先有对应的"科技/政策系统"或"NPC 群像系统"设计出来才知道怎么用，
  现在接线等于瞎猜用途
- `cities/`（复数目录）里那批 CG 级场景图（`city_imperial_capital_dawn`
  "帝都黎明"、`city_kaifeng_crisis`"开封危局"等）——命名风格明显是给
  特定战役/事件用的氛围图，不是城池代表图，需要先有对应事件才能用对
  地方
- "seven factions" 批次的旗帜/纹章/边框（`ui/faction_flags/`
  `ui/faction_frames/` 等 10 张）——对应的是一套更完整的"七国势力"UI，
  当前游戏只有 4 个可互动 Faction，这批素材是为将来扩展准备的，现在接
  等于给不存在的功能做界面

---

## 九、最值得优先利用的前 20 个现有孤儿资源

按"零新增美术成本 + 接线复杂度低 + 对玩家可见观感提升明显"排序：

1. 完颜昌立绘（`portrait_wanyan_chang.webp`）——ROSTER-001 CORE 级人物，零成本
2. 完颜宗翰立绘（`portrait_wanyan_zonghan.webp`）——同上
3. 完颜宗望立绘（`officer_wanyan_zongwang_full_01.webp`）——`Faction.rulerName` 指名的人物，长期没有对应的脸
4. `locations/bg_yingtian_court_day.webp`——应天府朝堂日景，已注册未使用
5. `locations/bg_yingtian_corridor.webp`——应天府走廊，已注册未使用
6. `map/decorations/fog_overlay.webp`——战争迷雾，地图氛围感直接相关
7. `map/decorations/frontline_warning.webp`——前线警示动画素材
8. `map/decorations/river_label.webp` / `mountain_label.webp`——地图地理标签，直接回应"地图不够真实"的诉求
9. `map/decorations/trade_route_glow.webp`——贸易路线光效
10. `map/decorations/army_banner_song.webp` / `army_banner_jin.webp`——地图军旗标识
11. `event_discover_yue_fei_soldier.webp`——"发现岳飞小兵"，叙事意图最明确的孤儿事件图之一
12. `event_han_shizhong_requests_battle.webp`——"韩世忠请战"
13. `event_li_gang_dismissed.webp`——"李纲罢相"
14. `event_qin_hui_returns_from_jin.webp`——"秦桧归宋"，直接对应 Canon v1.1 已经写好的历史节点
15. `event_zong_ze_guards_bianjing.webp`——"宗泽守汴京"
16. `event_yue_fei_northern_expedition_01.webp`——"岳飞北伐"
17. `event_jianyan_first_court_shock.webp`——"建炎首次朝议震动"，适合做开局早期事件
18. COURT-001 的 16 张具名通用官员——分支已经落地，只差调用点
19. 吴璘立绘（`wu_lin.webp`）——ROSTER-001 里叙事权重较高的人物之一，零成本
20. 朱胜非立绘（`zhu_shengfei.webp`）——ROSTER-001 里苗刘兵变专属线索人物之一，零成本

---

## 十、明确没做的事（按任务要求）

- 没有新增任何 Officer 实体，没有把 ROSTER-001 那 35 人的任何一个真的
  塞进 `InitialData.officers`。
- 没有创建假的普通官员 Officer。
- 没有修改任何 Work 正在集成的核心业务代码（`CityVisualRegistry.kt` 的
  路径 bug 只是记录，没有动手改一个字符）。
- 没有改版本号，没有碰历史事件核心逻辑，没有合并 main。
