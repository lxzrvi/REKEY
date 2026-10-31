package io.github.lxzrvi.rekey.ime

import android.content.Context
import io.github.lxzrvi.rekey.R
import java.util.Locale
import kotlin.collections.ArrayList
import kotlin.math.max

/**
 * Dictionary-backed suggestions.
 *
 * Three sources, in priority order:
 *  1. words the owner has actually typed (learned counts, persisted),
 *  2. a frequency-ranked 8,000-word English list, prefix-matched by binary search,
 *  3. a small bigram table for the next word when there is no prefix yet.
 *
 * This is a real predictor, not random words. It is not an autocorrect engine:
 * it never rewrites what was typed, it only offers completions.
 */
class WordPredictor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("rekey_words", Context.MODE_PRIVATE)

    /** Rank order is frequency order, so a lower index is a better suggestion. */
    private val dictionary: List<String> by lazy {
        runCatching {
            appContext.resources.openRawResource(R.raw.words_en).bufferedReader().useLines { lines ->
                lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
            }
        }.getOrDefault(emptyList())
    }

    /** Sorted once so a prefix is a binary search plus a short walk. */
    private val sorted: List<String> by lazy { dictionary.sorted() }
    private val rank: Map<String, Int> by lazy {
        dictionary.withIndex().associate { (i, w) -> w to i }
    }

    private val learned: MutableMap<String, Int> by lazy {
        val raw = prefs.getString("counts", "") ?: ""
        val m = HashMap<String, Int>()
        raw.split('\n').forEach { line ->
            val i = line.lastIndexOf('=')
            if (i > 0) {
                val w = line.substring(0, i)
                val c = line.substring(i + 1).toIntOrNull() ?: 0
                if (w.isNotBlank() && c > 0) m[w] = c
            }
        }
        m
    }

    private var dirty = 0

    /** Remember a finished word so the owner's vocabulary rises to the top. */
    fun learn(word: String) {
        val w = word.lowercase(Locale.ROOT).trim()
        if (w.length < 2 || w.length > 20 || !w.all { it.isLetter() }) return
        learned[w] = (learned[w] ?: 0) + 1
        dirty += 1
        if (dirty >= 12) flush()
    }

    fun flush() {
        if (dirty == 0) return
        dirty = 0
        // Keep the store bounded; the least used words are the first to go.
        val trimmed = if (learned.size > 3000) {
            learned.entries.sortedByDescending { it.value }.take(2000)
                .associate { it.key to it.value }
        } else learned.toMap()
        val raw = trimmed.entries.joinToString("\n") { "${it.key}=${it.value}" }
        prefs.edit().putString("counts", raw).apply()
    }

    /**
     * @param prefix the word currently being typed, possibly empty
     * @param previous the word before it, used for next-word guesses
     */
    fun suggest(prefix: String, previous: String?, count: Int): List<String> {
        val p = prefix.lowercase(Locale.ROOT)
        val out = ArrayList<String>(count)

        if (p.isNotEmpty()) {
            // Learned words first: they are the ones this owner actually uses.
            learned.entries.asSequence()
                .filter { it.key.startsWith(p) && it.key != p }
                .sortedByDescending { it.value }
                .take(count)
                .forEach { out.add(it.key) }

            if (out.size < count) {
                val list = sorted
                var lo = 0
                var hi = list.size
                while (lo < hi) {
                    val mid = (lo + hi) ushr 1
                    if (list[mid] < p) lo = mid + 1 else hi = mid
                }
                val hits = ArrayList<String>(64)
                var i = lo
                while (i < list.size && list[i].startsWith(p) && hits.size < 200) {
                    if (list[i] != p) hits.add(list[i])
                    i += 1
                }
                hits.sortWith(compareBy({ rank[it] ?: Int.MAX_VALUE }, { it.length }))
                hits.forEach { if (out.size < count && it !in out) out.add(it) }
            }
            // If the dictionary had nothing, at least offer the typed word.
            if (out.isEmpty()) out.add(p)
            return out.take(count)
        }

        // No prefix: guess the next word.
        val prev = previous?.lowercase(Locale.ROOT).orEmpty()
        Bigrams[prev]?.forEach { if (out.size < count && it !in out) out.add(it) }
        learned.entries.asSequence().sortedByDescending { it.value }.forEach {
            if (out.size < count && it.key !in out) out.add(it.key)
        }
        Starters.forEach { if (out.size < count && it !in out) out.add(it) }
        return out.take(max(1, count))
    }

    companion object {
        private val Starters = listOf("I", "the", "you", "and", "thanks", "okay", "what", "how")

        /** Deliberately small and hand-picked; enough to feel predictive. */
        private val Bigrams: Map<String, List<String>> = mapOf(
            "i" to listOf("am", "will", "have", "think", "don't"),
            "you" to listOf("are", "can", "have", "want"),
            "thank" to listOf("you"),
            "thanks" to listOf("for", "a"),
            "how" to listOf("are", "to", "much"),
            "what" to listOf("is", "are", "time"),
            "good" to listOf("morning", "night", "to"),
            "see" to listOf("you", "the"),
            "let" to listOf("me", "us"),
            "in" to listOf("the", "a"),
            "on" to listOf("the", "my"),
            "at" to listOf("the", "least"),
            "of" to listOf("the", "a"),
            "to" to listOf("the", "be", "do"),
            "is" to listOf("a", "the", "not"),
            "are" to listOf("you", "the"),
            "can" to listOf("you", "i"),
            "will" to listOf("be", "you"),
            "do" to listOf("you", "not"),
            "have" to listOf("a", "to"),
            "my" to listOf("name", "friend"),
            "the" to listOf("same", "best", "first"),
            "a" to listOf("lot", "few", "good"),
            "please" to listOf("let", "send"),
            "happy" to listOf("birthday", "to"),
        )
    }
}
