package com.mhrahmankwoshik.gambitstudio.util

import android.media.AudioManager
import android.media.ToneGenerator

object SoundPlayer {
    private var toneGen: ToneGenerator? = null

    init {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (ignored: Exception) {
        }
    }

    fun playMove() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
        } catch (ignored: Exception) {}
    }

    fun playCapture() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 60)
        } catch (ignored: Exception) {}
    }

    fun playCheck() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP2, 90)
        } catch (ignored: Exception) {}
    }
}
