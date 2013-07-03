package org.wordpress.android.ui.media;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;

import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.GetMediaTask.Callback;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;

public class MediaItemListFragment extends ListFragment {

    private ApiHelper.GetMediaTask mGetMediaTask;
    private MediaItemListAdapter mAdapter;
    private Cursor mCursor;
    private OnMediaItemSelectedListener mListener;
    
    public interface OnMediaItemSelectedListener {
        public void onMediaItemSelected(String mediaId);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        try {
            mListener = (OnMediaItemSelectedListener) activity;
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
        
        if(WordPress.getCurrentBlog() != null)
            refreshMediaFromServer();
    }

    private void loadCursor() {
        Blog blog = WordPress.getCurrentBlog();
        if(blog != null) {
            String blogId = String.valueOf(blog.getBlogId());
            mCursor = WordPress.wpDB.getMediaFilesForBlog(blogId);
            
            if(mCursor.moveToFirst()) {
                String mediaId = mCursor.getString(mCursor.getColumnIndex("uuid"));
                mListener.onMediaItemSelected(mediaId);
            }
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Cursor cursor = (Cursor) getListAdapter().getItem(position);
        String mediaId = cursor.getString(cursor.getColumnIndex("uuid"));
        mListener.onMediaItemSelected(mediaId);
        
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
