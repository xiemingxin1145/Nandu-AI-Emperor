package com.xiemingxin.nandu.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationPolicyTest {
    @Test
    fun mainMenuSettingsOverlayIsNotHiddenByMainMenuReturn() {
        assertTrue(AppNavigationPolicy.shouldShowMainMenu(true, false, false))
        assertFalse(AppNavigationPolicy.shouldShowMainMenu(true, false, true))
    }

    @Test
    fun settingsReturnClosesSettingsBeforeAnyUnderlyingPage() {
        assertEquals(AppBackTarget.SETTINGS, target(showSettings = true, currentTab = 4))
        assertEquals(AppBackTarget.PRIMARY_TAB, target(currentTab = 4))
        assertNull(target())
    }

    @Test
    fun prologueSystemBackReturnsToMainMenu() {
        assertEquals(AppBackTarget.PROLOGUE, target(showPrologue = true))
    }

    @Test
    fun cityAndPalaceLayersCloseBeforeTheirUnderlyingPrimaryTab() {
        assertEquals(AppBackTarget.CITY_INTERIOR, target(interiorCityId = "yingtianfu", currentTab = 2))
        assertEquals(AppBackTarget.PALACE, target(activePalaceId = "wende", currentTab = 1))
    }

    @Test
    fun officerListClosesBeforeLeavingMilitaryTab() {
        assertEquals(AppBackTarget.OFFICERS, target(showOfficerList = true, currentTab = 4))
        assertEquals(AppBackTarget.PRIMARY_TAB, target(currentTab = 4))
    }

    @Test
    fun battleLayerClosesBeforeLeavingItsUnderlyingPage() {
        assertEquals(AppBackTarget.BATTLE, target(showShunchangBattle = true, currentTab = 1))
    }

    private fun target(
        showSettings: Boolean = false,
        showPrologue: Boolean = false,
        interiorCityId: String? = null,
        activePalaceId: String? = null,
        showOfficerList: Boolean = false,
        showShunchangBattle: Boolean = false,
        currentTab: Int = 0
    ): AppBackTarget? = AppNavigationPolicy.backTarget(
        showSettings = showSettings,
        showPrologue = showPrologue,
        interiorCityId = interiorCityId,
        activePalaceId = activePalaceId,
        showOfficerList = showOfficerList,
        showShunchangBattle = showShunchangBattle,
        currentTab = currentTab
    )
}
