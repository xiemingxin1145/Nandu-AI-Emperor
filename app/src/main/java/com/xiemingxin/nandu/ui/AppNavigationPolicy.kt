package com.xiemingxin.nandu.ui

/** Pure routing rules shared by Compose rendering, system back, and JVM regression tests. */
internal object AppNavigationPolicy {
    fun shouldShowMainMenu(showIntro: Boolean, showPrologue: Boolean, showSettings: Boolean): Boolean =
        showIntro && !showPrologue && !showSettings

    fun backTarget(
        showSettings: Boolean,
        showPrologue: Boolean,
        interiorCityId: String?,
        activePalaceId: String?,
        showOfficerList: Boolean,
        showShunchangBattle: Boolean,
        currentTab: Int
    ): AppBackTarget? = when {
        showSettings -> AppBackTarget.SETTINGS
        showPrologue -> AppBackTarget.PROLOGUE
        interiorCityId != null -> AppBackTarget.CITY_INTERIOR
        activePalaceId != null -> AppBackTarget.PALACE
        showShunchangBattle -> AppBackTarget.BATTLE
        showOfficerList -> AppBackTarget.OFFICERS
        currentTab != 0 -> AppBackTarget.PRIMARY_TAB
        else -> null
    }
}

internal enum class AppBackTarget {
    SETTINGS,
    PROLOGUE,
    CITY_INTERIOR,
    PALACE,
    BATTLE,
    OFFICERS,
    PRIMARY_TAB
}
