# 南渡无悔 七大势力美术资源压缩导入包

## 处理结果

- 原始压缩包：10.95 MB
- 原始文件数：23 个 PNG
- 导出格式：WebP
- 导出文件数：23 个 WebP + 1 个 manifest
- 用途：交给 Claude 上传到 GitHub，同路径覆盖/新增即可。

## 上传方式

解压本包后，把 `app/src/main/assets/images/` 上传到仓库同路径。

## 目录内容

- `characters/portrait_*.webp`：核心人物头像
- `characters/halfbody_*.webp`：核心人物半身像
- `ui/faction_frames/`：势力边框
- `ui/faction_flags/`：国徽/战旗
- `ui/unit_icons/`：兵种图标
- `ui/function_icons/`：势力功能图标
- `manifest_seven_factions_batch1.json`：资源映射清单

## 注意

- 文件名已全部改为英文小写下划线。
- 已保留 Android assets 目录结构。
- 不要上传原始 PNG，直接上传本包里的 WebP。
- Claude 只上传资源，不要修改 Kotlin/Gradle。
