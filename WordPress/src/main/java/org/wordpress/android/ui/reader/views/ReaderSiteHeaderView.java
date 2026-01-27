package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.icu.text.CompactDecimalFormat;
import android.icu.text.NumberFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.ui.reader.ReaderInterfaces.OnFollowListener;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.tracker.ReaderTracker;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.PhotonUtils.Quality;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.image.BlavatarShape;
import org.wordpress.android.util.image.ImageManager;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

/**
 * topmost view in post adapter when showing blog preview - displays description, follower
 * count, and follow button
 */
public class ReaderSiteHeaderView extends LinearLayout {
    private static final int MINIMUM_NUMBER_FOLLOWERS_FORMAT = 10000;

    private final int mBlavatarSz;

    public interface OnBlogInfoLoadedListener {
        void onBlogInfoLoaded(ReaderBlog blogInfo);
    }

    public interface OnBlogInfoFailedListener {
        void onBlogInfoFailed();
    }

    public interface OnSubscriptionSettingsClickListener {
        void onSubscriptionSettingsClicked(long blogId, String blogName, String blogUrl);
    }

    private long mBlogId;
    private long mFeedId;
    private boolean mIsFeed;

    private ReaderFollowButton mFollowButton;
    @Nullable private ProgressBar mFollowProgress;
    @Nullable private ImageButton mSubscriptionSettingsButton;
    private ReaderBlog mBlogInfo;
    @Nullable private WeakReference<OnBlogInfoLoadedListener> mBlogInfoListenerRef;
    @Nullable private WeakReference<OnBlogInfoFailedListener> mBlogInfoFailedListenerRef;
    @Nullable private WeakReference<OnFollowListener> mFollowListenerRef;
    @Nullable private WeakReference<OnSubscriptionSettingsClickListener> mSubscriptionSettingsListenerRef;

    private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    @Inject AccountStore mAccountStore;
    @Inject ImageManager mImageManager;
    @Inject ReaderTracker mReaderTracker;

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
        final View view = inflate(context, R.layout.reader_site_header_view, this);
        mFollowButton = view.findViewById(R.id.follow_button);
        mFollowProgress = view.findViewById(R.id.follow_button_progress);
        mSubscriptionSettingsButton = view.findViewById(R.id.subscription_settings_button);
        view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public void setOnFollowListener(@Nullable OnFollowListener listener) {
        mFollowListenerRef = listener != null ? new WeakReference<>(listener) : null;
    }

    public void setOnBlogInfoLoadedListener(@Nullable OnBlogInfoLoadedListener listener) {
        mBlogInfoListenerRef = listener != null ? new WeakReference<>(listener) : null;
    }

    public void setOnBlogInfoFailedListener(@Nullable OnBlogInfoFailedListener listener) {
        mBlogInfoFailedListenerRef = listener != null ? new WeakReference<>(listener) : null;
    }

    public void setOnSubscriptionSettingsClickListener(@Nullable OnSubscriptionSettingsClickListener listener) {
        mSubscriptionSettingsListenerRef = listener != null ? new WeakReference<>(listener) : null;
    }

