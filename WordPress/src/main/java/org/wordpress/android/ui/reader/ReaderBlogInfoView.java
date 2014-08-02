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
 * header view showing blog name, description, follower count, follow button, and mshot
 * of the blog - designed for use in ReaderPostListFragment when previewing posts in a
 * blog (blog preview) but can reused elsewhere
 */
class ReaderBlogInfoView extends FrameLayout {
    public interface BlogInfoListener {
        void onBlogInfoLoaded();
        void onBlogInfoFailed();
    }

    private final WPNetworkImageView mImageMshot;
    private BlogInfoListener mBlogInfoListener;
    private ReaderBlog mBlogInfo;

    public ReaderBlogInfoView(Context context){
        super(context);
        View view = LayoutInflater.from(context).inflate(R.layout.reader_blog_info_view, this, true);
        view.setId(R.id.layout_blog_info_view);
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
        // this is the layout containing all of the blog info, including the mshot
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

        showFollowerCount(blogInfo.numSubscribers);
        ReaderUtils.showFollowStatus(txtFollowBtn, blogInfo.isFollowing);
        txtFollowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleBlogFollowStatus(txtFollowBtn);
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

    private void showFollowerCount(int numFollowers) {
        final TextView txtFollowCnt = (TextView) findViewById(R.id.text_follow_count);
        String count = getResources().getString(
                R.string.reader_label_followers,
                FormatUtils.formatInt(numFollowers));
        txtFollowCnt.setText(count);
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

    private void toggleBlogFollowStatus(final TextView txtFollow) {
        if (mBlogInfo == null || txtFollow == null) {
            return;
        }

        final boolean isAskingToFollow = !mBlogInfo.isFollowing;
        final int currentCount = mBlogInfo.numSubscribers;

        ReaderActions.ActionListener followListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                // revert to original state if follow/unfollow failed
                if (!succeeded) {
                    mBlogInfo.isFollowing = !isAskingToFollow;
                    mBlogInfo.numSubscribers = currentCount;
                    showFollowerCount(currentCount);
                    ReaderUtils.showFollowStatus(txtFollow, !isAskingToFollow);
                }
            }
        };

        ReaderAnim.animateFollowButton(txtFollow);

        if (ReaderBlogActions.performFollowAction(
                mBlogInfo.blogId,
                mBlogInfo.getUrl(),
                isAskingToFollow,
                followListener)) {
            int newCount;
            if (isAskingToFollow) {
                newCount = currentCount + 1;
            } else {
                newCount = (currentCount > 0 ? currentCount - 1 : currentCount);
            }
            mBlogInfo.isFollowing = isAskingToFollow;
            mBlogInfo.numSubscribers = newCount;
            showFollowerCount(newCount);
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
                            // now that the mshot has loaded, show full-size mshot when tapped
                            int displayWidth = DisplayUtils.getDisplayPixelWidth(getContext());
                            int startX = mImageMshot.getLeft();
                            int startY = mImageMshot.getTop();
                            ReaderActivityLauncher.showReaderPhotoViewer(
                                    getContext(),
                                    blogInfo.getMshotsUrl(displayWidth),
                                    mImageMshot,
                                    startX,
                                    startY);
                        }
                    });
                }
            }
        };

        int mshotWidth = getContext().getResources().getDimensionPixelSize(R.dimen.reader_mshot_image_width);
        mImageMshot.setImageUrl(
                blogInfo.getMshotsUrl(mshotWidth),
                WPNetworkImageView.ImageType.MSHOT,
                imageListener);
    }

}