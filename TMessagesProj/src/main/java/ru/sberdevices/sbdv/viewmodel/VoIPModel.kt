package ru.sberdevices.sbdv.viewmodel

import android.app.Activity
import android.util.ArrayMap
import android.util.Log
import androidx.annotation.AnyThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.telegram.messenger.MessagesController
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC
import org.telegram.ui.Components.voip.VoIPHelper
import ru.sberdevices.sbdv.analytics.AnalyticsCollector
import ru.sberdevices.sbdv.model.CallDirection
import ru.sberdevices.sbdv.model.CallEvent
import ru.sberdevices.sbdv.model.CallTechnicalInfo
import ru.sberdevices.sbdv.util.toSingleEvent

private inline class PeerId(val value: Int)

private class OutgoingCallIntent(val afterVoiceSearch: Boolean, val afterVoiceDirectCommand: Boolean)

/**
 * VoIPModel - not bounded to view lifecycle, which provided public methods for handle calling states
 */
@AnyThread
class VoIPModel(
    private val analyticsCollector: AnalyticsCollector
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun getUser() = MessagesController.getInstance(UserConfig.selectedAccount)
        .getUser(UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId())

    private val _callEvent = MutableLiveData<CallEvent>()
    val callEvent: LiveData<CallEvent> = _callEvent.toSingleEvent()

    private val outgoingCallIntents = ArrayMap<PeerId, OutgoingCallIntent>()

    private fun handleCallEvent(event: CallEvent) {
        Log.d(TAG, "onCallEvent($event)")
        _callEvent.postValue(event)
        scope.launch {
            analyticsCollector.onCallEvent(event)
        }
    }

    fun onCallInviting(callId: String, peer: TLRPC.User, direction: CallDirection) {
        if (direction == CallDirection.OUT) {
            val outgoingCallIntent: OutgoingCallIntent? = outgoingCallIntents.remove(PeerId(peer.id))
            handleCallEvent(CallEvent.InvitingToCall(callId, direction, peer,
                afterVoiceSearch = outgoingCallIntent?.afterVoiceSearch,
                afterVoiceDirectCommand = outgoingCallIntent?.afterVoiceDirectCommand
            ))
        } else {
            handleCallEvent(CallEvent.InvitingToCall(callId, direction, peer))
        }
    }

    fun onStartCall(callId: String, peer: TLRPC.User, direction: CallDirection, technicalInfo: CallTechnicalInfo?) {
        handleCallEvent(CallEvent.CallStarted(callId, direction, peer, technicalInfo))
    }

    fun onCancelInviting(callId: String, direction: CallDirection, reason: String) {
        handleCallEvent(CallEvent.CancelInviting(callId, direction, reason))
    }

    fun onEndCall(callId: String) {
        handleCallEvent(CallEvent.EndCall(callId))
    }

    fun onCallError(callId: String, message: String) {
        handleCallEvent(CallEvent.CallError(callId, message))
    }

    fun onCallRate(callId: String, rating: Int) {
        handleCallEvent(CallEvent.SetCallRating(callId, rating))
    }

    fun onOutgoingCallUserIntent(
        activity: Activity,
        userId: Int,
        afterVoiceSearch: Boolean = false,
        afterVoiceDirectCommand: Boolean = false
    ) {
        Log.d(TAG, "onOutgoingCallUserIntent($activity, $userId, $afterVoiceSearch, $afterVoiceDirectCommand)")

        val messagesController = MessagesController.getInstance(UserConfig.selectedAccount)
        val user = messagesController.getUser(userId)

        if (user != null) {
            val userFull = messagesController.getUserFull(user.id)
            outgoingCallIntents[PeerId(userId)] = OutgoingCallIntent(
                afterVoiceSearch = afterVoiceSearch,
                afterVoiceDirectCommand = afterVoiceDirectCommand
            )
            VoIPHelper.startCall(user, true, userFull != null && userFull.video_calls_available, activity, userFull)
        } else {
            Log.e(TAG, "Not found user with id = $userId")
        }
    }

    companion object {
        private val TAG = "VoIPModel"
    }
}