# 《南渡》开源架构参考清单

本文件只记录“可验证授权 + 值得吸收的设计”。公开仓库不等于可以随便复制，因此本项目默认只直接引入 CC0 资产；MIT 等代码仓库以架构思想和原创重实现为主，不整库搬运。

## 1. AI Town — a16z-infra/ai-town

- 仓库：https://github.com/a16z-infra/ai-town
- 授权：MIT
- 用途：AI 原生世界 / Agent 游戏循环架构参考。

《南渡》吸收的核心思想：

1. **权威世界状态与 Agent 主观状态分离**：城池、兵力、钱粮、位置、官职是代码世界真相；NPC 记忆、判断、计划属于 Agent 层。
2. **所有 Agent 行动必须回到游戏引擎校验**：大模型只能提出意图，不能直接篡改兵力、城池和资源。
3. **Tick 驱动世界**：玩家不操作时世界也继续推进；AI 角色在统一回合/旬循环里观察、计划、执行。
4. **长耗时 LLM 任务与主循环解耦**：模型调用失败、超时或返回坏结构时，世界不能卡死，应由本地决策器接管。
5. **记忆不等于事实**：角色可以误判、遗忘、被假情报误导，但真实世界状态必须唯一。

这与《南渡》现有 Stage 6/7 的“代码负责现实，AI 负责灵魂”方向一致，后续 Character Agent、情报迷雾、长期记忆都按这一分层继续。

## 2. GOAP / Utility AI

### kelindar/goap
- 仓库：https://github.com/kelindar/goap
- 授权：MIT
- 参考点：Goal -> Candidate Action -> Cost/Score -> Plan 的规划结构。

### JarkkoPar/Utility_AI
- 仓库：https://github.com/JarkkoPar/Utility_AI
- 授权：MIT
- 参考点：多个行为按当前环境指标动态打分，而不是写死 if/else 或随机抽行为。

### Sterberino/open-behavior-trees
- 仓库：https://github.com/Sterberino/open-behavior-trees
- 授权：MIT
- 参考点：行为树用于“执行阶段”，Utility/GOAP 用于“选什么目标”，两层不要混为一谈。

《南渡》实现原则：

- 势力层：Threat / Supply / Opportunity / Campaign Continuity -> 少量候选战略 -> AI/本地评分选择。
- 人物层：忠诚、野心、派系、关系、职责、风险、历史记忆 -> Utility Score -> 主动上奏/请战/结交/阻挠等候选行为。
- 执行层：所有行为变成结构化 Action，交给现有规则系统验证。
- 小模型只做“候选中的选择 + 文本表达”，不给它承担路径规划、数值运算和规则真相。

## 3. Kotlin 状态机参考

### KStateMachine/kstatemachine
- 仓库：https://github.com/KStateMachine/kstatemachine
- 参考点：明确状态/事件/迁移，适合军团状态、人物计划状态、外交任务状态。

当前不直接增加第三方运行时依赖，先保持 APK 简洁；需要时再单独评估许可证和依赖体积。

## 4. 游戏美术/资产库方向

### Tiddybub/2d-assets
- 仓库：https://github.com/Tiddybub/2d-assets
- 说明：聚合大量 CC0 2D 游戏资产，适合后续筛 UI 图标、地图标记、策略游戏通用组件。
- 原则：每个具体素材仍需检查其 SOURCE/许可证记录后再进入正式包。

## 5. 音频来源

详见 `docs/THIRD_PARTY_AUDIO.md`。

当前音频修复采用“白名单替换”而不是继续修补旧占位音频：

- 旧 BGM 全部清空后换成已验证的纯音乐。
- 高频 UI 声音全部换成已验证的短音效。
- Voice 目录暂时清空，直到人物语音系统显式调用，杜绝生成提示词误当背景音循环。

## 不采用的方式

- 不复制授权不明的商业游戏 APK 代码或素材。
- 不因为仓库“公开”就默认可商用。
- 不把 GPL/AGPL 代码直接塞进主工程，避免许可证污染。
- 不让 LLM 自己声称某个行动已经执行；世界状态只能由规则引擎改变。
