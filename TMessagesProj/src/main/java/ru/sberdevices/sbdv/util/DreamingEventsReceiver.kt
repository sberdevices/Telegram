package ru.sberdevices.sbdv.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.annotation.AnyThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

private const val TAG = "DreamingEventsReceiver"

enum class DreamingEvent {
    ACTION_DREAMING_STARTED, ACTION_DREAMING_STOPPED
}

class DreamingEventsReceiver(val context: Context) : BroadcastReceiver() {

    private val broadcastHandler: Handler by lazy {
        val thread = HandlerThread("dreaming_broadcast_thread").apply {
            start().also { Log.d(TAG, "thread started $threadId") }
        }
        Handler(thread.looper)
    }
    private val _lastEvent = MutableLiveData<DreamingEvent>()
    val lastEvent: LiveData<DreamingEvent> = _lastEvent

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "On ${intent.action}")
        when (intent.action) {
            Intent.ACTION_DREAMING_STARTED -> _lastEvent.postValue(DreamingEvent.ACTION_DREAMING_STARTED)
            Intent.ACTION_DREAMING_STOPPED -> _lastEvent.postValue(DreamingEvent.ACTION_DREAMING_STOPPED)
        }
    }

    @AnyThread
    fun startListen(): LiveData<DreamingEvent> {
        val filter = IntentFilter(Intent.ACTION_DREAMING_STARTED).apply {
            addAction(Intent.ACTION_DREAMING_STOPPED)
        }
        context.registerReceiver(this, filter, null, broadcastHandler)
        return lastEvent
    }

    @AnyThread
    fun stopListen() {
        context.unregisterReceiver(this)
    }
}
