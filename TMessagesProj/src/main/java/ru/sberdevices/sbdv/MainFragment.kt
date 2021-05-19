package ru.sberdevices.sbdv

import android.annotation.SuppressLint
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.appcompat.widget.AppCompatToggleButton
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.ui.ActionBar.AlertDialog
import ru.sberdevices.sbdv.appstate.ScreenState
import ru.sberdevices.sbdv.calls.RecentCallsFragment
import ru.sberdevices.sbdv.contacts.ContactsFragment
import ru.sberdevices.sbdv.model.AppEvent
import ru.sberdevices.sbdv.util.dimBehind
import ru.sberdevices.sbdv.view.AvatarView

private const val TAG = "MainFragment"
private const val AVATAR_CLICK_FOR_DEV_MODE_COUNT = 5
private const val PHONE_CLICK_FOR_DEV_MODE_COUNT = 5

@SuppressLint("InflateParams")
class MainFragment : Fragment() {

    private val userMenuLayout by lazy { LayoutInflater.from(context).inflate(R.layout.sbdv_user_menu_layout, null) }
    private val devMenuLayout by lazy { LayoutInflater.from(context).inflate(R.layout.sbdv_dev_menu_layout, null) }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val localConfig by lazy { SbdvServiceLocator.getLocalConfigSharedInstance() }

    private val analyticsCollector = SbdvServiceLocator.getAnalyticsSdkSharedInstance()

    private lateinit var recentCallsView: View
    private lateinit var contactsView: View
    private lateinit var contactsButton: View
    private lateinit var recentCallsButton: View
    private lateinit var menuButton: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.sbdv_fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        contactsView = view.findViewById(R.id.contactsView)

        recentCallsView = view.findViewById(R.id.recentCallsView)
        recentCallsButton = view.findViewById(R.id.recentCallsButton)
        recentCallsButton.setOnClickListener { selectTab(Tab.RECENT_CALLS) }

        menuButton = view.findViewById(R.id.settingsButton)
        menuButton.setOnClickListener { showMenu() }

        contactsButton = view.findViewById(R.id.contactsButton)
        contactsButton.setOnClickListener { selectTab(Tab.CONTACTS) }

        selectTab(Tab.CONTACTS, byUserClick = false)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    fun getScreenState(): ScreenState? {
        return if (contactsView.visibility == View.VISIBLE) {
            val fragment = childFragmentManager.findFragmentById(R.id.contactsView) as ContactsFragment?
            fragment?.getScreenState()
        } else {
            val fragment = childFragmentManager.findFragmentById(R.id.recentCallsView) as RecentCallsFragment?
            fragment?.getScreenState()
        }
    }

    private fun showMenu() {
        val marginEnd = requireContext().resources.getDimensionPixelSize(R.dimen.sbdv_logout_popup_margin_end)
        val marginTop = requireContext().resources.getDimensionPixelSize(R.dimen.sbdv_logout_popup_margin_top)

        val user = UserConfig.getInstance(UserConfig.selectedAccount).currentUser
        val avatarView = userMenuLayout.findViewById<AvatarView>(R.id.userMenuAvatarView)
        avatarView.run {
            setUser(user)
        }
        userMenuLayout.findViewById<TextView>(R.id.userFullNameTextView).run {
            val fullName = "${user.first_name ?: ""} ${user.last_name ?: ""}"
            text = fullName
        }
        val userPhoneTextView = userMenuLayout.findViewById<TextView>(R.id.userPhoneTextView)
        userPhoneTextView.run {
            val phoneNumber = user.phone
            if (phoneNumber != null && phoneNumber.isNotBlank()) {
                val formattedNumber = "+${PhoneNumberUtils.formatNumber(phoneNumber, "RU")}"
                text = formattedNumber
                isVisible = true
            } else {
                isVisible = false
            }

            focusable = View.FOCUSABLE
            requestFocus()
        }
        userMenuLayout.findViewById<TextView>(R.id.usernameTextView).run {
            val username = user.username
            if (username != null && username.isNotBlank()) {
                val usernameWithAt = "@${username}"
                text = usernameWithAt
            }
        }

        userMenuLayout.findViewById<View>(R.id.userMenuLogoutButtonView).setOnClickListener {
            onClickLogout()
        }

        PopupWindow(
            userMenuLayout,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).run {
            showAsDropDown(menuButton, -marginEnd, -marginTop)
            dimBehind(0.6f)
        }

        scope.launch {
            localConfig.localConfigStateFlow.collect { localConfiguration ->
                if (localConfiguration.developerModeEnabled) {
                    avatarView.setOnClickListener { showDevOptionsMenu() }
                } else {

                    var avatarClicksCount = 0
                    var phoneClicksCount = 0

                    fun checkDevClick() {
                        if (avatarClicksCount == AVATAR_CLICK_FOR_DEV_MODE_COUNT && phoneClicksCount == PHONE_CLICK_FOR_DEV_MODE_COUNT) {
                            Toast.makeText(context, "Developer options enabled", Toast.LENGTH_SHORT).show()
                            localConfig.onEnableDeveloperMode(true)
                            avatarView.setOnClickListener { showDevOptionsMenu() }
                            avatarClicksCount = 0
                            phoneClicksCount = 0
                        }
                        if (avatarClicksCount > AVATAR_CLICK_FOR_DEV_MODE_COUNT || phoneClicksCount > PHONE_CLICK_FOR_DEV_MODE_COUNT) {
                            avatarClicksCount = 0
                            phoneClicksCount = 0
                        }
                    }

                    avatarView.setOnClickListener {
                        avatarClicksCount++
                        if (avatarClicksCount == AVATAR_CLICK_FOR_DEV_MODE_COUNT) checkDevClick()
                    }
                    userPhoneTextView.setOnClickListener {
                        phoneClicksCount++
                        if (phoneClicksCount == PHONE_CLICK_FOR_DEV_MODE_COUNT) checkDevClick()
                    }
                }
            }
        }
    }

