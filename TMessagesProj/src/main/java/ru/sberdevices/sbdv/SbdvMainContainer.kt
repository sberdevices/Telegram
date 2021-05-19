package ru.sberdevices.sbdv

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import org.telegram.messenger.R
import ru.sberdevices.sbdv.appstate.AppStateRepository
import ru.sberdevices.sbdv.appstate.ScreenState
import ru.sberdevices.sbdv.model.AppEvent
import ru.sberdevices.sbdv.search.ContactSearchFragment

private const val TAG = "SbdvMainContainer"

private const val FRAGMENT_TAG_MAIN = "main"
private const val FRAGMENT_TAG_CONTACT_SEARCH = "contact_search"
private const val INTENT_ACTION_MAKE_CALL = "ru.sberdevices.telegramcalls.action.MAKE_CALL"
private const val INTENT_EXTRA_CALLEE_ID = "callee_id"

class SbdvMainContainer(context: Context) : FrameLayout(context) {

    private val fragmentManager = (context as AppCompatActivity).supportFragmentManager
    private val stateAppRepository = SbdvServiceLocator.getAppStateRepository()

    private val voipModel = SbdvServiceLocator.getVoIPModelSharedInstance()

    private val stateProvider = object : AppStateRepository.ScreenStateProvider {
        override fun getState(): ScreenState? {
            val searchFragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG_CONTACT_SEARCH)
            val mainFragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG_MAIN)
            return when {
                searchFragment != null -> (searchFragment as ContactSearchFragment).getScreenState()
                mainFragment != null -> (mainFragment as MainFragment).getScreenState()
                else -> null
            }
        }
    }

    private val analyticsCollector = SbdvServiceLocator.getAnalyticsSdkSharedInstance()

    init {
        LayoutInflater.from(context).inflate(R.layout.sbdv_container, this, true)
    }

    fun onStart() {
        stateAppRepository.setScreenStateProvider(stateProvider)

        if (ContactSearchFragment.hasContactsExtra(context)) {
            handleIntent((context as AppCompatActivity).intent)
        } else {
            val searchFragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG_CONTACT_SEARCH)
            if (searchFragment != null) fragmentManager.popBackStack()
        }
    }

    fun onStop() {
        stateAppRepository.setScreenStateProvider(null)
    }

    fun onBackPressed(): Boolean {
        val searchFragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG_CONTACT_SEARCH)
        if (searchFragment != null) {
            (context as AppCompatActivity).intent.removeExtra(ContactSearchFragment.CONTACTS_EXTRA)
            fragmentManager.popBackStack()
            return true
        }

        return false
    }

    fun onNewIntent(intent: Intent) {
        Log.d(TAG, "onNewIntent")

        (context as AppCompatActivity).intent = intent

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        Log.d(TAG, "handleIntent")

        if (ContactSearchFragment.hasContactsExtra(context)) {
            val searchFragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG_CONTACT_SEARCH)
            if (searchFragment == null) {
                fragmentManager.beginTransaction()
                    .replace(
                        R.id.mainFragmentView,
                        ContactSearchFragment::class.java,
                        null,
                        FRAGMENT_TAG_CONTACT_SEARCH
                    )
                    .addToBackStack("contact_search_state")
                    .commit()

                analyticsCollector.onAppEvent(AppEvent.SHOW_VOICE_SEARCH_RESULT_LIST)

                analyticsCollector.onAppEvent(AppEvent.SHOW_VOICE_SEARCH_RESULT_LIST)
            } else {
                (searchFragment as ContactSearchFragment).onNewIntent(intent)
            }
        } else if (intent.containsMakeCallAction()) {
            Log.d(TAG, "MAKE_CALL action received")
            try {
                val calleeId = intent.getStringExtra(INTENT_EXTRA_CALLEE_ID)!!.toInt()
                val isSearchFragmentVisible = fragmentManager.findFragmentByTag(FRAGMENT_TAG_CONTACT_SEARCH) != null
                voipModel.onOutgoingCallUserIntent(
                    activity = (context as AppCompatActivity),
                    userId = calleeId,
                    afterVoiceSearch = isSearchFragmentVisible,
                    afterVoiceDirectCommand = true
                )
            } catch (e: Exception) {
                when (e) {
                    is NullPointerException, is NumberFormatException -> {
                        Log.e(TAG, "Cannot parse calleeId: ${e.localizedMessage}")
                    }
                    else -> throw e
                }
            }
        } else {
            Log.d(TAG, "No contacts extra or make call action with callee id")
        }
    }

    private fun Intent.containsMakeCallAction(): Boolean = action == INTENT_ACTION_MAKE_CALL && hasExtra(INTENT_EXTRA_CALLEE_ID)
}
