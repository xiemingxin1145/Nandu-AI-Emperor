# COURT_NPC_IMAGE_QA.md

Phase C 出图质检 — 2026-08-20  
Branch: `art/song-court-npc-library-v1`

---

## 汇总

| 项 | 数量 |
|---|---|
| 本轮实际生成（JPG 源） | 约 20+ 次生成调用 |
| **合格入库 WebP** | **16** |
| 头像 portrait | **8** |
| 半身 halfbody | **8** |
| C 类列班 | **0** |
| 群像 crowd | **0** |
| 重生次数 | ≥2（陈允中去补子；部分半身） |
| 仍待修复 / 未做 | B16 + C8 + 群像6 + 其余 A 半身/头像缺口 |
| 推入 git 二进制 | 受限；主交付为 ZIP |

ZIP：会话产物 `Nandu_Court_NPC_Art_V1.zip`（约 2.5MB，含 16 webp）

---

## READY（头像+半身均有）

| id | 姓名 | portrait | halfbody | 备注 |
|---|---|---|---|---|
| npc_court_zhongshu_01 | 周彦章 | READY | READY | 锚点合格 |
| npc_censor_01 | 韩德裕 | READY | READY | 瘦冷合格 |
| npc_shumi_01 | 郭士衡 | READY | READY | 疲态合格；半身持军报 |
| npc_court_qiju_01 | 陆景修 | READY | READY | 年轻书卷；略偏插画 |
| npc_court_geishi_01 | 沈伯谦 | READY | READY | 花白须古板 |
| npc_court_zhongshu_02 | 陈允中 | READY | READY | **曾出明代补子，已重生无补子** |
| npc_hubu_01 | 王宗旦 | READY | LIMITED | 半身有「流水帐」汉字 + 非全透明底 |
| npc_neishi_01 | 张继恩 | READY | LIMITED | 半身灰底/薄纱感，建议重生透明 |

**PROMOTABLE 8 人：头像 8/8；半身 8/8（其中 2 张 LIMITED 建议重生）**

---

## 失败 / 不合格记录

| 问题 | 处理 |
|---|---|
| 陈允中初版明代补子（仙鹤方补） | 重生，去掉 buzi |
| 王宗旦半身可读汉字标签 | 标 LIMITED，待无字版 |
| 张继恩半身非干净透明 | 标 LIMITED |
| 部分生成偏写实摄影 vs 部分水墨 | 可接受范围内，后续统一 |

---

## 未完成

- 其余 A 档 4 人（兵部、知州、太常、马师古等）头像/半身  
- B 类 16 头像  
- C 类 8  
- 群像 6  

---

## 文件路径约定（已生成）

```
npc_court_v1/portrait_npc_court_zhongshu_01.webp
npc_court_v1/halfbody_npc_court_zhongshu_01.webp
…（见 ZIP 内清单）
```

目标仓库路径：`app/src/main/assets/images/characters/npc_court_v1/`
