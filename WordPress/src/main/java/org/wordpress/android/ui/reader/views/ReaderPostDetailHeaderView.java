package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * topmost view in post detail - shows blavatar + avatar, author name, blog name, and follow button
 */
public class ReaderPostDetailHeaderView extends LinearLayout {

    private ReaderPost mPost;
    private ReaderFollowButton mFollowButton;

    public ReaderPostDetailHeaderView(Context context) {
        super(context);
        initView(context);
    }

    public ReaderPostDetailHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ReaderPostDetailHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        View view = inflate(context, R.layout.reader_post_detail_header_view, this);
        mFollowButton = (ReaderFollowButton) view.findViewById(R.id.header_follow_button);
    }

    public void setPost(@NonNull ReaderPost post) {
        mPost = post;

        TextView txtTitle = (TextView) findViewById(R.id.text_header_title);
        TextView txtSubtitle = (TextView) findViewById(R.id.text_header_subtitle);

        boolean hasBlogName = mPost.hasBlogName();
        boolean hasAuthorName = mPost.hasAuthorName();

        if (hasBlogName && hasAuthorName) {
            // don't show author name if it's the same as the blog name
            if (mPost.getAuthorName().equals(mPost.getBlogName())) {
                txtTitle.setText(mPost.getAuthorName());
                txtSubtitle.setVisibility(View.GONE);
            } else {
                txtTitle.setText(mPost.getAuthorName());
                txtSubtitle.setText(mPost.getBlogName());
            }
        } else if (hasBlogName) {
            txtTitle.setText(mPost.getBlogName());
            txtSubtitle.setVisibility(View.GONE);
        } else if (hasAuthorName) {
            txtTitle.setText(mPost.getAuthorName());
            txtSubtitle.setVisibility(View.GONE);
        } else {
            txtTitle.setText(R.string.untitled);
            txtSubtitle.setVisibility(View.GONE);
        }

        // show blog preview when these views are tapped
        txtTitle.setOnClickListener(mClickListener);
        txtSubtitle.setOnClickListener(mClickListener);

        if (ReaderUtils.isLoggedOutReader()) {
            mFollowButton.setVisibility(View.GONE);
        } else {
            mFollowButton.setVisibility(View.VISIBLE);
            mFollowButton.setIsFollowed(mPost.isFollowedByCurrentUser);
            mFollowButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollowStatus();
                }
            });
        }

        // get local blog info so we can set the follower count and blavatar
        ReaderBlog blogInfo = mPost.isExternal ? ReaderBlogTable.getFeedInfo(mPost.feedId) : ReaderBlogTable.getBlogInfo(mPost.blogId);
        if (blogInfo != null) {
            showBlogInfo(blogInfo);
        }

        // update blog info if it's time or it doesn't exist
        if (blogInfo == null || ReaderBlogTable.isTimeToUpdateBlogInfo(blogInfo)) {
            updateBlogInfo();
        }
    }

    /*
     * get the latest info about this post's blog so we have an accurate follower count
     */
    private void updateBlogInfo() {
        if (!NetworkUtils.isNetworkAvailable(getContext())) return;

        ReaderActions.UpdateBlogInfoListener listener = new ReaderActions.UpdateBlogInfoListener() {
            @Override
            public void onResult(ReaderBlog blogInfo) {
                showBlogInfo(blogInfo);
            }
        };
        if (mPost.isExternal) {
            ReaderBlogActions.updateFeedInfo(mPost.feedId, null, listener);
        } else {
            ReaderBlogActions.updateBlogInfo(mPost.blogId, null, listener);
        }
    }

    private void showBlogInfo(ReaderBlog blogInfo) {
        String blavatarUrl = blogInfo != null ? blogInfo.getImageUrl() : null;
        String avatarUrl = mPost != null ? mPost.getPostAvatar() : null;
        showBlavatarAndAvatar(blavatarUrl, avatarUrl);
    }

    private void showBlavatarAndAvatar(String blavatarUrl, String avatarUrl) {
        boolean hasBlavatar = !TextUtils.isEmpty(blavatarUrl);
        boolean hasAvatar = !TextUtils.isEmpty(avatarUrl);

        AppLog.w(AppLog.T.READER, avatarUrl);

        int frameSize = getResources().getDimensionPixelSize(R.dimen.reader_detail_header_avatar_frame);

        View avatarFrame = findViewById(R.id.frame_avatar);
        WPNetworkImageView imgBlavatar = (WPNetworkImageView) findViewById(R.id.image_header_blavatar);
        WPNetworkImageView imgAvatar = (WPNetworkImageView) findViewById(R.id.image_header_avatar);

        /*
         *  - if there's a blavatar and an avatar, show both of them overlaid using default sizing
         *  - if there's only a blavatar, show it the full size of the parent frame and hide the avatar
         *  - if there's only an avatar, show it the full size of the parent frame and hide the blavatar
         *  - if there's neither a blavatar nor an avatar, hide them both
         */
        if (hasBlavatar && hasAvatar) {
            int blavatarSz = getResources().getDimensionPixelSize(R.dimen.reader_detail_header_blavatar);
            imgBlavatar.getLayoutParams().height = blavatarSz;
            imgBlavatar.getLayoutParams().width = blavatarSz;
            imgBlavatar.setImageUrl(
                    PhotonUtils.getPhotonImageUrl(blavatarUrl, blavatarSz, blavatarSz),
                    WPNetworkImageView.ImageType.BLAVATAR);
            imgBlavatar.setVisibility(View.VISIBLE);

            int avatarSz = getResources().getDimensionPixelSize(R.dimen.reader_detail_header_avatar);
            imgAvatar.getLayoutParams().height = avatarSz;
            imgAvatar.getLayoutParams().width = avatarSz;
            imgAvatar.setImageUrl(
                    GravatarUtils.fixGravatarUrl(avatarUrl, avatarSz),
                    WPNetworkImageView.ImageType.AVATAR);
            imgAvatar.setVisibility(View.VISIBLE);
        } else if (hasBlavatar) {
            imgBlavatar.getLayoutParams().height = frameSize;
            imgBlavatar.getLayoutParams().width = frameSize;
            imgBlavatar.setImageUrl(
                    PhotonUtils.getPhotonImageUrl(blavatarUrl, frameSize, frameSize),
                    WPNetworkImageView.ImageType.BLAVATAR);
            imgBlavatar.setVisibility(View.VISIBLE);

            imgAvatar.setVisibility(View.GONE);
        } else if (hasAvatar) {
            imgBlavatar.setVisibility(View.GONE);

            imgAvatar.getLayoutParams().height = frameSize;
            imgAvatar.getLayoutParams().width = frameSize;
            imgAvatar.setImageUrl(
                    GravatarUtils.fixGravatarUrl(avatarUrl, frameSize),
                    WPNetworkImageView.ImageType.AVATAR);
            imgAvatar.setVisibility(View.VISIBLE);
        } else {
            imgBlavatar.setVisibility(View.GONE);
            imgAvatar.setVisibility(View.GONE);
        }

        // hide the frame if there's neither a blavatar nor an avatar
        avatarFrame.setVisibility(hasAvatar || hasBlavatar ? View.VISIBLE : View.GONE);

        imgBlavatar.setOnClickListener(mClickListener);
        imgAvatar.setOnClickListener(mClickListener);
    }

    /*
     * click listener which shows blog preview
     */
    private final OnClickListener mClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPost != null) {
                ReaderActivityLauncher.showReaderBlogPreview(v.getContext(), mPost);
            }
        }
    };

    private void toggleFollowStatus() {
        if (!NetworkUtils.checkConnection(getContext())) return;

        final boolean isAskingToFollow = !mPost.isFollowedByCurrentUser;

        ReaderActions.ActionListener listener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (getContext() == null) return;

                mFollowButton.setEnabled(true);
                if (succeeded) {
                    mPost.isFollowedByCurrentUser = isAskingToFollow;
                } else {
                    int errResId = isAskingToFollow ? R.string.reader_toast_err_follow_blog : R.string.reader_toast_err_unfollow_blog;
                    ToastUtils.showToast(getContext(), errResId);
                    mFollowButton.setIsFollowed(!isAskingToFollow);
                }
                updateBlogInfo();
            }
        };

        // disable follow button until API call returns
        mFollowButton.setEnabled(false);

        boolean result;
        if (mPost.isExternal) {
            result = ReaderBlogActions.followFeedById(mPost.feedId, isAskingToFollow, listener);
        } else {
            result = ReaderBlogActions.followBlogById(mPost.blogId, isAskingToFollow, listener);
        }

        if (result) {
            mFollowButton.setIsFollowedAnimated(isAskingToFollow);
        }
    }
}
