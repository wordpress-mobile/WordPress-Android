package org.wordpress.android.ui.comments;

import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.comments.CommentsListFragment.OnCommentSelectedListener;
import org.wordpress.android.ui.notifications.NotificationFragment;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.ReaderPostDetailFragment;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;

public class CommentsActivity extends AppCompatActivity
        implements OnCommentSelectedListener,
                   NotificationFragment.OnPostClickListener,
                   CommentActions.OnCommentActionListener,
                   CommentActions.OnCommentChangeListener {
    private static final String KEY_SELECTED_COMMENT_ID = "selected_comment_id";
    static final String KEY_AUTO_REFRESHED = "has_auto_refreshed";
    static final String KEY_EMPTY_VIEW_MESSAGE = "empty_view_message";
    static final String SAVED_COMMENTS_STATUS_TYPE = "saved_comments_status_type";
    private long mSelectedCommentId;
    private final CommentList mTrashedComments = new CommentList();

    private final CommentStatus[] commentStatuses = {CommentStatus.UNKNOWN, CommentStatus.UNAPPROVED,
            CommentStatus.APPROVED, CommentStatus.TRASH, CommentStatus.SPAM};
    private Spinner mSpinner;
    private CommentsStatusSpinnerAdapter mCommentsStatusSpinnerAdapter;
    private CommentStatus mCurrentCommentStatusType = CommentStatus.UNKNOWN;
    private boolean mSelectingRememeberedStatusTypeOnCreate = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.comment_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (getIntent() != null && getIntent().hasExtra(SAVED_COMMENTS_STATUS_TYPE)) {
            mCurrentCommentStatusType = (CommentStatus) getIntent().getSerializableExtra(SAVED_COMMENTS_STATUS_TYPE);
        } else {
            // Read the value from app preferences here. Default to 0 - All
            mCurrentCommentStatusType = AppPrefs.getCommentsStatusFilter();
        }

        if (savedInstanceState == null) {
            CommentsListFragment commentsListFragment = new CommentsListFragment();
            // initialize comment status filter first time
            commentsListFragment.setCommentStatusFilter(mCurrentCommentStatusType);
            getFragmentManager().beginTransaction()
                    .add(R.id.layout_fragment_container, commentsListFragment, getString(R.string
                            .fragment_tag_comment_list))
                    .commitAllowingStateLoss();
        } else {
            getIntent().putExtra(KEY_AUTO_REFRESHED, savedInstanceState.getBoolean(KEY_AUTO_REFRESHED));
            getIntent().putExtra(KEY_EMPTY_VIEW_MESSAGE, savedInstanceState.getString(KEY_EMPTY_VIEW_MESSAGE));

            mSelectedCommentId = savedInstanceState.getLong(KEY_SELECTED_COMMENT_ID);
        }

        if (mSpinner == null && toolbar != null) {
            View view = View.inflate(this, R.layout.toolbar_spinner, toolbar);
            mSpinner = (Spinner) view.findViewById(R.id.action_bar_spinner);

            mCommentsStatusSpinnerAdapter = new CommentsStatusSpinnerAdapter(this, commentStatuses);

            mSelectingRememeberedStatusTypeOnCreate = true;
            mSpinner.setAdapter(mCommentsStatusSpinnerAdapter);
            mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (isFinishing()) {
                        return;
                    }

                    if (mSelectingRememeberedStatusTypeOnCreate){
                        mSelectingRememeberedStatusTypeOnCreate = false;
                        return;
                    }

                    final CommentStatus selectedCommentStatus =
                            (CommentStatus) mCommentsStatusSpinnerAdapter.getItem(position);

                    if (mCurrentCommentStatusType == selectedCommentStatus) {
                        AppLog.d(AppLog.T.COMMENTS, "The selected STATUS is already active: " +
                                selectedCommentStatus.getLabel());
                        return;
                    }

                    AppLog.d(AppLog.T.STATS, "NEW STATUS : " + selectedCommentStatus.getLabel());
                    mCurrentCommentStatusType = selectedCommentStatus;
                    AppPrefs.setCommentsStatusFilter(mCurrentCommentStatusType);
                    updateCommentList();

                    //trackCommentsAnalytics();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // nop
                }
            });
        }

        selectCurrentCommentStatusTypeInActionBar();

    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityId.trackLastActivity(ActivityId.COMMENTS);
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        AppLog.d(AppLog.T.COMMENTS, "comment activity new intent");
    }


    private CommentDetailFragment getDetailFragment() {
        Fragment fragment = getFragmentManager().findFragmentByTag(getString(
                R.string.fragment_tag_comment_detail));
        if (fragment == null) {
            return null;
        }
        return (CommentDetailFragment) fragment;
    }

    private boolean hasDetailFragment() {
        return (getDetailFragment() != null);
    }

    private CommentsListFragment getListFragment() {
        Fragment fragment = getFragmentManager().findFragmentByTag(getString(
                R.string.fragment_tag_comment_list));
        if (fragment == null) {
            return null;
        }
        return (CommentsListFragment) fragment;
    }

    private boolean hasListFragment() {
        return (getListFragment() != null);
    }

    private void showReaderFragment(long remoteBlogId, long postId) {
        FragmentManager fm = getFragmentManager();
        fm.executePendingTransactions();

        Fragment fragment = ReaderPostDetailFragment.newInstance(remoteBlogId, postId);
        FragmentTransaction ft = fm.beginTransaction();
        String tagForFragment = getString(R.string.fragment_tag_reader_post_detail);
        ft.add(R.id.layout_fragment_container, fragment, tagForFragment)
          .addToBackStack(tagForFragment)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        if (hasDetailFragment())
            ft.hide(getDetailFragment());
        ft.commit();
    }

    /*
     * called from comment list when user taps a comment
     */
    @Override
    public void onCommentSelected(long commentId) {
        mSelectedCommentId = commentId;
        FragmentManager fm = getFragmentManager();
        if (fm == null) return;

        fm.executePendingTransactions();
        CommentsListFragment listFragment = getListFragment();

        FragmentTransaction ft = fm.beginTransaction();
        String tagForFragment = getString(R.string.fragment_tag_comment_detail);
        CommentDetailFragment detailFragment = CommentDetailFragment.newInstance(WordPress.getCurrentLocalTableBlogId(),
                commentId);
        ft.add(R.id.layout_fragment_container, detailFragment, tagForFragment).addToBackStack(tagForFragment)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        if (listFragment != null) {
            ft.hide(listFragment);
        }
        ft.commitAllowingStateLoss();
    }

    /*
     * called from comment detail when user taps a link to a post - show the post in a
     * reader detail fragment
     */
    @Override
    public void onPostClicked(Note note, int remoteBlogId, int postId) {
        showReaderFragment(remoteBlogId, postId);
    }

    /*
     * reload the comment list from existing data
     */
    private void reloadCommentList() {
        CommentsListFragment listFragment = getListFragment();
        if (listFragment != null)
            listFragment.loadComments();
    }

    /*
     * tell the comment list to get recent comments from server
     */
    void updateCommentList() {
        CommentsListFragment listFragment = getListFragment();
        if (listFragment != null) {
            listFragment.setRefreshing(true);
            listFragment.setCommentStatusFilter(mCurrentCommentStatusType);
            listFragment.updateComments(false);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // https://code.google.com/p/android/issues/detail?id=19917
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }

        // retain the id of the highlighted and selected comments
        if (mSelectedCommentId != 0 && hasDetailFragment()) {
            outState.putLong(KEY_SELECTED_COMMENT_ID, mSelectedCommentId);
        }

        if (hasListFragment()) {
            outState.putBoolean(KEY_AUTO_REFRESHED, getListFragment().mHasAutoRefreshedComments);
            outState.putString(KEY_EMPTY_VIEW_MESSAGE, getListFragment().getEmptyViewMessage());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = CommentDialogs.createCommentDialog(this, id);
        if (dialog != null)
            return dialog;
        return super.onCreateDialog(id);
    }

    @Override
    public void onModerateComment(final int accountId, final Comment comment,
                                  final CommentStatus newStatus) {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        }

        if (newStatus == CommentStatus.APPROVED || newStatus == CommentStatus.UNAPPROVED) {
            getListFragment().setCommentIsModerating(comment.commentID, true);
            CommentActions.moderateComment(accountId, comment, newStatus,
                    new CommentActions.CommentActionListener() {
                @Override
                public void onActionResult(boolean succeeded) {
                    if (isFinishing() || !hasListFragment()) {
                        return;
                    }

                    getListFragment().setCommentIsModerating(comment.commentID, false);

                    if (succeeded) {
                        reloadCommentList();
                    } else {
                        ToastUtils.showToast(CommentsActivity.this,
                                R.string.error_moderate_comment,
                                ToastUtils.Duration.LONG
                        );
                    }
                }
            });
        } else if (newStatus == CommentStatus.SPAM || newStatus == CommentStatus.TRASH) {
            mTrashedComments.add(comment);
            getListFragment().removeComment(comment);
            getListFragment().setCommentIsModerating(comment.commentID, true);

            String message = (newStatus == CommentStatus.TRASH ? getString(R.string.comment_trashed) : getString(R.string.comment_spammed));
            View.OnClickListener undoListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mTrashedComments.remove(comment);
                    getListFragment().setCommentIsModerating(comment.commentID, false);
                    getListFragment().loadComments();
                }
            };

            Snackbar snackbar = Snackbar.make(getListFragment().getView(), message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo, undoListener);

            // do the actual moderation once the undo bar has been hidden
            snackbar.setCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar snackbar, int event) {
                    super.onDismissed(snackbar, event);

                    // comment will no longer exist in moderating list if action was undone
                    if (!mTrashedComments.contains(comment)) {
                        return;
                    }
                    mTrashedComments.remove(comment);

                    CommentActions.moderateComment(accountId, comment, newStatus, new CommentActions.CommentActionListener() {
                        @Override
                        public void onActionResult(boolean succeeded) {
                            if (isFinishing() || !hasListFragment()) {
                                return;
                            }
                            getListFragment().setCommentIsModerating(comment.commentID, false);
                            if (!succeeded) {
                                // show comment again upon error
                                getListFragment().loadComments();
                                ToastUtils.showToast(CommentsActivity.this,
                                        R.string.error_moderate_comment,
                                        ToastUtils.Duration.LONG
                                );
                            }
                        }
                    });
                }
            });

            snackbar.show();
        }
    }

    @Override
    public void onCommentChanged(CommentActions.ChangedFrom changedFrom, CommentActions.ChangeType changeType) {
        if (changedFrom == CommentActions.ChangedFrom.COMMENT_DETAIL) {
            switch (changeType) {
                case EDITED:
                    reloadCommentList();
                    break;
                case REPLIED:
                    updateCommentList();
                    break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
    * make sure the passed comment status type is the one selected in the actionbar
    */
    private void selectCurrentCommentStatusTypeInActionBar() {
        if (isFinishing()) {
            return;
        }

        if (mCommentsStatusSpinnerAdapter == null || mSpinner == null) {
            return;
        }

        int position = mCommentsStatusSpinnerAdapter.getIndexOfCommentStatus(mCurrentCommentStatusType);

        if (position > -1 && position != mSpinner.getSelectedItemPosition()) {
            mSpinner.setSelection(position);
        }
    }

    /*
     * adapter used by the comments status spinner
     */
    private class CommentsStatusSpinnerAdapter extends BaseAdapter {
        private final CommentStatus[] mStatuses;
        private final LayoutInflater mInflater;

        CommentsStatusSpinnerAdapter(Context context, CommentStatus[] statusesNames) {
            super();
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mStatuses = statusesNames;
        }

        @Override
        public int getCount() {
            return (mStatuses != null ? mStatuses.length : 0);
        }

        @Override
        public Object getItem(int position) {
            if (position < 0 || position >= getCount())
                return "";
            return mStatuses[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            if (convertView == null) {
                view = mInflater.inflate(R.layout.toolbar_spinner_item, parent, false);
            } else {
                view = convertView;
            }

            final TextView text = (TextView) view.findViewById(R.id.text);
            CommentStatus selectedTimeframe = (CommentStatus)getItem(position);
            text.setText(selectedTimeframe.getLabel());
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            CommentStatus selectedTimeframe = (CommentStatus)getItem(position);
            final TagViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.toolbar_spinner_dropdown_item, parent, false);
                holder = new TagViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (TagViewHolder) convertView.getTag();
            }

            holder.textView.setText(selectedTimeframe.getLabel());
            return convertView;
        }

        private class TagViewHolder {
            private final TextView textView;
            TagViewHolder(View view) {
                textView = (TextView) view.findViewById(R.id.text);
            }
        }

        public int getIndexOfCommentStatus(CommentStatus tm) {
            int pos = 0;
            for (int i = 0; i < mStatuses.length; i++) {
                if (mStatuses[i] == tm) {
                    pos = i;
                    return pos;
                }
            }
            return pos;
        }
    }
}
