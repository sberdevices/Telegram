package ru.sberdevices.sbdv.config

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.sberdevices.appconfig.ApplicationConfigProvider

interface AppConfigProvider {
    val configLiveData: LiveData<String>
}

class AppConfigProviderImpl(context: Context) : AppConfigProvider {

    private val provider = ApplicationConfigProvider.create(context.applicationContext)

    override val configLiveData = provider.configLiveData
}

class AppConfigProviderStub : AppConfigProvider {
    override val configLiveData = MutableLiveData<String>()
}
