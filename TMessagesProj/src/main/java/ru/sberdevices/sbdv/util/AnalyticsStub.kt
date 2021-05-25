package ru.sberdevices.sbdv.util

import com.google.android.exoplayer2.util.Log
import ru.sberdevices.analytics.Analytics

private const val TAG = "AnalyticsStub"

class AnalyticsStub : Analytics {

    override fun send(name: String, value: String) {
        Log.w(TAG, "send called on stub")
    }

    override fun release() {
        Log.w(TAG, "release called on stub")
    }
}
