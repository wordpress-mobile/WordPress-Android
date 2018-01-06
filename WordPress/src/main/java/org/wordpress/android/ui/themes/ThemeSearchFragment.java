package org.wordpress.android.ui.themes;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment for display the results of a theme search
 */
public class ThemeSearchFragment extends ThemeBrowserFragment implements SearchView.OnQueryTextListener,
        MenuItemCompat.OnActionExpandListener {
    public static final String TAG = ThemeSearchFragment.class.getName();
    private static final String BUNDLE_LAST_SEARCH = "BUNDLE_LAST_SEARCH";
    private static final int SEARCH_VIEW_MAX_WIDTH = 10000;

    private String mLastSearch = "";
    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;

    public static ThemeSearchFragment newInstance(SiteModel site) {
        ThemeSearchFragment fragment = new ThemeSearchFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(WordPress.SITE, site);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            mLastSearch = savedInstanceState.getString(BUNDLE_LAST_SEARCH);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return item.getItemId() == R.id.menu_theme_search;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        mCallback.onSearchClosed();
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (!mLastSearch.equals(query)) {
            search(query);
        }
        if (mSearchView != null) {
            mSearchView.clearFocus();
        }
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (!mLastSearch.equals(newText)) {
            search(newText);
        }
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.theme_search, menu);
    }

    @Override
    public void setRefreshing(boolean refreshing) {
        refreshView();
    }

    @Override
    protected void configureSwipeToRefresh(View view) {
        super.configureSwipeToRefresh(view);
        mSwipeToRefreshHelper.setRefreshing(false);
        mSwipeToRefreshHelper.setEnabled(false);
    }

    @Override
    protected void addHeaderViews(LayoutInflater inflater) {
        // No header on Search
    }

    @Override
    protected List<ThemeModel> fetchThemes() {
        List<ThemeModel> themes = super.fetchThemes();
        return filter(themes);
    }

    @Override
    protected void setEmptyViewVisible(boolean visible) {
        super.setEmptyViewVisible(visible);
        if (!TextUtils.isEmpty(mLastSearch)) {
            mEmptyTextView.setText(R.string.theme_no_search_result_found);
        }
    }

    private void search(String searchTerm) {
        mLastSearch = searchTerm;
        refreshView();
    }

    private void configureSearchView() {
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchMenuItem);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setQuery(mLastSearch, true);
        mSearchView.setMaxWidth(SEARCH_VIEW_MAX_WIDTH);
    }

    private List<ThemeModel> filter(@NonNull List<ThemeModel> themes) {
        if (TextUtils.isEmpty(mLastSearch)) {
            return themes;
        }
        List<ThemeModel> filteredThemes = new ArrayList<>();
        for (ThemeModel theme : themes) {
            if (theme.getName().toLowerCase().contains(mLastSearch.toLowerCase())) {
                filteredThemes.add(theme);
            }
        }
        return filteredThemes;
    }
}
