# 南渡无悔 Agent 融合方案

> 目标：把 DroidRun / DroidMind / AutoGLM 这类“手机自动化 + AI Agent”思路，安全地融合进《南渡无悔 · AI皇帝引擎》。第一阶段不让外部 Agent 直接操作手机，也不让 AI 直接改游戏数值，而是先做游戏内的“AI军师/晚晚分身”执行层。

## 核心原则

1. **不整包搬源码**：只吸收架构思想，不把第三方项目直接塞进主工程。
2. **AI 不直接改数值**：任何 AI 输出必须转成白名单命令，再交给 `GameRuleEngine` 裁决。
3. **先游戏内 Agent，后手机控制 Agent**：先让 Agent 会分析朝局、推荐圣旨、生成行动建议；稳定后再接 DroidRun / DroidMind 这类外部手机执行层。
4. **可插拔**：Agent 层只依赖 `GameState`、`AiProvider`、`EdictResult`、`GameRuleEngine`，不绑死某个模型。

## 三层结构

```text
Observation 观察层
  ↓ 读取 GameState / 当前宫殿 / 当前风险 / 历史日志
Planner 计划层
  ↓ 生成 AgentTask：目标、理由、建议圣旨、风险标签
Executor 执行层
  ↓ 转成 EdictResult / 白名单命令
GameRuleEngine 本地裁决
```

## 第一阶段：游戏内 AI 军师

新增模块建议：

```text
app/src/main/java/com/xiemingxin/nandu/agent/
  AgentTask.kt
  AgentSuggestion.kt
  AgentRisk.kt
  AgentPlanner.kt
  RuleBasedAgentPlanner.kt
  AgentExecutor.kt
```

能力：

- 自动读取当前国家状态。
- 判断粮草、军心、边防、官员风险。
- 生成 1~3 条建议圣旨。
- 给出群臣可能反应。
- 玩家确认后，再交给原有圣旨解析 / `GameRuleEngine` 执行。

## 第二阶段：半自动测试 Agent

让 Agent 帮你测试游戏：

```text
打开 App
进入朝堂
输入圣旨
检查返回 JSON
检查 GameState 是否变化
生成测试报告
```

这一步可以参考 DroidRun 的“视觉观察 + 动作执行”思路，但仍然只在测试环境用。

## 第三阶段：手机外部控制

接入方向：

- DroidRun：适合自动点屏、自动测试 APK。
- DroidMind：适合让 Claude / Cursor / Claude Code 通过 MCP 控制安卓设备。
- n8n：适合 GitHub Actions、日报、自动构建、消息提醒。
- Mem0：适合长期记忆，例如玩家偏好、历史朝局、常用策略。

## 和现有架构的连接点

当前项目已有：

```text
玩家圣旨
  ↓
AiProvider
  ↓
EdictResult JSON
  ↓
Kotlin 校验 + 白名单过滤
  ↓
GameRuleEngine
  ↓
GameState
```

Agent 层只插在“玩家圣旨”之前：

```text
AgentSuggestion
  ↓ 玩家确认/修改
玩家圣旨
  ↓ 原有流程
```

这样最安全，也最容易维护。

## 待办清单

- [ ] 新增 `agent` 包基础数据类。
- [ ] 新增 `RuleBasedAgentPlanner`，先不用联网模型，靠本地规则给建议。
- [ ] 在御书房 / 朝堂页面加入“晚晚军师建议”入口。
- [ ] 玩家可一键采用建议圣旨。
- [ ] Agent 建议写入起居注。
- [ ] 增加 GitHub Actions 构建校验。
- [ ] 第二阶段再接自动 UI 测试。

## 安全边界

- Agent 不能直接写 `GameState`。
- Agent 不能绕过 `GameRuleEngine`。
- Agent 不能执行非白名单命令。
- 外部手机控制只用于自动测试，不直接接生产玩家数据。
