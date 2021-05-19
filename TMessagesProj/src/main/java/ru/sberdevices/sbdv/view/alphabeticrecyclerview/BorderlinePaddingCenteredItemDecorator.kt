package ru.sberdevices.sbdv.view.alphabeticrecyclerview

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 *  Add border padding for first and last item
*/
class BorderlinePaddingCenteredItemDecorator : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        val itemPosition = parent.getChildAdapterPosition(view)

        if (itemPosition == RecyclerView.NO_POSITION) {
            return
        }
        val itemCount = state.itemCount

        if (itemPosition == 0) {
            outRect.set(parent.width / 2, view.paddingTop, view.paddingRight, view.paddingBottom)
        } else if (itemCount > 0 && itemPosition == itemCount - 1) {
            outRect.set(view.paddingLeft, view.paddingTop, parent.width / 2, view.paddingBottom)
        } else {
            outRect.set(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom)
        }
    }
}