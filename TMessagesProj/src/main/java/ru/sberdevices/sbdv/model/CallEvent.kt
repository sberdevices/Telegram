package ru.sberdevices.sbdv.model

import org.telegram.tgnet.TLRPC
import java.util.Calendar
import java.util.Date

data class CallTechnicalInfo(
    val encoder: String? = null,
    val decoder: String? = null
)

sealed class CallEvent(val callId: String, val dateCreate: Date = Calendar.getInstance().time) {

    data class InvitingToCall(
        private val cid: String,
        val direction: CallDirection,
        val peer: TLRPC.User,
        val afterVoiceSearch: Boolean? = null,
        val afterVoiceDirectCommand: Boolean? = null
    ) : CallEvent(cid)

    data class CancelInviting(private val cid: String, val direction: CallDirection, val reason: String) :
        CallEvent(cid)

    data class CallStarted(
        private val cid: String,
        val direction: CallDirection,
        val peer: TLRPC.User,
        val technicalInfo: CallTechnicalInfo? = null
    ) : CallEvent(cid)

    data class EndCall(private val cid: String) : CallEvent(cid)
    data class SetCallRating(private val cid: String, val userRateCall: Int? = 0) : CallEvent(cid)

    data class CallError(private val cid: String, val message: String) : CallEvent(cid)
}