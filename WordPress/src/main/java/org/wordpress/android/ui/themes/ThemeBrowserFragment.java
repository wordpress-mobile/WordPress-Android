package org.wordpress.android.ui.themes;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.store.ThemeStore;
import org.wordpress.android.ui.plans.PlansConstants;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper.RefreshListener;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.wordpress.android.widgets.HeaderGridView;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import static org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper;

/**
 * A fragment display the themes on a grid view.
 */
public class ThemeBrowserFragment extends Fragment implements RecyclerListener {
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
        void onSearchClicked();
    }

    protected SwipeToRefreshHelper mSwipeToRefreshHelper;
    protected ThemeBrowserActivity mThemeBrowserActivity;
    private String mCurrentThemeId;
    private HeaderGridView mGridView;
    private RelativeLayout mEmptyView;
    private TextView mNoResultText;
    private TextView mCurrentThemeTextView;
    private ThemeBrowserAdapter mAdapter;
    private ThemeBrowserFragmentCallback mCallback;
    private boolean mShouldRefreshOnStart;
    private TextView mEmptyTextView;
    private SiteModel mSite;

    @Inject ThemeStore mThemeStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (savedInstanceState == null) {
            mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }

        if (mSite == null) {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found, ToastUtils.Duration.SHORT);
            getActivity().finish();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mCallback = (ThemeBrowserFragmentCallback) context;
            mThemeBrowserActivity = (ThemeBrowserActivity) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement ThemeBrowserFragmentCallback");
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
        mNoResultText = (TextView) view.findViewById(R.id.theme_no_search_result_text);
        mEmptyTextView = (TextView) view.findViewById(R.id.text_empty);
        mEmptyView = (RelativeLayout) view.findViewById(R.id.empty_view);

        configureGridView(inflater, view);
        configureSwipeToRefresh(view);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (this instanceof ThemeSearchFragment) {
            mThemeBrowserActivity.setThemeSearchFragment((ThemeSearchFragment) this);
        } else {
            mThemeBrowserActivity.setThemeBrowserFragment(this);
        }

        Cursor cursor = fetchThemes();
        if (cursor == null) {
            return;
        }

        mAdapter = new ThemeBrowserAdapter(mThemeBrowserActivity, cursor, false, mCallback);
        setEmptyViewVisible(mAdapter.getCount() == 0);
        mGridView.setAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        mThemeBrowserActivity.fetchCurrentTheme();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
    }

    @Override
    public void onMovedToScrapHeap(View view) {
        // cancel image fetch requests if the view has been moved to recycler.
        WPNetworkImageView niv = (WPNetworkImageView) view.findViewById(R.id.theme_grid_item_image);
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
        configureAndAddSearchHeader(inflater);
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
                        if (!NetworkUtils.checkConnection(mThemeBrowserActivity)) {
                            mSwipeToRefreshHelper.setRefreshing(false);
                            mEmptyTextView.setText(R.string.no_network_title);
                            return;
                        }
                        setRefreshing(true);
                        mThemeBrowserActivity.fetchInstalledThemesIfJetpackSite();
                        mThemeBrowserActivity.fetchWpComThemesIfSyncTimedOut(true);
                    }
                }
        );
        mSwipeToRefreshHelper.setRefreshing(mShouldRefreshOnStart);
    }

    private void configureGridView(LayoutInflater inflater, View view) {
        mGridView = (HeaderGridView) view.findViewById(R.id.theme_listview);
        addHeaderViews(inflater);
        mGridView.setRecyclerListener(this);
    }

    private void addMainHeader(LayoutInflater inflater) {
        @SuppressLint("InflateParams")
        View header = inflater.inflate(R.layout.theme_grid_cardview_header, null);
        mCurrentThemeTextView = (TextView) header.findViewById(R.id.header_theme_text);

        setThemeNameIfAlreadyAvailable();
        LinearLayout customize = (LinearLayout) header.findViewById(R.id.customize);
        customize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onTryAndCustomizeSelected(mCurrentThemeId);
            }
        });

        LinearLayout details = (LinearLayout) header.findViewById(R.id.details);
        details.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onDetailsSelected(mCurrentThemeId);
            }
        });

        LinearLayout support = (LinearLayout) header.findViewById(R.id.support);
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

    private void configureAndAddSearchHeader(LayoutInflater inflater) {
        @SuppressLint("InflateParams")
        View headerSearch = inflater.inflate(R.layout.theme_grid_cardview_header_search, null);
        headerSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onSearchClicked();
            }
        });
        mGridView.addHeaderView(headerSearch);
        ImageButton searchButton = (ImageButton) headerSearch.findViewById(R.id.theme_search);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onSearchClicked();
            }
        });
    }

    private void setEmptyViewVisible(boolean visible) {
        if (!isAdded() || getView() == null) {
            return;
        }
        mEmptyView.setVisibility(visible ? RelativeLayout.VISIBLE : RelativeLayout.GONE);
        mGridView.setVisibility(visible ? View.GONE : View.VISIBLE);
        if (visible && !NetworkUtils.isNetworkAvailable(mThemeBrowserActivity)) {
            mEmptyTextView.setText(R.string.no_network_title);
        }
    }

    protected Cursor fetchThemes() {
        if (mSite == null) {
            return null;
        }

        if (mSite.isWPCom()) {
            return getSortedWpComThemesCursor();
        }

        // this is a Jetpack site, show two sections with headers
        return getSortedJetpackCursor();
    }

    protected void refreshView() {
        Cursor cursor = fetchThemes();
        if (cursor == null) {
            return;
        }
        if (mAdapter == null) {
            mAdapter = new ThemeBrowserAdapter(mThemeBrowserActivity, cursor, false, mCallback);
        }
        if (mNoResultText.isShown()) {
            mNoResultText.setVisibility(View.GONE);
        }
        mAdapter.changeCursor(cursor);
        mAdapter.notifyDataSetChanged();
        setEmptyViewVisible(mAdapter.getCount() == 0);
    }

    private Cursor getSortedWpComThemesCursor() {
        final List<ThemeModel> wpComThemes = mThemeStore.getWpComThemes();

        // first thing to do is attempt to find the active theme and move it to the front of the list
        moveActiveThemeToFront(wpComThemes);

        // then remove all premium themes from the list with an exception for the active theme
        if (!shouldShowPremiumThemes()) {
            removeNonActivePremiumThemes(wpComThemes);
        }

        // lastly convert the list into a Cursor for the adapter
        return createCursorForThemesList(wpComThemes);
    }

    private Cursor getSortedJetpackCursor() {
        final List<ThemeModel> wpComThemes = mThemeStore.getWpComThemes();
        final List<ThemeModel> uploadedThemes = mThemeStore.getThemesForSite(mSite);

        // put the active theme at the top of the uploaded themes list
        moveActiveThemeToFront(uploadedThemes);

        // remove all premium themes from the WP.com themes list
        removeNonActivePremiumThemes(wpComThemes);

        // remove uploaded themes from WP.com themes list (including active theme)
        removeDuplicateThemes(wpComThemes, uploadedThemes);

        // 1. Uploaded header
        // 2. Uploaded themes
        // 3. WP.com header
        // 4. WP.com themes
        final Cursor[] cursors = new Cursor[4];
        final Cursor uploadedThemesCursor = createCursorForThemesList(uploadedThemes);
        final Cursor wpComThemesCursor = createCursorForThemesList(wpComThemes);

        cursors[0] = ThemeBrowserAdapter.createHeaderCursor(
                getString(R.string.uploaded_themes_header), uploadedThemesCursor.getCount());
        cursors[1] = uploadedThemesCursor;
        cursors[2] = ThemeBrowserAdapter.createHeaderCursor(
                getString(R.string.wpcom_themes_header), wpComThemesCursor.getCount());
        cursors[3] = wpComThemesCursor;

        return new MergeCursor(cursors);
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
            if (theme.getPrice() > 0.f && !theme.getActive()) {
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

    protected Cursor createCursorForThemesList(List<ThemeModel> themes) {
        final MatrixCursor cursor = new MatrixCursor(ThemeBrowserAdapter.THEME_COLUMNS);
        for (ThemeModel theme : themes) {
            cursor.addRow(ThemeBrowserAdapter.createThemeCursorRow(theme));
        }
        return cursor;
    }
}
