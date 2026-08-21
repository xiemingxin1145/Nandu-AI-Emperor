# V1.6.2 预验收风险与集成交接

> 范围：`integration/v1.6.2-preacceptance`。这是风险记录，不是发布通过证明。
>
> Claude 的 STAB-006 已从 PR #60 正确合入；本集成任务只保留其原始事件核心和回归测试，不额外重写建炎事件或人物系统。

## 风险等级

- `P0 / RELEASE BLOCKER`：不解决不得交付 V1.6.2。
- `P1 / SMOKE BLOCKER`：正式入口体验存在明显错误，STAB-007 应修复或给出明确处置。
- `P2 / FOLLOW-UP`：不一定阻止本次集成，但必须保留在后续交接中。

| 编号 | 等级 | 发现位置 | 事实与影响 | 当前状态 / 建议负责人 |
| --- | --- | --- | --- | --- |
| RISK-001 | P1 / RESOLVED IN FINAL INTEGRATION | `MainActivity.kt` / `AppNavigationPolicy.kt` | 主菜单路由现在显式避开 `showSettings = true`，设置页能从主菜单和游戏中进入；返回只关闭当前设置层。 | 已修复；`AppNavigationPolicyTest` 覆盖主菜单 -> 设置 -> 主菜单。实际点击仍需真机。 |
| RISK-002 | P0 / WAITING FOR DEVICE AUDIO APPROVAL | `AudioResourceRegistry.kt` 与 `app/src/main/assets/audio/bgm/` | 已恢复 8/8 来自用户原始音乐的正式槽位候选 OGG，并附 `docs/audio/BGM_V162_DEVICE_CANDIDATE_SHA256.txt`；这些文件技术格式合规，但不是旧版逐字节归档，尚未完成玩家真机试听。 | 音频文件缺失已解除，最终审美与场景适配尚未验收；`MEDIA-03` 在用户确认前继续 `BLOCKED`，禁止把候选文件冒充已批准正式配乐。 |
| RISK-003 | P0 / RESOLVED IN FINAL INTEGRATION | `VictoryJudge.kt` | 亡国判定改为依据玩家势力当前 `capitalCityId` 与真实 `City.isCapital`，不再把 1127 年杭州固定当首都；结局文案改为“行在失守”。 | 已修复；5 组回归覆盖杭州失守不亡国、应天失守亡国、迁都和状态同步。 |
| RISK-004 | P1 / RESOLVED IN FINAL INTEGRATION | `MainActivity.kt` 军务入口 | 军务页左上返回已接回真实皇宫页面，不再传入空回调。 | 已修复；源码守卫与统一返回策略回归均覆盖。实际点击仍需真机。 |
| RISK-005 | P1 / DEVICE_REQUIRED | 全部二级 Compose 页面 | 统一 Compose `BackHandler` 已按设置、序章、内景、宫殿、战役、人物列表和主导航分层处理；天命绘卷也有独立返回处理。 | 代码和 6 组 JVM 路由回归已通过静态检查；Android 实体返回键和手势仍必须由真机逐个验证。 |
| RISK-006 | P2 / RESOLVED BY STAB-006 | `IntroScreen.kt` | 遗留组件固定“临安行在”文案已由 Claude 的 STAB-006 修正。 | 已随 PR #60 合流；保持原始修复，不另外重写历史事件核心。 |
| RISK-007 | P1 / RESOLVED IN FINAL INTEGRATION | `MainMenuScreen.kt` | 主菜单公开的 Demo 字样已替换为不含版本占位的历史文案“建炎元年 · 山河待兴”。 | 已修复；源码守卫禁止 Demo / 测试版重新回到正式主菜单。 |
| RISK-008 | P0 / RESOLVED IN FINAL INTEGRATION | STAB-006 历史事件分支 | PR #60 已修正 base 为 `fix/stab-005-video-player-compat`，并将 11 个 STAB-006 文件完整合流到 #59 集成线。 | 已合流；前置事件、人物生死、旗标、城市归属、概率、世界效果及年号切换统一参与完整 CI。 |
| RISK-009 | P1 / RESOLVED IN PREP | `CgVideoDialog.kt` | 事件卡“播放过场 CG”曾使用正式可达的 `android.widget.VideoView` + `file:///android_asset`，意味着 STAB-005 声称的视频单链路实际上漏掉了一条。 | 已在本预验收分支迁移为统一 `AssetVideoSurface`，视频始终静音，并增加静态 CG 保底与真实错误分类；源码守卫禁止 VideoView 回流。 |
| RISK-010 | P1 / RESOLVED IN PREP | `PROJECT_MASTER_PLAN.md` | cherry-pick COURT-001 后，文档出现两条同名任务：一处 `IN_PROGRESS`，另一处 `DONE`，容易误导其他 AI 重做已完成工作。 | 已手工合并为唯一 `DONE` 记录，保留 STAB-001～005、COURT、ROSTER 与预验收全部交接信息。 |
| RISK-011 | P1 / RESOLVED BY ART-HOTFIX-001 | `CityVisualRegistry.kt` / `MapScreen.kt` | 15 条重点城市地图图标路径和动态回退规则均漏了 `icons/`；地图正式界面还绕开 `mapIconPath`，动态城市背景也会漏掉鄂州、扬州这类已注册专属图。 | 已修复；16/16 图标、24 个别名、全部地图节点、动态势力首都图标、31 个城市背景及 APK 素材均有自动守卫。 |
| RISK-012 | P2 / FOLLOW-UP | 地图旧素材池与专属城池图 | 10 张地图装饰实际是半透明方框标记，不能把 `fog_overlay` 当真实全屏雾层；开局 36 座实际城市中另有 18 座尚无正式专属背景。 | 已安全接入选中框、前线预警、商路标记、宋军旗和金军旗；雾层、山河标签、区域牌、路线箭头暂缓，未注册城市保留既有通用背景，等待后续专项处理。 |
| RISK-013 | P1 / RESOLVED BY WORLD-UX-001 | `EmperorMainScreen.kt` / `EmperorViewModel.kt` | 群臣奏议此前只能浏览，玩家不知道实际采纳谁；需要澄清时仍可直接执行，命令预览还直接显示人物/城池内部 ID。 | 已接入臣议单选、多选、综合、保留上下文补充圣意和朱批双重门控；人物、军团、城市与命令全部转换为玩家可读中文。实际点击仍需真机。 |
| RISK-014 | P1 / RESOLVED BY WORLD-UX-001 | 世界回合状态 / `MapScreen.kt` / 四季演出 | 天下回合此前只给文字报告，独立推进历法时没有同步刷新季节，导致已存在的 4 组四季视频/CG 无法可靠进入正式流程。 | 已基于回合前后真实军团、补给、城池变化生成可跳过地图推演；仅实际季节切换时播放静音 Media3 视频，四张静态季节 CG 保底，不伪造不存在路线。 |
| RISK-015 | P1 / RESOLVED BY DELEGATION-INTEGRATION-001; DEVICE_REQUIRED | 宋军 AI 授权执行与朝议朱批 | Claude PR #64 已按 12 个实际后端/测试变更选择性接入 #59；长期圣旨会创建可保存、可撤销授权，按真实人物位置、地域、人口、守军、钱粮和禁战边界执行，并将实际募兵/修防/任将写入世界推演。 | 自动回归覆盖三档自治、无直属军团募兵、预算枯竭、禁止交战、撤销、亲令覆盖、执行日志和存档往返；完整朝议输入与多旬实际效果仍需手机验收。 |
| RISK-016 | P2 / LIMITED SCOPE | `WorldContextFactory` / 授权行动候选 | 当前世界模型上下文尚未把全部宋廷授权事项整理进同一次 `strategyCandidates`；本轮不擅自扩大模型协议。 | `奉旨而行` / `便宜从事` 对已有正式入口的募兵、修防和模型实际提交的任将、补给、调兵、交战进行本地裁决；复杂跨区域长期战略、多轮调运和外交后续专项迭代。 |