    @MainThread
    private fun showDevOptionsMenu() {
        Log.d(TAG, "openDevOptionsMenu()")
        val mockCallsToggleButton = devMenuLayout.findViewById<AppCompatToggleButton>(R.id.mockCallsToggleButton)
        mockCallsToggleButton.isChecked = localConfig.localConfigStateFlow.value.mockCallsOn
        mockCallsToggleButton.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            localConfig.onEnableMockCalls(isChecked)
        }

        PopupWindow(
            devMenuLayout,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).run {
            showAsDropDown(menuButton, -100, -100)
            dimBehind(0.6f)
        }
    }

    @MainThread
    private fun onClickLogout() {
        Log.d(TAG, "onClickLogout()")
        val dialogView: View = FrameLayout.inflate(context, R.layout.sbdv_alert_dialog_big, null)
        val builder = AlertDialog.Builder(context).apply {
            setTransparentBackground(true)
            setView(dialogView)
        }
        val dialog = builder.create()

        val dialogMargin = requireContext().resources.getDimensionPixelSize(R.dimen.margin_default)
        dialog.window?.let { window ->
            window.setGravity(Gravity.END)
            window.attributes.x = dialogMargin
        }
        dialog.show()

        val title = dialogView.findViewById<TextView>(R.id.sdbv_dialog_title)
        title.text = LocaleController.getString("LogOut", R.string.LogOut)

        val message = dialogView.findViewById<TextView>(R.id.sdbv_dialog_message)
        message.text = AndroidUtilities.replaceTags(
            LocaleController.formatString("AreYouSureLogout", R.string.AreYouSureLogout)
        )

        val positiveButton = dialogView.findViewById<Button>(R.id.sdbv_dialog_negative)
        positiveButton.text = LocaleController.getString("LogOff", R.string.LogOff)
        positiveButton.setOnClickListener {
            SbdvServiceLocator.getAnalyticsSdkSharedInstance().onAppEvent(AppEvent.LOGOUT)
            MessagesController.getInstance(UserConfig.selectedAccount).performLogout(1)
            dialog.dismiss()
        }

        val cancelButton = dialogView.findViewById<Button>(R.id.sdbv_dialog_positive)
        cancelButton.text = LocaleController.getString("StayHere", R.string.StayHere)
        cancelButton.setOnClickListener { dialog.dismiss() }
    }

    private fun selectTab(tab: Tab, byUserClick: Boolean = true) {
        recentCallsButton.isSelected = tab == Tab.RECENT_CALLS
        contactsButton.isSelected = tab == Tab.CONTACTS
        recentCallsView.isInvisible = tab != Tab.RECENT_CALLS
        contactsView.isInvisible = tab != Tab.CONTACTS

        if (byUserClick) {
            val appEvent = when (tab) {
                Tab.RECENT_CALLS -> AppEvent.CLICK_RECENT_LIST
                Tab.CONTACTS -> AppEvent.CLICK_CONTACTS_LIST
            }
            analyticsCollector.onAppEvent(appEvent)
        }
    }

    private enum class Tab {
        RECENT_CALLS, CONTACTS
    }
}
