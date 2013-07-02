package org.wordpress.android.ui.media;

import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.support.v4.app.ListFragment;

import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.GetMediaTask.Callback;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;

public class MediaItemListFragment extends ListFragment {

    private ApiHelper.GetMediaTask mGetMediaTask;
    private MediaItemListAdapter mAdapter;
    private Cursor mCursor;
    
    @Override
    public void onResume() {
        super.onResume();
        
        loadCursor();
        mAdapter = new MediaItemListAdapter(getActivity(), mCursor, 0);
        setListAdapter(mAdapter);
        
        if(WordPress.getCurrentBlog() != null)
            refreshMediaFromServer();
    }

    private void loadCursor() {
        Blog blog = WordPress.getCurrentBlog();
        if(blog != null) {
            String blogId = String.valueOf(blog.getBlogId());
            mCursor = WordPress.wpDB.getMediaFilesForBlog(blogId);
        }
    }
    
    private void refreshMediaFromServer() {
        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(WordPress.getCurrentBlog());
        
        mGetMediaTask = new ApiHelper.GetMediaTask(mCallback);
        mGetMediaTask.execute(apiArgs);
    }

    private Callback mCallback = new Callback() {
        
        @Override
        public void onSuccess() {
            loadCursor();
            
            mAdapter.changeCursor(mCursor);
        }

        @Override
        public void onFailure() {
            // TODO: handle failure
        }
    };    
    
}
