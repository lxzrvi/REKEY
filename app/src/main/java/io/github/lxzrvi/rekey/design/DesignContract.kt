package io.github.lxzrvi.rekey.design

/** Platform-independent design contracts. */
object BrandContract {
    const val APP_NAME = "REKEY"
    const val LOGO_ARGB = 0xFFFFFFFF
    const val ICON_ARGB = 0xFF000000
    const val BRAND_FONT = "Cinzel Decorative"
    const val BRAND_FOUNDRY = "Natanael Gama / NDISCOVER"
    const val BRAND_FONT_LICENSE = "SIL OFL 1.1"
    const val BODY_FONT = "Poppins"
}

enum class PoppinsWeight(val value: Int) {
    THIN(100), REGULAR(400), MEDIUM(500), BOLD(700)
}

object MotionContract {
    const val EDITOR_ENTER_MILLIS = 460
    const val EDITOR_EXIT_MILLIS = 400
    const val DRAWER_ENTER_MILLIS = 360
    const val DRAWER_EXIT_MILLIS = 280
    const val CARD_SETTLE_MILLIS = 420
    const val TEST_ENTER_MILLIS = 420
    const val TEST_EXIT_MILLIS = 360
    const val CARD_EXIT_MILLIS = 520
    const val CARD_GAP_CLOSE_MILLIS = 130L
}

/** Blur is always on and fixed by owner request: 15 dp, corners 15 dp. */
object GlassContract {
    const val MIN_SDK = 31
    const val BLUR_DP = 15f
    const val CORNER_DP = 15f
    const val NOISE_FACTOR = 0f
}

/** Text size is locked to the old S step; the drawer no longer offers a choice. */
object TypeContract {
    const val TEXT_SCALE = 0.9f
}

/** The squiggle background is drawn from the same geometry as the launcher icon. */
object PatternContract {
    const val DESIGN_WIDTH = 1024f
    const val DESIGN_HEIGHT = 1536f
    const val STROKE = 40f
    const val LIGHT_INK_ALPHA = 0.12f
    const val DARK_INK_ALPHA = 0.14f
}
