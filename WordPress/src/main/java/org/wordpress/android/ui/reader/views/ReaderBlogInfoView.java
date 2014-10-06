package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderAnim;
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
 * blog (blog preview) but can be reused elsewhere - call loadBlogInfo() to show the
 * info for a specific blog
 */
public class ReaderBlogInfoView extends LinearLayout {

    public interface BlogInfoListener {
        void onBlogInfoLoaded(ReaderBlog blogInfo);
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

        // set mshot width based on display width
        int displayWidth = DisplayUtils.getDisplayPixelWidth(getContext());
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mImageMshot.getLayoutParams();
        if (DisplayUtils.isLandscape(context)) {
            params.width = (int) (displayWidth * 0.15f);
        } else {
            params.width = (int)(displayWidth * 0.25f);
        }
    }

    /*
     * shows the blogInfo from local db (if available) then request latest blogInfo from server
     */
    public void loadBlogInfo(long blogId, String blogUrl, BlogInfoListener blogInfoListener) {
        mBlogInfoListener = blogInfoListener;
        showBlogInfo(ReaderBlogTable.getBlogInfo(blogId, blogUrl), false);
        requestBlogInfo(blogId, blogUrl);
    }

    /*
     * show blog header with info from passed blog filled in
     */
    private void showBlogInfo(final ReaderBlog blogInfo, boolean animateIn) {
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

        boolean wasEmpty = (mBlogInfo == null);
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

        // layout is invisible at design time
        if (layoutInner.getVisibility() != View.VISIBLE) {
            if (animateIn) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (layoutInner.getVisibility() != View.VISIBLE) {
                            ReaderAnim.fadeIn(layoutInner, ReaderAnim.Duration.SHORT);
                        }
                    }
                }, 500);
            } else {
                layoutInner.setVisibility(View.VISIBLE);
            }
        }

        // show the mshot if it hasn't already been shown
        if (mImageMshot.getUrl() == null) {
            loadMshotImage(blogInfo);
        }

        // fire listener the first time info is loaded
        if (wasEmpty && mBlogInfoListener != null) {
            mBlogInfoListener.onBlogInfoLoaded(blogInfo);
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
                    // animate it in if it wasn't already loaded from local db
                    boolean animateIn = isEmpty();
                    showBlogInfo(blogInfo, animateIn);
                } else if (isEmpty() && mBlogInfoListener != null) {
                    // only fire the failed event if blogInfo is empty - we don't want to fire
                    // the failed event if the blogInfo successfully loaded from the local db
                    // already, since ReaderPostListFragment interprets failure here to mean
                    // the blog is invalid
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
        // instead of requesting the mshot - note adjustViewBounds = true for the
        // imageView, so the lock will resize (ie: it won't be a tiny lock drawable)
        if (blogInfo.isPrivate) {
            mImageMshot.setImageResource(R.drawable.ic_action_secure);
            return;
        }

        // even though the mshot here is a thumbnail, request it using the full width of
        // the display to ensure that the cached mshot will be the same if/when the user
        // taps to view it full size
        int displayWidth = DisplayUtils.getDisplayPixelWidth(getContext());
        final String mshotUrl = blogInfo.getMshotsUrl(displayWidth);

        WPNetworkImageView.ImageListener imageListener = new WPNetworkImageView.ImageListener() {
            @Override
            public void onImageLoaded(boolean succeeded) {
                if (succeeded) {
                    mImageMshot.setOnTouchListener(new OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            // highlight the image on touch down
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                int color = v.getResources().getColor(R.color.blue_extra_light);
                                mImageMshot.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
                            } else {
                                mImageMshot.clearColorFilter();
                            }

                            // show mshot in photo viewer activity on touch up
                            if (event.getAction() == MotionEvent.ACTION_UP) {
                                ReaderActivityLauncher.showReaderPhotoViewer(
                                        getContext(),
                                        mshotUrl,
                                        null,
                                        mImageMshot,
                                        blogInfo.isPrivate,
                                        (int) event.getRawX(),
                                        (int) event.getRawY());
                            }

                            return true;
                        }
                    });
                }
            }
        };

        mImageMshot.setImageUrl(
                mshotUrl,
                WPNetworkImageView.ImageType.MSHOT,
                imageListener);
    }

}