package ru.sberdevices.sbdv.calls

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.MessageObject
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.TLRPC.TL_messageActionPhoneCall
import org.telegram.tgnet.TLRPC.TL_phoneCallDiscardReasonBusy
import org.telegram.tgnet.TLRPC.TL_phoneCallDiscardReasonMissed
import ru.sberdevices.sbdv.SbdvServiceLocator
import ru.sberdevices.sbdv.model.Contact
import java.util.ArrayList
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "CallRepository"

private data class CallLogRow (
    val userId: Int,
    val calls: ArrayList<TLRPC.Message>,
    val type: CallType,
    val video: Boolean
)

class CallRepository {
    private val _callRowsLiveData = MutableLiveData<List<CallLogRow>>()
    private val callRows = ArrayList<CallLogRow>()
    private val classGuid = ConnectionsManager.generateClassGuid()
    private var loadInProgress = AtomicBoolean()

    val callInfoLiveData: LiveData<List<CallInfo>> = CallInfoLiveData().distinctUntilChanged()

    inner class CallInfoLiveData : MediatorLiveData<List<CallInfo>>() {

        private var callRows: List<CallLogRow>? = null
        private var contacts: List<Contact>? = null

        private val contactRepository = SbdvServiceLocator.getContactsRepositorySharedInstance()

        init {
            addSource(_callRowsLiveData) { callRows ->
                this.callRows = callRows
                onUpdate()
            }
            addSource(contactRepository.contacts) { contacts ->
                this.contacts = contacts
                onUpdate()
            }
        }

        @Synchronized
        private fun onUpdate() {
            Log.d(TAG, "onUpdate(${callRows?.size}, ${contacts?.size})")
            val rows = callRows
            val contacts = contacts
            if (rows != null && contacts != null) {
                val calls = ArrayList<CallInfo>()
                val contactsMap = contacts.map { it.id to it }.toMap()
                rows.forEach { call ->
                    contactsMap[call.userId]?.let { contact ->
                        calls.add(
                            CallInfo(
                                contact = contact,
                                type = call.type,
                                callsCount = call.calls.size,
                                date = Date(call.calls[0].date * 1000L)
                            )
                        )
                    }
                }
                postValue(calls)
            }
        }
    }

    fun onNewMessages(messages: ArrayList<MessageObject>) = synchronized(this) {
        Log.d(TAG, "onNewMessages(${messages.size})")
        val loadInProgress = loadInProgress.get()
        if (!loadInProgress) {
            val currentAccount = UserConfig.selectedAccount
            var callsChanged = false
            for (msg in messages) {
                if (msg.messageOwner.action is TL_messageActionPhoneCall) {
                    val userID = if (msg.messageOwner.from_id.user_id == UserConfig.getInstance(currentAccount)
                            .getClientUserId()
                    ) msg.messageOwner.peer_id.user_id else msg.messageOwner.from_id.user_id
                    var callType = if (msg.messageOwner.from_id.user_id == UserConfig.getInstance(currentAccount)
                            .getClientUserId()
                    ) CallType.OUT else CallType.IN
                    val reason = msg.messageOwner.action.reason
                    if (callType == CallType.IN && (reason is TL_phoneCallDiscardReasonMissed || reason is TL_phoneCallDiscardReasonBusy)) {
                        callType = CallType.MISSED
                    }
                    if (callRows.size > 0) {
                        val topRow: CallLogRow = callRows[0]
                        if (topRow.userId == userID && topRow.type == callType) {
                            topRow.calls.add(0, msg.messageOwner)
                            callsChanged = true
                            continue
                        }
                    }

                    val row = CallLogRow(
                        userId = userID,
                        calls = ArrayList(),
                        type = callType,
                        video = msg.isVideoCall
                    )
                    row.calls.add(msg.messageOwner)
                    callRows.add(0, row)
                    callsChanged = true
                }
            }
            if (callsChanged) {
                notifyCallsChanged()
            }
        }
    }

    fun requestRecentCalls() = synchronized(this) {
        Log.d(TAG, "requestRecentCalls()")
        val currentAccount = UserConfig.selectedAccount
        if (!UserConfig.getInstance(currentAccount).isClientActivated) return

        val loadInProgress = !loadInProgress.compareAndSet(false, true)
        if (loadInProgress) return

        val req = TLRPC.TL_messages_search()
        req.limit = CALLS_COUNT
        req.peer = TLRPC.TL_inputPeerEmpty()
        req.filter = TLRPC.TL_inputMessagesFilterPhoneCalls()
        req.q = ""
        req.offset_id = 0

        val reqId = ConnectionsManager.getInstance(currentAccount).sendRequest(
            req, { response: TLObject, error: TLRPC.TL_error? ->
                AndroidUtilities.runOnUIThread {
                    if (error == null) {
                        callRows.clear()
                        val msgs = response as TLRPC.messages_Messages
                        var currentRow: CallLogRow? = if (callRows.size > 0) callRows[callRows.size - 1] else null
                        var callsChanged = false
                        for (a in msgs.messages.indices) {
                            val msg = msgs.messages[a]
                            if (msg.action == null || msg.action is TLRPC.TL_messageActionHistoryClear) {
                                continue
                            }
                            var callType = if (msg.from_id.user_id == UserConfig.getInstance(currentAccount)
                                    .getClientUserId()
                            ) CallType.OUT else CallType.IN
                            val reason = msg.action.reason
                            if (callType == CallType.IN && (reason is TLRPC.TL_phoneCallDiscardReasonMissed || reason is TLRPC.TL_phoneCallDiscardReasonBusy)) {
                                callType = CallType.MISSED
                            }
                            val userID = if (msg.from_id.user_id == UserConfig.getInstance(currentAccount)
                                    .getClientUserId()
                            ) msg.peer_id.user_id else msg.from_id.user_id
                            if (currentRow == null || currentRow.userId != userID || currentRow.type != callType) {
                                if (currentRow != null && !callRows.contains(currentRow)) {
                                    callRows.add(currentRow)
                                    callsChanged = true
                                }
                                currentRow = CallLogRow(
                                    userId = userID,
                                    calls = ArrayList(),
                                    type = callType,
                                    video = msg.action != null && msg.action.video
                                )
                            }
                            currentRow.calls.add(msg)
                        }
                        if (currentRow != null && currentRow.calls.size > 0 && !callRows.contains(currentRow)) {
                            callRows.add(currentRow)
                            callsChanged = true
                        }
                        if (callsChanged) {
                            notifyCallsChanged()
                        }
                    }
                    this.loadInProgress.set(false)
                }
            }, ConnectionsManager.RequestFlagFailOnServerErrors
        )
        ConnectionsManager.getInstance(currentAccount).bindRequestToGuid(reqId, classGuid)
    }

    private fun notifyCallsChanged() {
        _callRowsLiveData.postValue(callRows)
    }

    private companion object {
        private const val CALLS_COUNT = 50
    }
}