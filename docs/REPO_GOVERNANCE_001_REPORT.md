# REPO-GOVERNANCE-001 — 仓库治理审计报告

> 执行日期：2026-08-21 ｜ 执行分支：`docs/repo-governance-v1`（基于 `release/v1.6.1`）
>
> 范围：Open/Draft PR 审计归档、docs 统一索引、治理一致性检查、仓库卫生清单。
> 本报告只做记录与建议，**未关闭/合并任何 PR，未修改任何功能代码**。

---

## 一、PR 总览

当前共 **23 个 Open PR，全部为 Draft，无一合并**。PR 形成三条并行线：

1. **V1.6.x 发布与稳定化主线**（release/v1.6.1 → STAB 链 → V162 最终集成）
2. **序章/音频修复线**（fix/prologue-audio-timeline-20260820 → audio-cleanroom）
3. **living-world Canon 恢复线**（integration/living-world-canon-recovery-v1 及其 CI 验证 PR）

## 二、逐项审计表

| PR | 标题 | 分支（head → base） | 状态 | 已合并 | 主要内容 | 保留价值 |
| ---: | --- | --- | --- | --- | --- | --- |
| #50 | Release V1.6.1 RC | release/v1.6.1 → main | Draft | 否 | versionCode 28、固定测试签名、RELEASE_POLICY CI guard、序章节奏优化 | **高**：发布收口线 |
| #53 | STAB-001 gate + STAB-002 dynamic battle briefing | integration/v1.6.2-stab-001-002 → release/v1.6.1 | Draft | 否 | 顺昌入口门控 + 动态战役展示层，删除约 960 行写死 Demo | **高** |
| #54 | COURT-001 朝会列班视觉接线 | feat/court-001-crowd-wiring → release/v1.6.1 | Draft | 否 | 54 张朝堂群像接线、应天府城池图修复 | **高** |
| #55 | STAB-003 persistent battle directives | fix/stab-003 → integration/v1.6.2-stab-001-002 | Draft | 否 | 战役指令真实回写 GameState，存档可存活 | **高** |
| #56 | STAB-004 palace blank tasks | fix/stab-004 → fix/stab-003 | Draft | 否 | 宫殿待办空白修复 + 裁断后果分支 | **高** |
| #57 | ROSTER-001 人物池扩容审计 | docs/roster-001-expansion-audit → release/v1.6.1 | Draft | 否 | 仅新增审计文档 + 35 人候选名单 | 中（文档价值） |
| #58 | STAB-005 Media3 视频播放统一 | fix/stab-005 → fix/stab-004 | Draft | 否 | MediaPlayer → Media3 ExoPlayer，失败分类诊断 | **高** |
| #59 | V162 final integration | integration/v1.6.2-preacceptance → fix/stab-005 | Draft | 否 | 选择性合流 STAB-001~006 + COURT-001 + WORLD-UX 等，28 commits / 116 files | **高**：V1.6.2 验收枢纽 |
| #60 | STAB-006 年号推进 + 硬编码审计 | fix/stab-006 → fix/stab-005 | Draft | 否 | 建炎→绍兴年号修复、开场硬编码清理，自报 DONE | **高** |
| #61 | ART-001 美术资产盘点 | art-001-resource-audit → release/v1.6.1 | Draft | 否 | 449 张图盘点，约 39% 疑似孤儿，三个核心发现 | 中（文档价值） |
| #62 | AUDIO-RECOVERY-001 BGM 失踪审计 | docs/audio-recovery-001 → release/v1.6.1 | Draft | 否 | 结论：8 个正式 BGM 槽位 0 个可从 git 恢复二进制 | 中（文档价值） |
| #63 | AUDIO-RECOVERY-002 BGM 权威修正 | fix/audio-recovery-v162 → release/v1.6.1 | Draft | 否 | V1.6.1 BGM 二进制替换（进行中，标注 DO NOT INTEGRATE YET） | **高**（待完成） |
| #64 | DELEGATION-001 Imperial Mandate | feat/delegation-001-ai-government → integration/v1.6.2-preacceptance | Draft | 否 | 皇帝授权制 + DelegatedActionValidator，13 个新测试 | **高** |
| #65 | MAP-PROTOTYPE-001 双层天下地图预研 | prototype/map-globe-v1 → main | Draft | 否 | 伪球面投影原型 + 三路线对比文档，不碰正式山河页 | 中（独立预研） |
| #66 | MAP-PROTOTYPE-INTEGRATION-001 寰宇试览 | feat/map-globe-device-prototype → integration/v1.6.2-preacceptance | Draft | 否 | 择取 #65 的 4 个原型文件做真机试览 | 中（原型验证） |
| #38 | World Roster Foundation V1 | world-roster-foundation-v1 → main | Draft | 否 | 独立 world 包底座（人物/势力/人口），不改现有状态系统 | 中（未来底座） |
| #40 | 序章无声/秒过与穿越身份修复 | fix/prologue-audio-timeline-20260820 → test/art-asset-integration | Draft | 否 | 序章推进逻辑修复、第五幕穿越、TTS、音频互抢修复 | **高**（修复线） |
| #43 | Audio Cleanroom V1 | integration/audio-cleanroom-v1 → fix/prologue-audio-timeline-20260820 | Draft | 否 | 旧 BGM 清仓、视频强制静音、palaceId BGM 路由、CI 防回流 | **高**（修复线） |
| #46 | world AI knowledge-boundary regression fix | fix/world-ai-regression-tests-v1 → integration/living-world-canon-recovery-v1 | Draft | 否 | 世界 AI 确定性候选生成 + 人物可见性规则 | **高**（修复内容） |
| #42 | [CI ONLY] validate prologue/audio repair | fix/prologue-audio-timeline-20260820 → main | Draft | 否 | 仅触发 CI 验证，禁止合并 | 低（验证用） |
| #44 | [CI ONLY] living-world + 1127 Canon validation | integration/living-world-canon-recovery-v1 → main | Draft | 否 | 仅 CI 验证 | 低（验证用） |
| #45 | [CI] validate Canon recovery against Claude base | integration/living-world-canon-recovery-v1 → ci/living-world-canon-base | Draft | 否 | 仅 CI 验证 | 低（验证用） |
| #47 | [CI ONLY] validate world-AI regression fixes | fix/world-ai-regression-tests-v1 → ci/living-world-canon-base | Draft | 否 | 仅 CI 验证 | 低（验证用） |

