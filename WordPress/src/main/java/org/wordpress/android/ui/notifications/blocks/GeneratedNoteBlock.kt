package org.wordpress.android.ui.notifications.blocks

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import org.json.JSONObject
import org.wordpress.android.WordPress
import org.wordpress.android.ui.notifications.blocks.BlockType.UNKNOWN

class GeneratedNoteBlock(
    val text: String,
    noteObject: JSONObject,
    onNoteBlockTextClickListener: OnNoteBlockTextClickListener
) : NoteBlock(noteObject, onNoteBlockTextClickListener) {
    override fun getNoteText(): Spannable {
        val spannableStringBuilder = SpannableStringBuilder(text)
        val shouldLink = onNoteBlockTextClickListener != null

        // Process Ranges to add links and text formatting
        val map = mapOf("url" to metaSiteUrl)
        val rangeObject = JSONObject(map)
        val clickableSpan = object : NoteBlockClickableSpan(WordPress.getContext(), rangeObject,
                shouldLink, false) {
            override fun onClick(widget: View) {
                if (onNoteBlockTextClickListener != null) {
                    onNoteBlockTextClickListener.onNoteBlockTextClicked(this)
                }
            }
        }

        val indices = arrayOf(0, spannableStringBuilder.length)
        if (indices.size == 2 && indices[0] <= spannableStringBuilder.length
                && indices[1] <= spannableStringBuilder.length) {
            spannableStringBuilder
                    .setSpan(clickableSpan, indices[0], indices[1], Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            // Add additional styling if the range wants it
            if (clickableSpan.spanStyle != Typeface.NORMAL) {
                val styleSpan = StyleSpan(clickableSpan.spanStyle)
                spannableStringBuilder
                        .setSpan(styleSpan, indices[0], indices[1], Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }
        }

        return spannableStringBuilder
    }

    override fun hasImageMediaItem(): Boolean {
        return false
    }

    override fun getBlockType(): BlockType {
        return UNKNOWN
    }
}
