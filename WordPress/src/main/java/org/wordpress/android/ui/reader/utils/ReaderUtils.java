package org.wordpress.android.ui.reader.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.List;

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
     * return the path to use for the /batch/ endpoint from the list of request urls
     * https://developer.wordpress.com/docs/api/1/get/batch/
     */
    public static String getBatchEndpointForRequests(List<String> requestUrls) {
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

    /*
     * adds a transparent header to the passed listView
     */
    public static View addListViewHeader(ListView listView, int height) {
        if (listView == null) {
            return null;
        }
        RelativeLayout header = new RelativeLayout(listView.getContext());
        header.setLayoutParams(new AbsListView.LayoutParams(
                AbsListView.LayoutParams.MATCH_PARENT,
                height));
        listView.addHeaderView(header, null, false);
        return header;
    }

    /*
     * adds a rule which tells the view with targetId to be placed below layoutBelowId - only
     * works if viewParent is a RelativeLayout
     */
    public static void layoutBelow(ViewGroup viewParent, int targetId, int layoutBelowId) {
        if (viewParent == null || !(viewParent instanceof RelativeLayout)) {
            return;
        }

        View target = viewParent.findViewById(targetId);
        if (target == null) {
            return;
        }

        if (target.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) target.getLayoutParams();
            params.addRule(RelativeLayout.BELOW, layoutBelowId);
        }
    }

    /*
     * returns a bitmap of the passed view - note that the view must have layout for this to work
     */
    public static Bitmap createBitmapFromView(View view) {
        if (view == null) {
            return null;
        }

        view.buildDrawingCache();
        try {
            Bitmap bmp = view.getDrawingCache();
            if (bmp == null) {
                return null;
            }
            // return a copy of this bitmap since original will be destroyed when the
            // cache is destroyed
            return bmp.copy(Bitmap.Config.ARGB_8888, false);
        } finally {
            view.destroyDrawingCache();
        }
    }

    public static String getResizedImageUrl(final String imageUrl, int width, int height, boolean isPrivate) {
        if (isPrivate) {
            return getPrivateImageForDisplay(imageUrl, width, height);
        } else {
            return PhotonUtils.getPhotonImageUrl(imageUrl, width, height);
        }
    }

    /*
     * use this to request a reduced size image from a private post - images in private posts can't
     * use photon but these are usually wp images so they support the h= and w= query params
     */
    private static String getPrivateImageForDisplay(final String imageUrl, int width, int height) {
        if (TextUtils.isEmpty(imageUrl)) {
            return "";
        }

        final String query;
        if (width > 0 && height > 0) {
            query = "?w=" + width + "&h=" + height;
        } else if (width > 0) {
            query = "?w=" + width;
        } else if (height > 0) {
            query = "?h=" + height;
        } else {
            query = "";
        }
        // remove the existing query string, add the new one, and make sure the url is https:
        return UrlUtils.removeQuery(UrlUtils.makeHttps(imageUrl)) + query;
    }

    /*
     * returns the passed tagName formatted for use with our API
     * see sanitize_title_with_dashes in http://core.trac.wordpress.org/browser/tags/3.6/wp-includes/formatting.php#L0
     */
    public static String sanitizeTagName(final String tagName) {
        if (tagName == null) {
            return "";
        }

        // remove ampersands and number signs, replace spaces & periods with dashes
        String sanitized = tagName.trim()
                .replace("&", "")
                .replace("#", "")
                .replace(" ", "-")
                .replace(".", "-");

        // replace double dashes with single dash (may have been added above)
        while (sanitized.contains("--")) {
            sanitized = sanitized.replace("--", "-");
        }

        return sanitized.trim();
    }

    /*
     * returns the long text to use for a like label ("Liked by 3 people", etc.)
     */
    public static String getLongLikeLabelText(Context context, int numLikes, boolean isLikedByCurrentUser) {
        if (isLikedByCurrentUser) {
            switch (numLikes) {
                case 1:
                    return context.getString(R.string.reader_likes_only_you);
                case 2:
                    return context.getString(R.string.reader_likes_you_and_one);
                default:
                    return context.getString(R.string.reader_likes_you_and_multi, numLikes - 1);
            }
        } else {
            return (numLikes == 1 ?
                    context.getString(R.string.reader_likes_one) : context.getString(R.string.reader_likes_multi, numLikes));
        }
    }
}
