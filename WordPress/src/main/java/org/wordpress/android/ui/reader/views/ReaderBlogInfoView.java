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
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

public class ReaderBlogInfoView extends LinearLayout {

    public interface OnBlogInfoLoadedListener {
        void onBlogInfoLoaded(ReaderBlog blogInfo);
    }

    private long mBlogId;
    private long mFeedId;
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
        inflate(context, R.layout.reader_blog_info_view, this);
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

        // then get from server
        ReaderActions.UpdateBlogInfoListener listener = new ReaderActions.UpdateBlogInfoListener() {
            @Override
            public void onResult(ReaderBlog serverBlogInfo) {
                // only redisplay if different than local
                if (serverBlogInfo != null && !serverBlogInfo.isSameAs(localBlogInfo)) {
                    showBlogInfo(serverBlogInfo);
                }
            }
        };
        if (mBlogId != 0) {
            ReaderBlogActions.updateBlogInfo(mBlogId, null, listener);
        } else {
            ReaderBlogActions.updateFeedInfo(mFeedId, null, listener);
        }
    }

    private void showBlogInfo(ReaderBlog blogInfo) {
        if (blogInfo == null) return;

        ViewGroup layoutInfo = (ViewGroup) findViewById(R.id.layout_blog_info);
        ViewGroup layoutDescription = (ViewGroup) findViewById(R.id.layout_blog_description);
        TextView txtDescription = (TextView) layoutInfo.findViewById(R.id.text_blog_description);
        TextView txtFollowCount = (TextView) layoutInfo.findViewById(R.id.text_blog_follow_count);
        ReaderFollowButton followButton = (ReaderFollowButton) layoutInfo.findViewById(R.id.follow_button);

        if (blogInfo.hasDescription()) {
            txtDescription.setText(blogInfo.getDescription());
            layoutDescription.setVisibility(View.VISIBLE);
        } else {
            layoutDescription.setVisibility(View.GONE);
        }

        txtFollowCount.setText(String.format(getContext().getString(R.string.reader_label_follow_count), blogInfo.numSubscribers));

        followButton.setIsFollowed(blogInfo.isFollowing);
        followButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFollowStatus((ReaderFollowButton) v);
            }
        });

        if (layoutInfo.getVisibility() != View.VISIBLE) {
            layoutInfo.setVisibility(View.VISIBLE);
        }

        if (mBlogInfoListener != null) {
            mBlogInfoListener.onBlogInfoLoaded(blogInfo);
        }
    }

    private void toggleFollowStatus(final ReaderFollowButton followButton) {
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
                if (!succeeded && getContext() != null) {
                    int errResId = isAskingToFollow ? R.string.reader_toast_err_follow_blog : R.string.reader_toast_err_unfollow_blog;
                    ToastUtils.showToast(getContext(), errResId);
                    followButton.setIsFollowed(!isAskingToFollow);
                }
            }
        };

        boolean result;
        if (mFeedId != 0) {
            result = ReaderBlogActions.followFeedById(mFeedId, isAskingToFollow, listener);
        } else {
            result = ReaderBlogActions.followBlogById(mBlogId, isAskingToFollow, listener);
        }

        if (result) {
            followButton.setIsFollowedAnimated(isAskingToFollow);
        }
    }
}
