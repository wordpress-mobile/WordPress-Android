package org.wordpress.android.ui.reader;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.adapters.ReaderBlogAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderBlogAdapter.BlogFollowChangeListener;
import org.wordpress.android.ui.reader.adapters.ReaderBlogAdapter.BlogRecommendationIgnoredListener;
import org.wordpress.android.ui.reader.adapters.ReaderBlogAdapter.ReaderBlogType;
import org.wordpress.android.util.AppLog;

/**
 * fragment hosted by ReaderSubsActivity which shows either recommended blogs and followed blogs
 */
public class ReaderBlogFragment extends SherlockFragment
                                implements BlogFollowChangeListener, BlogRecommendationIgnoredListener {
    private ListView mListView;
    private ReaderBlogAdapter mAdapter;
    private ReaderBlogType mBlogType;
    private boolean mWasPaused;
    private static final String ARG_BLOG_TYPE = "blog_type";

    static ReaderBlogFragment newInstance(ReaderBlogType blogType) {
        AppLog.d(AppLog.T.READER, "reader blog fragment > newInstance");
        Bundle args = new Bundle();
        args.putSerializable(ARG_BLOG_TYPE, blogType);
        ReaderBlogFragment fragment = new ReaderBlogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        restoreState(args);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            AppLog.d(AppLog.T.READER, "reader blog fragment > restoring instance state");
            restoreState(savedInstanceState);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.reader_fragment_list, container, false);
        mListView = (ListView) view.findViewById(android.R.id.list);

        final TextView emptyView = (TextView)view.findViewById(R.id.text_empty);
        switch (getBlogType()) {
            case RECOMMENDED:
                emptyView.setText(R.string.reader_empty_recommended_blogs);
                break;
            case FOLLOWED:
                emptyView.setText(R.string.reader_empty_followed_blogs_title);
                break;
        }

        mListView.setEmptyView(emptyView);
        mListView.setAdapter(getBlogAdapter());

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getBlogAdapter().refresh();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ARG_BLOG_TYPE, getBlogType());
        outState.putBoolean(ReaderActivity.KEY_WAS_PAUSED, mWasPaused);
    }

    private void restoreState(Bundle args) {
        if (args != null) {
            mWasPaused = args.getBoolean(ReaderActivity.KEY_WAS_PAUSED);
            if (args.containsKey(ARG_BLOG_TYPE)) {
                mBlogType = (ReaderBlogType) args.getSerializable(ARG_BLOG_TYPE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mWasPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        // if the fragment is resuming from a paused state, reload the adapter to make sure
        // the follow status of all blogs is accurate - this is necessary in case the user
        // returned from an activity where the follow status may have been changed
        if (mWasPaused) {
            mWasPaused = false;
            if (hasBlogAdapter()) {
                getBlogAdapter().checkFollowStatus();
            }
        }
    }

    private boolean hasBlogAdapter() {
        return (mAdapter != null);
    }

    protected void refresh() {
        if (hasBlogAdapter()) {
            getBlogAdapter().refresh();
        }
    }

    private ReaderBlogAdapter getBlogAdapter() {
        if (mAdapter == null) {
            mAdapter = new ReaderBlogAdapter(getActivity(), getBlogType(), this, this);
        }
        return mAdapter;
    }

    private ReaderBlogType getBlogType() {
        return mBlogType;
    }

    /*
     * called from the adapter when a blog is followed or unfollowed
     */
    public void onFollowBlogChanged(long blogId, String blogUrl, boolean isFollowed) {
        if (getActivity() instanceof BlogFollowChangeListener) {
            ((BlogFollowChangeListener) getActivity()).onFollowBlogChanged(blogId, blogUrl, isFollowed);
        }
    }

    /*
     * called from the adapter when a blog recommendation is ignored
     */
    @Override
    public void onRecommendationIgnored(long blogId) {
        if (getActivity() instanceof BlogRecommendationIgnoredListener) {
            ((BlogRecommendationIgnoredListener) getActivity()).onRecommendationIgnored(blogId);
        }
    }
}
