package org.wordpress.android.ui.plugins;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPNetworkImageView.ImageType;

import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class PluginBrowserActivity extends AppCompatActivity
        implements SearchView.OnQueryTextListener,
        MenuItem.OnActionExpandListener {

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

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    private PluginBrowserViewModel mViewModel;
    private RecyclerView mSitePluginsRecycler;
    private RecyclerView mPopularPluginsRecycler;

    private RecyclerView mNewPluginsRecycler;
    private MenuItem mSearchMenuItem;

    private SearchView mSearchView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress)getApplication()).component().inject(this);
        setContentView(R.layout.plugin_browser_activity);

        mViewModel = ViewModelProviders.of(this, viewModelFactory).get(PluginBrowserViewModel.class);

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

        mViewModel.start();
        setupObservers();
    }

    private void setupObservers() {
        mViewModel.getSitePlugins().observe(this, new Observer<List<SitePluginModel>>() {
            @Override
            public void onChanged(@Nullable final List<SitePluginModel> sitePlugins) {
                refreshPluginAdapterAndVisibility(PluginListType.SITE, sitePlugins);
            }
        });

        mViewModel.getNewPlugins().observe(this, new Observer<List<WPOrgPluginModel>>() {
            @Override
            public void onChanged(@Nullable final List<WPOrgPluginModel> newPlugins) {
                refreshPluginAdapterAndVisibility(PluginListType.NEW, newPlugins);
            }
        });

        mViewModel.getPopularPlugins().observe(this, new Observer<List<WPOrgPluginModel>>() {
            @Override
            public void onChanged(@Nullable final List<WPOrgPluginModel> popularPlugins) {
                refreshPluginAdapterAndVisibility(PluginListType.POPULAR, popularPlugins);
            }
        });

        mViewModel.getSitePluginsListStatus().observe(this, new Observer<PluginBrowserViewModel.PluginListStatus>() {
            @Override
            public void onChanged(@Nullable PluginBrowserViewModel.PluginListStatus listStatus) {
                showProgress(listStatus == PluginBrowserViewModel.PluginListStatus.FETCHING
                        && mViewModel.isSitePluginsEmpty());

                if (listStatus == PluginBrowserViewModel.PluginListStatus.ERROR) {
                    ToastUtils.showToast(PluginBrowserActivity.this, R.string.plugin_fetch_error);
                }
            }
        });

        mViewModel.getLastUpdatedWpOrgPluginSlug().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String slug) {
                if (!TextUtils.isEmpty(slug)) {
                    reloadPluginWithSlug(slug);
                }
            }
        });
    }

    private void configureRecycler(@NonNull RecyclerView recycler) {
        recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recycler.setHasFixedSize(true);
        recycler.setAdapter(new PluginBrowserAdapter(this));
    }

    @Override
    protected void onDestroy() {
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
            mSearchView.setOnQueryTextListener(this);
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
            mViewModel.reloadAllPluginsFromStore();
        }
    }

    private void refreshPluginAdapterAndVisibility(@NonNull PluginListType pluginType, List<?> plugins) {
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
        adapter.setPlugins(plugins);

        int newVisibility = plugins.size() > 0 ? View.VISIBLE : View.GONE;
        int oldVisibility = cardView.getVisibility();
        if (newVisibility == View.VISIBLE && oldVisibility != View.VISIBLE) {
            AniUtils.fadeIn(cardView, AniUtils.Duration.MEDIUM);
        } else if (newVisibility != View.VISIBLE && oldVisibility == View.VISIBLE) {
            AniUtils.fadeOut(cardView, AniUtils.Duration.MEDIUM);
        }
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
        mViewModel.setSearchQuery(query);
        return true;
    }

    private PluginListFragment getListFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(PluginListFragment.TAG);
        if (fragment != null) {
            return (PluginListFragment) fragment;
        }
        return null;
    }

    private void showListFragment(@NonNull PluginListType listType) {
        PluginListFragment listFragment = getListFragment();
        if (listFragment != null) {
            listFragment.setListType(listType);
        } else {
            listFragment = PluginListFragment.newInstance(mViewModel.getSite(), listType);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, listFragment, PluginListFragment.TAG)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        }
        setTitle(listType.getTitleRes());
    }

    private void hideListFragment() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            onBackPressed();
        }
    }

    private void showProgress(boolean show) {
        findViewById(R.id.progress).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem menuItem) {
        showListFragment(PluginListType.SEARCH);
        mSearchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem menuItem) {
        mSearchView.setOnQueryTextListener(null);
        hideListFragment();
        mViewModel.setSearchQuery(null);
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
                wpOrgPlugin = mViewModel.getWPOrgPluginForSitePlugin(sitePlugin);
                // todo: move this to view model
                if (wpOrgPlugin == null) {
                    mViewModel.fetchWPOrgPlugin(sitePlugin.getSlug());
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
                            wpOrgPlugin = mViewModel.getWPOrgPluginForSitePlugin(sitePlugin);
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
