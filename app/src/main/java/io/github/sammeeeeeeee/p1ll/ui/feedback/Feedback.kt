package io.github.sammeeeeeeee.p1ll.ui.feedback

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.sammeeeeeeee.p1ll.R

/** Sound and touch, tuned together. */
interface Feel {
    /** [nth] = how many doses are now taken today, so the notes climb through the day. */
    fun taken(nth: Int) {}
    fun complete() {}
    fun undo() {}
    fun saved() {}
    fun tick() {}
    fun threshold() {}
}

object Silent : Feel

val LocalFeedback = staticCompositionLocalOf<Feel> { Silent }

// Effect constants are compile-time ints; every use is behind an SDK or support check.
@SuppressLint("InlinedApi")
class Feedback(context: Context) : Feel {
    @Volatile var sounds = true
    @Volatile var haptics = true

    private val app = context.applicationContext
    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val notes = listOf(R.raw.take_1, R.raw.take_2, R.raw.take_3, R.raw.take_4, R.raw.take_5, R.raw.take_6)
        .map { pool.load(app, it, 1) }
    private val chord = pool.load(app, R.raw.complete, 1)
    private val back = pool.load(app, R.raw.undo, 1)
    private val save = pool.load(app, R.raw.save, 1)

    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= 31) app.getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") app.getSystemService(Vibrator::class.java)

    override fun taken(nth: Int) {
        play(notes[(nth - 1).coerceIn(0, notes.lastIndex)], 0.8f)
        buzz(
            listOf(P(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.85f, 0), P(VibrationEffect.Composition.PRIMITIVE_TICK, 0.45f, 70)),
            fallback = VibrationEffect.EFFECT_CLICK,
        )
    }

    override fun complete() {
        play(chord, 0.85f)
        buzz(
            listOf(
                P(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.55f, 0),
                P(VibrationEffect.Composition.PRIMITIVE_CLICK, 1f, 30),
                P(VibrationEffect.Composition.PRIMITIVE_TICK, 0.6f, 90),
                P(VibrationEffect.Composition.PRIMITIVE_TICK, 0.35f, 80),
            ),
            fallback = VibrationEffect.EFFECT_HEAVY_CLICK,
        )
    }

    override fun undo() {
        play(back, 0.6f)
        buzz(listOf(P(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.5f, 0)), VibrationEffect.EFFECT_TICK)
    }

    override fun saved() {
        play(save, 0.7f)
        buzz(
            listOf(P(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.6f, 0), P(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.9f, 70)),
            VibrationEffect.EFFECT_DOUBLE_CLICK,
        )
    }

    override fun tick() = buzz(listOf(P(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f, 0)), VibrationEffect.EFFECT_TICK)

    override fun threshold() = buzz(listOf(P(VibrationEffect.Composition.PRIMITIVE_TICK, 0.9f, 0)), VibrationEffect.EFFECT_TICK)

    private fun play(id: Int, volume: Float) {
        if (sounds) pool.play(id, volume, volume, 1, 0, 1f)
    }

    private data class P(val primitive: Int, val scale: Float, val delay: Int)

    @SuppressLint("WrongConstant") // primitives come from the Composition constants above
    private fun buzz(primitives: List<P>, fallback: Int) {
        if (!haptics || !vibrator.hasVibrator()) return
        val effect = when {
            Build.VERSION.SDK_INT >= 30 &&
                vibrator.areAllPrimitivesSupported(*primitives.map { it.primitive }.toIntArray()) ->
                VibrationEffect.startComposition().apply {
                    primitives.forEach { addPrimitive(it.primitive, it.scale, it.delay) }
                }.compose()
            Build.VERSION.SDK_INT >= 29 -> VibrationEffect.createPredefined(fallback)
            else -> VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
        } else {
            vibrator.vibrate(effect)
        }
    }
}
