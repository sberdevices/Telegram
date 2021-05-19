package ru.sberdevices.sbdv.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.annotation.MainThread
import androidx.annotation.Px
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.telegram.messenger.ImageLocation
import org.telegram.messenger.ImageReceiver
import org.telegram.tgnet.TLRPC
import ru.sberdevices.sbdv.model.Contact
import ru.sberdevices.sbdv.view.AvatarPlaceholder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import ru.sberdevices.textprocessing.transliteration.Transliterator

fun TLRPC.User.getFullName() = "${first_name ?: ""} ${last_name ?: ""}"

fun TLRPC.User.getAvatarBitmap(@Px size: Int): Bitmap? {
    val imageLocation = ImageLocation.getForUser(this, false)
    return if (imageLocation != null) {
        val imageReceiver = ImageReceiver(null)
        val avatarPlaceholder = AvatarPlaceholder().apply {
            setName(first_name, last_name)
        }
        imageReceiver.setImage(
            imageLocation,
            null,
            avatarPlaceholder,
            null,
            this,
            0
        )
        val irBitmap = imageReceiver.bitmap
        val bitmap = if (irBitmap != null) Bitmap.createScaledBitmap(irBitmap, size, size, true) else null
        imageReceiver.clearImage()
        return bitmap
    } else {
        Log.w("getImageReceiver", "imageLocation for user $first_name $last_name is null")
        null
    }
}

fun Date.toFormattedString(format: String = "dd MMMM HH:mm", locale: Locale = Locale.forLanguageTag("ru")): String {
    val formatter = SimpleDateFormat(format, locale)
    return formatter.format(this)
}

fun Char.isEnglishLetter(): Boolean {
    return this in 'a'..'z' || this in 'A'..'Z'
}

fun Char.isRussianLetter(): Boolean {
    return this in 'а'..'я' || this in 'А'..'Я'
}

val Char.asciiCode: Int
    get() = this.toByte().toInt()

fun List<Any>.isSameList(list2: List<Any>): Boolean {
    if (size != list2.size)
        return false

    val pairList = this.zip(list2)

    return pairList.all { (elt1, elt2) ->
        elt1 == elt2
    }
}

fun <T> LiveData<T>.observeOnce(observer: (T) -> Unit) {
    val mediatorObserver = object : Observer<T> {
        override fun onChanged(t: T) {
            observer(t)
            removeObserver(this)
        }
    }
    observeForever(mediatorObserver)
}

@MainThread
fun PopupWindow.dimBehind(dimAmount: Float) {
    val container = contentView.rootView
    val context = contentView.context
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val p = container.layoutParams as WindowManager.LayoutParams
    p.flags = p.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
    p.dimAmount = dimAmount
    wm.updateViewLayout(container, p)
}

inline fun CoroutineScope.startCoroutineTimer(
    delayMillis: Long = 0,
    repeatMillis: Long = 0,
    crossinline action: () -> Unit
) = this.launch {
    delay(delayMillis)
    if (repeatMillis > 0) {
        while (isActive) {
            action()
            delay(repeatMillis)
        }
    } else {
        action()
    }
}

object OnClickListenerWrapper {
    const val THROTTLE_DELAY_MS = 500 //by default

    @JvmStatic
    @JvmOverloads
    fun throttleFirst(delay: Int = THROTTLE_DELAY_MS, onClickListener: View.OnClickListener): View.OnClickListener {
        return object : View.OnClickListener {

            var lastTime = 0L

            override fun onClick(v: View?) {
                val currentTime = System.currentTimeMillis()
                val mayDispatch = currentTime - lastTime >= delay
                if (mayDispatch) {
                    lastTime = currentTime
                    onClickListener.onClick(v)
                }
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun throttleFirst(delay: Int = THROTTLE_DELAY_MS, onClickListener: (View?) -> Unit): View.OnClickListener {
        val wrapper: View.OnClickListener = View.OnClickListener(onClickListener)
        return throttleFirst(delay, wrapper)
    }
}
