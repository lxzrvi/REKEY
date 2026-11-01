package io.github.lxzrvi.rekey.ui.editor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.lxzrvi.rekey.ui.components.rememberUriImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/** Ratios the app actually asks an image to fill. */
private data class Ratio(val label: String, val value: Float?)

private val Ratios = listOf(
    Ratio("Free", null),
    Ratio("Board 5:3", 5f / 3f),
    Ratio("Wide 16:9", 16f / 9f),
    Ratio("Key 1:1", 1f),
    Ratio("Photo 4:3", 4f / 3f),
    Ratio("Strip 3:1", 3f),
)

/**
 * Pick an image, then crop it before it is stored.
 *
 * The cropped result is written into filesDir rather than keeping a URI plus a
 * crop rectangle. A content URI can be revoked by the gallery at any time, and
 * re-cropping on every frame of a keyboard would be wasteful; one small PNG is
 * both stabler and cheaper.
 */
@Composable
internal fun ImageRow(
    label: String,
    uri: String?,
    preferredRatio: Float?,
    onChange: (String?) -> Unit,
) {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<Uri?>(null) }
    val thumb = rememberUriImage(uri, 256)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    picked, Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            pending = picked
        }
    }

    Row(
        Modifier.fillMaxWidth().clickable { launcher.launch(arrayOf("image/*")) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            if (uri.isNullOrBlank()) "Choose" else "Change",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(44.dp, 26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (thumb != null) {
                Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text("+", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (!uri.isNullOrBlank()) {
        TextButton(onClick = { onChange(null) }) { Text("Remove image", fontSize = 11.sp) }
    }

    pending?.let { src ->
        CropDialog(
            source = src,
            preferredRatio = preferredRatio,
            onDismiss = { pending = null },
            onCropped = { out -> onChange(out); pending = null },
        )
    }
}

@Composable
private fun CropDialog(
    source: Uri,
    preferredRatio: Float?,
    onDismiss: () -> Unit,
    onCropped: (String) -> Unit,
) {
    val context = LocalContext.current
    val bmp by produceState<Bitmap?>(null, source) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(source)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1600) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                context.contentResolver.openInputStream(source)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
            }.getOrNull()
        }
    }

    var left by remember { mutableFloatStateOf(0.08f) }
    var top by remember { mutableFloatStateOf(0.08f) }
    var right by remember { mutableFloatStateOf(0.92f) }
    var bottom by remember { mutableFloatStateOf(0.92f) }
    var ratio by remember { mutableStateOf(preferredRatio) }
    var saving by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(20.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Crop",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            val image = bmp
            if (image == null) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("Loading…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val imgAspect = image.width.toFloat() / image.height.coerceAtLeast(1)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(imgAspect.coerceIn(0.5f, 2.4f))
                        .clip(RoundedCornerShape(12.dp)),
                ) {
                    Image(
                        image.asImageBitmap(), null, Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds,
                    )
                    // Drag the middle to move, the bottom-right corner to resize.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                drawContent()
                                val l = left * size.width
                                val t = top * size.height
                                val r = right * size.width
                                val b = bottom * size.height
                                // Four bands around the selection. Punching a
                                // hole with BlendMode.Clear would also clear the
                                // sibling Image, because this Box does not own a
                                // separate compositing layer.
                                val shade = Color.Black.copy(alpha = 0.55f)
                                drawRect(shade, Offset.Zero, Size(size.width, t))
                                drawRect(shade, Offset(0f, b), Size(size.width, size.height - b))
                                drawRect(shade, Offset(0f, t), Size(l, b - t))
                                drawRect(shade, Offset(r, t), Size(size.width - r, b - t))
                                drawRect(
                                    Color.White,
                                    topLeft = Offset(l, t),
                                    size = Size(r - l, b - t),
                                    style = Stroke(2f),
                                )
                                drawRoundRect(
                                    Color.White,
                                    topLeft = Offset(r - 22f, b - 22f),
                                    size = Size(22f, 22f),
                                    cornerRadius = CornerRadius(5f, 5f),
                                )
                            }
                            .pointerInput(imgAspect, ratio) {
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    val dx = drag.x / w
                                    val dy = drag.y / h
                                    val nearCorner =
                                        change.position.x > right * w - 44f &&
                                            change.position.y > bottom * h - 44f
                                    if (nearCorner) {
                                        var nw = ((right + dx) - left).coerceIn(0.12f, 1f - left)
                                        var nh = ((bottom + dy) - top).coerceIn(0.12f, 1f - top)
                                        ratio?.let { want ->
                                            // Keep the requested ratio in IMAGE
                                            // pixels, not in view pixels.
                                            val viewToImg = imgAspect
                                            nh = (nw * viewToImg / want).coerceIn(0.12f, 1f - top)
                                            nw = (nh * want / viewToImg).coerceIn(0.12f, 1f - left)
                                        }
                                        right = (left + nw).coerceAtMost(1f)
                                        bottom = (top + nh).coerceAtMost(1f)
                                    } else {
                                        val cw = right - left
                                        val ch = bottom - top
                                        left = (left + dx).coerceIn(0f, 1f - cw)
                                        top = (top + dy).coerceIn(0f, 1f - ch)
                                        right = left + cw
                                        bottom = top + ch
                                    }
                                }
                            },
                    )
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Ratios.forEach { opt ->
                        val on = ratio == opt.value
                        val s = RoundedCornerShape(9.dp)
                        Box(
                            Modifier
                                .clip(s)
                                .background(
                                    if (on) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .clickable { ratio = opt.value }
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                        ) {
                            Text(
                                opt.label,
                                fontSize = 11.sp,
                                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (on) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    left = 0f; top = 0f; right = 1f; bottom = 1f; ratio = null
                }) { Text("Whole image", fontSize = 11.sp) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel", fontSize = 12.sp) }
                TextButton(
                    enabled = bmp != null && !saving,
                    onClick = {
                        val image2 = bmp ?: return@TextButton
                        saving = true
                        val l = (left * image2.width).roundToInt().coerceIn(0, image2.width - 1)
                        val t = (top * image2.height).roundToInt().coerceIn(0, image2.height - 1)
                        val cw = ((right - left) * image2.width).roundToInt()
                            .coerceIn(1, image2.width - l)
                        val ch = ((bottom - top) * image2.height).roundToInt()
                            .coerceIn(1, image2.height - t)
                        val dir = File(context.filesDir, "crops").apply { mkdirs() }
                        val out = File(dir, "crop_${System.currentTimeMillis()}.png")
                        val ok = runCatching {
                            val cut = Bitmap.createBitmap(image2, l, t, cw, ch)
                            out.outputStream().use { cut.compress(Bitmap.CompressFormat.PNG, 95, it) }
                            if (cut !== image2) cut.recycle()
                        }.isSuccess
                        saving = false
                        if (ok) onCropped(Uri.fromFile(out).toString()) else onDismiss()
                    },
                ) { Text("Use", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

