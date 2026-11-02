package io.github.lxzrvi.rekey.ui.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import kotlin.math.hypot

/**
 * ============================================================
 * KEY HINTS + ADVANCED LONG-PRESS POPUP
 * ============================================================
 * Features:
 * - Key ke top-right corner me chhota HINT (jaise Q par "1", W par "2")
 * - Long press par popup: accents, digits, symbols, currency, punctuation
 * - SLIDE-TO-SELECT: ungli ghumao, jis par chhodo wahi type hoga (Gboard jaisa)
 * - 6 se zyada options ho to popup multi-row ban jata hai
 * - Haptic tick har selection badalne par
 * - Shift/CapsLock par popup chars bhi UPPERCASE hote hain
 * - Screen ke edge se bahar nahi jata (auto clamp)
 * - Emoji skin-tone ke liye bhi wahi popup use hota hai
 *   (attach(..., alternates = emoji.variants))
 */
object KeyHints {

    // Top row letters par number hint (Q=1 ... P=0)
    private val digitHints = mapOf(
        "q" to "1", "w" to "2", "e" to "3", "r" to "4", "t" to "5",
        "y" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0"
    )

    // Letters ke accented alternates
    private val accents = mapOf(
        "a" to listOf("à", "á", "â", "ä", "æ", "ã", "å", "ā"),
        "c" to listOf("ç", "ć", "č"),
        "d" to listOf("ď", "đ"),
        "e" to listOf("è", "é", "ê", "ë", "ē", "ė", "ę"),
        "g" to listOf("ğ"),
        "i" to listOf("î", "ï", "í", "ī", "į", "ì"),
        "l" to listOf("ł"),
        "n" to listOf("ñ", "ń"),
        "o" to listOf("ô", "ö", "ò", "ó", "œ", "ø", "ō", "õ"),
        "r" to listOf("ř"),
        "s" to listOf("ß", "ś", "š", "ş"),
        "t" to listOf("ť"),
        "u" to listOf("û", "ü", "ù", "ú", "ū"),
        "y" to listOf("ý", "ÿ"),
        "z" to listOf("ž", "ź", "ż")
    )

    // Symbols / numbers / punctuation ke alternates
    // ('$' ko \$ likhna zaroori hai Kotlin strings me)
    private val symbols = mapOf(
        "." to listOf("…", ",", "!", "?", ":", ";", "'", "\"", "-"),
        "," to listOf(";", ":", "،", "、"),
        "-" to listOf("–", "—", "•", "_", "~"),
        "!" to listOf("¡", "‼", "⁉"),
        "?" to listOf("¿", "⁇", "⸮"),
        "'" to listOf("‘", "’", "‚", "‹", "›"),
        "\"" to listOf("“", "”", "„", "«", "»"),
        "\$" to listOf("₹", "€", "£", "¥", "¢", "₩", "₽", "₿"),
        "%" to listOf("‰", "‱", "℅"),
        "+" to listOf("±", "×", "÷", "∓"),
        "=" to listOf("≠", "≈", "≤", "≥", "≡"),
        "*" to listOf("×", "✱", "†", "‡", "★"),
        "#" to listOf("№", "♯", "℅"),
        "@" to listOf("©", "®", "™"),
        "&" to listOf("§", "¶"),
        "/" to listOf("÷", "\\", "|"),
        "(" to listOf("[", "{", "<", "⟨"),
        ")" to listOf("]", "}", ">", "⟩"),
        "<" to listOf("≤", "«", "‹"),
        ">" to listOf("≥", "»", "›"),
        "0" to listOf("°", "⁰", "₀", "∅"),
        "1" to listOf("¹", "₁", "½", "⅓", "¼"),
        "2" to listOf("²", "₂", "⅔"),
        "3" to listOf("³", "₃", "¾", "⅜"),
        "4" to listOf("⁴", "₄"),
        "5" to listOf("⁵", "₅"),
        "6" to listOf("⁶", "₆"),
        "7" to listOf("⁷", "₇"),
        "8" to listOf("⁸", "₈", "∞"),
        "9" to listOf("⁹", "₉")
    )

