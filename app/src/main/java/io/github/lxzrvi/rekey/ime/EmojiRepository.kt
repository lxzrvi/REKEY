package io.github.lxzrvi.rekey.ime

import android.content.Context
import android.os.Build
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/** One emoji plus the skin-tone variants that belong to it. */
data class EmojiEntry(val char: String, val name: String, val variants: List<String>)

data class EmojiCategory(val title: String, val items: List<EmojiEntry>)

/**
 * The Unicode emoji-test.txt file, parsed once.
 *
 * Everything the keyboard knows about emoji comes from here: the category grid,
 * the skin-tone alternates for the long-press popup, the keyword search, and the
 * list of real ZWJ sequences that [EmojiCombiner] checks before it falls back to
 * drawing a sticker.
 *
 * Only `fully-qualified` entries are kept. The `component` group (bare skin-tone
 * modifiers, hair components) is skipped because those are not emoji on their
 * own — they exist to be glued onto something else.
 */
object EmojiRepository {

    private const val ZWJ = '\u200D'
    private val SKIN = setOf(0x1F3FB, 0x1F3FC, 0x1F3FD, 0x1F3FE, 0x1F3FF)

    @Volatile
    private var loaded = false

    /** Categories in Unicode's own order, minus the component group. */
    @Volatile
    var categories: List<EmojiCategory> = emptyList()
        private set

    /** Real ZWJ sequences, e.g. heart + fire. Used by the combiner. */
    @Volatile
    var knownSequences: List<String> = emptyList()
        private set

    private var byBase: Map<String, EmojiEntry> = emptyMap()
    private var byWord: Map<String, String> = emptyMap()
    private var appContext: Context? = null

    /**
     * Glyphs newer than the device's font render as tofu boxes. The parser
     * reads the E-version from emoji-test.txt and drops anything the current
     * Android release cannot draw, so the grid only ever shows real emoji.
     */
    private val maxEmojiVersion: Float = when (Build.VERSION.SDK_INT) {
        31, 32 -> 13.0f
        33 -> 14.0f
        34 -> 15.0f
        35 -> 16.0f
        else -> 99f
    }

    /** Most-recently-used emoji, newest first. Survives IME restarts. */
    private val recentState = mutableStateListOf<String>()
    val recent: List<String> get() = recentState

    private const val RECENT_CAP = 36

    fun pushRecent(emoji: String) {
        if (emoji.isBlank()) return
        recentState.remove(emoji)
        recentState.add(0, emoji)
        while (recentState.size > RECENT_CAP) recentState.removeAt(recentState.lastIndex)
        runCatching {
            appContext?.getSharedPreferences("rekey_emoji", Context.MODE_PRIVATE)
                ?.edit()?.putString("recent", JSONArray(recentState).toString())?.apply()
        }
    }

    /** Idempotent. Safe to call from both the app and the IME. */
    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        if (loaded) return@withContext
        appContext = context.applicationContext
        runCatching {
            context.getSharedPreferences("rekey_emoji", Context.MODE_PRIVATE)
                .getString("recent", null)?.let { raw ->
                    val arr = JSONArray(raw)
                    for (i in 0 until arr.length()) {
                        val e = arr.optString(i, "")
                        if (e.isNotBlank()) recentState.add(e)
                    }
                }
        }
        val groups = LinkedHashMap<String, MutableList<EmojiEntry>>()
        val zwj = ArrayList<String>()
        // base sequence -> its skin-toned siblings, in tone order
        val tones = LinkedHashMap<String, MutableList<String>>()
        var group = "Symbols"

        runCatching {
            context.assets.open("emoji-test.txt").bufferedReader().useLines { lines ->
                for (raw in lines) {
                    val line = raw.trim()
                    if (line.startsWith("# group:")) {
                        group = line.removePrefix("# group:").trim()
                        continue
                    }
                    if (line.isEmpty() || line.startsWith("#")) continue
                    val semi = line.indexOf(';')
                    val hash = line.indexOf('#', semi + 1)
                    if (semi < 0 || hash < 0) continue
                    val status = line.substring(semi + 1, hash).trim()
                    if (status != "fully-qualified") continue
                    if (group == "Component") continue

                    val cps = line.substring(0, semi).trim().split(' ', '\t')
                        .mapNotNull { it.toIntOrNull(16) }
                    if (cps.isEmpty()) continue
                    val text = buildString { cps.forEach { appendCodePoint(it) } }
                    // The comment is "<emoji> E<version> <name>".
                    val comment = line.substring(hash + 1).trim()
                    // "<emoji> E<version> <name>" — skip glyphs this OS font lacks.
                    val afterEmoji = comment.substringAfter(' ')
                    val version = afterEmoji.removePrefix("E").substringBefore(' ').toFloatOrNull() ?: 0f
                    if (version > maxEmojiVersion) continue
                    val name = afterEmoji.substringAfter(' ').trim()

                    if (cps.contains(ZWJ.code)) zwj += text

                    val tone = cps.firstOrNull { it in SKIN }
                    if (tone != null) {
                        val base = buildString {
                            cps.filter { it !in SKIN }.forEach { appendCodePoint(it) }
                        }
                        tones.getOrPut(base) { ArrayList() } += text
                        continue
                    }
                    groups.getOrPut(group) { ArrayList() } += EmojiEntry(text, name, emptyList())
                }
            }
        }

