package org.wordpress.android.ui.media;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.SpinnerAdapter;

import com.actionbarsherlock.internal.widget.IcsAdapterView;
import com.actionbarsherlock.internal.widget.IcsAdapterView.OnItemSelectedListener;
import com.actionbarsherlock.internal.widget.IcsSpinner;

import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.SyncMediaLibraryTask.Callback;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.WPActionBarActivity;

public class MediaGridFragment extends Fragment implements OnItemClickListener {
    
    private GridView mGridView;
    private ApiHelper.SyncMediaLibraryTask mGetMediaTask;
    private MediaGridListAdapter mAdapter;
    private Cursor mCursor;
    private MediaGridListener mListener;
    private boolean mIsRefreshing = false;
    private IcsSpinner mSpinner;
    private Filter mFilter = Filter.ALL;

    public interface MediaGridListener {
        public void onMediaItemListDownloaded();
        public void onMediaItemSelected(String mediaId);
    }
    
    public enum Filter {
        ALL, IMAGES, UNATTACHED;
        
        public static Filter getFilter(int filterPos) {
            if (filterPos > Filter.values().length)
                return ALL;
            else 
                return Filter.values()[filterPos];
        }
    }

    private OnItemSelectedListener mFilterSelectedListener = new OnItemSelectedListener() {
;

        @Override
        public void onItemSelected(IcsAdapterView<?> parent, View view, int position, long id) {
            mCursor = null;
            setFilter(Filter.getFilter(position));
        }

        @Override
        public void onNothingSelected(IcsAdapterView<?> parent) { }
    };
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        
        View view = inflater.inflate(R.layout.media_grid_fragment, container);
        
        mGridView = (GridView) view.findViewById(R.id.media_gridview);
        mGridView.setOnItemClickListener(this);

        View spinnerContainer = view.findViewById(R.id.media_filter_spinner_container);
        spinnerContainer.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                if (mSpinner != null) {
                    mSpinner.performClick();
                }
            }
        });

        mSpinner = (IcsSpinner) view.findViewById(R.id.media_filter_spinner);
        mSpinner.setOnItemSelectedListener(mFilterSelectedListener);
        refreshSpinnerAdapter();
        
        return view;
    }

    private void refreshSpinnerAdapter() {
        if (getActivity() == null || WordPress.getCurrentBlog() == null)
            return; 
        
        Context context = ((WPActionBarActivity) getActivity()).getSupportActionBar().getThemedContext();
        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        
        int countAll = WordPress.wpDB.getMediaCountAll(blogId);
        int countImages = WordPress.wpDB.getMediaCountImages(blogId);
        int countUnattached = WordPress.wpDB.getMediaCountUnattached(blogId);
        
        String[] filters = new String[] {
            getResources().getString(R.string.all) + " (" + countAll + ")",
            getResources().getString(R.string.images) + " (" + countImages + ")",
            getResources().getString(R.string.unattached) + " (" + countUnattached + ")" 
        };
        
        SpinnerAdapter adapter = new ArrayAdapter<String>(context, R.layout.sherlock_spinner_dropdown_item, filters);
        
        mSpinner.setAdapter(adapter);
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
        
        setFilter(mFilter);
        if (mCursor != null) {
            mAdapter = new MediaGridListAdapter(getActivity(), mCursor, 0);
            mGridView.setAdapter(mAdapter);
        }
        
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
    
    public void refreshMediaFromServer() {
        if(WordPress.getCurrentBlog() == null)
            return; 
        
        if(!mIsRefreshing) {
            mIsRefreshing = true;

            List<Object> apiArgs = new ArrayList<Object>();
            apiArgs.add(WordPress.getCurrentBlog());
            mGetMediaTask = new ApiHelper.SyncMediaLibraryTask(mCallback);
            mGetMediaTask.execute(apiArgs);
        }
    }

    private Callback mCallback = new Callback() {
        
        @Override
        public void onSuccess() {
            mIsRefreshing = false;
            mListener.onMediaItemListDownloaded();
            setFilter(mFilter);

            refreshSpinnerAdapter();
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

    public void setFilterVisibility(int visibility) {
        if (mSpinner != null)
            mSpinner.setVisibility(visibility);
    }
    
    public void setFilter(Filter filter) {
        mFilter = filter;
        mSpinner.setSelection(mFilter.ordinal());
        mCursor = filterItems(mFilter);
        
        if (mCursor != null && mAdapter != null) {
            mAdapter.swapCursor(mCursor);
        }
    }

    private Cursor filterItems(Filter filter) {
        Blog blog = WordPress.getCurrentBlog();
        
        if(blog == null)
            return null;
        
        String blogId = String.valueOf(blog.getBlogId());
        
        switch (filter) {
        case ALL:
            return WordPress.wpDB.getMediaFilesForBlog(blogId);
        case IMAGES:
            return WordPress.wpDB.getMediaImagesForBlog(blogId);
        case UNATTACHED:
            return WordPress.wpDB.getMediaUnattachedForBlog(blogId);
        }
        return null;
    }

}
