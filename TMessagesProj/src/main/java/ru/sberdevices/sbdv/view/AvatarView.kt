package ru.sberdevices.sbdv.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.cardview.widget.CardView
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ImageLocation
import org.telegram.messenger.ImageReceiver
import org.telegram.tgnet.TLRPC

class AvatarView(context: Context, attrs: AttributeSet) : CardView(context, attrs) {
    private val image = ImageReceiver(this)
    private val avatarPlaceholder = AvatarPlaceholder(
        textColor = placeholderTextColor,
        textTypeFace = placeholderTypeFace
    )

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        image.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        image.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        image.setImageCoords(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        image.draw(canvas)
    }

    fun setUser(user: TLRPC.User) {
        if (user === image.parentObject) return

        avatarPlaceholder.setName(user.first_name, user.last_name)
        image.setImage(
            ImageLocation.getForUser(user, false),
            null,
            avatarPlaceholder,
            null,
            user,
            0
        )
        invalidate()
    }

    private companion object {
        const val placeholderTextColor = Color.WHITE
        val placeholderTypeFace: Typeface = AndroidUtilities.getTypeface("fonts/SBSansText-Regular.ttf") ?: Typeface.SANS_SERIF
    }
}
