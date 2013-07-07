package org.wordpress.android.ui.media;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.GetMediaTask.Callback;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;

public class MediaGridFragment extends Fragment implements OnItemClickListener {
    
    private GridView mGridView;
    private ApiHelper.GetMediaTask mGetMediaTask;
    private MediaGridListAdapter mAdapter;
    private Cursor mCursor;
    private MediaGridListener mListener;
    private boolean mIsRefreshing = false;

    public interface MediaGridListener {
        public void onMediaItemListDownloaded();
        public void onMediaItemSelected(String mediaId);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        
        mGridView = (GridView) inflater.inflate(R.layout.media_grid_fragment, container);
        mGridView.setOnItemClickListener(this);
        
        return mGridView;
        
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        try {
            mListener = (MediaGridListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement MediaGridListener");
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        mCursor = fetchMediaFromDB();
        mAdapter = new MediaGridListAdapter(getActivity(), mCursor, 0);
        mGridView.setAdapter(mAdapter);
        
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
    
    private Cursor fetchMediaFromDB() {
        Cursor cursor = null;
        Blog blog = WordPress.getCurrentBlog();
        if(blog != null) {
            String blogId = String.valueOf(blog.getBlogId());
            cursor = WordPress.wpDB.getMediaFilesForBlog(blogId);
        }
        return cursor;
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
            mListener.onMediaItemListDownloaded();
            mCursor = fetchMediaFromDB();
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor cursor = ((MediaGridListAdapter) parent.getAdapter()).getCursor();
        String mediaId = cursor.getString(cursor.getColumnIndex("mediaId"));
        mListener.onMediaItemSelected(mediaId);
    }

}
