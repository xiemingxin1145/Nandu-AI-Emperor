# 《南渡无悔》构建修复日志

## 2026-06-14 · V0.7.0 编译失败修复

### 故障现象
从构建 #185 到 #216，连续 30+ 次 GitHub Actions 构建失败，
包括纯图片上传、纯文档提交在内的所有 commit 全部 build failure。

### 根本原因
EventDirector.kt 第 8 行存在无效 import：
```kotlin
import kotlinx.serialization.json.content
```
`kotlinx.serialization.json` 包内没有名为 `content` 的可导入顶层符号。
（`.content` 是 JsonPrimitive 的属性，通过 `jsonPrimitive.content` 访问，无需 import。）

该行由 commit `5225743`（"fix: import json content for event director"）引入，
导致 Kotlin 编译器报 unresolved reference，整个项目无法编译。
此后每次构建都要编译这个坏文件，因此全部连环失败。

### 修复
- commit `4ca566f`：删除 EventDirector.kt 第 8 行无效 import
- 未改动任何逻辑，仅删一行
- 验证：构建 #217 SUCCESS，V0.7.0 APK 正常产出（17MB）

### 连带检查
检查了其他剧情系统文件，import 均干净，无连带问题：
- StoryEventEffectApplier.kt ✓
- StoryEventLoader.kt ✓
- StoryEventCard.kt ✓

### 经验
AI 自动生成代码时，对 kotlinx.serialization 这类库的 API 容易"想当然"添加
不存在的 import。后续若再遇连环构建失败，优先检查最近 commit 新增的 import 行。
