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

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
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
import org.wordpress.android.models.FilterCriteria;
import org.wordpress.android.ui.ActionableEmptyView;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.ui.ViewPagerFragment;
import org.wordpress.android.ui.comments.CommentListItem.Comment;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SmartToast;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;

import javax.inject.Inject;

import static org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper;

/**
 * @deprecated
 * Comments are being refactored as part of Comments Unification project. If you are adding any
 * features or modifying this class, please ping develric or klymyam
 */
@Deprecated
public class CommentsListFragment extends ViewPagerFragment {
    static final int COMMENTS_PER_PAGE = 30;
    static final int MAX_COMMENTS_IN_RESPONSE = 100;
    static final String COMMENT_FILTER_KEY = "COMMENT_FILTER_KEY";
    static final String LOADING_IN_PROGRESS_KEY = "LOADING_IN_PROGRESS_KEY";
    static final String AUTO_REFRESHED_KEY = "has_auto_refreshed";

    @Nullable
    @Override
    public View getScrollableViewForUniqueIdProvision() {
        return mRecyclerView;
    }

    interface OnCommentSelectedListener {
        void onCommentSelected(long commentId, CommentStatus statusFilter);
    }

    public enum CommentStatusCriteria implements FilterCriteria {
        ALL(R.string.comment_status_all, R.string.comment_tracker_label_all),
        UNAPPROVED(R.string.comment_status_unapproved, R.string.comment_tracker_label_pending),
        APPROVED(R.string.comment_status_approved, R.string.comment_tracker_label_approved),
        UNREPLIED(R.string.comment_status_unreplied, R.string.comment_tracker_label_unreplied),
        TRASH(R.string.comment_status_trash, R.string.comment_tracker_label_trashed),
        SPAM(R.string.comment_status_spam, R.string.comment_tracker_label_spam),
        DELETE(R.string.comment_status_trash, R.string.comment_tracker_label_trashed);

        private final int mLabelResId;
        private final int mTrackerLabelResId;

        CommentStatusCriteria(@StringRes int labelResId, @StringRes int trackerLabelResId) {
            mLabelResId = labelResId;
            mTrackerLabelResId = trackerLabelResId;
        }

        @StringRes
        public int getLabelResId() {
            return mLabelResId;
        }

        @StringRes
        public int getTrackerLabelResId() {
            return mTrackerLabelResId;
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
    private boolean mHasAutoRefreshedComments = false;

    private RecyclerView mRecyclerView;
    private CommentAdapter mAdapter;
    private ActionMode mActionMode;
    private CommentStatusCriteria mCommentStatusFilter = CommentStatusCriteria.ALL;
    private ActionableEmptyView mActionableEmptyView;
    private SiteModel mSite;
    private CustomSwipeRefreshLayout mSwipeRefreshLayout;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private View mLoadMoreProgress;

    @Inject Dispatcher mDispatcher;
    @Inject CommentStore mCommentStore;


    public static CommentsListFragment newInstance(CommentStatusCriteria commentStatusFilter) {
        Bundle args = new Bundle();
        CommentsListFragment fragment = new CommentsListFragment();
        args.putSerializable(COMMENT_FILTER_KEY, commentStatusFilter);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
        mDispatcher.register(this);
        updateSiteOrFinishActivity(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        mDispatcher.unregister(this);
        super.onDestroy();
    }

    private void updateSiteOrFinishActivity(Bundle savedInstanceState) {
        mSite = (SiteModel) getActivity().getIntent().getSerializableExtra(WordPress.SITE);
        mCommentStatusFilter = (CommentStatusCriteria) getArguments().getSerializable(COMMENT_FILTER_KEY);
        if (mSite == null) {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found, ToastUtils.Duration.SHORT);
            getActivity().finish();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.comment_list_fragment, container, false);

        mActionableEmptyView = view.findViewById(R.id.actionable_empty_view);

        mRecyclerView = view.findViewById(R.id.comments_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.addItemDecoration(
                new CommentListItemDecoration(view.getContext()));

        mRecyclerView.swapAdapter(getAdapter(), true);

        mLoadMoreProgress = view.findViewById(R.id.progress);

        mSwipeRefreshLayout = view.findViewById(R.id.ptr_layout);
        mSwipeToRefreshHelper = buildSwipeToRefreshHelper(
                mSwipeRefreshLayout,
                () -> {
                    if (!NetworkUtils.checkConnection(getContext())) {
                        if (getAdapter().getItemCount() == 0) {
                            showEmptyView(EmptyViewMessageType.NETWORK_ERROR);
                        }
                        mSwipeToRefreshHelper.setRefreshing(false);
                        return;
                    }
                    updateComments(false);
                }
        );

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            mIsUpdatingComments = savedInstanceState.getBoolean(LOADING_IN_PROGRESS_KEY, false);
            mHasAutoRefreshedComments = savedInstanceState.getBoolean(AUTO_REFRESHED_KEY, false);
        }

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            showEmptyView(EmptyViewMessageType.NETWORK_ERROR);
        } else {
            if (!mHasAutoRefreshedComments) {
                updateComments(false);
                mHasAutoRefreshedComments = true;
            } else {
                getAdapter().loadInitialCachedComments(mCommentStatusFilter.toCommentStatus());
            }
        }
    }


