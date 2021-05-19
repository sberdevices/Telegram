package ru.sberdevices.sbdv.analytics

import androidx.annotation.AnyThread
import ru.sberdevices.sbdv.model.AppEvent
import ru.sberdevices.sbdv.model.CallEvent

@AnyThread
interface AnalyticsCollector {
    fun onCallEvent(event: CallEvent)
    fun onAppEvent(event: AppEvent)
}