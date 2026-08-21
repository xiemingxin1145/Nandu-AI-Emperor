package com.xiemingxin.nandu.game

object InitialData {

    val factions = listOf(
        Faction(
            "song", "大宋", "宋", "赵构", "yingtianfu", "行在草创，主战主和并立",
            isPlayable = true, isAI = false,
            colorArgb = 0xFF2E86C1L, gold = 50000, grain = 200000, prestige = 30,
            relations = mapOf("jin" to -80, "rebel" to 10, "dali" to 25)
        ),
        Faction(
            "jin", "金国", "金", "完颜宗望", "kaifeng", "兵锋正盛，窥伺中原",
            colorArgb = 0xFFB22222L, gold = 100000, grain = 300000, prestige = 60,
            relations = mapOf("song" to -80, "rebel" to -30, "dali" to 0)
        ),
        Faction(
            "rebel", "地方义军", "义", "群豪", "xinyang", "散据中原，观望朝廷，易受招抚或离散",
            colorArgb = 0xFF8A6D3BL, gold = 0, grain = 0, prestige = 5,
            relations = mapOf("song" to 10, "jin" to -30)
        ),
        Faction(
            "dali", "大理", "理", "段氏", "dali", "西南观望，暂不直接参战",
            colorArgb = 0xFF3F7A4DL, gold = 20000, grain = 40000, prestige = 20,
            relations = mapOf("song" to 25, "jin" to 0)
        )
    )