## 三、归档分类

- **当前正式集成**：#50、#53、#54、#55、#56、#58、#59、#63、#64、#40、#43、#46 —— V1.6.x 主线、音频修复线、living-world 修复的实际工作内容。
- **已完成待处理**：#60（分支侧自报 DONE，待验收合流）、#59（最终集成已组装，待验收）。
- **独立原型**：#38（World Roster 底座）、#65（双层地图预研）、#66（寰宇真机试览）。
- **文档 / 审计**：#57、#61、#62。
- **已被后续实现替代（疑似冗余，待确认）**：#42（head 分支与 #40 相同，验证目的可由 #40/#43 承接）、#45（与 #47 验证同一 head，base 不同）。
- **建议未来关闭**（本轮不执行）：#42、#44、#45、#47 —— 均为 CI-only 验证 PR，自述"禁止合并"，验证消费后即可关闭。

## 四、重点 PR 核对

### PR #59 — V162 final integration（验收枢纽）

- head `integration/v1.6.2-preacceptance`（28f05f67），base `fix/stab-005-video-player-compat`；28 commits、116 files、+5610/-231；mergeable_state: clean。
- 已选择性合流 STAB-001~006、COURT-001（#54）、ROSTER-001 文档（#57）、ART-HOTFIX、WORLD-UX-001、DELEGATION-001 的 12 个后端/测试文件（不整包合 #64）。
- 明确声明：不合 main、不升版本、不集成 #65。
- **判断**：V1.6.2 的验收枢纽，STAB 链各 PR 的实际内容已在此汇聚；后续验收应以本 PR 的 APK 为准。

### PR #64 — DELEGATION-001

- head `feat/delegation-001-ai-government`，base `integration/v1.6.2-preacceptance`；12 files、+1089/-13。
- Imperial Mandate + DelegatedActionValidator，符合"模型只提候选、本地规则裁决"的总纲原则；13 个新测试，本地 144 个测试跑通，交 CI 终验。
- **判断**：其 12 个后端/测试文件已被 #59 选择性接入；本 PR 保留用于追踪完整授权制设计，不建议单独合并。

### PR #65 — MAP-PROTOTYPE-001

