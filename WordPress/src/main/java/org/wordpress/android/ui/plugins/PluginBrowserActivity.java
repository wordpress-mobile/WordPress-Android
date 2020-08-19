package org.wordpress.android.ui.plugins;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.google.android.material.appbar.AppBarLayout;

import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.ImmutablePluginModel;
import org.wordpress.android.models.networkresource.ListState;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.ColorUtils;
import org.wordpress.android.util.ContextExtensionsKt;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;
import org.wordpress.android.viewmodel.plugins.PluginBrowserViewModel;
import org.wordpress.android.viewmodel.plugins.PluginBrowserViewModel.PluginListType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class PluginBrowserActivity extends LocaleAwareActivity
        implements SearchView.OnQueryTextListener,
        MenuItem.OnActionExpandListener {
    @Inject ViewModelProvider.Factory mViewModelFactory;
    @Inject ImageManager mImageManager;
    private PluginBrowserViewModel mViewModel;

    private RecyclerView mSitePluginsRecycler;
    private RecyclerView mFeaturedPluginsRecycler;
    private RecyclerView mPopularPluginsRecycler;
    private RecyclerView mNewPluginsRecycler;
    private AppBarLayout mAppBar;

    private MenuItem mSearchMenuItem;
    private SearchView mSearchView;

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
        mAppBar = findViewById(R.id.appbar_main);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        SiteModel siteModel = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        if (siteModel == null) {
            ToastUtils.showToast(this, R.string.blog_not_found);
            finish();
            return;
        }

        if (savedInstanceState == null) {
            mViewModel.setSite(siteModel);
        } else {
            mViewModel.readFromBundle(savedInstanceState);
        }
        mViewModel.start();

        // site plugin list
        findViewById(R.id.text_manage).setOnClickListener(v -> showListFragment(PluginListType.SITE));

        // featured plugin list
        findViewById(R.id.text_all_featured).setOnClickListener(v -> showListFragment(PluginListType.FEATURED));

        // popular plugin list
        findViewById(R.id.text_all_popular).setOnClickListener(v -> showListFragment(PluginListType.POPULAR));

        // new plugin list
        findViewById(R.id.text_all_new).setOnClickListener(v -> showListFragment(PluginListType.NEW));

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                mViewModel.setTitle(getString(R.string.plugins));
            }
        });

        configureRecycler(mSitePluginsRecycler);
        configureRecycler(mFeaturedPluginsRecycler);
        configureRecycler(mPopularPluginsRecycler);
        configureRecycler(mNewPluginsRecycler);

        setupObservers();
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mViewModel.writeToBundle(outState);
    }

    private void setupObservers() {
        mViewModel.getTitle().observe(this, title -> setTitle(title));

        mViewModel.getSitePluginsLiveData()
                  .observe(this, listState -> {
                      if (listState != null) {
                          reloadPluginAdapterAndVisibility(PluginListType.SITE, listState);

                          showProgress(listState.isFetchingFirstPage() && listState.getData().isEmpty());

                          // We should ignore the errors due to network condition, unless this is the first
                          // fetch, the user
                          // can use the cached data and showing the error while the data is loaded might cause
                          // confusion
                          if (listState instanceof ListState.Error
                              && NetworkUtils.isNetworkAvailable(PluginBrowserActivity.this)) {
                              ToastUtils.showToast(PluginBrowserActivity.this, R.string.plugin_fetch_error);
                          }
                      }
                  });

        mViewModel.getFeaturedPluginsLiveData()
                  .observe(this, listState -> {
                      if (listState != null) {
                          reloadPluginAdapterAndVisibility(PluginListType.FEATURED, listState);
                      }
                  });

        mViewModel.getPopularPluginsLiveData()
                  .observe(this, listState -> {
                      if (listState != null) {
                          reloadPluginAdapterAndVisibility(PluginListType.POPULAR, listState);
                      }
                  });

        mViewModel.getNewPluginsLiveData()
                  .observe(this, listState -> {
                      if (listState != null) {
                          reloadPluginAdapterAndVisibility(PluginListType.NEW, listState);
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
        mSearchView.setMaxWidth(Integer.MAX_VALUE);

        PluginListFragment currentFragment = getCurrentFragment();
        if (currentFragment != null && currentFragment.getListType() == PluginListType.SEARCH) {
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
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            mAppBar.post(() -> {
                mAppBar.setLiftOnScrollTargetViewId(R.id.scroll_view);
                mAppBar.requestLayout();
            });
        }
        super.onBackPressed();
    }

    private void reloadPluginAdapterAndVisibility(@NonNull PluginListType pluginType,
                                                  @Nullable ListState<ImmutablePluginModel> listState) {
        if (listState == null) {
            return;
        }
        PluginBrowserAdapter adapter = null;
        View cardView = null;
        switch (pluginType) {
            case SITE:
                adapter = (PluginBrowserAdapter) mSitePluginsRecycler.getAdapter();
                cardView = findViewById(R.id.installed_plugins_container);
                break;
            case FEATURED:
                adapter = (PluginBrowserAdapter) mFeaturedPluginsRecycler.getAdapter();
                cardView = findViewById(R.id.featured_plugins_container);
                break;
            case POPULAR:
                adapter = (PluginBrowserAdapter) mPopularPluginsRecycler.getAdapter();
                cardView = findViewById(R.id.popular_plugins_container);
                break;
            case NEW:
                adapter = (PluginBrowserAdapter) mNewPluginsRecycler.getAdapter();
                cardView = findViewById(R.id.new_plugins_container);
                break;
            case SEARCH:
                return;
        }
        if (adapter == null || cardView == null) {
            return;
        }
        List<ImmutablePluginModel> plugins = listState.getData();
        adapter.setPlugins(plugins);

        int newVisibility = plugins.size() > 0 ? View.VISIBLE : View.GONE;
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

    private void showListFragment(@NonNull PluginListType listType) {
        mAppBar.post(() -> {
            mAppBar.setLiftOnScrollTargetViewId(R.id.recycler);
            mAppBar.requestLayout();
        });
        PluginListFragment listFragment = PluginListFragment.newInstance(mViewModel.getSite(), listType);
        getSupportFragmentManager().beginTransaction()
                                   .replace(R.id.fragment_container, listFragment, PluginListFragment.TAG)
                                   .addToBackStack(null)
                                   .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                   .commit();
        mViewModel.setTitle(getTitleForListType(listType));
        trackPluginListOpened(listType);
    }

    private @Nullable PluginListFragment getCurrentFragment() {
        return (PluginListFragment) getSupportFragmentManager().findFragmentByTag(PluginListFragment.TAG);
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

        void setPlugins(@NonNull List<ImmutablePluginModel> items) {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(mViewModel.getDiffCallback(mItems, items));
            mItems.clear();
            mItems.addAll(items);
            diffResult.dispatchUpdatesTo(this);
        }

        private @Nullable Object getItem(int position) {
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

        @NotNull
        @Override
        public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.plugin_browser_row, parent, false);
            return new PluginBrowserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NotNull ViewHolder viewHolder, int position) {
            PluginBrowserViewHolder holder = (PluginBrowserViewHolder) viewHolder;
            ImmutablePluginModel plugin = (ImmutablePluginModel) getItem(position);
            if (plugin == null) {
                return;
            }

            holder.mNameText.setText(plugin.getDisplayName());
            holder.mAuthorText.setText(plugin.getAuthorName());
            mImageManager.load(holder.mIcon, ImageType.PLUGIN, StringUtils.notNullStr(plugin.getIcon()));

            if (plugin.isInstalled()) {
                @StringRes int textResId;
                @ColorRes int colorResId;
                @DrawableRes int drawableResId;
                boolean isAutoManaged = PluginUtils.isAutoManaged(mViewModel.getSite(), plugin);
                if (isAutoManaged) {
                    textResId = R.string.plugin_auto_managed;
                    colorResId = ContextExtensionsKt
                            .getColorResIdFromAttribute(holder.mStatusIcon.getContext(), R.attr.wpColorSuccess);
                    drawableResId = android.R.color.transparent;
                } else if (PluginUtils.isUpdateAvailable(plugin)) {
                    textResId = R.string.plugin_needs_update;
                    colorResId = ContextExtensionsKt
                            .getColorResIdFromAttribute(holder.mStatusIcon.getContext(), R.attr.wpColorWarningDark);
                    drawableResId = R.drawable.ic_sync_white_24dp;
                } else if (plugin.isActive()) {
                    textResId = R.string.plugin_active;
                    colorResId = ContextExtensionsKt
                            .getColorResIdFromAttribute(holder.mStatusIcon.getContext(), R.attr.wpColorSuccess);
                    drawableResId = R.drawable.ic_checkmark_white_24dp;
                } else {
                    textResId = R.string.plugin_inactive;
                    colorResId = ContextExtensionsKt
                            .getColorResIdFromAttribute(holder.mStatusIcon.getContext(), R.attr.wpColorOnSurfaceMedium);
                    drawableResId = R.drawable.ic_cross_white_24dp;
                }
                holder.mStatusText.setText(textResId);
                holder.mStatusText.setTextColor(
                        AppCompatResources.getColorStateList(holder.mStatusText.getContext(), colorResId));
                holder.mStatusIcon.setVisibility(isAutoManaged ? View.GONE : View.VISIBLE);
                ColorUtils.INSTANCE.setImageResourceWithTint(holder.mStatusIcon, drawableResId, colorResId);
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
            private final ImageView mIcon;
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

                view.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    ImmutablePluginModel plugin = (ImmutablePluginModel) getItem(position);
                    if (plugin == null) {
                        return;
                    }

                    ActivityLauncher.viewPluginDetail(PluginBrowserActivity.this, mViewModel.getSite(),
                            plugin.getSlug());
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

    private void trackPluginListOpened(PluginListType listType) {
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
