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

/*
 * header view showing blog name, description, follower count & follow button
 * designed for use on ReaderPostListFragment, which shows an mshot of the blog
 */
public class ReaderBlogInfoHeader extends LinearLayout {
    private boolean mHasBlogInfo;

    protected interface OnBlogInfoListener {
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
        View view = inflater.inflate(R.layout.reader_blog_info_header, this, true);

        // adjust the spacer so the info overlaps the mshot image on ReaderPostListFragment
        /*View spacer = view.findViewById(R.id.view_mshot_spacer);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) spacer.getLayoutParams();
        int mshotHeight = getResources().getDimensionPixelSize(R.dimen.reader_mshot_image_height);
        params.height = (int)(mshotHeight * 0.75f);*/
    }

    public void setBlogId(long blogId, OnBlogInfoListener listener) {
        mListener = listener;
        showBlogInfo(ReaderBlogTable.getBlogInfo(blogId));
        requestBlogInfo(blogId);
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

        mHasBlogInfo = (blogInfo != null);

        if (mHasBlogInfo) {
            txtBlogName.setText(blogInfo.getName());
            txtDescription.setText(blogInfo.getDescription());
            txtDescription.setVisibility(blogInfo.hasDescription() ? View.VISIBLE : View.GONE);
            String numFollowers = getResources().getString(R.string.reader_label_followers, FormatUtils.formatInt(blogInfo.numSubscribers));
            txtFollowCnt.setText(numFollowers);

            boolean isFollowing = ReaderBlogTable.isFollowedBlogUrl(blogInfo.getUrl());
            showBlogFollowStatus(txtFollowBtn, isFollowing);
            txtFollowBtn.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);

            View.OnClickListener urlListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.openUrl(getContext(), blogInfo.getUrl());
                }
            };

            // tapping the spacer opens the blog in the browser - will appear to the user
            // that they tapped the mshot image
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
    private void requestBlogInfo(final long blogId) {
        if (!mHasBlogInfo) {
            showProgress();
        }
        ReaderActions.RequestBlogInfoListener listener = new ReaderActions.RequestBlogInfoListener() {
            @Override
            public void onResult(ReaderBlogInfo blogInfo) {
                hideProgress();
                if (blogInfo != null) {
                    showBlogInfo(blogInfo);
                }
            }
        };
        ReaderBlogActions.updateBlogInfo(blogId, listener);
    }

    private void toggleBlogFollowStatus(final TextView txtFollow, final ReaderBlogInfo blogInfo) {
        if (blogInfo == null || txtFollow == null) {
            return;
        }

        AniUtils.zoomAction(txtFollow);

        boolean isCurrentlyFollowing = ReaderBlogTable.isFollowedBlogUrl(blogInfo.getUrl());
        ReaderBlogActions.FollowAction action = (isCurrentlyFollowing ? ReaderBlogActions.FollowAction.UNFOLLOW : ReaderBlogActions.FollowAction.FOLLOW);
        if (!ReaderBlogActions.performFollowAction(action, blogInfo.blogId, blogInfo.getUrl())) {
            return;
        }

        boolean isNowFollowing = !isCurrentlyFollowing;
        showBlogFollowStatus(txtFollow, isNowFollowing);
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