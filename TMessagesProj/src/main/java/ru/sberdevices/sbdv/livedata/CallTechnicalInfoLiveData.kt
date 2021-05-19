package ru.sberdevices.sbdv.livedata

import android.util.Log
import androidx.lifecycle.MediatorLiveData
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import ru.sberdevices.sbdv.model.CallTechnicalInfo

private val TAG = "CallTechnicalInfoLiveData"

class CallTechnicalInfoLiveData : MediatorLiveData<CallTechnicalInfo>() {

    private var decoderCodecName: String? = null
    private var encoderCodecName: String? = null

    @Synchronized
    private fun checkReadyCodecs() {
        Log.d(TAG, "checkReadyCodecs()")
        if (decoderCodecName != null && encoderCodecName != null) {
            Log.d(TAG, "decoder codec = $decoderCodecName; encoder codec = $encoderCodecName")
            value = CallTechnicalInfo(encoder = encoderCodecName, decoder = decoderCodecName)
        }
    }

    init {
        addSource(
            DefaultVideoDecoderFactory.chosenDecoderCodec
        ) { codecInfo ->
            decoderCodecName = codecInfo.name
            checkReadyCodecs()
        }

        addSource(
            DefaultVideoEncoderFactory.chosenEncoderCodec
        ) { codecInfo ->
            encoderCodecName = codecInfo.name
            checkReadyCodecs()
        }
    }
}