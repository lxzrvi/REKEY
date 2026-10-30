package io.github.lxzrvi.rekey.design

/** Platform-independent design contracts. */
object BrandContract {
    const val APP_NAME = "REKEY"
    const val LOGO_ARGB = 0xFFFFFFFF
    const val ICON_ARGB = 0xFF000000
    const val BRAND_FONT = "Allura"
    const val BRAND_FOUNDRY = "TypeSETit / Robert E. Leuschke"
    const val BRAND_FONT_LICENSE = "SIL OFL 1.1"
    const val BODY_FONT = "Poppins"
}

enum class PoppinsWeight(val value: Int) {
    THIN(100), REGULAR(400), MEDIUM(500), BOLD(700)
}

object MotionContract {
    const val SPLASH_HOLD_MILLIS = 1100L
    const val SPLASH_EXIT_MILLIS = 520
    const val DRAWER_ENTER_MILLIS = 360
    const val DRAWER_EXIT_MILLIS = 280
    const val CARD_SETTLE_MILLIS = 420
}

object GlassContract {
    const val MIN_SDK = 31
    const val DEFAULT_BLUR_DP = 28f
    const val MIN_BLUR_DP = 8f
    const val MAX_BLUR_DP = 40f
    const val NOISE_FACTOR = 0f
}
