package org.wordpress.android.ui.themes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.store.ThemeStore;
import org.wordpress.android.ui.plans.PlansConstants;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper.RefreshListener;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.wordpress.android.widgets.HeaderGridView;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import static org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper;

/**
 * A fragment display the themes on a grid view.
 */
public class ThemeBrowserFragment extends Fragment
        implements RecyclerListener, SearchView.OnQueryTextListener, MenuItemCompat.OnActionExpandListener {

    public static final String TAG = ThemeBrowserFragment.class.getName();

    public static ThemeBrowserFragment newInstance(SiteModel site) {
        ThemeBrowserFragment fragment = new ThemeBrowserFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(WordPress.SITE, site);
        fragment.setArguments(bundle);
        return fragment;
    }

    interface ThemeBrowserFragmentCallback {
        void onActivateSelected(String themeId);
        void onTryAndCustomizeSelected(String themeId);
        void onViewSelected(String themeId);
        void onDetailsSelected(String themeId);
        void onSupportSelected(String themeId);
        void onSwipeToRefresh();
    }

    protected SwipeToRefreshHelper mSwipeToRefreshHelper;
    private String mCurrentThemeId;

    private HeaderGridView mGridView;
    private RelativeLayout mEmptyView;
    private TextView mNoResultText;
    private TextView mCurrentThemeTextView;

    private ThemeBrowserAdapter mAdapter;
    private boolean mShouldRefreshOnStart;
    protected TextView mEmptyTextView;
    private SiteModel mSite;

    private String mLastSearch = "";
    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;

    ThemeBrowserFragmentCallback mCallback;

    @Inject ThemeStore mThemeStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
        if (mSite == null) {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found, ToastUtils.Duration.SHORT);
            getActivity().finish();
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (ThemeBrowserFragmentCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ThemeBrowserFragmentCallback");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.theme_browser_fragment, container, false);

        setRetainInstance(true);
        mNoResultText = view.findViewById(R.id.theme_no_search_result_text);
        mEmptyTextView = view.findViewById(R.id.text_empty);
        mEmptyView = view.findViewById(R.id.empty_view);

        configureGridView(inflater, view);
        configureSwipeToRefresh(view);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getAdapter().setThemeList(fetchThemes());
        setEmptyViewVisible(getAdapter().getCount() == 0);
        mGridView.setAdapter(getAdapter());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.theme_search, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.removeItem(R.id.menu_search);

        mSearchMenuItem = menu.findItem(R.id.menu_theme_search);
        mSearchMenuItem.expandActionView();
        MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, this);

        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchMenuItem);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setQuery(mLastSearch, true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_theme_search) {
            AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.THEMES_ACCESSED_SEARCH, mSite);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return item.getItemId() == R.id.menu_theme_search;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
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

    private void search(String searchTerm) {
        mLastSearch = searchTerm;
        refreshView();
    }

    @Override
    public void onMovedToScrapHeap(View view) {
        // cancel image fetch requests if the view has been moved to recycler.
        WPNetworkImageView niv = view.findViewById(R.id.theme_grid_item_image);
        if (niv != null) {
            // this tag is set in the ThemeBrowserAdapter class
            String requestUrl = (String) niv.getTag();
            if (requestUrl != null) {
                // need a listener to cancel request, even if the listener does nothing
                ImageContainer container = WordPress.sImageLoader.get(requestUrl, new ImageListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }

                    @Override
                    public void onResponse(ImageContainer response, boolean isImmediate) {
                    }
                });
                container.cancelRequest();
            }
        }
    }

    public TextView getCurrentThemeTextView() {
        return mCurrentThemeTextView;
    }

    public void setCurrentThemeId(String currentThemeId) {
        mCurrentThemeId = currentThemeId;
        refreshView();
    }

    protected void addHeaderViews(LayoutInflater inflater) {
        addMainHeader(inflater);
    }

    protected void configureSwipeToRefresh(View view) {
        mSwipeToRefreshHelper = buildSwipeToRefreshHelper(
                (CustomSwipeRefreshLayout) view.findViewById(R.id.ptr_layout),
                new RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        if (!isAdded()) {
                            return;
                        }
                        if (!NetworkUtils.checkConnection(getActivity())) {
                            mSwipeToRefreshHelper.setRefreshing(false);
                            mEmptyTextView.setText(R.string.no_network_title);
                            return;
                        }
                        setRefreshing(true);
                        mCallback.onSwipeToRefresh();
                    }
                }
        );
        mSwipeToRefreshHelper.setRefreshing(mShouldRefreshOnStart);
    }

    private void configureGridView(LayoutInflater inflater, View view) {
        mGridView = view.findViewById(R.id.theme_listview);
        addHeaderViews(inflater);
        mGridView.setRecyclerListener(this);
    }

    private void addMainHeader(LayoutInflater inflater) {
        @SuppressLint("InflateParams")
        View header = inflater.inflate(R.layout.theme_grid_cardview_header, null);
        mCurrentThemeTextView = header.findViewById(R.id.header_theme_text);

        setThemeNameIfAlreadyAvailable();
        LinearLayout customize = header.findViewById(R.id.customize);
        customize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onTryAndCustomizeSelected(mCurrentThemeId);
            }
        });

        LinearLayout details = header.findViewById(R.id.details);
        details.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onDetailsSelected(mCurrentThemeId);
            }
        });

        LinearLayout support = header.findViewById(R.id.support);
        support.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onSupportSelected(mCurrentThemeId);
            }
        });

        mGridView.addHeaderView(header);
    }

    private void setThemeNameIfAlreadyAvailable() {
        ThemeModel currentTheme = mThemeStore.getActiveThemeForSite(mSite);
        if (currentTheme != null) {
            mCurrentThemeTextView.setText(currentTheme.getName());
        }
    }

    public void setRefreshing(boolean refreshing) {
        mShouldRefreshOnStart = refreshing;
        if (mSwipeToRefreshHelper != null) {
            mSwipeToRefreshHelper.setRefreshing(refreshing);
            if (!refreshing) {
                refreshView();
            }
        }
    }

    protected void setEmptyViewVisible(boolean visible) {
        if (!isAdded() || getView() == null) {
            return;
        }
        mEmptyView.setVisibility(visible ? RelativeLayout.VISIBLE : RelativeLayout.GONE);
        mGridView.setVisibility(visible ? View.GONE : View.VISIBLE);
        if (visible && !NetworkUtils.isNetworkAvailable(getActivity())) {
            mEmptyTextView.setText(R.string.no_network_title);
        }
    }

    protected List<ThemeModel> fetchThemes() {
        if (mSite == null) {
            return new ArrayList<>();
        }

        if (mSite.isWPCom()) {
            return getSortedWpComThemes();
        }

        // this is a Jetpack site, show two sections with headers
        return getSortedJetpackThemes();
    }

    private ThemeBrowserAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new ThemeBrowserAdapter(getActivity(), mSite.isWPCom(), mCallback);
        }
        return mAdapter;
    }

    protected void refreshView() {
        if (mNoResultText.isShown()) {
            mNoResultText.setVisibility(View.GONE);
        }
        getAdapter().setThemeList(fetchThemes());
        setEmptyViewVisible(getAdapter().getCount() == 0);
    }

    private List<ThemeModel> getSortedWpComThemes() {
        List<ThemeModel> wpComThemes = mThemeStore.getWpComThemes();

        // first thing to do is attempt to find the active theme and move it to the front of the list
        moveActiveThemeToFront(wpComThemes);

        // then remove all premium themes from the list with an exception for the active theme
        if (!shouldShowPremiumThemes()) {
            removeNonActivePremiumThemes(wpComThemes);
        }

        return wpComThemes;
    }

    private List<ThemeModel> getSortedJetpackThemes() {
        List<ThemeModel> wpComThemes = mThemeStore.getWpComThemes();
        List<ThemeModel> uploadedThemes = mThemeStore.getThemesForSite(mSite);

        // put the active theme at the top of the uploaded themes list
        moveActiveThemeToFront(uploadedThemes);

        // remove all premium themes from the WP.com themes list
        removeNonActivePremiumThemes(wpComThemes);

        // remove uploaded themes from WP.com themes list (including active theme)
        removeDuplicateThemes(wpComThemes, uploadedThemes);

        List<ThemeModel> allThemes = new ArrayList<>();
        allThemes.addAll(uploadedThemes);
        allThemes.addAll(wpComThemes);
        return allThemes;
    }

    protected void moveActiveThemeToFront(final List<ThemeModel> themes) {
        if (themes == null || themes.isEmpty() || TextUtils.isEmpty(mCurrentThemeId)) {
            return;
        }

        // find the index of the active theme
        int activeThemeIndex = 0;
        for (ThemeModel theme : themes) {
            if (mCurrentThemeId.equals(theme.getThemeId())) {
                theme.setActive(true);
                activeThemeIndex = themes.indexOf(theme);
                break;
            }
        }

        // move active theme to front of list
        if (activeThemeIndex > 0) {
            themes.add(0, themes.remove(activeThemeIndex));
        }
    }

    protected void removeNonActivePremiumThemes(final List<ThemeModel> themes) {
        if (themes == null || themes.isEmpty()) {
            return;
        }

        Iterator<ThemeModel> iterator = themes.iterator();
        while (iterator.hasNext()) {
            ThemeModel theme = iterator.next();
            if (!theme.isFree() && !theme.getActive()) {
                iterator.remove();
            }
        }
    }

    private void removeDuplicateThemes(final List<ThemeModel> wpComThemes, final List<ThemeModel> uploadedThemes) {
        if (wpComThemes == null || wpComThemes.isEmpty() || uploadedThemes == null || uploadedThemes.isEmpty()) {
            return;
        }

        for (ThemeModel uploadedTheme : uploadedThemes) {
            Iterator<ThemeModel> wpComIterator = wpComThemes.iterator();
            while (wpComIterator.hasNext()) {
                ThemeModel wpComTheme = wpComIterator.next();
                if (StringUtils.equals(wpComTheme.getThemeId(), uploadedTheme.getThemeId().replace("-wpcom", ""))) {
                    wpComIterator.remove();
                    break;
                }
            }
        }
    }

    protected boolean shouldShowPremiumThemes() {
        if (mSite == null) {
            return false;
        }
        long planId = mSite.getPlanId();
        return planId == PlansConstants.PREMIUM_PLAN_ID
                || planId == PlansConstants.BUSINESS_PLAN_ID
                || planId == PlansConstants.JETPACK_PREMIUM_PLAN_ID
                || planId == PlansConstants.JETPACK_BUSINESS_PLAN_ID;
    }
}
