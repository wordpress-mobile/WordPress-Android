@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.prefs

import android.content.Context
import android.content.res.Resources
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.TextViewCompat
import com.google.android.material.progressindicator.LinearProgressIndicator
import org.wordpress.android.R

/**
 * A preference that displays quota information with a text summary and a progress bar.
 * Used to show media storage quota usage in site settings.
 */
class QuotaPreference(
    context: Context,
    attrs: AttributeSet
) : Preference(context, attrs), PreferenceHint {
    private var hint: String? = null
    private var progress: Int = MIN_PROGRESS

    init {
        layoutResource = R.layout.quota_preference

        context.obtainStyledAttributes(attrs, R.styleable.QuotaPreference).apply {
            for (i in 0 until indexCount) {
                when (getIndex(i)) {
                    R.styleable.QuotaPreference_longClickHint -> hint = getString(i)
                }
            }
            recycle()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBindView(view: View) {
        super.onBindView(view)

        val res = context.resources

        view.findViewById<TextView>(android.R.id.title)?.apply {
            TextViewCompat.setTextAppearance(
                this,
                com.google.android.material.R.style.TextAppearance_MaterialComponents_Subtitle1
            )
            updateAlphaForEnabledState(res)
        }

        view.findViewById<TextView>(android.R.id.summary)?.apply {
            TextViewCompat.setTextAppearance(
                this,
                com.google.android.material.R.style.TextAppearance_MaterialComponents_Body2
            )
            updateAlphaForEnabledState(res)
        }

        view.findViewById<LinearProgressIndicator>(R.id.quota_progress)?.apply {
            if (progress < 0) {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                setProgress(progress)
                updateAlphaForEnabledState(res)
            }
        }
    }

    private fun View.updateAlphaForEnabledState(res: Resources) {
        alpha = if (!isEnabled) {
            ResourcesCompat.getFloat(
                res,
                com.google.android.material.R.dimen.material_emphasis_disabled
            )
        } else {
            1f
        }
    }

    /**
     * Sets the progress value for the quota progress bar.
     * @param value The progress value (0-100), or negative for unlimited storage
     */
    fun setProgress(value: Int) {
        val newProgress = if (value < 0) value else value.coerceIn(MIN_PROGRESS, MAX_PROGRESS)
        if (progress != newProgress) {
            progress = newProgress
            notifyChanged()
        }
    }

    /**
     * Gets the current progress value.
     * @return The progress value (0-100)
     */
    fun getProgress(): Int = progress

    override fun hasHint(): Boolean = !hint.isNullOrEmpty()

    override fun getHint(): String? = hint

    override fun setHint(hint: String?) {
        this.hint = hint
    }

    companion object {
        private const val MIN_PROGRESS = 0
        private const val MAX_PROGRESS = 100
    }
}
