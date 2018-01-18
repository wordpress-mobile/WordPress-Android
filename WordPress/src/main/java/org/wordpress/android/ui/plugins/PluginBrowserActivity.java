package org.wordpress.android.ui.plugins;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType;
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPNetworkImageView.ImageType;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class PluginBrowserActivity extends AppCompatActivity
        implements SearchView.OnQueryTextListener,
        MenuItem.OnActionExpandListener,
        PluginListFragment.PluginListFragmentListener {

    private static final String KEY_LAST_SEARCH = "last_search";

    public enum PluginListType {
        SITE,
        POPULAR,
        NEW;

        @StringRes int getTitleRes() {
            switch (this) {
                case POPULAR:
                    return R.string.plugin_caption_popular;
                case NEW:
                    return R.string.plugin_caption_new;
                default:
                    return R.string.plugin_caption_installed;
            }
        }
    }

    private SiteModel mSite;
    private final List<SitePluginModel> mSitePlugins = new ArrayList<>();

    private RecyclerView mSitePluginsRecycler;
    private RecyclerView mPopularPluginsRecycler;
    private RecyclerView mNewPluginsRecycler;

    private int mRowWidth;
    private int mRowHeight;
    private int mIconSize;

    private String mLastSearch;
    private MenuItem mSearchMenuItem;
    private SearchView mSearchView;

    @Inject PluginStore mPluginStore;
    @Inject Dispatcher mDispatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.plugin_browser_activity);
        mDispatcher.register(this);

        mSitePluginsRecycler = findViewById(R.id.installed_plugins_recycler);
        mPopularPluginsRecycler = findViewById(R.id.popular_plugins_recycler);
        mNewPluginsRecycler = findViewById(R.id.new_plugins_recycler);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setElevation(0);
        }

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mLastSearch = savedInstanceState.getString(KEY_LAST_SEARCH);
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        // site plugin list
        findViewById(R.id.text_manage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showListFragment(PluginListType.SITE);
            }
        });

        // popular plugin list
        findViewById(R.id.text_all_popular).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showListFragment(PluginListType.POPULAR);
            }
        });

        // new plugin list
        findViewById(R.id.text_all_new).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showListFragment(PluginListType.NEW);
            }
        });

        boolean isLandscape = DisplayUtils.isLandscape(this);
        int displayWidth = DisplayUtils.getDisplayPixelWidth(this);
        int displayHeight = DisplayUtils.getDisplayPixelHeight(this);
        int maxRows = isLandscape ? 6 : 4;
        mRowWidth = Math.round(displayWidth / (maxRows - 0.4f));
        mIconSize = mRowWidth;
        mRowHeight = Math.round(displayHeight / 3.4f); // TODO: landscape

        configureRecycler(mSitePluginsRecycler);
        configureRecycler(mPopularPluginsRecycler);
        configureRecycler(mNewPluginsRecycler);

        mSitePlugins.addAll(mPluginStore.getSitePlugins(mSite));
        refreshAllPlugins();

        if (savedInstanceState == null) {
            fetchAllPlugins();
        }
    }

    private void configureRecycler(@NonNull RecyclerView recycler) {
        recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recycler.setHasFixedSize(true);
        recycler.setAdapter(new PluginBrowserAdapter(this));
    }

    @Override
    protected void onDestroy() {
        mDispatcher.unregister(this);
        if (mSearchView != null) {
            mSearchView.setOnQueryTextListener(null);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        setTitle(R.string.plugins);
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);

        mSearchMenuItem = menu.findItem(R.id.menu_search);
        mSearchMenuItem.setOnActionExpandListener(this);
        mSearchView = (SearchView) mSearchMenuItem.getActionView();
        mSearchView.setOnQueryTextListener(this);

        if (!TextUtils.isEmpty(mLastSearch)) {
            mSearchMenuItem.expandActionView();
            onQueryTextSubmit(mLastSearch);
            mSearchView.setQuery(mLastSearch, true);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
        if (mSearchMenuItem != null && mSearchMenuItem.isActionViewExpanded()) {
            outState.putString(KEY_LAST_SEARCH, mSearchView.getQuery().toString());
        }
    }

    private void fetchAllPlugins() {
        for (PluginListType pluginType: PluginListType.values()) {
            fetchPlugins(pluginType);
        }
    }

    private void fetchPlugins(@NonNull PluginListType pluginType) {
        switch (pluginType) {
            case SITE:
                if (mPluginStore.getSitePlugins(mSite).size() == 0) {
                    showProgress(true);
                }
                mDispatcher.dispatch(PluginActionBuilder.newFetchSitePluginsAction(mSite));
                break;
            case POPULAR:
                PluginStore.FetchPluginDirectoryPayload popularPayload =
                        new PluginStore.FetchPluginDirectoryPayload(PluginDirectoryType.POPULAR, false);
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(popularPayload));
                break;
            case NEW:
                PluginStore.FetchPluginDirectoryPayload newPayload =
                        new PluginStore.FetchPluginDirectoryPayload(PluginDirectoryType.NEW, false);
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(newPayload));
                break;
        }
    }

    private void refreshAllPlugins() {
        for (PluginListType pluginType: PluginListType.values()) {
            refreshPlugins(pluginType);
        }
    }

    private void refreshPlugins(@NonNull PluginListType pluginType) {
        PluginBrowserAdapter adapter;
        View cardView;
        switch (pluginType) {
            case POPULAR:
                adapter = (PluginBrowserAdapter) mPopularPluginsRecycler.getAdapter();
                cardView = findViewById(R.id.popular_plugins_cardview);
                break;
            case NEW:
                adapter = (PluginBrowserAdapter) mNewPluginsRecycler.getAdapter();
                cardView = findViewById(R.id.new_plugins_cardview);
                break;
            default:
                adapter = (PluginBrowserAdapter) mSitePluginsRecycler.getAdapter();
                cardView = findViewById(R.id.installed_plugins_cardview);
                break;
        }

        List<?> plugins = getPlugins(pluginType);
        adapter.setPlugins(plugins);

        int newVisibility = plugins.size() > 0 ? View.VISIBLE : View.GONE;
        int oldVisibility = cardView.getVisibility();
        if (newVisibility == View.VISIBLE && oldVisibility != View.VISIBLE) {
            AniUtils.fadeIn(cardView, AniUtils.Duration.SHORT);
        } else if (newVisibility != View.VISIBLE && oldVisibility == View.VISIBLE) {
            AniUtils.fadeOut(cardView, AniUtils.Duration.SHORT);
        }
    }

    private List<?> getPlugins(@NonNull PluginListType pluginType) {
        switch (pluginType) {
            case POPULAR:
                return mPluginStore.getPluginDirectory(PluginDirectoryType.POPULAR);
            case NEW:
                return mPluginStore.getPluginDirectory(PluginDirectoryType.NEW);
            default:
                return mPluginStore.getSitePlugins(mSite);
        }
    }

    private SitePluginModel getSitePluginFromSlug(@Nullable String slug) {
        if (slug != null) {
            for (SitePluginModel plugin : mSitePlugins) {
                if (slug.equals(plugin.getSlug())) {
                    return plugin;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSitePluginsFetched(PluginStore.OnSitePluginsFetched event) {
        if (isFinishing()) {
            return;
        }
        showProgress(false);
        if (event.isError()) {
            ToastUtils.showToast(this, R.string.plugin_fetch_error);
            return;
        }
        refreshPlugins(PluginListType.SITE);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWPOrgPluginFetched(PluginStore.OnWPOrgPluginFetched event) {
        if (isFinishing()) {
            return;
        }
        if (event.isError()) {
            AppLog.e(AppLog.T.API, "An error occurred while fetching the wporg plugin with type: " + event.error.type);
            return;
        }
        if (!TextUtils.isEmpty(event.pluginSlug)) {
            refreshPluginWithSlug(event.pluginSlug);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPluginDirectoryFetched(PluginStore.OnPluginDirectoryFetched event) {
        if (isFinishing()) {
            return;
        }
        if (event.isError()) {
            AppLog.e(AppLog.T.API, "An error occurred while fetching the plugin directory: " + event.type);
            return;
        }
        switch (event.type) {
            case POPULAR:
                refreshPlugins(PluginListType.POPULAR);
                break;
            case NEW:
                refreshPlugins(PluginListType.NEW);
                break;
        }
    }

    private void refreshPluginWithSlug(@NonNull String slug) {
        ((PluginBrowserAdapter) mSitePluginsRecycler.getAdapter()).refreshPluginWithSlug(slug);
        ((PluginBrowserAdapter) mPopularPluginsRecycler.getAdapter()).refreshPluginWithSlug(slug);
        ((PluginBrowserAdapter) mNewPluginsRecycler.getAdapter()).refreshPluginWithSlug(slug);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (mSearchView != null) {
            mSearchView.clearFocus();
        }
        submitSearch(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        submitSearch(query);
        return true;
    }

    private boolean hasListFragment() {
        return getFragmentManager().findFragmentByTag(PluginListFragment.TAG) != null;
    }

    private void showListFragment(@NonNull PluginListType listType) {
        PluginListFragment listFragment;

        Fragment fragment = getFragmentManager().findFragmentByTag(PluginListFragment.TAG);
        if (fragment != null) {
            listFragment = (PluginListFragment) fragment;
            listFragment.setListType(listType);
        } else {
            listFragment = PluginListFragment.newInstance(mSite, listType);
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, listFragment, PluginListFragment.TAG)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void showListFragmentForQuery(@Nullable String query) {
        // TODO: search will be performed in a subsequent PR
    }

    private void submitSearch(String query) {
        showListFragmentForQuery(query);
    }

    private void hideListFragment() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        }
        setTitle(R.string.plugins);
    }

    @Override
    public List<?> onListFragmentRequestPlugins(@NonNull PluginListType listType) {
        setTitle(listType.getTitleRes());
        return getPlugins(listType);
    }

    private void showProgress(boolean show) {
        findViewById(R.id.progress).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem menuItem) {
        showListFragmentForQuery(null);
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem menuItem) {
        hideListFragment();
        return true;
    }

    private class PluginBrowserAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final PluginList mItems = new PluginList();
        private final LayoutInflater mLayoutInflater;

        PluginBrowserAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
            setHasStableIds(true);
        }

        void setPlugins(@NonNull List<?> items) {
            if (mItems.isSameList(items)) return;

            mItems.clear();
            mItems.addAll(items);

            // strip HTML here so we don't have to do it in every call to onBindViewHolder
            for (Object item: mItems) {
                if (item instanceof WPOrgPluginModel) {
                    WPOrgPluginModel plugin = (WPOrgPluginModel) item;
                    plugin.setAuthorAsHtml(HtmlUtils.fastStripHtml(plugin.getAuthorAsHtml()));
                }
            }

            notifyDataSetChanged();
        }

        private Object getItem(int position) {
            if (position < mItems.size()) {
                return mItems.get(position);
            }
            return null;
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        @Override
        public long getItemId(int position) {
            return mItems.getItemId(position);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.plugin_browser_row, parent, false);
            return new PluginBrowserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            PluginBrowserViewHolder holder = (PluginBrowserViewHolder) viewHolder;
            Object item = getItem(position);
            if (item == null) return;

            SitePluginModel sitePlugin;
            WPOrgPluginModel wpOrgPlugin;
            String name;
            String author;
            if (item instanceof SitePluginModel) {
                sitePlugin = (SitePluginModel) item;
                wpOrgPlugin = PluginUtils.getWPOrgPlugin(mPluginStore, sitePlugin);
                if (wpOrgPlugin == null) {
                    mDispatcher.dispatch(PluginActionBuilder.newFetchWporgPluginAction(sitePlugin.getSlug()));
                }
                name = sitePlugin.getName();
                author = sitePlugin.getAuthorName();
            } else {
                wpOrgPlugin = (WPOrgPluginModel) item;
                sitePlugin = getSitePluginFromSlug(wpOrgPlugin.getSlug());
                name = wpOrgPlugin.getName();
                author = wpOrgPlugin.getAuthorAsHtml();
            }

            String iconUrl = wpOrgPlugin != null ? wpOrgPlugin.getIcon() : null;

            holder.nameText.setText(name);
            holder.authorText.setText(author);
            holder.icon.setImageUrl(iconUrl, ImageType.PLUGIN_ICON);

            if (sitePlugin != null) {
                @StringRes int textResId;
                @ColorRes int colorResId;
                @DrawableRes int drawableResId;
                if (PluginUtils.isUpdateAvailable(sitePlugin, wpOrgPlugin)) {
                    textResId = R.string.plugin_needs_update;
                    colorResId = R.color.alert_yellow;
                    drawableResId = R.drawable.plugin_update_available_icon;
                } else if (sitePlugin.isActive()) {
                    textResId = R.string.plugin_active;
                    colorResId = R.color.alert_green;
                    drawableResId = R.drawable.ic_checkmark_green_24dp;
                } else {
                    textResId = R.string.plugin_inactive;
                    colorResId = R.color.grey;
                    drawableResId = R.drawable.ic_cross_grey_600_24dp;
                }
                holder.statusText.setText(textResId);
                holder.statusText.setTextColor(getResources().getColor(colorResId));
                holder.statusIcon.setImageResource(drawableResId);
                holder.statusContainer.setVisibility(View.VISIBLE);
                holder.ratingBar.setVisibility(View.GONE);
            } else {
                holder.statusContainer.setVisibility(View.GONE);
                holder.ratingBar.setVisibility(View.VISIBLE);
                holder.ratingBar.setRating(PluginUtils.getAverageStarRating(wpOrgPlugin));
            }
        }

        private void refreshPluginWithSlug(@NonNull String slug) {
            int index = mItems.indexOfPluginWithSlug(slug);
            if (index != -1) {
                notifyItemChanged(index);
            }
        }

        private class PluginBrowserViewHolder extends ViewHolder {
            final TextView nameText;
            final TextView authorText;
            final ViewGroup statusContainer;
            final TextView statusText;
            final ImageView statusIcon;
            final WPNetworkImageView icon;
            final RatingBar ratingBar;

            PluginBrowserViewHolder(View view) {
                super(view);
                nameText = view.findViewById(R.id.plugin_name);
                authorText = view.findViewById(R.id.plugin_author);
                icon = view.findViewById(R.id.plugin_icon);
                ratingBar = view.findViewById(R.id.rating_bar);

                statusContainer = view.findViewById(R.id.plugin_status_container);
                statusText = statusContainer.findViewById(R.id.plugin_status_text);
                statusIcon = statusContainer.findViewById(R.id.plugin_status_icon);

                view.getLayoutParams().width = mRowWidth;
                view.getLayoutParams().height = mRowHeight;

                icon.getLayoutParams().width = mIconSize;
                icon.getLayoutParams().height = mIconSize;

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getAdapterPosition();
                        Object item = getItem(position);
                        SitePluginModel sitePlugin;
                        if (item instanceof SitePluginModel) {
                            sitePlugin = (SitePluginModel) item;
                        } else {
                            WPOrgPluginModel wpOrgPlugin = (WPOrgPluginModel) item;
                            sitePlugin = getSitePluginFromSlug(wpOrgPlugin.getSlug());
                        }
                        // TODO: show detail for WPOrgPlugin
                        if (sitePlugin != null) {
                            ActivityLauncher.viewPluginDetail(PluginBrowserActivity.this, mSite, sitePlugin);
                        }
                    }
                });
            }
        }
    }

}
