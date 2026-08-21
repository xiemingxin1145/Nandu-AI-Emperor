# STAB-002 Dynamic Battle Roster QA

本文件记录 STAB-002 的独立验证范围；最终完成状态仍以 `docs/PROJECT_MASTER_PLAN.md` 为准。

## 本分支必须保证

- 战役日期来自 `GameState.calendar`，不得写死历史年号。
- 战役人物来自当前战区的真实人物/军团/驻防任命。
- 官阶来自当前人物状态、rankLevel / merit；职责来自实际 Army / AppointmentSystem。
- 已故、俘虏、尚未入局、在野、在途、异地人物不会因为历史名气被强行放入战役画面。
- 战区兵力、粮草、士气、敌情从 `GameState` 派生。
- STAB-003 完成前，不允许用 Compose 局部变量伪装战略决策已经写回世界。

## 自动测试

`BattleScenePresentationSystemTest` 覆盖：

1. 1127 开局不伪造岳飞/刘锜/韩世忠参战；
2. 当前战区真实主帅正确成为主将；
3. 已故/异地历史人物不会上镜；
4. 同一人物不同 rankLevel 显示不同当前官阶；
5. 页面日期跟随当前 GameCalendar；
6. 支援将领也必须真实位于战区。

## 集成依赖

本任务故意不修改 `MainActivity.kt` / `PalaceHallScreen.kt`，等待 STAB-001 的动态战役门控完成后再接入正式入口，以减少并行 AI 开发冲突。
