package org.wordpress.android.ui.stockmedia;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.wordpress.android.fluxc.model.StockMediaModel;

import java.util.ArrayList;
import java.util.List;

public class StockMediaRetainedFragment extends Fragment {
    private final List<StockMediaModel> mStockMediaList = new ArrayList<>();
    private final List<Integer> mSelectedItems = new ArrayList<>();

    static StockMediaRetainedFragment newInstance() {
        return new StockMediaRetainedFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @NonNull List<StockMediaModel> getStockMediaList() {
        return mStockMediaList;
    }

    void setStockMediaList(@NonNull List<StockMediaModel> mediaList) {
        mStockMediaList.clear();
        mStockMediaList.addAll(mediaList);
    }

    @NonNull List<Integer> getSelectedItems() {
        return mSelectedItems;
    }

    void setSelectedItems(@NonNull List<Integer> items) {
        mSelectedItems.clear();
        mSelectedItems.addAll(items);
    }
}
