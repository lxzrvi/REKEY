package io.github.lxzrvi.rekey.ui.keyboard

/**
 * ============================================================
 * SYMBOL PAGES  (5 changing pages)
 * ============================================================
 * Number layer (?123) me "shift" ki jagah jo key aati hai (=< / 1/5),
 * use dabane par pages ghoomte hain: 1/5 -> 2/5 -> 3/5 -> 4/5 -> 5/5 -> 1/5 ...
 *
 * Har page me 4 rows hain. Apne existing keyboard layout ki rows me
 * SymbolPager.currentRows() ke characters bind kar do.
 */
object SymbolPages {

    const val PAGE_COUNT = 5

    // Helper: "a b c" -> ["a","b","c"]  (space se alag karke row banata hai)
    private fun row(s: String): List<String> = s.split(" ")

    // NOTE: Kotlin string me '$' ko \$ likhna padta hai, '\' ko \\ aur '"' ko \"
    val pages: List<List<List<String>>> = listOf(

        // ---------- PAGE 1/5 : basic punctuation + numbers ----------
        listOf(
            row("1 2 3 4 5 6 7 8 9 0"),
            row("@ # \$ _ & - + ( ) /"),
            row("* \" ' : ; ! ? % ^ ~"),
            row(", . … ¿ ¡ ` | \\ · •")
        ),

        // ---------- PAGE 2/5 : math + currency + fractions ----------
        listOf(
            row("± × ÷ = ≠ ≈ < > ≤ ≥"),
            row("₹ \$ € £ ¥ ¢ ₩ ₽ ₿ ₺"),
            row("√ ∞ ∑ ∏ ∫ ∂ ∆ π µ °"),
            row("% ‰ ‱ ¼ ½ ¾ ⅓ ⅔ ⅛ ⅜")
        ),

        // ---------- PAGE 3/5 : arrows + shapes + marks ----------
        listOf(
            row("← ↑ → ↓ ↔ ↕ ⇐ ⇒ ⇔ ↩"),
            row("★ ☆ ♥ ♦ ♣ ♠ ♪ ♫ ☀ ☁"),
            row("● ○ ■ □ ▲ △ ▼ ▽ ◆ ◇"),
            row("✓ ✔ ✗ ✘ ☑ ☒ ✚ ✖ ❖ ✦")
        ),

        // ---------- PAGE 4/5 : brackets + quotes + legal ----------
        listOf(
            row("{ } [ ] « » ‹ › 〈 〉"),
            row("“ ” ‘ ’ „ ‚ – — ‐ ․"),
            row("© ® ™ § ¶ † ‡ № ℗ ℠"),
            row("¦ ¬ ¤ ¨ ¯ ´ ¸ ˆ ˜ ˚")
        ),

        // ---------- PAGE 5/5 : greek + super/subscript + units ----------
        listOf(
            row("α β γ δ ε θ λ μ σ ω"),
            row("¹ ² ³ ⁴ ⁵ ⁶ ⁷ ⁸ ⁹ ⁰"),
            row("₁ ₂ ₃ ₄ ₅ ₆ ₇ ₈ ₉ ₀"),
            row("Ω Σ Δ Π Φ Ψ Λ Γ Θ ℃")
        )
    )
}

/**
 * Page switch ka logic. UI se connect karne ka tareeka:
 *
 * val pager = SymbolPager { page, rows ->
 *     bindRowsToKeys(rows)                      // apna function: rows ke chars keys par lagao
 *     toggleKey.text = pager.toggleKeyLabel()   // "2/5" jaisa label shift-wali key par
 * }
 * toggleKey.setOnClickListener { pager.next() }                     // tap -> agla page
 * toggleKey.setOnLongClickListener { pager.previous(); true }       // long press -> pichhla page
 * // Letters layout par wapas jaane par: pager.reset()
 */
class SymbolPager(
    private val onPageChanged: (page: Int, rows: List<List<String>>) -> Unit
) {
    var page: Int = 0
        private set

    fun currentRows(): List<List<String>> = SymbolPages.pages[page]

    /** Shift-wali key par dikhane ka label, jaise "1/5" */
    fun toggleKeyLabel(): String = "${page + 1}/${SymbolPages.PAGE_COUNT}"

    fun next() = setPage((page + 1) % SymbolPages.PAGE_COUNT)

    fun previous() = setPage((page - 1 + SymbolPages.PAGE_COUNT) % SymbolPages.PAGE_COUNT)

    fun reset() = setPage(0)

    fun setPage(p: Int) {
        page = p.coerceIn(0, SymbolPages.PAGE_COUNT - 1)
        onPageChanged(page, currentRows())
    }
}
