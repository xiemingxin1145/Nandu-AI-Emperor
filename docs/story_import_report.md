# 《南渡无悔》剧情导入报告

**版本：** V0.6.x  
**模块：** 建炎初期剧情事件库（第一批）  

---

## 导入概况

| 项目 | 内容 |
|------|------|
| 剧情文件 | `app/src/main/assets/story/story_events_jianyan_01.json` |
| 事件总数 | 20个 |
| 文件大小 | 约23KB |
| 读取代码 | 已由GPT接入（StoryEventLoader） |
| 字段一致性 | ✅ 已校验，与GameRuleEngine数值字段一致 |

---

## 事件类型分布

| 类型 | 数量 | 说明 |
|------|------|------|
| main_story 主线 | 4 | 建炎初议、宗泽北望、李纲去位、秦桧归来 |
| side_story 支线 | 1 | 张浚北伐议 |
| talent_discovery 人才寻访 | 4 | 岳飞、韩世忠、吴玠、刘锜 |
| jin_event 金国事件 | 2 | 金兵渡淮、兀术南征 |
| city_crisis 城池危机 | 2 | 扬州惊变、建康围城 |
| random_court 随机朝堂 | 2 | 主和疏议、国库告罄 |
| random_military 随机军情 | 3 | 江南告急、义军蜂起、军中疫病 |
| folk_rumor 民间传闻 | 2 | 天子南逃之议、背刺精忠 |

---

## 数值字段清单（与GameRuleEngine对接）

### 国家级数值
- `gold` 国库金银
- `grain` 粮草
- `troopMorale` 军心
- `courtStability` 朝堂稳定
- `jinThreat` 金国威胁
- `warFactionPower` 主战派势力
- `peaceFactionPower` 主和派势力
- `popularSupport` 民心

### 城池防御
- `cityDefense_jiankang` / `cityDefense_yangzhou` / `cityDefense_chuzhou`
- `cityDefense_linan` / `cityDefense_sichuan` / `cityDefense_xianren_pass`

### 人物成长（loyalty/rank/troops）
- 岳飞 `yue_fei_loyalty` `yue_fei_rank` `yue_fei_troops`
- 韩世忠 `han_shizhong_loyalty` `han_shizhong_rank` `han_shizhong_troops`
- 吴玠 `wu_jie_loyalty` `wu_jie_rank` `wu_jie_troops`
- 刘锜 `liu_qi_loyalty` `liu_qi_rank`
- 李纲 `li_gang_loyalty`、宗泽 `zong_ze_loyalty`
- 秦桧 `qin_hui_loyalty`、张浚 `zhang_jun_loyalty`

### 特殊数值
- `rebel_faction_power` 义军势力
- `epidemic_severity` 疫病严重度

---

## 历史边界遵守情况

✅ 岳飞开局为军中小卒，需寻访+拔擢逐步成长  
✅ 韩世忠较早出场，但需任命与军务成长  
✅ 秦桧前期为主和倾向文士，第三章"秦桧归来"埋下奸相伏笔，未脸谱化  
✅ 赵构开局处境不稳，无敌明君设定已规避  
✅ 金国（完颜宗弼/宗翰）保留压迫感，非弱智敌人  
✅ 主战派/主和派各有逻辑，秦桧议和论附带"金人是否真会停手"的反问  

---

## 待补充（后续批次）

1. 第四章之后剧情（绍兴和议、岳飞冤案分支）
2. 更多人才寻访事件（梁红玉、虞允文等）
3. 西夏、大理外交事件
4. 季节天气联动的随机事件

---

*本报告由晚晚整理，剧情JSON与GPT版本已校验同源*
