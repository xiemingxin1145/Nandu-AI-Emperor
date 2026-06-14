package com.xiemingxin.nandu.game

object InitialData {

    val factions = listOf(
        Faction("song", "大宋", "宋", "赵构", "linan", "南渡未稳，主战主和并立", isPlayable = true),
        Faction("jin", "金国", "金", "完颜宗望", "kaifeng", "兵锋正盛，窥伺江淮"),
        Faction("rebel", "地方义军", "义", "群豪", "huaihe", "观望朝廷，易受招抚或离散"),
        Faction("dali", "大理", "理", "段氏", "chengdu", "西南观望，暂不直接参战")
    )

    val officers = listOf(
        Officer("yue_fei",       "岳飞",   "军中小卒", command=99, force=98, strategy=93, politics=52, loyalty=100, currentCityId="xiangyang", status=OfficerStatus.HIDDEN),
        Officer("han_shizhong",  "韩世忠", "低阶武官", command=94, force=95, strategy=85, politics=58, loyalty=95,  currentCityId="jiankang", status=OfficerStatus.SOLDIER),
        Officer("li_gang",       "李纲",   "主战派", command=82, force=45, strategy=88, politics=92, loyalty=98,  currentCityId="linan"),
        Officer("zong_ze",       "宗泽",   "主战派", command=90, force=65, strategy=92, politics=85, loyalty=100, currentCityId="kaifeng", status=OfficerStatus.WANDERING),
        Officer("zhao_ding",     "赵鼎",   "文臣派", command=55, force=30, strategy=86, politics=95, loyalty=96,  currentCityId="linan"),
        Officer("qin_hui",       "秦桧",   "寒门文士", command=35, force=25, strategy=85, politics=90, loyalty=30,  currentCityId="linan", status=OfficerStatus.HIDDEN),
        Officer("wu_jie",        "吴玠",   "边地武人", command=90, force=85, strategy=88, politics=65, loyalty=95,  currentCityId="xinguan", status=OfficerStatus.HIDDEN),
        Officer("zhang_jun",     "张浚",   "主战派", command=75, force=50, strategy=84, politics=82, loyalty=90,  currentCityId="linan"),
        Officer("liu_qi",        "刘锜",   "军中小卒", command=88, force=88, strategy=80, politics=55, loyalty=92, currentCityId="huaihe", status=OfficerStatus.HIDDEN),
        Officer("zhang_俊",      "张俊",   "低阶武官", command=78, force=75, strategy=60, politics=65, loyalty=55, currentCityId="linan", status=OfficerStatus.SOLDIER)
    )

    val cities = listOf(
        City("linan",    "临安",    "song", troops=20000, defense=75, grain=150000, gold=80000, popularSupport=85, controlState="STABLE"),
        City("jiankang", "建康",    "song", troops=25000, defense=70, grain=80000,  gold=30000, popularSupport=75, controlState="FRONTLINE"),
        City("ezhou",    "鄂州",    "song", troops=40000, defense=65, grain=100000, gold=20000, popularSupport=80, controlState="STABLE"),
        City("xiangyang","襄阳",    "song", troops=15000, defense=80, grain=50000,  gold=15000, popularSupport=70, controlState="FRONTLINE"),
        City("huaihe",   "淮河防线","song", troops=30000, defense=60, grain=60000,  gold=10000, popularSupport=65, controlState="FRONTLINE"),
        City("yangzhou", "扬州",    "song", troops=10000, defense=50, grain=40000,  gold=25000, popularSupport=60, controlState="CONTESTED"),
        City("xinguan",  "兴元府",  "song", troops=35000, defense=70, grain=90000,  gold=15000, popularSupport=78, controlState="STABLE"),
        City("chengdu",  "成都",    "song", troops=20000, defense=65, grain=120000, gold=40000, popularSupport=82, controlState="STABLE"),
        City("fuzhou",   "福州",    "song", troops=8000,  defense=55, grain=30000,  gold=20000, popularSupport=80, controlState="STABLE"),
        City("kaifeng",  "开封",    "jin",  troops=50000, defense=90, grain=200000, gold=100000, popularSupport=30, controlState="FALLEN"),
        City("taiyuan",  "太原",    "jin",  troops=35000, defense=85, grain=80000,  gold=40000,  popularSupport=35, controlState="FALLEN"),
        City("daming",   "大名府",  "jin",  troops=30000, defense=80, grain=70000,  gold=35000,  popularSupport=40, controlState="FALLEN")
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
