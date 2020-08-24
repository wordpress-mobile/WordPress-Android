package org.wordpress.android.ui.stockmedia;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.StockMediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.StockMediaModel;
import org.wordpress.android.fluxc.store.MediaStore.OnStockMediaUploaded;
import org.wordpress.android.fluxc.store.MediaStore.UploadStockMediaPayload;
import org.wordpress.android.fluxc.store.StockMediaStore;
import org.wordpress.android.fluxc.store.StockMediaStore.FetchStockMediaListPayload;
import org.wordpress.android.fluxc.store.StockMediaStore.OnStockMediaListFetched;
import org.wordpress.android.ui.ActionableEmptyView;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaPreviewActivity;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity;
import org.wordpress.android.ui.stockmedia.StockMediaRetainedFragment.StockMediaRetainedData;
import org.wordpress.android.util.AccessibilityUtils;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PhotoPickerUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ViewUtilsKt;
import org.wordpress.android.util.WPLinkMovementMethod;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class StockMediaPickerActivity extends LocaleAwareActivity implements SearchView.OnQueryTextListener {
    private static final int MIN_SEARCH_QUERY_SIZE = 3;
    private static final String TAG_RETAINED_FRAGMENT = "retained_fragment";

    private static final String KEY_SEARCH_QUERY = "search_query";
    private static final String KEY_IS_SHOWING_EMPTY_VIEW = "is_showing_empty_view";
    private static final String KEY_IS_UPLOADING = "is_uploading";
    public static final String KEY_REQUEST_CODE = "request_code";
    public static final String KEY_UPLOADED_MEDIA_IDS = "uploaded_media_ids";

    private SiteModel mSite;

    private StockMediaAdapter mAdapter;
    private StockMediaRetainedFragment mRetainedFragment;
    private ProgressDialog mProgressDialog;

    private RecyclerView mRecycler;
    private ViewGroup mSelectionBar;
    private TextView mTextAdd;
    private TextView mTextPreview;

    private SearchView mSearchView;
    private String mSearchQuery;
    private final Handler mHandler = new Handler();

    private int mThumbWidth;
    private int mThumbHeight;

    private boolean mIsFetching;
    private boolean mIsShowingEmptyView;
    private boolean mIsUploading;
    private boolean mCanLoadMore;

    private int mNextPage;
    private int mRequestCode;

    @SuppressWarnings("unused")
    @Inject StockMediaStore mStockMediaStore;
    @Inject Dispatcher mDispatcher;
    @Inject ImageManager mImageManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        setContentView(R.layout.media_picker_activity);

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
        mTextPreview = findViewById(R.id.text_preview);

        if (savedInstanceState == null) {
            showEmptyView(true);
            mRequestCode = getIntent().getIntExtra(KEY_REQUEST_CODE, 0);
        } else {
            mSearchQuery = savedInstanceState.getString(KEY_SEARCH_QUERY);
            mIsUploading = savedInstanceState.getBoolean(KEY_IS_UPLOADING);
            mIsShowingEmptyView = savedInstanceState.getBoolean(KEY_IS_SHOWING_EMPTY_VIEW);
            mRequestCode = savedInstanceState.getInt(KEY_REQUEST_CODE);

            if (mIsShowingEmptyView) {
                showEmptyView(true);
            }

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

        mTextAdd.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (null != mSite && mSite.hasDiskSpaceQuotaInformation() && mSite.getSpaceAvailable() <= 0) {
                    ToastUtils.showToast(StockMediaPickerActivity.this, R.string.error_media_quota_exceeded_toast);
                    return;
                }
                uploadSelection();
            }
        });

        if (isMultiSelectEnabled()) {
            mTextPreview.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    previewSelection();
                }
            });
        } else {
            mTextAdd.setText(R.string.photo_picker_use_photo);
            mTextPreview.setVisibility(View.GONE);
        }

        configureSearchView();
    }

    @Override
    protected void onDestroy() {
        if (mSearchView != null) {
            mSearchView.setOnQueryTextListener(null);
            mSearchView.setOnCloseListener(null);
        }
        showUploadProgressDialog(false);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_IS_SHOWING_EMPTY_VIEW, mIsShowingEmptyView);
        outState.putBoolean(KEY_IS_UPLOADING, mIsUploading);
        outState.putInt(KEY_REQUEST_CODE, mRequestCode);

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

    private boolean isMultiSelectEnabled() {
        return mRequestCode == RequestCodes.STOCK_MEDIA_PICKER_MULTI_SELECT;
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
            mIsShowingEmptyView = show;
            ActionableEmptyView actionableEmptyView = findViewById(R.id.actionable_empty_view);
            actionableEmptyView.setVisibility(show ? View.VISIBLE : View.GONE);
            actionableEmptyView.updateLayoutForSearch(true, 0);

            if (show) {
                boolean isEmpty = mSearchQuery == null || mSearchQuery.length() < MIN_SEARCH_QUERY_SIZE;

                if (isEmpty) {
                    actionableEmptyView.title.setText(R.string.stock_media_picker_initial_empty_text);
                    String link = "<a href='https://pexels.com/'>Pexels</a>";
                    Spanned html = Html.fromHtml(getString(R.string.stock_media_picker_initial_empty_subtext, link));
                    actionableEmptyView.subtitle.setText(html);
                    actionableEmptyView.getSubtitle().setMovementMethod(WPLinkMovementMethod.getInstance());
                    actionableEmptyView.subtitle.setVisibility(View.VISIBLE);
                } else {
                    actionableEmptyView.title.setText(R.string.media_empty_search_list);
                    actionableEmptyView.subtitle.setVisibility(View.GONE);
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

        FetchStockMediaListPayload payload = new FetchStockMediaListPayload(searchQuery, page);
        mDispatcher.dispatch(StockMediaActionBuilder.newFetchStockMediaAction(payload));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStockMediaListFetched(OnStockMediaListFetched event) {
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

        AnalyticsTracker.track(AnalyticsTracker.Stat.STOCK_MEDIA_SEARCHED);

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
    public void onStockMediaUploaded(OnStockMediaUploaded event) {
        mIsUploading = false;
        showUploadProgressDialog(false);

        if (event.isError()) {
            ToastUtils.showToast(this, R.string.media_upload_error);
            AppLog.e(AppLog.T.MEDIA, "An error occurred while uploading stock media");
        } else {
            trackUploadedStockMediaEvent(event.mediaList);
            int count = event.mediaList.size();
            if (count == 0) {
                AppLog.w(AppLog.T.MEDIA, "No stock media chosen");
                return;
            }

            Intent intent = new Intent();
            if (isMultiSelectEnabled()) {
                long[] idArray = new long[count];
                for (int i = 0; i < count; i++) {
                    idArray[i] = event.mediaList.get(i).getMediaId();
                }
                intent.putExtra(KEY_UPLOADED_MEDIA_IDS, idArray);
            } else {
                intent.putExtra(PhotoPickerActivity.EXTRA_MEDIA_ID, event.mediaList.get(0).getMediaId());
            }

            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void trackUploadedStockMediaEvent(@NonNull List<MediaModel> mediaList) {
        if (mediaList.size() == 0) {
            AppLog.e(AppLog.T.MEDIA, "Cannot track uploaded stock media event if mediaList is empty");
            return;
        }

        boolean isMultiselect = mediaList.size() > 1;
        Map<String, Object> properties = new HashMap<>();
        properties.put("is_part_of_multiselection", isMultiselect);
        properties.put("number_of_media_selected", mediaList.size());
        AnalyticsTracker.track(AnalyticsTracker.Stat.STOCK_MEDIA_UPLOADED, properties);
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
            if (isMultiSelectEnabled()) {
                String labelAdd = String.format(getString(R.string.add_count), numSelected);
                mTextAdd.setText(labelAdd);

                String labelPreview = String.format(getString(R.string.preview_count), numSelected);
                mTextPreview.setText(labelPreview);
            }
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
        MediaPreviewActivity.showPreview(this, null, imageUrlList, imageUrlList.get(0));
    }

    private void uploadSelection() {
        if (!NetworkUtils.checkConnection(this)) return;

        mIsUploading = true;
        showUploadProgressDialog(true);
        List<StockMediaModel> items = mAdapter.getSelectedStockMedia();
        UploadStockMediaPayload payload = new UploadStockMediaPayload(mSite, items);
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

        @NonNull
        @Override
        public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.media_picker_thumbnail, parent, false);
            return new StockViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
            StockMediaModel media = mItems.get(position);
            String imageUrl = PhotonUtils.getPhotonImageUrl(media.getThumbnail(), mThumbWidth, mThumbHeight);
            mImageManager.load(holder.mImageView, ImageType.PHOTO, imageUrl, ScaleType.CENTER_CROP);

            holder.mImageView.setContentDescription(media.getTitle());


            boolean isSelected = isItemSelected(position);
            holder.mSelectionCountTextView.setSelected(isSelected);
            if (isMultiSelectEnabled()) {
                if (isSelected) {
                    int count = mSelectedItems.indexOf(position) + 1;
                    String label = Integer.toString(count);
                    holder.mSelectionCountTextView.setText(label);
                } else {
                    holder.mSelectionCountTextView.setText(null);
                }
            } else {
                holder.mSelectionCountTextView.setVisibility(
                        isSelected || isMultiSelectEnabled() ? View.VISIBLE : View.GONE);
            }

            float scale = isSelected ? SCALE_SELECTED : SCALE_NORMAL;
            if (holder.mImageView.getScaleX() != scale) {
                holder.mImageView.setScaleX(scale);
                holder.mImageView.setScaleY(scale);
            }

            if (mCanLoadMore && position == getItemCount() - 1) {
                fetchStockMedia(mSearchQuery, mNextPage);
            }
            addImageSelectedToAccessibilityFocusedEvent(holder.mImageView, position);
        }

        private void addImageSelectedToAccessibilityFocusedEvent(ImageView imageView, int position) {
            AccessibilityUtils.addPopulateAccessibilityEventFocusedListener(imageView, event -> {
                if (isValidPosition(position)) {
                    if (isItemSelected(position)) {
                        final String imageSelectedText = imageView.getContext().getString(
                                R.string.photo_picker_image_selected);
                        if (!imageView.getContentDescription().toString().contains(imageSelectedText)) {
                            imageView.setContentDescription(
                                    imageView.getContentDescription() + " "
                                    + imageSelectedText);
                        }
                    }
                }
            });
        }

        boolean isValidPosition(int position) {
            return position >= 0 && position < getItemCount();
        }

        boolean isItemSelected(int position) {
            return mSelectedItems.contains(position);
        }

        void setItemSelected(StockViewHolder holder, int position, boolean selected) {
            if (!isValidPosition(position)) return;

            // if this is single select, make sure to deselect any existing selection
            if (selected && !isMultiSelectEnabled() && !mSelectedItems.isEmpty()) {
                int prevPosition = mSelectedItems.get(0);
                StockViewHolder prevHolder = (StockViewHolder) mRecycler.findViewHolderForAdapterPosition(prevPosition);
                if (prevHolder != null) {
                    setItemSelected(prevHolder, prevPosition, false);
                } else {
                    // holder may be null if not laid out
                    mSelectedItems.clear();
                }
            }

            if (selected) {
                mSelectedItems.add(position);
            } else {
                int index = mSelectedItems.indexOf(position);
                if (index == -1) {
                    return;
                }
                mSelectedItems.remove(index);
            }

            // show and animate the count bubble
            if (isMultiSelectEnabled()) {
                if (selected) {
                    String label = Integer.toString(mSelectedItems.indexOf(position) + 1);
                    holder.mSelectionCountTextView.setText(label);
                } else {
                    holder.mSelectionCountTextView.setText(null);
                }
                AniUtils.startAnimation(holder.mSelectionCountTextView, R.anim.pop);
            } else {
                if (selected) {
                    AniUtils.scaleIn(holder.mSelectionCountTextView, AniUtils.Duration.MEDIUM);
                } else {
                    AniUtils.scaleOut(holder.mSelectionCountTextView, AniUtils.Duration.MEDIUM);
                }
            }

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
            PhotoPickerUtils.announceSelectedImageForAccessibility(holder.mImageView, !isSelected);
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
        private final ImageView mImageView;
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
            ViewUtilsKt.redirectContextClickToLongPressListener(mImageView);
        }
    }

    private int getColumnCount() {
        return DisplayUtils.isLandscape(this) ? 4 : 3;
    }
}
