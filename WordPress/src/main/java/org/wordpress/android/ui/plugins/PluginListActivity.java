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
import org.wordpress.android.fluxc.store.PluginStore.OnPluginsChanged;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.DividerItemDecoration;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPNetworkImageView.ImageType;

import java.util.List;

import javax.inject.Inject;

public class PluginListActivity extends AppCompatActivity {
    private SiteModel mSite;
    private PluginListAdapter mAdapter;

    @Inject PluginStore mPluginStore;
    @Inject Dispatcher mDispatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.plugin_list_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
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
        mDispatcher.dispatch(PluginActionBuilder.newFetchPluginsAction(mSite));

        setupViews();
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
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.plugins_recycler_view);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                DividerItemDecoration.VERTICAL_LIST);
        recyclerView.addItemDecoration(dividerItemDecoration);
        mAdapter = new PluginListAdapter(this);
        recyclerView.setAdapter(mAdapter);

        refreshPluginList();
    }

    private void refreshPluginList() {
        mAdapter.setPlugins(mPluginStore.getPlugins(mSite));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPluginsChanged(OnPluginsChanged event) {
        if (event.isError()) {
            ToastUtils.showToast(this, "An error occurred while fetching the plugins: " + event.error.message);
            return;
        }
        refreshPluginList();
    }

    private class PluginListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
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
            return new PluginViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            PluginModel pluginModel = getItem(position);
            if (pluginModel != null) {
                PluginViewHolder pluginHolder = (PluginViewHolder) holder;
                pluginHolder.name.setText(pluginModel.getDisplayName());
                pluginHolder.status.setText(getPluginStatusText(pluginModel));
                PluginInfoModel pluginInfo = getOrFetchPluginInfo(pluginModel);
                String iconUrl = pluginInfo != null ? pluginInfo.getIcon() : "";
                pluginHolder.icon.setImageUrl(iconUrl, ImageType.PLUGIN_ICON);
            }
        }

        private class PluginViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            TextView status;
            WPNetworkImageView icon;

            PluginViewHolder(View view) {
                super(view);
                name = (TextView) view.findViewById(R.id.plugin_name);
                status = (TextView) view.findViewById(R.id.plugin_status);
                icon = (WPNetworkImageView) view.findViewById(R.id.plugin_icon);
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

    private PluginInfoModel getOrFetchPluginInfo(@NonNull PluginModel plugin) {
        String slug = plugin.getSlug();
        if (TextUtils.isEmpty(slug)) {
            return null;
        }
        PluginInfoModel pluginInfo = mPluginStore.getPluginInfoBySlug(slug);
        if (pluginInfo == null) {
            mDispatcher.dispatch(PluginActionBuilder.newFetchPluginInfoAction(slug));
        }
        return pluginInfo;
    }
}
