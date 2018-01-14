package org.wordpress.android.ui.plugins;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
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
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPNetworkImageView.ImageType;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class PluginBrowserActivity extends AppCompatActivity {
    private SiteModel mSite;
    private List<SitePluginModel> mSitePlugins;

    private RecyclerView mInstalledPluginsRecycler;
    private PluginDirectoryAdapter mInstalledPluginsAdapter;

    @Inject PluginStore mPluginStore;
    @Inject Dispatcher mDispatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.plugin_browser_activity);
        mDispatcher.register(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setElevation(0);
        }

        mInstalledPluginsRecycler = findViewById(R.id.installed_plugins_recycler);
        mInstalledPluginsRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mInstalledPluginsRecycler.setHasFixedSize(true);

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

        mSitePlugins = mPluginStore.getSitePlugins(mSite);
        loadInstalledPlugins();
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

    private void loadInstalledPlugins() {
        List<WPOrgPluginModel> wpOrgPluginList = new ArrayList<>();
        for (SitePluginModel sitePlugin: mSitePlugins) {
            WPOrgPluginModel wpOrgPlugin = PluginUtils.getWPOrgPlugin(mPluginStore, sitePlugin);
            if (wpOrgPlugin != null) {
                wpOrgPluginList.add(wpOrgPlugin);
            } else {
                mDispatcher.dispatch(PluginActionBuilder.newFetchWporgPluginAction(sitePlugin.getSlug()));
            }
        }

        mInstalledPluginsAdapter = new PluginDirectoryAdapter(this);
        mInstalledPluginsRecycler.setAdapter(mInstalledPluginsAdapter);
        mInstalledPluginsAdapter.setPlugins(wpOrgPluginList);
    }

    private SitePluginModel getSitePluginFromSlug(@Nullable String slug) {
        if (slug != null && mSitePlugins != null) {
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
    public void onWPOrgPluginFetched(PluginStore.OnWPOrgPluginFetched event) {
        if (isFinishing()) {
            return;
        }
        if (event.isError()) {
            AppLog.e(AppLog.T.API, "An error occurred while fetching the wporg plugin with type: " + event.error.type);
            return;
        }
        if (!TextUtils.isEmpty(event.pluginSlug)) {
            // TODO: refresh adapters
        }
    }

    private class PluginDirectoryAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final List<WPOrgPluginModel> mWpOrgPlugins = new ArrayList<>();
        private final LayoutInflater mLayoutInflater;

        PluginDirectoryAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
            setHasStableIds(true);
        }

        public void setPlugins(List<WPOrgPluginModel> plugins) {
            mWpOrgPlugins.clear();
            mWpOrgPlugins.addAll(plugins);
            notifyDataSetChanged();
        }

        private WPOrgPluginModel getItem(int position) {
            if (position < mWpOrgPlugins.size()) {
                return mWpOrgPlugins.get(position);
            }
            return null;
        }

        @Override
        public int getItemCount() {
            return mWpOrgPlugins.size();
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getId();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.plugin_browser_row, parent, false);
            return new PluginViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            PluginViewHolder pluginHolder = (PluginViewHolder) holder;
            WPOrgPluginModel wpOrgPlugin = getItem(position);
            SitePluginModel sitePlugin = getSitePluginFromSlug(wpOrgPlugin.getSlug());
            boolean isUpdatable = sitePlugin != null && PluginUtils.isUpdateAvailable(sitePlugin, wpOrgPlugin);

            pluginHolder.name.setText(wpOrgPlugin.getName());
            pluginHolder.ratingBar.setVisibility(isUpdatable ? View.GONE : View.VISIBLE);
            pluginHolder.update.setVisibility(isUpdatable ? View.VISIBLE : View.GONE);
            pluginHolder.icon.setImageUrl(wpOrgPlugin.getIcon(), ImageType.PLUGIN_ICON);
        }

        private void refreshPluginWithSlug(@NonNull String slug) {
            int index = indexOfPluginWithSlug(slug);
            if (index != -1) {
                notifyItemChanged(index);
            }
        }

        private int indexOfPluginWithSlug(@NonNull String slug) {
            for (int i = 0; i < mWpOrgPlugins.size(); i++) {
                if (slug.equalsIgnoreCase(mWpOrgPlugins.get(i).getSlug())) {
                    return i;
                }
            }
            return -1;
        }

        private class PluginViewHolder extends ViewHolder {
            final TextView name;
            final TextView author;
            final TextView update;
            final WPNetworkImageView icon;
            final RatingBar ratingBar;

            PluginViewHolder(View view) {
                super(view);
                name = view.findViewById(R.id.plugin_name);
                author = view.findViewById(R.id.plugin_author);
                update = view.findViewById(R.id.plugin_update);
                icon = view.findViewById(R.id.plugin_icon);
                ratingBar = view.findViewById(R.id.rating_bar);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getAdapterPosition();
                        WPOrgPluginModel plugin = getItem(position);
                        if (plugin != null) {
                            // TODO: ActivityLauncher.viewPluginDetail(PluginDirectoryActivity.this, mSite, plugin);
                        }
                    }
                });
            }
        }
    }
}