    public void loadBlogInfo(
            final long blogId,
            final long feedId,
            final String source
    ) {
        mBlogId = blogId;
        mFeedId = feedId;

        if (blogId == 0 && feedId == 0) {
            ToastUtils.showToast(getContext(), R.string.reader_toast_err_show_blog);
            OnBlogInfoFailedListener failedListener =
                    mBlogInfoFailedListenerRef != null ? mBlogInfoFailedListenerRef.get() : null;
            if (failedListener != null) {
                failedListener.onBlogInfoFailed();
            }
            return;
        }

        mIsFeed = ReaderUtils.isExternalFeed(mBlogId, mFeedId);

        // run in background to avoid ANR
        mExecutorService.execute(() -> {
            final ReaderBlog localBlogInfo;
            if (mIsFeed) {
                localBlogInfo = ReaderBlogTable.getFeedInfo(mFeedId);
            } else {
                localBlogInfo = ReaderBlogTable.getBlogInfo(mBlogId);
            }

            mMainHandler.post(() -> {
                if (localBlogInfo != null) {
                    showBlogInfo(localBlogInfo, source);
                }
                // then get from server if doesn't exist locally or is time to update it
                if (localBlogInfo == null || ReaderBlogTable.isTimeToUpdateBlogInfo(localBlogInfo)) {
                    ReaderActions.UpdateBlogInfoListener listener = serverBlogInfo -> {
                        if (isAttachedToWindow()) {
                            if (serverBlogInfo != null) {
                                showBlogInfo(serverBlogInfo, source);
                            } else if (localBlogInfo == null) {
                                // No local info and server returned null - blog/feed not found
                                OnBlogInfoFailedListener failedListener =
                                        mBlogInfoFailedListenerRef != null
                                                ? mBlogInfoFailedListenerRef.get() : null;
                                if (failedListener != null) {
                                    failedListener.onBlogInfoFailed();
                                }
                            }
                        }
                    };

                    if (mIsFeed) {
                        ReaderBlogActions.updateFeedInfo(mFeedId, null, listener);
                    } else {
                        ReaderBlogActions.updateBlogInfo(mBlogId, null, listener);
                    }
                }
            });
        });
    }

    private void showBlogInfo(ReaderBlog blogInfo, String source) {
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
            if (blogInfo.getUrl() != null) {
                txtBlogName.setText(UrlUtils.getHost(blogInfo.getUrl()));
            } else {
                txtBlogName.setText(R.string.reader_untitled_post);
            }
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

        final String imageUrl = blogInfo.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            showBlavatarImage(blogInfo, blavatarImg);
        } else {
            blavatarImg.setVisibility(View.GONE);
        }

        loadFollowCount(blogInfo, txtFollowCount);

