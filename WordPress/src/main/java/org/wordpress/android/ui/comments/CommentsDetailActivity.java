package org.wordpress.android.ui.comments;

import static org.wordpress.android.ui.comments.CommentsListFragment.COMMENTS_PER_PAGE;

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
import org.wordpress.android.models.CommentList;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPViewPager;
import org.wordpress.android.widgets.WPViewPagerTransformer;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import javax.inject.Inject;

public class CommentsDetailActivity extends AppCompatActivity implements CommentAdapter.OnLoadMoreListener {
    public static final String COMMENT_ID_EXTRA = "commentId";
    public static final String COMMENT_STATUS_FILTER_EXTRA = "commentStatusFilter";

    @Inject CommentStore mCommentStore;
    @Inject Dispatcher mDispatcher;

    private WPViewPager mViewPager;
    private ProgressBar progressBar;

    private long mCommentId;
    private CommentStatus commentStatus;
    private SiteModel mSite;
    private CommentDetailFragmentAdapter adapter;
    private ViewPager.OnPageChangeListener mOnPageChangeListener;

    private boolean isLoadingComments;
    private boolean mIsUpdatingComments;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        mDispatcher.register(this);
        AppLog.i(AppLog.T.COMMENTS, "Creating CommentsDetailActivity");

        setContentView(R.layout.comments_detail_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0);
            actionBar.setTitle(R.string.comments);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            mCommentId = getIntent().getLongExtra(COMMENT_ID_EXTRA, -1);
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            commentStatus = (CommentStatus) getIntent().getSerializableExtra(COMMENT_STATUS_FILTER_EXTRA);

        } else {
            mCommentId = savedInstanceState.getLong(COMMENT_ID_EXTRA);
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            commentStatus = (CommentStatus) savedInstanceState.getSerializable(COMMENT_STATUS_FILTER_EXTRA);
        }

        //set up the viewpager and adapter for lateral navigation
        mViewPager = (WPViewPager) findViewById(R.id.viewpager);
        mViewPager.setPageTransformer(false,
                                      new WPViewPagerTransformer(WPViewPagerTransformer.TransformType.SLIDE_OVER));

        progressBar = (ProgressBar) findViewById(R.id.progress_loading);

        loadDataInViewPager();
    }

    @Override
    public void onDestroy() {
        mDispatcher.unregister(this);
        super.onDestroy();
    }

    private void loadDataInViewPager() {
        if (isLoadingComments) {
            AppLog.w(AppLog.T.COMMENTS, "load comments task already active");
        } else {
            new LoadCommentsTask(mCommentStore, commentStatus, mSite, new LoadCommentsTask.LoadingCallback() {
                @Override
                public void isLoading(boolean loading) {
                    setLoadingState(loading);
                    isLoadingComments = loading;
                }

                @Override
                public void loadingFinished(CommentList commentList) {
                    loadViewPagerAdapter(commentList);
                    setLoadingState(false);
                }
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void loadViewPagerAdapter(CommentList commentList) {
        adapter = new CommentDetailFragmentAdapter(getFragmentManager(), commentList, mSite, CommentsDetailActivity.this);
        mViewPager.setAdapter(adapter);
        final int commentIndex = adapter.commentIndex(mCommentId);
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
                    final CommentModel comment = adapter.getCommentAtPosition(position);
                    if (comment != null) {
                        mCommentId = comment.getRemoteCommentId();
                    }
                }
            };
        }
        mViewPager.setCurrentItem(commentIndex);

        mViewPager.addOnPageChangeListener(mOnPageChangeListener);
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
        if (mIsUpdatingComments) {
            AppLog.w(AppLog.T.COMMENTS, "update comments task already running");
            return;
        } else if (!NetworkUtils.isNetworkAvailable(this)) {
            ToastUtils.showToast(this, getString(R.string.error_refresh_comments_showing_older));
            return;
        }

        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentsAction(new CommentStore.FetchCommentsPayload(mSite, commentStatus, COMMENTS_PER_PAGE, adapter.getCount())));
        mIsUpdatingComments = true;
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCommentChanged(CommentStore.OnCommentChanged event) {
        mIsUpdatingComments = false;
        // Don't refresh the list on push, we already updated comments
        if (event.causeOfChange != CommentAction.PUSH_COMMENT) {
            loadDataInViewPager();
        }
        if (event.isError()) {
            if (!TextUtils.isEmpty(event.error.message)) {
                ToastUtils.showToast(this, event.error.message);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(COMMENT_ID_EXTRA, mCommentId);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putSerializable(COMMENT_STATUS_FILTER_EXTRA, commentStatus);
        super.onSaveInstanceState(outState);
    }

    private void showErrorToastAndFinish() {
        AppLog.e(AppLog.T.COMMENTS, "Comment could not be found.");
        ToastUtils.showToast(this, R.string.error_load_comment);
        finish();
    }

    private void setLoadingState(boolean visible) {
        if (progressBar != null) {
            boolean showProgressBar = visible && (adapter == null || adapter.isEmpty());
            progressBar.setVisibility(showProgressBar ? View.VISIBLE : View.GONE);
        }
    }
}
