package com.xiemingxin.nandu.game

/**
 * 临时兼容层：V0.6.4 人物官阶成长准备字段。
 * 先让 Profile 层可编译，下一步再把 rankLevel / merit 写入 Officer 主结构和存档。
 */
val Officer.rankLevel: Int get() = 0
val Officer.merit: Int get() = 0
