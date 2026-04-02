package org.wordpress.android.ui.utils

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

object HideItemDivider

fun RecyclerView.addItemDivider(drawable: Drawable) {
    if (layoutManager !is LinearLayoutManager) return
    addItemDecoration(DividerItemDecorator(drawable))
}

fun RecyclerView.addItemDivider(@DrawableRes drawableRes: Int) {
    AppCompatResources.getDrawable(context, drawableRes)?.let { drawable ->
        addItemDivider(drawable)
    }
}

class DividerItemDecorator(private val divider: Drawable) : RecyclerView.ItemDecoration() {
    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val dividerLeft = parent.paddingStart
        val dividerRight = parent.width - parent.paddingEnd
        val childCount = parent.childCount
        for (i in 0..childCount - 2) {
            val child: View = parent.getChildAt(i)
            if (child.tag == HideItemDivider) continue

            val params = child.layoutParams as RecyclerView.LayoutParams
            val dividerTop: Int = child.bottom + params.bottomMargin
            val dividerBottom = dividerTop + (divider.intrinsicHeight)
            divider.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom)
            divider.draw(canvas)
        }
    }
}
