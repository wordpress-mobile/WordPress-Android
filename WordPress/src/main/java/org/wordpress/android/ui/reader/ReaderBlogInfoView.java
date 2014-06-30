package org.wordpress.android.ui.reader;

import android.content.Context;
import android.graphics.Matrix;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/*
 * header view showing blog name, description, follower count, follow button, and
 * mshot of the blog - designed specifically for use in ReaderPostListFragment
 * when previewing posts in a blog (blog preview)
 */
class ReaderBlogInfoView extends FrameLayout {
    public interface BlogInfoListener {
        void onBlogInfoLoaded();
        void onBlogInfoFailed();
    }
    private BlogInfoListener mBlogInfoListener;

    private final WPNetworkImageView mImageMshot;
    private final ViewGroup mInfoContainerView;
    private final ProgressBar mMshotProgress;

    private final int mMshotWidth;
    private final int mMshotDefaultHeight;

    private float mCurrentMshotScale = 1.0f;
    private ReaderBlog mBlogInfo;

    public ReaderBlogInfoView(Context context){
        super(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.reader_blog_info_view, this, true);
        view.setId(R.id.layout_blog_info_view);

        mMshotDefaultHeight = context.getResources().getDimensionPixelSize(R.dimen.reader_mshot_image_height);
        mMshotWidth = (int) (mMshotDefaultHeight * 1.33f);

        mImageMshot = (WPNetworkImageView) view.findViewById(R.id.image_mshot);
        mInfoContainerView = (ViewGroup) view.findViewById(R.id.layout_bloginfo_container);

        // position the progressBar halfway down the mshot - done this way to avoid it
        // moving when the mshot container is resized
        mMshotProgress = (ProgressBar) view.findViewById(R.id.progress_mshot);
        mMshotProgress.setTranslationY(mMshotDefaultHeight / 2);
    }

    /*
     * shows the blogInfo from local db (if available) then request latest blogInfo from server
     */
    public void loadBlogInfo(long blogId, String blogUrl, BlogInfoListener blogInfoListener) {
        mBlogInfoListener = blogInfoListener;
        showBlogInfo(ReaderBlogTable.getBlogInfo(blogId, blogUrl));
        requestBlogInfo(blogId, blogUrl);
    }

