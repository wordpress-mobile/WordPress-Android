package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.icu.text.CompactDecimalFormat;
import android.icu.text.NumberFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.PhotonUtils.Quality;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.config.ReaderImprovementsFeatureConfig;
import org.wordpress.android.util.image.BlavatarShape;
import org.wordpress.android.util.image.ImageManager;

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

    private long mBlogId;
    private long mFeedId;
    private boolean mIsFeed;

    private ReaderFollowButton mFollowButton;
    private ReaderBlog mBlogInfo;
    private OnBlogInfoLoadedListener mBlogInfoListener;
    private OnFollowListener mFollowListener;

    private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    
    @Inject AccountStore mAccountStore;
    @Inject ImageManager mImageManager;
    @Inject ReaderTracker mReaderTracker;
    @Inject ReaderImprovementsFeatureConfig mReaderImprovementsFeatureConfig;

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
        final int layoutRes;
        if (mReaderImprovementsFeatureConfig.isEnabled()) {
            layoutRes = R.layout.reader_site_header_view_new;
        } else {
            layoutRes = R.layout.reader_site_header_view;
        }
        final View view = inflate(context, layoutRes, this);
        mFollowButton = view.findViewById(R.id.follow_button);
        view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public void setOnFollowListener(OnFollowListener listener) {
        mFollowListener = listener;
    }

    public void setOnBlogInfoLoadedListener(OnBlogInfoLoadedListener listener) {
        mBlogInfoListener = listener;
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
                            showBlogInfo(serverBlogInfo, source);
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

        if (mReaderImprovementsFeatureConfig.isEnabled()) {
            final String imageUrl = blogInfo.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                showBlavatarImage(blogInfo, blavatarImg);
            } else {
                blavatarImg.setVisibility(View.GONE);
            }
        } else {
            showBlavatarImage(blogInfo, blavatarImg);
        }

        loadFollowCount(blogInfo, txtFollowCount);

        if (!mAccountStore.hasAccessToken()) {
            mFollowButton.setVisibility(View.GONE);
        } else {
            mFollowButton.setVisibility(View.VISIBLE);
            mFollowButton.setIsFollowed(blogInfo.isFollowing);
            mFollowButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollowStatus(v, source);
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

    private void loadFollowCount(ReaderBlog blogInfo, TextView txtFollowCount) {
        if (mReaderImprovementsFeatureConfig.isEnabled()) {
            final CompactDecimalFormat compactDecimalFormat =
                    CompactDecimalFormat.getInstance(LocaleManager.getSafeLocale(getContext()),
                            CompactDecimalFormat.CompactStyle.SHORT);

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
                    LocaleManager.getSafeLocale(getContext()),
                    getContext().getString(followersStringRes), formattedNumberSubscribers)
            );
        } else {
            txtFollowCount.setText(String.format(
                    LocaleManager.getSafeLocale(getContext()),
                    getContext().getString(R.string.reader_label_subscribe_count),
                    blogInfo.numSubscribers));
        }
    }

    private void showBlavatarImage(ReaderBlog blogInfo, ImageView blavatarImg) {
        blavatarImg.setVisibility(View.VISIBLE);
        mImageManager.loadIntoCircle(blavatarImg,
                SiteUtils.getSiteImageType(blogInfo.isP2orA8C(), BlavatarShape.CIRCULAR),
                PhotonUtils.getPhotonImageUrl(blogInfo.getImageUrl(), mBlavatarSz, mBlavatarSz, Quality.HIGH));
    }

    private void toggleFollowStatus(final View followButton, final String source) {
        if (!NetworkUtils.checkConnection(getContext())) {
            return;
        }
        // disable follow button until API call returns
        mFollowButton.setEnabled(false);

        final boolean isAskingToFollow;
        if (mIsFeed) {
            isAskingToFollow = !ReaderBlogTable.isFollowedFeed(mFeedId);
        } else {
            isAskingToFollow = !ReaderBlogTable.isFollowedBlog(mBlogId);
        }

        mFollowButton.setIsFollowed(isAskingToFollow);

        if (mFollowListener != null) {
            if (isAskingToFollow) {
                mFollowListener.onFollowTapped(
                        followButton,
                        mBlogInfo.getName(),
                        mIsFeed ? 0 : mBlogInfo.blogId,
                        mBlogInfo.feedId);
            } else {
                mFollowListener.onFollowingTapped();
            }
        }

        ReaderActions.ActionListener listener = succeeded -> {
            if (getContext() == null) {
                return;
            }
            mFollowButton.setEnabled(true);
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
            mFollowButton.setIsFollowed(!isAskingToFollow);
        }
    }
}
