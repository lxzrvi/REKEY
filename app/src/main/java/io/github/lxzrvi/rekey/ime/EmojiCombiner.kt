// Package must become io.github.lxzrvi.rekey.ime (or .emoji) when adopted.
// HTML entities and markdown fences from the paste have been cleaned up here.
package io.github.lxzrvi.rekey.ime

import android.content.ClipDescription
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import java.io.File

/**
 * ============================================================
 * EMOJI COMBINER  (do emoji jodo — poora offline)
 * ============================================================
 * Combine karne ke 2 tareeke (order me try hote hain):
 *
 * 1. ZWJ SEQUENCE (asli Unicode combine)
 *    emoji1 + U+200D + emoji2 — agar Unicode me ye valid hai to ek hi emoji ban
 *    jata hai jo har app me text ki tarah chalta hai.
 *    e.g. ❤️ + 🔥 = ❤️‍🔥 , 🐻 + ❄️ = 🐻‍❄️ , 👨 + 🚀 = 👨‍🚀
 *
 * 2. MASHUP IMAGE (agar ZWJ valid nahi)
 *    Dono emoji ko canvas par draw karke ek PNG sticker banate hain
 *    (SPLIT = aadha-aadha, BADGE = chhota corner me, OVERLAP = halka blend,
 *     SIDE = side by side) aur app me image ki tarah bhejte hain.
 *    Agar wo app image nahi leti to text "emoji1emoji2" jata hai.
 *
 * NOTE: Google ka "Emoji Kitchen" jaisa artwork Google ki proprietary images
 * hain, wo offline/bundle nahi kiye ja sakte. Ye approach 100% offline aur
 * license-safe hai.
 *
 * ------------------- ONE-TIME SETUP (image bhejne ke liye) -------------------
 * build.gradle:   implementation("androidx.core:core-ktx:1.13.1")
 *
 * AndroidManifest.xml (<application> ke andar):
 * <provider
 *     android:name="androidx.core.content.FileProvider"
 *     android:authorities="${applicationId}.fileprovider"
 *     android:exported="false"
 *     android:grantUriPermissions="true">
 *     <meta-data android:name="android.support.FILE_PROVIDER_PATHS"
 *                android:resource="@xml/file_paths" />
 * </provider>
 *
 * res/xml/file_paths.xml:
 * <paths><cache-path name="mashups" path="mashups/" /></paths>
 */

enum class MashupStyle { SPLIT, BADGE, OVERLAP, SIDE }

sealed class CombineResult {
    /** Asli Unicode combined emoji (text) */
    data class Zwj(val text: String) : CombineResult()

    /** Image sticker + fallback text */
    data class Mashup(val bitmap: Bitmap, val fallbackText: String) : CombineResult()
}

class EmojiCombiner(private val context: Context) {

    private val ZWJ = "\u200D"
    private val VS16 = "\uFE0F"

    // FE0F hata kar lookup (stripped -> original sequence), taaki "❤" aur "❤️"
    // dono match hon
    private val strippedLookup: Map<String, String> by lazy {
        EmojiRepository.knownSequences.associateBy { it.replace(VS16, "") }
    }

    /** Main function: a aur b ko jodo */
    fun combine(a: String, b: String, style: MashupStyle = MashupStyle.SPLIT): CombineResult {
        // Step 1: dono order me ZWJ try karo
        for ((x, y) in listOf(a to b, b to a)) {
            val candidate = (x + ZWJ + y).replace(VS16, "")
            strippedLookup[candidate]?.let { return CombineResult.Zwj(it) }
        }
        // Step 2: ZWJ nahi mila -> image mashup
        return CombineResult.Mashup(drawMashup(a, b, style), a + b)
    }

    /** UI me "kya hua" dikhane ke liye ek line (preview ke neeche label) */
    fun describe(r: CombineResult): String = when (r) {
        is CombineResult.Zwj -> "✓ Valid Unicode combo (text ke roop me chalega)"
        is CombineResult.Mashup -> "🖼 Mashup sticker (image ke roop me jayega)"
    }

    // ---------------------------------------------------------------
    //  Mashup drawing
    // ---------------------------------------------------------------
    private fun drawMashup(a: String, b: String, style: MashupStyle): Bitmap {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = size * 0.78f
        }
        val full = size.toFloat()
        val half = full / 2f

        // Vertical center me text draw karne ke liye baseline
        fun baseline(paint: Paint, centerY: Float) =
            centerY - (paint.descent() + paint.ascent()) / 2f

