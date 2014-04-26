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
import org.wordpress.android.ui.reader.adapters.ReaderBlogAdapter.ReaderBlogType;
import org.wordpress.android.util.AppLog;

/**
 * fragment hosted by ReaderSubsActivity which shows either recommended blogs and followed blogs
 */
public class ReaderBlogFragment extends SherlockFragment {
    private ListView mListView;
    private ReaderBlogAdapter mAdapter;
    private ReaderBlogType mBlogType;
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

    private void restoreState(Bundle args) {
        if (args == null) {
            return;
        }
        if (args.containsKey(ARG_BLOG_TYPE)) {
            mBlogType = (ReaderBlogType) args.getSerializable(ARG_BLOG_TYPE);
        }
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
    }

    private boolean hasBlogAdapter() {
        return (mAdapter != null);
    }

    protected void refreshBlogs() {
        if (hasBlogAdapter()) {
            getBlogAdapter().refresh();
        }
    }

    private ReaderBlogAdapter getBlogAdapter() {
        if (mAdapter == null) {
            mAdapter = new ReaderBlogAdapter(getActivity(), getBlogType());
        }
        return mAdapter;
    }

    private ReaderBlogType getBlogType() {
        return mBlogType;
    }
}
