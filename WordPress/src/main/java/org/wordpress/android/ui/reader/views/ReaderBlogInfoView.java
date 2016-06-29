package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPNetworkImageView.ImageType;

/**
 * topmost view in post adapter when showing blog preview - displays description, follower
 * count, and follow button
 */
public class ReaderBlogInfoView extends LinearLayout {

    public interface OnBlogInfoLoadedListener {
        void onBlogInfoLoaded(ReaderBlog blogInfo);
    }

    private long mBlogId;
    private long mFeedId;
    private ReaderFollowButton mFollowButton;
    private ReaderBlog mBlogInfo;
    private OnBlogInfoLoadedListener mBlogInfoListener;

    public ReaderBlogInfoView(Context context) {
        super(context);
        initView(context);
    }

    public ReaderBlogInfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ReaderBlogInfoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        View view = inflate(context, R.layout.reader_blog_info_view, this);
        mFollowButton = (ReaderFollowButton) view.findViewById(R.id.follow_button);
    }

    public void setOnBlogInfoLoadedListener(OnBlogInfoLoadedListener listener) {
        mBlogInfoListener = listener;
    }

    public void loadBlogInfo(long blogId, long feedId) {
        mBlogId = blogId;
        mFeedId = feedId;

        // first get info from local db
        final ReaderBlog localBlogInfo;
        if (mBlogId != 0) {
            localBlogInfo = ReaderBlogTable.getBlogInfo(mBlogId);
        } else if (mFeedId != 0) {
            localBlogInfo = ReaderBlogTable.getFeedInfo(mFeedId);
        } else {
            ToastUtils.showToast(getContext(), R.string.reader_toast_err_get_blog_info);
            return;
        }
        if (localBlogInfo != null) {
            showBlogInfo(localBlogInfo);
        }

        // then get from server if doesn't exist locally or is time to update it
        if (localBlogInfo == null || ReaderBlogTable.isTimeToUpdateBlogInfo(localBlogInfo)) {
            ReaderActions.UpdateBlogInfoListener listener = new ReaderActions.UpdateBlogInfoListener() {
                @Override
                public void onResult(ReaderBlog serverBlogInfo) {
                    showBlogInfo(serverBlogInfo);
                }
            };
            if (mFeedId != 0) {
                ReaderBlogActions.updateFeedInfo(mFeedId, null, listener);
            } else {
                ReaderBlogActions.updateBlogInfo(mBlogId, null, listener);
            }
        }
    }

    private void showBlogInfo(ReaderBlog blogInfo) {
        // do nothing if unchanged
        if (blogInfo == null || blogInfo.isSameAs(mBlogInfo)) {
            return;
        }

        mBlogInfo = blogInfo;

        ViewGroup layoutInfo = (ViewGroup) findViewById(R.id.layout_blog_info);
        TextView txtBlogName = (TextView) layoutInfo.findViewById(R.id.text_blog_name);
        TextView txtDomain = (TextView) layoutInfo.findViewById(R.id.text_domain);
        TextView txtDescription = (TextView) layoutInfo.findViewById(R.id.text_blog_description);
        TextView txtFollowCount = (TextView) layoutInfo.findViewById(R.id.text_blog_follow_count);
        WPNetworkImageView imgBlavatar = (WPNetworkImageView) layoutInfo.findViewById(R.id.image_blavatar);

        if (blogInfo.hasName()) {
            txtBlogName.setText(blogInfo.getName());
        } else {
            txtBlogName.setText(R.string.reader_untitled_post);
        }

        if (blogInfo.hasUrl()) {
            txtDomain.setText(UrlUtils.getHost(blogInfo.getUrl()));
            txtDomain.setVisibility(View.VISIBLE);
        } else {
            txtDomain.setVisibility(View.GONE);
        }

        if (blogInfo.hasDescription()) {
            txtDescription.setText(blogInfo.getDescription());
            txtDescription.setVisibility(View.VISIBLE);
        } else {
            txtDescription.setVisibility(View.GONE);
        }

        txtFollowCount.setText(String.format(getContext().getString(R.string.reader_label_follow_count), blogInfo.numSubscribers));

        if (ReaderUtils.isLoggedOutReader()) {
            mFollowButton.setVisibility(View.GONE);
        } else {
            mFollowButton.setVisibility(View.VISIBLE);
            mFollowButton.setIsFollowed(blogInfo.isFollowing);
            mFollowButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollowStatus();
                }
            });
        }

        if (blogInfo.hasImageUrl()) {
            int imageSize = getContext().getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
            String imageUrl = PhotonUtils.getPhotonImageUrl(blogInfo.getImageUrl(), imageSize, imageSize);
            imgBlavatar.setImageUrl(imageUrl, ImageType.BLAVATAR);
        } else {
            imgBlavatar.showDefaultBlavatarImage();
        }

        if (layoutInfo.getVisibility() != View.VISIBLE) {
            layoutInfo.setVisibility(View.VISIBLE);
        }

        if (mBlogInfoListener != null) {
            mBlogInfoListener.onBlogInfoLoaded(blogInfo);
        }
    }

    private void toggleFollowStatus() {
        if (!NetworkUtils.checkConnection(getContext())) {
            return;
        }

        final boolean isAskingToFollow;
        if (mFeedId != 0) {
            isAskingToFollow = !ReaderBlogTable.isFollowedFeed(mFeedId);
        } else {
            isAskingToFollow = !ReaderBlogTable.isFollowedBlog(mBlogId);
        }

        ReaderActions.ActionListener listener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (getContext() == null) {
                    return;
                }
                mFollowButton.setEnabled(true);
                if (!succeeded) {
                    int errResId = isAskingToFollow ? R.string.reader_toast_err_follow_blog : R.string.reader_toast_err_unfollow_blog;
                    ToastUtils.showToast(getContext(), errResId);
                    mFollowButton.setIsFollowed(!isAskingToFollow);
                }
            }
        };

        // disable follow button until API call returns
        mFollowButton.setEnabled(false);

        boolean result;
        if (mFeedId != 0) {
            result = ReaderBlogActions.followFeedById(mFeedId, isAskingToFollow, listener);
        } else {
            result = ReaderBlogActions.followBlogById(mBlogId, isAskingToFollow, listener);
        }

        if (result) {
            mFollowButton.setIsFollowedAnimated(isAskingToFollow);
        }
    }
}
