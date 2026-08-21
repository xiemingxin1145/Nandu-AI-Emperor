# V1.6.2 预验收风险与集成交接

> 范围：`integration/v1.6.2-preacceptance`。这是风险记录，不是发布通过证明。
>
> Claude 正在独立修改 STAB-006 历史事件核心；本分支不得修改其 `EventDirector.kt`、`StoryEventLoader.kt`、`StoryEventEffectApplier.kt`、新增事件测试或建炎事件 JSON。

## 风险等级

- `P0 / RELEASE BLOCKER`：不解决不得交付 V1.6.2。
- `P1 / SMOKE BLOCKER`：正式入口体验存在明显错误，STAB-007 应修复或给出明确处置。
- `P2 / FOLLOW-UP`：不一定阻止本次集成，但必须保留在后续交接中。

| 编号 | 等级 | 发现位置 | 事实与影响 | 当前状态 / 建议负责人 |
| --- | --- | --- | --- | --- |
| RISK-001 | P1 / SMOKE BLOCKER | `MainActivity.kt` 主菜单分支 | 主菜单 `if (showIntro && !showPrologue)` 在 `if (showSettings)` 之前立即 `return`；主菜单点击“世事设置”仅把 `showSettings` 设为 true，重组后仍再次命中主菜单，设置页无法从主菜单进入。游戏内设置入口不受该提前返回影响。 | 未修改；STAB-007 导航修复。必须补主菜单 -> 设置 -> 主菜单回归。 |
| RISK-002 | P0 / RELEASE BLOCKER | `AudioResourceRegistry.kt` 与 `app/src/main/assets/audio/bgm/` | 8 首审核通过的正式 OGG 已注册，但仓库 BGM 目录只有 README，8/8 二进制均不存在。以仓库直接构建的 CI APK 因此没有正式 BGM。此前可试听版本依赖构建后外部注入，不能被当前 CI 重现。 | 未修改；STAB-008 前恢复**相同已验收音频**，不得回灌旧污染 BGM。审计脚本与 APK 步骤持续输出明确 warning。 |
| RISK-003 | P0 / RELEASE BLOCKER | `VictoryJudge.kt` | 开局真实首都是 `yingtianfu`，但即时失败条件仍固定查找 `linan` 并写死“临安失守 → 亡国”。杭州失守会错误亡国，而应天失守可能不触发对应结局，直接违背世界状态唯一真相。 | 未修改；与 STAB-006 全仓历史硬编码审计有关，应交由 Claude 或最终集成者明确处理并补双向回归。 |
| RISK-004 | P1 / SMOKE BLOCKER | `MainActivity.kt` 军务入口 | `MilitaryScreenV4(..., onBack = {})` 传入空回调。若页面显示内部返回按钮，用户点击不会产生任何动作。底部导航可以切标签，但不能替代页面返回契约。 | 未修改；STAB-007 检查实际返回控件并修复或移除不可用入口。 |
| RISK-005 | P1 / SMOKE BLOCKER | 全部二级 Compose 页面 | 全仓未发现统一的 Compose `BackHandler`；页面返回图标与 Android 系统返回键 / 返回手势可能行为不同，甚至直接退出 Activity。仅静态检查无法证明真机行为。 | 未修改；手机逐个验收设置、宫殿、内景、CG、序章、绘卷与战役。 |
| RISK-006 | P2 / FOLLOW-UP | `IntroScreen.kt` | 遗留旧 Intro 组件仍保留 `contentDescription = "临安行在"`；当前正式 `NanduApp` 使用 `PrologueScreen`，未发现 `IntroScreen(` 正式调用，因此属于遗留代码而非当前主入口穿帮。 | 未修改；清理遗留入口或修正文案，避免未来重新接入时回归。 |
| RISK-007 | P1 / SMOKE BLOCKER | `MainMenuScreen.kt` | 主菜单底部仍公开显示 `"建炎元年  Demo 版"`，与 V1.6.2 去 Demo 化目标不符。 | 未修改；STAB-007 替换为真实版本或中性历史文案，禁止写死旧版本号。 |
| RISK-008 | P0 / RELEASE BLOCKER | STAB-006 历史事件分支 | `fix/stab-006-historical-hardcode-audit` 尚未合入当前预验收线，因此前置事件、人物生死、旗标、城市归属、概率与剧情世界效果不代表最终完成状态。 | `WAIT-006`；仅在 Claude 两套 CI 完成后合入，再重跑完整矩阵。 |
| RISK-009 | P1 / RESOLVED IN PREP | `CgVideoDialog.kt` | 事件卡“播放过场 CG”曾使用正式可达的 `android.widget.VideoView` + `file:///android_asset`，意味着 STAB-005 声称的视频单链路实际上漏掉了一条。 | 已在本预验收分支迁移为统一 `AssetVideoSurface`，视频始终静音，并增加静态 CG 保底与真实错误分类；源码守卫禁止 VideoView 回流。 |
| RISK-010 | P1 / RESOLVED IN PREP | `PROJECT_MASTER_PLAN.md` | cherry-pick COURT-001 后，文档出现两条同名任务：一处 `IN_PROGRESS`，另一处 `DONE`，容易误导其他 AI 重做已完成工作。 | 已手工合并为唯一 `DONE` 记录，保留 STAB-001～005、COURT、ROSTER 与预验收全部交接信息。 |

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

禁止改动：

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

- STAB-001～005 已验证修复链，基线 PR #58。
- COURT-001 两个独立提交：54 张群像 / 普通朝官透明图、真实注册、殿内列班、应天府图像接线、应天 / 杭州首都视觉纠正。
- ROSTER-001 审计文档一份；正式 Officer 仍维持原来的 12 人。
- 正式剧情 CG 统一接入 Media3，移除最后一条 VideoView 路径。
- 预验收 JVM 单测、54 张图片 SHA-256 全量校验、应天资源校验、六幕旁白校验，以及 51 条 APK 内视频逐条 ffprobe。
- `docs/V162_SMOKE_TEST_MATRIX.md` 中的正式入口逐项检查表。

## 合入 STAB-006 后仍需完成

1. 手工合并总纲，保持主线任务顺序与唯一 NEXT。
2. 处理 `RISK-001`、`RISK-003`、`RISK-004`、`RISK-007`。
3. 恢复 8 首已验收正式 BGM，并重新核查 APK 内文件与播放。
4. 完整执行手机 Smoke Test，特别是系统返回、视频硬件解码和音频路由。
5. 只有进入 STAB-008 时才提升 `versionName = V1.6.2`、`versionCode = 29`，继续使用固定开发签名。
6. 两套 CI、签名、APK 拆包和手机验收都通过之前，不得合 main。
