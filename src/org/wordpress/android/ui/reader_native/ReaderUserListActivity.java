package org.wordpress.android.ui.reader_native;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderLikeTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.reader_native.adapters.ReaderUserAdapter;

/**
 * Created by nbradbury on 7/8/13.
 */
public class ReaderUserListActivity extends FragmentActivity {
    protected static final String ARG_BLOG_ID = "blog_id";
    protected static final String ARG_POST_ID = "post_id";

    private ReaderPost mPost;
    private ReaderUserAdapter.ReaderUserListType mListType = ReaderUserAdapter.ReaderUserListType.UNKNOWN;

    private ListView mListView;
    private TextView mTxtTitle;

    private ListView getListView() {
        if (mListView==null)
            mListView = (ListView) findViewById(android.R.id.list);
        return mListView;
    }

    private ReaderUserAdapter mAdapter;
    private ReaderUserAdapter getAdapter() {
        if (mAdapter==null)
            mAdapter = new ReaderUserAdapter(this, mListType, mPost);
        return mAdapter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_reader_userlist);

        // hide title until text set by updateTitle()
        mTxtTitle = (TextView) findViewById(R.id.text_title);
        mTxtTitle.setVisibility(View.INVISIBLE);

        // for now this activity only supports showing users who like a specific post
        mListType = ReaderUserAdapter.ReaderUserListType.LIKE_POST;
        long blogId = getIntent().getLongExtra(ARG_BLOG_ID, 0);
        long postId = getIntent().getLongExtra(ARG_POST_ID, 0);
        mPost = ReaderPostTable.getPost(blogId, postId);

        getListView().setAdapter(getAdapter());
        updateTitle();
    }

    private void updateTitle() {
        switch (mListType) {
            case LIKE_POST:
                final Handler handler = new Handler();
                new Thread() {
                    @Override
                    public void run() {
                        int numLikes = ReaderLikeTable.getNumLikesForPost(mPost);
                        boolean isLikedByCurrentUser = ReaderPostTable.isPostLikedByCurrentUser(mPost);
                        final String title;
                        if (isLikedByCurrentUser) {
                            switch (numLikes) {
                                case 1 :
                                    title = getString(R.string.reader_likes_only_you);
                                    break;
                                case 2 :
                                    title = getString(R.string.reader_likes_you_and_one_long);
                                    break;
                                default :
                                    title = getString(R.string.reader_likes_you_and_multi_long, numLikes-1);
                                    break;
                            }
                        } else {
                            title = (numLikes == 1 ? getString(R.string.reader_likes_one) : getString(R.string.reader_likes_multi, numLikes));
                        }

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                mTxtTitle.setText(title);
                                mTxtTitle.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }.start();
                break;

            default :
                mTxtTitle.setText(R.string.reader_title_userlist_default);
                mTxtTitle.setVisibility(View.VISIBLE);
                break;
        }
    }

}
