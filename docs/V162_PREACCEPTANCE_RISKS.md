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
| RISK-002 | P0 / RELEASE BLOCKER | `AudioResourceRegistry.kt` 与 `app/src/main/assets/audio/bgm/` | 8 首审核通过的正式 OGG 已注册，但仓库 BGM 目录只有 README，8/8 二进制均不存在。以仓库直接构建的 CI APK 因此没有正式 BGM。此前可试听版本依赖构建后外部注入，不能被当前 CI 重现。 | 未修改；STAB-008 前恢复**相同已验收音频**，不得回灌旧污染 BGM。审计脚本与 APK 步骤持续输出明确 warning。 |
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

## BGM 缺失清单

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

这些文件不是“允许为空的 pending BGM 槽位”。它们已被主菜单、皇宫、地图、城池与军务正式路由引用，缺失必须作为真实发布阻塞记录。

## 本分支保护边界

Claude 原始实现仅允许通过 PR #60 合流；本次最终集成不额外改写：

```text
app/src/main/java/com/xiemingxin/nandu/story/EventDirector.kt
app/src/main/java/com/xiemingxin/nandu/story/StoryEventLoader.kt
app/src/main/java/com/xiemingxin/nandu/story/StoryEventEffectApplier.kt
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

## STAB-008 之前仍需完成

1. 找回 8 首此前人工验收通过的原始正式 OGG；目前仓库和项目文件中均没有这些音乐，状态真实为 **0/8 BLOCKED**。
2. 重新核对 OGG Vorbis / 48 kHz / stereo、注册表命中、真实 APK 入包和各场景试听，不允许注入旧污染音频或空文件。
3. 由真实 Android 手机完成其余 48 项入口交互验收，以及 BGM 恢复后的第 49 项。
4. 检查 Android 返回键 / 手势、视频硬件解码、序章前后幕、旁白切换、真实模型中转与存档导入导出。
5. STAB-007 全部通过前保持 `IN_PROGRESS` 与 **6/8**；不要开启 STAB-008、升级 versionCode 或合 main。
