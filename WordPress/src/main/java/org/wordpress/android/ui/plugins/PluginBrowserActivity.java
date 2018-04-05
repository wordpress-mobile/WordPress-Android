package org.wordpress.android.ui.plugins;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentManager;
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
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.ImmutablePluginModel;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.viewmodel.PluginBrowserViewModel;
import org.wordpress.android.viewmodel.PluginBrowserViewModel.PluginListType;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPNetworkImageView.ImageType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class PluginBrowserActivity extends AppCompatActivity
        implements SearchView.OnQueryTextListener,
        MenuItem.OnActionExpandListener {
    @Inject ViewModelProvider.Factory mViewModelFactory;
    protected PluginBrowserViewModel mViewModel;

    private RecyclerView mSitePluginsRecycler;
    private RecyclerView mFeaturedPluginsRecycler;
    private RecyclerView mPopularPluginsRecycler;
    private RecyclerView mNewPluginsRecycler;

    private MenuItem mSearchMenuItem;
    private SearchView mSearchView;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        setContentView(R.layout.plugin_browser_activity);

        mViewModel = ViewModelProviders.of(this, mViewModelFactory).get(PluginBrowserViewModel.class);

        mSitePluginsRecycler = findViewById(R.id.installed_plugins_recycler);
        mFeaturedPluginsRecycler = findViewById(R.id.featured_plugins_recycler);
        mPopularPluginsRecycler = findViewById(R.id.popular_plugins_recycler);
        mNewPluginsRecycler = findViewById(R.id.new_plugins_recycler);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            mViewModel.setSite((SiteModel) getIntent().getSerializableExtra(WordPress.SITE));
        } else {
            mViewModel.readFromBundle(savedInstanceState);
        }
        mViewModel.start();

        if (mViewModel.getSite() == null) {
            ToastUtils.showToast(this, R.string.blog_not_found);
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

        // featured plugin list
        findViewById(R.id.text_all_featured).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showListFragment(PluginListType.FEATURED);
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

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                    mViewModel.setTitle(getString(R.string.plugins));
                }
            }
        });

        configureRecycler(mSitePluginsRecycler);
        configureRecycler(mFeaturedPluginsRecycler);
        configureRecycler(mPopularPluginsRecycler);
        configureRecycler(mNewPluginsRecycler);

        setupObservers();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mViewModel.writeToBundle(outState);
    }

    private void setupObservers() {
        mViewModel.getTitle().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String title) {
                setTitle(title);
            }
        });

        mViewModel.getSitePlugins().observe(this, new Observer<List<ImmutablePluginModel>>() {
            @Override
            public void onChanged(@Nullable final List<ImmutablePluginModel> sitePlugins) {
                reloadPluginAdapterAndVisibility(PluginListType.SITE, sitePlugins);
            }
        });

        mViewModel.getFeaturedPlugins().observe(this, new Observer<List<ImmutablePluginModel>>() {
            @Override
            public void onChanged(@Nullable final List<ImmutablePluginModel> featuredPlugins) {
                reloadPluginAdapterAndVisibility(PluginListType.FEATURED, featuredPlugins);
            }
        });

        mViewModel.getNewPlugins().observe(this, new Observer<List<ImmutablePluginModel>>() {
            @Override
            public void onChanged(@Nullable final List<ImmutablePluginModel> newPlugins) {
                reloadPluginAdapterAndVisibility(PluginListType.NEW, newPlugins);
            }
        });

        mViewModel.getPopularPlugins().observe(this, new Observer<List<ImmutablePluginModel>>() {
            @Override
            public void onChanged(@Nullable final List<ImmutablePluginModel> popularPlugins) {
                reloadPluginAdapterAndVisibility(PluginListType.POPULAR, popularPlugins);
            }
        });

        mViewModel.getSitePluginsListStatus().observe(this, new Observer<PluginBrowserViewModel.PluginListStatus>() {
            @Override
            public void onChanged(@Nullable PluginBrowserViewModel.PluginListStatus listStatus) {
                showProgress(listStatus == PluginBrowserViewModel.PluginListStatus.FETCHING
                             && mViewModel.isSitePluginsEmpty());

                // We should ignore the errors due to network condition, unless this is the first fetch, the user can
                // use the cached version of them and showing the error while the data is loaded might cause confusion
                if (listStatus == PluginBrowserViewModel.PluginListStatus.ERROR
                    && NetworkUtils.isNetworkAvailable(PluginBrowserActivity.this)) {
                    ToastUtils.showToast(PluginBrowserActivity.this, R.string.plugin_fetch_error);
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

    protected void reloadPluginAdapterAndVisibility(@NonNull PluginListType pluginType,
                                                    @Nullable List<ImmutablePluginModel> plugins) {
        PluginBrowserAdapter adapter = null;
        View cardView = null;
        switch (pluginType) {
            case SITE:
                adapter = (PluginBrowserAdapter) mSitePluginsRecycler.getAdapter();
                cardView = findViewById(R.id.installed_plugins_cardview);
                break;
            case FEATURED:
                adapter = (PluginBrowserAdapter) mFeaturedPluginsRecycler.getAdapter();
                cardView = findViewById(R.id.featured_plugins_cardview);
                break;
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
        }
        if (adapter == null || cardView == null) {
            return;
        }
        adapter.setPlugins(plugins);

        int newVisibility = plugins != null && plugins.size() > 0 ? View.VISIBLE : View.GONE;
        int oldVisibility = cardView.getVisibility();
        if (newVisibility == View.VISIBLE && oldVisibility != View.VISIBLE) {
            AniUtils.fadeIn(cardView, AniUtils.Duration.MEDIUM);
        } else if (newVisibility != View.VISIBLE && oldVisibility == View.VISIBLE) {
            AniUtils.fadeOut(cardView, AniUtils.Duration.MEDIUM);
        }
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
        mViewModel.setSearchQuery(query != null ? query : "");
        return true;
    }

    protected void showListFragment(@NonNull PluginListType listType) {
        PluginListFragment listFragment = PluginListFragment.newInstance(mViewModel.getSite(), listType);
        getSupportFragmentManager().beginTransaction()
                                   .add(R.id.fragment_container, listFragment, PluginListFragment.TAG)
                                   .addToBackStack(null)
                                   .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                   .commit();
        mViewModel.setTitle(getTitleForListType(listType));
        trackPluginListOpened(listType);
    }

    private void hideListFragment() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            onBackPressed();
        }
    }

    protected void showProgress(boolean show) {
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
        mViewModel.setSearchQuery("");
        return true;
    }

    private class PluginBrowserAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final PluginList mItems = new PluginList();
        private final LayoutInflater mLayoutInflater;

        PluginBrowserAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
            setHasStableIds(true);
        }

        void setPlugins(@Nullable List<ImmutablePluginModel> items) {
            mItems.clear();
            mItems.addAll(items);
            notifyDataSetChanged();
        }

        protected @Nullable Object getItem(int position) {
            return mItems.getItem(position);
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
            ImmutablePluginModel plugin = (ImmutablePluginModel) getItem(position);
            if (plugin == null) {
                return;
            }

            holder.mNameText.setText(plugin.getDisplayName());
            holder.mAuthorText.setText(plugin.getAuthorName());
            holder.mIcon.setImageUrl(plugin.getIcon(), ImageType.PLUGIN_ICON);

            if (plugin.isInstalled()) {
                @StringRes int textResId;
                @ColorRes int colorResId;
                @DrawableRes int drawableResId;
                if (PluginUtils.isUpdateAvailable(plugin)) {
                    textResId = R.string.plugin_needs_update;
                    colorResId = R.color.alert_yellow;
                    drawableResId = R.drawable.plugin_update_available_icon;
                } else if (plugin.isActive()) {
                    textResId = R.string.plugin_active;
                    colorResId = R.color.alert_green;
                    drawableResId = R.drawable.ic_checkmark_green_24dp;
                } else {
                    textResId = R.string.plugin_inactive;
                    colorResId = R.color.grey;
                    drawableResId = R.drawable.ic_cross_grey_600_24dp;
                }
                holder.mStatusText.setText(textResId);
                holder.mStatusText.setTextColor(getResources().getColor(colorResId));
                holder.mStatusIcon.setImageResource(drawableResId);
                holder.mStatusContainer.setVisibility(View.VISIBLE);
                holder.mRatingBar.setVisibility(View.GONE);
            } else {
                holder.mStatusContainer.setVisibility(View.GONE);
                holder.mRatingBar.setVisibility(View.VISIBLE);
                holder.mRatingBar.setRating(plugin.getAverageStarRating());
            }
        }

        private class PluginBrowserViewHolder extends ViewHolder {
            private final TextView mNameText;
            private final TextView mAuthorText;
            private final ViewGroup mStatusContainer;
            private final TextView mStatusText;
            private final ImageView mStatusIcon;
            private final WPNetworkImageView mIcon;
            private final RatingBar mRatingBar;

            PluginBrowserViewHolder(View view) {
                super(view);
                mNameText = view.findViewById(R.id.plugin_name);
                mAuthorText = view.findViewById(R.id.plugin_author);
                mIcon = view.findViewById(R.id.plugin_icon);
                mRatingBar = view.findViewById(R.id.rating_bar);

                mStatusContainer = view.findViewById(R.id.plugin_status_container);
                mStatusText = mStatusContainer.findViewById(R.id.plugin_status_text);
                mStatusIcon = mStatusContainer.findViewById(R.id.plugin_status_icon);

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getAdapterPosition();
                        ImmutablePluginModel plugin = (ImmutablePluginModel) getItem(position);
                        if (plugin == null) {
                            return;
                        }

                        ActivityLauncher.viewPluginDetail(PluginBrowserActivity.this, mViewModel.getSite(),
                                plugin.getSlug());
                    }
                });
            }
        }
    }

    private String getTitleForListType(@NonNull PluginListType pluginListType) {
        switch (pluginListType) {
            case FEATURED:
                return getString(R.string.plugin_caption_featured);
            case POPULAR:
                return getString(R.string.plugin_caption_popular);
            case NEW:
                return getString(R.string.plugin_caption_new);
            case SEARCH:
                return getString(R.string.plugin_caption_search);
            case SITE:
                return getString(R.string.plugin_caption_installed);
        }
        return getString(R.string.plugins);
    }

    void trackPluginListOpened(PluginListType listType) {
        if (listType == PluginListType.SEARCH) {
            // Although it's named as "search performed" we are actually only tracking the first search
            AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.PLUGIN_SEARCH_PERFORMED, mViewModel.getSite());
            return;
        }
        Map<String, Object> properties = new HashMap<>();
        String type = null;
        switch (listType) {
            case SITE:
                type = "installed";
                break;
            case FEATURED:
                type = "featured";
                break;
            case POPULAR:
                type = "popular";
                break;
            case NEW:
                type = "newest";
                break;
        }
        properties.put("type", type);
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_PLUGIN_LIST, mViewModel.getSite(), properties);
    }
}
