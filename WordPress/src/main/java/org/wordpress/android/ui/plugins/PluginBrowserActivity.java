package org.wordpress.android.ui.plugins;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import org.wordpress.android.fluxc.store.PluginStore.OnPluginDirectorySearched;
import org.wordpress.android.fluxc.store.PluginStore.SearchPluginDirectoryPayload;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPNetworkImageView.ImageType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

public class PluginBrowserActivity extends AppCompatActivity
        implements SearchView.OnQueryTextListener,
        MenuItem.OnActionExpandListener,
        PluginListFragment.PluginListFragmentListener {

    public enum PluginListType {
        SITE,
        POPULAR,
        NEW,
        SEARCH;

        @StringRes int getTitleRes() {
            switch (this) {
                case POPULAR:
                    return R.string.plugin_caption_popular;
                case NEW:
                    return R.string.plugin_caption_new;
                case SEARCH:
                    return R.string.plugin_caption_search;
                default:
                    return R.string.plugin_caption_installed;
            }
        }
    }

    private PluginBrowserViewModel mViewModel;

    private final Handler mHandler = new Handler();
    private RecyclerView mSitePluginsRecycler;
    private RecyclerView mPopularPluginsRecycler;
    private RecyclerView mNewPluginsRecycler;

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

        mViewModel = ViewModelProviders.of(this).get(PluginBrowserViewModel.class);

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
            mViewModel.setSite((SiteModel) getIntent().getSerializableExtra(WordPress.SITE));
        }

        if (mViewModel.getSite() == null) {
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

        configureRecycler(mSitePluginsRecycler);
        configureRecycler(mPopularPluginsRecycler);
        configureRecycler(mNewPluginsRecycler);

        mViewModel.setSitePlugins(mPluginStore.getSitePlugins(mViewModel.getSite()));
        mViewModel.setNewPlugins(mPluginStore.getPluginDirectory(PluginDirectoryType.NEW));
        mViewModel.setPopularPlugins(mPluginStore.getPluginDirectory(PluginDirectoryType.POPULAR));
        reloadAllPlugins();

        if (savedInstanceState == null) {
            fetchPlugins(PluginListType.SITE, false);
            fetchPlugins(PluginListType.POPULAR, false);
            fetchPlugins(PluginListType.NEW, false);
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
        if (mSearchMenuItem != null) {
            mSearchMenuItem.setOnActionExpandListener(null);
        }
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
        mSearchView = (SearchView) mSearchMenuItem.getActionView();

        if (!TextUtils.isEmpty(mViewModel.getSearchQuery())) {
            mSearchMenuItem.expandActionView();
            mSearchView.setQuery(mViewModel.getSearchQuery(), false);
            fetchPlugins(PluginListType.SEARCH, false);
        }

        mSearchMenuItem.setOnActionExpandListener(this);

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCodes.PLUGIN_DETAIL) {
            reloadAllPlugins();
            reloadListFragment();
        }
    }

    private void fetchPlugins(@NonNull PluginListType pluginType, boolean loadMore) {
        switch (pluginType) {
            case SITE:
                if (mPluginStore.getSitePlugins(mViewModel.getSite()).size() == 0) {
                    showProgress(true);
                }
                mDispatcher.dispatch(PluginActionBuilder.newFetchSitePluginsAction(mViewModel.getSite()));
                break;
            case POPULAR:
                PluginStore.FetchPluginDirectoryPayload popularPayload =
                        new PluginStore.FetchPluginDirectoryPayload(PluginDirectoryType.POPULAR, loadMore);
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(popularPayload));
                break;
            case NEW:
                PluginStore.FetchPluginDirectoryPayload newPayload =
                        new PluginStore.FetchPluginDirectoryPayload(PluginDirectoryType.NEW, loadMore);
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(newPayload));
                break;
            case SEARCH:
                if (mViewModel.getSearchResults().size() == 0) {
                    showProgress(true);
                }
                SearchPluginDirectoryPayload searchPayload =
                        new SearchPluginDirectoryPayload(mViewModel.getSearchQuery(), 1);
                mDispatcher.dispatch(PluginActionBuilder.newSearchPluginDirectoryAction(searchPayload));
                break;
        }
    }

    private void reloadAllPlugins() {
        for (PluginListType pluginType: PluginListType.values()) {
            reloadPlugins(pluginType);
        }
    }

    private void reloadPlugins(@NonNull PluginListType pluginType) {
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
            case SEARCH:
                return;
            default:
                adapter = (PluginBrowserAdapter) mSitePluginsRecycler.getAdapter();
                cardView = findViewById(R.id.installed_plugins_cardview);
                break;
        }

        List<?> plugins = mViewModel.getPluginsForListType(pluginType);
        adapter.setPlugins(plugins);

        // TODO: handle the visibility with a subscription

        int newVisibility = plugins.size() > 0 ? View.VISIBLE : View.GONE;
        int oldVisibility = cardView.getVisibility();
        if (newVisibility == View.VISIBLE && oldVisibility != View.VISIBLE) {
            AniUtils.fadeIn(cardView, AniUtils.Duration.MEDIUM);
        } else if (newVisibility != View.VISIBLE && oldVisibility == View.VISIBLE) {
            AniUtils.fadeOut(cardView, AniUtils.Duration.MEDIUM);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSitePluginsFetched(PluginStore.OnSitePluginsFetched event) {
        if (isFinishing()) {
            return;
        }
        showProgress(false);
        if (event.isError()) {
            AppLog.e(AppLog.T.PLUGINS, "An error occurred while fetching site plugins with type: " + event.error.type);
            ToastUtils.showToast(this, R.string.plugin_fetch_error);
            return;
        }
        mViewModel.setSitePlugins(mPluginStore.getSitePlugins(mViewModel.getSite()));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWPOrgPluginFetched(PluginStore.OnWPOrgPluginFetched event) {
        if (isFinishing()) {
            return;
        }
        if (event.isError()) {
            AppLog.e(AppLog.T.PLUGINS, "An error occurred while fetching the wporg plugin with type: " + event.error.type);
            return;
        }
        if (!TextUtils.isEmpty(event.pluginSlug)) {
            reloadPluginWithSlug(event.pluginSlug);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPluginDirectoryFetched(PluginStore.OnPluginDirectoryFetched event) {
        if (isFinishing()) {
            return;
        }

        if (event.isError()) {
            AppLog.e(AppLog.T.PLUGINS, "An error occurred while fetching the plugin directory: " + event.type);
            return;
        }

        switch (event.type) {
            case POPULAR:
                mViewModel.setPopularPlugins(mPluginStore.getPluginDirectory(PluginDirectoryType.POPULAR));
                break;
            case NEW:
                mViewModel.setNewPlugins(mPluginStore.getPluginDirectory(PluginDirectoryType.NEW));
                break;
        }

        PluginListType listType = event.type == PluginDirectoryType.POPULAR ?
                PluginListType.POPULAR : PluginListType.NEW;
        PluginListFragment fragment = getListFragment();
        if (fragment != null && fragment.getListType() == listType) {
            // TODO: save this to the view model instead
            fragment.onLoadedMore(event.canLoadMore);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPluginDirectorySearched(OnPluginDirectorySearched event) throws InterruptedException {
        if (isFinishing()) return;

        showProgress(false);

        if (event.isError()) {
            AppLog.e(AppLog.T.PLUGINS, "An error occurred while searching the plugin directory");
            ToastUtils.showToast(this, R.string.plugin_search_error);
            return;
        }

        mViewModel.setSearchResults(event.searchTerm, event.plugins);

        // make sure the search list fragment is still active and that this is the same as the most
        // recent search (could be a stale response)
        PluginListFragment fragment = getListFragment();
        if (fragment == null
                || fragment.getListType() != PluginListType.SEARCH) {
            return;
        }

        fragment.showEmptyView(mViewModel.getSearchResults().isEmpty()
                && !TextUtils.isEmpty(mViewModel.getSearchQuery()));
        reloadListFragment();
    }

    private void reloadPluginWithSlug(@NonNull String slug) {
        ((PluginBrowserAdapter) mSitePluginsRecycler.getAdapter()).reloadPluginWithSlug(slug);
        ((PluginBrowserAdapter) mPopularPluginsRecycler.getAdapter()).reloadPluginWithSlug(slug);
        ((PluginBrowserAdapter) mNewPluginsRecycler.getAdapter()).reloadPluginWithSlug(slug);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (mSearchView != null) {
            mSearchView.clearFocus();
        }
        ActivityUtils.hideKeyboard(this);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        submitSearch(query, true);
        return true;
    }

    private PluginListFragment getListFragment() {
        Fragment fragment = getFragmentManager().findFragmentByTag(PluginListFragment.TAG);
        if (fragment != null) {
            return (PluginListFragment) fragment;
        }
        return null;
    }

    private PluginListFragment showListFragment(@NonNull PluginListType listType) {
        PluginListFragment listFragment = getListFragment();
        if (listFragment != null) {
            listFragment.setListType(listType);
        } else {
            listFragment = PluginListFragment.newInstance(mViewModel.getSite(), listType);
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, listFragment, PluginListFragment.TAG)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        }
        return listFragment;
    }

    private void reloadListFragment() {
        PluginListFragment fragment = getListFragment();
        if (fragment != null) {
            fragment.requestPlugins();
        }
    }

    private void submitSearch(@Nullable final String query, boolean delayed) {
        mViewModel.setSearchQuery(query);

        if (delayed) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (StringUtils.equals(query, mViewModel.getSearchQuery())) {
                        submitSearch(query, false);
                    }
                }
            }, 250);
        } else {
            mViewModel.clearSearchResults();
            PluginListFragment fragment = showListFragment(PluginListType.SEARCH);
            fragment.showEmptyView(false);
            if (!TextUtils.isEmpty(mViewModel.getSearchQuery())) {
                fetchPlugins(PluginListType.SEARCH, false);
            }
        }
    }

    private void hideListFragment() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            onBackPressed();
        }
    }

    @Override
    public List<?> onListFragmentRequestPlugins(@NonNull PluginListType listType) {
        setTitle(listType.getTitleRes());
        return mViewModel.getPluginsForListType(listType);
    }

    @Override
    public void onListFragmentLoadMore(@NonNull PluginListType listType) {
        fetchPlugins(listType, true);
    }

    private void showProgress(boolean show) {
        findViewById(R.id.progress).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem menuItem) {
        submitSearch(null, true);
        mSearchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem menuItem) {
        mSearchView.setOnQueryTextListener(null);
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
                name = sitePlugin.getDisplayName();
                author = sitePlugin.getAuthorName();
            } else {
                wpOrgPlugin = (WPOrgPluginModel) item;
                sitePlugin = mViewModel.getSitePluginFromSlug(wpOrgPlugin.getSlug());
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

        private void reloadPluginWithSlug(@NonNull String slug) {
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

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getAdapterPosition();
                        Object item = getItem(position);
                        SitePluginModel sitePlugin;
                        WPOrgPluginModel wpOrgPlugin;
                        if (item instanceof SitePluginModel) {
                            sitePlugin = (SitePluginModel) item;
                            wpOrgPlugin = mPluginStore.getWPOrgPluginBySlug(sitePlugin.getSlug());
                        } else {
                            wpOrgPlugin = (WPOrgPluginModel) item;
                            sitePlugin = mViewModel.getSitePluginFromSlug(wpOrgPlugin.getSlug());
                        }
                        if (sitePlugin != null) {
                            ActivityLauncher.viewPluginDetailForResult(PluginBrowserActivity.this, mViewModel.getSite(), sitePlugin);
                        } else {
                            ActivityLauncher.viewPluginDetailForResult(PluginBrowserActivity.this, mViewModel.getSite(), wpOrgPlugin);
                        }
                    }
                });
            }
        }
    }

}
