package org.wordpress.android.ui.plugins;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.SitePluginModel;
import org.wordpress.android.fluxc.model.WPOrgPluginModel;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginConfigured;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginDeleted;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginsFetched;
import org.wordpress.android.fluxc.store.PluginStore.OnWPOrgPluginFetched;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.wordpress.android.widgets.DividerItemDecoration;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPNetworkImageView.ImageType;

import java.util.List;

import javax.inject.Inject;

import static org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper;

public class PluginListActivity extends AppCompatActivity {
    private static final String KEY_REFRESHING = "KEY_REFRESHING";

    private SiteModel mSite;
    private RecyclerView mRecyclerView;
    private PluginListAdapter mAdapter;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;

    @Inject PluginStore mPluginStore;
    @Inject Dispatcher mDispatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        setContentView(R.layout.plugin_list_activity);
        mDispatcher.register(this);

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
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        setupViews();

        if (savedInstanceState != null) {
            mSwipeToRefreshHelper.setRefreshing(savedInstanceState.getBoolean(KEY_REFRESHING, false));
        } else {
            // Fetch plugins for the first time the activity is created
            mSwipeToRefreshHelper.setRefreshing(true);
            fetchPluginList();
        }
    }

    @Override
    protected void onDestroy() {
        mDispatcher.unregister(this);
        super.onDestroy();
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
        outState.putBoolean(KEY_REFRESHING, mSwipeToRefreshHelper.isRefreshing());
    }

    private void setupViews() {
        mRecyclerView = findViewById(R.id.plugins_recycler_view);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL_LIST);
        mRecyclerView.addItemDecoration(dividerItemDecoration);
        mAdapter = new PluginListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeToRefreshHelper = buildSwipeToRefreshHelper(
                (CustomSwipeRefreshLayout) findViewById(R.id.ptr_layout),
                new SwipeToRefreshHelper.RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        if (isFinishing()) {
                            return;
                        }
                        if (!NetworkUtils.checkConnection(PluginListActivity.this)) {
                            mSwipeToRefreshHelper.setRefreshing(false);
                            return;
                        }
                        fetchPluginList();
                    }
                }
        );

        refreshPluginList();
    }

    private void fetchPluginList() {
        mDispatcher.dispatch(PluginActionBuilder.newFetchSitePluginsAction(mSite));
    }

    private void refreshPluginList() {
        mAdapter.setPlugins(mPluginStore.getSitePlugins(mSite));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSitePluginsFetched(OnSitePluginsFetched event) {
        if (isFinishing()) {
            return;
        }
        mSwipeToRefreshHelper.setRefreshing(false);
        if (event.isError()) {
            ToastUtils.showToast(this, R.string.plugin_fetch_error);
            return;
        }
        refreshPluginList();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWPOrgPluginFetched(OnWPOrgPluginFetched event) {
        if (isFinishing()) {
            return;
        }
        if (event.isError()) {
            AppLog.e(T.API, "An error occurred while fetching the wporg plugin with type: " + event.error.type);
            return;
        }
        if (!TextUtils.isEmpty(event.pluginSlug)) {
            mAdapter.refreshPluginWithSlug(event.pluginSlug);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSitePluginConfigured(OnSitePluginConfigured event) {
        if (event.isError()) {
            // We can ignore the error since the action is taken in `PluginDetailActivity`
            return;
        }
        if (!isFinishing()) {
            refreshPluginList();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSitePluginDeleted(OnSitePluginDeleted event) {
        if (!isFinishing()) {
            refreshPluginList();
        }
    }

    private class PluginListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {
        private List<SitePluginModel> mPlugins;

        private final LayoutInflater mLayoutInflater;

        PluginListAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
        }

        public void setPlugins(List<SitePluginModel> plugins) {
            mPlugins = plugins;
            notifyDataSetChanged();
        }

        private SitePluginModel getItem(int position) {
            if (mPlugins != null && position < mPlugins.size()) {
                return mPlugins.get(position);
            }
            return null;
        }

        @Override
        public int getItemCount() {
            return mPlugins != null ? mPlugins.size() : 0;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.plugin_list_row, parent, false);
            view.setOnClickListener(this);
            return new PluginViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            SitePluginModel sitePlugin = getItem(position);
            if (sitePlugin != null) {
                PluginViewHolder pluginHolder = (PluginViewHolder) holder;
                pluginHolder.name.setText(sitePlugin.getDisplayName());
                pluginHolder.status.setText(getPluginStatusText(sitePlugin));
                WPOrgPluginModel wpOrgPlugin = PluginUtils.getWPOrgPlugin(mPluginStore, sitePlugin);
                if (wpOrgPlugin == null) {
                    mDispatcher.dispatch(PluginActionBuilder.newFetchWporgPluginAction(sitePlugin.getSlug()));
                }
                String iconUrl = wpOrgPlugin != null ? wpOrgPlugin.getIcon() : "";
                pluginHolder.icon.setImageUrl(iconUrl, ImageType.PLUGIN_ICON);

                if (wpOrgPlugin != null && PluginUtils.isUpdateAvailable(sitePlugin, wpOrgPlugin)) {
                    pluginHolder.updateAvailableIcon.setVisibility(View.VISIBLE);
                } else {
                    pluginHolder.updateAvailableIcon.setVisibility(View.GONE);
                }
            }
        }

        private void refreshPluginWithSlug(@NonNull String slug) {
            int index = indexOfPluginWithSlug(slug);
            if (index != -1) {
                notifyItemChanged(index);
            }
        }

        private int indexOfPluginWithSlug(@NonNull String slug) {
            for (int i = 0 ; i < mPlugins.size(); i++) {
                if (slug.equalsIgnoreCase(mPlugins.get(i).getSlug())) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public void onClick(View view) {
            int itemPosition = mRecyclerView.getChildLayoutPosition(view);
            SitePluginModel plugin = getItem(itemPosition);
            ActivityLauncher.viewPluginDetail(PluginListActivity.this, mSite, plugin);
        }

        private class PluginViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            TextView status;
            WPNetworkImageView icon;
            ImageView updateAvailableIcon;

            PluginViewHolder(View view) {
                super(view);
                name = view.findViewById(R.id.plugin_name);
                status = view.findViewById(R.id.plugin_status);
                icon = view.findViewById(R.id.plugin_icon);
                updateAvailableIcon = view.findViewById(R.id.plugin_update_available_icon);
            }
        }
    }

    private String getPluginStatusText(@NonNull SitePluginModel plugin) {
        String activeStatus = plugin.isActive() ? getString(R.string.plugin_active)
                : getString(R.string.plugin_inactive);
        String autoUpdateStatus = plugin.isAutoUpdateEnabled() ? getString(R.string.plugin_autoupdates_on)
                : getString(R.string.plugin_autoupdates_off);
        return activeStatus + ", " + autoUpdateStatus;
    }
}
