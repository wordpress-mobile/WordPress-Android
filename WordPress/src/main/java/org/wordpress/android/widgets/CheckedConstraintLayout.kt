package org.wordpress.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.CheckBox
import android.widget.Checkable
import androidx.constraintlayout.widget.ConstraintLayout

class CheckedConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), Checkable {
    private lateinit var checkBox: CheckBox
    override fun onFinishInflate() {
        super.onFinishInflate()
        val childCount = childCount
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            if (view is CheckBox) {
                checkBox = view
            }
        }
    }

    override fun isChecked() = checkBox.isChecked

    override fun toggle() = checkBox.toggle()

    override fun setChecked(checked: Boolean) {
        checkBox.isChecked = checked
    }
}

