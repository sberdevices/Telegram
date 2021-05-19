package ru.sberdevices.sbdv.analytics

import android.util.Log
import androidx.annotation.AnyThread
import org.json.JSONObject
import ru.sberdevices.analytics.Analytics
import ru.sberdevices.sbdv.model.AppEvent
import ru.sberdevices.sbdv.model.CallDirection
import ru.sberdevices.sbdv.model.CallEvent
import ru.sberdevices.sbdv.model.CallTechnicalInfo
import ru.sberdevices.sbdv.util.toFormattedString
import java.util.Date

private const val DATE_FORMAT = "dd.MM.yyyy HH:mm:ss"

@AnyThread
internal class AnalyticsCollectorImpl(private val analytics: Analytics) : AnalyticsCollector {

    @Volatile
    private var sumCallInfo: SumCallInfo? = null

    @Synchronized
    override fun onCallEvent(event: CallEvent) {
        Log.d(TAG, "onCallEvent($event)")
        when (event) {
            is CallEvent.InvitingToCall -> {
                sumCallInfo = SumCallInfo().apply {
                    direction = event.direction
                    afterVoiceSearch = event.afterVoiceSearch
                    afterVoiceDirectCommand = event.afterVoiceDirectCommand
                }
                analytics.send(EVENTS_PREFIX + INVITING_TO_CALL_SUFFIX, event.toJson().toString())
            }
            is CallEvent.CancelInviting -> {
                sumCallInfo?.cancelReason = event.reason
                analytics.send(EVENTS_PREFIX + CANCEL_INVITING_SUFFIX, event.toJson().toString())
                onSumCallReady(false)
            }
            is CallEvent.CallStarted -> {
                sumCallInfo?.let { info ->
                    info.callId = event.callId
                    info.dateStart = event.dateCreate
                    info.technicalInfo = event.technicalInfo
                }
                analytics.send(EVENTS_PREFIX + CALL_STARTED_SUFFIX, event.toJson().toString())
            }
            is CallEvent.EndCall -> {
                sumCallInfo?.let { info ->
                    info.dateEnd = event.dateCreate
                    val startTime = info.dateStart
                    if (startTime != null && info.dateEnd != null) {
                        val duration = Date(event.dateCreate.time - startTime.time)
                        info.durationSec = (duration.time / 1000).toInt()
                    }
                }
                analytics.send(EVENTS_PREFIX + CALL_END_SUFFIX, event.toCallEventJson().toString())
                onSumCallReady(true)
            }
            is CallEvent.SetCallRating -> {
                sumCallInfo?.callRating = event.userRateCall
                analytics.send(EVENTS_PREFIX + CALL_RATED, event.toJson().toString())
                onSumCallReady(true)
            }
            is CallEvent.CallError -> {
                sumCallInfo?.error = event.message
                analytics.send(EVENTS_PREFIX + CALL_ERROR_SUFFIX, event.toJson().toString())
                onSumCallReady(true)
            }
        }
    }

    override fun onAppEvent(event: AppEvent) {
        Log.d(TAG, "onAppEvent($event)")
        analytics.send(EVENTS_PREFIX + event.name.toLowerCase(), "")
    }

    private fun onSumCallReady(isSuccessCall: Boolean) {
        Log.d(TAG, "onSumCallReady(isSuccessCall = $isSuccessCall)")
        sumCallInfo?.let { callInfo ->
            callInfo.success = isSuccessCall
            val sumCallInfoJson = callInfo.toJson().toString()
            Log.d(TAG, "send to amplitude $sumCallInfoJson")
            analytics.send(EVENTS_PREFIX + FULL_CALL_INFO_SUFFIX, sumCallInfoJson)
        }
        sumCallInfo = null
    }

    companion object {
        private const val TAG = "AnalyticsCollectorImpl"
        private const val EVENTS_PREFIX = "telegram_calls_"
        private const val FULL_CALL_INFO_SUFFIX = "full_call_info"
        private const val INVITING_TO_CALL_SUFFIX = "inviting_to_call"
        private const val CANCEL_INVITING_SUFFIX = "cancel_inviting"
        private const val CALL_STARTED_SUFFIX = "call_started"
        private const val CALL_END_SUFFIX = "call_end"
        private const val CALL_ERROR_SUFFIX = "call_error"
        private const val CALL_RATED = "call_rated"
    }
}

private fun CallTechnicalInfo.toJson(): JSONObject {
    return JSONObject().apply {
        put("encoder", encoder)
        put("decoder", decoder)
    }
}

private data class SumCallInfo(
    var callId: String? = null,
    var direction: CallDirection? = null,
    var success: Boolean? = false,
    var dateStart: Date? = null,
    var dateEnd: Date? = null,
    var durationSec: Int? = null,
    var technicalInfo: CallTechnicalInfo? = null,
    var cancelReason: String? = null,
    var error: String? = null,
    var callRating: Int? = null,
    var afterVoiceSearch: Boolean? = null,
    var afterVoiceDirectCommand: Boolean? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("callId", callId)
            put("direction", direction?.name)
            put("success", success)
            put("dateStart", dateStart?.toFormattedString(DATE_FORMAT))
            put("dateEnd", dateEnd?.toFormattedString(DATE_FORMAT))
            put("durationSec", durationSec)
            put("technicalInfo", technicalInfo?.toJson())
            put("cancelReason", cancelReason)
            put("error", error)
            put("callRating", callRating)
            put("afterVoiceSearch", afterVoiceSearch)
            put("afterVoiceDirectCommand", afterVoiceDirectCommand)
        }
    }
}

private fun CallEvent.toCallEventJson(): JSONObject {
    return JSONObject().apply {
        put("callId", callId)
    }
}

private fun CallEvent.InvitingToCall.toJson(): JSONObject {
    return toCallEventJson().apply {
        put("direction", direction.name)
        put("afterVoiceSearch", afterVoiceSearch)
        put("afterVoiceDirectCommand", afterVoiceDirectCommand)
    }
}

private fun CallEvent.CancelInviting.toJson(): JSONObject {
    return toCallEventJson().apply {
        put("direction", direction.name)
        put("reason", reason)
    }
}

private fun CallEvent.CallStarted.toJson(): JSONObject {
    return toCallEventJson().apply {
        put("direction", direction.name)
        put("technicalInfo", technicalInfo?.toJson())
        put("reason", technicalInfo)
    }
}

private fun CallEvent.CallError.toJson(): JSONObject {
    return toCallEventJson().apply {
        put("message", message)
    }
}

private fun CallEvent.SetCallRating.toJson(): JSONObject {
    return toCallEventJson().apply {
        put("callRating", userRateCall)
    }
}