    val officers = listOf(
        // V1.1 历史 Canon：岳飞建炎元年五月因越职上书被夺官，此时是流落军中的白身，
        // 非普通朝班可见之人；WANDERING 且可征辟，符合《金佗稡编》卷四记载。
        Officer("yue_fei", "岳飞", "军中小卒", command=99, force=98, strategy=93, politics=52, loyalty=100, currentCityId="xiangyang", status=OfficerStatus.WANDERING, charm=75, ambition=40, rankLevel=0, origin="军户", skills=listOf("步战","骑战","严军","野战","北伐"), bio="相州汤阴人，少负气节，沉厚寡言。建炎元年五月越职上书言事，被夺官逐出军中，此刻正流落待访。志在收复中原，待明主识拔。"),
        // V1.1 历史 Canon：韩世忠此时随行在有军职，但不是常朝文班固定成员，
        // 只在军务性质的场合（枢密院/SHUMI）以有限身份出席。
        Officer("han_shizhong", "韩世忠", "低阶武官", command=94, force=95, strategy=85, politics=58, loyalty=95, currentCityId="yingtianfu", status=OfficerStatus.IN_CAPITAL, charm=80, ambition=45, rankLevel=2, origin="军户", skills=listOf("水战","江防","突袭","守江"), bio="延安人，行伍出身，勇冠三军。此时任御营武职，随驾在南京应天府，尚未独当一面。"),
        Officer("li_gang", "李纲", "主战派", command=82, force=45, strategy=88, politics=92, loyalty=98, currentCityId="yingtianfu", charm=78, ambition=35, rankLevel=4, origin="士族", skills=listOf("守城","城防","民心","抗围城"), bio="邵武人，靖康年间力主抗金，守东京有功。开局当日刚抵应天行在并入见拜相，刚直敢言，然易遭主和派排挤。"),
        // V1.1 历史 Canon：《建炎以来系年要录》卷六载，六月己未朔宗泽本人自卫南分兵屯河上，
        // 以数百骑赴南都入对——开局当天他确实在朝，但很快外任知开封府、转东京留守，
        // 此后只应通过奏疏/军报常态出现，不再肉身参加普通朝会。scheduledTurn 处理这个自动转场。
        Officer("zong_ze", "宗泽", "主战派", command=90, force=65, strategy=92, politics=85, loyalty=100, currentCityId="yingtianfu", status=OfficerStatus.IN_COURT, scheduledStatus=OfficerStatus.DEPLOYED, scheduledCityId="kaifeng", scheduledTurn=3, charm=82, ambition=30, rankLevel=4, origin="寒门", skills=listOf("召义军","提振军心","中原反抗","抗金"), bio="婺州义乌人，老成持重。六月己未朔自卫南赴南京应天府入对，旬日内即将外任，转东京留守，招抚河北义军，三呼渡河而殁。其志可昭日月。"),
        // V1.1 历史 Canon：赵鼎建炎元年尚未进入本局核心叙事视野，不应作为开局默认 IN_COURT。
        Officer("zhao_ding", "赵鼎", "文臣派", command=55, force=30, strategy=86, politics=95, loyalty=96, currentCityId="linan", status=OfficerStatus.NOT_YET_RELEVANT, charm=72, ambition=40, rankLevel=4, origin="寒门", skills=listOf("政务","筹粮","安民","理财"), bio="解州闻喜人，中兴贤相。此时尚未进入朝廷核心，后期方显理财调和之才。"),
        // V1.1 历史 Canon：秦桧此时仍羁留金营，建炎四年十月方才南归。
        Officer("qin_hui", "秦桧", "寒门文士", command=35, force=25, strategy=85, politics=90, loyalty=30, currentCityId="kaifeng", status=OfficerStatus.CAPTIVE, charm=70, ambition=80, rankLevel=2, origin="寒门", skills=listOf("外交","党争","议和","内斗"), bio="江宁人。靖康中随二帝北狩，此刻仍羁留金方，尚未南归。其归诚真伪，日后朝野多有疑虑。"),
        // V1.1 历史 Canon：吴玠此时尚未进入本局核心叙事视野。
        Officer("wu_jie", "吴玠", "边地武人", command=90, force=85, strategy=88, politics=65, loyalty=95, currentCityId="xinguan", status=OfficerStatus.NOT_YET_RELEVANT, charm=68, ambition=42, rankLevel=3, origin="军户", skills=listOf("山地战","守关","克骑兵","川陕防线"), bio="德顺军陇干人，西北边军出身。此时尚未进入行在核心视野，后于仙人关、和尚原大破金军。"),
        Officer("zhang_jun", "张浚", "主战派", command=75, force=50, strategy=84, politics=82, loyalty=90, currentCityId="yingtianfu", charm=74, ambition=60, rankLevel=2, origin="士族", skills=listOf("政务","调度","荐才"), bio="汉州绵竹人。建炎初已在行在任事，但尚非后来的宣抚宰相；其后方逐渐掌兵与入相。"),
        // V1.1 历史 Canon：刘锜此时尚未进入本局核心叙事视野。
        Officer("liu_qi", "刘锜", "军中小卒", command=88, force=88, strategy=80, politics=55, loyalty=92, currentCityId="shouchun", status=OfficerStatus.NOT_YET_RELEVANT, charm=66, ambition=38, rankLevel=1, origin="将门", skills=listOf("守城","硬抗","反冲锋","顺昌大捷"), bio="德顺军人，泸川军节度使刘仲武之子。此时尚未进入行在核心视野，后于顺昌之战大破金军。"),
        Officer("zhang_jun2", "张俊", "低阶武官", command=78, force=75, strategy=60, politics=65, loyalty=55, currentCityId="yingtianfu", status=OfficerStatus.SOLDIER, charm=58, ambition=70, rankLevel=2, origin="军户", skills=listOf("野战","征伐","逐利"), bio="成纪人，行伍出身。建炎初仍属武臣体系中的一员，后方跻身中兴诸将。"),
        // V1.1 历史 Canon：黄潜善、汪伯彦开局即在朝，与李纲、宗泽的主战路线构成政治张力。
        Officer("huang_qianshan", "黄潜善", "执政文臣", command=40, force=20, strategy=55, politics=78, loyalty=55, currentCityId="yingtianfu", status=OfficerStatus.IN_COURT, charm=68, ambition=65, rankLevel=4, origin="士族", skills=listOf("因循","主和","巡幸"), bio="福州人，建炎初执政，与汪伯彦并进，力主南幸避敌，后因应对金军失当遭贬。"),
        Officer("wang_boyan", "汪伯彦", "执政文臣", command=42, force=22, strategy=58, politics=76, loyalty=52, currentCityId="yingtianfu", status=OfficerStatus.IN_COURT, charm=66, ambition=68, rankLevel=4, origin="士族", skills=listOf("因循","主和","逢迎"), bio="祁门人，藩邸旧臣，深得赵构信重，与黄潜善同秉朝政，主张避敌南幸，后同遭贬黜。")
    )

