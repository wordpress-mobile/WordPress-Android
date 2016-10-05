package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * topmost view in post detail - contains post blavatar+avatar, author name, blog name, follower
 * count and follow button
 */
public class ReaderPostDetailHeaderView extends LinearLayout {

    private ReaderPost mPost;
    private ReaderFollowButton mFollowButton;
    private int mFollowerCount;

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

        View frameAvatar = view.findViewById(R.id.frame_avatar);
        View header = view.findViewById(R.id.layout_post_header);

        RelativeLayout.LayoutParams paramsFrame = (RelativeLayout.LayoutParams) frameAvatar.getLayoutParams();
        RelativeLayout.LayoutParams paramsHeader = (RelativeLayout.LayoutParams) header.getLayoutParams();

        // in landscape we want the detail below the avatar, otherwise we want it to the right
        if (DisplayUtils.isLandscape(context)) {
            paramsFrame.addRule(RelativeLayout.CENTER_HORIZONTAL);
            paramsHeader.addRule(RelativeLayout.CENTER_HORIZONTAL);
            paramsHeader.addRule(RelativeLayout.BELOW, frameAvatar.getId());
        } else {
            paramsFrame.addRule(RelativeLayout.CENTER_VERTICAL);
            paramsHeader.addRule(RelativeLayout.CENTER_VERTICAL);
            paramsHeader.addRule(RelativeLayout.RIGHT_OF, frameAvatar.getId());
        }
    }

    public void setPost(@NonNull ReaderPost post) {
        mPost = post;

        TextView txtBlogName = (TextView) findViewById(R.id.text_header_blog_name);
        TextView txtAuthorName = (TextView) findViewById(R.id.text_header_author_name);
        WPNetworkImageView imgAvatar = (WPNetworkImageView) findViewById(R.id.image_header_avatar);

        boolean hasBlogName = mPost.hasBlogName();
        boolean hasAuthorName = mPost.hasAuthorName();

        if (hasBlogName && hasAuthorName) {
            txtBlogName.setText(mPost.getBlogName());
            // don't show author name if it's the same as the blog name
            if (mPost.getAuthorName().equals(mPost.getBlogName())) {
                txtAuthorName.setVisibility(View.GONE);
            } else {
                txtAuthorName.setText(mPost.getAuthorName());
                txtAuthorName.setVisibility(View.VISIBLE);
            }
        } else if (hasBlogName) {
            txtBlogName.setText(mPost.getBlogName());
            txtAuthorName.setVisibility(View.GONE);
        } else if (hasAuthorName) {
            txtAuthorName.setText(mPost.getAuthorName());
            txtBlogName.setVisibility(View.GONE);
        } else {
            txtBlogName.setText(R.string.untitled);
            txtAuthorName.setVisibility(View.GONE);
        }

        // show blog preview when these views are tapped
        txtBlogName.setOnClickListener(mClickListener);
        txtAuthorName.setOnClickListener(mClickListener);
        imgAvatar.setOnClickListener(mClickListener);

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

    private void setFollowerCount(int count) {
        if (count != mFollowerCount) {
            mFollowerCount = count;
            TextView txtFollowerCount = (TextView) findViewById(R.id.text_header_follow_count);
            txtFollowerCount.setText(String.format(getContext().getString(R.string.reader_label_follow_count), count));
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
        if (blogInfo == null) return;

        setFollowerCount(blogInfo.numSubscribers);

        boolean hasBlavatar = blogInfo.hasImageUrl();
        WPNetworkImageView imgBlavatar = (WPNetworkImageView) findViewById(R.id.image_header_blavatar);
        if (hasBlavatar) {
            int blavatarSz = getResources().getDimensionPixelSize(R.dimen.reader_detail_header_blavatar);
            String blavatarUrl = PhotonUtils.getPhotonImageUrl(blogInfo.getImageUrl(), blavatarSz, blavatarSz);
            imgBlavatar.setVisibility(View.VISIBLE);
            imgBlavatar.setImageUrl(blavatarUrl, WPNetworkImageView.ImageType.BLAVATAR);
        } else {
            imgBlavatar.setVisibility(View.GONE);

        }

        // avatar size depends on whether there's a blavatar
        int resId = hasBlavatar ? R.dimen.reader_detail_header_avatar : R.dimen.reader_detail_header_avatar_frame;
        int avatarSz = getContext().getResources().getDimensionPixelSize(resId);
        WPNetworkImageView imgAvatar = (WPNetworkImageView) findViewById(R.id.image_header_avatar);
        imgAvatar.getLayoutParams().height = avatarSz;
        imgAvatar.getLayoutParams().width = avatarSz;

        if (mPost != null && mPost.hasPostAvatar()) {
            String avatarUrl = GravatarUtils.fixGravatarUrl(mPost.getPostAvatar(), avatarSz);
            imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
        } else {
            imgAvatar.showDefaultGravatarImage();
        }
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
        final int currentFollowerCount = mFollowerCount;
        int newFollowerCount = isAskingToFollow ? mFollowerCount + 1 : mFollowerCount - 1;

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
                    setFollowerCount(currentFollowerCount);
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
            setFollowerCount(newFollowerCount);
        }
    }
}
