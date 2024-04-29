package org.wordpress.android.ui.notifications.blocks

import android.text.Spannable
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableRange
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.util.RtlUtils
import org.wordpress.android.util.getRangeValueOrEmpty
import org.wordpress.android.util.image.ImageManager

class FooterNoteBlock(
    noteObject: FormattableContent,
    imageManager: ImageManager,
    notificationsUtilsWrapper: NotificationsUtilsWrapper,
    onNoteBlockTextClickListener: OnNoteBlockTextClickListener?
) : NoteBlock(noteObject, imageManager, notificationsUtilsWrapper, onNoteBlockTextClickListener) {
    private lateinit var mClickableSpan: NoteBlockClickableSpan

    private val noticonGlyph: String
        get() = noteData.getRangeValueOrEmpty(0)

    override val noteText: Spannable
        get() = mNotificationsUtilsWrapper.getSpannableContentForRanges(
            noteData, null,
            onNoteBlockTextClickListener, true
        )
    override val blockType: BlockType
        get() = BlockType.FOOTER
    override val layoutResourceId: Int
        get() = R.layout.note_block_footer

    fun setClickableSpan(rangeObject: FormattableRange?, noteType: String?) {
        if (rangeObject == null) {
            return
        }
        mClickableSpan = NoteBlockClickableSpan(rangeObject, mShouldLink = false, mIsFooter = true)
        mClickableSpan.setCustomType(noteType)
    }


    override fun configureView(view: View): View {
        val noteBlockHolder = view.tag as FooterNoteBlockHolder

        // Note text
        if (!TextUtils.isEmpty(noteText)) {
            val spannable = noteText
            val spans = spannable.getSpans(0, spannable.length, NoteBlockClickableSpan::class.java)
            for (span in spans) {
                span.enableColors(view.context)
            }
            noteBlockHolder.textView.text = spannable
            noteBlockHolder.textView.visibility = View.VISIBLE
        } else {
            noteBlockHolder.textView.visibility = View.GONE
        }
        val noticonGlyph = noticonGlyph
        if (!TextUtils.isEmpty(noticonGlyph)) {
            noteBlockHolder.noticonView.visibility = View.VISIBLE
            noteBlockHolder.noticonView.text = noticonGlyph
            // mirror noticon in the rtl mode
            if (RtlUtils.isRtl(noteBlockHolder.noticonView.context)) {
                noteBlockHolder.noticonView.scaleX = -1f
            }
        } else {
            noteBlockHolder.noticonView.visibility = View.GONE
        }
        return view
    }

    override fun getViewHolder(view: View): Any = FooterNoteBlockHolder(view)

    internal inner class FooterNoteBlockHolder(view: View) {
        val textView: TextView = view.findViewById(R.id.note_footer_text)
        val noticonView: TextView = view.findViewById(R.id.note_footer_noticon)

        init {
            view.findViewById<View>(R.id.note_footer).setOnClickListener {
                onNoteBlockTextClickListener?.onNoteBlockTextClicked(mClickableSpan)
            }
        }
    }
}