    val cities = listOf(
        // ===== 两浙路（1127 开局：杭州尚未升为临安府，越州亦尚未改绍兴） =====
        City("linan", "杭州", "song", troops=12000, defense=62, grain=100000, gold=55000, popularSupport=82, controlState="STABLE", route="两浙西路", cityLevel="州", terrain="river", population=300000, commerce=82, agriculture=72, isCapital=false, isWaterNode=true, x=11000, y=6800),
        City("shaoxing", "越州", "song", troops=8000, defense=60, grain=90000, gold=45000, popularSupport=88, controlState="STABLE", route="两浙东路", cityLevel="州", terrain="river", population=260000, commerce=76, agriculture=75, isWaterNode=true, x=11600, y=7000),
        City("suzhou", "平江", "song", troops=10000, defense=58, grain=120000, gold=70000, popularSupport=82, controlState="STABLE", route="两浙西路", cityLevel="府", terrain="river", population=350000, commerce=88, agriculture=80, isWaterNode=true, x=10800, y=6300),
        City("mingzhou", "明州", "song", troops=6000, defense=55, grain=50000, gold=40000, popularSupport=80, controlState="STABLE", route="两浙东路", cityLevel="州", terrain="coast", population=200000, commerce=85, agriculture=60, isWaterNode=true, x=12200, y=7100),
        City("wenzhou", "温州", "song", troops=5000, defense=50, grain=40000, gold=35000, popularSupport=80, controlState="STABLE", route="两浙东路", cityLevel="州", terrain="coast", population=180000, commerce=78, agriculture=58, isWaterNode=true, x=12000, y=7800),
        // ===== 江南东西路 =====
        City("jiankang", "建康", "song", troops=25000, defense=70, grain=80000, gold=30000, popularSupport=75, controlState="FRONTLINE", route="江南东路", cityLevel="府", terrain="river", population=350000, commerce=82, agriculture=68, isWaterNode=true, x=10200, y=5600),
        City("ningguo", "宣州", "song", troops=7000, defense=55, grain=60000, gold=20000, popularSupport=78, controlState="STABLE", route="江南东路", cityLevel="州", terrain="mountain", population=160000, commerce=60, agriculture=70, x=10000, y=6300),
        City("hongzhou", "洪州", "song", troops=12000, defense=62, grain=130000, gold=35000, popularSupport=80, controlState="STABLE", route="江南西路", cityLevel="州", terrain="river", population=280000, commerce=72, agriculture=82, isWaterNode=true, x=9200, y=7200),
        City("ganzhou", "虔州", "song", troops=6000, defense=58, grain=70000, gold=18000, popularSupport=72, controlState="STABLE", route="江南西路", cityLevel="州", terrain="mountain", population=150000, commerce=55, agriculture=68, x=9000, y=8000),
        // ===== 荆湖路 =====
        City("ezhou", "鄂州", "song", troops=40000, defense=65, grain=100000, gold=20000, popularSupport=80, controlState="STABLE", route="荆湖北路", cityLevel="州", terrain="river", population=260000, commerce=70, agriculture=75, isWaterNode=true, x=8400, y=6400),
        City("jiangling", "江陵", "song", troops=18000, defense=68, grain=110000, gold=25000, popularSupport=76, controlState="STABLE", route="荆湖北路", cityLevel="府", terrain="river", population=240000, commerce=68, agriculture=78, isWaterNode=true, x=7800, y=6000),
        City("tanzhou", "潭州", "song", troops=10000, defense=60, grain=120000, gold=22000, popularSupport=78, controlState="STABLE", route="荆湖南路", cityLevel="州", terrain="river", population=220000, commerce=65, agriculture=80, isWaterNode=true, x=8000, y=7400),
        // ===== 京西南路·荆襄防线 =====
        City("xiangyang", "襄阳", "song", troops=15000, defense=80, grain=50000, gold=15000, popularSupport=70, controlState="FRONTLINE", route="京西南路", cityLevel="府", terrain="pass", population=180000, commerce=58, agriculture=62, isPass=true, x=8200, y=5000),
        City("dengzhou", "邓州", "song", troops=8000, defense=62, grain=40000, gold=10000, popularSupport=60, controlState="CONTESTED", route="京西南路", cityLevel="州", terrain="plain", population=120000, commerce=50, agriculture=65, x=8400, y=4400),
        // ===== 淮南东西路·江淮防线 =====
        City("yangzhou", "扬州", "song", troops=10000, defense=50, grain=40000, gold=25000, popularSupport=60, controlState="CONTESTED", route="淮南东路", cityLevel="州", terrain="river", population=200000, commerce=80, agriculture=62, isWaterNode=true, x=10600, y=5100),
        City("chuzhou", "楚州", "song", troops=12000, defense=58, grain=50000, gold=12000, popularSupport=58, controlState="FRONTLINE", route="淮南东路", cityLevel="州", terrain="river", population=150000, commerce=60, agriculture=66, isWaterNode=true, x=10800, y=4600),
        City("hefei", "庐州", "song", troops=14000, defense=64, grain=55000, gold=14000, popularSupport=62, controlState="FRONTLINE", route="淮南西路", cityLevel="州", terrain="plain", population=160000, commerce=58, agriculture=70, x=9800, y=5000),
        City("shouchun", "寿春", "song", troops=10000, defense=66, grain=45000, gold=10000, popularSupport=58, controlState="FRONTLINE", route="淮南西路", cityLevel="府", terrain="river", population=130000, commerce=52, agriculture=64, isWaterNode=true, x=9600, y=4600),
        City("xinyang", "信阳", "song", troops=6000, defense=55, grain=30000, gold=8000, popularSupport=55, controlState="CONTESTED", route="京西南路", cityLevel="军", terrain="pass", population=90000, commerce=45, agriculture=58, isPass=true, x=9000, y=4400),
        // ===== 川陕四路 =====
        City("xinguan", "兴元府", "song", troops=35000, defense=70, grain=90000, gold=15000, popularSupport=78, controlState="STABLE", route="利州路", cityLevel="府", terrain="pass", population=170000, commerce=55, agriculture=68, isPass=true, x=6600, y=4200),
        City("chengdu", "成都", "song", troops=20000, defense=65, grain=120000, gold=40000, popularSupport=82, controlState="STABLE", route="成都府路", cityLevel="府", terrain="plain", population=380000, commerce=78, agriculture=88, x=5200, y=5400),
        City("zizhou", "潼川", "song", troops=8000, defense=58, grain=70000, gold=18000, popularSupport=78, controlState="STABLE", route="潼川府路", cityLevel="府", terrain="river", population=180000, commerce=62, agriculture=78, isWaterNode=true, x=5800, y=5800),
        City("kuizhou", "夔州", "song", troops=10000, defense=68, grain=50000, gold=12000, popularSupport=72, controlState="STABLE", route="夔州路", cityLevel="州", terrain="pass", population=120000, commerce=48, agriculture=60, isPass=true, isWaterNode=true, x=7000, y=6000),
        City("xianren_pass", "仙人关", "song", troops=12000, defense=85, grain=20000, gold=5000, popularSupport=70, controlState="FRONTLINE", route="利州路", cityLevel="关", terrain="pass", population=20000, commerce=20, agriculture=25, isPass=true, x=6200, y=3800),
        // ===== 福建·广南 =====
        City("fuzhou", "福州", "song", troops=8000, defense=55, grain=30000, gold=20000, popularSupport=80, controlState="STABLE", route="福建路", cityLevel="州", terrain="coast", population=200000, commerce=82, agriculture=55, isWaterNode=true, x=11000, y=8400),
        City("quanzhou", "泉州", "song", troops=5000, defense=52, grain=25000, gold=50000, popularSupport=82, controlState="STABLE", route="福建路", cityLevel="州", terrain="coast", population=250000, commerce=95, agriculture=50, isWaterNode=true, x=11200, y=8900),
        City("guangzhou", "广州", "song", troops=8000, defense=58, grain=40000, gold=60000, popularSupport=78, controlState="STABLE", route="广南东路", cityLevel="州", terrain="coast", population=280000, commerce=92, agriculture=58, isWaterNode=true, x=8800, y=9200),
        // ===== 南京应天府·建炎元年行在 =====
        City("yingtianfu", "南京应天府", "song", troops=20000, defense=68, grain=70000, gold=26000, popularSupport=68, controlState="FRONTLINE", route="京东西路", cityLevel="府", terrain="plain", population=220000, commerce=65, agriculture=68, isCapital=true, x=10000, y=4000),
        // ===== 金占区·黄河以北 =====
        City("kaifeng", "开封", "jin", troops=50000, defense=90, grain=200000, gold=100000, popularSupport=30, controlState="FALLEN", route="京畿路", cityLevel="府", terrain="plain", population=500000, commerce=85, agriculture=70, x=9400, y=3400),
        City("taiyuan", "太原", "jin", troops=35000, defense=85, grain=80000, gold=40000, popularSupport=35, controlState="FALLEN", route="河东路", cityLevel="府", terrain="mountain", population=200000, commerce=55, agriculture=58, x=8200, y=2400),
        City("daming", "大名府", "jin", troops=30000, defense=80, grain=70000, gold=35000, popularSupport=40, controlState="FALLEN", route="河北东路", cityLevel="府", terrain="plain", population=250000, commerce=60, agriculture=66, x=9600, y=2800),
        City("zhending", "真定", "jin", troops=22000, defense=78, grain=60000, gold=28000, popularSupport=38, controlState="FALLEN", route="河北西路", cityLevel="府", terrain="plain", population=180000, commerce=52, agriculture=62, x=9000, y=2200),
        City("hejian", "河间", "jin", troops=18000, defense=72, grain=50000, gold=24000, popularSupport=42, controlState="FALLEN", route="河北东路", cityLevel="府", terrain="plain", population=150000, commerce=48, agriculture=60, x=9800, y=2200),
        City("luoyang", "洛阳", "jin", troops=20000, defense=82, grain=70000, gold=30000, popularSupport=45, controlState="FALLEN", route="京西北路", cityLevel="府", terrain="river", population=220000, commerce=58, agriculture=64, x=8600, y=3600),
        City("jingzhao", "京兆府", "jin", troops=25000, defense=80, grain=90000, gold=35000, popularSupport=40, controlState="CONTESTED", route="永兴军路", cityLevel="府", terrain="plain", population=300000, commerce=62, agriculture=70, x=6800, y=3200),
        City("zhongshan", "中山", "jin", troops=15000, defense=75, grain=40000, gold=20000, popularSupport=48, controlState="CONTESTED", route="河北西路", cityLevel="府", terrain="mountain", population=120000, commerce=45, agriculture=55, x=9400, y=1800)
    )

