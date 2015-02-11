package org.wordpress.android.ui.reader.views;

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
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderAnim;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPNetworkImageView.ImageType;

/*
 * header view showing blog name, description, follower count, and mshot of the
 * blog - designed for use in ReaderPostListFragment when previewing posts in a
 * blog (blog preview) but can be reused elsewhere - call loadBlogInfo() to show
 * the info for a specific blog
 */
public class ReaderBlogInfoView extends FrameLayout {

    public interface BlogInfoListener {
        void onBlogInfoLoaded(ReaderBlog blogInfo);
        void onBlogInfoFailed();
    }

    private BlogInfoListener mBlogInfoListener;
    private ReaderBlog mBlogInfo;

    public ReaderBlogInfoView(Context context){
        super(context);

        View view = LayoutInflater.from(context).inflate(R.layout.reader_blog_info_view, this, true);
        view.setId(R.id.layout_blog_info_view);
    }

    /*
     * shows the blogInfo from local db (if available) then request latest blogInfo from server
     */
    public void loadBlogInfo(long blogId, BlogInfoListener blogInfoListener) {
        mBlogInfoListener = blogInfoListener;
        showBlogInfo(ReaderBlogTable.getBlogInfo(blogId), false);
        requestBlogInfo(blogId);
    }
    public void loadFeedInfo(long feedId, BlogInfoListener blogInfoListener) {
        mBlogInfoListener = blogInfoListener;
        showBlogInfo(ReaderBlogTable.getBlogInfo(feedId), false);
        requestFeedInfo(feedId);
    }

    /*
     * show blog header with info from passed blog filled in
     */
    private void showBlogInfo(final ReaderBlog blogInfo, boolean animateIn) {
        // this is the layout containing the blog info views
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

        final TextView txtBlogName = (TextView) layoutInner.findViewById(R.id.text_blog_name);
        final TextView txtDescription = (TextView) layoutInner.findViewById(R.id.text_blog_description);
        final WPNetworkImageView imgBlavatar = (WPNetworkImageView) layoutInner.findViewById(R.id.image_blavatar);

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

        int blavatarSz = getResources().getDimensionPixelSize(R.dimen.blavatar_sz_large);
        int imageResId = (blogInfo.isExternal() ? R.drawable.gravatar_placeholder : R.drawable.app_icon);
        imgBlavatar.setDefaultImageResId(imageResId);
        imgBlavatar.setErrorImageResId(imageResId);
        imgBlavatar.setImageUrl(GravatarUtils.blavatarFromUrl(blogInfo.getUrl(), blavatarSz), ImageType.BLAVATAR);

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

    private void requestBlogInfo(long blogId) {
        ReaderBlogActions.updateBlogInfo(blogId, null, mInfoListener);
    }

    private void requestFeedInfo(long feedId) {
        ReaderBlogActions.updateFeedInfo(feedId, mInfoListener);
    }

    ReaderActions.UpdateBlogInfoListener mInfoListener = new ReaderActions.UpdateBlogInfoListener() {
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

}