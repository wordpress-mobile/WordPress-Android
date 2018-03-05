package org.wordpress.android.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.StockMediaActionBuilder;
import org.wordpress.android.fluxc.model.StockMediaModel;
import org.wordpress.android.fluxc.store.StockMediaStore;
import org.wordpress.android.util.AppLog;

import java.util.List;

import javax.inject.Inject;

public class StockMediaViewModel extends ViewModel {
    private static final String KEY_SEARCH_QUERY = "KEY_SEARCH_QUERY";

    private final Dispatcher mDispatcher;
    private final StockMediaStore mStockMediaStore;

    private String mSearchQuery;
    private boolean mIsFetching;
    private boolean mCanLoadMore;
    private int mNextPage;

    private final Handler mHandler;
    private final MutableLiveData<List<StockMediaModel>> mSearchResults;

    @SuppressWarnings("WeakerAccess")
    @Inject
    public StockMediaViewModel(@NonNull Dispatcher dispatcher, @NonNull StockMediaStore stockMediaStore) {
        super();
        mDispatcher = dispatcher;
        mStockMediaStore = stockMediaStore;

        mDispatcher.register(this);

        mHandler = new Handler();
        mSearchResults = new MutableLiveData<>();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDispatcher.unregister(this);
    }

    public void writeToBundle(@NonNull Bundle outState) {
        outState.putString(KEY_SEARCH_QUERY, mSearchQuery);
    }

    public void readFromBundle(@NonNull Bundle savedInstanceState) {
        mSearchQuery = savedInstanceState.getString(KEY_SEARCH_QUERY);
    }

    public void fetchStockPhotos(@Nullable String searchTerm, int page) {
        mIsFetching = true;
        mSearchQuery = searchTerm;
        StockMediaStore.FetchStockMediaListPayload payload =
                new StockMediaStore.FetchStockMediaListPayload(searchTerm, page);
        mDispatcher.dispatch(StockMediaActionBuilder.newFetchStockMediaAction(payload));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void OnStockMediaListFetched(StockMediaStore.OnStockMediaListFetched event) {
        mIsFetching = false;

        if (event.isError()) {
            AppLog.e(AppLog.T.MEDIA, "An error occurred while searching stock media");
            return;
        }
        if (mSearchQuery == null || !mSearchQuery.equals(event.searchTerm)) {
            return;
        }

        mNextPage = event.nextPage;
        mCanLoadMore = event.canLoadMore;
        mSearchResults.setValue(event.mediaList);
    }

    public int getNextPage() {
        return mNextPage;
    }

    public boolean canLoadMore() {
        return mCanLoadMore;
    }

    public String getSearchQuery() {
        return mSearchQuery;
    }

    public LiveData<List<StockMediaModel>> getSearchResults() {
        return mSearchResults;
    }
}
