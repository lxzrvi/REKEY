package io.github.lxzrvi.rekey.ui.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.lxzrvi.rekey.design.GlassContract
import io.github.lxzrvi.rekey.ui.components.Glyph
import io.github.lxzrvi.rekey.ui.components.RekeyGlyph

private const val FEEDBACK_EMAIL = "thaparavi382@gmail.com"
private const val REPO_URL = "https://github.com/lxzrvi/REKEY"

private val BoxShape = RoundedCornerShape(GlassContract.CORNER_DP.dp)

/** One tappable line in the drawer. */
@Composable
internal fun DrawerAction(
    glyph: Glyph,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(BoxShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), BoxShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RekeyGlyph(glyph, MaterialTheme.colorScheme.primary, Modifier.size(17.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RekeyGlyph(Glyph.CHEVRON_RIGHT, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(13.dp))
    }
}

/** Feedback, Share and About, in that order, at the foot of the drawer. */
@Composable
internal fun DrawerFooter() {
    val context = LocalContext.current
    var feedback by remember { mutableStateOf(false) }
    var about by remember { mutableStateOf(false) }

    DrawerAction(Glyph.BOLD, "Feedback", "Tell me what broke or what to add") { feedback = true }
    DrawerAction(Glyph.TRANSLATE, "Share REKEY", "Send the app to a friend") {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "REKEY — a keyboard you can actually make yours.\n$REPO_URL",
            )
        }
        runCatching { context.startActivity(Intent.createChooser(send, "Share REKEY")) }
    }
    DrawerAction(Glyph.CHECK, "About", "REKEY by lxzrvi") { about = true }

    if (feedback) FeedbackDialog(onDismiss = { feedback = false })
    if (about) AboutDialog(onDismiss = { about = false })
}

@Composable
private fun FeedbackDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Feedback",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "What happened, and what were you doing when it happened? Anything that actually matters helps.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val fieldShape = RoundedCornerShape(12.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .clip(fieldShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
            ) {
                if (message.isEmpty()) {
                    Text(
                        "Type your message…",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                BasicTextField(
                    value = message,
                    onValueChange = { message = it },
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                )
            }
            if (failed) {
                Text(
                    "No mail app found. Write to $FEEDBACK_EMAIL",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel", fontSize = 12.sp) }
                TextButton(
                    enabled = message.isNotBlank(),
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$FEEDBACK_EMAIL")
                            putExtra(Intent.EXTRA_SUBJECT, "REKEY feedback")
                            putExtra(Intent.EXTRA_TEXT, message)
                        }
                        try {
                            context.startActivity(intent)
                            onDismiss()
                        } catch (_: ActivityNotFoundException) {
                            failed = true
                        }
                    },
                ) { Text("Send", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "REKEY",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "A keyboard you can take apart and put back together your way — " +
                    "backgrounds, gradients, photos inside the keys, per-key sounds, " +
                    "corner radii, spacing, a suggestion engine that learns how you write, " +
                    "and the whole Unicode emoji set. Everything runs offline; nothing you " +
                    "type ever leaves the phone.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Developer", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("lxzrvi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                REPO_URL,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL))) }
                },
            )
            Text(
                "I built this with a lot of hard work. Enjoy it — and if you hit an error " +
                    "that actually matters, please send feedback so I can fix it.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Close", fontSize = 12.sp) }
            }
        }
    }
}

// ---- language ---------------------------------------------------------------

/** Where a language change should apply. */
enum class LanguageScope { KEYBOARD, BOTH, APP }

data class AppLanguage(val tag: String, val english: String, val native: String)

/** The five most-used languages in the world, by total speakers. */
val TopLanguages = listOf(
    AppLanguage("en", "English", "English"),
    AppLanguage("zh", "Chinese", "中文"),
    AppLanguage("hi", "Hindi", "हिन्दी"),
    AppLanguage("es", "Spanish", "Español"),
    AppLanguage("ar", "Arabic", "العربية"),
)

/**
 * Four boxes under the tester. The first is the language; the other three are
 * placeholders until the owner says what belongs in them.
 */
@Composable
internal fun QuickBoxes(
    language: String,
    onLanguage: (String, LanguageScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    var choosing by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<AppLanguage?>(null) }
    val current = TopLanguages.firstOrNull { it.tag == language } ?: TopLanguages.first()

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickBox(
                modifier = Modifier.weight(1f),
                glyph = Glyph.GLOBE,
                title = "Language",
                value = current.native,
                onClick = { choosing = true },
            )
            QuickBox(
                modifier = Modifier.weight(1f),
                glyph = Glyph.PALETTE,
                title = "Theme",
                value = "—",
                onClick = {},
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Sounds" to Glyph.CLIPBOARD, "Layout" to Glyph.KEYBOARD_TEXT).forEach { (t, g) ->
                QuickBox(
                    modifier = Modifier.weight(1f),
                    glyph = g,
                    title = t,
                    value = "—",
                    onClick = {},
                )
            }
        }
    }

    if (choosing) {
        Dialog(onDismissRequest = { choosing = false }) {
            SheetColumn("Choose a language") {
                TopLanguages.forEach { l ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                if (l.tag == language) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .clickable { pending = l; choosing = false }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            l.native,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            l.english,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    pending?.let { l ->
        Dialog(onDismissRequest = { pending = null }) {
            SheetColumn("Use ${l.english}?") {
                Text(
                    "Do you want this language on the keyboard only, or in the app too?",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                listOf(
                    LanguageScope.KEYBOARD to "Keyboard",
                    LanguageScope.BOTH to "Keyboard + App",
                    LanguageScope.APP to "App",
                ).forEach { (scope, label) ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(11.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onLanguage(l.tag, scope); pending = null }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                TextButton(onClick = { pending = null }) { Text("Cancel", fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun SheetColumn(title: String, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        content()
    }
}

@Composable
private fun QuickBox(
    modifier: Modifier,
    glyph: Glyph,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .height(76.dp)
            .clip(BoxShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), BoxShape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        RekeyGlyph(glyph, MaterialTheme.colorScheme.primary, Modifier.size(15.dp))
        Column {
            Text(
                title,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}
