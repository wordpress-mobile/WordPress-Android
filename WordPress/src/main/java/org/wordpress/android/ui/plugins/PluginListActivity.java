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
import android.widget.ProgressBar;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.model.PluginInfoModel;
import org.wordpress.android.fluxc.model.PluginModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.fluxc.store.PluginStore.OnPluginInfoChanged;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginConfigured;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginDeleted;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginsFetched;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.DividerItemDecoration;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPNetworkImageView.ImageType;

import java.util.List;

import javax.inject.Inject;

public class PluginListActivity extends AppCompatActivity {
    private SiteModel mSite;
    private RecyclerView mRecyclerView;
    private PluginListAdapter mAdapter;
    private ProgressBar mProgressBar;

    @Inject PluginStore mPluginStore;
    @Inject Dispatcher mDispatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.plugin_list_activity);

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

        mDispatcher.register(this);
        mDispatcher.dispatch(PluginActionBuilder.newFetchSitePluginsAction(mSite));

        setupViews();
        if (mPluginStore.getSitePlugins(mSite).size() == 0) {
            mProgressBar.setVisibility(View.VISIBLE);
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

        mProgressBar = findViewById(R.id.plugin_progress_bar);

        refreshPluginList();
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
        mProgressBar.setVisibility(View.GONE);
        if (event.isError()) {
            AppLog.e(T.API, "An error occurred while fetching the plugins: " + event.error.message);
            return;
        }
        refreshPluginList();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPluginInfoChanged(OnPluginInfoChanged event) {
        if (isFinishing()) {
            return;
        }
        if (event.isError()) {
            AppLog.e(T.API, "An error occurred while fetching the plugin info with type: " + event.error.type);
            return;
        }
        if (event.pluginInfo != null && !TextUtils.isEmpty(event.pluginInfo.getSlug())) {
            mAdapter.refreshPluginWithSlug(event.pluginInfo.getSlug());
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
        private List<PluginModel> mPlugins;

        private final LayoutInflater mLayoutInflater;

        PluginListAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
        }

        public void setPlugins(List<PluginModel> plugins) {
            mPlugins = plugins;
            notifyDataSetChanged();
        }

        private PluginModel getItem(int position) {
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
            PluginModel pluginModel = getItem(position);
            if (pluginModel != null) {
                PluginViewHolder pluginHolder = (PluginViewHolder) holder;
                pluginHolder.name.setText(pluginModel.getDisplayName());
                pluginHolder.status.setText(getPluginStatusText(pluginModel));
                PluginInfoModel pluginInfo = PluginUtils.getPluginInfo(mPluginStore, pluginModel);
                if (pluginInfo == null) {
                    mDispatcher.dispatch(PluginActionBuilder.newFetchPluginInfoAction(pluginModel.getSlug()));
                }
                String iconUrl = pluginInfo != null ? pluginInfo.getIcon() : "";
                pluginHolder.icon.setImageUrl(iconUrl, ImageType.PLUGIN_ICON);

                if (pluginInfo != null && PluginUtils.isUpdateAvailable(pluginModel, pluginInfo)) {
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
            PluginModel plugin = getItem(itemPosition);
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

    private String getPluginStatusText(@NonNull PluginModel plugin) {
        String activeStatus = plugin.isActive() ? getString(R.string.plugin_active)
                : getString(R.string.plugin_inactive);
        String autoUpdateStatus = plugin.isAutoUpdateEnabled() ? getString(R.string.plugin_autoupdates_on)
                : getString(R.string.plugin_autoupdates_off);
        return activeStatus + ", " + autoUpdateStatus;
    }
}
