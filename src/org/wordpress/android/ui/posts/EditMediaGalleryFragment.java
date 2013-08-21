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

    private static final String ARGS_MEDIA_IDS = "ARGS_MEDIA_IDS";
    private DraggableGridView mGridView;
    private MediaGridAdapter mGridAdapter;

    public static EditMediaGalleryFragment newInstance(ArrayList<String> mediaIds) {
        EditMediaGalleryFragment fragment = new EditMediaGalleryFragment();
        
        Bundle args = new Bundle();
        args.putStringArrayList(ARGS_MEDIA_IDS, mediaIds);
        fragment.setArguments(args);
        
        return fragment;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        
        mGridAdapter = new MediaGridAdapter(getActivity(), null, 0, new ArrayList<String>());
        mGridAdapter.setCallback(this);
        
        View view = inflater.inflate(R.layout.edit_media_gallery_fragment, container, false);
        
        mGridView = (DraggableGridView) view.findViewById(R.id.edit_media_gallery_gridview);
//        addViews(mGridView);
        
        return view;
    }

    private void addViews(DraggableGridView gridView) {
        if (WordPress.getCurrentBlog() == null)
            return;
        
        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        
        ArrayList<String> ids = getArguments().getStringArrayList(ARGS_MEDIA_IDS);
        Cursor cursor = WordPress.wpDB.getMediaFiles(blogId, ids);
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
    
    
    
}
