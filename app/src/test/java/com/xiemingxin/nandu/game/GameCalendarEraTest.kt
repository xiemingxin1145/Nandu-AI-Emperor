package com.xiemingxin.nandu.game

import org.junit.Test
import org.junit.Assert.*

/**
 * V1.6.2 STAB-006：GameCalendar 年号切换回归测试。
 *
 * 修复前 advance() 无论推进多少年都硬编码"建炎"前缀，玩家推进十几二十年后
 * 界面上仍然显示"建炎十五年"这种史书上不存在的年号——这是正式流程里会被
 * 玩家直接看到的历史穿帮。建炎只用了四年（1127-1130），绍兴元年起于1131年。
 */
class GameCalendarEraTest {

    // 从建炎元年（year=1）连续推进到指定 year，返回该年首个 GameCalendar。
    private fun advanceToYear(targetYear: Int): GameCalendar {
        var cal = GameCalendar()
        while (cal.year < targetYear) {
            // 每年 12 个月 * 3 旬，快进到年底再跨一次年
            repeat(12 * 3) { cal = cal.advance() }
        }
        return cal
    }

    @Test
    fun openingYearIsJianyanYuan() {
        val cal = GameCalendar()
        assertEquals("建炎元年", cal.eraName)
        assertEquals(1, cal.year)
    }

    @Test
    fun jianyanEraCoversExactlyFourYears() {
        assertEquals("建炎元年", advanceToYear(1).eraName)
        assertEquals("建炎二年", advanceToYear(2).eraName)
        assertEquals("建炎三年", advanceToYear(3).eraName)
        assertEquals("建炎四年", advanceToYear(4).eraName)
    }

    @Test
    fun fifthGameYearSwitchesToShaoxingYuan() {
        val cal = advanceToYear(5)
        assertEquals("绍兴元年", cal.eraName)
    }

    @Test
    fun eraNameNeverStaysJianyanPastYearFour() {
        // 之前的 bug：无论推进多少年，advance() 都会一直吐出"建炎N年"。
        // 这里断言远期年份（对应顺昌之战候选窗口附近）确实已经切到绍兴纪年。
        val farCal = advanceToYear(15)
        assertFalse("推进15个游戏年后年号不应仍是建炎", farCal.eraName.startsWith("建炎"))
        assertEquals("绍兴十一年", farCal.eraName)
    }

    @Test
    fun eraNameForIsConsistentWithAdvance() {
        // eraNameFor 是纯函数版本，应该跟真实 advance() 产生的结果一致，
        // 方便其它系统（如战报文案）不必真的跑一遍日历推进就能拿到正确年号。
        for (year in 1..20) {
            assertEquals(advanceToYear(year).eraName, GameCalendar.eraNameFor(year))
        }
    }

    @Test
    fun displayTextUsesSwitchedEraName() {
        val cal = advanceToYear(6)
        assertTrue(cal.displayText().startsWith("绍兴二年"))
    }
}
