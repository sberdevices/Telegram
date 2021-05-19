package ru.sberdevices.sbdv.util

import com.google.android.exoplayer2.util.Log
import ru.sberdevices.services.calls.CallManager
import ru.sberdevices.services.calls.Contact
import ru.sberdevices.services.calls.aidl.CallAppState

private const val TAG = "CallManagerStub"

class CallManagerStub : CallManager {

    override fun addListener(listener: CallManager.Listener) {
        Log.w(TAG, "addListener called on stub")
    }

    override fun close() {
        Log.w(TAG, "addListener() called on stub")
    }

    override fun removeListener(listener: CallManager.Listener) {
        Log.w(TAG, "removeListener() called on stub")
    }

    override fun setCallAppState(state: CallAppState) {
        Log.w(TAG, "setCallAppState() called on stub")
    }

    override fun setContacts(contacts: List<Contact>) {
        Log.w(TAG, "setContacts() called on stub")
    }
}