        val resolved = groups.map { (title, items) ->
            EmojiCategory(title, items.map { it.copy(variants = tones[it.char].orEmpty()) })
        }
        val flat = LinkedHashMap<String, EmojiEntry>()
        resolved.forEach { c -> c.items.forEach { flat[it.char] = it } }
        // A tone-only base that never appeared unqualified still deserves a slot.
        tones.forEach { (base, variants) ->
            if (!flat.containsKey(base) && variants.isNotEmpty()) {
                flat[base] = EmojiEntry(base, base, variants)
            }
        }

        categories = resolved
        knownSequences = zwj
        byBase = flat
        byWord = buildWordIndex(resolved)
        loaded = true
    }

    /** True once the asset has been parsed. The UI falls back until then. */
    val isReady: Boolean get() = loaded

    fun variantsFor(emoji: String): List<String> = byBase[emoji]?.variants.orEmpty()

    /**
     * Word -> emoji for the suggestion strip. CLDR names are literal ("red
     * heart"), so everyday words need a hand-written layer on top.
     */
    fun emojiForWord(word: String): String? {
        val w = word.lowercase()
        CURATED[w]?.let { return it }
        return byWord[w]
    }

    /** Name search for the emoji panel. Substring match, shortest names first. */
    fun search(query: String, limit: Int = 60): List<EmojiEntry> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        return byBase.values.asSequence()
            .filter { it.name.contains(q) }
            .sortedBy { it.name.length }
            .take(limit)
            .toList()
    }

    private fun buildWordIndex(cats: List<EmojiCategory>): Map<String, String> {
        val out = HashMap<String, String>()
        cats.forEach { c ->
            c.items.forEach { e ->
                // First word of the name is the strongest signal; whole name
                // also gets a slot so "grinning face" works.
                val words = e.name.lowercase().split(' ', '-', ':', ',')
                    .map { it.trim() }
                    .filter { it.length >= 3 && it.all(Char::isLetter) }
                words.forEach { w -> if (!STOP.contains(w)) out.putIfAbsent(w, e.char) }
            }
        }
        return out
    }

    private val STOP = setOf(
        "with", "and", "the", "for", "face", "man", "woman", "person", "light",
        "medium", "dark", "skin", "tone", "black", "white", "small", "large",
    )

    private val CURATED = mapOf(
        "love" to "\u2764\uFE0F", "pyaar" to "\u2764\uFE0F", "pyar" to "\u2764\uFE0F",
        "heart" to "\u2764\uFE0F", "dil" to "\u2764\uFE0F", "happy" to "\uD83D\uDE0A",
        "khush" to "\uD83D\uDE0A", "smile" to "\uD83D\uDE42", "sad" to "\uD83D\uDE22",
        "udaas" to "\uD83D\uDE22", "cry" to "\uD83D\uDE2D", "rona" to "\uD83D\uDE2D",
        "lol" to "\uD83D\uDE02", "haha" to "\uD83D\uDE02", "hehe" to "\uD83D\uDE04",
        "fire" to "\uD83D\uDD25", "aag" to "\uD83D\uDD25", "lit" to "\uD83D\uDD25",
        "cool" to "\uD83D\uDE0E", "ok" to "\uD83D\uDC4D", "okay" to "\uD83D\uDC4D",
        "theek" to "\uD83D\uDC4D", "thik" to "\uD83D\uDC4D", "yes" to "\uD83D\uDC4D",
        "haan" to "\uD83D\uDC4D", "no" to "\uD83D\uDC4E", "nahi" to "\uD83D\uDC4E",
        "thanks" to "\uD83D\uDE4F", "thank" to "\uD83D\uDE4F", "shukriya" to "\uD83D\uDE4F",
        "please" to "\uD83D\uDE4F", "pray" to "\uD83D\uDE4F", "party" to "\uD83C\uDF89",
        "wow" to "\uD83D\uDE2E", "kiss" to "\uD83D\uDE18", "money" to "\uD83D\uDCB0",
        "paisa" to "\uD83D\uDCB0", "paise" to "\uD83D\uDCB0", "food" to "\uD83C\uDF55",
        "khana" to "\uD83C\uDF55", "chai" to "\uD83C\uDF75", "tea" to "\uD83C\uDF75",
        "coffee" to "\u2615", "cake" to "\uD83C\uDF82", "birthday" to "\uD83C\uDF82",
        "sun" to "\u2600\uFE0F", "rain" to "\uD83C\uDF27\uFE0F", "star" to "\u2B50",
        "music" to "\uD83C\uDFB5", "gaana" to "\uD83C\uDFB5", "song" to "\uD83C\uDFB5",
        "phone" to "\uD83D\uDCF1", "car" to "\uD83D\uDE97", "gaadi" to "\uD83D\uDE97",
        "dog" to "\uD83D\uDC36", "cat" to "\uD83D\uDC31", "eyes" to "\uD83D\uDC40",
        "time" to "\u23F0", "wait" to "\u23F3", "sleep" to "\uD83D\uDE34",
        "soja" to "\uD83D\uDE34", "angry" to "\uD83D\uDE21", "gussa" to "\uD83D\uDE21",
        "think" to "\uD83E\uDD14", "socho" to "\uD83E\uDD14", "hundred" to "\uD83D\uDCAF",
        "india" to "\uD83C\uDDEE\uD83C\uDDF3", "flag" to "\uD83C\uDDEE\uD83C\uDDF3",
        "rocket" to "\uD83D\uDE80", "check" to "\u2705", "done" to "\u2705",
        "cross" to "\u274C", "warning" to "\u26A0\uFE0F", "idea" to "\uD83D\uDCA1",
    )
}
