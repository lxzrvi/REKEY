package io.github.lxzrvi.rekey.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Process-wide decode cache.
 *
 * A preset card is re-hosted three times a second — stack copy, drag overlay,
 * dismissal flight, editor travel — and decoding the same photo again for each
 * host made the card flash its background a frame late on every tap. One small
 * LRU keyed by uri+size keeps every host pixel-identical on the first frame.
 */
private val imageCache = object : LruCache<String, ImageBitmap>(24)

/** Decoded background, downsampled so a 12 MP photo never lands on the UI thread. */
private suspend fun decode(context: Context, uri: String, maxPx: Int): ImageBitmap? =
    withContext(Dispatchers.IO) {
        val key = "$uri@$maxPx"
        imageCache.get(key)?.let { return@withContext it }
        runCatching {
            val parsed = Uri.parse(uri)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(parsed)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            while (longest / (sample * 2) >= maxPx) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(parsed)?.use {
                BitmapFactory.decodeStream(it, null, opts)?.asImageBitmap()
            }
        }.getOrNull()?.also { imageCache.put(key, it) }
    }

/**
 * Loads a persisted content URI off the main thread.
 *
 * Returns null while loading and on failure, so callers can fall back to the
 * flat colour instead of showing an empty board. Hits are synchronous.
 */
@Composable
fun rememberUriImage(uri: String?, maxPx: Int = 1080): ImageBitmap? {
    val context = LocalContext.current
    val key = if (uri.isNullOrBlank()) null else "$uri@$maxPx"
    val cached = if (key == null) null else imageCache.get(key)
    return produceState<ImageBitmap?>(cached, key) {
        value = if (key == null) null else decode(context, uri!!, maxPx)
    }.value
}
