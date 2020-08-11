package org.wordpress.android.ui.comments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.CommentAction;
import org.wordpress.android.fluxc.generated.CommentActionBuilder;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.CommentStore;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsPayload;
import org.wordpress.android.fluxc.store.CommentStore.OnCommentChanged;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.ScrollableViewInitializedListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPViewPager;
import org.wordpress.android.widgets.WPViewPagerTransformer;

import javax.inject.Inject;

import static org.wordpress.android.ui.comments.CommentsListFragment.COMMENTS_PER_PAGE;

public class CommentsDetailActivity extends LocaleAwareActivity
        implements CommentAdapter.OnLoadMoreListener,
        CommentActions.OnCommentActionListener, ScrollableViewInitializedListener {
    public static final String COMMENT_ID_EXTRA = "commentId";
    public static final String COMMENT_STATUS_FILTER_EXTRA = "commentStatusFilter";

    @Inject CommentStore mCommentStore;
    @Inject Dispatcher mDispatcher;

    private WPViewPager mViewPager;
    private AppBarLayout mAppBarLayout;
    private ProgressBar mProgressBar;

    private long mCommentId;
    private CommentStatus mStatusFilter;
    private SiteModel mSite;
    private CommentDetailFragmentAdapter mAdapter;
    private ViewPager.OnPageChangeListener mOnPageChangeListener;

    private boolean mIsLoadingComments;
    private boolean mIsUpdatingComments;
    private boolean mCanLoadMoreComments = true;

    @Override
    public void onBackPressed() {
        CollapseFullScreenDialogFragment fragment = (CollapseFullScreenDialogFragment)
                getSupportFragmentManager().findFragmentByTag(CollapseFullScreenDialogFragment.TAG);

        if (fragment != null) {
            fragment.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        mDispatcher.register(this);
        AppLog.i(AppLog.T.COMMENTS, "Creating CommentsDetailActivity");

        setContentView(R.layout.comments_detail_activity);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            mCommentId = getIntent().getLongExtra(COMMENT_ID_EXTRA, -1);
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            mStatusFilter = (CommentStatus) getIntent().getSerializableExtra(COMMENT_STATUS_FILTER_EXTRA);
        } else {
            mCommentId = savedInstanceState.getLong(COMMENT_ID_EXTRA);
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mStatusFilter = (CommentStatus) savedInstanceState.getSerializable(COMMENT_STATUS_FILTER_EXTRA);
        }

        // set up the viewpager and adapter for lateral navigation
        mViewPager = findViewById(R.id.viewpager);
        mViewPager.setPageTransformer(false,
                new WPViewPagerTransformer(WPViewPagerTransformer.TransformType.SLIDE_OVER));

        mProgressBar = findViewById(R.id.progress_loading);
        mAppBarLayout = findViewById(R.id.appbar_main);

        // Asynchronously loads comments and build the adapter
        loadDataInViewPager();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(COMMENT_ID_EXTRA, mCommentId);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putSerializable(COMMENT_STATUS_FILTER_EXTRA, mStatusFilter);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        mDispatcher.unregister(this);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLoadMore() {
        updateComments();
    }

    private void updateComments() {
        if (mIsUpdatingComments) {
            AppLog.w(AppLog.T.COMMENTS, "update comments task already running");
            return;
        } else if (!NetworkUtils.isNetworkAvailable(this)) {
            ToastUtils.showToast(this, getString(R.string.error_refresh_comments_showing_older));
            return;
        } else if (!mCanLoadMoreComments) {
            AppLog.w(AppLog.T.COMMENTS, "no more comments to be loaded");
            return;
        }

        final int offset = mAdapter.getCount();
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentsAction(
                new FetchCommentsPayload(mSite, mStatusFilter, COMMENTS_PER_PAGE, offset)));
        mIsUpdatingComments = true;
        setLoadingState(true);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCommentChanged(OnCommentChanged event) {
        mIsUpdatingComments = false;
        setLoadingState(false);
        // Don't refresh the list on push, we already updated comments
        if (event.causeOfChange != CommentAction.PUSH_COMMENT) {
            if (event.changedCommentsLocalIds.size() > 0) {
                loadDataInViewPager();
            } else if (!event.isError()) {
                // There are no more comments to load
                mCanLoadMoreComments = false;
            }
        }
        if (event.isError()) {
            if (!TextUtils.isEmpty(event.error.message)) {
                ToastUtils.showToast(this, event.error.message);
            }
        }
    }

    private void loadDataInViewPager() {
        if (mIsLoadingComments) {
            AppLog.w(AppLog.T.COMMENTS, "load comments task already active");
        } else {
            new LoadCommentsTask(mCommentStore, mStatusFilter, mSite, new LoadCommentsTask.LoadingCallback() {
                @Override
                public void isLoading(boolean loading) {
                    setLoadingState(loading);
                    mIsLoadingComments = loading;
                }

                @Override
                public void loadingFinished(CommentList commentList) {
                    if (!commentList.isEmpty()) {
                        showCommentList(commentList);
                    }
                }
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void showCommentList(CommentList commentList) {
        final int previousItem = mViewPager.getCurrentItem();

        // Only notify adapter when loading new page
        if (mAdapter != null && mAdapter.isAddingNewComments(commentList)) {
            mAdapter.onNewItems(commentList);
        } else {
            // If current items change, rebuild the adapter
            mAdapter = new CommentDetailFragmentAdapter(getSupportFragmentManager(), commentList, mSite,
                    CommentsDetailActivity.this);
            mViewPager.setAdapter(mAdapter);
        }

        final int commentIndex = mAdapter.commentIndex(mCommentId);
        if (commentIndex < 0) {
            showErrorToastAndFinish();
        }
        if (mOnPageChangeListener != null) {
            mViewPager.removeOnPageChangeListener(mOnPageChangeListener);
        } else {
            mOnPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    final CommentModel comment = mAdapter.getCommentAtPosition(position);
                    if (comment != null) {
                        mCommentId = comment.getRemoteCommentId();
                    }
                }
            };
        }
        if (commentIndex != previousItem) {
            mViewPager.setCurrentItem(commentIndex);
        }

        mViewPager.addOnPageChangeListener(mOnPageChangeListener);
    }

    private void showErrorToastAndFinish() {
        AppLog.e(AppLog.T.COMMENTS, "Comment could not be found.");
        ToastUtils.showToast(this, R.string.error_load_comment);
        finish();
    }

    private void setLoadingState(boolean visible) {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }


    @Override
    public void onModerateComment(final SiteModel site,
                                  final CommentModel comment,
                                  final CommentStatus newStatus) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(CommentsActivity.COMMENT_MODERATE_ID_EXTRA, comment.getRemoteCommentId());
        resultIntent.putExtra(CommentsActivity.COMMENT_MODERATE_STATUS_EXTRA, newStatus.toString());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onScrollableViewInitialized(int containerId) {
        mAppBarLayout.setLiftOnScrollTargetViewId(containerId);
    }
}
