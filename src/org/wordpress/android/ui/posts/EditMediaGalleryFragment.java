package org.wordpress.android.ui.posts;

import java.util.ArrayList;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.DraggableGridView;
import org.wordpress.android.ui.media.MediaGridAdapter;
import org.wordpress.android.ui.media.MediaGridAdapter.MediaGridAdapterCallback;

public class EditMediaGalleryFragment extends SherlockFragment implements MediaGridAdapterCallback {

    private DraggableGridView mGridView;
    private MediaGridAdapter mGridAdapter;
    private ArrayList<String> mIds;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        
        mIds = new ArrayList<String>();
        
        mGridAdapter = new MediaGridAdapter(getActivity(), null, 0, new ArrayList<String>());
        mGridAdapter.setCallback(this);
        
        View view = inflater.inflate(R.layout.edit_media_gallery_fragment, container, false);
        
        mGridView = (DraggableGridView) view.findViewById(R.id.edit_media_gallery_gridview);
//        addViews(mGridView);
        
        return view;
    }

    public void setMediaIds(ArrayList<String> ids) {
        mIds = ids;
    }
    
    private void addViews(DraggableGridView gridView) {
        if (WordPress.getCurrentBlog() == null)
            return;
        
        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        
        Cursor cursor = WordPress.wpDB.getMediaFiles(blogId, mIds);
        mGridAdapter.swapCursor(cursor);
        
        for (int i = 0; i < mGridAdapter.getDataCount(); i++) {
            View view = mGridAdapter.getView(i, null, mGridView);
            mGridView.addView(view);
        }
    }

    @Override
    public void fetchMoreData(int offset) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onRetryUpload(String mediaId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isInMultiSelect() {
        return false;
    }
    
    public ArrayList<String> getMediaIds() {
        return mIds;
    }

    public String getMediaIdsAsString() {
        String ids = "";
        if (mIds.size() > 0) {
            ids = "ids=\"";
            for(String id : mIds) {
                ids += id + ",";
            }
            ids = ids.substring(0, ids.length() - 1);
            ids += "\"";
        }
        return ids;
    }
    
}
