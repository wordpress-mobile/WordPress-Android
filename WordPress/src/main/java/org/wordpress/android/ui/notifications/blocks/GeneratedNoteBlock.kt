package org.wordpress.android.ui.notifications.blocks

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableRange
import org.wordpress.android.ui.notifications.blocks.BlockType.BASIC
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.util.image.ImageManager

@Deprecated("This should be removed once we start receiving Read the source block from the backend")
class GeneratedNoteBlock(
    val text: String,
    imageManager: ImageManager,
    notificationsUtilsWrapper: NotificationsUtilsWrapper,
    val clickListener: OnNoteBlockTextClickListener,
    val pingbackUrl: String
) : NoteBlock(FormattableContent(), imageManager, notificationsUtilsWrapper, clickListener) {
    override fun getNoteText(): Spannable {
        val spannableStringBuilder = SpannableStringBuilder(text)

        // Process Ranges to add links and text formatting
        val formattableRange = FormattableRange(null, null, null, null, pingbackUrl, null)
        val clickableSpan = object : NoteBlockClickableSpan(WordPress.getContext(), formattableRange,
                true, false) {
            override fun onClick(widget: View) {
                clickListener.onNoteBlockTextClicked(this)
            }
        }

        val start = 0
        val end = spannableStringBuilder.length
        spannableStringBuilder.setSpan(clickableSpan, start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

        // Add additional styling if the range wants it
        val styleSpan = StyleSpan(Typeface.ITALIC)
        spannableStringBuilder.setSpan(styleSpan, start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

        return spannableStringBuilder
    }

    override fun getMetaSiteUrl(): String {
        return pingbackUrl
    }

    override fun hasImageMediaItem(): Boolean {
        return false
    }

    override fun getBlockType(): BlockType {
        return BASIC
    }
}
