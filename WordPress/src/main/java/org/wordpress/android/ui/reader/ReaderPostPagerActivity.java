package org.wordpress.android.ui.reader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.WPLaunchActivity;
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenInReader;
import org.wordpress.android.ui.deeplinks.DeepLinkOpenWebLinksWithJetpackHelper;
import org.wordpress.android.ui.deeplinks.DeepLinkTrackingUtils;
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureFullScreenOverlayFragment;
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureFullScreenOverlayViewModel;
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureOverlayActions.ForwardToJetpack;
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureCollectionOverlaySource;
import org.wordpress.android.ui.mysite.SelectedSiteRepository;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostIdList;
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter;
import org.wordpress.android.ui.reader.tracker.ReaderTracker;
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType;
import org.wordpress.android.ui.reader.utils.ReaderPostSeenStatusWrapper;
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource;
import org.wordpress.android.ui.uploads.UploadActionUseCase;
import org.wordpress.android.ui.uploads.UploadUtils;
import org.wordpress.android.ui.uploads.UploadUtilsWrapper;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UriWrapper;
import org.wordpress.android.util.UrlUtilsWrapper;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper;
import org.wordpress.android.util.config.SeenUnseenWithCounterFeatureConfig;
import org.wordpress.android.widgets.WPSwipeSnackbar;
import org.wordpress.android.widgets.WPViewPager;
import org.wordpress.android.widgets.WPViewPagerTransformer;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/*
 * shows reader post detail fragments in a ViewPager - primarily used for easy swiping between
 * posts with a specific tag or in a specific blog, but can also be used to show a single
 * post detail.
 *
 * It also displays intercepted WordPress.com URls in the following forms
 *
 * http[s]://wordpress.com/read/blogs/{blogId}/posts/{postId}
 * http[s]://wordpress.com/read/feeds/{feedId}/posts/{feedItemId}
 * http[s]://{username}.wordpress.com/{year}/{month}/{day}/{postSlug}
 *
 * Will also handle jumping to the comments section, liking a commend and liking a post directly
 */
@AndroidEntryPoint
public class ReaderPostPagerActivity extends LocaleAwareActivity {
    /**
     * Type of URL intercepted
     */
    private enum InterceptType {
        READER_BLOG,
        READER_FEED,
        WPCOM_POST_SLUG
    }

    /**
     * operation to perform automatically when opened via deeplinking
     */
    public enum DirectOperation {
        COMMENT_JUMP,
        COMMENT_REPLY,
        COMMENT_LIKE,
        POST_LIKE,
    }

    private WPViewPager mViewPager;
    private ProgressBar mProgress;

    private ReaderTag mCurrentTag;
    private boolean mIsFeed;
    private long mBlogId;
    private long mPostId;
    private int mCommentId;
    private DirectOperation mDirectOperation;
    private String mInterceptedUri;
    private int mLastSelectedPosition = -1;
    private ReaderPostListType mPostListType;

    private boolean mPostSlugsResolutionUnderway;
    private boolean mIsRequestingMorePosts;
    private boolean mIsSinglePostView;
    private boolean mIsRelatedPostView;

    private boolean mBackFromLogin;

    private final HashSet<Integer> mTrackedPositions = new HashSet<>();

    @Inject SiteStore mSiteStore;
    @Inject ReaderTracker mReaderTracker;
    @Inject AnalyticsUtilsWrapper mAnalyticsUtilsWrapper;
    @Inject ReaderPostTableWrapper mReaderPostTableWrapper;
    @Inject PostStore mPostStore;
    @Inject Dispatcher mDispatcher;
    @Inject UploadActionUseCase mUploadActionUseCase;
    @Inject UploadUtilsWrapper mUploadUtilsWrapper;
    @Inject ReaderPostSeenStatusWrapper mPostSeenStatusWrapper;
    @Inject SeenUnseenWithCounterFeatureConfig mSeenUnseenWithCounterFeatureConfig;
    @Inject UrlUtilsWrapper mUrlUtilsWrapper;
    @Inject DeepLinkTrackingUtils mDeepLinkTrackingUtils;
    @Inject SelectedSiteRepository mSelectedSiteRepository;
    @Inject DeepLinkOpenWebLinksWithJetpackHelper mDeepLinkOpenWebLinksWithJetpackHelper;
    private JetpackFeatureFullScreenOverlayViewModel mJetpackFullScreenViewModel;
    @Inject AccountStore mAccountStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        mJetpackFullScreenViewModel = new ViewModelProvider(this).get(JetpackFeatureFullScreenOverlayViewModel.class);

