package org.wordpress.android.ui.plugins;


import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.ImmutablePluginModel;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.wordpress.android.viewmodel.PluginBrowserViewModel;
import org.wordpress.android.viewmodel.PluginBrowserViewModel.PluginListType;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.List;

import javax.inject.Inject;

import static org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper;

public class PluginListFragment extends Fragment {
    public static final String TAG = PluginListFragment.class.getName();

    @Inject ViewModelProvider.Factory mViewModelFactory;

    private static final String ARG_LIST_TYPE = "list_type";

    protected PluginBrowserViewModel mViewModel;

    protected RecyclerView mRecycler;
    protected PluginListType mListType;
    protected SwipeToRefreshHelper mSwipeToRefreshHelper;

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

        mListType = (PluginListType) getArguments().getSerializable(ARG_LIST_TYPE);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // this enables us to clear the search icon in onCreateOptionsMenu when the list isn't showing search results
        setHasOptionsMenu(mListType != PluginListType.SEARCH);

        // Use the same view model as the PluginBrowserActivity
        mViewModel = ViewModelProviders.of(getActivity(), mViewModelFactory).get(PluginBrowserViewModel.class);
        setupObservers();
    }

    private void setupObservers() {
        mViewModel.getSitePlugins().observe(this, new Observer<List<ImmutablePluginModel>>() {
            @Override
            public void onChanged(@Nullable final List<ImmutablePluginModel> sitePlugins) {
                if (mListType == PluginListType.SITE) {
                    reloadPlugins();
                }
            }
        });

        mViewModel.getFeaturedPlugins().observe(this, new Observer<List<ImmutablePluginModel>>() {
            @Override
            public void onChanged(@Nullable final List<ImmutablePluginModel> featuredPlugins) {
                if (mListType == PluginListType.FEATURED) {
                    reloadPlugins();
                }
            }
        });

        mViewModel.getNewPlugins().observe(this, new Observer<List<ImmutablePluginModel>>() {
            @Override
            public void onChanged(@Nullable final List<ImmutablePluginModel> newPlugins) {
                if (mListType == PluginListType.NEW) {
                    reloadPlugins();
                }
            }
        });

        mViewModel.getPopularPlugins().observe(this, new Observer<List<ImmutablePluginModel>>() {
            @Override
            public void onChanged(@Nullable final List<ImmutablePluginModel> popularPlugins) {
                if (mListType == PluginListType.POPULAR) {
                    reloadPlugins();
                }
            }
        });

        mViewModel.getSearchResults().observe(this, new Observer<List<ImmutablePluginModel>>() {
            @Override
            public void onChanged(@Nullable final List<ImmutablePluginModel> popularPlugins) {
                if (mListType == PluginListType.SEARCH) {
                    reloadPlugins();
                }
            }
        });

        mViewModel.getSitePluginsListStatus().observe(this, new Observer<PluginBrowserViewModel.PluginListStatus>() {
            @Override
            public void onChanged(@Nullable PluginBrowserViewModel.PluginListStatus listStatus) {
                if (mListType == PluginListType.SITE) {
                    refreshProgressBars(listStatus);
                }
            }
        });

        mViewModel.getFeaturedPluginsListStatus()
                  .observe(this, new Observer<PluginBrowserViewModel.PluginListStatus>() {
                      @Override
                      public void onChanged(@Nullable PluginBrowserViewModel.PluginListStatus listStatus) {
                          if (mListType == PluginListType.FEATURED) {
                              refreshProgressBars(listStatus);
                          }
                      }
                  });

        mViewModel.getNewPluginsListStatus().observe(this, new Observer<PluginBrowserViewModel.PluginListStatus>() {
            @Override
            public void onChanged(@Nullable PluginBrowserViewModel.PluginListStatus listStatus) {
                if (mListType == PluginListType.NEW) {
                    refreshProgressBars(listStatus);
                }
            }
        });

        mViewModel.getPopularPluginsListStatus().observe(this, new Observer<PluginBrowserViewModel.PluginListStatus>() {
            @Override
            public void onChanged(@Nullable PluginBrowserViewModel.PluginListStatus listStatus) {
                if (mListType == PluginListType.POPULAR) {
                    refreshProgressBars(listStatus);
                }
            }
        });

        mViewModel.getSearchPluginsListStatus().observe(this, new Observer<PluginBrowserViewModel.PluginListStatus>() {
            @Override
            public void onChanged(@Nullable PluginBrowserViewModel.PluginListStatus listStatus) {
                if (mListType == PluginListType.SEARCH) {
                    refreshProgressBars(listStatus);
                    if (listStatus == PluginBrowserViewModel.PluginListStatus.ERROR) {
                        ToastUtils.showToast(getActivity(), R.string.plugin_search_error);
                    }

                    showEmptyView(mViewModel.shouldShowEmptySearchResultsView());
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.plugin_list_fragment, container, false);

        mRecycler = view.findViewById(R.id.recycler);
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mRecycler.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));

        mSwipeToRefreshHelper = buildSwipeToRefreshHelper(
                (CustomSwipeRefreshLayout) view.findViewById(R.id.ptr_layout),
                new SwipeToRefreshHelper.RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        if (NetworkUtils.checkConnection(getActivity())) {
                            mViewModel.pullToRefresh(mListType);
                        } else {
                            mSwipeToRefreshHelper.setRefreshing(false);
                        }
                    }
                });
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
    }

    void reloadPlugins() {
        setPlugins(mViewModel.getPluginsForListType(mListType));
    }

    private void setPlugins(@Nullable List<ImmutablePluginModel> plugins) {
        PluginListAdapter adapter;
        if (mRecycler.getAdapter() == null) {
            adapter = new PluginListAdapter(getActivity());
            mRecycler.setAdapter(adapter);
        } else {
            adapter = (PluginListAdapter) mRecycler.getAdapter();
        }
        adapter.setPlugins(plugins);
    }

    protected void refreshProgressBars(PluginBrowserViewModel.PluginListStatus pluginListStatus) {
        if (!isAdded() || getView() == null) {
            return;
        }
        // We want to show the swipe refresher for the initial fetch but not while loading more
        mSwipeToRefreshHelper.setRefreshing(pluginListStatus == PluginBrowserViewModel.PluginListStatus.FETCHING);
        // We want to show the progress bar at the bottom while loading more but not for initial fetch
        boolean showLoadMore = pluginListStatus == PluginBrowserViewModel.PluginListStatus.LOADING_MORE;
        getView().findViewById(R.id.progress).setVisibility(showLoadMore ? View.VISIBLE : View.GONE);
    }

    void showEmptyView(boolean show) {
        if (isAdded() && getView() != null) {
            getView().findViewById(R.id.text_empty).setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private class PluginListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final PluginList mItems = new PluginList();
        private final LayoutInflater mLayoutInflater;

        PluginListAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
            setHasStableIds(true);
        }

        void setPlugins(@Nullable List<ImmutablePluginModel> items) {
            mItems.clear();
            mItems.addAll(items);
            notifyDataSetChanged();
        }

        protected @Nullable Object getItem(int position) {
            return mItems.getItem(position);
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
            ImmutablePluginModel plugin = (ImmutablePluginModel) getItem(position);
            if (plugin == null) {
                return;
            }

            PluginViewHolder holder = (PluginViewHolder) viewHolder;
            holder.mName.setText(plugin.getDisplayName());
            holder.mAuthor.setText(plugin.getAuthorName());
            holder.mIcon.setImageUrl(plugin.getIcon(), WPNetworkImageView.ImageType.PLUGIN_ICON);

            if (plugin.isInstalled()) {
                @StringRes int textResId;
                @ColorRes int colorResId;
                @DrawableRes int drawableResId;
                if (PluginUtils.isUpdateAvailable(plugin)) {
                    textResId = R.string.plugin_needs_update;
                    colorResId = R.color.alert_yellow;
                    drawableResId = R.drawable.plugin_update_available_icon;
                } else if (plugin.isActive()) {
                    textResId = R.string.plugin_active;
                    colorResId = R.color.alert_green;
                    drawableResId = R.drawable.ic_checkmark_green_24dp;
                } else {
                    textResId = R.string.plugin_inactive;
                    colorResId = R.color.grey;
                    drawableResId = R.drawable.ic_cross_grey_600_24dp;
                }
                holder.mStatusText.setText(textResId);
                holder.mStatusText.setTextColor(getResources().getColor(colorResId));
                holder.mStatusIcon.setImageResource(drawableResId);
                holder.mStatusText.setVisibility(View.VISIBLE);
                holder.mStatusIcon.setVisibility(View.VISIBLE);
                holder.mRatingBar.setVisibility(View.GONE);
            } else {
                holder.mStatusText.setVisibility(View.GONE);
                holder.mStatusIcon.setVisibility(View.GONE);
                holder.mRatingBar.setVisibility(View.VISIBLE);
                holder.mRatingBar.setRating(plugin.getAverageStarRating());
            }

            if (position == getItemCount() - 1) {
                mViewModel.loadMore(mListType);
            }
        }

        private class PluginViewHolder extends RecyclerView.ViewHolder {
            private final TextView mName;
            private final TextView mAuthor;
            private final TextView mStatusText;
            private final ImageView mStatusIcon;
            private final WPNetworkImageView mIcon;
            private final RatingBar mRatingBar;

            PluginViewHolder(View view) {
                super(view);
                mName = view.findViewById(R.id.plugin_name);
                mAuthor = view.findViewById(R.id.plugin_author);
                mStatusText = view.findViewById(R.id.plugin_status_text);
                mStatusIcon = view.findViewById(R.id.plugin_status_icon);
                mIcon = view.findViewById(R.id.plugin_icon);
                mRatingBar = view.findViewById(R.id.rating_bar);

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getAdapterPosition();
                        ImmutablePluginModel plugin = (ImmutablePluginModel) getItem(position);
                        if (plugin == null) {
                            return;
                        }

                        ActivityLauncher.viewPluginDetail(getActivity(), mViewModel.getSite(),
                                plugin.getSlug());
                    }
                });
            }
        }
    }
}
