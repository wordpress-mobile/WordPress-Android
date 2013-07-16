package org.wordpress.android.ui.media;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.CheckableFrameLayout;
import org.wordpress.android.ui.CheckableFrameLayout.OnCheckedChangeListener;

public class MediaGridAdapter extends CursorAdapter {
    
    private MediaGridAdapterCallback mCallback;
    private ArrayList<String> mCheckedItems;
    
    public interface MediaGridAdapterCallback {
        public void onPrefetchData(int offset);
    }
    
    public MediaGridAdapter(Context context, Cursor c, int flags, ArrayList<String> checkedItems) {
        super(context, c, flags);
        mCheckedItems = checkedItems;
    }
    
    public ArrayList<String> getCheckedItems() {
        return mCheckedItems;
    }
    
    @SuppressLint("DefaultLocale")
	@Override
    public void bindView(final View view, Context context, Cursor cursor) {
        final String mediaId = cursor.getString(cursor.getColumnIndex("mediaId"));
        
        TextView title = (TextView) view.findViewById(R.id.media_grid_item_name);
        title.setText(cursor.getString(cursor.getColumnIndex("title")));
        
        String thumbnailURL = cursor.getString(cursor.getColumnIndex("thumbnailURL"));
        NetworkImageView imageView = (NetworkImageView) view.findViewById(R.id.media_grid_item_image);
        
        if (thumbnailURL != null) { 
            imageView.setImageUrl(thumbnailURL, WordPress.imageLoader);
            imageView.setTag(thumbnailURL);
        }
        
        // get the file extension from the fileURL
        String fileURL = cursor.getString(cursor.getColumnIndex("fileURL"));
        String fileType = fileURL.replaceAll(".*\\.(\\w+)$", "$1").toUpperCase();
        
        TextView fileTypeView = (TextView) view.findViewById(R.id.media_grid_item_filetype);
        fileTypeView.setText(fileType);

        final int position = cursor.getPosition();
        
        CheckableFrameLayout frameLayout = (CheckableFrameLayout) view;
        
        frameLayout.setTag(mediaId);
        frameLayout.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(CheckableFrameLayout view, boolean isChecked) {
                String mediaId = (String) view.getTag();
                if (isChecked) {
                    if (!mCheckedItems.contains(mediaId)) {
                        mCheckedItems.add(mediaId);
                    }
                } else {
                    mCheckedItems.remove(mediaId);
                }
                
            }
        });
        frameLayout.setChecked(mCheckedItems.contains(mediaId));
            
        updateGridWidth(context, view);        
        // if we are near the end, make a call to fetch more
        if ( cursor.getCount() - position == 25 || (position == cursor.getCount() - 1)) {
            if (mCallback != null)
                mCallback.onPrefetchData(cursor.getCount());
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup root) {
        LayoutInflater inflater = LayoutInflater.from(context);
        
        return inflater.inflate(R.layout.media_grid_item, root, false);
    }

    private void updateGridWidth(Context context, View view) {
        // make it so that total padding = 1/12 of screen width
        // since there are two columns on the grid, each column will get
        // 11/24 of the remaining space available
        
        // (the padding is based on the mocks)

        // phone-size - full screen - use entire screen
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        
        // TODO: tablet-size
        
        int width = (int) (screenWidth * 11.0f / 24.0f);
        view.setLayoutParams(new GridView.LayoutParams(width, width));
    }

    
    public void setCallback(MediaGridAdapterCallback callback) {
        mCallback = callback;
    }
}