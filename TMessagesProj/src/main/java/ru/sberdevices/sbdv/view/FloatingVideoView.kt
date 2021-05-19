package ru.sberdevices.sbdv.view

import android.content.Context
import android.graphics.RectF
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import org.telegram.ui.Components.LayoutHelper
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import kotlin.math.hypot

private const val MIN_DELTA_TO_START_DRAGGING = 10f
private const val DELTA_TIME_MS_TO_CLICK = 500f
private const val SMALL_SIDE_SIZE = 160f
private const val LAYOUT_MARGIN = 32f

class FloatingVideoView(context: Context) : FrameLayout(context), RendererCommon.RendererEvents {
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchStartTime: Long = 0
    private var dragStarted = false

    private val translatingArea = RectF(0f, 0f, 1f, 1f)
    private val relativePosition = Point(1f, 0f)
    private var isLayoutHorizontal = true

    private var uiElementsTopHeight = 0f
    private var uiElementsBottomHeight = 0f

    var onFirstFrameRenderedCallback: (() -> Unit)? = null

    val renderer = SurfaceViewRenderer(context)
    var isUiVisible = true
        set(value) {
            field = value
            updateTranslatingArea()
        }

    init {
        pivotX = 0f
        pivotY = 0f
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED)
        addView(
            renderer,
            LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER)
        )
    }

    @JvmOverloads
    fun init(context: EglBase.Context, onFirstFrameRenderedCallback: (() -> Unit)? = null) {
        renderer.init(context, this)
        this.onFirstFrameRenderedCallback = onFirstFrameRenderedCallback
    }

    override fun onFirstFrameRendered() {
        onFirstFrameRenderedCallback?.invoke()
    }

    override fun onFrameResolutionChanged(videoWidth: Int, videoHeight: Int, rotation: Int) {
        post {
            val currentWidth = if (layoutParams.width > 0) layoutParams.width else measuredWidth
            val currentHeight = if (layoutParams.height > 0) layoutParams.height else measuredHeight
            val isNewFrameHorizontal = videoWidth >= videoHeight
            isLayoutHorizontal = currentWidth >= currentHeight
            if (isLayoutHorizontal != isNewFrameHorizontal) {
                // обновлять вьюшку нужно лишь при смене ориентации
                // потому что в процессе звонка скачет разрешение видео, а не его пропорции
                layoutParams.width = currentHeight
                layoutParams.height = currentWidth
                isLayoutHorizontal = isNewFrameHorizontal
                requestLayout()
                updateTranslatingArea()
                updatePosition()
            }
        }
    }

    fun setUiElementHeight(topUiElementsHeight: Float, bottomUiElementsHeight: Float) {
        uiElementsTopHeight = topUiElementsHeight
        uiElementsBottomHeight = bottomUiElementsHeight
    }

    private fun updateTranslatingArea() {
        val scaledWidth = measuredWidth * scaleX
        val scaledHeight = measuredHeight * scaleY
        val uiLayoutTopMargin = if (isUiVisible) uiElementsTopHeight else 0f
        val uiLayoutBottomMargin = if (isUiVisible) uiElementsBottomHeight else 0f
        translatingArea.left = LAYOUT_MARGIN
        translatingArea.right = (parent as View).width - scaledWidth - LAYOUT_MARGIN
        translatingArea.top = uiLayoutTopMargin + LAYOUT_MARGIN
        translatingArea.bottom = (parent as View).height - scaledHeight - LAYOUT_MARGIN - uiLayoutBottomMargin
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val width = right - left
        val height = bottom - top
        val scale = SMALL_SIDE_SIZE / if (isLayoutHorizontal) height.toFloat() else width.toFloat()
        scaleX = scale
        scaleY = scale
        updateTranslatingArea()
        updatePosition()
    }

    private fun setTranslation(x: Float, y: Float) {
        val newX = x.coerceIn(translatingArea.left, translatingArea.right)
        val newY = y.coerceIn(translatingArea.top, translatingArea.bottom)
        relativePosition.x = (newX - translatingArea.left) / translatingArea.width()
        relativePosition.y = (newY - translatingArea.top) / translatingArea.height()
    }

    private fun updatePosition() {
        translationX = translatingArea.left + relativePosition.x * translatingArea.width()
        translationY = translatingArea.top + relativePosition.y * translatingArea.height()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val parent = parent
        val action = event.action
        val currentTime = System.currentTimeMillis()
        val x = event.rawX
        val y = event.rawY
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartTime = currentTime
                touchStartX = x
                touchStartY = y
            }
            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                if (!dragStarted) {
                    val distance = hypot(x - touchStartX, y - touchStartY)
                    if (distance > MIN_DELTA_TO_START_DRAGGING) {
                        dragStarted = true
                    }
                }
                if (dragStarted) {
                    val newX = translationX + x - touchStartX
                    val newY = translationY + y - touchStartY
                    setTranslation(newX, newY)
                    updatePosition()
                    touchStartX = x
                    touchStartY = y
                }
            }
            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (!dragStarted && (currentTime - touchStartTime < DELTA_TIME_MS_TO_CLICK)) {
                    performClick()
                }
                dragStarted = false
            }
        }
        return true
    }

    private data class Point(var x: Float, var y: Float)
}