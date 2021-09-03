package org.wordpress.android.ui.comments;

import android.os.AsyncTask;

import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.ui.comments.unified.CommentsStoreAdapter;

import java.util.List;

/**
 * @deprecated
 * Comments are being refactored as part of Comments Unification project. If you are adding any
 * features or modifying this class, please ping develric or klymyam
 */
@Deprecated
class LoadCommentsTask extends AsyncTask<Void, Void, CommentList> {
    interface LoadingCallback {
        void isLoading(boolean loading);

        void loadingFinished(CommentList commentList);
    }

    private final CommentsStoreAdapter mCommentsStoreAdapter;
    private final CommentStatus mStatusFilter;
    private final SiteModel mSite;
    private final LoadingCallback mLoadingCallback;

    LoadCommentsTask(CommentsStoreAdapter commentsStoreAdapter,
                     CommentStatus statusFilter,
                     SiteModel site,
                     LoadingCallback loadingCallback) {
        this.mCommentsStoreAdapter = commentsStoreAdapter;
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
            comments = mCommentsStoreAdapter.getCommentsForSite(mSite, false, 0,
                                                        CommentStatus.APPROVED, CommentStatus.UNAPPROVED);
        } else {
            comments = mCommentsStoreAdapter.getCommentsForSite(mSite, false, 0, mStatusFilter);
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
