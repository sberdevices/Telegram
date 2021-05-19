package ru.sberdevices.sbdv.viewmodel

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.telegram.tgnet.TLRPC
import ru.sberdevices.playsyncsdk.PlaybackSyncManager
import ru.sberdevices.playsyncsdk.model.CallData
import ru.sberdevices.playsyncsdk.model.CallUser
import ru.sberdevices.sbdv.config.Config
import ru.sberdevices.sbdv.model.CallDirection
import ru.sberdevices.sbdv.model.CallEvent
import ru.sberdevices.sbdv.util.getAvatarBitmap
import ru.sberdevices.sbdv.util.getFullName
import ru.sberdevices.sbdv.util.observeOnce
import ru.sberdevices.sbdv.util.toSingleEvent

private const val TAG = "VoIPViewModel"

private const val FETCH_AVATAR_SIZE_PX = 64
private const val START_SMARTFOCUS_ON_START_CALL = true

/**
 * VoIPViewModel - provide public methods for handle view (VoipFragment) events
 */
class VoIPViewModel(
    private val voipModel: VoIPModel,
    private val playbackSyncManager: PlaybackSyncManager,
    private val config: Config
) : ViewModel() {

    private val _playbackSyncLiveEvent = MutableLiveData<Boolean>()
    val playbackSyncLiveEvent: LiveData<Boolean> = _playbackSyncLiveEvent.toSingleEvent()

    private var _isSmartFocusEnabled = MutableLiveData<Boolean>(START_SMARTFOCUS_ON_START_CALL)
    val isSmartFocusEnabled: LiveData<Boolean> = _isSmartFocusEnabled

    private var _watchPartyEnabled = MutableLiveData<Boolean>()
    val watchPartyEnabledLiveData = _watchPartyEnabled

    private val commonViewingInviteListener = object : PlaybackSyncManager.InviteListener {
        override fun onAcceptInviting() {
            Log.d(TAG, "InviteListener.onAcceptInviting()")
        }

        override fun onIncomingInviting() {
            Log.d(TAG, "InviteListener.onIncomingInviting()")
        }

        override fun onContentChosen() {
            Log.d(TAG, "InviteListener.onContentChosen()")
        }

        override fun onDeclineInviting() {
            Log.d(TAG, "InviteListener.onDeclineInviting()")
        }

        override fun onStartPlaySync() {
            Log.d(TAG, "InviteListener.onStartPlaySync()")
            _playbackSyncLiveEvent.postValue(true)
        }

        override fun onConnectionError() {
            Log.d(TAG, "InviteListener.onConnectionError()")
        }
    }

    private val callEventObserver = Observer<CallEvent> { callEvent ->
        Log.d(TAG, "On new call event ${callEvent.javaClass.name}")
        when (callEvent) {
            is CallEvent.CallStarted -> {
                onStartCall(callEvent.callId, callEvent.peer, callEvent.direction)
            }
            is CallEvent.EndCall -> {
                playbackSyncManager.disconnect()
            }
        }
    }

    init {
        Log.d(TAG, "<init> with $config @${hashCode()}")

        voipModel.callEvent.observeForever(callEventObserver)

        viewModelScope.launch(Dispatchers.Default) {
            config.watchPartyEnabledFlow.collect { watchPartyEnabled ->
                Log.d(TAG, "watchPartyEnabled = $watchPartyEnabled")
                _watchPartyEnabled.postValue(watchPartyEnabled)
            }
        }
    }

    @MainThread
    private fun onStartCall(callId: String, peer: TLRPC.User, direction: CallDirection) {
        Log.d(TAG, "onStartCall(callId=$callId, peer=${peer.id}, direction=$direction")
        _watchPartyEnabled.observeOnce { watchPartyEnabled ->
            if (watchPartyEnabled) {
                if (!playbackSyncManager.isStarted()) {
                    // TODO: check correct change peer on second call with other peer
                    val user = voipModel.getUser()
                    Log.d(TAG, "Connect To Play Sync")
                    val callPeer =
                        CallUser(peer.getFullName(), photoBitmap = peer.getAvatarBitmap(FETCH_AVATAR_SIZE_PX))
                    val callUser =
                        CallUser(user.getFullName(), photoBitmap = user.getAvatarBitmap(FETCH_AVATAR_SIZE_PX))
                    val callData = CallData(
                        callId = callId,
                        peer = callPeer,
                        user = callUser
                    )
                    playbackSyncManager.connect(callData, commonViewingInviteListener)
                } else {
                    Log.d(TAG, "Already connected To Play Sync")
                }
            }
        }
    }

    @MainThread
    fun onToggleSmartFocus() {
        val newValue = !(_isSmartFocusEnabled.value ?: START_SMARTFOCUS_ON_START_CALL)
        Log.d(TAG, "onToggleSmartFocus: $newValue")
        _isSmartFocusEnabled.value = newValue
    }

    @MainThread
    fun onClickCommonViewing() {
        Log.d(TAG, "onClickCommonViewing()" + hashCode())
        playbackSyncManager.inviteToPlaySync()
    }

    override fun onCleared() {
        Log.d(TAG, "onCleared()@" + hashCode())
        voipModel.callEvent.removeObserver(callEventObserver)
        playbackSyncManager.disconnect()
        super.onCleared()
    }
}