    private void showEmptyView(EmptyViewMessageType messageType) {
        int emptyViewMessageStringId = 0;
        if (messageType == EmptyViewMessageType.NO_CONTENT) {
            switch (mCommentStatusFilter) {
                case APPROVED:
                    emptyViewMessageStringId = R.string.comments_empty_list_filtered_approved;
                    break;
                case UNAPPROVED:
                    emptyViewMessageStringId = R.string.comments_empty_list_filtered_pending;
                    break;
                case UNREPLIED:
                    emptyViewMessageStringId = R.string.comments_empty_list_filtered_unreplied;
                    break;
                case SPAM:
                    emptyViewMessageStringId = R.string.comments_empty_list_filtered_spam;
                    break;
                case TRASH:
                    emptyViewMessageStringId = R.string.comments_empty_list_filtered_trashed;
                    break;
                case DELETE:
                case ALL:
                default:
                    emptyViewMessageStringId = R.string.comments_empty_list;
                    break;
            }
            mActionableEmptyView.image.setImageResource(R.drawable.img_illustration_empty_results_216dp);
            mActionableEmptyView.image.setVisibility(View.VISIBLE);
        } else {
            @DrawableRes int emptyViewIllustrationId = 0;
            int emptyViewIllustrationVisibility = View.GONE;
            switch (messageType) {
                case LOADING:
                    emptyViewMessageStringId = R.string.comments_fetching;
                    break;
                case NETWORK_ERROR:
                    emptyViewMessageStringId = R.string.no_network_message;
                    emptyViewIllustrationId = R.drawable.img_illustration_cloud_off_152dp;
                    emptyViewIllustrationVisibility = View.VISIBLE;
                    break;
                case PERMISSION_ERROR:
                    emptyViewMessageStringId = R.string.error_refresh_unauthorized_comments;
                    break;
                case GENERIC_ERROR:
                    emptyViewMessageStringId = R.string.error_refresh_comments;
                    break;
            }
            mActionableEmptyView.image.setImageResource(emptyViewIllustrationId);
            mActionableEmptyView.image.setVisibility(emptyViewIllustrationVisibility);
        }
        mActionableEmptyView.title.setText(emptyViewMessageStringId);
        mActionableEmptyView.setVisibility(View.VISIBLE);
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
                    mActionableEmptyView.setVisibility(View.GONE);
                } else if (!mIsUpdatingComments) {
                    // Change LOADING to NO_CONTENT message
                    showEmptyView(EmptyViewMessageType.NO_CONTENT);
                }
            };

