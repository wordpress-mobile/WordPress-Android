package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.models.ReaderUserList;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.adapters.ReaderUserAdapter;
import org.wordpress.android.util.DisplayUtils;

/*
 * displays a list of users who like a specific reader post
 */
public class ReaderUserListActivity extends Activity {
    private static final String LIST_STATE = "list_state";
    private Parcelable mListState = null;
    private ListView mListView;

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
            // restore listView state so user returns to the previously scrolled-to item
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

        long blogId = getIntent().getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
        long postId = getIntent().getLongExtra(ReaderConstants.ARG_POST_ID, 0);

        if (savedInstanceState != null) {
            mListState = savedInstanceState.getParcelable(LIST_STATE);
        }

        // use a fixed size for the root view so the activity won't change
        // size as users are loaded
        final ViewGroup rootView = (ViewGroup) findViewById(R.id.layout_container);
        int displayHeight = DisplayUtils.getDisplayPixelHeight(this);
        boolean isLandscape = DisplayUtils.isLandscape(this);
        int maxHeight = displayHeight - (displayHeight / (isLandscape ? 5 : 3));
        rootView.getLayoutParams().height = maxHeight;

        getListView().setAdapter(getAdapter());
        loadUsers(blogId, postId);
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

    private void loadUsers(final long blogId, final long postId) {
        new Thread() {
            @Override
            public void run() {
                final String title = getTitleString(blogId, postId);
                final TextView txtTitle = (TextView) findViewById(R.id.text_title);

                final ReaderUserList users =
                        ReaderUserTable.getUsersWhoLikePost(
                                blogId,
                                postId,
                                ReaderConstants.READER_MAX_USERS_TO_DISPLAY);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) {
                            txtTitle.setText(title);
                            getAdapter().setUsers(users);
                        }
                    }
                });
            }
        }.start();
    }

    private String getTitleString(final long blogId, final long postId) {
        int numLikes = ReaderPostTable.getNumLikesForPost(blogId, postId);
        boolean isLikedByCurrentUser = ReaderPostTable.isPostLikedByCurrentUser(blogId, postId);

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

}