    /**
     * Four corner hints, in [TL, TR, BR, BL] order (null = empty corner).
     *
     * TR carries the digit on the top row, otherwise the first accent or
     * symbol; the remaining corners are filled from accents, then symbols,
     * then a shared punctuation pool, so every key always shows four hints
     * for the long-press slide to choose from.
     */
    private val pool = listOf(
        "'", ":", "-", ")", "(", "/", "*", "+", "=", "_",
        ".", ",", "?", "!", "\"", "<", ">", "[", "]", "{",
        "}", "$", "%", "&", "#", "@", "~", "^", "|", ";",
    )

    fun cornersFor(key: String, upperCase: Boolean = false): Array<String?> {
        val k = key.lowercase()
        val out = arrayOfNulls<String>(4) // TL, TR, BR, BL
        val acc = accents[k]?.let { a -> if (upperCase) a.map { it.uppercase() } else a }.orEmpty()
        val sym = symbols[key].orEmpty()
        val dig = digitHints[k]
        // TR: digit first (top row), else first accent, else first symbol.
        out[1] = dig ?: acc.getOrNull(0) ?: sym.getOrNull(0) ?: pool[0]
        // TL: second accent, else second symbol, else pool.
        out[0] = acc.getOrNull(1) ?: sym.getOrNull(1) ?: pool[1]
        // BR: first symbol, else third accent, else pool.
        out[2] = sym.getOrNull(0) ?: acc.getOrNull(2) ?: pool[2]
        // BL: second symbol, else fourth accent, else pool.
        out[3] = sym.getOrNull(1) ?: acc.getOrNull(3) ?: pool[3]
        // Never repeat a character across corners.
        for (i in out.indices) {
            val c = out[i] ?: continue
            for (j in 0 until i) if (out[j] == c) { out[i] = null; break }
        }
        // A corner left empty by the dedupe borrows from the pool.
        for (i in out.indices) {
            if (out[i] == null) out[i] = pool.firstOrNull { p -> out.none { it == p } }
        }
        return out
    }

    /** Key par dikhane wala chhota hint (null = koi hint nahi) */
    fun hintFor(key: String): String? {
        val k = key.lowercase()
        digitHints[k]?.let { return it }
        // Letters ke alawa baaki keys par pehla alternate hint bane
        if (k.length == 1 && k[0].isLetter()) return null
        return symbols[key]?.firstOrNull()
    }

    /**
     * Long press popup ke options.
     * Order: [digit (agar top row), accents..., symbols...]
     * Pehla option default select hota hai (long press karke bina hile chhodo to
     * wahi type hoga).
     */
    fun alternatesFor(key: String, upperCase: Boolean = false): List<String> {
        val k = key.lowercase()
        val list = mutableListOf<String>()
        digitHints[k]?.let { list += it }                 // digit case-independent
        accents[k]?.let { a -> list += if (upperCase) a.map { it.uppercase() } else a }
        symbols[key]?.let { list += it }
        return list
    }

    /**
     * Agar tum keys ko khud Canvas par draw karte ho (custom View), to hint aise
     * draw karo: KeyHints.drawHint(canvas, keyRect, hint, hintPaint)
     */
    fun drawHint(canvas: Canvas, keyRect: RectF, hint: String, paint: Paint) {
        paint.textSize = keyRect.height() * 0.26f
        paint.alpha = 150
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            hint,
            keyRect.right - keyRect.width() * 0.10f,
            keyRect.top + paint.textSize * 1.15f,
            paint
        )
    }
}

/**
 * Long-press popup controller. Ek key View par attach karo:
 *
 * val popup = LongPressPopup(context) { text -> currentInputConnection.commitText(text, 1) }
 *
 * popup.attach(
 *     key = keyView,
 *     primary = { if (shift) "Q" else "q" },
 *     alternates = { KeyHints.alternatesFor("q", upperCase = shift) }
 * )
 *
 * // Emoji skin tone:
 * popup.attach(emojiCell, primary = { e.char }, alternates = { e.variants })
 *
 * Agar tumhari key par pehle se onClick/onTouch hai to usse hata do — ye
 * controller dono (tap + long press) sambhal leta hai.
 */
