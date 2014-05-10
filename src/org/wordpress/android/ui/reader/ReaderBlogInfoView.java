package org.wordpress.android.ui.reader;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderBlogInfo;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/*
 * header view showing blog name, description, follower count, follow button, and
 * mshot of the blog - designed specifically for use in ReaderPostListFragment
 * when previewing posts in a blog (blog preview)
 */
class ReaderBlogInfoView extends FrameLayout {
    private WPNetworkImageView mImageMshot;
    private ViewGroup mInfoContainerView;
    private ViewGroup mMshotContainerView;

    private float mCurrentMshotScale = 1.0f;
    private boolean mHasLoadedMshot;
    private boolean mHasLoadedInfo;

    private int mMshotWidth;
    private int mMshotDefaultHeight;

    public ReaderBlogInfoView(Context context){
        super(context);
        inflateView(context);
    }
    public ReaderBlogInfoView(Context context, AttributeSet attributes){
        super(context, attributes);
        inflateView(context);
    }
    public ReaderBlogInfoView(Context context, AttributeSet attributes, int defStyle){
        super(context, attributes, defStyle);
        inflateView(context);
    }

    private void inflateView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.reader_blog_info_view, this, true);

        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        int marginWidth = context.getResources().getDimensionPixelSize(R.dimen.reader_list_margin);
        mMshotWidth = displayWidth - (marginWidth * 2);
        mMshotDefaultHeight = context.getResources().getDimensionPixelSize(R.dimen.reader_mshot_image_height);

        mImageMshot = (WPNetworkImageView) view.findViewById(R.id.image_mshot);
        mImageMshot.setImageType(WPNetworkImageView.ImageType.MSHOT);

        mInfoContainerView = (ViewGroup) view.findViewById(R.id.layout_bloginfo_container);
        mMshotContainerView = (ViewGroup) view.findViewById(R.id.layout_mshot_container);
    }

    /*
     * shows the blogInfo from local db (if available) then request latest blogInfo from server
     */
    public void setBlogIdAndUrl(long blogId, String blogUrl) {
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

        if (blogInfo != null) {
            mHasLoadedInfo = true;
        }

        // don't show blogInfo until it's complete (has either a name or description)
        if ((blogInfo != null && !blogInfo.isIncomplete())) {
            mInfoContainerView.setVisibility(View.VISIBLE);

            if (blogInfo.hasName()) {
                txtBlogName.setText(blogInfo.getName());
                // tapping the blog name opens the blog in the browser
                txtBlogName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ReaderActivityLauncher.openUrl(getContext(), blogInfo.getUrl());
                    }
                });
            }

            if (blogInfo.hasDescription()) {
                txtDescription.setText(blogInfo.getDescription());
                txtDescription.setVisibility(View.VISIBLE);
            } else {
                txtDescription.setVisibility(View.GONE);
            }

            // only show the follower count if there are subscribers
            if (blogInfo.numSubscribers > 0) {
                String numFollowers = getResources().getString(R.string.reader_label_followers,
                        FormatUtils.formatInt(blogInfo.numSubscribers));
                txtFollowCnt.setText(numFollowers);
                txtFollowCnt.setVisibility(View.VISIBLE);
            } else {
                txtFollowCnt.setVisibility(View.INVISIBLE);
            }

            ReaderUtils.showFollowStatus(txtFollowBtn, blogInfo.isFollowing);
            txtFollowBtn.setVisibility(View.VISIBLE);
            txtFollowBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleBlogFollowStatus(txtFollowBtn, blogInfo);
                }
            });

            // show the mshot if it hasn't already been shown
            if (!mHasLoadedMshot) {
                loadMshotImage(blogInfo);
            }
        } else {
            mInfoContainerView.setVisibility(View.INVISIBLE);
        }
    }

    public boolean hasLoaded() {
        return mHasLoadedInfo;
    }

    /*
    * request latest info for this blog
    */
    private void requestBlogInfo(long blogId, String blogUrl) {
        ReaderActions.UpdateBlogInfoListener listener = new ReaderActions.UpdateBlogInfoListener() {
            @Override
            public void onResult(ReaderBlogInfo blogInfo) {
                if (blogInfo != null) {
                    showBlogInfo(blogInfo);
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
            ReaderUtils.showFollowStatus(txtFollow, isAskingToFollow);
        }
    }

    private void loadMshotImage(final ReaderBlogInfo blogInfo) {
        // can't get mshot for private blogs
        if (blogInfo == null || blogInfo.isPrivate) {
            hideMshotProgress();
            return;
        }

        WPNetworkImageView.ImageListener imageListener = new WPNetworkImageView.ImageListener() {
            @Override
            public void onImageLoaded(boolean succeeded) {
                hideMshotProgress();
                if (succeeded) {
                    mHasLoadedMshot = true;
                }
            }
        };
        final String imageUrl = blogInfo.getMshotsUrl(mMshotWidth);
        mImageMshot.setImageUrl(imageUrl, WPNetworkImageView.ImageType.MSHOT, imageListener);
    }

    /*
     * hide the progress bar that appears on the mshot - note that it's set to visible at
     * design time, so it'll stay visible until this is called
     */
    private void hideMshotProgress() {
        final ProgressBar progress = (ProgressBar) findViewById(R.id.progress_mshot);
        progress.setVisibility(View.GONE);
    }

    /*
     * scale the mshot image based on the scroll position of ReaderPostListFragment's listView
     */
    public void scaleMshotImageBasedOnScrollPos(int scrollPos) {
        // calculate the mshot scale based on the listView's scroll position
        float scale = Math.max(0f, 0.9f + (-scrollPos * 0.005f));
        if (scale != mCurrentMshotScale) {
            float centerX = mMshotWidth * 0.5f;
            Matrix matrix = new Matrix();
            matrix.setScale(scale, scale, centerX, 0);
            mImageMshot.setImageMatrix(matrix);
            mCurrentMshotScale = scale;
        }
    }

    /*
     * sets the top of the container view holding the info
     */
    public void setInfoContainerTop(int top) {
        if (mInfoContainerView.getTranslationY() != top) {
            mInfoContainerView.setTranslationY(top);

            // force the mshot container to match the bottom of the info container to
            // prevent the bottom of the mshot from appearing below the info
            int infoBottom = top + mInfoContainerView.getHeight();

            LayoutParams mshotParams = (LayoutParams) mMshotContainerView.getLayoutParams();
            if (mshotParams.height != infoBottom) {
                mshotParams.height = infoBottom;
                requestLayout();
            }
        }
    }

    public int getInfoContainerHeight() {
        return mInfoContainerView.getHeight();
    }

    public int getMshotDefaultHeight() {
        return mMshotDefaultHeight;
    }
}