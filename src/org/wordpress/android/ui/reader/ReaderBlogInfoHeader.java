package org.wordpress.android.ui.reader;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderBlogInfo;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.ToastUtils;

/*
 * header view showing blog name, description, follower count & follow button
 * designed for use on ReaderPostListFragment, which shows an mshot of the blog
 */
class ReaderBlogInfoHeader extends LinearLayout {
    private boolean mHasBlogInfo;

    interface OnBlogInfoListener {
        void onBlogInfoShown(ReaderBlogInfo blogInfo);
    }
    private OnBlogInfoListener mListener;

    public ReaderBlogInfoHeader(Context context){
        super(context);
        inflateView(context);
    }
    public ReaderBlogInfoHeader(Context context, AttributeSet attributes){
        super(context, attributes);
        inflateView(context);
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public ReaderBlogInfoHeader(Context context, AttributeSet attributes, int defStyle){
        super(context, attributes, defStyle);
        inflateView(context);
    }

    private void inflateView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.reader_blog_info_header, this, true);
    }

    public void setBlogIdAndUrl(long blogId, String blogUrl, OnBlogInfoListener listener) {
        mListener = listener;
        showBlogInfo(ReaderBlogTable.getBlogInfo(blogId, blogUrl));
        requestBlogInfo(blogId, blogUrl);
    }

    /*
     * show blog header with info from passed blog filled in
     */
    private void showBlogInfo(final ReaderBlogInfo blogInfo) {
        final TextView txtBlogName = (TextView) findViewById(R.id.text_blog_name);
        final TextView txtDescription = (TextView) findViewById(R.id.text_blog_description);
        final TextView txtFollowCnt = (TextView) findViewById(R.id.text_follow_count);
        final TextView txtFollowBtn = (TextView) findViewById(R.id.text_follow_blog);
        final View divider = findViewById(R.id.divider);
        final View spacer = findViewById(R.id.view_header_image_spacer);

        // make sure the blog info is complete - it may have been set by the read/following/mine
        // endpoint which doesn't include the name, description or follower count
        mHasBlogInfo = (blogInfo != null && blogInfo.isComplete());

        if (mHasBlogInfo) {
            txtBlogName.setText(blogInfo.getName());
            txtDescription.setText(blogInfo.getDescription());
            txtDescription.setVisibility(blogInfo.hasDescription() ? View.VISIBLE : View.GONE);

            // only show the follower count if there are subscribers
            if (blogInfo.numSubscribers > 0) {
                String numFollowers = getResources().getString(R.string.reader_label_followers, FormatUtils.formatInt(blogInfo.numSubscribers));
                txtFollowCnt.setText(numFollowers);
                txtFollowCnt.setVisibility(View.VISIBLE);
            } else {
                txtFollowCnt.setVisibility(View.INVISIBLE);
            }

            showBlogFollowStatus(txtFollowBtn, blogInfo.isFollowing);
            txtFollowBtn.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);

            View.OnClickListener urlListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.openUrl(getContext(), blogInfo.getUrl());
                }
            };

            // tapping the spacer opens the blog in the browser - will appear to the user
            // that they tapped the mshot image on ReaderPostListFragment
            spacer.setOnClickListener(urlListener);
            txtBlogName.setOnClickListener(urlListener);

            txtFollowBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleBlogFollowStatus(txtFollowBtn, blogInfo);
                }
            });

            if (mListener != null) {
                mListener.onBlogInfoShown(blogInfo);
            }
        } else {
            txtFollowBtn.setVisibility(View.INVISIBLE);
            divider.setVisibility(View.INVISIBLE);
        }
    }

    /*
    * request latest info for this blog
    */
    private void requestBlogInfo(long blogId, String blogUrl) {
        if (!mHasBlogInfo) {
            showProgress();
        }
        ReaderActions.UpdateBlogInfoListener listener = new ReaderActions.UpdateBlogInfoListener() {
            @Override
            public void onResult(ReaderBlogInfo blogInfo) {
                hideProgress();
                if (blogInfo != null) {
                    showBlogInfo(blogInfo);
                } else if (!mHasBlogInfo) {
                    ToastUtils.showToast(getContext(), R.string.reader_toast_err_get_blog);
                }
            }
        };
        ReaderBlogActions.updateBlogInfo(blogId, blogUrl, listener);
    }

    private void toggleBlogFollowStatus(TextView txtFollow, ReaderBlogInfo blogInfo) {
        if (blogInfo == null || txtFollow == null) {
            return;
        }

        AniUtils.zoomAction(txtFollow);

        boolean isAskingToFollow = !blogInfo.isFollowing;
        if (ReaderBlogActions.performFollowAction(blogInfo.blogId, blogInfo.getUrl(), isAskingToFollow, null)) {
            showBlogFollowStatus(txtFollow, isAskingToFollow);
        }
    }

    /*
     * updates the follow button in the blog header to match whether the current
     * user is following this blog
     */
    private void showBlogFollowStatus(TextView txtFollow, boolean isFollowed) {
        String following = getContext().getString(R.string.reader_btn_unfollow).toUpperCase();
        String follow = getContext().getString(R.string.reader_btn_follow).toUpperCase();

        txtFollow.setSelected(isFollowed);
        txtFollow.setText(isFollowed ? following : follow);
        int drawableId = (isFollowed ? R.drawable.note_icon_following : R.drawable.note_icon_follow);
        txtFollow.setCompoundDrawablesWithIntrinsicBounds(drawableId, 0, 0, 0);
    }

    private void showProgress() {
        ProgressBar progress = (ProgressBar) findViewById(R.id.progress_blog_info);
        progress.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        ProgressBar progress = (ProgressBar) findViewById(R.id.progress_blog_info);
        progress.setVisibility(View.GONE);
    }
}