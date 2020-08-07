package org.wordpress.android.ui.comments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.models.FilterCriteria;
import org.wordpress.android.ui.ActionableEmptyView;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.ui.FilteredRecyclerView;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SmartToast;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class CommentsListFragment extends Fragment {
    static final int COMMENTS_PER_PAGE = 30;

    interface OnCommentSelectedListener {
        void onCommentSelected(long commentId, CommentStatus statusFilter);
    }

    public enum CommentStatusCriteria implements FilterCriteria {
        ALL(R.string.comment_status_all),
        UNAPPROVED(R.string.comment_status_unapproved),
        APPROVED(R.string.comment_status_approved),
        TRASH(R.string.comment_status_trash),
        SPAM(R.string.comment_status_spam),
        DELETE(R.string.comment_status_trash);

        private final int mLabelResId;

        CommentStatusCriteria(@StringRes int labelResId) {
            mLabelResId = labelResId;
        }

        @Override
        public String getLabel() {
            return WordPress.getContext().getString(mLabelResId);
        }

        public static CommentStatusCriteria fromCommentStatus(CommentStatus status) {
            return valueOf(status.name());
        }

        public CommentStatus toCommentStatus() {
            return CommentStatus.fromString(name());
        }
    }

    private boolean mIsUpdatingComments = false;
    private boolean mCanLoadMoreComments = true;
    boolean mHasAutoRefreshedComments = false;

    private final CommentStatusCriteria[] mCommentStatuses = {
            CommentStatusCriteria.ALL, CommentStatusCriteria.UNAPPROVED, CommentStatusCriteria.APPROVED,
            CommentStatusCriteria.TRASH, CommentStatusCriteria.SPAM};

    private EmptyViewMessageType mEmptyViewMessageType = EmptyViewMessageType.NO_CONTENT;
    private FilteredRecyclerView mFilteredCommentsView;
    private CommentAdapter mAdapter;
    private ActionMode mActionMode;
    private CommentStatusCriteria mCommentStatusFilter = CommentStatusCriteria.ALL;
    private ActionableEmptyView mActionableEmptyView;
    private SiteModel mSite;

    @Inject Dispatcher mDispatcher;
    @Inject CommentStore mCommentStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
        mDispatcher.register(this);
        updateSiteOrFinishActivity(savedInstanceState);
        if (savedInstanceState == null) {
            SmartToast.show(getActivity(), SmartToast.SmartToastType.COMMENTS_LONG_PRESS);
        }
    }

    @Override
    public void onDestroy() {
        mDispatcher.unregister(this);
        super.onDestroy();
    }

    private void updateSiteOrFinishActivity(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            if (getArguments() != null) {
                mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
            } else {
                mSite = (SiteModel) getActivity().getIntent().getSerializableExtra(WordPress.SITE);
            }
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }

        if (mSite == null) {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found, ToastUtils.Duration.SHORT);
            getActivity().finish();
        }
    }

    private CommentAdapter getAdapter() {
        if (mAdapter == null) {
            // called after comments have been loaded
            CommentAdapter.OnDataLoadedListener dataLoadedListener = isEmpty -> {
                if (!isAdded()) {
                    return;
                }

                if (!isEmpty) {
                    // Hide the empty view if there are already some displayed comments
                    mFilteredCommentsView.hideEmptyView();
                    mFilteredCommentsView.setToolbarScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL);
                    mActionableEmptyView.setVisibility(View.GONE);
                } else if (!mIsUpdatingComments) {
                    // Change LOADING to NO_CONTENT message
                    mFilteredCommentsView.updateEmptyView(EmptyViewMessageType.NO_CONTENT);
                }
            };

            // adapter calls this to request more comments from server when it reaches the end
            CommentAdapter.OnLoadMoreListener loadMoreListener = () -> {
                if (mCanLoadMoreComments && !mIsUpdatingComments) {
                    updateComments(true);
                }
            };

            // adapter calls this when selected comments have changed (CAB)
            CommentAdapter.OnSelectedItemsChangeListener changeListener =
                    () -> {
                        if (mActionMode != null) {
                            if (getSelectedCommentCount() == 0) {
                                mActionMode.finish();
                            } else {
                                updateActionModeTitle();
                                // must invalidate to ensure onPrepareActionMode is called
                                mActionMode.invalidate();
                            }
                        }
                    };

            CommentAdapter.OnCommentPressedListener pressedListener = new CommentAdapter.OnCommentPressedListener() {
                @Override
                public void onCommentPressed(int position, View view) {
                    CommentModel comment = getAdapter().getItem(position);
                    if (comment == null) {
                        return;
                    }
                    if (mActionMode == null) {
                        mFilteredCommentsView.invalidate();
                        if (getActivity() instanceof OnCommentSelectedListener) {
                            ((OnCommentSelectedListener) getActivity()).onCommentSelected(comment.getRemoteCommentId(),
                                    mCommentStatusFilter
                                            .toCommentStatus());
                        }
                    } else {
                        getAdapter().toggleItemSelected(position, view);
                    }
                }

                @Override
                public void onCommentLongPressed(int position, View view) {
                    // enable CAB if it's not already enabled
                    if (mActionMode == null) {
                        if (getActivity() instanceof AppCompatActivity) {
                            ((AppCompatActivity) getActivity()).startSupportActionMode(new ActionModeCallback());
                            getAdapter().setEnableSelection(true);
                            getAdapter().setItemSelected(position, true, view);
                        }
                    } else {
                        getAdapter().toggleItemSelected(position, view);
                    }
                }
            };

            mAdapter = new CommentAdapter(getActivity(), mSite);
            mAdapter.setOnCommentPressedListener(pressedListener);
            mAdapter.setOnDataLoadedListener(dataLoadedListener);
            mAdapter.setOnLoadMoreListener(loadMoreListener);
            mAdapter.setOnSelectedItemsChangeListener(changeListener);
        }

        return mAdapter;
    }

    private boolean hasAdapter() {
        return (mAdapter != null);
    }

    private int getSelectedCommentCount() {
        return getAdapter().getSelectedCommentCount();
    }

    void removeComment(CommentModel comment) {
        if (hasAdapter() && comment != null) {
            getAdapter().removeComment(comment);
        }
        // Show the empty view if the comment count drop to zero
        updateEmptyView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle extras = getActivity().getIntent().getExtras();
        if (extras != null) {
            mHasAutoRefreshedComments = extras.getBoolean(CommentsActivity.KEY_AUTO_REFRESHED);
            mEmptyViewMessageType = EmptyViewMessageType.getEnumFromString(extras.getString(
                    CommentsActivity.KEY_EMPTY_VIEW_MESSAGE));
        } else {
            mHasAutoRefreshedComments = false;
            mEmptyViewMessageType = EmptyViewMessageType.NO_CONTENT;
        }

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            mFilteredCommentsView.updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            return;
        }

        // Restore the empty view's message
        mFilteredCommentsView.updateEmptyView(mEmptyViewMessageType);

        if (!mHasAutoRefreshedComments) {
            updateComments(false);
            mHasAutoRefreshedComments = true;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.comment_list_fragment, container, false);

        mActionableEmptyView = view.findViewById(R.id.actionable_empty_view);
        mFilteredCommentsView = view.findViewById(R.id.filtered_recycler_view);
        mFilteredCommentsView
                .addItemDecoration(new DividerItemDecoration(view.getContext(), DividerItemDecoration.VERTICAL));
        mFilteredCommentsView.setLogT(AppLog.T.COMMENTS);
        mFilteredCommentsView.setFilterListener(new FilteredRecyclerView.FilterListener() {
            @Override
            public List<FilterCriteria> onLoadFilterCriteriaOptions(boolean refresh) {
                @SuppressWarnings("unchecked")
                ArrayList<FilterCriteria> criteria = new ArrayList();
                Collections.addAll(criteria, mCommentStatuses);
                return criteria;
            }

            @Override
            public void onLoadFilterCriteriaOptionsAsync(
                    FilteredRecyclerView.FilterCriteriaAsyncLoaderListener listener,
                    boolean refresh) {
            }

            @Override
            public void onLoadData(boolean forced) {
                updateComments(false);
            }

            @Override
            public void onFilterSelected(int position, FilterCriteria criteria) {
                AppPrefs.setCommentsStatusFilter((CommentStatusCriteria) criteria);
                mCommentStatusFilter = (CommentStatusCriteria) criteria;
            }

            @Override
            public FilterCriteria onRecallSelection() {
                mCommentStatusFilter = AppPrefs.getCommentsStatusFilter();
                return mCommentStatusFilter;
            }

            @Override
            public String onShowEmptyViewMessage(EmptyViewMessageType emptyViewMsgType) {
                if (emptyViewMsgType == EmptyViewMessageType.NO_CONTENT) {
                    FilterCriteria filter = mFilteredCommentsView.getCurrentFilter();
                    String title;

                    if (filter == null || filter == CommentStatusCriteria.ALL) {
                        title = getString(R.string.comments_empty_list);
                    } else {
                        switch (mCommentStatusFilter) {
                            case APPROVED:
                                title = getString(R.string.comments_empty_list_filtered_approved);
                                break;
                            case UNAPPROVED:
                                title = getString(R.string.comments_empty_list_filtered_pending);
                                break;
                            case SPAM:
                                title = getString(R.string.comments_empty_list_filtered_spam);
                                break;
                            case TRASH:
                                title = getString(R.string.comments_empty_list_filtered_trashed);
                                break;
                            default:
                                title = getString(R.string.comments_empty_list);
                        }
                    }

                    mActionableEmptyView.title.setText(title);
                    mActionableEmptyView.setVisibility(View.VISIBLE);
                    mFilteredCommentsView.setToolbarScrollFlags(0);
                    return "";
                } else {
                    int stringId = 0;
                    switch (emptyViewMsgType) {
                        case LOADING:
                            stringId = R.string.comments_fetching;
                            break;
                        case NETWORK_ERROR:
                            stringId = R.string.no_network_message;
                            break;
                        case PERMISSION_ERROR:
                            stringId = R.string.error_refresh_unauthorized_comments;
                            break;
                        case GENERIC_ERROR:
                            stringId = R.string.error_refresh_comments;
                            break;
                    }

                    mActionableEmptyView.setVisibility(View.GONE);
                    mFilteredCommentsView.setToolbarScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL);
                    return getString(stringId);
                }
            }

            @Override
            public void onShowCustomEmptyView(EmptyViewMessageType emptyViewMsgType) {
            }
        });

        mFilteredCommentsView.setToolbarSpinnerDrawable(R.drawable.ic_dropdown_primary_30_24dp);
        mFilteredCommentsView.setToolbarLeftAndRightPadding(
                getResources().getDimensionPixelSize(R.dimen.margin_filter_spinner),
                getResources().getDimensionPixelSize(R.dimen.margin_none));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFilteredCommentsView.getAdapter() == null) {
            mFilteredCommentsView.setAdapter(getAdapter());
            if (!NetworkUtils.isNetworkAvailable(getActivity())) {
                ToastUtils.showToast(getActivity(), getString(R.string.error_refresh_comments_showing_older));
            }
            getAdapter().loadComments(mCommentStatusFilter.toCommentStatus());
        }
    }

    void setCommentStatusFilter(CommentStatus statusFilter) {
        mCommentStatusFilter = CommentStatusCriteria.fromCommentStatus(statusFilter);
    }

    private void dismissDialog(int id) {
        if (!isAdded()) {
            return;
        }
        try {
            getActivity().dismissDialog(id);
        } catch (IllegalArgumentException e) {
            // raised when dialog wasn't created
        }
    }

    private void moderateSelectedComments(final CommentStatus newStatus) {
        final CommentList selectedComments = getAdapter().getSelectedComments();
        final CommentList updateComments = new CommentList();

        // build list of comments whose status is different than passed
        for (CommentModel comment : selectedComments) {
            if (CommentStatus.fromString(comment.getStatus()) != newStatus) {
                updateComments.add(comment);
            }
        }
        if (updateComments.size() == 0) {
            return;
        }

        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        getAdapter().clearSelectedComments();
        finishActionMode();

        moderateComments(updateComments, newStatus);
    }

    private void confirmDeleteComments() {
        if (mCommentStatusFilter == CommentStatusCriteria.TRASH) {
            AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(getActivity());
            dialogBuilder.setTitle(getResources().getText(R.string.delete));
            int resId = getAdapter().getSelectedCommentCount() > 1 ? R.string.dlg_sure_to_delete_comments
                    : R.string.dlg_sure_to_delete_comment;
            dialogBuilder.setMessage(getResources().getText(resId));
            dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),
                    (dialog, whichButton) -> deleteSelectedComments(true));
            dialogBuilder.setNegativeButton(getResources().getText(R.string.no), null);
            dialogBuilder.setCancelable(true);
            dialogBuilder.create().show();
        } else {
            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
            builder.setMessage(R.string.dlg_confirm_trash_comments);
            builder.setTitle(R.string.trash);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.dlg_confirm_action_trash, (dialog, id) -> deleteSelectedComments(false));
            builder.setNegativeButton(R.string.dlg_cancel_action_dont_trash, (dialog, id) -> dialog.cancel());

            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    private void deleteSelectedComments(boolean deletePermanently) {
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }
        final int dlgId = deletePermanently ? CommentDialogs.ID_COMMENT_DLG_DELETING
                : CommentDialogs.ID_COMMENT_DLG_TRASHING;
        final CommentList selectedComments = getAdapter().getSelectedComments();
        CommentStatus newStatus = CommentStatus.TRASH;
        if (deletePermanently) {
            newStatus = CommentStatus.DELETED;
        }
        dismissDialog(dlgId);
        finishActionMode();
        moderateComments(selectedComments, newStatus);
    }

    private boolean shouldRemoveCommentFromList(CommentModel comment) {
        CommentStatus status = CommentStatus.fromString(comment.getStatus());
        switch (mCommentStatusFilter) {
            case ALL:
                return status != CommentStatus.APPROVED && status != CommentStatus.UNAPPROVED;
            case UNAPPROVED:
                return status != CommentStatus.UNAPPROVED;
            case APPROVED:
                return status != CommentStatus.APPROVED;
            case TRASH:
                return status != CommentStatus.TRASH;
            case SPAM:
                return status != CommentStatus.SPAM;
            case DELETE:
            default:
                return true;
        }
    }

    private void moderateComments(CommentList comments, CommentStatus status) {
        for (CommentModel comment : comments) {
            // Preemptive update
            comment.setStatus(status.toString());
            if (shouldRemoveCommentFromList(comment)) {
                removeComment(comment);
            }
            if (status == CommentStatus.DELETED) {
                // For deletion, we need to dispatch a specific action.
                mDispatcher.dispatch(CommentActionBuilder.newDeleteCommentAction(
                        new RemoteCommentPayload(mSite, comment)));
            } else {
                // Dispatch the update
                mDispatcher.dispatch(CommentActionBuilder.newPushCommentAction(
                        new RemoteCommentPayload(mSite, comment)));
            }
        }
    }

    void loadComments() {
        // this is called from CommentsActivity when a comment was changed in the detail view,
        // and the change will already be in SQLite so simply reload the comment adapter
        // to show the change
        getAdapter().loadComments(mCommentStatusFilter.toCommentStatus());
    }

    void updateEmptyView() {
        // this is called from CommentsActivity in the case the last moment for a given type has been changed from that
        // status, leaving the list empty, so we need to update the empty view. The method inside FilteredRecyclerView
        // does the handling itself, so we only check for null here.
        if (mFilteredCommentsView != null) {
            mFilteredCommentsView.updateEmptyView(EmptyViewMessageType.NO_CONTENT);
        }
    }

    /*
     * get latest comments from server, or pass loadMore=true to get comments beyond the
     * existing ones
     */
    private void updateComments(boolean loadMore) {
        if (mIsUpdatingComments) {
            AppLog.w(AppLog.T.COMMENTS, "update comments task already running");
            return;
        } else if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            mFilteredCommentsView.updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            mFilteredCommentsView.setRefreshing(false);
            ToastUtils.showToast(getActivity(), getString(R.string.error_refresh_comments_showing_older));
            // we're offline, load/refresh whatever we have in our local db
            getAdapter().loadComments(mCommentStatusFilter.toCommentStatus());
            return;
        }

        // immediately load/refresh whatever we have in our local db as we wait for the API call to get latest results
        if (!loadMore) {
            getAdapter().loadComments(mCommentStatusFilter.toCommentStatus());
        }

        mFilteredCommentsView.updateEmptyView(EmptyViewMessageType.LOADING);

        int offset = 0;
        if (loadMore) {
            offset = getAdapter().getItemCount();
            mFilteredCommentsView.showLoadingProgress();
        }
        mFilteredCommentsView.setRefreshing(true);

        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentsAction(
                new FetchCommentsPayload(mSite, mCommentStatusFilter.toCommentStatus(), COMMENTS_PER_PAGE, offset)));
    }


    String getEmptyViewMessage() {
        return mEmptyViewMessageType.name();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable(WordPress.SITE, mSite);
        super.onSaveInstanceState(outState);
    }

    /****
     * Contextual ActionBar (CAB) routines
     ***/
    private void updateActionModeTitle() {
        if (mActionMode == null) {
            return;
        }
        int numSelected = getSelectedCommentCount();
        if (numSelected > 0) {
            mActionMode.setTitle(Integer.toString(numSelected));
        } else {
            mActionMode.setTitle("");
        }
    }

    private void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    private final class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            mActionMode = actionMode;
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.menu_comments_cab, menu);
            mFilteredCommentsView.setSwipeToRefreshEnabled(false);
            SmartToast.disableSmartToast(SmartToast.SmartToastType.COMMENTS_LONG_PRESS);
            return true;
        }

        private void setItemEnabled(Menu menu, int menuId, boolean isEnabled, boolean isVisible) {
            final MenuItem item = menu.findItem(menuId);
            if (item == null || (item.isEnabled() == isEnabled && item.isVisible() == isVisible)) {
                return;
            }
            item.setVisible(isVisible);
            item.setEnabled(isEnabled);
            if (item.getIcon() != null) {
                // must mutate the drawable to avoid affecting other instances of it
                Drawable icon = item.getIcon().mutate();
                icon.setAlpha(isEnabled ? 255 : 128);
                item.setIcon(icon);
            }
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            final CommentList selectedComments = getAdapter().getSelectedComments();

            boolean hasSelection = (selectedComments.size() > 0);
            boolean hasApproved = hasSelection && selectedComments.hasAnyWithStatus(CommentStatus.APPROVED);
            boolean hasUnapproved = hasSelection && selectedComments.hasAnyWithStatus(CommentStatus.UNAPPROVED);
            boolean hasSpam = hasSelection && selectedComments.hasAnyWithStatus(CommentStatus.SPAM);
            boolean hasAnyNonSpam = hasSelection && selectedComments.hasAnyWithoutStatus(CommentStatus.SPAM);
            boolean hasTrash = hasSelection && selectedComments.hasAnyWithStatus(CommentStatus.TRASH);

            setItemEnabled(menu, R.id.menu_approve, hasUnapproved || hasSpam || hasTrash, true);
            setItemEnabled(menu, R.id.menu_unapprove, hasApproved, true);
            setItemEnabled(menu, R.id.menu_spam, hasAnyNonSpam, hasAnyNonSpam);
            setItemEnabled(menu, R.id.menu_unspam, hasSpam && !hasAnyNonSpam, hasSpam && !hasAnyNonSpam);
            setItemEnabled(menu, R.id.menu_trash, hasSelection, true);

            final MenuItem trashItem = menu.findItem(R.id.menu_trash);
            if (trashItem != null && mCommentStatusFilter == CommentStatusCriteria.TRASH) {
                trashItem.setTitle(R.string.mnu_comment_delete_permanently);
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            int numSelected = getSelectedCommentCount();
            if (numSelected == 0) {
                return false;
            }

            int i = menuItem.getItemId();
            if (i == R.id.menu_approve) {
                moderateSelectedComments(CommentStatus.APPROVED);
                return true;
            } else if (i == R.id.menu_unapprove) {
                moderateSelectedComments(CommentStatus.UNAPPROVED);
                return true;
            } else if (i == R.id.menu_unspam) {
                moderateSelectedComments(CommentStatus.APPROVED);
                return true;
            } else if (i == R.id.menu_spam) {
                moderateSelectedComments(CommentStatus.SPAM);
                return true;
            } else if (i == R.id.menu_trash) {
                // unlike the other status changes, we ask the user to confirm trashing
                confirmDeleteComments();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            getAdapter().setEnableSelection(false);
            mFilteredCommentsView.setSwipeToRefreshEnabled(true);
            mActionMode = null;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCommentChanged(OnCommentChanged event) {
        mFilteredCommentsView.hideLoadingProgress();
        mFilteredCommentsView.setRefreshing(false);

        // Don't refresh the list on push, we already updated comments
        if (event.causeOfChange != CommentAction.PUSH_COMMENT) {
            loadComments();
        }
        if (event.isError()) {
            if (!TextUtils.isEmpty(event.error.message)) {
                ToastUtils.showToast(getActivity(), event.error.message);
            }
            // Reload the comment list in case of an error, we want to revert the UI to the previous state.
            loadComments();
        }
    }
}
