package org.wordpress.android.ui.comments;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.CommentStore;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPViewPager;
import org.wordpress.android.widgets.WPViewPagerTransformer;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import javax.inject.Inject;

public class CommentsDetailActivity extends AppCompatActivity {
    public static final String COMMENT_ID_EXTRA = "commentId";
    public static final String COMMENT_STATUS_FILTER_EXTRA = "commentStatusFilter";

    @Inject CommentStore mCommentStore;

    private WPViewPager mViewPager;

    private long mCommentId;
    private CommentStatus commentStatus;
    private SiteModel mSite;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
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
        //TODO mCommentStore.getCommentBySiteAndRemoteId(mSite, mCommentId);
        //TODO updateUI()
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
}