    val armies = listOf(
        // id 保留 army_song_linan 仅为兼容旧存档/引用，实际开局驻地已改为南京应天府。
        Army("army_song_linan", "御营司行在军", "song", "", "yingtianfu", "yingtianfu", 15000, 62, "field_army", "yingtianfu",
            statusCode = ArmyStatus.GARRISONED, status = "拱卫行在"),
        Army("army_song_jiankang", "建康水军残部", "song", "", "jiankang", "jiankang", 9000, 60, "naval", "jiankang",
            statusCode = ArmyStatus.GARRISONED, status = "江防"),
        Army("army_song_ezhou", "鄂州地方军", "song", "", "ezhou", "ezhou", 12000, 58, "frontier", "ezhou",
            statusCode = ArmyStatus.GARRISONED, status = "驻防"),
        Army("army_jin_kaifeng", "金军开封大营", "jin", "", "kaifeng", "kaifeng", 30000, 82, "cavalry", "kaifeng",
            statusCode = ArmyStatus.GARRISONED, status = "占领"),
        Army("army_jin_daming", "金军大名府兵团", "jin", "", "daming", "daming", 22000, 78, "cavalry", "daming",
            statusCode = ArmyStatus.GARRISONED, status = "南侵预备"),
        Army("army_jin_taiyuan", "金军河东兵团", "jin", "", "taiyuan", "taiyuan", 25000, 80, "cavalry", "taiyuan",
            statusCode = ArmyStatus.GARRISONED, status = "镇守")
    )
}
