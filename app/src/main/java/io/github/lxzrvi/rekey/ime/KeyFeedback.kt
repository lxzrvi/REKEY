package io.github.lxzrvi.rekey.ime

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import io.github.lxzrvi.rekey.R
import io.github.lxzrvi.rekey.data.FeelStyle

/**
 * Key sound and vibration.
 *
 * A [SoundPool] is used rather than MediaPlayer: a key can be struck ten times a
 * second and each strike must start with no setup cost. Only the selected sound
 * is ever loaded, so switching presets does not decode ten files.
 *
 * The raw resources are referenced through R directly. Looking them up by name
 * would let the resource shrinker drop them, because nothing in the code would
 * appear to use them.
 */
class KeyFeedback(context: Context) {

    private val appContext = context.applicationContext
    private val sounds: IntArray = intArrayOf(
        R.raw.key_soft, R.raw.key_mechanical, R.raw.key_typewriter,
        R.raw.key_bubble, R.raw.key_glass, R.raw.key_wood,
        R.raw.key_click, R.raw.key_pop, R.raw.key_tactile, R.raw.key_muted,
    )

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val vibrator: Vibrator? =
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            ?: runCatching { context.getSystemService(Vibrator::class.java) }.getOrNull()

    private var spec = FeelStyle()
    private var loadedIndex = -1
    private var sampleId = 0
    private var ready = false

    init {
        pool.setOnLoadCompleteListener { _, _, status -> ready = status == 0 }
    }

    /** Called whenever the active preset changes; reloads only if the sound did. */
    fun configure(style: FeelStyle) {
        spec = style
        val index = style.soundIndex.coerceIn(0, sounds.lastIndex)
        if (index != loadedIndex) {
            if (sampleId != 0) {
                pool.unload(sampleId)
                sampleId = 0
            }
            ready = false
            loadedIndex = index
            if (style.soundOn) sampleId = pool.load(appContext, sounds[index], 1)
        } else if (style.soundOn && sampleId == 0) {
            ready = false
            sampleId = pool.load(appContext, sounds[index], 1)
        }
    }

    /** One key strike. Safe to call before the sample has finished loading. */
    fun tap() {
        if (spec.soundOn && ready && sampleId != 0) {
            val v = spec.soundVolume.coerceIn(0f, 1f)
            pool.play(sampleId, v, v, 1, 0, 1f)
        }
        if (spec.hapticOn) {
            val v = vibrator ?: return
            if (!v.hasVibrator()) return
            val ms = spec.hapticDuration.coerceIn(1, 100).toLong()
            runCatching {
                if (v.hasAmplitudeControl()) {
                    v.vibrate(
                        VibrationEffect.createOneShot(ms, spec.hapticStrength.coerceIn(1, 255)),
                    )
                } else {
                    v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
        }
    }

    /**
     * Audition a sound from the editor.
     *
     * Uses its own short-lived pool so it cannot disturb the typing pool's
     * load listener or its currently loaded sample.
     */
    fun preview(index: Int, volume: Float) {
        val i = index.coerceIn(0, sounds.lastIndex)
        val one = SoundPool.Builder().setMaxStreams(1).build()
        var id = 0
        one.setOnLoadCompleteListener { sp, sample, status ->
            if (sample == id && status == 0) {
                val v = volume.coerceIn(0f, 1f)
                sp.play(sample, v, v, 1, 0, 1f)
            }
        }
        id = one.load(appContext, sounds[i], 1)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            runCatching { one.release() }
        }, 700)
    }

    fun release() {
        runCatching { pool.release() }
    }
}
