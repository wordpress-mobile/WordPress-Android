package org.wordpress.android.ui.stockmedia;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.StockMediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.StockMediaModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.StockMediaStore;
import org.wordpress.android.ui.media.MediaPreviewActivity;
import org.wordpress.android.ui.stockmedia.StockMediaRetainedFragment.StockMediaRetainedData;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPLinkMovementMethod;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class StockMediaPickerActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    private static final int MIN_SEARCH_QUERY_SIZE = 3;
    private static final String TAG_RETAINED_FRAGMENT = "retained_fragment";

    private static final String KEY_SEARCH_QUERY = "search_query";
    private static final String KEY_IS_UPLOADING = "is_uploading";
    public static final String KEY_UPLOADED_MEDIA_IDS = "uploaded_media_ids";

    private SiteModel mSite;

    private StockMediaAdapter mAdapter;
    private StockMediaRetainedFragment mRetainedFragment;
    private ProgressDialog mProgressDialog;

    private RecyclerView mRecycler;
    private ViewGroup mSelectionBar;
    private TextView mTextAdd;

    private SearchView mSearchView;
    private String mSearchQuery;
    private final Handler mHandler = new Handler();

    private int mThumbWidth;
    private int mThumbHeight;

    private boolean mIsFetching;
    private boolean mIsUploading;
    private boolean mCanLoadMore;
    private int mNextPage;

    @SuppressWarnings("unused")
    @Inject StockMediaStore mStockMediaStore;
    @Inject Dispatcher mDispatcher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        setContentView(R.layout.stock_media_picker_activity);

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

        FragmentManager fm = getSupportFragmentManager();
        mRetainedFragment = (StockMediaRetainedFragment) fm.findFragmentByTag(TAG_RETAINED_FRAGMENT);
        if (mRetainedFragment == null) {
            mRetainedFragment = StockMediaRetainedFragment.newInstance();
            fm.beginTransaction().add(mRetainedFragment, TAG_RETAINED_FRAGMENT).commit();
        }

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

        mRecycler = findViewById(R.id.recycler);
        mRecycler.setLayoutManager(new GridLayoutManager(this, getColumnCount()));

        mAdapter = new StockMediaAdapter();
        mRecycler.setAdapter(mAdapter);

        mSelectionBar = findViewById(R.id.container_selection_bar);
        mTextAdd = findViewById(R.id.text_add);
        mTextAdd.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                uploadSelection();
            }
        });
        findViewById(R.id.text_preview).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                previewSelection();
            }
        });

        if (savedInstanceState == null) {
            showEmptyView(true);
        } else {
            mSearchQuery = savedInstanceState.getString(KEY_SEARCH_QUERY);
            mIsUploading = savedInstanceState.getBoolean(KEY_IS_UPLOADING);
            if (mIsUploading) {
                showUploadProgressDialog(true);
            }
            if (!TextUtils.isEmpty(mSearchQuery)) {
                StockMediaRetainedData data = mRetainedFragment.getData();
                if (data != null) {
                    mCanLoadMore = data.canLoadMore();
                    mNextPage = data.getNextPage();
                    mAdapter.setMediaList(data.getStockMediaList());
                    mAdapter.setSelectedItems(data.getSelectedItems());
                } else {
                    submitSearch(mSearchQuery, true);
                }
            }
        }

        configureSearchView();
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
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_IS_UPLOADING, mIsUploading);

        if (mSite != null) {
            outState.putSerializable(WordPress.SITE, mSite);
        }

        String query = mSearchView != null ? mSearchView.getQuery().toString() : null;
        outState.putString(KEY_SEARCH_QUERY, query);

        StockMediaRetainedData data = new StockMediaRetainedData(mAdapter.mItems,
                mAdapter.mSelectedItems,
                mCanLoadMore,
                mNextPage);
        mRetainedFragment.setData(data);
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        mDispatcher.unregister(this);
        super.onStop();
    }

    @Override
    public void onPause() {
        if (isFinishing() && mRetainedFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(mRetainedFragment).commit();
        }
        super.onPause();
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
        if (!StringUtils.equals(query, mSearchQuery)) {
            submitSearch(query, true);
        }
        return true;
    }

    private void configureSearchView() {
        mSearchView = findViewById(R.id.search_view);

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
                    String message = getString(R.string.stock_media_picker_initial_empty_text);
                    String subMessage = getString(R.string.stock_media_picker_initial_empty_subtext);
                    String link = "<a href='https://pexels.com/'>Pexels</a>";
                    String html = message
                                  + "<br /><br />"
                                  + "<small>" + String.format(subMessage, link) + "</small>";
                    txtEmpty.setMovementMethod(WPLinkMovementMethod.getInstance());
                    txtEmpty.setText(Html.fromHtml(html));
                } else {
                    txtEmpty.setText(R.string.stock_media_picker_empty_results);
                }
            }
        }
    }

    private void showProgress(boolean show) {
        if (!isFinishing()) {
            findViewById(R.id.progress).setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showUploadProgressDialog(boolean show) {
        if (show) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage(getString(R.string.uploading_media));
            mProgressDialog.show();
        } else if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private void submitSearch(@Nullable final String query, boolean delayed) {
        mSearchQuery = query;

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
            fetchStockMedia(query, 1);
        }
    }

    private void fetchStockMedia(@Nullable String searchQuery, int page) {
        // We should always fetch the first page, but We should only load more pages if we are not
        // already fetching anything
        if ((mIsFetching && page != 1) || !NetworkUtils.checkConnection(this)) {
            return;
        }

        mSearchQuery = searchQuery;

        if (mSearchQuery == null || mSearchQuery.length() < MIN_SEARCH_QUERY_SIZE) {
            mIsFetching = false;
            showProgress(false);
            mAdapter.clear();
            showEmptyView(true);
            return;
        }

        if (page == 1) {
            mAdapter.clear();
        }

        showProgress(true);
        mIsFetching = true;
        showEmptyView(false);

        AppLog.d(AppLog.T.MEDIA, "Fetching stock media page " + page);

        StockMediaStore.FetchStockMediaListPayload payload =
                new StockMediaStore.FetchStockMediaListPayload(searchQuery, page);
        mDispatcher.dispatch(StockMediaActionBuilder.newFetchStockMediaAction(payload));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStockMediaListFetched(StockMediaStore.OnStockMediaListFetched event) {
        // make sure these results are for the same query
        if (mSearchQuery == null || !mSearchQuery.equals(event.searchTerm)) {
            return;
        }

        mIsFetching = false;
        showProgress(false);

        if (event.isError()) {
            AppLog.e(AppLog.T.MEDIA, "An error occurred while searching stock media");
            ToastUtils.showToast(this, R.string.media_generic_error);
            mCanLoadMore = false;
            return;
        }

        mNextPage = event.nextPage;
        mCanLoadMore = event.canLoadMore;

        // set the results to the event's mediaList if this is the first page, otherwise add to the existing results
        if (event.nextPage == 2) {
            mAdapter.setMediaList(event.mediaList);
        } else {
            mAdapter.addMediaList(event.mediaList);
        }

        showEmptyView(mAdapter.isEmpty() && !TextUtils.isEmpty(mSearchQuery));
    }


    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStockMediaUploaded(MediaStore.OnStockMediaUploaded event) {
        mIsUploading = false;
        showUploadProgressDialog(false);

        if (event.isError()) {
            ToastUtils.showToast(this, R.string.media_upload_error);
            AppLog.e(AppLog.T.MEDIA, "An error occurred while uploading stock media");
        } else {
            ArrayList<Integer> idList = new ArrayList<>();
            for (MediaModel media : event.mediaList) {
                idList.add(media.getId());
            }
            Intent intent = new Intent();
            intent.putIntegerArrayListExtra(KEY_UPLOADED_MEDIA_IDS, idList);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void showSelectionBar(final boolean show) {
        if (show && mSelectionBar.getVisibility() != View.VISIBLE) {
            AniUtils.animateBottomBar(mSelectionBar, true);
        } else if (!show && mSelectionBar.getVisibility() == View.VISIBLE) {
            AniUtils.animateBottomBar(mSelectionBar, false);
        } else {
            return;
        }

        // when the animation completes, adjust the relative layout params of the recycler to make
        // sure the bar doesn't overlap the bottom row when showing
        long msDelay = AniUtils.Duration.SHORT.toMillis(this);
        mHandler.postDelayed(new Runnable() {
            @Override public void run() {
                if (!isFinishing()) {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mRecycler.getLayoutParams();
                    if (show) {
                        params.addRule(RelativeLayout.ABOVE, R.id.container_selection_bar);
                    } else {
                        params.addRule(RelativeLayout.ABOVE, 0);
                    }
                }
            }
        }, msDelay);
    }

    private void notifySelectionCountChanged() {
        int numSelected = mAdapter.getSelectionCount();
        if (numSelected > 0) {
            String label = String.format(getString(R.string.add_count), numSelected);
            mTextAdd.setText(label);
            showSelectionBar(true);
            if (numSelected == 1) {
                ActivityUtils.hideKeyboardForced(mSearchView);
            }
        } else {
            showSelectionBar(false);
        }
    }

    private void previewSelection() {
        List<StockMediaModel> items = mAdapter.getSelectedStockMedia();
        if (items.size() == 0) return;

        ArrayList<String> imageUrlList = new ArrayList<>();
        for (StockMediaModel media : items) {
            imageUrlList.add(media.getUrl());
        }
        MediaPreviewActivity.showPreview(this, imageUrlList);
    }

    private void uploadSelection() {
        if (!NetworkUtils.checkConnection(this)) return;

        mIsUploading = true;
        showUploadProgressDialog(true);
        List<StockMediaModel> items = mAdapter.getSelectedStockMedia();
        MediaStore.UploadStockMediaPayload payload =
                new MediaStore.UploadStockMediaPayload(mSite, items);
        mDispatcher.dispatch(MediaActionBuilder.newUploadStockMediaAction(payload));
    }

    class StockMediaAdapter extends RecyclerView.Adapter<StockViewHolder> {
        private static final float SCALE_NORMAL = 1.0f;
        private static final float SCALE_SELECTED = .8f;

        private final List<StockMediaModel> mItems = new ArrayList<>();
        private final ArrayList<Integer> mSelectedItems = new ArrayList<>();

        StockMediaAdapter() {
            setHasStableIds(true);
        }

        void setMediaList(@NonNull List<StockMediaModel> mediaList) {
            mItems.clear();
            mItems.addAll(mediaList);
            notifyDataSetChanged();
        }

        void addMediaList(@NonNull List<StockMediaModel> mediaList) {
            mItems.addAll(mediaList);
            notifyDataSetChanged();
        }

        void clear() {
            mItems.clear();
            if (mSelectedItems.size() > 0) {
                mSelectedItems.clear();
                notifySelectionCountChanged();
            }
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

        boolean isEmpty() {
            return getItemCount() == 0;
        }

        @Override
        public StockViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.stock_media_picker_thumbnail, parent, false);
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

            if (mCanLoadMore && position == getItemCount() - 1) {
                fetchStockMedia(mSearchQuery, mNextPage);
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
            long delayMs = AniUtils.Duration.SHORT.toMillis(StockMediaPickerActivity.this);
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

        @SuppressWarnings("unused")
        private List<StockMediaModel> getSelectedStockMedia() {
            List<StockMediaModel> items = new ArrayList<>();
            for (int i : mSelectedItems) {
                items.add(mItems.get(i));
            }
            return items;
        }

        private void setSelectedItems(@NonNull List<Integer> selectedItems) {
            if (mSelectedItems.isEmpty() && selectedItems.isEmpty()) {
                return;
            }

            mSelectedItems.clear();
            mSelectedItems.addAll(selectedItems);
            notifyDataSetChanged();
            notifySelectionCountChanged();
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