        setContentView(R.layout.reader_activity_post_pager);

        mViewPager = findViewById(R.id.viewpager);
        mProgress = findViewById(R.id.progress_loading);

        if (savedInstanceState != null) {
            mIsFeed = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_FEED);
            mBlogId = savedInstanceState.getLong(ReaderConstants.ARG_BLOG_ID);
            mPostId = savedInstanceState.getLong(ReaderConstants.ARG_POST_ID);
            mDirectOperation = (DirectOperation) savedInstanceState
                    .getSerializable(ReaderConstants.ARG_DIRECT_OPERATION);
            mCommentId = savedInstanceState.getInt(ReaderConstants.ARG_COMMENT_ID);
            mIsSinglePostView = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_SINGLE_POST);
            mIsRelatedPostView = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_RELATED_POST);
            mInterceptedUri = savedInstanceState.getString(ReaderConstants.ARG_INTERCEPTED_URI);
            if (savedInstanceState.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType =
                        (ReaderPostListType) savedInstanceState.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_TAG)) {
                mCurrentTag = (ReaderTag) savedInstanceState.getSerializable(ReaderConstants.ARG_TAG);
            }
            mPostSlugsResolutionUnderway =
                    savedInstanceState.getBoolean(ReaderConstants.KEY_POST_SLUGS_RESOLUTION_UNDERWAY);
            if (savedInstanceState.containsKey(ReaderConstants.KEY_TRACKED_POSITIONS)) {
                Serializable positions = savedInstanceState.getSerializable(ReaderConstants.KEY_TRACKED_POSITIONS);
                if (positions instanceof HashSet) {
                    mTrackedPositions.addAll((HashSet<Integer>) positions);
                }
            }
        } else {
            mIsFeed = getIntent().getBooleanExtra(ReaderConstants.ARG_IS_FEED, false);
            mBlogId = getIntent().getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
            mPostId = getIntent().getLongExtra(ReaderConstants.ARG_POST_ID, 0);
            mDirectOperation = (DirectOperation) getIntent()
                    .getSerializableExtra(ReaderConstants.ARG_DIRECT_OPERATION);
            mCommentId = getIntent().getIntExtra(ReaderConstants.ARG_COMMENT_ID, 0);
            mIsSinglePostView = getIntent().getBooleanExtra(ReaderConstants.ARG_IS_SINGLE_POST, false);
            mIsRelatedPostView = getIntent().getBooleanExtra(ReaderConstants.ARG_IS_RELATED_POST, false);
            mInterceptedUri = getIntent().getStringExtra(ReaderConstants.ARG_INTERCEPTED_URI);
            if (getIntent().hasExtra(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType =
                        (ReaderPostListType) getIntent().getSerializableExtra(ReaderConstants.ARG_POST_LIST_TYPE);
            }
            if (getIntent().hasExtra(ReaderConstants.ARG_TAG)) {
                mCurrentTag = (ReaderTag) getIntent().getSerializableExtra(ReaderConstants.ARG_TAG);
            }
        }

        if (mPostListType == null) {
            mPostListType = ReaderPostListType.TAG_FOLLOWED;
        }

        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                trackPostAtPositionIfNeeded(position);

                if (mLastSelectedPosition > -1 && mLastSelectedPosition != position) {
                    // pause the previous web view - important because otherwise embedded content
                    // will continue to play
                    ReaderPostDetailFragment lastFragment = getDetailFragmentAtPosition(mLastSelectedPosition);
                    if (lastFragment != null) {
                        lastFragment.pauseWebView();
                    }
                }

                // resume the newly active webView if it was previously paused
                ReaderPostDetailFragment thisFragment = getDetailFragmentAtPosition(position);
                if (thisFragment != null) {
                    thisFragment.resumeWebViewIfPaused();
                }

                mLastSelectedPosition = position;
            }
        });

        mViewPager.setPageTransformer(
                false,
                new WPViewPagerTransformer(WPViewPagerTransformer.TransformType.SLIDE_OVER)
        );

        observeOverlayEvents();
    }

    private void observeOverlayEvents() {
        mJetpackFullScreenViewModel.getAction().observe(this,
                action -> {
                    if (action instanceof ForwardToJetpack) {
                        if (!mDeepLinkOpenWebLinksWithJetpackHelper.handleOpenLinksInJetpackIfPossible()) {
                            finishDeepLinkRequestFromOverlay(getIntent().getAction(), getIntent().getData());
                        } else {
                            WPActivityUtils.disableReaderDeeplinks(this);
                            ActivityLauncher.openJetpackForDeeplink(this, getIntent().getAction(),
                                    new UriWrapper(getIntent().getData()));
                            finish();
                        }
                    } else {
                        finishDeepLinkRequestFromOverlay(getIntent().getAction(), getIntent().getData());
                    }
                });
    }

    private void handleDeepLinking() {
        String action = getIntent().getAction();
        Uri uri = getIntent().getData();

        String host = "";
        if (uri != null) {
            host = uri.getHost();
        }


        if (uri == null) {
            mReaderTracker.trackDeepLink(AnalyticsTracker.Stat.DEEP_LINKED, action, host, uri);
            // invalid uri so, just show the entry screen
            Intent intent = new Intent(this, WPLaunchActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        if (!checkAndShowOpenWebLinksWithJetpackOverlayIfNeeded()) {
            finishDeepLinkRequest(action, uri);
        }
    }

    private void finishDeepLinkRequestFromOverlay(String action, Uri uri) {
        finishDeepLinkRequest(action, uri);
        // We interrupted the normal flow to show the overly, we now need to rerun these methods on a dismiss action
        loadPosts(mBlogId, mPostId);
        mBackFromLogin = false;
    }

    private void finishDeepLinkRequest(String action, Uri uri) {
        InterceptType interceptType = InterceptType.READER_BLOG;
        String blogIdentifier = null; // can be an id or a slug
        String postIdentifier = null; // can be an id or a slug

        mInterceptedUri = uri.toString();

        List<String> segments = uri.getPathSegments();

        // Handled URLs look like this: http[s]://wordpress.com/read/feeds/{feedId}/posts/{feedItemId}
        // with the first segment being 'read'.
        if (segments != null) {
            // Builds stripped URI for tracking purposes
            UriWrapper wrappedUri = new UriWrapper(uri);
            if (segments.get(0).equals("read")) {
                if (segments.size() > 2) {
                    blogIdentifier = segments.get(2);

                    if (segments.get(1).equals("blogs")) {
                        interceptType = InterceptType.READER_BLOG;
                    } else if (segments.get(1).equals("feeds")) {
                        interceptType = InterceptType.READER_FEED;
                        mIsFeed = true;
                    }
                }

                if (segments.size() > 4 && segments.get(3).equals("posts")) {
                    postIdentifier = segments.get(4);
                }

                parseFragment(uri);
                mDeepLinkTrackingUtils.track(action, new OpenInReader(wrappedUri), wrappedUri);
                showPost(interceptType, blogIdentifier, postIdentifier);
                return;
            } else if (segments.size() >= 4) {
                blogIdentifier = uri.getHost();
                try {
                    postIdentifier = URLEncoder.encode(segments.get(3), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    AppLog.e(AppLog.T.READER, e);
                    ToastUtils.showToast(this, R.string.error_generic);
                }

                parseFragment(uri);
                detectLike(uri);

                interceptType = InterceptType.WPCOM_POST_SLUG;
                mDeepLinkTrackingUtils.track(action, new OpenInReader(wrappedUri), wrappedUri);
                showPost(interceptType, blogIdentifier, postIdentifier);
                return;
            }
        }

        // at this point, just show the entry screen
        Intent intent = new Intent(this, WPLaunchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void showPost(@NonNull InterceptType interceptType, final String blogIdentifier, final String
            postIdentifier) {
        if (!TextUtils.isEmpty(blogIdentifier) && !TextUtils.isEmpty(postIdentifier)) {
            mIsSinglePostView = true;
            mIsRelatedPostView = false;

            switch (interceptType) {
                case READER_BLOG:
                    if (parseIds(blogIdentifier, postIdentifier)) {
                        mReaderTracker.trackBlogPost(
                                AnalyticsTracker.Stat.READER_BLOG_POST_INTERCEPTED,
                                mBlogId,
                                mPostId
                        );
                        // IDs have now been set so, let ReaderPostPagerActivity normally display the post
                    } else {
                        ToastUtils.showToast(this, R.string.error_generic);
                    }
                    break;
                case READER_FEED:
                    if (parseIds(blogIdentifier, postIdentifier)) {
                        mReaderTracker.trackFeedPost(
                                AnalyticsTracker.Stat.READER_FEED_POST_INTERCEPTED,
                                mBlogId,
                                mPostId
                        );
                        // IDs have now been set so, let ReaderPostPagerActivity normally display the post
                    } else {
                        ToastUtils.showToast(this, R.string.error_generic);
                    }
                    break;
                case WPCOM_POST_SLUG:
                    mReaderTracker.trackBlogPost(
                            AnalyticsTracker.Stat.READER_WPCOM_BLOG_POST_INTERCEPTED,
                            blogIdentifier,
                            postIdentifier,
                            mCommentId
                    );

                    // try to get the post from the local db
                    ReaderPost post = ReaderPostTable.getBlogPost(blogIdentifier, postIdentifier, true);
                    if (post != null) {
                        // set the IDs and let ReaderPostPagerActivity normally display the post
                        mBlogId = post.blogId;
                        mPostId = post.postId;
                    } else {
                        // not stored locally, so request it
                        ReaderPostActions.requestBlogPost(
                                blogIdentifier, postIdentifier,
                                new ReaderActions.OnRequestListener<String>() {
                                    @Override
                                    public void onSuccess(String blogUrl) {
                                        mPostSlugsResolutionUnderway = false;

                                        // the scheme is removed to match the query pattern in ReaderPostTable
                                        // .getBlogPost
                                        String primaryBlogIdentifier = mUrlUtilsWrapper.removeScheme(blogUrl);

                                        // getBlogPost utilizes the primaryBlogIdentifier instead of blogIdentifier
                                        // since
                                        // the custom and *.wordpress.com domains need to be used interchangeably since
                                        // they can both be used as the primary domain when identifying the blog_url
                                        // in the ReaderPostTable query.
                                        ReaderPost post =
                                                ReaderPostTable.getBlogPost(primaryBlogIdentifier, postIdentifier,
                                                        true);
                                        ReaderEvents.PostSlugsRequestCompleted slugsResolved = (post != null)
                                                ? new ReaderEvents.PostSlugsRequestCompleted(200, post.blogId,
                                                post.postId)
                                                : new ReaderEvents.PostSlugsRequestCompleted(200, 0, 0);
                                        // notify that the slug resolution request has completed
                                        EventBus.getDefault().post(slugsResolved);

                                        // post wasn't available locally earlier so, track it now
                                        if (post != null) {
                                            trackPost(post.blogId, post.postId);
                                        }
                                    }

                                    @Override
                                    public void onFailure(int statusCode) {
                                        mPostSlugsResolutionUnderway = false;
                                        // notify that the slug resolution request has completed
                                        EventBus.getDefault()
                                                .post(new ReaderEvents.PostSlugsRequestCompleted(statusCode, 0, 0));
                                    }
                                });
                        mPostSlugsResolutionUnderway = true;
                    }

                    break;
            }
        } else {
            ToastUtils.showToast(this, R.string.error_generic);
        }
    }

    private boolean parseIds(String blogIdentifier, String postIdentifier) {
        try {
            mBlogId = Long.parseLong(blogIdentifier);
            mPostId = Long.parseLong(postIdentifier);
        } catch (NumberFormatException e) {
            AppLog.e(AppLog.T.READER, e);
            return false;
        }

        return true;
    }

    private Boolean checkAndShowOpenWebLinksWithJetpackOverlayIfNeeded() {
        if (!isSignedInWPComOrHasWPOrgSite()) return false;

        if (!mDeepLinkOpenWebLinksWithJetpackHelper.shouldShowOpenLinksInJetpackOverlay()) return false;

        mDeepLinkOpenWebLinksWithJetpackHelper.onOverlayShown();
        JetpackFeatureFullScreenOverlayFragment
                .newInstance(
                        null,
                        false,
                        true,
                        SiteCreationSource.UNSPECIFIED,
                        false,
                        JetpackFeatureCollectionOverlaySource.UNSPECIFIED)
                .show(getSupportFragmentManager(), JetpackFeatureFullScreenOverlayFragment.TAG);
        return true;
    }

    private Boolean isSignedInWPComOrHasWPOrgSite() {
        if (mAccountStore == null || mSiteStore == null) return false;
        return FluxCUtils.isSignedInWPComOrHasWPOrgSite(mAccountStore, mSiteStore);
    }

    /**
     * Parse the URL fragment and interpret it as an operation to perform. For example, a "#comments" fragment is
     * interpreted as a direct jump into the comments section of the post.
     *
     * @param uri the full URI input, including the fragment
     */
    private void parseFragment(Uri uri) {
        // default to do-nothing w.r.t. comments
        mDirectOperation = null;

        if (uri == null || uri.getFragment() == null) {
            return;
        }

        final String fragment = uri.getFragment();

        final Pattern fragmentCommentsPattern = Pattern.compile("comments", Pattern.CASE_INSENSITIVE);
        final Pattern fragmentCommentIdPattern = Pattern.compile("comment-(\\d+)", Pattern.CASE_INSENSITIVE);
        final Pattern fragmentRespondPattern = Pattern.compile("respond", Pattern.CASE_INSENSITIVE);

        // check for the general "#comments" fragment to jump to the comments section
        Matcher commentsMatcher = fragmentCommentsPattern.matcher(fragment);
        if (commentsMatcher.matches()) {
            mDirectOperation = DirectOperation.COMMENT_JUMP;
            mCommentId = 0;
            return;
        }

        // check for the "#respond" fragment to jump to the reply box
        Matcher respondMatcher = fragmentRespondPattern.matcher(fragment);
        if (respondMatcher.matches()) {
            mDirectOperation = DirectOperation.COMMENT_REPLY;

            // check whether we are to reply to a specific comment
            final String replyToCommentId = uri.getQueryParameter("replytocom");
            if (replyToCommentId != null) {
                try {
                    mCommentId = Integer.parseInt(replyToCommentId);
                } catch (NumberFormatException e) {
                    AppLog.e(AppLog.T.UTILS, "replytocom cannot be converted to int" + replyToCommentId, e);
                }
            }

            return;
        }

        // check for the "#comment-xyz" fragment to jump to a specific comment
        Matcher commentIdMatcher = fragmentCommentIdPattern.matcher(fragment);
        if (commentIdMatcher.find() && commentIdMatcher.groupCount() > 0) {
            mCommentId = Integer.valueOf(commentIdMatcher.group(1));
            mDirectOperation = DirectOperation.COMMENT_JUMP;
        }
    }

    /**
     * Parse the URL query parameters and detect attempt to like a post or a comment
     *
     * @param uri the full URI input, including the query parameters
     */
    private void detectLike(Uri uri) {
        // check whether we are to like something
        final boolean doLike = "1".equals(uri.getQueryParameter("like"));
        final String likeActor = uri.getQueryParameter("like_actor");

        if (doLike && likeActor != null && likeActor.trim().length() > 0) {
            mDirectOperation = DirectOperation.POST_LIKE;

            // check whether we are to like a specific comment
            final String likeCommentId = uri.getQueryParameter("commentid");
            if (likeCommentId != null) {
                try {
                    mCommentId = Integer.parseInt(likeCommentId);
                    mDirectOperation = DirectOperation.COMMENT_LIKE;
                } catch (NumberFormatException e) {
                    AppLog.e(AppLog.T.UTILS, "commentid cannot be converted to int" + likeCommentId, e);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppLog.d(T.READER, "TRACK READER ReaderPostPagerActivity > START Count");
        mReaderTracker.start(ReaderTrackerType.PAGED_POST);
        EventBus.getDefault().register(this);

        // We register the dispatcher in order to receive the OnPostUploaded event and show the snackbar
        mDispatcher.register(this);

        if (!hasPagerAdapter() || mBackFromLogin) {
            if (ActivityUtils.isDeepLinking(getIntent()) || ReaderConstants.ACTION_VIEW_POST
                    .equals(getIntent().getAction())) {
                handleDeepLinking();
            }

            loadPosts(mBlogId, mPostId);

            // clear up the back-from-login flag anyway
            mBackFromLogin = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppLog.d(T.READER, "TRACK READER ReaderPostPagerActivity > STOP Count");
        mReaderTracker.stop(ReaderTrackerType.PAGED_POST);
        EventBus.getDefault().unregister(this);
        mDispatcher.unregister(this);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean hasPagerAdapter() {
        return (mViewPager != null && mViewPager.getAdapter() != null);
    }

    private PostPagerAdapter getPagerAdapter() {
        if (mViewPager != null && mViewPager.getAdapter() != null) {
            return (PostPagerAdapter) mViewPager.getAdapter();
        } else {
            return null;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(ReaderConstants.ARG_IS_SINGLE_POST, mIsSinglePostView);
        outState.putBoolean(ReaderConstants.ARG_IS_RELATED_POST, mIsRelatedPostView);
        outState.putString(ReaderConstants.ARG_INTERCEPTED_URI, mInterceptedUri);

        outState.putSerializable(ReaderConstants.ARG_DIRECT_OPERATION, mDirectOperation);
        outState.putInt(ReaderConstants.ARG_COMMENT_ID, mCommentId);

        if (hasCurrentTag()) {
            outState.putSerializable(ReaderConstants.ARG_TAG, getCurrentTag());
        }
        if (getPostListType() != null) {
            outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, getPostListType());
        }

        ReaderBlogIdPostId id = getAdapterCurrentBlogIdPostId();
        if (id != null) {
            outState.putLong(ReaderConstants.ARG_BLOG_ID, id.getBlogId());
            outState.putLong(ReaderConstants.ARG_POST_ID, id.getPostId());
        }

        outState.putBoolean(ReaderConstants.KEY_POST_SLUGS_RESOLUTION_UNDERWAY, mPostSlugsResolutionUnderway);

        if (mTrackedPositions.size() > 0) {
            outState.putSerializable(ReaderConstants.KEY_TRACKED_POSITIONS, mTrackedPositions);
        }

        super.onSaveInstanceState(outState);
    }

    private ReaderBlogIdPostId getAdapterCurrentBlogIdPostId() {
        PostPagerAdapter adapter = getPagerAdapter();
        if (adapter == null) {
            return null;
        }
        return adapter.getCurrentBlogIdPostId();
    }

    private ReaderBlogIdPostId getAdapterBlogIdPostIdAtPosition(int position) {
        PostPagerAdapter adapter = getPagerAdapter();
        if (adapter == null) {
            return null;
        }
        return adapter.getBlogIdPostIdAtPosition(position);
    }

    @Override
    public void onBackPressed() {
        ReaderPostDetailFragment fragment = getActiveDetailFragment();
        if (fragment != null && fragment.isCustomViewShowing()) {
            // if full screen video is showing, hide the custom view rather than navigate back
            fragment.hideCustomView();
        } else {
            if (fragment != null && fragment.goBackInPostHistory()) {
                // noop - fragment moved back to a previous post
            } else {
                super.onBackPressed();
            }
        }
    }

    /*
     * perform analytics tracking and bump the page view for the post at the passed position
     * if it hasn't already been done
     */
    private void trackPostAtPositionIfNeeded(int position) {
        if (!hasPagerAdapter() || mTrackedPositions.contains(position)) {
            return;
        }

        ReaderBlogIdPostId idPair = getAdapterBlogIdPostIdAtPosition(position);
        if (idPair == null) {
            return;
        }

        AppLog.d(AppLog.T.READER, "reader pager > tracking post at position " + position);
        mTrackedPositions.add(position);

        trackPost(idPair.getBlogId(), idPair.getPostId());
    }

    /*
     * perform analytics tracking and bump the page view for the post
     */
    private void trackPost(long blogId, long postId) {
        // bump the page view
        ReaderPostActions.bumpPageViewForPost(mSiteStore, blogId, postId);

        if (mSeenUnseenWithCounterFeatureConfig.isEnabled()) {
            ReaderPost currentPost = ReaderPostTable.getBlogPost(blogId, postId, true);
            if (currentPost != null) {
                mPostSeenStatusWrapper.markPostAsSeenSilently(currentPost);
            }
        }

        // analytics tracking
        mReaderTracker.trackPost(
                AnalyticsTracker.Stat.READER_ARTICLE_OPENED,
                mReaderPostTableWrapper.getBlogPost(blogId, postId, true)
        );
    }

    /*
     * loads the blogId/postId pairs used to populate the pager adapter - passed blogId/postId will
     * be made active after loading unless gotoNext=true, in which case the post after the passed
     * one will be made active
     */
    private void loadPosts(final long blogId, final long postId) {
        new Thread() {
            @Override
            public void run() {
                final ReaderBlogIdPostIdList idList;
                if (mIsSinglePostView) {
                    idList = new ReaderBlogIdPostIdList();
                    idList.add(new ReaderBlogIdPostId(blogId, postId));
                } else {
                    int maxPosts = ReaderConstants.READER_MAX_POSTS_TO_DISPLAY;
                    switch (getPostListType()) {
                        case TAG_FOLLOWED:
                        case TAG_PREVIEW:
                            idList = ReaderPostTable.getBlogIdPostIdsWithTag(getCurrentTag(), maxPosts);
                            break;
                        case BLOG_PREVIEW:
                            idList = ReaderPostTable.getBlogIdPostIdsInBlog(blogId, maxPosts);
                            break;
                        case SEARCH_RESULTS:
                        default:
                            return;
                    }
                }

                final int currentPosition = mViewPager.getCurrentItem();
                final int newPosition = idList.indexOf(blogId, postId);

                runOnUiThread(() -> {
                    if (isFinishing()) {
                        return;
                    }

                    AppLog.d(T.READER, "reader pager > creating adapter");
                    PostPagerAdapter adapter =
                            new PostPagerAdapter(getSupportFragmentManager(), idList);
                    mViewPager.setAdapter(adapter);
                    if (adapter.isValidPosition(newPosition)) {
                        mViewPager.setCurrentItem(newPosition);
                        trackPostAtPositionIfNeeded(newPosition);
                    } else if (adapter.isValidPosition(currentPosition)) {
                        mViewPager.setCurrentItem(currentPosition);
                        trackPostAtPositionIfNeeded(currentPosition);
                    }

                    // let the user know they can swipe between posts
                    if (adapter.getCount() > 1 && !AppPrefs.isReaderSwipeToNavigateShown()) {
                        WPSwipeSnackbar.show(mViewPager);
                        AppPrefs.setReaderSwipeToNavigateShown(true);
                    }
                });
            }
        }.start();
    }

    private ReaderTag getCurrentTag() {
        return mCurrentTag;
    }

    private boolean hasCurrentTag() {
        return mCurrentTag != null;
    }

    private ReaderPostListType getPostListType() {
        return mPostListType;
    }

    private Fragment getActivePagerFragment() {
        PostPagerAdapter adapter = getPagerAdapter();
        if (adapter == null) {
            return null;
        }
        return adapter.getActiveFragment();
    }

    private ReaderPostDetailFragment getActiveDetailFragment() {
        Fragment fragment = getActivePagerFragment();
        if (fragment instanceof ReaderPostDetailFragment) {
            return (ReaderPostDetailFragment) fragment;
        } else {
            return null;
        }
    }

    private Fragment getPagerFragmentAtPosition(int position) {
        PostPagerAdapter adapter = getPagerAdapter();
        if (adapter == null) {
            return null;
        }
        return adapter.getFragmentAtPosition(position);
    }

    private ReaderPostDetailFragment getDetailFragmentAtPosition(int position) {
        Fragment fragment = getPagerFragmentAtPosition(position);
        if (fragment instanceof ReaderPostDetailFragment) {
            return (ReaderPostDetailFragment) fragment;
        } else {
            return null;
        }
    }

    /*
     * called when user scrolls towards the last posts - requests older posts with the
     * current tag or in the current blog
     */
    private void requestMorePosts() {
        if (mIsRequestingMorePosts) {
            return;
        }

        AppLog.d(AppLog.T.READER, "reader pager > requesting older posts");
        switch (getPostListType()) {
            case TAG_PREVIEW:
            case TAG_FOLLOWED:
                ReaderPostServiceStarter.startServiceForTag(
                        this,
                        getCurrentTag(),
                        ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER);
                break;

            case BLOG_PREVIEW:
                ReaderPostServiceStarter.startServiceForBlog(
                        this,
                        mBlogId,
                        ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER);
                break;
            case SEARCH_RESULTS:
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ReaderEvents.UpdatePostsStarted event) {
        if (isFinishing()) {
            return;
        }

        mIsRequestingMorePosts = true;
        mProgress.setVisibility(View.VISIBLE);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ReaderEvents.UpdatePostsEnded event) {
        if (isFinishing()) {
            return;
        }

        PostPagerAdapter adapter = getPagerAdapter();
        if (adapter == null) {
            return;
        }

        mIsRequestingMorePosts = false;
        mProgress.setVisibility(View.GONE);

        if (event.getResult() == ReaderActions.UpdateResult.HAS_NEW) {
            AppLog.d(AppLog.T.READER, "reader pager > older posts received");
            // remember which post to keep active
            ReaderBlogIdPostId id = adapter.getCurrentBlogIdPostId();
            long blogId = (id != null ? id.getBlogId() : 0);
            long postId = (id != null ? id.getPostId() : 0);
            loadPosts(blogId, postId);
        } else {
            AppLog.d(AppLog.T.READER, "reader pager > all posts loaded");
            adapter.mAllPostsLoaded = true;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ReaderEvents.DoSignIn event) {
        if (isFinishing()) {
            return;
        }

        mReaderTracker.trackUri(AnalyticsTracker.Stat.READER_SIGN_IN_INITIATED, mInterceptedUri);
        ActivityLauncher.loginWithoutMagicLink(this);
    }

    /**
     * pager adapter containing post detail fragments
     **/
    private class PostPagerAdapter extends FragmentStatePagerAdapter {
        private final ReaderBlogIdPostIdList mIdList;
        private boolean mAllPostsLoaded;

        // this is used to retain created fragments so we can access them in
        // getFragmentAtPosition() - necessary because the pager provides no
        // built-in way to do this - note that destroyItem() removes fragments
        // from this map when they're removed from the adapter, so this doesn't
        // retain *every* fragment
        private final SparseArray<Fragment> mFragmentMap = new SparseArray<>();

        @SuppressLint("WrongConstant") PostPagerAdapter(FragmentManager fm, ReaderBlogIdPostIdList ids) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            mIdList = (ReaderBlogIdPostIdList) ids.clone();
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            // work around "Fragement no longer exists for key" Android bug
            // by catching the IllegalStateException
            // https://code.google.com/p/android/issues/detail?id=42601
            try {
                AppLog.d(AppLog.T.READER, "reader pager > adapter restoreState");
                super.restoreState(state, loader);
            } catch (IllegalStateException e) {
                AppLog.e(AppLog.T.READER, e);
            }
        }

        @Override
        public Parcelable saveState() {
            AppLog.d(AppLog.T.READER, "reader pager > adapter saveState");
            return super.saveState();
        }

        private boolean canRequestMostPosts() {
            return !mAllPostsLoaded
                   && !mIsSinglePostView
                   && (mIdList != null && mIdList.size() < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY)
                   && NetworkUtils.isNetworkAvailable(ReaderPostPagerActivity.this);
        }

        boolean isValidPosition(int position) {
            return (position >= 0 && position < getCount());
        }

        @Override
        public int getCount() {
            return mIdList.size();
        }

        @Override
        public Fragment getItem(int position) {
            if ((position == getCount() - 1) && canRequestMostPosts()) {
                requestMorePosts();
            }

            return ReaderPostDetailFragment.Companion.newInstance(
                    mIsFeed,
                    mIdList.get(position).getBlogId(),
                    mIdList.get(position).getPostId(),
                    mDirectOperation,
                    mCommentId,
                    mIsRelatedPostView,
                    mInterceptedUri,
                    getPostListType(),
                    mPostSlugsResolutionUnderway);
        }

        @Override
        public @NonNull Object instantiateItem(ViewGroup container, int position) {
            Object item = super.instantiateItem(container, position);
            if (item instanceof Fragment) {
                mFragmentMap.put(position, (Fragment) item);
            }
            return item;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            mFragmentMap.remove(position);
            super.destroyItem(container, position, object);
        }

        private Fragment getActiveFragment() {
            return getFragmentAtPosition(mViewPager.getCurrentItem());
        }

        private Fragment getFragmentAtPosition(int position) {
            if (isValidPosition(position)) {
                return mFragmentMap.get(position);
            } else {
                return null;
            }
        }

        private ReaderBlogIdPostId getCurrentBlogIdPostId() {
            return getBlogIdPostIdAtPosition(mViewPager.getCurrentItem());
        }

        ReaderBlogIdPostId getBlogIdPostIdAtPosition(int position) {
            if (isValidPosition(position)) {
                return mIdList.get(position);
            } else {
                return null;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RequestCodes.EDIT_POST:
                if (resultCode != Activity.RESULT_OK || data == null || isFinishing()) {
                    return;
                }
                int localId = data.getIntExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, 0);
                final SiteModel site = (SiteModel) data.getSerializableExtra(WordPress.SITE);
                final PostModel post = mPostStore.getPostByLocalPostId(localId);

                if (EditPostActivity.checkToRestart(data)) {
                    ActivityLauncher.editPostOrPageForResult(data, ReaderPostPagerActivity.this, site,
                            data.getIntExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, 0));

                    // a restart will happen so, no need to continue here
                    break;
                }

                if (site != null && post != null) {
                    mUploadUtilsWrapper.handleEditPostResultSnackbars(
                            this,
                            findViewById(R.id.coordinator),
                            data,
                            post,
                            site,
                            mUploadActionUseCase.getUploadAction(post),
                            v -> UploadUtils.publishPost(ReaderPostPagerActivity.this, post, site, mDispatcher));
                }
                break;
            case RequestCodes.DO_LOGIN:
                if (resultCode == Activity.RESULT_OK) {
                    mBackFromLogin = true;
                }
                break;
            case RequestCodes.NO_REBLOG_SITE:
                if (resultCode == Activity.RESULT_OK) {
                    finish(); // Finish activity to make My Site page visible
                }
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostUploaded(OnPostUploaded event) {
        SiteModel site = mSiteStore.getSiteByLocalId(mSelectedSiteRepository.getSelectedSiteLocalId());
        if (site != null && event.post != null) {
            mUploadUtilsWrapper.onPostUploadedSnackbarHandler(
                    this,
                    findViewById(R.id.coordinator),
                    event.isError(),
                    event.isFirstTimePublish,
                    event.post,
                    null,
                    site);
        }
    }
}
