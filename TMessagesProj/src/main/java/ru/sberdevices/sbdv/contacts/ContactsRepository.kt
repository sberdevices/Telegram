package ru.sberdevices.sbdv.contacts

import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.telegram.messenger.ContactsController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.UserConfig
import ru.sberdevices.sbdv.model.Contact
import ru.sberdevices.sbdv.util.startCoroutineTimer

private const val TAG = "ContactsRepository"
private const val UPDATE_INTERVAL_MS = 180_000L

class ContactsRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _contacts = MutableLiveData<List<Contact>>()
    val contacts: LiveData<List<Contact>> = _contacts.distinctUntilChanged()

    /**
     * Calling contacts update after contactsDidLoad event and unsubscribe because after the first event new
     * unnecessary contactsDidLoad events will be called several times per second
     */
    private val oneTimeContactsLoadObserver = object : NotificationCenter.NotificationCenterDelegate {
        override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
            Log.d(TAG, "on contactsDidLoad")
            scope.launch {
                updateFromMessagesController()
            }
            NotificationCenter.getInstance(UserConfig.selectedAccount)
                .removeObserver(this, NotificationCenter.contactsDidLoad)
        }
    }

    /**
     * Calling contact loading observing after mainUserInfoChanged event (when user log-in)
     */
    private val userInfoChangeObserver = object : NotificationCenter.NotificationCenterDelegate {
        override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
            Log.d(TAG, "on mainUserInfoChanged")
            observeOnceContactsDidLoad()
        }
    }

    init {
        Log.d(TAG, "init()")

        scope.startCoroutineTimer(UPDATE_INTERVAL_MS, UPDATE_INTERVAL_MS) {
            scope.launch {
                updateContactsFromServer()
            }
        }

        NotificationCenter.getInstance(UserConfig.selectedAccount)
            .addObserver(userInfoChangeObserver, NotificationCenter.mainUserInfoChanged)

        observeOnceContactsDidLoad()

        scope.launch {
            updateFromMessagesController()
        }
    }

    private fun observeOnceContactsDidLoad(){
        NotificationCenter.getInstance(UserConfig.selectedAccount)
            .addObserver(oneTimeContactsLoadObserver, NotificationCenter.contactsDidLoad)
    }

    @AnyThread
    private fun updateFromMessagesController() {
        val contacts = ContactsController.getInstance(UserConfig.selectedAccount).usersSectionsDict.values
            .flatten()
            .asSequence()
            .map { MessagesController.getInstance(UserConfig.selectedAccount).getUser(it.user_id) }
            .filter { user ->
                !user.bot && !user.deleted && !user.support && !user.self &&
                    (!user.first_name.isNullOrEmpty() || !user.last_name.isNullOrEmpty() || !user.username.isNullOrEmpty())
            }
            .sortedBy { user -> user.sortWeight }
            .map { user ->
                Contact(
                    id = user.id,
                    firstName = user.first_name,
                    lastName = user.last_name,
                    user = user,
                    displayedNumber = null
                )
            }
            .toList()
        Log.d(TAG, "updateFromMessagesController(); ${contacts.size} contacts")
        _contacts.postValue(contacts)
    }

    private fun loadFromServer() {
        Log.d(TAG, "loadFromServer()")
        ContactsController.getInstance(UserConfig.selectedAccount).loadContacts(false, 0)
    }

    @WorkerThread
    private suspend fun updateContactsFromServer() = withContext(Dispatchers.IO) {
        Log.d(TAG, "updateContactsFromServer()")
        observeOnceContactsDidLoad()
        loadFromServer()
    }
}