            // adapter calls this to request more comments from server when it reaches the end
            CommentAdapter.OnLoadMoreListener loadMoreListener = () -> {
                updateComments(true);
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
                    CommentModel comment = ((Comment) getAdapter().getItem(position)).getComment();
                    if (comment == null) {
                        return;
                    }
                    if (mActionMode == null) {
                        mRecyclerView.invalidate();
                        if (getActivity() instanceof OnCommentSelectedListener) {
                            CommentStatus commentStatus;
                            // for purposes of comment details UNREPLIED should be treated as ALL
                            if (mCommentStatusFilter == CommentStatusCriteria.UNREPLIED) {
                                commentStatus = CommentStatusCriteria.ALL.toCommentStatus();
                            } else {
                                commentStatus = mCommentStatusFilter.toCommentStatus();
                            }

                            ((OnCommentSelectedListener) getActivity())
                                    .onCommentSelected(comment.getRemoteCommentId(), commentStatus);
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
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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


    private void moderateComments(CommentList comments, CommentStatus status) {
        // track batch actions
        switch (status) {
            case APPROVED:
                AnalyticsTracker.track(Stat.COMMENT_BATCH_APPROVED);
                break;
            case UNAPPROVED:
                AnalyticsTracker.track(Stat.COMMENT_BATCH_UNAPPROVED);
                break;
            case SPAM:
                AnalyticsTracker.track(Stat.COMMENT_BATCH_SPAMMED);
                break;
            case TRASH:
                AnalyticsTracker.track(Stat.COMMENT_BATCH_TRASHED);
                break;
            case DELETED:
                AnalyticsTracker.track(Stat.COMMENT_BATCH_DELETED);
                break;
            case ALL:
            case UNSPAM:
            case UNTRASH:
                break; // noop
        }

        for (CommentModel comment : comments) {
            if (getActivity() instanceof CommentsActivity) {
                ((CommentsActivity) getActivity()).onModerateComment(comment, status, false);
            }
        }
    }

    void loadComments() {
        // this is called from CommentsActivity when a comment was changed in the detail view,
        // and the change will already be in SQLite so simply reload the comment adapter
        // to show the change
        getAdapter().reloadComments(mCommentStatusFilter.toCommentStatus());
    }

    /*
     * get latest comments from server, or pass loadMore=true to get comments beyond the
     * existing ones
     */
    private void updateComments(boolean loadMore) {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            showEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            mSwipeRefreshLayout.setRefreshing(false);
            ToastUtils.showToast(getActivity(), getString(R.string.error_refresh_comments_showing_older));
            // we're offline, load/refresh whatever we have in our local db
            getAdapter().reloadComments(mCommentStatusFilter.toCommentStatus());
            return;
        }

        if (getAdapter().getItemCount() == 0) {
            showEmptyView(EmptyViewMessageType.LOADING);
        }

        // immediately load/refresh whatever we have in our local db as we wait for the API call to get latest results
        if (!loadMore) {
            mSwipeRefreshLayout.setRefreshing(true);
            getAdapter().loadInitialCachedComments(mCommentStatusFilter.toCommentStatus());
        }

        int offset = 0;
        if (loadMore) {
            offset = getAdapter().getItemCount();
            mLoadMoreProgress.setVisibility(View.VISIBLE);
        }
        mIsUpdatingComments = true;
        if (mCommentStatusFilter == CommentStatusCriteria.UNREPLIED) {
            mDispatcher.dispatch(CommentActionBuilder.newFetchCommentsAction(
                    new FetchCommentsPayload(mSite, CommentStatus.ALL, MAX_COMMENTS_IN_RESPONSE, 0)));
        } else {
            mDispatcher.dispatch(CommentActionBuilder.newFetchCommentsAction(
                    new FetchCommentsPayload(mSite, mCommentStatusFilter.toCommentStatus(), COMMENTS_PER_PAGE,
                            offset)));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(LOADING_IN_PROGRESS_KEY, mIsUpdatingComments);
        outState.putSerializable(AUTO_REFRESHED_KEY, mHasAutoRefreshedComments);
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
            mSwipeRefreshLayout.setEnabled(false);
            SmartToast.disableSmartToast(SmartToast.SmartToastType.COMMENTS_LONG_PRESS);

            if (getActivity() instanceof CommentsActivity) {
                ((CommentsActivity) getActivity()).onActionModeToggled(true);
            }

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
            mSwipeRefreshLayout.setEnabled(true);
            mActionMode = null;
            if (getActivity() instanceof CommentsActivity) {
                ((CommentsActivity) getActivity()).onActionModeToggled(false);
            }
        }
    }

    private boolean shouldRefreshCommentsList(OnCommentChanged event) {
        if (event.requestedStatus == null) {
            return true;
        }

        if (event.requestedStatus.equals(mCommentStatusFilter.toCommentStatus())) {
            return true;
        }

        if (mCommentStatusFilter.toCommentStatus() == CommentStatus.UNREPLIED && event.requestedStatus
                .equals(CommentStatus.ALL)) {
            return true;
        }

        return event.causeOfChange == CommentAction.UPDATE_COMMENT
               && mCommentStatusFilter.toCommentStatus().equals(CommentStatus.ALL) && (
                       event.requestedStatus.equals(CommentStatus.UNAPPROVED) || event.requestedStatus
                               .equals(CommentStatus.APPROVED));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCommentChanged(OnCommentChanged event) {
        mLoadMoreProgress.setVisibility(View.GONE);
        mSwipeRefreshLayout.setRefreshing(false);
        mIsUpdatingComments = false;

        // Don't refresh the list on push, we already updated comments
        if (event.causeOfChange != CommentAction.PUSH_COMMENT) {
            if (shouldRefreshCommentsList(event)) {
                if (event.offset > 0) {
                    getAdapter().loadMoreComments(mCommentStatusFilter.toCommentStatus(), event.offset);
                } else {
                    getAdapter().reloadComments(mCommentStatusFilter.toCommentStatus());
                }
            }
            return;
        }

        if (event.isError()) {
            if (!TextUtils.isEmpty(event.error.message)) {
                ToastUtils.showToast(getActivity(), event.error.message);
            }
            // Reload the comment list in case of an error, we want to revert the UI to the previous state.
            getAdapter().reloadComments(mCommentStatusFilter.toCommentStatus());
        }
    }
}
