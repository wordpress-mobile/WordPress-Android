package org.wordpress.android.util.helpers

import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.widget.TextSwitcher
import org.wordpress.android.R.anim
import org.wordpress.android.R.layout
import java.lang.ref.WeakReference

/**
 * A simple helper to switch texts used to display progress e.g. of site being created.
 * The helper is a runnable and once started (externally) will keep running until cancelled
 * Each run changes the text to the next one in the sequence (stringIds) in a cyclic manner
 *
 * using a WeakReference for the text switcher will prevent future executions once the view is
 * destroyed (or at least no longer referenced)
 *
 * @param textSwitcher - a week-ref to a text-switcher which displays the sequence
 * @param stringIds - a list of string resource ids to sequence
 * @param delay - how long to delay the next text-change (i.e. how long is each text displayed)
 */
class DynamicTextsProgressionHelper(
    private val textSwitcher: WeakReference<TextSwitcher>,
    private val stringIds: List<Int>,
    val delay: Long
) : Runnable {
    val nText: Int = stringIds.size

    // the count of how many times `nextText` was executed
    var count = -1

    var canceled = false

    init {
        textSwitcher.get()?.apply {
            removeAllViews()
            visibility = View.VISIBLE
            setFactory {
                // inflate a text view that matches the design for this screen
                LayoutInflater.from(context)
                        .inflate(layout.site_creation_progress_text, this, false)
            }
            // set fade in/out animations
            inAnimation = AnimationUtils.loadAnimation(
                    context,
                    anim.fade_in
            )
            outAnimation = AnimationUtils.loadAnimation(
                    context,
                    anim.fade_out
            )
            // to avoid a cross-over effect - the `in` animation should wait till the `out` animation ends
            inAnimation.startOffset = outAnimation.duration

            inAnimation.setAnimationListener(object : AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    // if still running - post this change-text action again in `delay` ms
                    if (!canceled) {
                        postDelayed(this@DynamicTextsProgressionHelper, delay)
                    }
                }

                override fun onAnimationStart(animation: Animation?) {
                }
            })
            // now set the first text immediately
            setCurrentText(nextText())
        }
    }

    fun cancel() {
        canceled = true
        textSwitcher.get()?.apply {
            removeCallbacks(this@DynamicTextsProgressionHelper)
            visibility = View.GONE
        }
    }

    override fun run() {
        if (canceled) {
            return
        }
        // update to the next text
        textSwitcher.get()?.apply {
            setText(nextText())
        }
    }

    private fun nextText(): CharSequence? {
        // progress with the count and get the next text
        count++
        return textSwitcher.get()?.context?.resources?.getString(stringIds[count % nText])
    }
}