## BGM 待真机试听清单

```text
audio/bgm/bgm_main_menu.ogg
audio/bgm/bgm_chuigong_hall_entry.ogg
audio/bgm/bgm_chuigong_hall_loop.ogg
audio/bgm/bgm_garden_loop.ogg
audio/bgm/bgm_study_loop.ogg
audio/bgm/bgm_worldmap_loop.ogg
audio/bgm/bgm_linan_loop.ogg
audio/bgm/bgm_military_camp_loop.ogg
```

上述 8 个正式注册槽位现在已有用户来源的候选 OGG，且 SHA-256 清单已入仓；由于其与旧版归档并非逐字节一致，是否通过仍取决于用户在真实手机逐场景试听，不能根据格式检查直接宣布批准。

## 本分支保护边界

Claude 历史事件原始实现仅允许通过 PR #60 合流；本轮另外只接受 PR #64 对 `StoryEventEffectApplier.kt` 已审查的人物忠诚度正规任命系统接线，不额外重写：

```text
app/src/main/java/com/xiemingxin/nandu/story/EventDirector.kt
app/src/main/java/com/xiemingxin/nandu/story/StoryEventLoader.kt
app/src/test/java/com/xiemingxin/nandu/story/StoryEventTriggerGuardTest.kt
app/src/test/java/com/xiemingxin/nandu/story/StoryEventEffectApplierTest.kt
建炎事件 JSON 的历史门控逻辑
app/build.gradle.kts 中的 versionCode / versionName / signingConfigs
```

