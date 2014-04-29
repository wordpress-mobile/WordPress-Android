package org.wordpress.android.ui.reader;

import android.widget.TextView;

import org.wordpress.android.R;

/**
 * Created by nbradbury on 4/29/14.
 */
public class ReaderUtils {

    /*
     * used with TextViews that have the ReaderTextView.Follow style to show
     * the passed follow state
     */
    public static void showFollowStatus(final TextView txtFollow, boolean isFollowed) {
        if (txtFollow == null) {
            return;
        }

        if (isFollowed) {
            txtFollow.setText(txtFollow.getContext().getString(R.string.reader_btn_unfollow).toUpperCase());
        } else {
            txtFollow.setText(txtFollow.getContext().getString(R.string.reader_btn_follow).toUpperCase());
        }

        int drawableId = (isFollowed ? R.drawable.note_icon_following : R.drawable.note_icon_follow);
        txtFollow.setCompoundDrawablesWithIntrinsicBounds(drawableId, 0, 0, 0);

        txtFollow.setSelected(isFollowed);
    }
}
