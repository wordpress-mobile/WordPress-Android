package org.wordpress.android.widgets

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatTextView
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.StrikethroughSpan
import android.util.AttributeSet
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.revisions.Diff
import org.wordpress.android.fluxc.model.revisions.DiffOperations.ADD
import org.wordpress.android.fluxc.model.revisions.DiffOperations.DELETE

class DiffView : AppCompatTextView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun showDiffs(diffs: List<Diff>) {
        text = null

        diffs.forEachIndexed { index, diff ->
            var diffValue = diff.value

            // add tiny spacing before and after DEL and ADD diffs (will be included in the span)
            if (diff.operation == ADD || diff.operation == DELETE) {
                diffValue = "\u200A" + diffValue + "\u200A"
            }

            val diffContent = SpannableString(diffValue)

            if (diff.operation == ADD) {
                diffContent.setSpan(
                        ColorUnderlineSpan(
                                ContextCompat.getColor(
                                        context,
                                        R.color.revision_diff_add_action_underline
                                )
                        ), 0, diffContent.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                diffContent.setSpan(
                        BackgroundColorSpan(
                                ContextCompat.getColor(
                                        context,
                                        R.color.revision_diff_add_action_background
                                )
                        ), 0, diffContent.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else if (diff.operation == DELETE) {
                diffContent.setSpan(StrikethroughSpan(), 0, diffContent.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                diffContent.setSpan(
                        ColorUnderlineSpan(
                                ContextCompat.getColor(
                                        context,
                                        R.color.revision_diff_del_action_underline
                                )
                        ), 0, diffContent.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                diffContent.setSpan(
                        BackgroundColorSpan(
                                ContextCompat.getColor(
                                        context,
                                        R.color.revision_diff_del_action_background
                                )
                        ), 0, diffContent.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            // if there is ADD or DEL diff before current ADD or DEL diff we add a little spacing between them
            if (index > 0 && (diff.operation == ADD || diff.operation == DELETE) &&
                    (diffs[index - 1].operation == ADD || diffs[index - 1].operation == DELETE)) {
                append("\u200A")
            }

            append(diffContent)
        }
    }
}
