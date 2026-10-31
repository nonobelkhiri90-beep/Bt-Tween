package com.carcam.core

import android.content.Context
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper

/** يكتشف أن أي تطبيق موسيقى يشغّل صوتاً الآن */
class MusicStateMonitor(
    context: Context,
    private val onChange: (active: Boolean) -> Unit,
) {
    private val am = context.getSystemService(AudioManager::class.java)
    private val cb = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
            onChange(am.isMusicActive)
        }
    }

    fun start() {
        am.registerAudioPlaybackCallback(cb, Handler(Looper.getMainLooper()))
        onChange(am.isMusicActive)
    }

    fun stop() = am.unregisterAudioPlaybackCallback(cb)
}
