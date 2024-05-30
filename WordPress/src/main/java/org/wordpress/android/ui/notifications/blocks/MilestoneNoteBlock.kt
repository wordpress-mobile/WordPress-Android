package org.wordpress.android.ui.notifications.blocks

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
}
