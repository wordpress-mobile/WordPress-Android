package org.wordpress.android.ui.reader;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.adapters.ReaderRecommendedBlogAdapter;
import org.wordpress.android.util.AppLog;

/**
 * fragment hosted by ReaderTagActivity which shows recommended blogs
 */
public class ReaderRecommendedBlogFragment extends SherlockFragment {
    private ListView mListView;
    private ReaderRecommendedBlogAdapter mAdapter;

    static ReaderRecommendedBlogFragment newInstance() {
        AppLog.d(AppLog.T.READER, "reader recommended blog list > newInstance");
        ReaderRecommendedBlogFragment fragment = new ReaderRecommendedBlogFragment();
        return fragment;
    }

    ListView getListView() {
        return mListView;
    }

    private boolean hasBlogAdapter() {
        return (mAdapter != null);
    }

    protected void refreshBlogs() {
        if (hasBlogAdapter()) {
            getBlogAdapter().refresh();
        }
    }

    private ReaderRecommendedBlogAdapter getBlogAdapter() {
        if (mAdapter == null) {
            mAdapter = new ReaderRecommendedBlogAdapter(getActivity());
        }
        return mAdapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.reader_fragment_list, container, false);
        mListView = (ListView) view.findViewById(android.R.id.list);

        final TextView emptyView = (TextView)view.findViewById(R.id.text_empty);
        emptyView.setText(R.string.reader_empty_recommended_blogs);
        mListView.setEmptyView(emptyView);

        mListView.setAdapter(getBlogAdapter());
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getBlogAdapter().refresh();
    }
}
