package org.wordpress.android.ui;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.media.MediaGridAdapter;

/**
 * A GridView implementation that aims to do multiselect on GridViews since
 * multi-select isn't supported pre-API 11. 
 *
 */
public class MultiSelectGridView extends GridView implements  AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener {

    private OnItemClickListener mOnItemClickListener;
    private MultiSelectListener mMultiSelectListener;
    private MediaGridAdapter mAdapter;
    private boolean mIsInMultiSelectMode;
    private boolean mIsMultiSelectModeEnabled;
    private boolean mIsHighlightSelectModeEnabled;
    
    public interface MultiSelectListener {
        public void onMultiSelectChange(int count);
    }
    
    public MultiSelectGridView(Context context) {
        super(context);
        init();
    }
    
    public MultiSelectGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public MultiSelectGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        super.setOnItemClickListener(this);
        super.setOnItemLongClickListener(this);
        mIsMultiSelectModeEnabled = true;
        mIsHighlightSelectModeEnabled = false;
    }

    public void setMultiSelectModeActive(boolean active) {
        mIsInMultiSelectMode = active;
    }
    
    public boolean isInMultiSelectMode(){
        return mIsInMultiSelectMode ;
    }
    
    public void setMultiSelectModeEnabled(boolean enabled) {
        mIsMultiSelectModeEnabled = enabled;
    }
    
    public boolean isMultiSelectModeEnabled() {
        return mIsMultiSelectModeEnabled;
    }
    
    public void setHighlightSelectModeEnabled(boolean enabled) {
        mIsHighlightSelectModeEnabled = enabled;
    }
    
    public boolean isHighlightSelectModeEnabled() {
        return mIsHighlightSelectModeEnabled;
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        CheckableFrameLayout frameLayout = ((CheckableFrameLayout) view.findViewById(R.id.media_grid_frame_layout));

        Cursor cursor = ((CursorAdapter) parent.getAdapter()).getCursor();
        
        int mediaIdCol = cursor.getColumnIndex("mediaId");
        if (mediaIdCol == -1)
            return;
        
        String mediaId = cursor.getString(mediaIdCol);
        
        // run the default behavior if not in multiselect mode
        if (!isInMultiSelectMode()) {            
            getSelectedItems().clear();
            getSelectedItems().add(mediaId);
            frameLayout.setChecked(true);
            if (mOnItemClickListener != null)
                mOnItemClickListener.onItemClick(parent, view, position, id);
            mAdapter.notifyDataSetChanged();
            
            if (isHighlightSelectModeEnabled())
                notifyMultiSelectCountChanged();
            
            return;
        }
        
        if (getSelectedItems().contains(mediaId)) {
            // unselect item
            frameLayout.setChecked(false);
        } else { 
            // select item
            frameLayout.setChecked(true);
        }
        notifyMultiSelectCountChanged();

    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        
        // do not allow item long clicks if multi-select is disabled
        if (!mIsMultiSelectModeEnabled)
            return true;
        
        mIsInMultiSelectMode = true;

        Cursor cursor = ((CursorAdapter) parent.getAdapter()).getCursor();
        String mediaId = cursor.getString(cursor.getColumnIndex("mediaId"));
        
        if (!getSelectedItems().contains(mediaId))
            getSelectedItems().add(mediaId);
        notifyMultiSelectCountChanged();
        
        ((CheckableFrameLayout) view.findViewById(R.id.media_grid_frame_layout)).setChecked(true);
        
        return true;
    }

    private void notifyMultiSelectCountChanged() {
        if (mMultiSelectListener != null) {
            int size = getSelectedItems().size();
            if (size == 0) {
                mIsInMultiSelectMode = false;
            }
            mMultiSelectListener.onMultiSelectChange(size);
        }
    }

    @Override
    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }
    
    @Override
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        // not implemented
    }

    public void setMultiSelectListener(MultiSelectListener listener) {
        mMultiSelectListener = listener;
    }
    
    public void cancelSelection() {
        getSelectedItems().clear();
        mAdapter.notifyDataSetChanged();
        notifyMultiSelectCountChanged();
    }
    
    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        mAdapter = (MediaGridAdapter) adapter;
    }
    
    private ArrayList<String> getSelectedItems() {
        return mAdapter.getCheckedItems();
    }
}
