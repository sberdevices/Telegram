package ru.sberdevices.sbdv.appstate

import android.content.Context
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import ru.sberdevices.sbdv.model.Contact
import ru.sberdevices.services.appstate.AppStateManagerFactory
import ru.sberdevices.services.appstate.AppStateProvider
import ru.sberdevices.textprocessing.transliteration.BgnLikeTransliterator
import ru.sberdevices.textprocessing.transliteration.Transliterator

@MainThread
class AppStateRepository(context: Context) {

    private val stateManager = AppStateManagerFactory.createRequestManager(context)

    private val transliterator = BgnLikeTransliterator()

    private val stateProvider = object : AppStateProvider {
        @AnyThread
        override fun getState(): String {
            val (screenState, callState, isAuthenticated) = runBlocking(Dispatchers.Main) {
                Triple(
                    screenStateProvider?.getState(),
                    callStateProvider?.getState(),
                    authStateProvider?.isAuthenticated()
                )
            }

            val json = screenState?.contacts?.toJson(transliterator) ?: JSONObject()

            with(callState ?: CallState()) {
                json.put("cameraEnabled", cameraEnabled)
                    .put("microphoneEnabled", microphoneEnabled)
                    .put("callState", state.toJson())
            }

            json.put("isAuthenticated", isAuthenticated ?: false)

            return json.toString()
        }
    }

    private var screenStateProvider: ScreenStateProvider? = null
    private var callStateProvider: CallStateProvider? = null
    private var authStateProvider: AuthStateProvider? = null

    init {
        stateManager.setProvider(stateProvider)
    }

    fun setScreenStateProvider(provider: ScreenStateProvider?) {
        screenStateProvider = provider
    }

    fun setCallStateProvider(provider: CallStateProvider?) {
        callStateProvider = provider
    }

    fun setAuthStateProvider(provider: AuthStateProvider?) {
        authStateProvider = provider
    }

    @MainThread
    interface ScreenStateProvider {
        fun getState(): ScreenState?
    }

    @MainThread
    interface CallStateProvider {
        fun getState(): CallState
    }

    @MainThread
    interface AuthStateProvider {
        fun isAuthenticated(): Boolean
    }
}

data class ScreenState(val contacts: List<Contact>?)

data class CallState(
    val cameraEnabled: Boolean = false,
    val microphoneEnabled: Boolean = false,
    val state: State = State.NONE,
) {

    enum class State {
        NONE,
        RINGING,
        DIALING,
        ACTIVE
    }
}

private fun CallState.State.toJson(): String {
    return when (this) {
        CallState.State.NONE -> "none"
        CallState.State.RINGING -> "ringing"
        CallState.State.DIALING -> "dialing"
        CallState.State.ACTIVE -> "active"
    }
}

private fun List<Contact>.toJson(transliterator: Transliterator): JSONObject {
    val ignoredWords = JSONArray()
        .put("позвони")
        .put("набери")

    val itemSelector = JSONObject()
        .put("ignored_words", ignoredWords)

    val root = JSONObject()
        .put("item_selector", itemSelector)

    val array = JSONArray()
    for ((index, contact) in this.withIndex()) {
        val transliteratedFirstName = contact.firstName?.let { transliterator.transliterate(it) }
        val transliteratedLastName = contact.lastName?.let { transliterator.transliterate(it) }

        val contactJson = JSONObject()
            .put("number", index + 1)
            .put("id", contact.id.toString())
            .put("first_name", transliteratedFirstName)
            .put("last_name", transliteratedLastName)
            .put("visible", true)

        val builder = StringBuilder()
        if (!transliteratedFirstName.isNullOrEmpty()) builder.append(transliteratedFirstName)
        if (!transliteratedFirstName.isNullOrEmpty() && !transliteratedLastName.isNullOrEmpty()) builder.append(" ")
        if (!transliteratedLastName.isNullOrEmpty()) builder.append(transliteratedLastName)
        contactJson.put("title", builder.toString())

        array.put(contactJson)
    }
    itemSelector.put("items", array)

    return root
}
