package io.github.lxzrvi.rekey.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignContractTest {
    @Test
    fun logoIsWhiteAndIconIsBlack() {
        assertEquals(0xFFFFFFFF, BrandContract.LOGO_ARGB)
        assertEquals(0xFF000000, BrandContract.ICON_ARGB)
    }

    @Test
    fun poppinsHasFourDistinctRequestedWeights() {
        assertEquals(listOf(100, 400, 500, 700), PoppinsWeight.entries.map { it.value })
    }

    @Test
    fun selectedHolidayFontIsNotMisrepresentedAsBundled() {
        assertEquals("Letterhend Studio", BrandContract.SELECTED_BRAND_FOUNDRY)
        assertEquals("Holiday", BrandContract.SELECTED_BRAND_FONT)
        assertFalse(BrandContract.LICENSED_BRAND_FONT_BUNDLED)
    }

    @Test
    fun motionDurationsAreBoundedAndPositive() {
        assertTrue(MotionContract.FADE_MILLIS > 0)
        assertTrue(MotionContract.EXIT_MILLIS in 1..400)
        assertTrue(MotionContract.ENTER_MILLIS in 1..400)
    }
}
