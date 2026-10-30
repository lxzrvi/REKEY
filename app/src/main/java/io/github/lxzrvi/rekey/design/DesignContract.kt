package io.github.lxzrvi.rekey.design

/** Platform-independent brand decisions that can be checked without an Android runtime. */
object BrandContract {
    const val APP_NAME = "REKEY"
    const val LOGO_ARGB = 0xFFFFFFFF
    const val ICON_ARGB = 0xFF000000
    const val SELECTED_BRAND_FOUNDRY = "Letterhend Studio"
    const val SELECTED_BRAND_FONT = "Holiday"

    // Never report the temporary Poppins wordmark as licensed Holiday typography.
    const val LICENSED_BRAND_FONT_BUNDLED = false
}

enum class PoppinsWeight(val value: Int) {
    THIN(100),
    REGULAR(400),
    MEDIUM(500),
    BOLD(700),
}

object MotionContract {
    const val ENTER_MILLIS = 320
    const val EXIT_MILLIS = 240
    const val FADE_MILLIS = 180
}
