package org.wordpress.android.ui.plugins;

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
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
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
import org.wordpress.android.fluxc.model.SitePluginModel;
import org.wordpress.android.fluxc.model.WPOrgPluginModel;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.StringUtils;
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

    private RecyclerView mNewPluginsRecycler;
    private PluginDirectoryAdapter mNewPluginsAdapter;

    private int mRowWidth;
    private int mRowHeight;
    private int mIconSize;

    @Inject PluginStore mPluginStore;
    @Inject Dispatcher mDispatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.plugin_browser_activity);
        mDispatcher.register(this);

        mInstalledPluginsRecycler = findViewById(R.id.installed_plugins_recycler);
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
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        findViewById(R.id.text_manage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogPlugins(PluginBrowserActivity.this, mSite);
            }
        });

        boolean isLandscape = DisplayUtils.isLandscape(this);
        int margin = getResources().getDimensionPixelSize(R.dimen.margin_extra_large);
        int displayWidth = DisplayUtils.getDisplayPixelWidth(this);
        int displayHeight = DisplayUtils.getDisplayPixelHeight(this);
        int maxRows = isLandscape ? 6 : 4;
        mRowWidth = Math.round(displayWidth / (maxRows - 0.4f));
        mIconSize = mRowWidth - (margin * 2);
        mRowHeight = isLandscape ? displayHeight / 2 : displayHeight / 3;

        mInstalledPluginsRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mInstalledPluginsRecycler.setHasFixedSize(true);

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

            // strip HTML here so we don't have to do it in every call to onBindViewHolder
            for (WPOrgPluginModel plugin: mWpOrgPlugins) {
                plugin.setAuthorAsHtml(HtmlUtils.fastStripHtml(plugin.getAuthorAsHtml()));
            }

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
            WPOrgPluginModel wpOrgPlugin = getItem(position);
            if (wpOrgPlugin != null) {
                return wpOrgPlugin.getId();
            }
            return -1;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.plugin_browser_row, parent, false);
            return new PluginViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            PluginViewHolder holder = (PluginViewHolder) viewHolder;
            WPOrgPluginModel wpOrgPlugin = getItem(position);
            if (wpOrgPlugin == null) return;

            SitePluginModel sitePlugin = getSitePluginFromSlug(wpOrgPlugin.getSlug());
            boolean isUpdatable = sitePlugin != null && PluginUtils.isUpdateAvailable(sitePlugin, wpOrgPlugin);

            holder.nameText.setText(wpOrgPlugin.getName());
            holder.authorText.setText(wpOrgPlugin.getAuthorAsHtml());
            holder.icon.setImageUrl(wpOrgPlugin.getIcon(), ImageType.PLUGIN_ICON);

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
                int rating = StringUtils.stringToInt(wpOrgPlugin.getRating(), 1);
                int averageRating = Math.round(rating / 20f);
                holder.ratingBar.setRating(averageRating);
            }
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
            final TextView nameText;
            final TextView authorText;
            final ViewGroup statusContainer;
            final TextView statusText;
            final ImageView statusIcon;
            final WPNetworkImageView icon;
            final RatingBar ratingBar;

            PluginViewHolder(View view) {
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
                        WPOrgPluginModel wpOrgPlugin = getItem(position);
                        SitePluginModel sitePlugin = wpOrgPlugin != null ? getSitePluginFromSlug(wpOrgPlugin.getSlug()) : null;
                        if (sitePlugin != null) {
                            ActivityLauncher.viewPluginDetail(PluginBrowserActivity.this, mSite, sitePlugin);
                        } else {
                            // TODO: show detail for WPOrgPlugin - wait for detail redesign to be merged
                        }
                    }
                });
            }
        }
    }
}
