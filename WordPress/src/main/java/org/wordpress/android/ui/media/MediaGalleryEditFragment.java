package org.wordpress.android.ui.media;

import android.app.Fragment;
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

import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.DragSortListView.DropListener;
import com.mobeta.android.dslv.DragSortListView.RemoveListener;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Fragment where containing a drag-sort listview where the user can drag items
 * to change their position in a media gallery
 */
public class MediaGalleryEditFragment extends Fragment implements DropListener, RemoveListener {
    private static final String SAVED_MEDIA_IDS = "SAVED_MEDIA_IDS";
    private MediaGalleryAdapter mGridAdapter;
    private ArrayList<String> mIds;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mIds = new ArrayList<String>();
        if (savedInstanceState != null) {
            mIds = savedInstanceState.getStringArrayList(SAVED_MEDIA_IDS);
        }

        mGridAdapter = new MediaGalleryAdapter(getActivity(), R.layout.media_gallery_item, null, true,
                MediaImageLoader.getInstance());

        View view = inflater.inflate(R.layout.media_gallery_edit_fragment, container, false);

        DragSortListView gridView = (DragSortListView) view.findViewById(R.id.edit_media_gallery_gridview);
        gridView.setAdapter(mGridAdapter);
        gridView.setOnCreateContextMenuListener(this);
        gridView.setDropListener(this);
        gridView.setRemoveListener(this);
        refreshGridView();

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(SAVED_MEDIA_IDS, mIds);
    }

    private void refreshGridView() {
        if (WordPress.getCurrentBlog() == null) {
            return;
        }

        String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
        Cursor cursor = WordPress.wpDB.getMediaFiles(blogId, mIds);
        if (cursor == null) {
            mGridAdapter.changeCursor(null);
            return;
        }
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
        final int mPos;
        private final int mCount;

        // a map of custom position to cursor position
        private final SparseIntArray mPositions;

        /**
         * A wrapper to allow for a custom order of items in a cursor *
         */
        public OrderedCursor(Cursor cursor, SparseIntArray positions) {
            super(cursor);
            cursor.moveToPosition(-1);
            mPos = 0;
            mCount = cursor.getCount();
            mPositions = positions;
        }

        @Override
        public boolean move(int offset) {
            return this.moveToPosition(this.mPos + offset);
        }

        @Override
        public boolean moveToNext() {
            return this.moveToPosition(this.mPos + 1);
        }

        @Override
        public boolean moveToPrevious() {
            return this.moveToPosition(this.mPos - 1);
        }

        @Override
        public boolean moveToFirst() {
            return this.moveToPosition(0);
        }

        @Override
        public boolean moveToLast() {
            return this.moveToPosition(this.mCount - 1);
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
        if (cursor == null) {
            return;
        }
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

    @Override
    public void drop(int from, int to) {
        String id = mIds.get(from);
        mIds.remove(id);
        mIds.add(to, id);
        refreshGridView();
    }

    @Override
    public void remove(int position) {
    }
}
