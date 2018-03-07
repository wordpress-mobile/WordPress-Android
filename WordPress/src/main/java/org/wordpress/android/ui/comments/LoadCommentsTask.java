package org.wordpress.android.ui.comments;

import android.os.AsyncTask;

import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.CommentStore;
import org.wordpress.android.models.CommentList;

import java.util.List;

class LoadCommentsTask extends AsyncTask<Void, Void, CommentList> {
    interface LoadingCallback {
        void isLoading(boolean loading);

        void loadingFinished(CommentList commentList);
    }

    private final CommentStore mCommentStore;
    private final CommentStatus mStatusFilter;
    private final SiteModel mSite;
    private final LoadingCallback mLoadingCallback;

    LoadCommentsTask(CommentStore commentStore,
                     CommentStatus statusFilter,
                     SiteModel site,
                     LoadingCallback loadingCallback) {
        this.mCommentStore = commentStore;
        this.mStatusFilter = statusFilter;
        this.mSite = site;
        this.mLoadingCallback = loadingCallback;
    }

    @Override
    protected void onPreExecute() {
        mLoadingCallback.isLoading(true);
    }

    @Override
    protected void onCancelled() {
        mLoadingCallback.isLoading(false);
    }

    @Override
    protected CommentList doInBackground(Void... params) {
        List<CommentModel> comments;
        if (mStatusFilter == null || mStatusFilter == CommentStatus.ALL) {
            // The "all" filter actually means "approved" + "unapproved" (but not "spam", "trash" or "deleted")
            comments = mCommentStore.getCommentsForSite(mSite, false,
                                                        CommentStatus.APPROVED, CommentStatus.UNAPPROVED);
        } else {
            comments = mCommentStore.getCommentsForSite(mSite, false, mStatusFilter);
        }

        CommentList tmpComments = new CommentList();
        tmpComments.addAll(comments);

        return tmpComments;
    }

    @Override
    protected void onPostExecute(CommentList loadedCommentList) {
        mLoadingCallback.loadingFinished(loadedCommentList);
        mLoadingCallback.isLoading(false);
    }
}
