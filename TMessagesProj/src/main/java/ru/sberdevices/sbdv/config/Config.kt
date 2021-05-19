package ru.sberdevices.sbdv.config

import android.content.Context
import android.util.Size
import androidx.annotation.AnyThread
import androidx.lifecycle.asFlow
import androidx.lifecycle.distinctUntilChanged
import com.google.android.exoplayer2.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.json.JSONException
import org.json.JSONObject
import ru.sberdevices.appconfig.ApplicationConfigProvider

private val DEFAULT_CROP_RESOLUTION = Size(736, 414)

// It's effectively a singleton, so we don't want to close resources
@AnyThread
class Config(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var _voipProtocolVersion: Int = 0
    private var _voipFeedbackDisplayProbability: Float = 0f
    private var _voipFeedbackDisplayingControlledByTelegram: Boolean = true
    private var _voipCropResolution = DEFAULT_CROP_RESOLUTION

    private val applicationConfigProvider by lazy {
        ApplicationConfigProvider.create(context.applicationContext)
    }

    private val configChangesFlow by lazy {
        applicationConfigProvider.configLiveData
            .distinctUntilChanged()
            .asFlow()
            .map { config -> config.parseToJsonObject() }
            .flowOn(Dispatchers.IO)
    }

    init {
        Log.d(TAG, "<init>@" + hashCode())

        applicationConfigProvider.configLiveData
            .distinctUntilChanged()
            .asFlow()
            .map { jsonString -> jsonString.parseToJsonObject() }
            .onEach { config ->
                synchronized(this@Config) {
                    _voipProtocolVersion = config.optInt("voipProtocolVersion")
                    _voipFeedbackDisplayProbability =
                        config.optDouble(KEY_VOIP_FEEDBACK_DISPLAY_PROBABILITY, .0).toFloat()
                    _voipFeedbackDisplayingControlledByTelegram =
                        config.optBoolean(KEY_VOIP_FEEDBACK_DISPLAYING_CONTROLLED_BY_TELEGRAM, true)

                    config.optJSONObject("voipCropResolution")?.let {
                        val width = it.optInt("width", DEFAULT_CROP_RESOLUTION.width)
                        val height = it.optInt("height", DEFAULT_CROP_RESOLUTION.height)
                        _voipCropResolution = Size(width, height)
                    }
                }
            }
            .launchIn(scope)
    }

    val watchPartyEnabledFlow: Flow<Boolean> by lazy {
        configChangesFlow.map { configJson -> configJson.optBoolean(KEY_WATCH_PARTY_ENABLED, false) }
    }

    val voipProtocolVersion: Int
        @Synchronized
        get() = _voipProtocolVersion

    val voipFeedbackDisplayProbability: Float
        @Synchronized
        get() = _voipFeedbackDisplayProbability

    val voipFeedbackDisplayingControlledByTelegram: Boolean
        @Synchronized
        get() = _voipFeedbackDisplayingControlledByTelegram

    val voipCropResolution: Size
        @Synchronized
        get() = _voipCropResolution

    private fun String.parseToJsonObject(): JSONObject {
        Log.d(TAG, "New app config: $this")
        return try {
            JSONObject(this)
        } catch (e: JSONException) {
            Log.e(TAG, "JSONException occurred on parsing application config", e)
            JSONObject()
        }
    }

    private companion object {
        const val TAG = "Config"
        const val IS_SBERDEVICE = true
        private const val KEY_WATCH_PARTY_ENABLED = "watchPartyEnabled"
        private const val KEY_VOIP_FEEDBACK_DISPLAY_PROBABILITY = "voipFeedbackDisplayProbability"
        private const val KEY_VOIP_FEEDBACK_DISPLAYING_CONTROLLED_BY_TELEGRAM =
            "voipFeedbackDisplayingControlledByTelegram"
    }
}
