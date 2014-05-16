package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
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

/**
 * displays a list of users who like a specific reader post
 */
public class ReaderUserListActivity extends Activity {
    private ListView mListView;
    private ReaderPost mPost;
    private static final String LIST_STATE = "list_state";
    private Parcelable mListState = null;

    private ListView getListView() {
        if (mListView == null) {
            mListView = (ListView) findViewById(android.R.id.list);
        }
        return mListView;
    }

    private ReaderUserAdapter mAdapter;
    private ReaderUserAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new ReaderUserAdapter(this, mDataLoadedListener);
        }
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

        // for now this activity only supports showing users who like a specific post
        long blogId = getIntent().getLongExtra(ReaderActivity.ARG_BLOG_ID, 0);
        long postId = getIntent().getLongExtra(ReaderActivity.ARG_POST_ID, 0);
        mPost = ReaderPostTable.getPost(blogId, postId);

        if (savedInstanceState != null) {
            mListState = savedInstanceState.getParcelable(LIST_STATE);
        }

        getListView().setAdapter(getAdapter());
        loadUsers();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (getListView().getFirstVisiblePosition() > 0) {
            outState.putParcelable(LIST_STATE, getListView().onSaveInstanceState());
        }
    }

    private String getTitleString() {
        int numLikes = ReaderPostTable.getNumLikesForPost(mPost);
        boolean isLikedByCurrentUser = ReaderPostTable.isPostLikedByCurrentUser(mPost);

        if (isLikedByCurrentUser) {
            switch (numLikes) {
                case 1 :
                    return getString(R.string.reader_likes_only_you);
                case 2 :
                    return getString(R.string.reader_likes_you_and_one);
                default :
                    return getString(R.string.reader_likes_you_and_multi, numLikes-1);
            }
        } else {
            return (numLikes == 1 ? getString(R.string.reader_likes_one) : getString(R.string.reader_likes_multi, numLikes));
        }
    }

    private void loadUsers() {
        new Thread() {
            @Override
            public void run() {
                final String title = getTitleString();
                final ReaderUserList users = ReaderUserTable.getUsersWhoLikePost(mPost, ReaderConstants.READER_MAX_USERS_TO_DISPLAY);
                final TextView txtTitle = (TextView) findViewById(R.id.text_title);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) {
                            getAdapter().setUsers(users);
                            txtTitle.setText(title);
                        }
                    }
                });
            }
        }.start();
    }
}
