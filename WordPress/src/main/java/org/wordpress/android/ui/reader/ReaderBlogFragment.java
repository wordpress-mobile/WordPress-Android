package org.wordpress.android.ui.reader;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.models.ReaderRecommendedBlog;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.ActionableEmptyView;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.adapters.ReaderBlogAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderBlogAdapter.ReaderBlogType;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.reader_fragment_list, container, false);
        mRecyclerView = view.findViewById(R.id.recycler_view);

        // options menu (with search) only appears for followed blogs
        setHasOptionsMenu(getBlogType() == ReaderBlogType.FOLLOWED);

        return view;
    }

    private void checkEmptyView() {
        if (!isAdded() || getView() == null) {
            return;
        }

        ActionableEmptyView actionableEmptyView = getView().findViewById(R.id.actionable_empty_view);

        if (actionableEmptyView == null) {
            return;
        }

        if (hasBlogAdapter() && getBlogAdapter().isEmpty()) {
            actionableEmptyView.setVisibility(View.VISIBLE);
            actionableEmptyView.image.setImageResource(R.drawable.img_illustration_following_empty_results_196dp);
            actionableEmptyView.subtitle.setText(R.string.reader_empty_followed_blogs_description);
            actionableEmptyView.button.setText(R.string.reader_empty_followed_blogs_button_discover);
            actionableEmptyView.button.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View view) {
                    ReaderTag tag = ReaderUtils.getTagFromEndpoint(ReaderTag.DISCOVER_PATH);

                    if (!ReaderTagTable.tagExists(tag)) {
                        tag = ReaderTagTable.getFirstTag();
                    }

                    AppPrefs.setReaderTag(tag);

                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                }
            });

            switch (getBlogType()) {
                case RECOMMENDED:
                    actionableEmptyView.title.setText(R.string.reader_empty_recommended_blogs);
                    break;
                case FOLLOWED:
                    if (getBlogAdapter().hasSearchFilter()) {
                        actionableEmptyView.updateLayoutForSearch(true, 0);
                        actionableEmptyView.title.setText(R.string.reader_empty_followed_blogs_search_title);
                        actionableEmptyView.subtitle.setVisibility(View.GONE);
                        actionableEmptyView.button.setVisibility(View.GONE);
                        actionableEmptyView.image.setVisibility(View.GONE);
                    } else {
                        actionableEmptyView.updateLayoutForSearch(false, 0);
                        actionableEmptyView.title.setText(R.string.reader_empty_followed_blogs_title);
                        actionableEmptyView.subtitle.setVisibility(View.VISIBLE);
                        actionableEmptyView.button.setVisibility(View.VISIBLE);
                        actionableEmptyView.image.setVisibility(View.VISIBLE);
                    }
                    break;
            }
        } else {
            actionableEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRecyclerView.setAdapter(getBlogAdapter());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
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
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setQueryHint(getString(R.string.reader_hint_search_followed_sites));

        searchMenu.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
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
            mAdapter = new ReaderBlogAdapter(getActivity(), getBlogType(), mSearchFilter);
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
