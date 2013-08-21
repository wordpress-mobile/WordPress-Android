package org.wordpress.android.ui.media;

import java.util.ArrayList;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.ActionMode.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.MultiSelectGridView;
import org.wordpress.android.ui.MultiSelectGridView.MultiSelectListener;
import org.wordpress.android.ui.media.MediaGridAdapter.MediaGridAdapterCallback;

public class MediaMultiSelectFragment extends SherlockFragment implements MediaGridAdapterCallback, MultiSelectListener, Callback {

    private MultiSelectGridView mGridView;
    private MediaGridAdapter mGridAdapter;
    private ArrayList<String> mCheckedItems;
    private ActionMode mActionMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mCheckedItems = new ArrayList<String>();
        
        mGridAdapter = new MediaGridAdapter(getActivity(), null, 0, mCheckedItems);
        mGridAdapter.setCallback(this);
        
        View view = inflater.inflate(R.layout.media_multi_select_fragment, container, false);
        
        mGridView = (MultiSelectGridView) view.findViewById(R.id.media_gridview);
        mGridView.setAdapter(mGridAdapter);
        mGridView.setMultiSelectListener(this);
        

        mActionMode = getSherlockActivity().startActionMode(this);
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        refreshGridView();
    }

    private void refreshGridView() {
        mGridAdapter.swapCursor(getImagesFromDB());
    }

    private Cursor getImagesFromDB() {
        Blog blog = WordPress.getCurrentBlog();

        if (blog == null)
            return null;

        String blogId = String.valueOf(blog.getBlogId());
        
        return WordPress.wpDB.getMediaImagesForBlog(blogId);
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
        return mGridView.isInMultiSelectMode();
    }

    @Override
    public void onMultiSelectChange(int count) {
        mActionMode.setTitle(count + " selected");
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        // TODO Auto-generated method stub
        
    }
    
}
