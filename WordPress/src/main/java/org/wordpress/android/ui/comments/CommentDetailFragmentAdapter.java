package org.wordpress.android.ui.comments;

import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.CommentStore;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v13.app.FragmentStatePagerAdapter;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class CommentDetailFragmentAdapter extends FragmentStatePagerAdapter {

    private final CommentStore mCommentStore;
    private final CommentStatus statusFilter;
    private final SiteModel mSite;
    private final CommentAdapter.OnDataLoadedListener onDataLoadedListener;
    private final CommentAdapter.OnLoadMoreListener onLoadMoreListener;
    private final CommentList mComments = new CommentList();

    CommentDetailFragmentAdapter(FragmentManager fm,
                                 CommentStore mCommentStore,
                                 CommentStatus statusFilter,
                                 SiteModel mSite,
                                 CommentAdapter.OnDataLoadedListener onDataLoadedListener,
                                 CommentAdapter.OnLoadMoreListener onLoadMoreListener) {
        super(fm);
        this.mCommentStore = mCommentStore;
        this.statusFilter = statusFilter;
        this.mSite = mSite;
        this.onDataLoadedListener = onDataLoadedListener;
        this.onLoadMoreListener = onLoadMoreListener;
        loadComments();
    }

    @Override
    public Fragment getItem(int position) {
        return CommentDetailFragment.newInstance(mSite, getComment(position));
    }

    public int commentIndex(long commentId) {
        return mComments.indexOfCommentId(commentId);
    }

    private CommentModel getComment(int position) {
        if (position == mComments.size() - 1) {
            onLoadMoreListener.onLoadMore();
        }
        return mComments.get(position);
    }

    @Override
    public int getCount() {
        return mComments.size();
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        // work around "Fragment no longer exists for key" Android bug
        // by catching the IllegalStateException
        // https://code.google.com/p/android/issues/detail?id=42601
        try {
            AppLog.d(AppLog.T.COMMENTS, "comments pager > adapter restoreState");
            super.restoreState(state, loader);
        } catch (IllegalStateException e) {
            AppLog.e(AppLog.T.COMMENTS, e);
        }
    }

    @Override
    public Parcelable saveState() {
        AppLog.d(AppLog.T.COMMENTS, "comments pager > adapter saveState");
        Bundle bundle = (Bundle) super.saveState();
        if (bundle == null) {
            bundle = new Bundle();
        }
        // This is a possible solution to https://github.com/wordpress-mobile/WordPress-Android/issues/5456
        // See https://issuetracker.google.com/issues/37103380#comment77 for more details
        bundle.putParcelableArray("states", null);
        return bundle;
    }

    public CommentModel getCommentAtPosition(int position){
        if (isValidPosition(position))
            return mComments.get(position);
        else
            return null;
    }

    private boolean isValidPosition(int position) {
        return (position >= 0 && position < getCount());
    }

    /*
     * load comments using an AsyncTask
     */
    private void loadComments() {
        if (mIsLoadTaskRunning) {
            AppLog.w(AppLog.T.COMMENTS, "load comments task already active");
        } else {
            new LoadCommentsTask(mCommentStore, statusFilter, mSite, new LoadCommentsCallback()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /*
     * AsyncTask to load comments from SQLite
     */
    private boolean mIsLoadTaskRunning = false;

    private class LoadCommentsCallback implements LoadCommentsTask.LoadingCallback {
        @Override
        public void isLoading(boolean loading) {
            mIsLoadTaskRunning = loading;
        }

        @Override
        public void loadingFinished(CommentList commentList) {
            if (!mComments.isSameList(commentList)) {
                mComments.clear();
                mComments.addAll(commentList);
                // Sort by date
                Collections.sort(mComments, new Comparator<CommentModel>() {
                    @Override
                    public int compare(CommentModel commentModel, CommentModel t1) {
                        Date d0 = DateTimeUtils.dateFromIso8601(commentModel.getDatePublished());
                        Date d1 = DateTimeUtils.dateFromIso8601(t1.getDatePublished());
                        if (d0 == null || d1 == null) {
                            return 0;
                        }
                        return d1.compareTo(d0);
                    }
                });
                notifyDataSetChanged();
                onDataLoadedListener.onDataLoaded(true);
            }
        }
    }
}
