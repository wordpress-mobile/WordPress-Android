package org.wordpress.android.ui.notifications.blocks

import android.text.Spannable
import android.text.SpannableString
import org.wordpress.android.R
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.util.image.ImageManager

class MilestoneNoteBlock(
    noteData: FormattableContent,
    imageManager: ImageManager,
    notificationsUtilsWrapper: NotificationsUtilsWrapper,
    onNoteBlockTextClickListener: OnNoteBlockTextClickListener?,
) : NoteBlock(
    noteData,
    imageManager,
    notificationsUtilsWrapper,
    onNoteBlockTextClickListener
) {
    override var mIsBadge = true
    override var mIsViewMilestone = true
    override val layoutResourceId: Int
        get() = R.layout.note_block_milestone

    // The first item of the list which contains the badge is skipped because it is used for the legacy screen's title.
    override val noteText: Spannable
        get() = if(containsBadgeMediaType()) SpannableString("") else super.noteText
}
