package org.wordpress.android.ui.media;

import java.util.ArrayList;
import java.util.Collections;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.actionbarsherlock.app.SherlockFragment;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.media.MediaGridAdapter.MediaGridAdapterCallback;

public class MediaGalleryEditFragment extends SherlockFragment implements MediaGridAdapterCallback {

    private GridView mGridView;
    private MediaGridAdapter mGridAdapter;
    private ArrayList<String> mIds;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        
        mIds = new ArrayList<String>();
        
        mGridAdapter = new MediaGridAdapter(getActivity(), null, 0, new ArrayList<String>());
        mGridAdapter.setCallback(this);
        
        View view = inflater.inflate(R.layout.media_gallery_edit_fragment, container, false);
        
        mGridView = (GridView) view.findViewById(R.id.edit_media_gallery_gridview);
        mGridView.setAdapter(mGridAdapter);
        mGridView.setOnCreateContextMenuListener(this);
        refreshGridView();
        
        return view;
    }

    
    private void refreshGridView() {
        if (WordPress.getCurrentBlog() == null)
            return;
        
        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        
        Cursor cursor = WordPress.wpDB.getMediaFiles(blogId, mIds);
        
        if (cursor == null)
            return;
        
        
        SparseIntArray positions = mapIdsToCursorPositions(cursor);
        mGridAdapter.swapCursor(new OrderedCursor(cursor, positions));
    }


    private SparseIntArray mapIdsToCursorPositions(Cursor cursor) {
        SparseIntArray positions = new SparseIntArray();
        int size = mIds.size();
        for (int i = 0; i < size; i++) {
            while (cursor.moveToNext()) {
                String mediaId = cursor.getString(cursor.getColumnIndex("mediaId"));
                if (mediaId.equals(mIds.get(i))) {
                    positions.put(i, cursor.getPosition());
                    cursor.moveToPosition(-1);
                    break;
                }
            }
        }
        return positions;
    }

    @Override
    public void fetchMoreData(int offset) {
        // do nothing
    }

    @Override
    public void onRetryUpload(String mediaId) {
        // do nothing
    }

    @Override
    public boolean isInMultiSelect() {
        return false;
    }

    public void addMediaIds(ArrayList<String> ids) {
        if (ids == null)
            return;
        
        if (mIds == null) {
            mIds = ids;
        } else {
            mIds.addAll(ids);
        }
        refreshGridView();
    }

    public void setMediaIds(ArrayList<String> ids) {
        mIds = ids;
        refreshGridView();
    }
    
    public ArrayList<String> getMediaIds() {
        return mIds;
    }

    public void reverseIds() {
        Collections.reverse(mIds);
        refreshGridView();
    }
 
    private class OrderedCursor extends CursorWrapper {

        int mPos;
        private int mCount;
        
        // a map of custom position to cursor position
        private SparseIntArray mPositions;
        
        public OrderedCursor(Cursor cursor, SparseIntArray positions) {
            super(cursor);
            cursor.moveToPosition(-1);
            mPos = 0;
            mCount = cursor.getCount();
            mPositions = positions;
        }
        
        @Override
        public boolean move(int offset) {
            return this.moveToPosition(this.mPos+offset);
        }

        @Override
        public boolean moveToNext() {
            return this.moveToPosition(this.mPos+1);
        }

        @Override
        public boolean moveToPrevious() {
            return this.moveToPosition(this.mPos-1);
        }

        @Override
        public boolean moveToFirst() {
            return this.moveToPosition(0);
        }

        @Override
        public boolean moveToLast() {
            return this.moveToPosition(this.mCount-1);
        }
        
        @Override
        public boolean moveToPosition(int position) {
            return super.moveToPosition(mPositions.get(position));
        }

        
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Cursor cursor = mGridAdapter.getCursor();
        if (cursor == null)
            return;
        cursor.moveToPosition(info.position);
        String mediaId = cursor.getString(cursor.getColumnIndex("mediaId"));

        menu.add(ContextMenu.NONE, mIds.indexOf(mediaId), ContextMenu.NONE, R.string.delete);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int index = item.getItemId();
        mIds.remove(index);
        refreshGridView();
        return true;
    }
}
