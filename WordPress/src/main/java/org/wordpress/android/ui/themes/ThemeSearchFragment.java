package org.wordpress.android.ui.themes;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.NetworkUtils;

/**
 * A fragment for display the results of a theme search
 */
public class ThemeSearchFragment extends ThemeBrowserFragment implements SearchView.OnQueryTextListener,
        MenuItemCompat.OnActionExpandListener {
    public static final String TAG = ThemeSearchFragment.class.getName();
    private static final String BUNDLE_LAST_SEARCH = "BUNDLE_LAST_SEARCH";
    public static final int SEARCH_VIEW_MAX_WIDTH = 10000;

    public static ThemeSearchFragment newInstance() {
        return new ThemeSearchFragment();
    }

    private String mLastSearch = "";
    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            mLastSearch = savedInstanceState.getString(BUNDLE_LAST_SEARCH);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState(outState);
    }

    private void saveState(Bundle outState) {
        outState.putString(BUNDLE_LAST_SEARCH, mLastSearch);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.removeItem(R.id.menu_search);

        mSearchMenuItem = menu.findItem(R.id.menu_theme_search);
        mSearchMenuItem.expandActionView();
        MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, this);

        configureSearchView();
    }

    public void configureSearchView() {
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchMenuItem);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setQuery(mLastSearch, true);
        mSearchView.setMaxWidth(SEARCH_VIEW_MAX_WIDTH);
    }

    private void clearFocus(View view) {
        if (view != null) {
            view.clearFocus();
        }
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        if (item.getItemId() == R.id.menu_theme_search) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        mThemeBrowserActivity.setIsInSearchMode(false);
        mThemeBrowserActivity.showToolbar();
        mThemeBrowserActivity.getFragmentManager().popBackStack();
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (!mLastSearch.equals(query)) {
            mLastSearch = query;
            search(query);
        }
        clearFocus(mSearchView);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (!mLastSearch.equals(newText) && !newText.equals("")) {
            mLastSearch = newText;
            search(newText);
        }
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.theme_search, menu);
    }

    @Override
    protected void addHeaderViews(LayoutInflater inflater) {
        // No header on Search
    }

    @Override
    protected void configureSwipeToRefresh(View view) {
        super.configureSwipeToRefresh(view);
        mSwipeToRefreshHelper.setEnabled(false);
    }

    @Override
    public void setRefreshing(boolean refreshing) {
        refreshView(getSpinnerPosition());
    }

    @Override
    protected Cursor fetchThemes(int position) {
        if (WordPress.getCurrentBlog() == null) {
            return null;
        }

        String blogId = String.valueOf(WordPress.getCurrentBlog().getRemoteBlogId());

        return WordPress.wpDB.getThemes(blogId, mLastSearch);
    }

    public void search(String searchTerm) {
        mLastSearch = searchTerm;

        if (NetworkUtils.isNetworkAvailable(mThemeBrowserActivity)) {
            mThemeBrowserActivity.searchThemes(searchTerm);
        } else {
            refreshView(getSpinnerPosition());
        }
    }
}
