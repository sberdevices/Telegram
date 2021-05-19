package ru.sberdevices.sbdv.analytics

import androidx.annotation.AnyThread
import ru.sberdevices.analytics.Analytics

@AnyThread
class AnalyticsCollectorFactory {

    companion object {
        @JvmStatic
        fun getAnalyticsSdk(analytics: Analytics): AnalyticsCollector = AnalyticsCollectorImpl(analytics)
    }
}