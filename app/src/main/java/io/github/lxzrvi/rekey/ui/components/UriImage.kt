package io.github.lxzrvi.rekey.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decoded background, downsampled so a 12 MP photo never lands on the UI thread. */
private suspend fun decode(context: Context, uri: String, maxPx: Int): ImageBitmap? =
    withContext(Dispatchers.IO) {
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
        }.getOrNull()
    }

/**
 * Loads a persisted content URI off the main thread.
 *
 * Returns null while loading and on failure, so callers can fall back to the
 * flat colour instead of showing an empty board.
 */
@Composable
fun rememberUriImage(uri: String?, maxPx: Int = 1080): ImageBitmap? {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(null, uri, maxPx) {
        value = if (uri.isNullOrBlank()) null else decode(context, uri, maxPx)
    }.value
}
