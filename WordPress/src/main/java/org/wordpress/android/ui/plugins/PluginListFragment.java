package org.wordpress.android.ui.plugins;


import android.arch.lifecycle.Observer;
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

import java.util.List;

import javax.inject.Inject;

public class PluginListFragment extends Fragment {
    public static final String TAG = PluginListFragment.class.getName();
    private static final String ARG_LIST_TYPE = "list_type";

    private PluginBrowserViewModel mViewModel;

    private RecyclerView mRecycler;
    private PluginListType mListType;

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

        setupObservers();
    }

    private void setupObservers() {
        mViewModel.getSitePlugins().observe(this, new Observer<List<SitePluginModel>>() {
            @Override
            public void onChanged(@Nullable final List<SitePluginModel> sitePlugins) {
                if (mListType == PluginListType.SITE) {
                    reloadPlugins();
                }
            }
        });

        mViewModel.getNewPlugins().observe(this, new Observer<List<WPOrgPluginModel>>() {
            @Override
            public void onChanged(@Nullable final List<WPOrgPluginModel> newPlugins) {
                if (mListType == PluginListType.NEW) {
                    reloadPlugins();
                }
            }
        });

        mViewModel.getPopularPlugins().observe(this, new Observer<List<WPOrgPluginModel>>() {
            @Override
            public void onChanged(@Nullable final List<WPOrgPluginModel> popularPlugins) {
                if (mListType == PluginListType.POPULAR) {
                    reloadPlugins();
                }
            }
        });

        mViewModel.getIsLoadingMoreNewPlugins().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean loadingMore) {
                if (mListType == PluginListType.NEW) {
                    showProgress(loadingMore != null ? loadingMore : false);
                }
            }
        });

        mViewModel.getIsLoadingMorePopularPlugins().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean loadingMore) {
                if (mListType == PluginListType.POPULAR) {
                    showProgress(loadingMore != null ? loadingMore : false);
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.plugin_list_fragment, container, false);

        mRecycler = view.findViewById(R.id.recycler);
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mRecycler.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));

        return view;
    }

    void reloadPlugins() {
        setPlugins(mViewModel.getPluginsForListType(mListType));
    }

    void setListType(@NonNull PluginListType listType) {
        showProgress(false);
        mListType = listType;
        getArguments().putSerializable(ARG_LIST_TYPE, mListType);
        reloadPlugins();
    }

    PluginListType getListType() {
        return mListType;
    }

    private void setPlugins(@Nullable List<?> plugins) {
        PluginListAdapter adapter;
        if (mRecycler.getAdapter() == null) {
            adapter = new PluginListAdapter(getActivity());
            mRecycler.setAdapter(adapter);
        } else {
            adapter = (PluginListAdapter) mRecycler.getAdapter();
        }
        adapter.setPlugins(plugins);
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

    private WPOrgPluginModel getWPOrgPluginForSitePlugin(SitePluginModel sitePlugin) {
        WPOrgPluginModel wpOrgPlugin = mViewModel.getCachedWPOrgPluginForSitePlugin(sitePlugin);
        // In most cases, this check won't be necessary, but we should still do it as a fallback
        if (wpOrgPlugin == null) {
            wpOrgPlugin = mPluginStore.getWPOrgPluginBySlug(sitePlugin.getSlug());
            mViewModel.cacheWPOrgPluginIfNecessary(wpOrgPlugin);
        }
        return wpOrgPlugin;
    }

    private class PluginListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final PluginList mItems = new PluginList();
        private final LayoutInflater mLayoutInflater;

        PluginListAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
            setHasStableIds(true);
        }

        public void setPlugins(@Nullable List<?> plugins) {
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
                wpOrgPlugin = getWPOrgPluginForSitePlugin(sitePlugin);
            } else {
                wpOrgPlugin = (WPOrgPluginModel) item;
                sitePlugin = mViewModel.getSitePluginFromSlug(wpOrgPlugin.getSlug());
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

            if (position == getItemCount() - 1) {
                mViewModel.loadMore(mListType);
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
                            sitePlugin = mViewModel.getSitePluginFromSlug(wpOrgPlugin.getSlug());
                        }
                        if (sitePlugin != null) {
                            ActivityLauncher.viewPluginDetailForResult(getActivity(), mViewModel.getSite(), sitePlugin);
                        } else {
                            ActivityLauncher.viewPluginDetailForResult(getActivity(), mViewModel.getSite(), wpOrgPlugin);
                        }
                    }
                });
            }
        }
    }
}