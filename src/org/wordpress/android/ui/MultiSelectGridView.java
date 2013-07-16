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

import org.wordpress.android.ui.media.MediaGridAdapter;

/**
 * A GridView implementation that aims to do multiselect on GridViews since
 * multi-select isn't supported pre-API 11. 
 *
 */
public class MultiSelectGridView extends GridView implements AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener {

    private OnItemClickListener mOnItemClickListener;
    private MultiSelectListener mMultiSelectListener;
    private MediaGridAdapter mAdapter;
    
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
    }

    private boolean isInMultiSelectMode(){
        return getSelectedItems().size() > 0;
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        
        // run the default behavior if not in multiselect mode
        if (!isInMultiSelectMode()) {
            mOnItemClickListener.onItemClick(parent, view, position, id);
            return;
        }
        
        Cursor cursor = ((CursorAdapter) parent.getAdapter()).getCursor();
        String mediaId = cursor.getString(cursor.getColumnIndex("mediaId"));

        CheckableFrameLayout frameLayout = ((CheckableFrameLayout) view);        
        
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
        if (isInMultiSelectMode())
            return false;

        Cursor cursor = ((CursorAdapter) parent.getAdapter()).getCursor();
        String mediaId = cursor.getString(cursor.getColumnIndex("mediaId"));
        
        getSelectedItems().add(mediaId);
        notifyMultiSelectCountChanged();
        
        ((CheckableFrameLayout) view).setChecked(true);
        
        return true;
    }

    private void notifyMultiSelectCountChanged() {
        if (mMultiSelectListener != null)
            mMultiSelectListener.onMultiSelectChange(getSelectedItems().size());
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
