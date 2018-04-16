package org.wordpress.android.ui.stockmedia;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import org.wordpress.android.fluxc.model.StockMediaModel;

import java.util.ArrayList;
import java.util.List;

public class StockMediaRetainedFragment extends Fragment {
    static StockMediaRetainedFragment newInstance() {
        return new StockMediaRetainedFragment();
    }

    static class StockMediaRetainedData {
        private final List<StockMediaModel> mStockMediaList;
        private final ArrayList<Integer> mSelectedItems;
        private final boolean mCanLoadMore;
        private final int mNextPage;

        StockMediaRetainedData(@NonNull List<StockMediaModel> stockMediaList,
                               @NonNull ArrayList<Integer> selectedItems,
                               boolean canLoadMore,
                               int nextPage) {
            mStockMediaList = stockMediaList;
            mSelectedItems = selectedItems;
            mCanLoadMore = canLoadMore;
            mNextPage = nextPage;
        }

        @NonNull List<StockMediaModel> getStockMediaList() {
            return mStockMediaList;
        }

        @NonNull List<Integer> getSelectedItems() {
            return mSelectedItems;
        }

        boolean canLoadMore() {
            return mCanLoadMore;
        }

        int getNextPage() {
            return mNextPage;
        }
    }

    private StockMediaRetainedData mData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable StockMediaRetainedData getData() {
        return mData;
    }

    void setData(@Nullable StockMediaRetainedData data) {
        mData = data;
    }
}
