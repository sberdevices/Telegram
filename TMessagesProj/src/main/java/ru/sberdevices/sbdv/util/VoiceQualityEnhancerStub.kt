package ru.sberdevices.sbdv.util

import com.google.android.exoplayer2.util.Log
import ru.sberdevices.sdk.echocancel.VoiceQualityEnhancer

private const val TAG = "VoiceQualityEnhancerStub"

class VoiceQualityEnhancerStub : VoiceQualityEnhancer {
    override fun close() {
        Log.w(TAG, "close()")
    }

    override fun start() {
        Log.w(TAG, "start()")
    }

    override fun stop() {
        Log.w(TAG, "stop()")
    }
}
