# 2026-08 美术与 CG Batch 1 接入说明

## 接入结果

本批接入用户提供的豆包生成素材：3 名核心人物、1 张临安朝堂背景、4 张战争/诏令 CG、1 段 14.16 秒战前视频。

- 岳飞、韩世忠、秦桧的新定装图已接入 `ArtResourceRegistry`，替代运行时对应人物图路径。
- 垂拱殿使用新临安大殿背景。
- `jin_army_crosses_huai`、`yangzhou_panic_flee`、`han_shizhong_requests_battle` 已精确映射新 CG。
- 其他包含出征、压境、金军、渡淮、下诏、顺昌等关键词的动态事件可命中新 CG 回退。
- 符合军务条件的剧情卡显示“播放战前过场 · 14秒”，点击后使用系统 `VideoView` 播放本地 H.264/AAC 视频。

## 目录

```text
app/src/main/assets/images/characters/batch1/
app/src/main/assets/images/events/batch1/
app/src/main/assets/images/palace/batch1/
app/src/main/assets/video/VID-CZ-001-PREWAR-V01.mp4
```

## 格式修正

原交付的 8 张图片扩展名为 `.png`，实际文件内容均为 JPEG，且人物图不含透明通道。接入版已重新解码并转换为 Android 可直接读取的 WebP：

- 人物半身母版：1024×1536 WebP。
- 人物头像：从母版上方裁切并输出 512×512 WebP。
- 场景与事件 CG：1280×720 WebP。

原视频参数为 HEVC/H.265、1280×720、24fps、AAC 44.1kHz。为提高 Android 设备兼容性，接入版转码为：

- H.264 Main Profile Level 3.1。
- 960×540、24fps、`yuv420p`；720P 原件保留在用户交付包中，APK 使用移动端优化版。
- AAC 双声道、48kHz。
- `faststart` 元数据前置。

## 原型限制

- 三张人物图仍带纯色背景，尚未抠成透明立绘；当前适合人物卡和原型演示。
- 视频右上角带“豆包AI生成”水印。代码将其标记为 `prototypeOnly`，正式发行前必须替换无水印版本。
- CG 中的历史服饰与建筑尚未经过专业宋史美术考据，只能视作统一画风原型。
- 原始 ZIP 和原始 HEVC 视频不进入 APK，避免重复占用安装包体积；仓库只保存规范化运行时资产。

## 替换正式素材

保持下列运行时路径不变，即可无代码替换：

```text
images/characters/batch1/portrait_yue_fei_batch1.webp
images/characters/batch1/halfbody_yue_fei_batch1.webp
images/characters/batch1/portrait_han_shizhong_batch1.webp
images/characters/batch1/halfbody_han_shizhong_batch1.webp
images/characters/batch1/portrait_qin_hui_batch1.webp
images/characters/batch1/halfbody_qin_hui_batch1.webp
images/palace/batch1/chuigongdian_batch1.webp
images/events/batch1/event_shunchang_prewar_batch1.webp
images/events/batch1/event_imperial_edict_batch1.webp
images/events/batch1/event_army_departure_batch1.webp
images/events/batch1/event_jin_approach_batch1.webp
video/VID-CZ-001-PREWAR-V01.mp4
```

替换视频无水印正式版后，将 `CgResourceRegistry.prewar.prototypeOnly` 改为 `false`。
