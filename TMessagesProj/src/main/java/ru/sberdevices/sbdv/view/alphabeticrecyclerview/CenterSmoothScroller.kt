package ru.sberdevices.sbdv.view.alphabeticrecyclerview

import android.content.Context
import androidx.recyclerview.widget.LinearSmoothScroller

/**
 *  Make scroll to target item to center of recyclerview
 */
class CenterSmoothScroller(context: Context) : LinearSmoothScroller(context) {
    override fun calculateDtToFit(
        viewStart: Int,
        viewEnd: Int,
        boxStart: Int,
        boxEnd: Int,
        snapPreference: Int
    ): Int = (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)
}
