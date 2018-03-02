package org.wordpress.android.ui.stockphotos;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.StockMediaActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.StockMediaModel;
import org.wordpress.android.fluxc.store.StockMediaStore;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class StockPhotoPickerActivity extends AppCompatActivity {

    private SiteModel mSite;

    private RecyclerView mRecycler;
    private StockPhotoAdapter mAdapter;

    private int mThumbWidth;
    private int mThumbHeight;

    private boolean mInMultiSelect;
    private boolean mIsFetching;

    private int mNextPage;
    private boolean mCanLoadMore;

    private SearchView mSearchView;
    private String mSearchQuery;
    private final Handler mHandler = new Handler();

    @Inject Dispatcher mDispatcher;
    @Inject StockMediaStore mStockMediaStore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        setContentView(R.layout.stock_photo_picker_activity);

        int displayWidth = DisplayUtils.getDisplayPixelWidth(this);
        mThumbWidth = displayWidth / getColumnCount();
        mThumbHeight = (int) (mThumbWidth * 0.75f);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        mRecycler = findViewById(R.id.recycler);
        mRecycler.setLayoutManager(new GridLayoutManager(this, getColumnCount()));

        mAdapter = new StockPhotoAdapter();
        mRecycler.setAdapter(mAdapter);

        mSearchView = findViewById(R.id.search_view);
        mSearchView.setEnabled(false);
        mSearchView.setIconifiedByDefault(false);

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                submitSearch(query, false);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String query) {
                submitSearch(query, true);
                return true;
            }
        });

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    protected void onStop() {
        mDispatcher.unregister(this);
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSite != null) {
            outState.putSerializable(WordPress.SITE, mSite);
        }
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

    private void showProgress(boolean show) {
        if (!isFinishing()) {
            findViewById(R.id.progress).setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void submitSearch(@Nullable final String query, boolean delayed) {
        mSearchQuery = query;

        if (TextUtils.isEmpty(query) || query.length() <= 2) {
            mAdapter.clear();
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

        mIsFetching = true;
        showProgress(true);

        StockMediaStore.FetchStockMediaListPayload payload =
                new StockMediaStore.FetchStockMediaListPayload(searchTerm, page);
        mDispatcher.dispatch(StockMediaActionBuilder.newFetchStockMediaAction(payload));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void OnStockMediaListFetched(StockMediaStore.OnStockMediaListFetched event) {
        mIsFetching = false;
        if (isFinishing()) return;

        showProgress(false);

        if (event.isError()) {
            AppLog.e(AppLog.T.MEDIA, "An error occurred while searching stock media");
            return;
        }
        if (mSearchQuery == null || !mSearchQuery.equals(event.searchTerm)) {
            return;
        }

        mNextPage = event.nextPage;
        mCanLoadMore = event.canLoadMore;

        if (mNextPage <= 2) {
            mAdapter.setMediaList(event.mediaList);
        } else {
            mAdapter.addMediaList(event.mediaList);
        }
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
            mSelectedItems.clear();
            notifyDataSetChanged();
        }

        void addMediaList(@NonNull List<StockMediaModel> mediaList) {
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
            // TODO: not sure we can guarantee uniqueness
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
            holder.imageView.setImageUrl(imageUrl, WPNetworkImageView.ImageType.PHOTO);

            boolean isSelected = isItemSelected(position);
            holder.selectionCountTextView.setSelected(isSelected);
            if (isSelected) {
                int count = mSelectedItems.indexOf(position) + 1;
                holder.selectionCountTextView.setText(Integer.toString(count));
            } else {
                holder.selectionCountTextView.setText(null);
            }

            float scale = isSelected ? SCALE_SELECTED : SCALE_NORMAL;
            if (holder.imageView.getScaleX() != scale) {
                holder.imageView.setScaleX(scale);
                holder.imageView.setScaleY(scale);
            }

            if (mCanLoadMore && !mIsFetching && position == getItemCount() - 1) {
                requestStockPhotos(mSearchQuery, mNextPage);
            }
        }

        boolean isValidPosition(int position) {
            return position >= 0 && position < getItemCount();
        }

        void setInMultiSelect(boolean value) {
            if (mInMultiSelect != value) {
                mInMultiSelect = value;
                clearSelection();
            }
        }

        void clearSelection() {
            if (mSelectedItems.size() > 0) {
                mSelectedItems.clear();
                notifyDataSetChanged();
            }
        }

        boolean isItemSelected(int position) {
            return mSelectedItems.contains(position);
        }

        void setItemSelected(StockViewHolder holder, int position, boolean selected) {
            if (!isValidPosition(position)) return;

            if (selected) {
                mSelectedItems.add(position);
            } else if (mSelectedItems.contains(position)) {
                mSelectedItems.remove(position);
            } else {
                return;
            }

            // show and animate the count
            if (selected) {
                holder.selectionCountTextView.setText(Integer.toString(mSelectedItems.indexOf(position) + 1));
            } else {
                holder.selectionCountTextView.setText(null);
            }
            AniUtils.startAnimation(holder.selectionCountTextView, R.anim.pop);
            holder.selectionCountTextView.setVisibility(selected ? View.VISIBLE : View.GONE);

            // scale the thumbnail
            if (selected) {
                AniUtils.scale(holder.imageView, SCALE_NORMAL, SCALE_SELECTED, AniUtils.Duration.SHORT);
            } else {
                AniUtils.scale(holder.imageView, SCALE_SELECTED, SCALE_NORMAL, AniUtils.Duration.SHORT);
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
        }

        private void setSelectedItems(ArrayList<Integer> selectedItems) {
            mSelectedItems.clear();
            mSelectedItems.addAll(selectedItems);
            notifyDataSetChanged();
        }
    }

    class StockViewHolder extends RecyclerView.ViewHolder {
        private final WPNetworkImageView imageView;
        private final TextView selectionCountTextView;

        public StockViewHolder(View view) {
            super(view);

            imageView = view.findViewById(R.id.image_thumbnail);
            imageView.getLayoutParams().width = mThumbWidth;
            imageView.getLayoutParams().height = mThumbHeight;

            selectionCountTextView = view.findViewById(R.id.text_selection_count);
            selectionCountTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (mAdapter.isValidPosition(position)) {
                        mAdapter.setInMultiSelect(true);
                        mAdapter.toggleItemSelected(StockViewHolder.this, position);
                    }
                }
            });
        }
    }

    private int getColumnCount() {
        return DisplayUtils.isLandscape(this) ? 4 : 3;
    }

}