## 实际已整合内容

- STAB-001～006 已验证修复链，基线 PR #58，STAB-006 通过 PR #60 合流。
- COURT-001 两个独立提交：54 张群像 / 普通朝官透明图、真实注册、殿内列班、应天府图像接线、应天 / 杭州首都视觉纠正。
- ROSTER-001 审计文档一份；正式 Officer 仍维持原来的 12 人。
- 正式剧情 CG 统一接入 Media3，移除最后一条 VideoView 路径。
- 预验收 JVM 单测、54 张图片 SHA-256 全量校验、应天资源校验、六幕旁白校验，以及 51 条 APK 内视频逐条 ffprobe。
- `docs/V162_SMOKE_TEST_MATRIX.md` 中的正式入口逐项检查表。
- 主菜单设置、真实首都亡国判定、军务返回、系统返回、绘卷返回和 Demo 文案修复。
- `ART-HOTFIX-001`：15 条重点城市图标路径 + 1 条动态规则修复；16 张图标 / 24 个注册别名、31 张城市背景与 5 个真实状态驱动地图装饰接线。
- `WORLD-UX-001` + `DELEGATION-INTEGRATION-001`：真实臣议裁决、三档皇帝授权、预算/禁战/人物位置门控、自动募兵与修防、亲令覆盖、可追责持久化记录和实际世界行动回放。

## STAB-008 之前仍需完成

1. 对现已恢复的 **8/8 用户来源 BGM 候选**完成真实手机试听，确认主菜单、皇宫、地图与军务等场景适配；确认前继续视为人工验收阻塞。
2. 重新核对 OGG Vorbis / 48 kHz / stereo、SHA-256 候选清单、注册表命中、真实 APK 入包和各场景试听，不允许注入旧污染音频或空文件。
3. 由真实 Android 手机完成其余 48 项入口交互验收，以及 BGM 恢复后的第 49 项。
4. 检查 Android 返回键 / 手势、视频硬件解码、序章前后幕、旁白切换、真实模型中转与存档导入导出。
5. STAB-007 全部通过前保持 `IN_PROGRESS` 与 **6/8**；不要开启 STAB-008、升级 versionCode 或合 main。
