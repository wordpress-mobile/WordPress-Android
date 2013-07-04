package org.wordpress.android.ui.media;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.GetMediaTask.Callback;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;

public class MediaItemListFragment extends ListFragment {

    private ApiHelper.GetMediaTask mGetMediaTask;
    private MediaItemListAdapter mAdapter;
    private Cursor mCursor;
    private MediaItemListListener mListener;
    private boolean mIsRefreshing = false;
    
    public interface MediaItemListListener {
        public void onMediaItemListDownloaded();
        public void onMediaItemSelected(String mediaId);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        try {
            mListener = (MediaItemListListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnMediaItemSelectedListener");
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        loadCursor();
        mAdapter = new MediaItemListAdapter(getActivity(), mCursor, 0);
        setListAdapter(mAdapter);
        
        refreshMediaFromServer();
    }

    public void search(String searchTerm) {
        Blog blog = WordPress.getCurrentBlog();
        if(blog != null) {
            String blogId = String.valueOf(blog.getBlogId());
            mCursor = WordPress.wpDB.getMediaFilesForBlog(blogId, searchTerm);
            mAdapter.changeCursor(mCursor);
        }
    }
    
    private void loadCursor() {
        Blog blog = WordPress.getCurrentBlog();
        if(blog != null) {
            String blogId = String.valueOf(blog.getBlogId());
            mCursor = WordPress.wpDB.getMediaFilesForBlog(blogId);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Cursor cursor = (Cursor) getListAdapter().getItem(position);
        String mediaId = cursor.getString(cursor.getColumnIndex("uuid"));
        mListener.onMediaItemSelected(mediaId);
        
    }
    
    public void refreshMediaFromServer() {
        if(WordPress.getCurrentBlog() == null)
            return; 
        
        if(!mIsRefreshing) {
            mIsRefreshing = true;

            List<Object> apiArgs = new ArrayList<Object>();
            apiArgs.add(WordPress.getCurrentBlog());
            mGetMediaTask = new ApiHelper.GetMediaTask(mCallback);
            mGetMediaTask.execute(apiArgs);
        }
    }

    private Callback mCallback = new Callback() {
        
        @Override
        public void onSuccess() {
            mIsRefreshing = false;
            loadCursor();
            mListener.onMediaItemListDownloaded();
            mAdapter.changeCursor(mCursor);
        }

        @Override
        public void onFailure() {
            mIsRefreshing = false;
            
        }
    };    
    
    public boolean isRefreshing() {
        return mIsRefreshing;
    }
    
}
