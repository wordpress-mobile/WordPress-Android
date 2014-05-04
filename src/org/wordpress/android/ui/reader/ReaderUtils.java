package org.wordpress.android.ui.reader;

import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderUrlList;

/**
 * Created by nbradbury on 4/29/14.
 */
public class ReaderUtils {

    /*
     * used with TextViews that have the ReaderTextView.Follow style to show
     * the passed follow state
     */
    public static void showFollowStatus(final TextView txtFollow, boolean isFollowed) {
        // selected state is same as followed state, so do nothing if they already match
        if (txtFollow == null || txtFollow.isSelected() == isFollowed) {
            return;
        }

        if (isFollowed) {
            txtFollow.setText(txtFollow.getContext().getString(R.string.reader_btn_unfollow));
        } else {
            txtFollow.setText(txtFollow.getContext().getString(R.string.reader_btn_follow));
        }

        int drawableId = (isFollowed ? R.drawable.note_icon_following : R.drawable.note_icon_follow);
        txtFollow.setCompoundDrawablesWithIntrinsicBounds(drawableId, 0, 0, 0);

        txtFollow.setSelected(isFollowed);
    }

    /*
     * returns true if the passed view's tag is the same as the passed string - this is used
     * with imageViews that show network images, to avoid reloading the image if the imageView
     * is already tagged with the image url
     */
    public static boolean viewHasTag(final View view, final String tag) {
        if (view == null || tag == null) {
            return false;
        } else {
            return tag.equals(view.getTag());
        }
    }

    /*
     * return the path to use for the /batch/ endpoint from the list of request urls
     * https://developer.wordpress.com/docs/api/1/get/batch/
     */
    public static String getBatchEndpointForRequests(ReaderUrlList requestUrls) {
        StringBuilder sbBatch = new StringBuilder("/batch/");
        if (requestUrls != null) {
            boolean isFirst = true;
            for (String url : requestUrls) {
                if (!TextUtils.isEmpty(url)) {
                    if (isFirst) {
                        isFirst = false;
                        sbBatch.append("?");
                    } else {
                        sbBatch.append("&");
                    }
                    sbBatch.append("urls%5B%5D=").append(Uri.encode(url));
                }
            }
        }
        return sbBatch.toString();
    }
}
