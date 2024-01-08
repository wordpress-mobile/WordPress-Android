package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderLikeTable;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderUserIdList;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import javax.inject.Inject;

/*
 * LinearLayout which shows liking users - used by ReaderPostDetailFragment
 */
public class ReaderLikingUsersView extends LinearLayout {
    @Inject ImageManager mImageManager;
    private LoadAvatarsTask mLoadAvatarsTask;
    private final int mLikeAvatarSz;

    public ReaderLikingUsersView(Context context) {
        this(context, null);
    }

    public ReaderLikingUsersView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ((WordPress) context.getApplicationContext()).component().inject(this);

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        mLikeAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_small);
    }

    public void showLikingUsers(final ReaderPost post, final long currentUserId) {
        if (post == null) {
            return;
        }

        if (mLoadAvatarsTask != null) {
            mLoadAvatarsTask.cancel(false);
        }
        mLoadAvatarsTask = new LoadAvatarsTask(this, currentUserId, mLikeAvatarSz, getMaxAvatars());
        mLoadAvatarsTask.execute(post);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mLoadAvatarsTask != null) {
            mLoadAvatarsTask.cancel(false);
        }
    }

    /*
     * returns count of avatars that can fit the current space
     */
    private int getMaxAvatars() {
        final int marginAvatar = getResources().getDimensionPixelSize(R.dimen.margin_extra_small);
        final int marginReader = getResources().getDimensionPixelSize(R.dimen.reader_detail_margin);
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
            ImageView imgAvatar;
            // reuse existing view when possible, otherwise inflate a new one
            if (index < numExistingViews) {
                imgAvatar = (ImageView) getChildAt(index);
            } else {
                imgAvatar = (ImageView) inflater.inflate(R.layout.reader_like_avatar, this, false);
                addView(imgAvatar);
            }
            mImageManager.loadIntoCircle(imgAvatar, ImageType.AVATAR, StringUtils.notNullStr(url));
            index++;
        }
    }

    @SuppressWarnings("deprecation")
    private static class LoadAvatarsTask extends AsyncTask<ReaderPost, Void, ArrayList<String>> {
        private final WeakReference<ReaderLikingUsersView> mViewReference;
        private final long mCurrentUserId;
        private final int mLikeAvatarSize;
        private final int mMaxAvatars;

        LoadAvatarsTask(ReaderLikingUsersView view, long currentUserId, int likeAvatarSz, int maxAvatars) {
            mViewReference = new WeakReference<>(view);
            mCurrentUserId = currentUserId;
            mLikeAvatarSize = likeAvatarSz;
            mMaxAvatars = maxAvatars;
        }

        @Override
        protected ArrayList<String> doInBackground(ReaderPost... posts) {
            if (posts.length != 1 || posts[0] == null) {
                return null;
            }
            ReaderPost post = posts[0];
            ReaderUserIdList avatarIds = ReaderLikeTable.getLikesForPost(post);
            return ReaderUserTable.getAvatarUrls(avatarIds, mMaxAvatars, mLikeAvatarSize, mCurrentUserId);
        }

        @Override
        protected void onPostExecute(ArrayList<String> avatars) {
            super.onPostExecute(avatars);
            ReaderLikingUsersView view = mViewReference.get();
            if (view != null && avatars != null && !isCancelled()) {
                view.mLoadAvatarsTask = null;
                view.showLikingAvatars(avatars);
            }
        }
    }
}
