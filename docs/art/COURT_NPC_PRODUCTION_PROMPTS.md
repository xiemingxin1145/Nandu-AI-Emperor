# COURT_NPC_PRODUCTION_PROMPTS.md

36 名普通南宋官员 + 6 群像生成提示词  
风格总锁：**Southern Song dynasty official, realistic historical painting, muted ink and mineral colors, no epic glow, no modern face, no Qing costume, no Ming flying-fish robe, no braid, no fantasy armor**

输出规格：
- 头像：512×512，正面或 3/4，透明或净色底
- 半身：1024×1536，膝上，透明背景
- 群像：1280×720 或 1920×1080，横图

全局 Negative：
`modern clothing, Qing dynasty, queue braid, Ming bufu, flying fish robe, anime, beauty filter, dramatic rim light, storm background, glowing eyes, logo, text, watermark`

---

## A 档（12）— 头像 + 半身

### npc_court_zhongshu_01 周彦章 · 中书舍人 · 42
Portrait/Halfbody: Middle-aged Song male official, square jaw, short neat beard, calm tired eyes, black futou with long horizontal wings (zhanjiao putou), scarlet or dark red public robe (gongfu), wide black belt, holding ivory hu tablet, Southern Song court style, ordinary capable bureaucrat, three-quarter view half body transparent background

### npc_court_zhongshu_02 陈允中 · 中书舍人 · 34
Younger thin face, sparse mustache, alert eyes, same zhanjiao putou, slightly brighter robe, nervous upright posture, greenish-black belt, hu tablet, no hero aura

### npc_court_geishi_01 沈伯谦 · 给事中 · 51
Longer face, salt-pepper beard, stern thin lips, dark purple-brown official robe, zhanjiao putou, holds hu, look of stubborn pedant

### npc_court_qiju_01 陆景修 · 起居舍人 · 29
Youthful clean-shaven, pale, scholarly, slightly oversized futou, careful posture, ink-stained fingertip optional, soft lighting

### npc_censor_01 韩德裕 · 监察御史 · 46
Sharp cheekbones, cold small eyes, short pointed beard, dark robe, severe expression, thin frame

### npc_censor_02 马师古 · 殿中侍御史 · 38
Rounder face, thick eyebrows, aggressive jaw, dark official robe, zhanjiao putou, confrontational stare but still ordinary official

### npc_hubu_01 王宗旦 · 户部郎官 · 44
Plump cheeks, calculating eyes, well-groomed goatee, slightly richer fabric texture, ledger under arm optional for halfbody

### npc_shumi_01 郭士衡 · 枢密院承旨 · 48
Rectangular face, calm heavy-lidded eyes, neat beard, darker martial-civil hybrid robe (still Song civil cut), composed

### npc_bingbu_01 高怀远 · 兵部郎官 · 41
Weathered skin, scar on brow optional small, short beard, practical robe, less silk sheen

### npc_neishi_01 张继恩 · 内侍押班 · 53
Beardless eunuch official, soft cheeks, shrewd eyes, distinctive Song eunuch official cap and patterned robe, not comedic

### npc_zhizhou_01 赵令仪 · 知州模板 · 47
Broad face, reliable uncle look, modest futou, travel-dust suggestion on boots for halfbody, provincial senior official

### npc_taichang_01 孔文仲 · 太常寺官 · 56
Gaunt elderly, white-streaked beard, ritual solemnity, darker ceremonial-leaning robe, thin hands

---

## B 档（16）— 至少头像；半身可选

npc_court_menxia_01 丁元亮 39 门下省属官 — oval face, mild, short beard  
npc_court_shangshu_01 曹公谨 45 尚书省属官 — heavy jowls, weary  
npc_court_mishu_01 苏季良 32 秘书省 — pale bookish, almost no beard  
npc_censor_03 李师颜 50 御史台属官 — gray temples, dry smile  
npc_jian_01 冯正臣 43 谏官 — upright thin, anxious brow  
npc_duzhi_01 郑永年 40 度支 — narrow eyes, thin mustache  
npc_zhuanyun_01 吴克己 46 转运司 — sun-darkened, practical  
npc_cangchang_01 金安国 37 仓场 — stout, oily complexion  
npc_shumi_02 何武成 35 枢密属官 — young ambitious, clean jaw  
npc_tongzhi_01 潘师雄 52 统制类武臣 — darker skin, military futou/helmet hybrid Song style soft, no plate anime armor  
npc_jinjun_01 许彦直 28 禁军军官 — young round face, stiff posture  
npc_zhizhou_02 宋伯达 43 知州 — mediocre honest look  
npc_tongpan_01 刘德方 36 通判 — clever, slight smile  
npc_xianling_01 崔野 31 县令 — provincial plain robe tier  
npc_neishi_02 王福宁 41 传宣内侍 — beardless, polite mask  
npc_honglu_01 袁宗礼 49 鸿胪礼仪 — formal, long sleeves emphasis  

（生成时逐条展开为完整英文 prompt，锁定年龄体型胡须与展脚幞头。）

---

## C 档（8）— 侧身/半侧/远景可用

npc_crowd_wen_01 … npc_crowd_wen_04 文臣列班脸  
npc_crowd_wu_01 … npc_crowd_wu_02 武臣列班脸  
npc_crowd_nei_01 内侍侧影  
npc_crowd_low_01 低阶属官远景  

要求：彼此五官不同，分辨率可低于 A，可 3/4 侧或略虚。

---

## 群像 6 张（court_crowd_v1）

1. **文臣左班** — line of Song civil officials in zhanjiao putou and dark red/purple gongfu, hall interior, no readable faces required for back rows  
2. **武臣右班** — Song military officers, restrained armor or military robes, not fantasy  
3. **中后排百官** — denser crowd, slight motion  
4. **低阶远景** — smaller figures, muted  
5. **内侍列队** — eunuch officials along corridor  
6. **候朝/散班** — officials waiting outside hall, morning light  

---

## 服饰安全说明

- 文官：展脚幞头 + 圆领公服 + 腰带 + 笏（朝会感）  
- 不写死具体品色对照表（史有争议处用暗赤/暗紫/深青等安全色）  
- 武臣：避免明甲仙侠；用宋式武缘袍、轻甲元素  
- 内侍：无胡须、特有巾帽与纹样，克制不丑化  