- head `prototype/map-globe-v1`，base `main`；4 files、+476/-0；mergeable_state: clean。
- 独立预研：伪球面投影 + Compose Demo + 两份设计文档；明确不碰正式地图/朝议/integration 线。
- **判断**：保留价值在于 V1.7 寰宇过场的路线决策；其文件已被 #66 择取做真机试览。维持 Draft 待评审即可。

## 五、治理一致性检查

核对对象：`AGENTS.md`、`START_HERE.md`、`docs/PROJECT_MASTER_PLAN.md`（均在 `release/v1.6.1`）。

| 检查项 | 结果 |
| --- | --- |
| 任务状态不一致 | **发现并已修复**：总纲原记 STAB-001 为 NEXT、其余 STAB 为 TODO，但 #53/#55/#56/#58/#59/#60 等 Draft PR 已在施工。已同步为 IN_PROGRESS 并补记 PR 编号。 |
| 已完成任务仍标 TODO | 未发现（所有 PR 均未合并，无任务达到 DONE，完成数维持 0/8）。 |
| 旧版本号 | 未发现：总纲标注 V1.6.1 RC / versionCode 28，与 RELEASE_POLICY 及 PR #50 一致。 |
| 已失效"下一步" | **发现并已修复**：原"当前下一任务 STAB-001"已变为多 PR 并行验收，顶部字段已更新。 |
| 指向不存在的文件 | 未发现：AGENTS.md 引用的 PROJECT_MASTER_PLAN / RELEASE_POLICY / docs/audio 均存在。 |
| 重复说明 | 轻微：START_HERE.md 与 AGENTS.md 阅读顺序表述重复，属有意冗余，不处理。 |
| 相互冲突的说明 | 未发现。 |
| **治理文件缺失于 main** | **重要发现**：AGENTS.md / START_HERE.md / PROJECT_MASTER_PLAN.md / RELEASE_POLICY.md 只存在于 release/v1.6.1 等分支，**main 分支上没有**。任何基于 main 接手的 AI 都读不到治理规则。建议 #50 合流时一并解决。 |

## 六、仓库卫生清单（只记录，不删除）

| 类别 | 项目 | 说明 |
| --- | --- | --- |
| 临时构建日志 | 根目录 `build-error-latest.txt`（约 71KB） | 构建日志不应入库；建议移入 .gitignore 并从仓库移除（需另行授权） |
| 临时分支 | `tmp/ignore`、`tmp/noop`、`tmp/noop2` | 明显临时用途，建议未来清理 |
| 分支总数 | **82 个分支** | 大量 stage2-* / audio-* / art-* 历史分支，建议已合并/已废弃的定期清理 |
| 日志式文档 | `docs/build_fix_log.md` | 属历史档案，可保留但建议标注"归档" |
| 状态标记文件 | `docs/OPEN_SOURCE_PASS_STATUS.md` | marker commit 性质，可在验收完成后归档 |
| 仓库体积 | 约 196MB | 77MB V3 视频资产直接进入 git 历史，每次素材替换都会永久膨胀；强烈建议评估 Git LFS 或对象存储 |
| 孤儿资源 | 约 39% 图片疑似孤儿（见 PR #61 ART-001） | 待集成负责人决策接线或清理 |
| BGM 二进制缺口 | 8 个正式 BGM 槽位无法从 git 恢复（见 PR #62） | 大文件上传通道问题，#63 正在处理 |

## 七、建议以后处理的事项（≤5 项）

1. **治理文件合入 main**：让 AGENTS.md / START_HERE.md / PROJECT_MASTER_PLAN.md / RELEASE_POLICY.md 随 #50 进入 main，避免基于 main 的接手者读不到规则。
2. **验收并合流 STAB 链**：以 #59 为枢纽完成 STAB-001~007 真机验收，按 RELEASE_POLICY 升版本后合入 release 线；随后关闭 4 个 CI-only PR（#42/#44/#45/#47）。
3. **仓库瘦身**：将 `build-error-latest.txt` 移出仓库并加入 .gitignore；评估视频资产迁移 Git LFS，控制仓库体积增长。
4. **分支清理**：制定分支保留规则，清理 tmp/* 与已完结的 stage*/audio*/art* 历史分支（82 → 目标 30 以内）。
5. **BGM 二进制通道**：按 #62/#63 结论解决大体积音频上传问题，让 8 个 BGM 槽位真正可恢复、可追溯。
