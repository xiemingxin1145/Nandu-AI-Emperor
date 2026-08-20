# 2026-08 美术扩展 Batch 2 接入说明

## 新增资产

本批在 Batch 1 基础上追加 6 名角色、1 张顺昌城头背景和 1 张郾城大捷 CG：

- 赵构：朝服定装。
- 秦桧 V2：使用规范展脚幞头，替换运行时 V1 软巾版。
- 赵鼎：朝服定装。
- 刘锜：顺昌守将定装。
- 吴玠：川陕边将定装。
- 完颜宗弼（金兀术）：金军主帅定装。
- 顺昌城头：无人物背景，可承载守城部署和战前对白。
- 郾城大捷：胜利事件 CG。

## 运行时接线

- 六名人物的 `portrait` 与 `halfbody` 已统一登记到 `ArtResourceRegistry`。
- 序章赵构立绘不再使用硬编码路径，改走统一注册表。
- `CharacterAppearanceSystem` 的已登场人物不再拼接旧文件名，改走统一注册表；未登场人物统一使用剪影回退。
- 顺昌城头可通过 `ArtResourceRegistry.locationBackground("shunchang_wall")` 调用。
- 标题或描述中含“郾城”“大捷”“凯旋”的事件自动使用郾城大捷 CG。

## 目录

```text
app/src/main/assets/images/characters/batch2/
app/src/main/assets/images/locations/batch2/bg_shunchang_wall_batch2.webp
app/src/main/assets/images/events/batch2/event_yancheng_victory_batch2.webp
```

## 格式与限制

- 原文件扩展名为 `.png`，实际内容仍为 JPEG；接入时已重新解码为 WebP。
- 人物图不含透明通道，仍属于人物卡/原型演示资产。
- 人物头像为 512×512，半身/全身母版为 1024×1536，场景与 CG 为 1280×720。
- 军事人物之间仍存在相近的盔甲模板和站姿，正式美术阶段应进一步强化刘锜、吴玠、岳飞、完颜宗弼的轮廓差异。
