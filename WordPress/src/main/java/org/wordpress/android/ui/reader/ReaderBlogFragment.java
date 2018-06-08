package org.wordpress.android.ui.reader;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.models.ReaderRecommendedBlog;
import org.wordpress.android.ui.reader.adapters.ReaderBlogAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderBlogAdapter.ReaderBlogType;
import org.wordpress.android.ui.reader.views.ReaderRecyclerView;
import org.wordpress.android.util.AppLog;

/*
 * fragment hosted by ReaderSubsActivity which shows either recommended blogs and followed blogs
 */
public class ReaderBlogFragment extends Fragment
        implements ReaderBlogAdapter.BlogClickListener {
    private ReaderRecyclerView mRecyclerView;
    private ReaderBlogAdapter mAdapter;
    private ReaderBlogType mBlogType;
    private String mSearchFilter;
    private boolean mIgnoreNextSearch;

    private static final String ARG_BLOG_TYPE = "blog_type";
    private static final String KEY_SEARCH_FILTER = "search_filter";

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
            mIgnoreNextSearch = true;
            restoreState(savedInstanceState);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.reader_fragment_list, container, false);
        mRecyclerView = (ReaderRecyclerView) view.findViewById(R.id.recycler_view);

        // options menu (with search) only appears for followed blogs
        setHasOptionsMenu(getBlogType() == ReaderBlogType.FOLLOWED);

        return view;
    }

    private void checkEmptyView() {
        if (!isAdded()) {
            return;
        }

        TextView emptyView = (TextView) getView().findViewById(R.id.text_empty);
        if (emptyView == null) {
            return;
        }

        boolean isEmpty = hasBlogAdapter() && getBlogAdapter().isEmpty();
        if (isEmpty) {
            switch (getBlogType()) {
                case RECOMMENDED:
                    emptyView.setText(R.string.reader_empty_recommended_blogs);
                    break;
                case FOLLOWED:
                    if (getBlogAdapter().hasSearchFilter()) {
                        emptyView.setText(R.string.reader_empty_followed_blogs_search_title);
                    } else {
                        emptyView.setText(R.string.reader_empty_followed_blogs_title);
                    }
                    break;
            }
        }
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRecyclerView.setAdapter(getBlogAdapter());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(ARG_BLOG_TYPE, getBlogType());
        if (getBlogAdapter().hasSearchFilter()) {
            outState.putString(KEY_SEARCH_FILTER, getBlogAdapter().getSearchFilter());
        }
        super.onSaveInstanceState(outState);
    }

    private void restoreState(Bundle args) {
        if (args != null) {
            if (args.containsKey(ARG_BLOG_TYPE)) {
                mBlogType = (ReaderBlogType) args.getSerializable(ARG_BLOG_TYPE);
            }
            if (args.containsKey(KEY_SEARCH_FILTER)) {
                mSearchFilter = args.getString(KEY_SEARCH_FILTER);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    /*
     * note this will only be called for followed blogs
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.reader_subs, menu);

        MenuItem searchMenu = menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) searchMenu.getActionView();
        searchView.setQueryHint(getString(R.string.reader_hint_search_followed_sites));

        MenuItemCompat.setOnActionExpandListener(searchMenu, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                setSearchFilter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // when the fragment is recreated this will be called with an empty query
                // string, causing the existing search query to be lost - work around this
                // by ignoring the next search performed after recreation
                if (mIgnoreNextSearch) {
                    mIgnoreNextSearch = false;
                    AppLog.i(AppLog.T.READER, "reader subs > ignoring search");
                } else {
                    setSearchFilter(newText);
                }
                return false;
            }
        });

        // make sure the search view is expanded and reflects the current filter
        if (!TextUtils.isEmpty(mSearchFilter)) {
            searchMenu.expandActionView();
            searchView.clearFocus();
            searchView.setQuery(mSearchFilter, false);
        }
    }

    void refresh() {
        if (hasBlogAdapter()) {
            AppLog.d(AppLog.T.READER, "reader subs > refreshing blog fragment " + getBlogType().name());
            getBlogAdapter().refresh();
        }
    }

    private void setSearchFilter(String constraint) {
        mSearchFilter = constraint;
        getBlogAdapter().setSearchFilter(constraint);
    }

    private boolean hasBlogAdapter() {
        return (mAdapter != null);
    }

    private ReaderBlogAdapter getBlogAdapter() {
        if (mAdapter == null) {
            mAdapter = new ReaderBlogAdapter(getBlogType(), mSearchFilter);
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

    @Override
    public void onBlogClicked(Object item) {
        if (item instanceof ReaderRecommendedBlog) {
            ReaderRecommendedBlog blog = (ReaderRecommendedBlog) item;
            ReaderActivityLauncher.showReaderBlogPreview(getActivity(), blog.blogId);
        } else if (item instanceof ReaderBlog) {
            ReaderBlog blog = (ReaderBlog) item;
            ReaderActivityLauncher.showReaderBlogOrFeedPreview(getActivity(), blog.blogId, blog.feedId);
        }
    }
}
