package org.wordpress.android.ui.stockphotos;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.StockMediaModel;
import org.wordpress.android.ui.media.MediaPreviewActivity;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPLinkMovementMethod;
import org.wordpress.android.viewmodel.StockMediaViewModel;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class StockPhotoPickerActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    private static final String KEY_SELECTED_ITEMS = "selected_items";
    private static final int MIN_SEARCH_QUERY_SIZE = 3;

    private SiteModel mSite;

    private StockPhotoAdapter mAdapter;

    private ViewGroup mSelectionBar;
    private TextView mTextInsert;

    private SearchView mSearchView;
    private String mSearchQuery;
    private final Handler mHandler = new Handler();

    private int mThumbWidth;
    private int mThumbHeight;

    @Inject ViewModelProvider.Factory mViewModelFactory;
    private StockMediaViewModel mViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        setContentView(R.layout.stock_photo_picker_activity);

        mViewModel = ViewModelProviders.of(this, mViewModelFactory).get(StockMediaViewModel.class);

        int displayWidth = DisplayUtils.getDisplayPixelWidth(this);
        mThumbWidth = displayWidth / getColumnCount();
        mThumbHeight = (int) (mThumbWidth * 0.75f);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }
        configureSearchView();

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new GridLayoutManager(this, getColumnCount()));

        mAdapter = new StockPhotoAdapter();
        recycler.setAdapter(mAdapter);

        mSelectionBar = findViewById(R.id.container_selection_bar);
        mTextInsert = findViewById(R.id.text_insert);
        mTextInsert.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                uploadSelection();
            }
        });
        findViewById(R.id.text_clear).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mAdapter.clearSelection();
            }
        });

        setupObserver();

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            showEmptyView(true);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mViewModel.readFromBundle(savedInstanceState);
            if (savedInstanceState.containsKey(KEY_SELECTED_ITEMS)) {
                ArrayList<Integer> selectedItems = savedInstanceState.getIntegerArrayList(KEY_SELECTED_ITEMS);
                if (selectedItems != null) {
                    mAdapter.setSelectedItems(selectedItems);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mSearchView != null) {
            mSearchView.setOnQueryTextListener(null);
            mSearchView.setOnCloseListener(null);
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mViewModel.writeToBundle(outState);
        if (mSite != null) {
            outState.putSerializable(WordPress.SITE, mSite);
        }
        if (!mAdapter.mSelectedItems.isEmpty()) {
            outState.putIntegerArrayList(KEY_SELECTED_ITEMS, mAdapter.mSelectedItems);
        }
    }

    /*
     * observe when the ViewModel's search results have changed and update the adapter with them
     */
    private void setupObserver() {
        mViewModel.getSearchResults().observe(this, new Observer<List<StockMediaModel>>() {
            @Override
            public void onChanged(@Nullable final List<StockMediaModel> mediaList) {
                if (!isFinishing()) {
                    showProgress(false);
                    showEmptyView(mediaList.isEmpty()
                                  && !TextUtils.isEmpty(mViewModel.getSearchQuery()));
                    mAdapter.setMediaList(mediaList);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (mSearchView != null) {
            mSearchView.clearFocus();
        }
        ActivityUtils.hideKeyboard(this);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        submitSearch(query, true);
        return true;
    }

    private void configureSearchView() {
        mSearchView = findViewById(R.id.search_view);
        mSearchView.setQuery(mViewModel.getSearchQuery(), false);

        // don't allow the SearchView to be closed
        mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override public boolean onClose() {
                return true;
            }
        });

        mSearchView.setOnQueryTextListener(this);
    }

    private void showEmptyView(boolean show) {
        if (!isFinishing()) {
            TextView txtEmpty = findViewById(R.id.text_empty);
            txtEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                boolean isEmpty = mSearchQuery == null || mSearchQuery.length() < MIN_SEARCH_QUERY_SIZE;
                if (isEmpty) {
                    String message = getString(R.string.stock_photo_picker_initial_empty_text);
                    String subMessage = getString(R.string.stock_photo_picker_initial_empty_subtext);
                    String link = "<a href='https://pexels.com/'>Pexels</a>";
                    String html = message
                                  + "<br /><br />"
                                  + "<small>" + String.format(subMessage, link) + "</small>";
                    txtEmpty.setMovementMethod(WPLinkMovementMethod.getInstance());
                    txtEmpty.setText(Html.fromHtml(html));
                } else {
                    txtEmpty.setText(R.string.stock_photo_picker_empty_results);
                }
            }
        }
    }

    private void showProgress(boolean show) {
        if (!isFinishing()) {
            findViewById(R.id.progress).setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void submitSearch(@Nullable final String query, boolean delayed) {
        mSearchQuery = query;

        if (query == null || query.length() < MIN_SEARCH_QUERY_SIZE) {
            mAdapter.clear();
            showEmptyView(true);
            return;
        }

        if (delayed) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (StringUtils.equals(query, mSearchQuery)) {
                        submitSearch(query, false);
                    }
                }
            }, 500);
        } else {
            requestStockPhotos(query, 1);
        }
    }

    private void requestStockPhotos(@Nullable String searchTerm, int page) {
        if (!NetworkUtils.checkConnection(this)) return;

        if (page == 1) {
            mAdapter.clear();
        }

        showProgress(true);
        mViewModel.fetchStockPhotos(searchTerm, page);
    }

    private void showSelectionBar() {
        if (mSelectionBar.getVisibility() != View.VISIBLE) {
            AniUtils.animateBottomBar(mSelectionBar, true);
        }
    }

    private void hideSelectionBar() {
        if (mSelectionBar.getVisibility() == View.VISIBLE) {
            AniUtils.animateBottomBar(mSelectionBar, false);
        }
    }

    private void notifySelectionCountChanged() {
        int numSelected = mAdapter.getSelectionCount();
        if (numSelected > 0) {
            String label = getString(R.string.insert) + " " + Integer.toString(numSelected);
            mTextInsert.setText(label);
            showSelectionBar();
            if (numSelected == 1) {
                ActivityUtils.hideKeyboardForced(mSearchView);
            }
        } else {
            hideSelectionBar();
        }
    }

    private void uploadSelection() {
        ToastUtils.showToast(this, "Uploading will be added in a later PR");
        // List<StockMediaModel> items = mAdapter.getSelectedItems();
    }

    class StockPhotoAdapter extends RecyclerView.Adapter<StockViewHolder> {
        private static final float SCALE_NORMAL = 1.0f;
        private static final float SCALE_SELECTED = .8f;

        private final List<StockMediaModel> mItems = new ArrayList<>();
        private final ArrayList<Integer> mSelectedItems = new ArrayList<>();

        StockPhotoAdapter() {
            setHasStableIds(true);
        }

        void setMediaList(@NonNull List<StockMediaModel> mediaList) {
            mItems.clear();
            mItems.addAll(mediaList);
            notifyDataSetChanged();
        }

        void clear() {
            mItems.clear();
            mSelectedItems.clear();
            notifyDataSetChanged();
        }

        @Override
        public long getItemId(int position) {
            return mItems.get(position).getId().hashCode();
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        @Override
        public StockViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.stock_photo_picker_thumbnail, parent, false);
            return new StockViewHolder(view);
        }

        @Override
        public void onBindViewHolder(StockViewHolder holder, int position) {
            StockMediaModel media = mItems.get(position);
            String imageUrl = PhotonUtils.getPhotonImageUrl(media.getThumbnail(), mThumbWidth, mThumbHeight);
            holder.mImageView.setImageUrl(imageUrl, WPNetworkImageView.ImageType.PHOTO);

            boolean isSelected = isItemSelected(position);
            holder.mSelectionCountTextView.setSelected(isSelected);
            if (isSelected) {
                int count = mSelectedItems.indexOf(position) + 1;
                String label = Integer.toString(count);
                holder.mSelectionCountTextView.setText(label);
            } else {
                holder.mSelectionCountTextView.setText(null);
            }

            float scale = isSelected ? SCALE_SELECTED : SCALE_NORMAL;
            if (holder.mImageView.getScaleX() != scale) {
                holder.mImageView.setScaleX(scale);
                holder.mImageView.setScaleY(scale);
            }

            if (!mViewModel.isFetching() && mViewModel.canLoadMore() && position == getItemCount() - 1) {
                requestStockPhotos(mSearchQuery, mViewModel.getNextPage());
            }
        }

        boolean isValidPosition(int position) {
            return position >= 0 && position < getItemCount();
        }

        boolean isItemSelected(int position) {
            return mSelectedItems.contains(position);
        }

        void setItemSelected(StockViewHolder holder, int position, boolean selected) {
            if (!isValidPosition(position)) return;

            if (selected) {
                mSelectedItems.add(position);
            } else {
                int index = mSelectedItems.indexOf(position);
                if (index == -1) {
                    return;
                }
                mSelectedItems.remove(index);
            }

            // show and animate the count
            if (selected) {
                String label = Integer.toString(mSelectedItems.indexOf(position) + 1);
                holder.mSelectionCountTextView.setText(label);
            } else {
                holder.mSelectionCountTextView.setText(null);
            }
            AniUtils.startAnimation(holder.mSelectionCountTextView, R.anim.pop);
            holder.mSelectionCountTextView.setVisibility(selected ? View.VISIBLE : View.GONE);

            // scale the thumbnail
            if (selected) {
                AniUtils.scale(holder.mImageView, SCALE_NORMAL, SCALE_SELECTED, AniUtils.Duration.SHORT);
            } else {
                AniUtils.scale(holder.mImageView, SCALE_SELECTED, SCALE_NORMAL, AniUtils.Duration.SHORT);
            }

            // redraw after the scale animation completes
            long delayMs = AniUtils.Duration.SHORT.toMillis(StockPhotoPickerActivity.this);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            }, delayMs);
        }

        private void toggleItemSelected(StockViewHolder holder, int position) {
            if (!isValidPosition(position)) return;

            boolean isSelected = isItemSelected(position);
            setItemSelected(holder, position, !isSelected);
            notifySelectionCountChanged();
        }

        private List<StockMediaModel> getSelectedItems() {
            List<StockMediaModel> items = new ArrayList<>();
            for (int i : mSelectedItems) {
                items.add(mItems.get(i));
            }
            return items;
        }

        private void setSelectedItems(@NonNull ArrayList<Integer> selectedItems) {
            mSelectedItems.clear();
            mSelectedItems.addAll(selectedItems);
            if (!mItems.isEmpty()) {
                notifyDataSetChanged();
            }
            notifySelectionCountChanged();
        }

        private void clearSelection() {
            if (mSelectedItems.size() > 0) {
                mSelectedItems.clear();
                notifyDataSetChanged();
                notifySelectionCountChanged();
            }
        }

        int getSelectionCount() {
            return mSelectedItems.size();
        }
    }

    class StockViewHolder extends RecyclerView.ViewHolder {
        private final WPNetworkImageView mImageView;
        private final TextView mSelectionCountTextView;

        StockViewHolder(View view) {
            super(view);

            mImageView = view.findViewById(R.id.image_thumbnail);
            mSelectionCountTextView = view.findViewById(R.id.text_selection_count);

            mImageView.getLayoutParams().width = mThumbWidth;
            mImageView.getLayoutParams().height = mThumbHeight;

            mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (mAdapter.isValidPosition(position)) {
                        mAdapter.toggleItemSelected(StockViewHolder.this, position);
                    }
                }
            });

            mImageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int position = getAdapterPosition();
                    if (mAdapter.isValidPosition(position)) {
                        MediaPreviewActivity.showPreview(v.getContext(), mSite, mAdapter.mItems.get(position).getUrl());
                    }
                    return true;
                }
            });
        }
    }

    private int getColumnCount() {
        return DisplayUtils.isLandscape(this) ? 4 : 3;
    }
}
