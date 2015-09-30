package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderLikeTable;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderUserIdList;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;

/*
 * LinearLayout which shows liking users - used by ReaderPostDetailFragment
 */
public class ReaderLikingUsersView extends LinearLayout {
    private final int mLikeAvatarSz;

    public ReaderLikingUsersView(Context context) {
        this(context, null);
    }

    public ReaderLikingUsersView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        mLikeAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_small);
    }

    public void showLikingUsers(final ReaderPost post) {
        if (post == null) {
            return;
        }

        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                // get avatar URLs of liking users up to the max, sized to fit
                int maxAvatars = getMaxAvatars();
                ReaderUserIdList avatarIds = ReaderLikeTable.getLikesForPost(post);
                final ArrayList<String> avatars = ReaderUserTable.getAvatarUrls(avatarIds, maxAvatars, mLikeAvatarSz);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        showLikingAvatars(avatars);
                    }
                });
            }
        }.start();
    }

    /*
     * returns count of avatars that can fit the current space
     */
    private int getMaxAvatars() {
        int marginAvatar = getResources().getDimensionPixelSize(R.dimen.margin_extra_small);
        int marginReader = getResources().getDimensionPixelSize(R.dimen.reader_detail_margin);
        int likeAvatarSizeWithMargin = mLikeAvatarSz + (marginAvatar * 2);
        int spaceForAvatars = getWidth() - (marginReader * 2);
        return spaceForAvatars / likeAvatarSizeWithMargin;
    }

    /*
     * note that the passed list of avatar urls has already been Photon-ized,
     * so there's no need to do that here
     */
    private void showLikingAvatars(final ArrayList<String> avatarUrls) {
        if (avatarUrls == null || avatarUrls.size() == 0) {
            removeAllViews();
            return;
        }

        // remove excess existing views
        int numExistingViews = getChildCount();
        if (numExistingViews > avatarUrls.size()) {
            int numToRemove = numExistingViews - avatarUrls.size();
            removeViews(numExistingViews - numToRemove, numToRemove);
        }

        int index = 0;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (String url : avatarUrls) {
            WPNetworkImageView imgAvatar;
            // reuse existing view when possible, otherwise inflate a new one
            if (index < numExistingViews) {
                imgAvatar = (WPNetworkImageView) getChildAt(index);
            } else {
                imgAvatar = (WPNetworkImageView) inflater.inflate(R.layout.reader_like_avatar, this, false);
                addView(imgAvatar);
            }
            imgAvatar.setImageUrl(url, WPNetworkImageView.ImageType.AVATAR);
            index++;
        }
    }
}