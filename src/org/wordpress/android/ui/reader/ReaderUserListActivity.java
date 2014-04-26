package org.wordpress.android.ui.reader;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderUserList;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.adapters.ReaderUserAdapter;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SysUtils;

/**
 * displays a list of users who like a specific reader post
 */
public class ReaderUserListActivity extends FragmentActivity {
    private ListView mListView;
    private TextView mTxtTitle;
    private ReaderPost mPost;
    private static final String LIST_STATE = "list_state";
    private Parcelable mListState = null;

    private ListView getListView() {
        if (mListView==null)
            mListView = (ListView) findViewById(android.R.id.list);
        return mListView;
    }

    private ReaderUserAdapter mAdapter;
    private ReaderUserAdapter getAdapter() {
        if (mAdapter == null)
            mAdapter = new ReaderUserAdapter(this, mDataLoadedListener);
        return mAdapter;
    }

    /*
     * called by adapter when data has been loaded
     */
    private final ReaderActions.DataLoadedListener mDataLoadedListener = new ReaderActions.DataLoadedListener() {
        @Override
        public void onDataLoaded(boolean isEmpty) {
            // restore listView state - this returns to the previously scrolled-to item
            if (!isEmpty && mListState != null) {
                getListView().onRestoreInstanceState(mListState);
                mListState = null;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_userlist);

        // hide title until text set by updateTitle()
        mTxtTitle = (TextView) findViewById(R.id.text_title);
        mTxtTitle.setVisibility(View.INVISIBLE);

        // for now this activity only supports showing users who like a specific post
        long blogId = getIntent().getLongExtra(ReaderActivity.ARG_BLOG_ID, 0);
        long postId = getIntent().getLongExtra(ReaderActivity.ARG_POST_ID, 0);
        mPost = ReaderPostTable.getPost(blogId, postId);

        if (savedInstanceState != null)
            mListState = savedInstanceState.getParcelable(LIST_STATE);

        getListView().setAdapter(getAdapter());
        loadUsers();
        updateTitle();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (getListView().getFirstVisiblePosition() > 0)
            outState.putParcelable(LIST_STATE, getListView().onSaveInstanceState());
    }

    private void updateTitle() {
        new Thread() {
            @Override
            public void run() {
                //int numLikes = ReaderLikeTable.getNumLikesForPost(mPost);
                int numLikes = ReaderPostTable.getNumLikesForPost(mPost);
                boolean isLikedByCurrentUser = ReaderPostTable.isPostLikedByCurrentUser(mPost);

                final String title;
                if (isLikedByCurrentUser) {
                    switch (numLikes) {
                        case 1 :
                            title = getString(R.string.reader_likes_only_you);
                            break;
                        case 2 :
                            title = getString(R.string.reader_likes_you_and_one);
                            break;
                        default :
                            title = getString(R.string.reader_likes_you_and_multi, numLikes-1);
                            break;
                    }
                } else {
                    title = (numLikes == 1 ? getString(R.string.reader_likes_one) : getString(R.string.reader_likes_multi, numLikes));
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTxtTitle.setText(title);
                        mTxtTitle.setVisibility(View.VISIBLE);
                    }
                });
            }
        }.start();
    }

    @SuppressLint("NewApi")
    private void loadUsers() {
        if (mIsTaskRunning)
            AppLog.w(AppLog.T.READER, "user task already running");
        if (SysUtils.canUseExecuteOnExecutor()) {
            new LoadUsersTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new LoadUsersTask().execute();
        }
    }

    private boolean mIsTaskRunning = false;
    private class LoadUsersTask extends AsyncTask<Void, Void, Boolean> {
        ReaderUserList tmpUsers;
        @Override
        protected void onPreExecute() {
            mIsTaskRunning = true;
        }
        @Override
        protected void onCancelled() {
            mIsTaskRunning = false;
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            tmpUsers = ReaderUserTable.getUsersWhoLikePost(mPost, ReaderConstants.READER_MAX_USERS_TO_DISPLAY);
            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                getAdapter().setUsers(tmpUsers);
            }
            mIsTaskRunning = false;
        }
    }
}
