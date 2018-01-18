package org.wordpress.android.ui.plugins;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class PluginListFragment extends Fragment {

    public static final String TAG = PluginListFragment.class.getName();

    private RecyclerView mRecycler;
    private SiteModel mSite;
    private final List<SitePluginModel> mSitePlugins = new ArrayList<>();

    @Inject PluginStore mPluginStore;
    @Inject Dispatcher mDispatcher;

    public static PluginListFragment newInstance(@NonNull SiteModel site) {
        PluginListFragment fragment = new PluginListFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(WordPress.SITE, site);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
        mSitePlugins.addAll(mPluginStore.getSitePlugins(mSite));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.plugin_list_fragment, container, false);

        mRecycler = view.findViewById(R.id.recycler);
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));

        return view;
    }

    public void setPlugins(@NonNull List<Object> plugins) {
        PluginListAdapter adapter = new PluginListAdapter(getActivity());
        mRecycler.setAdapter(adapter);
        adapter.setPlugins(plugins);
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

    private class PluginListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final PluginList mItems = new PluginList();
        private final LayoutInflater mLayoutInflater;

        PluginListAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
            setHasStableIds(true);
        }

        public void setPlugins(List<Object> plugins) {
            mItems.clear();
            mItems.addAll(plugins);
            notifyDataSetChanged();
        }

        private Object getItem(int position) {
            if (position < mItems.size()) {
                return mItems.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return mItems.getItemId(position);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.plugin_list_row, parent, false);
            return new PluginViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Object item = getItem(position);
            SitePluginModel sitePlugin;
            WPOrgPluginModel wpOrgPlugin;
            if (item instanceof SitePluginModel) {
                sitePlugin = (SitePluginModel) item;
                wpOrgPlugin = PluginUtils.getWPOrgPlugin(mPluginStore, sitePlugin);
            } else {
                wpOrgPlugin = (WPOrgPluginModel) item;
                sitePlugin = getSitePluginFromSlug(wpOrgPlugin.getSlug());
            }

            String name = sitePlugin != null ? sitePlugin.getDisplayName() : wpOrgPlugin.getName();
            String status = sitePlugin != null ? getPluginStatusText(sitePlugin) : null;
            String iconUrl = wpOrgPlugin != null ? wpOrgPlugin.getIcon() : null;
            boolean isUpdatable = sitePlugin != null && PluginUtils.isUpdateAvailable(sitePlugin, wpOrgPlugin);

            PluginViewHolder pluginHolder = (PluginViewHolder) holder;
            pluginHolder.name.setText(name);
            pluginHolder.status.setText(status);
            pluginHolder.icon.setImageUrl(iconUrl, WPNetworkImageView.ImageType.PLUGIN_ICON);
            pluginHolder.updateAvailableIcon.setVisibility(isUpdatable ? View.VISIBLE : View.GONE);
        }

        private void refreshPluginWithSlug(@NonNull String slug) {
            int index = mItems.indexOfPluginWithSlug(slug);
            if (index != -1) {
                notifyItemChanged(index);
            }
        }

        private class PluginViewHolder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView status;
            final WPNetworkImageView icon;
            final ImageView updateAvailableIcon;

            PluginViewHolder(View view) {
                super(view);
                name = view.findViewById(R.id.plugin_name);
                status = view.findViewById(R.id.plugin_status);
                icon = view.findViewById(R.id.plugin_icon);
                updateAvailableIcon = view.findViewById(R.id.plugin_update_available_icon);

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getAdapterPosition();
                        Object item = getItem(position);
                        if (item instanceof SitePluginModel) {
                            SitePluginModel sitePlugin = (SitePluginModel) item;
                            ActivityLauncher.viewPluginDetail(getActivity(), mSite, sitePlugin);
                        } else {
                            // TODO: show detail for WPOrgPlugin - wait for detail redesign to be merged
                        }
                    }
                });
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