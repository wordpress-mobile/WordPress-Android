package org.wordpress.android.ui.plugins;


import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
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
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.plugins.PluginBrowserActivity.PluginListType;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.widgets.DividerItemDecoration;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

public class PluginListFragment extends Fragment {
    public static final String TAG = PluginListFragment.class.getName();
    private static final String ARG_LIST_TYPE = "list_type";

    private PluginBrowserViewModel mViewModel;

    public interface PluginListFragmentListener {
        List<?> onListFragmentRequestPlugins(@NonNull PluginListType listType);
        void onListFragmentLoadMore(@NonNull PluginListType listType);
    }

    private RecyclerView mRecycler;
    private PluginListType mListType;

    private final HashMap<String, SitePluginModel> mSitePluginsMap = new HashMap<>();
    private final HashMap<String, WPOrgPluginModel> mWPOrgPluginsMap = new HashMap<>();

    private PluginListFragmentListener mListener;

    @Inject PluginStore mPluginStore;

    public static PluginListFragment newInstance(@NonNull SiteModel site, @NonNull PluginListType listType) {
        PluginListFragment fragment = new PluginListFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(WordPress.SITE, site);
        bundle.putSerializable(ARG_LIST_TYPE, listType);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        mViewModel = ViewModelProviders.of(getActivity()).get(PluginBrowserViewModel.class);

        mListType = (PluginListType) getArguments().getSerializable(ARG_LIST_TYPE);

        List<SitePluginModel> sitePlugins = mPluginStore.getSitePlugins(mViewModel.getSite());
        for (SitePluginModel plugin: sitePlugins) {
            mSitePluginsMap.put(plugin.getSlug(), plugin);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.plugin_list_fragment, container, false);

        mRecycler = view.findViewById(R.id.recycler);
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mRecycler.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        try {
            mListener = (PluginListFragmentListener) getActivity();
            requestPlugins();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement PluginListFragmentListener");
        }
    }

    void requestPlugins() {
        setPlugins(mListener.onListFragmentRequestPlugins(mListType));
    }

    void setListType(@NonNull PluginListType listType) {
        mListType = listType;
        getArguments().putSerializable(ARG_LIST_TYPE, mListType);
        requestPlugins();
    }

    PluginListType getListType() {
        return mListType;
    }

    private void setPlugins(@NonNull List<?> plugins) {
        // preload .org plugins for site plugins
        mWPOrgPluginsMap.clear();
        for (Object item: plugins) {
            if (item instanceof SitePluginModel) {
                SitePluginModel sitePlugin = (SitePluginModel) item;
                WPOrgPluginModel wpOrgPlugin = PluginUtils.getWPOrgPlugin(mPluginStore, sitePlugin);
                if (wpOrgPlugin != null) {
                    mWPOrgPluginsMap.put(wpOrgPlugin.getSlug(), wpOrgPlugin);
                }
            }
        }

        PluginListAdapter adapter;
        if (mRecycler.getAdapter() == null) {
            adapter = new PluginListAdapter(getActivity());
            mRecycler.setAdapter(adapter);
        } else {
            adapter = (PluginListAdapter) mRecycler.getAdapter();
        }
        adapter.setPlugins(plugins);
    }

    private SitePluginModel getSitePluginFromSlug(@Nullable String slug) {
        return mSitePluginsMap.get(slug);
    }

    private WPOrgPluginModel getWPOrgPluginFromSlug(@Nullable String slug) {
        return mWPOrgPluginsMap.get(slug);
    }

    private void showProgress(boolean show) {
        if (isAdded()) {
            getView().findViewById(R.id.progress).setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    void showEmptyView(boolean show) {
        if (isAdded()) {
            getView().findViewById(R.id.text_empty).setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void loadMore() {
        showProgress(true);
        mViewModel.setLoadingMorePlugins(mListType, true);
        mListener.onListFragmentLoadMore(mListType);
    }

    void onLoadedMore() {
        mViewModel.setLoadingMorePlugins(mListType, false);
        if (isAdded()) {
            showProgress(false);
            requestPlugins();
        }
    }

    private class PluginListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final PluginList mItems = new PluginList();
        private final LayoutInflater mLayoutInflater;

        PluginListAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
            setHasStableIds(true);
        }

        public void setPlugins(List<?> plugins) {
            if (!mItems.isSameList(plugins)) {
                mItems.clear();
                mItems.addAll(plugins);
                notifyDataSetChanged();
            }
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
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            Object item = getItem(position);
            SitePluginModel sitePlugin;
            WPOrgPluginModel wpOrgPlugin;
            if (item instanceof SitePluginModel) {
                sitePlugin = (SitePluginModel) item;
                wpOrgPlugin = getWPOrgPluginFromSlug(sitePlugin.getSlug());
            } else {
                wpOrgPlugin = (WPOrgPluginModel) item;
                sitePlugin = getSitePluginFromSlug(wpOrgPlugin.getSlug());
            }

            String name = sitePlugin != null ? sitePlugin.getDisplayName() : wpOrgPlugin.getName();
            String author = sitePlugin != null ? sitePlugin.getAuthorName() : HtmlUtils.fastStripHtml(wpOrgPlugin.getAuthorAsHtml());
            String iconUrl = wpOrgPlugin != null ? wpOrgPlugin.getIcon() : null;

            PluginViewHolder holder = (PluginViewHolder) viewHolder;
            holder.name.setText(name);
            holder.author.setText(author);
            holder.icon.setImageUrl(iconUrl, WPNetworkImageView.ImageType.PLUGIN_ICON);

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
                holder.statusText.setVisibility(View.VISIBLE);
                holder.statusIcon.setVisibility(View.VISIBLE);
                holder.ratingBar.setVisibility(View.GONE);
            } else {
                holder.statusText.setVisibility(View.GONE);
                holder.statusIcon.setVisibility(View.GONE);
                holder.ratingBar.setVisibility(View.VISIBLE);
                holder.ratingBar.setRating(PluginUtils.getAverageStarRating(wpOrgPlugin));
            }

            if (mViewModel.canLoadMorePlugins(mListType)
                    && !mViewModel.isLoadingMorePlugins(mListType)
                    && position == getItemCount() - 1) {
                loadMore();
            }
        }

        private class PluginViewHolder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView author;
            final TextView statusText;
            final ImageView statusIcon;
            final WPNetworkImageView icon;
            final RatingBar ratingBar;

            PluginViewHolder(View view) {
                super(view);
                name = view.findViewById(R.id.plugin_name);
                author = view.findViewById(R.id.plugin_author);
                statusText = view.findViewById(R.id.plugin_status_text);
                statusIcon = view.findViewById(R.id.plugin_status_icon);
                icon = view.findViewById(R.id.plugin_icon);
                ratingBar = view.findViewById(R.id.rating_bar);

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
                            sitePlugin = getSitePluginFromSlug(wpOrgPlugin.getSlug());
                        }
                        if (sitePlugin != null) {
                            ActivityLauncher.viewPluginDetailForResult(getActivity(), mViewModel.getSite(), sitePlugin);
                        } else if (wpOrgPlugin != null) {
                            ActivityLauncher.viewPluginDetailForResult(getActivity(), mViewModel.getSite(), wpOrgPlugin);
                        }
                    }
                });
            }
        }
    }
}