package org.wordpress.android.ui.media;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.GetMediaTask.Callback;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;

public class MediaItemListFragment extends ListFragment {

    private ApiHelper.GetMediaTask mGetMediaTask;
    private MediaCursorAdapter mAdapter;
    private Cursor mCursor;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        
        Log.d("WordPress", "MediaItemListFragment onResume()");
        
        if(WordPress.getCurrentBlog() != null)
            refreshMedia();
        
        loadCursor();
        
        mAdapter = new MediaCursorAdapter(getActivity(), mCursor, 0);
        setListAdapter(mAdapter);
        
    }

    private void loadCursor() {
        Blog blog = WordPress.getCurrentBlog();
        if(blog != null) {
            String blogId = String.valueOf(blog.getBlogId());
            mCursor = WordPress.wpDB.getMediaFilesForBlog(blogId);
        }
    }
    
    private void refreshMedia() {
        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(WordPress.getCurrentBlog());
        
        mGetMediaTask = new ApiHelper.GetMediaTask(mCallback);
        mGetMediaTask.execute(apiArgs);
    }

    
    class MediaCursorAdapter extends CursorAdapter {

        public MediaCursorAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView title = (TextView) view.findViewById(R.id.media_listitem_title);
            title.setText(cursor.getString(cursor.getColumnIndex("title")));
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup root) {
            
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.media_listitem, root, false);
            
            return view;
        }
        
    }

    private Callback mCallback = new Callback() {
        
        @Override
        public void onSuccess() {
            loadCursor();
            
            mAdapter.changeCursor(mCursor);
        }
    };    
    
}
