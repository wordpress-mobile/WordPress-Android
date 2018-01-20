package org.wordpress.android.ui.plugins;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
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
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.plugins.PluginBrowserActivity.PluginListType;
import org.wordpress.android.widgets.DividerItemDecoration;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class PluginListFragment extends Fragment {

    public static final String TAG = PluginListFragment.class.getName();
    private static final String ARG_LIST_TYPE = "list_type";

    public interface PluginListFragmentListener {
        List<?> onListFragmentRequestPlugins(@NonNull PluginListFragment fragment);
        void onListFragmentLoadMore(@NonNull PluginListFragment fragment);
    }

    private RecyclerView mRecycler;
    private PluginListType mListType;

    private boolean mCanLoadMore = true;
    private boolean mIsLoadingMore;

    private SiteModel mSite;
    private final List<SitePluginModel> mSitePlugins = new ArrayList<>();

    private PluginListFragmentListener mListener;

    @Inject PluginStore mPluginStore;
    @Inject Dispatcher mDispatcher;

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

        mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
        mSitePlugins.addAll(mPluginStore.getSitePlugins(mSite));
        mListType = (PluginListType) getArguments().getSerializable(ARG_LIST_TYPE);
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

    private void requestPlugins() {
        setPlugins(mListener.onListFragmentRequestPlugins(this));
    }

    void setListType(@NonNull PluginListType listType) {
        // site plugins are retrieved all at once
        if (listType == PluginListType.SITE) {
            mCanLoadMore = false;
        }

        if (!listType.equals(mListType)) {
            mListType = listType;
            getArguments().putSerializable(ARG_LIST_TYPE, mListType);
            requestPlugins();
        }
    }

    PluginListType getListType() {
        return mListType;
    }

    private void setPlugins(@NonNull List<?> plugins) {
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
        if (slug != null) {
            for (SitePluginModel plugin : mSitePlugins) {
                if (slug.equals(plugin.getSlug())) {
                    return plugin;
                }
            }
        }
        return null;
    }

    private void showProgress(boolean show) {
        if (isAdded()) {
            getView().findViewById(R.id.progress).setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    boolean isEmpty() {
        if (isAdded() && mRecycler != null && mRecycler.getAdapter() != null) {
            return mRecycler.getAdapter().getItemCount() == 0;
        }
        return true;
    }

    private void loadMore() {
        showProgress(true);
        mIsLoadingMore = true;
        mListener.onListFragmentLoadMore(PluginListFragment.this);
    }

    void onLoadedMore(boolean canLoadMore) {
        mCanLoadMore = canLoadMore;
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
                wpOrgPlugin = PluginUtils.getWPOrgPlugin(mPluginStore, sitePlugin);
            } else {
                wpOrgPlugin = (WPOrgPluginModel) item;
                sitePlugin = getSitePluginFromSlug(wpOrgPlugin.getSlug());
            }

            String name = sitePlugin != null ? sitePlugin.getDisplayName() : wpOrgPlugin.getName();
            String iconUrl = wpOrgPlugin != null ? wpOrgPlugin.getIcon() : null;

            PluginViewHolder holder = (PluginViewHolder) viewHolder;
            holder.name.setText(name);
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

            if (mCanLoadMore
                    && !mIsLoadingMore
                    && position == getItemCount() - 1) {
                loadMore();
            }
        }

        private void refreshPluginWithSlug(@NonNull String slug) {
            int index = mItems.indexOfPluginWithSlug(slug);
            if (index != -1) {
                notifyItemChanged(index);
            }
        }

        private class PluginViewHolder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView statusText;
            final ImageView statusIcon;
            final WPNetworkImageView icon;
            final RatingBar ratingBar;

            PluginViewHolder(View view) {
                super(view);
                name = view.findViewById(R.id.plugin_name);
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
                        if (item instanceof SitePluginModel) {
                            sitePlugin = (SitePluginModel) item;
                        } else {
                            WPOrgPluginModel wpOrgPlugin = (WPOrgPluginModel) item;
                            sitePlugin = getSitePluginFromSlug(wpOrgPlugin.getSlug());
                        }
                        // TODO: show detail for WPOrgPlugin
                        if (sitePlugin != null) {
                            ActivityLauncher.viewPluginDetail(getActivity(), mSite, sitePlugin);
                        }
                    }
                });
            }
        }
    }
}