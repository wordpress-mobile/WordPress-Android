package org.wordpress.android.ui.reader.views

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R

enum class ReaderFollowButtonType(
    val value: Int,
    @StringRes val captionFollow: Int,
    @StringRes val captionFollowing: Int,
    @DrawableRes val iconFollow: Int,
    @DrawableRes val iconFollowing: Int
) {
    FOLLOW_SITE(
        0,
        R.string.reader_btn_follow,
        R.string.reader_btn_unfollow,
        R.drawable.ic_reader_follow_white_24dp,
        R.drawable.ic_reader_following_white_24dp
    ),

    // Note: even though AS does not catch it and it says it is not used, FOLLOW_COMMENTS is actually used currently
    // by fromInt function to evaluate wpReaderFollowButtonType attr for example in the ReaderFollowButton that
    // is placed in reader_comments_post_header_view.xml. Mind of this before to remove!
    FOLLOW_COMMENTS(
        1,
        R.string.reader_btn_follow_comments,
        R.string.reader_btn_following_comments,
        R.drawable.ic_reader_follow_conversation_white_24dp,
        R.drawable.ic_reader_following_conversation_white_24dp
    );

    companion object {
        fun fromInt(value: Int): ReaderFollowButtonType =
            values().firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("ReaderFollowButtonType wrong value $value")
    }
}
