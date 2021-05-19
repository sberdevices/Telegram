package ru.sberdevices.sbdv

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.UserConfig
import java.util.ArrayList

private const val TAG = "SbdvBaseFragment"

abstract class SbdvBaseFragment : Fragment(), NotificationCenter.NotificationCenterDelegate {
    private val notificationCenter: NotificationCenter by lazy {
        NotificationCenter.getInstance(UserConfig.selectedAccount)
    }

    protected val messagesController: MessagesController by lazy {
        MessagesController.getInstance(UserConfig.selectedAccount)
    }

    open fun onMainUserInfoChange() {}

    open fun onNewMessages(messages: ArrayList<MessageObject>) {}

    open fun onDeleteMessages() {}

    private val voipModel = SbdvServiceLocator.getVoIPModelSharedInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notificationCenter.addObserver(this, NotificationCenter.didReceiveNewMessages)
        notificationCenter.addObserver(this, NotificationCenter.messagesDeleted)
        notificationCenter.addObserver(this, NotificationCenter.mainUserInfoChanged)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        notificationCenter.removeObserver(this, NotificationCenter.didReceiveNewMessages)
        notificationCenter.removeObserver(this, NotificationCenter.messagesDeleted)
        notificationCenter.removeObserver(this, NotificationCenter.mainUserInfoChanged)
    }

    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
        if (id == NotificationCenter.didReceiveNewMessages) {
            val scheduled = args[2] as Boolean
            if (!scheduled) {
                val messages = args[1] as ArrayList<MessageObject>
                onNewMessages(messages)
            }
        }
        if (id == NotificationCenter.messagesDeleted) {
            val scheduled = args[2] as Boolean
            if (!scheduled) {
                onDeleteMessages()
            }
        }
        if (id == NotificationCenter.mainUserInfoChanged) {
            onMainUserInfoChange()
        }
    }

    internal fun onCallToUserClick(userId: Int, fromVoiceSearch: Boolean = false) {
        Log.d(TAG, "onCallToUserClick()")
        val messagesController = MessagesController.getInstance(UserConfig.selectedAccount)
        val user = messagesController.getUser(userId)

        if (user != null) {
            val needMockCalls = SbdvServiceLocator.getLocalConfigSharedInstance().localConfigStateFlow.value.mockCallsOn
            if (needMockCalls) {
                Toast.makeText(
                    context,
                    "Mock voice call to " + user.first_name.orEmpty() + " " + user.last_name.orEmpty() + " " + user.username.orEmpty(),
                    Toast.LENGTH_LONG
                ).apply {
                    setGravity(Gravity.CENTER, 0, 0)
                    show()
                }
            } else {
                voipModel.onOutgoingCallUserIntent(
                    activity = requireActivity(),
                    userId = userId,
                    afterVoiceSearch = fromVoiceSearch,
                    afterVoiceDirectCommand = false
                )
            }
        }
    }
}
