package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.ui.reader.ReaderInterfaces.OnFollowListener;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.PhotonUtils.Quality;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import javax.inject.Inject;

/**
 * topmost view in post adapter when showing blog preview - displays description, follower
 * count, and follow button
 */
public class ReaderSiteHeaderView extends LinearLayout {
    private final int mBlavatarSz;

    public interface OnBlogInfoLoadedListener {
        void onBlogInfoLoaded(ReaderBlog blogInfo);
    }

    private long mBlogId;
    private long mFeedId;
    // Represents the true siteId/blogId for a feed situation (blogId == feedId)
    private long mSiteId;
    private ReaderFollowButton mFollowButton;
    private ReaderBlog mBlogInfo;
    private OnBlogInfoLoadedListener mBlogInfoListener;
    private OnFollowListener mFollowListener;

    @Inject AccountStore mAccountStore;
    @Inject ImageManager mImageManager;

    public ReaderSiteHeaderView(Context context) {
        this(context, null);
    }

    public ReaderSiteHeaderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReaderSiteHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ((WordPress) context.getApplicationContext()).component().inject(this);
        mBlavatarSz = getResources().getDimensionPixelSize(R.dimen.blavatar_sz_extra_large);
        initView(context);
    }

    private void initView(Context context) {
        View view = inflate(context, R.layout.reader_site_header_view, this);
        mFollowButton = view.findViewById(R.id.follow_button);
    }

    public void setOnFollowListener(OnFollowListener listener) {
        mFollowListener = listener;
    }

    public void setOnBlogInfoLoadedListener(OnBlogInfoLoadedListener listener) {
        mBlogInfoListener = listener;
    }

    public void loadBlogInfo(long blogId, long feedId) {
        mBlogId = blogId;
        mFeedId = feedId;

        final ReaderBlog localBlogInfo;

        // In case this is a feed request, grab the actual blogId/siteId for the feed from the post table.
        // When it's a feed situation, blogId is set to the value of feedId before it gets here. We need the
        // actual blogId/siteId for the feed so we can make a request for the icon image.
        if (blogId == feedId || blogId == 0) {
            mSiteId = ReaderPostTable.getBlogIdForFeed(mFeedId);
        } else {
            mSiteId = 0L;
        }

        // first get info from local db - always check by feedId first, following by blogId - why?
        // Because in the case of feeds, the blogId is set to the same value as feedId AND we happen to
        // be following a blogId that matches our feedId, then that record is returned from the db therefore
        // we will have the wrong data.
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
                    if (isAttachedToWindow()) {
                        showBlogInfo(serverBlogInfo);
                        handleMissingImage(mSiteId, mFeedId, serverBlogInfo);
                    }
                }
            };

            if (mFeedId != 0) {
                ReaderBlogActions.updateFeedInfo(mFeedId, null, listener);
            } else {
                ReaderBlogActions.updateBlogInfo(mBlogId, null, listener);
            }
        } else {
            // if we don't need to go to the server, make sure we have an image to use on the header
            handleMissingImage(mSiteId, mFeedId, localBlogInfo);
        }
    }

    /**
     * If the blogInfo does not contain an image, do a call out to get if from /read/sites/{siteId}
     * @param blogIdForFeed
     * @param feedId
     * @param blogInfo
     */
    private void handleMissingImage(long blogIdForFeed, long feedId, ReaderBlog blogInfo) {
        if (blogIdForFeed > 0 && blogInfo != null && !blogInfo.hasImageUrl()) {
            ReaderActions.UpdateBlogInfoListener listener = serverBlogInfo -> {
                if (isAttachedToWindow()) {
                    updateImage(blogInfo, serverBlogInfo);
                }
            };
            ReaderBlogActions.updateImageForFeedByBlog(blogIdForFeed, null, feedId, listener);
        }
    }

    private void updateImage(ReaderBlog blogInfo, ReaderBlog serverBlogInfo) {
        ViewGroup layoutInfo = findViewById(R.id.layout_blog_info);
        ImageView blavatarImg = layoutInfo.findViewById(R.id.image_blavatar);
        if (serverBlogInfo.hasImageUrl()) {
            mImageManager.loadIntoCircle(blavatarImg, ImageType.BLAVATAR_CIRCULAR,
                   PhotonUtils.getPhotonImageUrl(serverBlogInfo.getImageUrl(), mBlavatarSz, mBlavatarSz, Quality.HIGH));
        }
        blogInfo.setImageUrl(serverBlogInfo.getImageUrl());
        if (mBlogInfoListener != null) {
            mBlogInfoListener.onBlogInfoLoaded(blogInfo);
        }
    }

    private void showBlogInfo(ReaderBlog blogInfo) {
        // do nothing if unchanged
        if (blogInfo == null || blogInfo.isSameAs(mBlogInfo)) {
            return;
        }

        mBlogInfo = blogInfo;

        ViewGroup layoutInfo = findViewById(R.id.layout_blog_info);
        TextView txtBlogName = layoutInfo.findViewById(R.id.text_blog_name);
        TextView txtDomain = layoutInfo.findViewById(R.id.text_domain);
        TextView txtDescription = layoutInfo.findViewById(R.id.text_blog_description);
        TextView txtFollowCount = layoutInfo.findViewById(R.id.text_blog_follow_count);
        ImageView blavatarImg = layoutInfo.findViewById(R.id.image_blavatar);

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

        if (blogInfo.hasImageUrl()) {
            mImageManager.loadIntoCircle(blavatarImg, ImageType.BLAVATAR_CIRCULAR,
                    PhotonUtils.getPhotonImageUrl(blogInfo.getImageUrl(), mBlavatarSz, mBlavatarSz, Quality.HIGH));
        }

        txtFollowCount.setText(String.format(
                LocaleManager.getSafeLocale(getContext()),
                getContext().getString(R.string.reader_label_follow_count),
                blogInfo.numSubscribers));

        if (!mAccountStore.hasAccessToken()) {
            mFollowButton.setVisibility(View.GONE);
        } else {
            mFollowButton.setVisibility(View.VISIBLE);
            mFollowButton.setIsFollowed(blogInfo.isFollowing);
            mFollowButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollowStatus(v);
                }
            });
        }

        if (layoutInfo.getVisibility() != View.VISIBLE) {
            layoutInfo.setVisibility(View.VISIBLE);
        }

        if (mBlogInfoListener != null) {
            mBlogInfoListener.onBlogInfoLoaded(blogInfo);
        }
    }

    private void toggleFollowStatus(final View followButton) {
        if (!NetworkUtils.checkConnection(getContext())) {
            return;
        }

        final boolean isAskingToFollow;
        if (mFeedId != 0) {
            isAskingToFollow = !ReaderBlogTable.isFollowedFeed(mFeedId);
        } else {
            isAskingToFollow = !ReaderBlogTable.isFollowedBlog(mBlogId);
        }

        if (mFollowListener != null) {
            if (isAskingToFollow) {
                mFollowListener.onFollowTapped(
                        followButton,
                        mBlogInfo.getName(),
                        mSiteId > 0 ? mSiteId : mBlogInfo.blogId);
            } else {
                mFollowListener.onFollowingTapped();
            }
        }

        ReaderActions.ActionListener listener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (getContext() == null) {
                    return;
                }
                mFollowButton.setEnabled(true);
                if (!succeeded) {
                    int errResId = isAskingToFollow ? R.string.reader_toast_err_follow_blog
                            : R.string.reader_toast_err_unfollow_blog;
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
