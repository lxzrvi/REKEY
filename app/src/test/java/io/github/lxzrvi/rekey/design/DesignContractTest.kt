package io.github.lxzrvi.rekey.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignContractTest {
    @Test
    fun appNameIsExactlyRekeyInCapitals() {
        assertEquals("REKEY", BrandContract.APP_NAME)
    }

    @Test
    fun alluraIsOnlyTheBrandFontAndPoppinsRemainsBodyFont() {
        assertEquals("Allura", BrandContract.BRAND_FONT)
        assertEquals("Poppins", BrandContract.BODY_FONT)
        assertEquals("SIL OFL 1.1", BrandContract.BRAND_FONT_LICENSE)
        assertEquals(listOf(100, 400, 500, 700), PoppinsWeight.entries.map { it.value })
    }

    @Test
    fun logoAndIconKeepRequestedMonochrome() {
        assertEquals(0xFFFFFFFF, BrandContract.LOGO_ARGB)
        assertEquals(0xFF000000, BrandContract.ICON_ARGB)
    }

    @Test
    fun pureBlurTargetsAndroid12AndAboveWithoutNoise() {
        assertEquals(31, GlassContract.MIN_SDK)
        assertEquals(0f, GlassContract.NOISE_FACTOR)
        assertTrue(GlassContract.DEFAULT_BLUR_DP in GlassContract.MIN_BLUR_DP..GlassContract.MAX_BLUR_DP)
    }

    @Test
    fun motionDurationsAreBoundedAndPositive() {
        assertTrue(MotionContract.SPLASH_HOLD_MILLIS in 500L..2000L)
        assertTrue(MotionContract.SPLASH_EXIT_MILLIS in 1..800)
        assertTrue(MotionContract.DRAWER_ENTER_MILLIS in 1..600)
        assertTrue(MotionContract.DRAWER_EXIT_MILLIS in 1..600)
    }
}
