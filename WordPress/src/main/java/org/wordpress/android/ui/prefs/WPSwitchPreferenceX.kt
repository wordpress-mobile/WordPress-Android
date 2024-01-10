package org.wordpress.android.ui.prefs

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.text.TextUtils
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreference
import org.wordpress.android.R

/** AndroidX implementation of [WPSwitchPreference]
 * @see WPSwitchPreference*/
class WPSwitchPreferenceX(context: Context, attrs: AttributeSet?) :
    SwitchPreference(context, attrs), PreferenceHint {
    private var mHint: String? = null
    private var mTint: ColorStateList? = null
    private var mThumbTint: ColorStateList? = null
    private var mStartOffset = 0

    init {
        val array = context.obtainStyledAttributes(attrs, R.styleable.SummaryEditTextPreference)
        for (i in 0 until array.indexCount) {
            val index = array.getIndex(i)
            if (index == R.styleable.SummaryEditTextPreference_longClickHint) {
                mHint = array.getString(index)
            } else if (index == R.styleable.SummaryEditTextPreference_iconTint) {
                val resourceId = array.getResourceId(index, 0)
                if (resourceId != 0) {
                    mTint = AppCompatResources.getColorStateList(context, resourceId)
                }
            } else if (index == R.styleable.SummaryEditTextPreference_switchThumbTint) {
                mThumbTint = array.getColorStateList(index)
            } else if (index == R.styleable.SummaryEditTextPreference_startOffset) {
                mStartOffset = array.getDimensionPixelSize(index, 0)
            }
        }
        array.recycle()
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val view = holder.itemView
        val icon = view.findViewById<ImageView>(android.R.id.icon)
        if (icon != null && mTint != null) {
            icon.imageTintList = mTint
        }
        val titleView = view.findViewById<TextView>(android.R.id.title)
        if (titleView != null) {
            val res = context.resources

            // add padding to the start of nested preferences
            if (!TextUtils.isEmpty(dependency)) {
                val margin = res.getDimensionPixelSize(R.dimen.margin_large)
                ViewCompat.setPaddingRelative(titleView, margin + mStartOffset, 0, 0, 0)
            } else {
                ViewCompat.setPaddingRelative(titleView, mStartOffset, 0, 0, 0)
            }
        }

        // style custom switch preference
        val switchControl = getSwitch(view as ViewGroup)
        if (switchControl != null) {
            if (mThumbTint != null) {
                switchControl.thumbTintList = mThumbTint
            }
        }

        // Add padding to start of switch.
        ViewCompat.setPaddingRelative(
            getSwitch(view)!!,
            context.resources.getDimensionPixelSize(R.dimen.margin_extra_large), 0, 0, 0
        )
    }
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun getSwitch(parentView: ViewGroup): Switch? {
        for (i in 0 until parentView.childCount) {
            val childView = parentView.getChildAt(i)
            if (childView is Switch) {
                return childView
            } else if (childView is ViewGroup) {
                val theSwitch = getSwitch(childView)
                if (theSwitch != null) {
                    return theSwitch
                }
            }
        }
        return null
    }

    override fun hasHint(): Boolean {
        return !TextUtils.isEmpty(mHint)
    }

    override fun getHint(): String {
        return mHint!!
    }

    override fun setHint(hint: String) {
        mHint = hint
    }
}
