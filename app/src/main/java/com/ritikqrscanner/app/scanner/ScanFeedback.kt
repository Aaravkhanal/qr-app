package com.ritikqrscanner.app.scanner

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.ritikqrscanner.app.BuildConfig

class ScanFeedback(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private var toneGenerator: ToneGenerator? = null

    fun confirm(soundEnabled: Boolean, hapticEnabled: Boolean) {
        if (soundEnabled) playSound()
        if (hapticEnabled) vibrate()
    }

    private fun playSound() {
        try {
            val audioManager = appContext.getSystemService(AudioManager::class.java)
            // Respect silent and vibrate modes. A scan never overrides the user's ringer policy.
            if (audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
            val player = toneGenerator ?: ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70).also {
                toneGenerator = it
            }
            player.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (error: RuntimeException) {
            logFailure("Unable to play scan confirmation", error)
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                appContext.getSystemService(VibratorManager::class.java)
                    ?.defaultVibrator
                    ?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                val vibrator = appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator?.vibrate(60)
                }
            }
        } catch (error: RuntimeException) {
            logFailure("Unable to vibrate after scan", error)
        }
    }

    override fun close() {
        try {
            toneGenerator?.release()
        } catch (error: RuntimeException) {
            logFailure("Unable to release scan audio", error)
        } finally {
            toneGenerator = null
        }
    }

    private fun logFailure(message: String, error: RuntimeException) {
        if (BuildConfig.DEBUG) Log.w(TAG, message, error)
    }

    private companion object {
        const val TAG = "ScanFeedback"
    }
}
