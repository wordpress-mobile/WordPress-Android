package org.wordpress.android.ui.comments;

import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.util.AppLog;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v13.app.FragmentStatePagerAdapter;

public class CommentDetailFragmentAdapter extends FragmentStatePagerAdapter {

    private final SiteModel mSite;
    private final CommentAdapter.OnLoadMoreListener onLoadMoreListener;
    private final CommentList mComments;

    CommentDetailFragmentAdapter(FragmentManager fm, CommentList mComments, SiteModel mSite, CommentAdapter.OnLoadMoreListener onLoadMoreListener) {
        super(fm);
        this.mSite = mSite;
        this.onLoadMoreListener = onLoadMoreListener;
        this.mComments = mComments;
    }

    @Override
    public Fragment getItem(int position) {
        return CommentDetailFragment.newInstance(mSite, getComment(position));
    }

    int commentIndex(long commentId) {
        return mComments.indexOfCommentId(commentId);
    }

    boolean isEmpty() {
        return getCount() == 0;
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

    CommentModel getCommentAtPosition(int position){
        if (isValidPosition(position))
            return mComments.get(position);
        else
            return null;
    }

    private boolean isValidPosition(int position) {
        return (position >= 0 && position < getCount());
    }
}
