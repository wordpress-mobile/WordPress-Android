package org.wordpress.android.ui.menus.views;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.models.MenuModel;
import org.wordpress.android.networking.menus.MenusDataModeler;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.ui.menus.event.MenuEvents;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.widgets.RecyclerItemDecoration;

import java.util.List;

import de.greenrobot.event.EventBus;

public class MenuItemsView extends RelativeLayout {

    private RecyclerView mRecyclerView;
    private TextView mEmptyView;
    private MenuModel mMenu;
    private List<MenuItemModel> mFlattenedList;

    private MenuItemAdapter mItemAdapter;
    private MenuItemAdapter.MenuItemInteractions mListener;
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

    public void setListener(MenuItemAdapter.MenuItemInteractions listener) {
        mListener = listener;
    }

    public MenuItemAdapter getAdapter(){
        if (mItemAdapter == null) {
            mItemAdapter = new MenuItemAdapter(getContext(), mListener);
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

        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            int dragFrom = -1;
            int dragTo = -1;

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return true;
            }

            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                if (dragFrom == -1) {
                    dragFrom =  fromPosition;
                }
                dragTo = toPosition;
                mItemAdapter.onItemMove(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                mItemAdapter.deleteMenuItem(viewHolder.getAdapterPosition());
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                if(dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                    mItemAdapter.onDrop(dragFrom, dragTo);
                }

                dragFrom = dragTo = -1;
            }

        });
        touchHelper.attachToRecyclerView(mRecyclerView);
    }

    public void setMenu(MenuModel menu){
        mMenu = menu;
        getAdapter();
        if (menu != null) {
            mFlattenedList = MenusDataModeler.flattenMenuItemModelList(menu.menuItems, 0);
            mItemAdapter.replaceMenuItems(mFlattenedList);
        }
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

    public List<MenuItemModel> getCurrentMenuItems(){
        if (mItemAdapter == null) return null;
        return mItemAdapter.getCurrentMenuItems();
    }

    private boolean hasAdapter() {
        return (mItemAdapter != null);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(MenuEvents.AddMenuClicked event) {
        mRecyclerView.scrollToPosition(event.mPosition);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }
}
