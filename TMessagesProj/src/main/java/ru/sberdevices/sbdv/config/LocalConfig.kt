package ru.sberdevices.sbdv.config

import android.content.Context
import androidx.core.content.edit
import com.google.android.exoplayer2.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LocalConfiguration(val developerModeEnabled: Boolean, val mockCallsOn: Boolean)

private const val TAG = "LocalConfig"

class LocalConfig(context: Context) {

    private val sharedPreferences =
        context.applicationContext.getSharedPreferences(LOCAL_CONFIG_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)

    private val localConfigMutableStateFlow = MutableStateFlow<LocalConfiguration>(
        readPreferences()
    )
    val localConfigStateFlow: StateFlow<LocalConfiguration> = localConfigMutableStateFlow

    private fun readPreferences(): LocalConfiguration {
        val developerModeEnabled = sharedPreferences.getBoolean(DEVELOPER_MODE_ENABLED_KEY, false)
        val mockCallsOn = sharedPreferences.getBoolean(MOCK_CALL_ON_KEY, false) && developerModeEnabled
        val localConfiguration = LocalConfiguration(developerModeEnabled, mockCallsOn)
        Log.d(TAG, "localConfiguration = $localConfiguration")
        return localConfiguration
    }

    @Synchronized
    fun onEnableDeveloperMode(developerModeEnabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(DEVELOPER_MODE_ENABLED_KEY, developerModeEnabled)
        }
        localConfigMutableStateFlow.value = readPreferences()
    }

    @Synchronized
    fun onEnableMockCalls(mockCallsOn: Boolean) {
        sharedPreferences.edit {
            putBoolean(MOCK_CALL_ON_KEY, mockCallsOn)
        }
        localConfigMutableStateFlow.value = readPreferences()
    }

    private companion object {
        const val LOCAL_CONFIG_SHARED_PREFERENCES_KEY = "LOCAL_CONFIG_SHARED_PREFERENCES_KEY"
        const val DEVELOPER_MODE_ENABLED_KEY = "DEVELOPER_MODE_ENABLED_KEY"
        const val MOCK_CALL_ON_KEY = "MOCK_CALL_ON_KEY"
    }
}