package com.xiemingxin.nandu.game

object InitialData {

    val factions = listOf(
        Faction("song", "大宋", "宋", "赵构", "linan", "南渡未稳，主战主和并立", isPlayable = true),
        Faction("jin", "金国", "金", "完颜宗望", "kaifeng", "兵锋正盛，窥伺江淮"),
        Faction("rebel", "地方义军", "义", "群豪", "huaihe", "观望朝廷，易受招抚或离散"),
        Faction("dali", "大理", "理", "段氏", "chengdu", "西南观望，暂不直接参战")
    )

    val officers = listOf(
        Officer("yue_fei", "岳飞", "军中小卒", command=99, force=98, strategy=93, politics=52, loyalty=100, currentCityId="xiangyang", status=OfficerStatus.HIDDEN, charm=75, ambition=40, rankLevel=0, origin="军户", skills=listOf("步战","骑战","严军","野战","北伐"), bio="相州汤阴人，少负气节，沉厚寡言。背刺精忠报国，志在收复中原。开局沉于行伍，待明主识拔。"),
        Officer("han_shizhong", "韩世忠", "低阶武官", command=94, force=95, strategy=85, politics=58, loyalty=95, currentCityId="jiankang", status=OfficerStatus.SOLDIER, charm=80, ambition=45, rankLevel=2, origin="军户", skills=listOf("水战","江防","突袭","守江"), bio="延安人，行伍出身，勇冠三军。其妻梁红玉亦有巾帼之志。擅水战，黄天荡可建奇功。"),
        Officer("li_gang", "李纲", "主战派", command=82, force=45, strategy=88, politics=92, loyalty=98, currentCityId="linan", charm=78, ambition=35, rankLevel=4, origin="士族", skills=listOf("守城","城防","民心","抗围城"), bio="邵武人，靖康年间力主抗金，守东京有功。刚直敢言，然易遭主和派排挤。"),
        Officer("zong_ze", "宗泽", "主战派", command=90, force=65, strategy=92, politics=85, loyalty=100, currentCityId="kaifeng", status=OfficerStatus.WANDERING, charm=82, ambition=30, rankLevel=4, origin="寒门", skills=listOf("召义军","提振军心","中原反抗","抗金"), bio="婺州义乌人，老成持重。东京留守，招抚河北义军，三呼渡河而殁。其志可昭日月。"),
        Officer("zhao_ding", "赵鼎", "文臣派", command=55, force=30, strategy=86, politics=95, loyalty=96, currentCityId="linan", charm=72, ambition=40, rankLevel=4, origin="寒门", skills=listOf("政务","筹粮","安民","理财"), bio="解州闻喜人，中兴贤相。长于治理，善理财赋，调和朝堂。然与秦桧政见相左。"),
        Officer("qin_hui", "秦桧", "寒门文士", command=35, force=25, strategy=85, politics=90, loyalty=30, currentCityId="linan", status=OfficerStatus.HIDDEN, charm=70, ambition=80, rankLevel=2, origin="寒门", skills=listOf("外交","党争","议和","内斗"), bio="江宁人，工于心计。靖康中随二帝北狩，后自金营南归，其归诚真伪，朝野多有疑虑。"),
        Officer("wu_jie", "吴玠", "边地武人", command=90, force=85, strategy=88, politics=65, loyalty=95, currentCityId="xinguan", status=OfficerStatus.HIDDEN, charm=68, ambition=42, rankLevel=3, origin="军户", skills=listOf("山地战","守关","克骑兵","川陕防线"), bio="德顺军陇干人，西北边军出身。仙人关、和尚原大破金军，川陕锁钥赖以不失。"),
        Officer("zhang_jun", "张浚", "主战派", command=75, force=50, strategy=84, politics=82, loyalty=90, currentCityId="linan", charm=74, ambition=60, rankLevel=4, origin="士族", skills=listOf("都督诸军","北伐","调度","荐才"), bio="汉州绵竹人，志大才疏而忠勇可嘉。力主北伐，然富平之败为其所累，用之需权衡。"),
        Officer("liu_qi", "刘锜", "军中小卒", command=88, force=88, strategy=80, politics=55, loyalty=92, currentCityId="huaihe", status=OfficerStatus.HIDDEN, charm=66, ambition=38, rankLevel=1, origin="将门", skills=listOf("守城","硬抗","反冲锋","顺昌大捷"), bio="德顺军人，泸川军节度使刘仲武之子。沉毅善守，顺昌之战大破金军铁浮屠，威震淮西。"),
        Officer("zhang_jun2", "张俊", "低阶武官", command=78, force=75, strategy=60, politics=65, loyalty=55, currentCityId="linan", status=OfficerStatus.SOLDIER, charm=58, ambition=70, rankLevel=2, origin="军户", skills=listOf("野战","征伐","逐利"), bio="成纪人，中兴四将之一，然贪财好利，晚节附秦桧构陷岳飞。忠诚存疑，用之当防。")
    )

    val cities = listOf(
        // ===== 南宋核心区·两浙路 =====
        City("linan", "临安", "song", troops=20000, defense=75, grain=150000, gold=80000, popularSupport=85, controlState="STABLE", route="两浙西路", cityLevel="府", terrain="river", population=400000, commerce=90, agriculture=70, isCapital=true, isWaterNode=true, x=11000, y=6800),
        City("shaoxing", "绍兴", "song", troops=8000, defense=60, grain=90000, gold=45000, popularSupport=88, controlState="STABLE", route="两浙东路", cityLevel="府", terrain="river", population=300000, commerce=80, agriculture=75, isWaterNode=true, x=11600, y=7000),
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
        // ===== 应天府·宋金交界前线 =====
        City("yingtianfu", "应天府", "song", troops=15000, defense=60, grain=50000, gold=18000, popularSupport=55, controlState="FRONTLINE", route="京东西路", cityLevel="府", terrain="plain", population=200000, commerce=62, agriculture=68, x=10000, y=4000),
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
        Army("army_song_linan", "御营司残军", "song", "", "linan", "linan", 15000, 62, "field_army", "linan", "拱卫"),
        Army("army_song_jiankang", "建康水军残部", "song", "", "jiankang", "jiankang", 9000, 60, "naval", "jiankang", "江防"),
        Army("army_song_ezhou", "鄂州地方军", "song", "", "ezhou", "ezhou", 12000, 58, "frontier", "ezhou", "驻防"),
        Army("army_jin_kaifeng", "金军开封大营", "jin", "", "kaifeng", "kaifeng", 30000, 82, "cavalry", "kaifeng", "占领"),
        Army("army_jin_daming", "金军大名府兵团", "jin", "", "daming", "daming", 22000, 78, "cavalry", "daming", "南侵预备"),
        Army("army_jin_taiyuan", "金军河东兵团", "jin", "", "taiyuan", "taiyuan", 25000, 80, "cavalry", "taiyuan", "镇守")
    )
}
