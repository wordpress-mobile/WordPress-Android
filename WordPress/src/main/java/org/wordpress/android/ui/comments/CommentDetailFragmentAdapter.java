package org.wordpress.android.ui.comments;

import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.CommentStore;
import org.wordpress.android.util.AppLog;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v13.app.FragmentStatePagerAdapter;

import java.util.List;

public class CommentDetailFragmentAdapter extends FragmentStatePagerAdapter {

    private final CommentStore commentStore;
    private long mCommentId;
    private CommentStatus commentStatus;
    private SiteModel mSite;
    private List<CommentModel> comments;

    public CommentDetailFragmentAdapter(FragmentManager fm, CommentStore commentStore, long mCommentId, CommentStatus commentStatus, SiteModel mSite) {
        super(fm);
        this.commentStore = commentStore;
        this.mCommentId = mCommentId;
        this.commentStatus = commentStatus;
        this.mSite = mSite;
    }

    @Override
    public Fragment getItem(int position) {
        return CommentDetailFragment.newInstance(mSite, getComment(position));
    }

    private CommentModel getComment(int position) {
        return comments.get(position);
    }

    @Override
    public int getCount() {
        return comments.size();
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
}
