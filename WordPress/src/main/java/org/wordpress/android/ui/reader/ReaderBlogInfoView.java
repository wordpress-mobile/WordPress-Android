package org.wordpress.android.ui.reader;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/*
 * header view showing blog name, description, follower count, follow button, and
 * mshot of the blog - designed for use in ReaderPostListFragment when previewing posts
 * in a blog (blog preview) but can reused elsewhere
 */
class ReaderBlogInfoView extends FrameLayout {
    public interface BlogInfoListener {
        void onBlogInfoLoaded();
        void onBlogInfoFailed();
    }
    private BlogInfoListener mBlogInfoListener;

    private final WPNetworkImageView mImageMshot;
    private final int mMshotWidth;
    private ReaderBlog mBlogInfo;

    public ReaderBlogInfoView(Context context){
        super(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.reader_blog_info_view, this, true);
        view.setId(R.id.layout_blog_info_view);

        mMshotWidth = context.getResources().getDimensionPixelSize(R.dimen.reader_mshot_image_width);
        mImageMshot = (WPNetworkImageView) view.findViewById(R.id.image_mshot);
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
        final ViewGroup layoutInner = (ViewGroup) findViewById(R.id.layout_bloginfo_inner);

        if (blogInfo == null) {
            layoutInner.setVisibility(View.INVISIBLE);
            return;
        }

        // do nothing if blogInfo hasn't changed
        if (mBlogInfo != null && mBlogInfo.isSameAs(blogInfo)) {
            return;
        }

        mBlogInfo = blogInfo;

        final TextView txtBlogName = (TextView) findViewById(R.id.text_blog_name);
        final TextView txtDescription = (TextView) findViewById(R.id.text_blog_description);
        final TextView txtFollowCnt = (TextView) findViewById(R.id.text_follow_count);
        final TextView txtFollowBtn = (TextView) findViewById(R.id.text_follow_blog);

        if (blogInfo.hasUrl()) {
            // clicking the blog name shows the blog in the browser
            txtBlogName.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.openUrl(getContext(), blogInfo.getUrl());
                }
            });
        }

        if (blogInfo.hasName()) {
            txtBlogName.setText(blogInfo.getName());
        } else if (blogInfo.hasUrl()) {
            txtBlogName.setText(UrlUtils.getDomainFromUrl(blogInfo.getUrl()));
        } else {
            txtBlogName.setText(getContext().getString(R.string.reader_untitled_post));
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

        String numFollowers = getResources().getString(R.string.reader_label_followers,
                FormatUtils.formatInt(blogInfo.numSubscribers));
        txtFollowCnt.setText(numFollowers);

        ReaderUtils.showFollowStatus(txtFollowBtn, blogInfo.isFollowing);
        txtFollowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleBlogFollowStatus(txtFollowBtn, blogInfo);
            }
        });

        // layout is invisible at design time, animate it in after a brief delay
        // the first time it's shown
        if (layoutInner.getVisibility() != View.VISIBLE) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ReaderAnim.scaleIn(layoutInner, ReaderAnim.Duration.MEDIUM);
                }
            }, 250);
        }

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
                } else if (isEmpty() && mBlogInfoListener != null) {
                    mBlogInfoListener.onBlogInfoFailed();
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
            return;
        }

        // mshot for private blogs will just be a login screen, so show a lock icon
        // instead of requesting the mshot
        if (blogInfo.isPrivate) {
            mImageMshot.setImageResource(R.drawable.ic_action_secure);
            return;
        }

        WPNetworkImageView.ImageListener imageListener = new WPNetworkImageView.ImageListener() {
            @Override
            public void onImageLoaded(boolean succeeded) {
                if (succeeded) {
                    mImageMshot.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // now that the mshot has loaded, show full-size mshot
                            // in photo viewer when tapped
                            int displayWidth = DisplayUtils.getDisplayPixelWidth(getContext());
                            String mshotUrlFull = blogInfo.getMshotsUrl(displayWidth);
                            ReaderActivityLauncher.showReaderPhotoViewer(getContext(), mshotUrlFull);
                        }
                    });
                }
            }
        };

        String mshotUrl = blogInfo.getMshotsUrl(mMshotWidth);
        mImageMshot.setImageUrl(mshotUrl, WPNetworkImageView.ImageType.MSHOT, imageListener);
    }

}