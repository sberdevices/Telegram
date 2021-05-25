package ru.sberdevices.sbdv

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.exoplayer2.util.Log
import ru.sberdevices.analytics.Analytics
import ru.sberdevices.sbdv.analytics.AnalyticsCollector
import ru.sberdevices.sbdv.analytics.AnalyticsCollectorFactory
import ru.sberdevices.sbdv.appstate.AppStateRepository
import ru.sberdevices.sbdv.config.Config
import ru.sberdevices.sbdv.config.LocalConfig
import ru.sberdevices.sbdv.contacts.ContactsRepository
import ru.sberdevices.sbdv.util.AnalyticsStub
import ru.sberdevices.sbdv.util.CallManagerStub
import ru.sberdevices.sbdv.util.DreamingEventsReceiver
import ru.sberdevices.sbdv.util.VoiceQualityEnhancerStub
import ru.sberdevices.sbdv.viewmodel.VoIPModel
import ru.sberdevices.sdk.echocancel.VoiceQualityEnhancer
import ru.sberdevices.sdk.echocancel.VoiceQualityEnhancerFactory
import ru.sberdevices.services.calls.CallManager
import ru.sberdevices.services.calls.CallManagerFactory
import ru.sberdevices.settings.Settings

private const val TAG = "SbdvServiceLocator"

/**
 * Self-made simple ServiceLocator instead of using DI
 */
@SuppressLint("StaticFieldLeak")
object SbdvServiceLocator {

    private lateinit var context: Context

    @JvmStatic
    fun init(context: Context) {
        this.context = context.applicationContext
        dreamingEventsReceiver.startListen()
        getAppStateRepository() // We need to create an instance
        config
    }

    private val analytics: Analytics by lazy {
        try {
            Analytics.create(context)
        } catch (t: Throwable) {
            Log.w(TAG, "AnalyticsStub created instead of a real one")
            AnalyticsStub()
        }
    }

    private val analyticsSdk by lazy { AnalyticsCollectorFactory.getAnalyticsSdk(analytics) }

    private val voipModelInstance: VoIPModel by lazy { VoIPModel(analyticsSdk) }

    private val dreamingEventsReceiver: DreamingEventsReceiver by lazy { DreamingEventsReceiver(context) }

    private val contactsRepository: ContactsRepository by lazy { ContactsRepository() }

    private val localConfig by lazy { LocalConfig(context) }

    private val stateRepository: AppStateRepository by lazy { AppStateRepository(context) }

    private val settings: Settings by lazy { Settings(context) }

    @JvmStatic
    fun getAnalyticsSdkSharedInstance(): AnalyticsCollector = analyticsSdk

    @JvmStatic
    fun getVoIPModelSharedInstance(): VoIPModel = voipModelInstance

    @JvmStatic
    val config: Config by lazy { Config(context) }

    @JvmStatic
    fun getDreamingEventsReceiverSharedInstance(): DreamingEventsReceiver = dreamingEventsReceiver

    @JvmStatic
    fun getContactsRepositorySharedInstance(): ContactsRepository = contactsRepository

    @JvmStatic
    fun getLocalConfigSharedInstance(): LocalConfig = localConfig

    @JvmStatic
    fun getAppStateRepository(): AppStateRepository = stateRepository

    @JvmStatic
    fun getSettingsSharedInstance(): Settings = settings

    @JvmStatic
    fun getCallManager(): CallManager {
        return try {
            CallManagerFactory.create(context)
        } catch (t: Throwable) {
            Log.w(TAG, "CallManagerStub created instead of a real one")
            CallManagerStub()
        }
    }

    @JvmStatic
    fun getVoiceQualityEnhancer(): VoiceQualityEnhancer {
        return try {
            VoiceQualityEnhancerFactory.create(context)
        } catch (t: Throwable) {
            Log.w(TAG, "VoiceQualityEnhancerStub created instead of a real one")
            VoiceQualityEnhancerStub()
        }
    }
}
