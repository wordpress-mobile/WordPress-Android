package org.wordpress.android.ui.reader;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.wordpress.android.R;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;

/*
 * LinearLayout which shows liking users - used by ReaderPostDetailFragment
 */
class ReaderLikingUsersView extends LinearLayout {

    public ReaderLikingUsersView(Context context) {
        this(context, null);
    }

    public ReaderLikingUsersView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOrientation(HORIZONTAL);
        this.setGravity(Gravity.CENTER_VERTICAL);
    }

    /*
     * note that the passed list of avatar urls has already been Photon-ized,
     * so there's no need to do that here
     */
    void showLikingAvatars(final ArrayList<String> avatarUrls) {
        ViewGroup parent = this;

        if (avatarUrls == null || avatarUrls.size() == 0) {
            parent.removeAllViews();
            return;
        }

        // remove excess existing views
        int numExistingViews = parent.getChildCount();
        if (numExistingViews > avatarUrls.size()) {
            int numToRemove = numExistingViews - avatarUrls.size();
            parent.removeViews(numExistingViews - numToRemove, numToRemove);
        }

        int index = 0;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (String url : avatarUrls) {
            WPNetworkImageView imgAvatar;
            // reuse existing view when possible, otherwise inflate a new one
            if (index < numExistingViews) {
                imgAvatar = (WPNetworkImageView) parent.getChildAt(index);
            } else {
                imgAvatar = (WPNetworkImageView) inflater.inflate(R.layout.reader_like_avatar, parent, false);
                addView(imgAvatar);
            }
            imgAvatar.setImageUrl(url, WPNetworkImageView.ImageType.AVATAR);
            index++;
        }
    }
}