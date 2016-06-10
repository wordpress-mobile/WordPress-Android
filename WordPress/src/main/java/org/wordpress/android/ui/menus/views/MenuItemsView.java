package org.wordpress.android.ui.menus.views;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuModel;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.widgets.RecyclerItemDecoration;


public class MenuItemsView extends RelativeLayout {

    private RecyclerView mRecyclerView;
    private TextView mEmptyView;
    private MenuModel mMenu;

    private MenuItemAdapter mItemAdapter;
    private AppLog.T mTAG;

    public MenuItemsView(Context context) {
        super(context);
        init();
    }

    public MenuItemsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MenuItemsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setAdapter(MenuItemAdapter adapter){
        mItemAdapter = adapter;
        mRecyclerView.setAdapter(adapter);
    }

    public MenuItemAdapter getAdapter(){
        if (mItemAdapter == null) {
            mItemAdapter = new MenuItemAdapter(getContext());
            mRecyclerView.setAdapter(mItemAdapter);
        }
        return mItemAdapter;
    }

    public void setLogT(AppLog.T tag){
        mTAG = tag;
    }

    private void init() {
        inflate(getContext(), R.layout.menu_items_recyclerview_component, this);

        int spacingHorizontal = 0;
        int spacingVertical = DisplayUtils.dpToPx(getContext(), 1);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.addItemDecoration(new RecyclerItemDecoration(spacingHorizontal, spacingVertical));

        mEmptyView = (TextView) findViewById(R.id.empty_view);

    }

    public void setMenu(MenuModel menu){
        mMenu = menu;
        getAdapter();
        if (menu != null) {
            mItemAdapter.replaceMenuItems(menu.menuItems);
        }
    }

    private boolean hasAdapter() {
        return (mItemAdapter != null);
    }

    public boolean emptyViewIsVisible(){
        return (mEmptyView != null && mEmptyView.getVisibility() == View.VISIBLE);
    }

    public void hideEmptyView() {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    public void updateEmptyView(EmptyViewMessageType emptyViewMessageType) {
        if (mEmptyView == null) return;

        if ((hasAdapter() && mItemAdapter.getItemCount() == 0) || !hasAdapter()) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }
    }

}
