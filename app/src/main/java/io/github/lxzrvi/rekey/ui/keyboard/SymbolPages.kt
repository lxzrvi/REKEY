package io.github.lxzrvi.rekey.ui.keyboard

/**
 * ============================================================
 * SYMBOL PAGES  (6 changing pages, 3 rows each)
 * ============================================================
 * Number layer (?123) me "shift" ki jagah jo key aati hai (=< / 1/6),
 * use dabane par pages ghoomte hain: 1/6 -> 2/6 ... -> 6/6 -> 1/6.
 * Tap = agla page, long press = pichhla page, ABC par wapas aate hi reset.
 *
 * Har page ki rows: [10 chars, 10 chars, 8 chars] — row 3 page counter aur
 * backspace ke beech me baithti hai, isliye board sirf 3 rows ka rehta hai.
 */
object SymbolPages {

    const val PAGE_COUNT = 6

    // Helper: "a b c" -> ["a","b","c"]  (space se alag karke row banata hai)
    private fun row(s: String): List<String> = s.split(" ")

    // NOTE: Kotlin string me '$' ko \$ likhna padta hai, '\' ko \\ aur '"' ko \"
    val pages: List<List<List<String>>> = listOf(

        // ---------- PAGE 1/6 : digits + basic punctuation ----------
        listOf(
            row("1 2 3 4 5 6 7 8 9 0"),
            row("@ # \$ _ & - + ( ) /"),
            row("* \" ' : ; ! ? %")
        ),

        // ---------- PAGE 2/6 : math + currency + roots ----------
        listOf(
            row("± × ÷ = ≠ ≈ < > ≤ ≥"),
            row("₹ \$ € £ ¥ ¢ ₩ ₽ ₿ ₺"),
            row("√ ∞ ∑ ∏ ∫ ∂ ∆ π")
        ),

        // ---------- PAGE 3/6 : arrows + shapes + marks ----------
        listOf(
            row("← ↑ → ↓ ↔ ↕ ⇐ ⇒ ⇔ ↩"),
            row("★ ☆ ♥ ♦ ♣ ♠ ♪ ♫ ☀ ☁"),
            row("● ○ ■ □ ▲ △ ▼ ▽")
        ),

        // ---------- PAGE 4/6 : brackets + quotes + legal ----------
        listOf(
            row("{ } [ ] « » ‹ › 〈 〉"),
            row("“ ” ‘ ’ „ ‚ – — ‐ ․"),
            row("© ® ™ § ¶ † ‡ №")
        ),

        // ---------- PAGE 5/6 : greek + super/subscript ----------
        listOf(
            row("α β γ δ ε θ λ μ σ ω"),
            row("¹ ² ³ ⁴ ⁵ ⁶ ⁷ ⁸ ⁹ ⁰"),
            row("₁ ₂ ₃ ₄ ₅ ₆ ₇ ₈")
        ),

        // ---------- PAGE 6/6 : fractions + misc marks ----------
        listOf(
            row("‰ ‱ ¼ ½ ¾ ⅓ ⅔ ⅛ ⅜ ℃"),
            row("… ¿ ¡ ` | \\ · • ⟨ ⟩"),
            row("¦ ¬ ¤ ¨ ¯ ´ ¸ ˆ")
        )
    )
}

/**
 * Page switch ka logic (live keyboard isi ko use karta hai).
 */
class SymbolPager(
    private val onPageChanged: (page: Int, rows: List<List<String>>) -> Unit
) {
    var page: Int = 0
        private set

    fun currentRows(): List<List<String>> = SymbolPages.pages[page]

    /** Counter key par dikhane ka label, jaise "1/6" */
    fun toggleKeyLabel(): String = "${page + 1}/${SymbolPages.PAGE_COUNT}"

    fun next() = setPage((page + 1) % SymbolPages.PAGE_COUNT)

    fun previous() = setPage((page - 1 + SymbolPages.PAGE_COUNT) % SymbolPages.PAGE_COUNT)

    fun reset() = setPage(0)

    fun setPage(p: Int) {
        page = p.coerceIn(0, SymbolPages.PAGE_COUNT - 1)
        onPageChanged(page, currentRows())
    }
}