    /*
     * show blog header with info from passed blog filled in
     */
    private void showBlogInfo(final ReaderBlog blogInfo) {
        final ViewGroup layoutInner = (ViewGroup) findViewById(R.id.layout_bloginfo_container_inner);

        if (blogInfo == null) {
            layoutInner.setVisibility(View.INVISIBLE);
            return;
        }

        // do nothing if blogInfo hasn't changed
        if (mBlogInfo != null && mBlogInfo.isSameAs(blogInfo)) {
            return;
        }

        mBlogInfo = blogInfo;
        layoutInner.setVisibility(View.VISIBLE);

        final TextView txtBlogName = (TextView) findViewById(R.id.text_blog_name);
        final TextView txtDescription = (TextView) findViewById(R.id.text_blog_description);
        final TextView txtFollowCnt = (TextView) findViewById(R.id.text_follow_count);
        final TextView txtFollowBtn = (TextView) findViewById(R.id.text_follow_blog);

        if (blogInfo.hasUrl()) {
            // clicking the blog name or mshot shows the blog in the browser
            View.OnClickListener urlClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.openUrl(getContext(), blogInfo.getUrl());
                }
            };
            txtBlogName.setOnClickListener(urlClickListener);
            mImageMshot.setOnClickListener(urlClickListener);
        }

        if (blogInfo.hasName()) {
            txtBlogName.setText(blogInfo.getName());
        }

        if (blogInfo.hasDescription()) {
            txtDescription.setText(blogInfo.getDescription());
            txtDescription.setVisibility(View.VISIBLE);
        } else if (blogInfo.hasUrl()) {
            txtDescription.setText(UrlUtils.getDomainFromUrl(blogInfo.getUrl()));
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
        if (mImageMshot.getUrl() == null) {
            loadMshotImage(blogInfo);
        }

        if (mBlogInfoListener != null) {
            mBlogInfoListener.onBlogInfoLoaded();
        }
    }

    public boolean isEmpty() {
        return mBlogInfo == null;
    }

    /*
    * request latest info for this blog
    */
    private void requestBlogInfo(long blogId, String blogUrl) {
        ReaderActions.UpdateBlogInfoListener listener = new ReaderActions.UpdateBlogInfoListener() {
            @Override
            public void onResult(ReaderBlog blogInfo) {
                if (blogInfo != null) {
                    showBlogInfo(blogInfo);
                } else {
                    hideProgress();
                    if (isEmpty() && mBlogInfoListener != null) {
                        mBlogInfoListener.onBlogInfoFailed();
                    }

                }
            }
        };
        ReaderBlogActions.updateBlogInfo(blogId, blogUrl, listener);
    }

    private void toggleBlogFollowStatus(TextView txtFollow, ReaderBlog blogInfo) {
        if (blogInfo == null || txtFollow == null) {
            return;
        }

        ReaderAnim.animateFollowButton(txtFollow);

        boolean isAskingToFollow = !blogInfo.isFollowing;
        if (ReaderBlogActions.performFollowAction(blogInfo.blogId, blogInfo.getUrl(), isAskingToFollow, null)) {
            ReaderUtils.showFollowStatus(txtFollow, isAskingToFollow);
        }
    }

    private void loadMshotImage(final ReaderBlog blogInfo) {
        if (blogInfo == null || !blogInfo.hasUrl()) {
            hideProgress();
            return;
        }

        // mshot for private blogs will just be a login screen, so show a lock icon
        // instead of requesting the mshot
        if (blogInfo.isPrivate) {
            hideProgress();
            mImageMshot.setScaleType(ImageView.ScaleType.CENTER);
            mImageMshot.setImageResource(R.drawable.ic_action_secure);
            return;
        }

        WPNetworkImageView.ImageListener imageListener = new WPNetworkImageView.ImageListener() {
            @Override
            public void onImageLoaded(boolean succeeded) {
                hideProgress();
            }
        };
        final String imageUrl = blogInfo.getMshotsUrl(mMshotWidth);
        mImageMshot.setImageUrl(imageUrl, WPNetworkImageView.ImageType.MSHOT, imageListener);
    }

    /*
     * hide the progress bar that appears on the mshot - note that it's set to visible at
     * design time, so it'll stay visible until this is called
     */
    private void hideProgress() {
        mMshotProgress.setVisibility(View.GONE);
    }

    /*
     * scale the mshot image based on the scroll position of ReaderPostListFragment's listView
     */
    public void scaleMshotImageBasedOnScrollPos(int scrollPos) {
        float scale = Math.max(0f, 0.9f + (-scrollPos * 0.0025f));
        if (scale != mCurrentMshotScale) {
            float centerX = mMshotWidth * 0.5f;
            float centerY = mMshotDefaultHeight * 0.5f;
            Matrix matrix = new Matrix();
            matrix.setScale(scale, scale, centerX, centerY);
            mImageMshot.setImageMatrix(matrix);
            mCurrentMshotScale = scale;
        }
    }

    /*
     * sets the top of the container view holding the info (ie: everything except the mshot)
     */
    public void moveInfoContainer(int top) {
        if (mInfoContainerView.getTranslationY() != top) {
            mInfoContainerView.setTranslationY(top);

            // force the container to match the bottom of the info container to
            // prevent the bottom of the mshot from appearing below the info
            int infoBottom = top + mInfoContainerView.getHeight();
            ViewGroup.LayoutParams params = this.getLayoutParams();
            if (params.height != infoBottom) {
                params.height = infoBottom;
                requestLayout();
            }
        }
    }

    public int getInfoContainerHeight() {
        return mInfoContainerView.getHeight();
    }

    public int getMshotHeight() {
        return mMshotDefaultHeight;
    }
}