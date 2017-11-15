package org.wordpress.android.ui.comments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v13.app.FragmentStatePagerAdapter;

import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.util.AppLog;

public class CommentDetailFragmentAdapter extends FragmentStatePagerAdapter {

    private final SiteModel mSite;
    private final CommentAdapter.OnLoadMoreListener mOnLoadMoreListener;
    private final CommentList mCommentList;

    CommentDetailFragmentAdapter(FragmentManager fm,
                                 CommentList commentList,
                                 SiteModel site,
                                 CommentAdapter.OnLoadMoreListener onLoadMoreListener) {
        super(fm);
        this.mSite = site;
        this.mOnLoadMoreListener = onLoadMoreListener;
        this.mCommentList = commentList;
    }

    @Override
    public Fragment getItem(int position) {
        return CommentDetailFragment.newInstance(mSite, getComment(position));
    }

    void onNewItems(CommentList commentList) {
        mCommentList.clear();
        mCommentList.addAll(commentList);
        notifyDataSetChanged();
    }

    int commentIndex(long commentId) {
        return mCommentList.indexOfCommentId(commentId);
    }

    boolean isEmpty() {
        return getCount() == 0;
    }

    CommentModel getCommentAtPosition(int position) {
        if (isValidPosition(position))
            return mCommentList.get(position);
        else
            return null;
    }

    @Override
    public int getCount() {
        return mCommentList.size();
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

    private CommentModel getComment(int position) {
        if (position == getCount() - 1) {
            mOnLoadMoreListener.onLoadMore();
        }
        return mCommentList.get(position);
    }

    private boolean isValidPosition(int position) {
        return (position >= 0 && position < getCount());
    }
}
