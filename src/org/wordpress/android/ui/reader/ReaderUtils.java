package org.wordpress.android.ui.reader;

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
import org.wordpress.android.util.UrlUtils;

import java.util.List;

public class ReaderUtils {
    public static interface FullScreenListener {
        public boolean onRequestFullScreen(boolean enable);
        public boolean isFullScreen();
        public boolean isFullScreenSupported();
    }

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
     * set the top margin for the passed view
     */
    public static void setTopMargin(View view, int height) {
        if (view != null && view.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
            params.topMargin = height;
        }
    }

    /*
     * adds a transparent header to the passed listView
     */
    public static View addListViewHeader(ListView listView, int height) {
        if (listView == null) {
            return null;
        }
        RelativeLayout headerFake = new RelativeLayout(listView.getContext());
        headerFake.setLayoutParams(new AbsListView.LayoutParams(
                AbsListView.LayoutParams.MATCH_PARENT,
                height));
        listView.addHeaderView(headerFake, null, false);
        return headerFake;
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

    /*
     * use this to request a reduced size image from a private post - images in private posts can't
     * use photon but these are usually wp images so they support the h= and w= query params
     */
    public static String getPrivateImageForDisplay(final String imageUrl, int width, int height) {
        if (TextUtils.isEmpty(imageUrl)) {
            return "";
        }

        final String query;
        if (width > 0 && height > 0) {
            query = String.format("?w=%d&h=%d", width, height);
        } else if (width > 0) {
            query = String.format("?w=%d", width);
        } else if (height > 0) {
            query = String.format("?h=%d", height);
        } else {
            query = "";
        }
        // remove the existing query string, add the new one, and make sure the url is https:
        return UrlUtils.removeQuery(UrlUtils.makeHttps(imageUrl)) + query;
    }
}
