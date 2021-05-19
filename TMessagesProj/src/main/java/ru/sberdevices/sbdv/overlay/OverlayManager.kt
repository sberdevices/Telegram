package ru.sberdevices.sbdv.overlay

import android.app.Application
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.google.android.exoplayer2.util.Log

private const val TAG = "OverlayManager"

class OverlayManager(application: Application) {

    private val windowManager = application.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: View? = null

    fun replaceOverlay(view: View) {
        Log.d(TAG, "showOverlayView()")

        if (overlayView != null) windowManager.removeView(overlayView)
        overlayView = view

        val params = buildLayoutParams()
        windowManager.addView(view, params)
    }

    fun removeOverlay() {
        Log.d(TAG, "removeOverlayView()")

        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }
}