class LongPressPopup(
    private val context: Context,
    private val onCommit: (String) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var window: PopupWindow? = null
    private var cells: List<TextView> = emptyList()
    private var items: List<String> = emptyList()
    private var selected = 0
    private var shown = false

    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), context.resources.displayMetrics
    ).toInt()

    @SuppressLint("ClickableViewAccessibility")
    fun attach(
        key: View,
        primary: () -> String,
        alternates: () -> List<String>,
        onTap: (String) -> Unit = onCommit      // sirf tap par kya ho (default: commit)
    ) {
        key.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    shown = false
                    v.isPressed = true
                    // Long-press timeout ke baad popup dikhao
                    handler.postDelayed(
                        { show(v, alternates()) },
                        ViewConfiguration.getLongPressTimeout().toLong()
                    )
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (shown) updateSelection(v, e.rawX, e.rawY)   // slide-to-select
                    true
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacksAndMessages(null)
                    v.isPressed = false
                    if (shown) {
                        items.getOrNull(selected)?.let(onCommit)   // jis par chhoda wahi
                        dismiss()
                    } else {
                        onTap(primary())                           // normal tap
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacksAndMessages(null)
                    v.isPressed = false
                    dismiss()
                    true
                }
                else -> false
            }
        }
    }

    // ---------------------------------------------------------------
    //  Popup banana aur dikhana
    // ---------------------------------------------------------------
    private fun show(anchor: View, alternates: List<String>) {
        if (alternates.isEmpty()) return    // koi option nahi -> long press normal tap
        items = alternates
        selected = 0
        shown = true
        anchor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

        val perRow = 6                      // 6 se zyada ho to nayi row
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), dp(6), dp(6), dp(6))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.parseColor("#F2303134"))
            }
        }

        val built = ArrayList<TextView>()
        items.chunked(perRow).forEach { chunk ->
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            chunk.forEach { s ->
                val tv = TextView(context).apply {
                    text = s
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    minWidth = dp(44)
                    minHeight = dp(48)
                }
                built += tv
                row.addView(tv)
            }
            root.addView(row)
        }
        cells = built
        highlight()

        root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val w = root.measuredWidth
        val h = root.measuredHeight

        // Key ke upar centered, par screen ke andar clamp
        val loc = IntArray(2); anchor.getLocationOnScreen(loc)
        val screenW = context.resources.displayMetrics.widthPixels
        val x = (loc[0] + anchor.width / 2 - w / 2).coerceIn(dp(4), screenW - w - dp(4))
        val y = (loc[1] - h - dp(8)).coerceAtLeast(0)

        window = PopupWindow(root, w, h, false).apply {
            isTouchable = false             // touches key View ke paas hi rahein
            isClippingEnabled = false
            elevation = dp(8).toFloat()
            showAtLocation(anchor.rootView, Gravity.NO_GRAVITY, x, y)
        }
    }

    // Ungli ke sabse paas wala cell select karo
    private fun updateSelection(anchor: View, rawX: Float, rawY: Float) {
        var best = selected
        var bestDist = Float.MAX_VALUE
        val loc = IntArray(2)
        cells.forEachIndexed { i, tv ->
            tv.getLocationOnScreen(loc)
            val cx = loc[0] + tv.width / 2f
            val cy = loc[1] + tv.height / 2f
            val d = hypot(rawX - cx, rawY - cy)
            if (d < bestDist) { bestDist = d; best = i }
        }
        if (best != selected) {
            selected = best
            highlight()
            anchor.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)   // tick
        }
    }

    private fun highlight() {
        cells.forEachIndexed { i, tv ->
            tv.background = if (i == selected) GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor("#FF4C8DF6"))
            } else null
        }
    }

    private fun dismiss() {
        window?.dismiss()
        window = null
        shown = false
    }
}
