# batch4_living_world

本目录是《南渡无悔》第 4 批「活场景」视频资产位。

## 当前状态（2026-08-20）

| 项 | 状态 |
|---|---|
| 分支 | `art/video-batch4-living-world` |
| 基线 | `fix/prologue-audio-timeline-20260820` @ `7a6b471` |
| 已有 V3 总清单 | `../v3_inventory_manifest.json`（51 条，不重复生成） |
| 本批计划新片 | 14 条（见 `manifest.json`） |
| 已提交 MP4 | **0** |
| 玩法代码改动 | **无**（按任务单禁止） |

## 为什么还没有 MP4

当前执行会话**没有**可调用的电影级视频生成管线（Seedance / Kling / Runway 等）。  
任务单明确：**禁止用假占位视频冒充成品**。

因此本提交只包含：

1. 现有 51 条 V3 扫描清单  
2. batch4 生产队列 manifest  
3. 逐条英文生成提示词 + ffmpeg 去音轨规范  

拿到真实 MP4 后，按 `manifest.json` 文件名放入本目录，用：

```bash
ffmpeg -i input.mp4 -c:v libx264 -pix_fmt yuv420p -an -movflags +faststart B4_xx_name.mp4
```

再更新 manifest 里对应条目的 `status`、真实时长与分辨率。

## 硬规则（不可破）

- 最终文件尽量 **video-only**（无音轨）  
- 优先 H.264  
- 南宋审美，禁止清宫/辫子/仙侠/现代建筑  
- 不在此分支改 Kotlin / 音频注册表 / 序章接线  