        when (style) {
            MashupStyle.SPLIT -> {
                // Left half = a, right half = b
                c.save(); c.clipRect(0f, 0f, half, full)
                c.drawText(a, half, baseline(p, half), p); c.restore()
                c.save(); c.clipRect(half, 0f, full, full)
                c.drawText(b, half, baseline(p, half), p); c.restore()
            }
            MashupStyle.BADGE -> {
                // a bada, b chhota bottom-right corner me
                c.drawText(a, half, baseline(p, half), p)
                val small = Paint(p).apply { textSize = size * 0.42f }
                c.drawText(b, full * 0.72f, baseline(small, full * 0.74f), small)
            }
            MashupStyle.OVERLAP -> {
                // a poora, uske upar b halka transparent
                c.drawText(a, half, baseline(p, half), p)
                val ghost = Paint(p).apply { alpha = 150 }
                c.drawText(b, half, baseline(ghost, half), ghost)
            }
            MashupStyle.SIDE -> {
                // Side by side, thoda sa overlap + halka tilt
                val s = Paint(p).apply { textSize = size * 0.56f }
                c.save(); c.rotate(-8f, full * 0.30f, half)
                c.drawText(a, full * 0.30f, baseline(s, half), s); c.restore()
                c.save(); c.rotate(8f, full * 0.70f, half)
                c.drawText(b, full * 0.70f, baseline(s, half), s); c.restore()
            }
        }
        return bmp
    }

    // ---------------------------------------------------------------
    //  Text field me bhejna
    // ---------------------------------------------------------------
    /**
     * InputMethodService me use:
     *   val r = combiner.combine(a, b)
     *   combiner.send(r, currentInputConnection, currentInputEditorInfo)
     */
    fun send(r: CombineResult, ic: InputConnection, info: EditorInfo) {
        when (r) {
            is CombineResult.Zwj -> ic.commitText(r.text, 1)
            is CombineResult.Mashup -> if (!commitImage(ic, info, r.bitmap)) {
                ic.commitText(r.fallbackText, 1)   // app image nahi leti -> text fallback
            }
        }
    }

    private fun commitImage(ic: InputConnection, info: EditorInfo, bmp: Bitmap): Boolean {
        // Check: kya ye text field PNG image accept karta hai?
        val accepts = EditorInfoCompat.getContentMimeTypes(info)
            .any { ClipDescription.compareMimeTypes(it, "image/png") }
        if (!accepts) return false

        // PNG ko cache folder me save karo
        val dir = File(context.cacheDir, "mashups").apply { mkdirs() }
        dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 60 * 60 * 1000 }
            ?.forEach { it.delete() }                              // purani files saaf
        val file = File(dir, "emoji_${System.currentTimeMillis()}.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val desc = ClipDescription("emoji mashup", arrayOf("image/png"))
        val content = InputContentInfoCompat(uri, desc, null)

        var flags = 0
        if (Build.VERSION.SDK_INT >= 25) {
            flags = flags or InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
        }
        return InputConnectionCompat.commitContent(ic, info, content, flags, null)
    }
}

/**
 * ============================================================
 * COMBINE SESSION  (UI flow: "do emoji chuno -> preview -> send")
 * ============================================================
 * Emoji grid me user ke tap ko session.pick(emoji) se jodo.
 * 1st tap -> slot A,  2nd tap -> slot B -> turant combine hoke preview callback.
 * 3rd tap -> naya round shuru (A reset).
 *
 * val session = CombineSession(combiner) { a, b, result ->
 *     previewTextA.text = a ?: "?"
 *     previewTextB.text = b ?: "?"
 *     when (result) {
 *         is CombineResult.Zwj    -> previewText.text = result.text
 *         is CombineResult.Mashup -> previewImage.setImageBitmap(result.bitmap)
 *         null -> Unit
 *     }
 *     statusLabel.text = result?.let { combiner.describe(it) } ?: "Dusra emoji chuno"
 * }
 * sendButton.setOnClickListener { session.send(currentInputConnection, currentInputEditorInfo) }
 */
class CombineSession(
    private val combiner: EmojiCombiner,
    var style: MashupStyle = MashupStyle.SPLIT,
    private val onUpdate: (a: String?, b: String?, result: CombineResult?) -> Unit
) {
    private var a: String? = null
    private var b: String? = null
    private var result: CombineResult? = null

    fun pick(emoji: String) {
        if (a == null || (a != null && b != null)) {   // naya round
            a = emoji; b = null; result = null
        } else {
            b = emoji
            result = combiner.combine(a!!, b!!, style)
        }
        onUpdate(a, b, result)
    }

    /** Style badalne par (SPLIT/BADGE/...) preview dobara banao */
    fun changeStyle(newStyle: MashupStyle) {
        style = newStyle
        if (a != null && b != null) {
            result = combiner.combine(a!!, b!!, style)
            onUpdate(a, b, result)
        }
    }

    fun send(ic: InputConnection, info: EditorInfo) {
        result?.let { combiner.send(it, ic, info) }
        a = null; b = null; result = null
        onUpdate(null, null, null)
    }
}
