package io.github.lxzrvi.rekey.ui.onboarding

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.lxzrvi.rekey.R
import io.github.lxzrvi.rekey.design.GlassContract
import io.github.lxzrvi.rekey.design.PatternContract
import io.github.lxzrvi.rekey.ime.RekeyImeService
import io.github.lxzrvi.rekey.ui.components.Glyph
import io.github.lxzrvi.rekey.ui.components.RekeyGlyph
import io.github.lxzrvi.rekey.ui.components.RekeyWordmark
import io.github.lxzrvi.rekey.ui.home.SquigglePattern
import kotlinx.coroutines.delay

/**
 * First-run flow, built from the same squiggle background, cards and buttons as
 * the main page. Step 1 enables the input method, step 2 makes it active; both
 * are re-checked every time the app comes back to the foreground.
 */
@Composable
fun SetupScreen(
    darkTheme: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imm = remember(context) {
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }
    val imeId = remember(context) {
        ComponentName(context, RekeyImeService::class.java).flattenToShortString()
    }

    var enabled by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(false) }
    var step by remember { mutableIntStateOf(0) }

    fun refresh() {
        enabled = runCatching {
            imm.enabledInputMethodList.any { it.id == imeId || it.packageName == context.packageName }
        }.getOrDefault(false)
        selected = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) == imeId
        }.getOrDefault(false)
        step = when {
            !enabled -> 0
            !selected -> 1
            else -> 2
        }
    }

    // Picking REKEY in the picker is the last step: continue on its own, with a
    // beat long enough for the second tick to be seen.
    LaunchedEffect(step) {
        if (step == 2) {
            delay(420)
            onFinished()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        refresh()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // The keyboard picker is a dialog, not another activity: this screen stays
    // RESUMED while it is open, so ON_RESUME never fires after the choice.
    // Poll until both steps are satisfied.
    LaunchedEffect(Unit) {
        while (step < 2) {
            delay(350)
            refresh()
        }
    }

    Box(modifier.fillMaxSize().testTag("setup")) {
        SquigglePattern(
            darkTheme = darkTheme,
            strokeAlpha = if (darkTheme) PatternContract.DARK_INK_ALPHA else PatternContract.LIGHT_INK_ALPHA,
        )
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RekeyWordmark(
                fontSize = 19.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Start,
            )
            Text(
                text = stringResource(R.string.setup_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.setup_subtitle),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SetupStep(
                index = 1,
                title = stringResource(R.string.setup_step_one),
                body = stringResource(R.string.setup_step_one_body),
                action = stringResource(R.string.setup_open_settings),
                done = enabled,
                active = step == 0,
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
            )
            SetupStep(
                index = 2,
                title = stringResource(R.string.setup_step_two),
                body = stringResource(R.string.setup_step_two_body),
                action = stringResource(R.string.setup_open_picker),
                done = selected,
                active = step == 1,
                onClick = { runCatching { imm.showInputMethodPicker() } },
            )

            Spacer(Modifier.height(2.dp))
            Button(
                onClick = onFinished,
                enabled = step == 2,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("setup_continue"),
            ) {
                Text(stringResource(R.string.setup_continue), fontSize = 14.sp)
            }
            TextButton(
                onClick = onFinished,
                modifier = Modifier.fillMaxWidth().testTag("setup_skip"),
            ) {
                Text(
                    text = stringResource(R.string.setup_skip),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SetupStep(
    index: Int,
    title: String,
    body: String,
    action: String,
    done: Boolean,
    active: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(GlassContract.CORNER_DP.dp)
    val accent = MaterialTheme.colorScheme.primary
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (active) accent.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                shape = shape,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (done) accent else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (done) {
                    RekeyGlyph(Glyph.CHECK, MaterialTheme.colorScheme.onPrimary, Modifier.size(15.dp))
                } else {
                    Text(
                        text = index.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (done) {
                Text(
                    text = stringResource(R.string.setup_done_label),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = accent,
                )
            }
        }
        Text(
            text = body,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onClick,
            enabled = !done,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(40.dp),
        ) {
            Text(action, fontSize = 13.sp)
        }
        Text(
            text = stringResource(R.string.setup_step_index, index),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}
