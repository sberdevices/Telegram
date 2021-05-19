package org.telegram.messenger.voip

import org.webrtc.CapturerObserver
import org.webrtc.VideoFrame
import ru.sberdevices.sbdv.SbdvServiceLocator
import ru.sberdevices.smartfocus.SmartFocusTracker

class VideoCropper(private val capturerObserver: CapturerObserver) : CapturerObserver {

    private val resolution = SbdvServiceLocator.config.voipCropResolution

    override fun onCapturerStarted(success: Boolean) {
        capturerObserver.onCapturerStarted(success)
    }

    override fun onCapturerStopped() {
        capturerObserver.onCapturerStopped()
    }

    override fun onFrameCaptured(frame: VideoFrame?) {
        val croppedFrame = createCroppedFrame(frame)
        capturerObserver.onFrameCaptured(croppedFrame)
        croppedFrame?.release()
    }

    private fun createCroppedFrame(frame: VideoFrame?): VideoFrame? {
        var croppedFrame: VideoFrame? = null
        if (frame != null) {
            val cropRect = focusTracker.onFrame()
            val mirroredLeft = frame.buffer.width - cropRect.right
            val croppedBuffer = frame.buffer.cropAndScale(
                mirroredLeft,
                cropRect.top,
                cropRect.width(),
                cropRect.height(),
                resolution.width,
                resolution.height
            )
            croppedFrame = VideoFrame(croppedBuffer, frame.rotation, frame.timestampNs)
        }
        return croppedFrame
    }

    companion object {

        @JvmStatic
        fun setSmartFocusEnabled(enable: Boolean) {
            focusTracker.setEnabled(enable)
        }

        @JvmField
        val focusTracker = SmartFocusTracker(VideoCameraCapturer.CAPTURE_WIDTH, VideoCameraCapturer.CAPTURE_HEIGHT)
    }
}
