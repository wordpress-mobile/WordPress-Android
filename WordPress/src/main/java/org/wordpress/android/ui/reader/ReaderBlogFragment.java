package org.wordpress.android.ui.reader;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.models.ReaderRecommendedBlog;
import org.wordpress.android.ui.reader.adapters.ReaderBlogAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderBlogAdapter.BlogFollowChangeListener;
import org.wordpress.android.ui.reader.adapters.ReaderBlogAdapter.ReaderBlogType;
import org.wordpress.android.ui.reader.views.ReaderRecyclerView;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.WPActivityUtils;

/*
 * fragment hosted by ReaderSubsActivity which shows either recommended blogs and followed blogs
 */
public class ReaderBlogFragment extends Fragment
        implements BlogFollowChangeListener,
                   ReaderBlogAdapter.BlogClickListener {
    private ReaderRecyclerView mRecyclerView;
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
        View view = inflater.inflate(R.layout.reader_fragment_list, container, false);
        mRecyclerView = (ReaderRecyclerView) view.findViewById(R.id.recycler_view);
        return view;
    }

    void checkEmptyView() {
        if (!isAdded()) {
            return;
        }
        boolean isEmpty = hasBlogAdapter() && getBlogAdapter().isEmpty();
        TextView emptyView = (TextView) getView().findViewById(R.id.text_empty);
        if (isEmpty) {
            switch (getBlogType()) {
                case RECOMMENDED:
                    emptyView.setText(R.string.reader_empty_recommended_blogs);
                    break;
                case FOLLOWED:
                    emptyView.setText(R.string.reader_empty_followed_blogs_title);
                    break;
            }
        }
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRecyclerView.setAdapter(getBlogAdapter());
        refresh();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(ARG_BLOG_TYPE, getBlogType());
        outState.putBoolean(ReaderConstants.KEY_WAS_PAUSED, mWasPaused);
        super.onSaveInstanceState(outState);
    }

    private void restoreState(Bundle args) {
        if (args != null) {
            mWasPaused = args.getBoolean(ReaderConstants.KEY_WAS_PAUSED);
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

    void refresh() {
        if (hasBlogAdapter()) {
            getBlogAdapter().refresh();
        }
    }

    private boolean hasBlogAdapter() {
        return (mAdapter != null);
    }

    private ReaderBlogAdapter getBlogAdapter() {
        if (mAdapter == null) {
            Context context = WPActivityUtils.getThemedContext(getActivity());
            mAdapter = new ReaderBlogAdapter(context, getBlogType());
            mAdapter.setFollowChangeListener(this);
            mAdapter.setBlogClickListener(this);
            mAdapter.setDataLoadedListener(new ReaderInterfaces.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    checkEmptyView();
                }
            });

        }
        return mAdapter;
    }

    public ReaderBlogType getBlogType() {
        return mBlogType;
    }

    /*
     * called from the adapter when a blog is followed or unfollowed - note that the network
     * request has already occurred by the time this is called
     */
    public void onFollowBlogChanged() {
        if (getActivity() instanceof BlogFollowChangeListener) {
            ((BlogFollowChangeListener) getActivity()).onFollowBlogChanged();
        }
    }

    @Override
    public void onBlogClicked(Object item) {
        final long blogId;
        final String blogUrl;
        final long feedId;
        if (item instanceof ReaderRecommendedBlog) {
            ReaderRecommendedBlog blog = (ReaderRecommendedBlog) item;
            blogId = blog.blogId;
            blogUrl = blog.getBlogUrl();
            feedId = 0;
        } else if (item instanceof ReaderBlog) {
            ReaderBlog blog = (ReaderBlog) item;
            blogId = blog.blogId;
            blogUrl = blog.getUrl();
            feedId = blog.feedId;
        } else {
            return;
        }

        if (feedId != 0) {
            ReaderActivityLauncher.showReaderFeedPreview(getActivity(), feedId);
        } else if (blogId != 0 || !TextUtils.isEmpty(blogUrl)) {
            ReaderActivityLauncher.showReaderBlogPreview(getActivity(), blogId, blogUrl);
        }
    }
}
