package org.wordpress.android.ui.notifications.blocks

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import org.json.JSONObject
import org.wordpress.android.WordPress
import org.wordpress.android.ui.notifications.blocks.BlockType.BASIC

@Deprecated("This should be removed once we receive a Pingback notification from the backend")
class GeneratedNoteBlock(
    val text: String,
    val clickListener: OnNoteBlockTextClickListener,
    val pingbackUrl: String
) : NoteBlock(JSONObject(), clickListener) {
    override fun getNoteText(): Spannable {
        val spannableStringBuilder = SpannableStringBuilder(text)

        // Process Ranges to add links and text formatting
        val map = mapOf("url" to pingbackUrl)
        val rangeObject = JSONObject(map)
        val clickableSpan = object : NoteBlockClickableSpan(WordPress.getContext(), rangeObject,
                true, false) {
            override fun onClick(widget: View) {
                clickListener.onNoteBlockTextClicked(this)
            }
        }

        val indices = arrayOf(0, spannableStringBuilder.length)
        if (indices.size == 2 && indices[0] <= spannableStringBuilder.length
                && indices[1] <= spannableStringBuilder.length) {
            spannableStringBuilder
                    .setSpan(clickableSpan, indices[0], indices[1], Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            // Add additional styling if the range wants it
            val styleSpan = StyleSpan(Typeface.ITALIC)
            spannableStringBuilder
                    .setSpan(styleSpan, indices[0], indices[1], Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        }

        return spannableStringBuilder
    }

    override fun hasImageMediaItem(): Boolean {
        return false
    }

    override fun getBlockType(): BlockType {
        return BASIC
    }
}