        mFollowButton.setVisibility(View.VISIBLE);
        mFollowButton.setIsFollowed(blogInfo.isFollowing);
        mFollowButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mAccountStore.hasAccessToken()) {
                    OnFollowListener followListener = mFollowListenerRef != null ? mFollowListenerRef.get() : null;
                    if (followListener != null) {
                        followListener.onFollowTappedWhenLoggedOut();
                    }
                } else {
                    toggleFollowStatus(v, source);
                }
            }
        });

        // Show subscription settings button for followed WordPress.com blogs (not external feeds)
        updateSubscriptionSettingsButtonVisibility(blogInfo);

        if (layoutInfo.getVisibility() != View.VISIBLE) {
            layoutInfo.setVisibility(View.VISIBLE);
        }

        OnBlogInfoLoadedListener blogInfoListener =
                mBlogInfoListenerRef != null ? mBlogInfoListenerRef.get() : null;
        if (blogInfoListener != null) {
            blogInfoListener.onBlogInfoLoaded(blogInfo);
        }
    }

    private void loadFollowCount(ReaderBlog blogInfo, TextView txtFollowCount) {
        final CompactDecimalFormat compactDecimalFormat =
                CompactDecimalFormat.getInstance(
                        Locale.getDefault(),
                        CompactDecimalFormat.CompactStyle.SHORT
                );

        final int followersStringRes;
        if (blogInfo.numSubscribers == 1) {
            followersStringRes = R.string.reader_label_subscribers_count_single;
        } else {
            followersStringRes = R.string.reader_label_subscribers_count;
        }

        final String formattedNumberSubscribers;
        // Reference: pcdRpT-3BI-p2#comment-5978
        if (blogInfo.numSubscribers >= MINIMUM_NUMBER_FOLLOWERS_FORMAT) {
            formattedNumberSubscribers = compactDecimalFormat.format(blogInfo.numSubscribers);
        } else {
            formattedNumberSubscribers = NumberFormat.getInstance().format(blogInfo.numSubscribers);
        }
        txtFollowCount.setText(String.format(
                getContext().getString(followersStringRes), formattedNumberSubscribers)
        );
    }

    private void showBlavatarImage(ReaderBlog blogInfo, ImageView blavatarImg) {
        blavatarImg.setVisibility(View.VISIBLE);
        mImageManager.loadIntoCircle(blavatarImg,
                SiteUtils.getSiteImageType(blogInfo.isP2orA8C(), BlavatarShape.CIRCULAR),
                PhotonUtils.getPhotonImageUrl(blogInfo.getImageUrl(), mBlavatarSz, mBlavatarSz, Quality.HIGH));
    }

    private void setFollowButtonLoading(boolean isLoading) {
        mFollowButton.setIsLoading(isLoading);
        if (mFollowProgress != null) {
            mFollowProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private void toggleFollowStatus(final View followButton, final String source) {
        if (!NetworkUtils.checkConnection(getContext())) {
            return;
        }
        // disable follow button and show loading indicator until API call returns
        setFollowButtonLoading(true);

        final boolean isAskingToFollow;
        if (mIsFeed) {
            isAskingToFollow = !ReaderBlogTable.isFollowedFeed(mFeedId);
        } else {
            isAskingToFollow = !ReaderBlogTable.isFollowedBlog(mBlogId);
        }

        mFollowButton.setIsFollowed(isAskingToFollow);

        OnFollowListener followListener =
                mFollowListenerRef != null ? mFollowListenerRef.get() : null;
        if (followListener != null) {
            if (isAskingToFollow) {
                followListener.onFollowTapped(
                        followButton,
                        mBlogInfo.getName(),
                        mIsFeed ? 0 : mBlogInfo.blogId,
                        mBlogInfo.feedId);
            } else {
                followListener.onFollowingTapped();
            }
        }

        ReaderActions.ActionListener listener = succeeded -> {
            if (getContext() == null) {
                return;
            }
            setFollowButtonLoading(false);
            if (!succeeded) {
                int errResId = isAskingToFollow ? R.string.reader_toast_err_unable_to_follow_blog
                        : R.string.reader_toast_err_unable_to_unfollow_blog;
                ToastUtils.showToast(getContext(), errResId);
                mFollowButton.setIsFollowed(!isAskingToFollow);
            }
        };


        boolean result;
        if (mIsFeed) {
            result = ReaderBlogActions.followFeedById(
                    mBlogId,
                    mFeedId,
                    isAskingToFollow,
                    listener,
                    source,
                    mReaderTracker
            );
        } else {
            result = ReaderBlogActions.followBlogById(
                    mBlogId,
                    mFeedId,
                    isAskingToFollow,
                    listener,
                    source,
                    mReaderTracker
            );
        }

        if (!result) {
            setFollowButtonLoading(false);
            mFollowButton.setIsFollowed(!isAskingToFollow);
        }
    }

    private void updateSubscriptionSettingsButtonVisibility(ReaderBlog blogInfo) {
        if (mSubscriptionSettingsButton == null) {
            return;
        }

        // Only show settings for followed WordPress.com blogs (not external feeds)
        boolean showSettings = blogInfo.isFollowing && !mIsFeed && mAccountStore.hasAccessToken();
        mSubscriptionSettingsButton.setVisibility(showSettings ? View.VISIBLE : View.GONE);

        if (showSettings) {
            mSubscriptionSettingsButton.setOnClickListener(v -> {
                OnSubscriptionSettingsClickListener listener =
                        mSubscriptionSettingsListenerRef != null ? mSubscriptionSettingsListenerRef.get() : null;
                if (listener != null) {
                    String blogName = blogInfo.hasName() ? blogInfo.getName() : "";
                    String blogUrl = blogInfo.hasUrl() ? UrlUtils.getHost(blogInfo.getUrl()) : "";
                    listener.onSubscriptionSettingsClicked(mBlogId, blogName, blogUrl);
                }
            });
        }
    }
}
