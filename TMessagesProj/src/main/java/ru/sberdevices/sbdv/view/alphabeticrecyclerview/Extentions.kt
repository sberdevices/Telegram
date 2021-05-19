package ru.sberdevices.sbdv.view.alphabeticrecyclerview

import android.content.Context

fun Context.pxToSp(px: Int): Float {
    val scaledDensity = resources.displayMetrics.scaledDensity
    return px / scaledDensity
}

fun Context.spToPx(sp: Int): Int {
    val scaledDensity = resources.displayMetrics.scaledDensity
    return (sp * scaledDensity).toInt()
}