package io.github.lxzrvi.rekey.ime

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.PriorityQueue
import kotlin.math.ln

enum class SuggestionType { LITERAL, COMPLETION, CORRECTION, NEXT_WORD, EMOJI }

data class Suggestion(val text: String, val type: SuggestionType, val score: Double)

/**
 * Offline suggestion engine.
 *
 * Prefix completion and fuzzy autocorrect both run on one trie. Each node caches
 * the largest frequency in its subtree, so completion is a best-first search
 * that never has to walk a whole subtree, and autocorrect prunes any branch
 * whose Levenshtein row already exceeds the distance budget.
 *
 * The user's own typing is folded back into the same trie and into a bigram
 * table, then persisted to filesDir. Nothing leaves the device.
 */
class SuggestionEngine(private val context: Context) {

    private class Node {
        val kids = HashMap<Char, Node>()
        var freq = 0
        var isWord = false
        var best = 0
    }

    private val root = Node()
    private val lock = Any()
    private val userUnigram = HashMap<String, Int>()
    private val bigram = HashMap<String, HashMap<String, Int>>()

    @Volatile
    private var ready = false

    val isReady: Boolean get() = ready

    private val userFile get() = File(context.filesDir, "user_lm.tsv")

    suspend fun init(dictAsset: String = "words.txt") = withContext(Dispatchers.IO) {
        if (ready) return@withContext
        synchronized(lock) {
            runCatching {
                context.assets.open(dictAsset).bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val t = line.trim().split(' ', '\t')
                        val w = t.firstOrNull()?.lowercase().orEmpty()
                        if (w.isNotEmpty()) {
                            val f = t.getOrNull(1)?.toLongOrNull()
                                ?.coerceAtMost(1_000_000_000L)?.toInt() ?: 1
                            insert(w, f, add = false)
                        }
                    }
                }
            }
            loadUserModel()
        }
        ready = true
    }

    private fun insert(word: String, freq: Int, add: Boolean) {
        var n = root
        for (c in word) n = n.kids.getOrPut(c) { Node() }
        n.isWord = true
        n.freq = if (add) n.freq + freq else maxOf(n.freq, freq)
        var m = root
        m.best = maxOf(m.best, n.freq)
        for (c in word) {
            m = m.kids[c] ?: return
            m.best = maxOf(m.best, n.freq)
        }
    }

    suspend fun suggest(prevWord: String?, typed: String, limit: Int = 3): List<Suggestion> =
        withContext(Dispatchers.Default) {
            if (!ready) return@withContext emptyList()
            val prev = prevWord?.lowercase()
            val t = typed.lowercase()
            val out = LinkedHashMap<String, Suggestion>()

            fun add(s: Suggestion) {
                val k = s.text.lowercase()
                val old = out[k]
                if (old == null || s.score > old.score) out[k] = s
            }

            var emoji: String? = null

            synchronized(lock) {
                if (t.isEmpty()) {
                    bigram[prev]?.entries?.sortedByDescending { it.value }
                        ?.take(limit * 2)?.forEach {
                            add(Suggestion(it.key, SuggestionType.NEXT_WORD, 10 + ln(it.value + 1.0)))
                        }
                    if (out.isEmpty()) completions("", limit).forEach {
                        add(Suggestion(it.first, SuggestionType.NEXT_WORD, ln(it.second + 1.0)))
                    }
                    // "i love " -> the heart arrives before you finish the word.
                    prev?.let { p -> EmojiRepository.emojiForWord(p) }?.let {
                        emoji = it
                    }
                } else {
                    val isKnown = find(t)?.isWord == true

                    completions(t, limit * 4).forEach { (w, f) ->
                        add(Suggestion(w, SuggestionType.COMPLETION, ln(f + 1.0) + bigramBoost(prev, w)))
                    }

                    if (!isKnown && t.length >= 3) {
                        val maxDist = if (t.length <= 4) 1 else 2
                        fuzzy(t, maxDist).forEach { (w, f, d) ->
                            add(Suggestion(w, SuggestionType.CORRECTION, ln(f + 1.0) - 3.0 * d + bigramBoost(prev, w)))
                        }
                    }

                    // Exactly what the user typed always leads the strip, with
                    // its own casing: known word or not, that is the word they
                    // meant to commit.
                    add(Suggestion(typed, SuggestionType.LITERAL, Double.MAX_VALUE))

                    if (t.length >= 3) emoji = EmojiRepository.emojiForWord(t)
                }
            }

            val words = out.values.sortedByDescending { it.score }
                .map { it.copy(text = applyCase(typed, it)) }
            val room = if (emoji != null) (limit - 1).coerceAtLeast(1) else limit
            val result = words.take(room).toMutableList()
            emoji?.let { result += Suggestion(it, SuggestionType.EMOJI, 0.0) }
            result
        }

    private fun applyCase(typed: String, s: Suggestion): String {
        if (s.type == SuggestionType.LITERAL || s.type == SuggestionType.EMOJI || typed.isEmpty()) {
            return s.text
        }
        return when {
            typed.length > 1 && typed.all { it.isUpperCase() } -> s.text.uppercase()
            typed[0].isUpperCase() -> s.text.replaceFirstChar { it.uppercase() }
            else -> s.text
        }
    }

    private fun bigramBoost(prev: String?, word: String): Double {
        val c = bigram[prev]?.get(word) ?: return 0.0
        return 2.0 * ln(1.0 + c)
    }

    private fun find(s: String): Node? {
        var n = root
        for (c in s) n = n.kids[c] ?: return null
        return n
    }

    private class Entry(val score: Int, val node: Node, val text: String, val isWord: Boolean)

    private fun completions(prefix: String, k: Int): List<Pair<String, Int>> {
        val start = find(prefix) ?: return emptyList()
        val pq = PriorityQueue<Entry>(compareByDescending { it.score })
        pq.add(Entry(start.best, start, prefix, false))
        val res = ArrayList<Pair<String, Int>>()
        while (pq.isNotEmpty() && res.size < k) {
            val e = pq.poll() ?: break
            if (e.isWord) {
                res += e.text to e.score
                continue
            }
            if (e.node.isWord) pq.add(Entry(e.node.freq, e.node, e.text, true))
            for ((c, child) in e.node.kids) pq.add(Entry(child.best, child, e.text + c, false))
        }
        return res
    }

    private fun fuzzy(word: String, maxDist: Int): List<Triple<String, Int, Int>> {
        val out = ArrayList<Triple<String, Int, Int>>()
        val first = IntArray(word.length + 1) { it }
        walk(root, StringBuilder(), first, word, maxDist, out)
        return out.sortedWith(
            compareBy<Triple<String, Int, Int>> { it.third }.thenByDescending { it.second }
        ).take(8)
    }

    private fun walk(
        node: Node, text: StringBuilder, prevRow: IntArray,
        word: String, maxDist: Int, out: MutableList<Triple<String, Int, Int>>,
    ) {
        for ((c, child) in node.kids) {
            val row = IntArray(word.length + 1)
            row[0] = prevRow[0] + 1
            var minV = row[0]
            for (i in 1..word.length) {
                val cost = if (word[i - 1] == c) 0 else 1
                row[i] = minOf(row[i - 1] + 1, prevRow[i] + 1, prevRow[i - 1] + cost)
                if (row[i] < minV) minV = row[i]
            }
            text.append(c)
            if (child.isWord && row[word.length] <= maxDist) {
                out += Triple(text.toString(), child.freq, row[word.length])
            }
            if (minV <= maxDist) walk(child, text, row, word, maxDist, out)
            text.setLength(text.length - 1)
        }
    }

    fun learn(prevWord: String?, word: String) {
        val w = word.lowercase()
        if (w.length < 2 || !w.all { it.isLetter() || it == '\'' }) return
        synchronized(lock) {
            userUnigram[w] = (userUnigram[w] ?: 0) + 1
            insert(w, 30, add = true)
            if (!prevWord.isNullOrBlank()) {
                bigram.getOrPut(prevWord.lowercase()) { HashMap() }.merge(w, 1, Int::plus)
            }
        }
    }

    suspend fun flush() = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        synchronized(lock) {
            userUnigram.forEach { (w, c) -> sb.append("U\t").append(w).append('\t').append(c).append('\n') }
            bigram.forEach { (p, m) ->
                m.forEach { (n, c) -> sb.append("B\t").append(p).append('\t').append(n).append('\t').append(c).append('\n') }
            }
        }
        runCatching { userFile.writeText(sb.toString()) }
        Unit
    }

    private fun loadUserModel() {
        val f = userFile
        if (!f.exists()) return
        runCatching {
            f.forEachLine { line ->
                val p = line.split('\t')
                when {
                    p.size == 3 && p[0] == "U" -> {
                        val c = p[2].toIntOrNull() ?: return@forEachLine
                        userUnigram[p[1]] = c
                        insert(p[1], 30 * c, add = true)
                    }
                    p.size == 4 && p[0] == "B" -> {
                        val c = p[3].toIntOrNull() ?: return@forEachLine
                        bigram.getOrPut(p[1]) { HashMap() }[p[2]] = c
                    }
                }
            }
        }
    }

    suspend fun resetLearning() = withContext(Dispatchers.IO) {
        synchronized(lock) { userUnigram.clear(); bigram.clear() }
        runCatching { userFile.delete() }
        Unit
    }
}
