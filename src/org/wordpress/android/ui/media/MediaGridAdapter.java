package org.wordpress.android.ui.media;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.TypedValue;
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
import org.wordpress.android.util.Utils;

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
        
        TextView uploadDateView = (TextView) view.findViewById(R.id.media_grid_item_upload_date);
        if (uploadDateView != null) {
            String date = MediaUtils.getDate(cursor.getLong(cursor.getColumnIndex("date_created_gmt")));
            uploadDateView.setText("Uploaded on: " + date);
        }
        
        TextView filenameView = (TextView) view.findViewById(R.id.media_grid_item_filename);
        if (filenameView != null) {
            String fileName = cursor.getString(cursor.getColumnIndex("fileName"));
            filenameView.setText("File name: " + fileName);
        }

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
        if  (Utils.isXLarge(context)) {
            fileTypeView.setText("File type: " + fileType);
        } else {
            fileTypeView.setText(fileType);
        }
        

        TextView dimensionView = (TextView) view.findViewById(R.id.media_grid_item_dimension);
        if (dimensionView != null) {
            if( MediaUtils.isValidImage(fileURL)) {
                int width = cursor.getInt(cursor.getColumnIndex("width"));
                int height = cursor.getInt(cursor.getColumnIndex("height"));
                
                String dimensions = width + "x" + height;
                dimensionView.setText("Dimensions: " + dimensions);
                dimensionView.setVisibility(View.VISIBLE);
            } else {
                dimensionView.setVisibility(View.GONE);
            }
        }

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

    /** Updates the width of a cell to max out the space available, for phones **/
    private void updateGridWidth(Context context, View view) {

        int maxWidth = context.getResources().getDisplayMetrics().widthPixels;
        int columnCount = context.getResources().getInteger(R.integer.media_grid_num_columns);
        
        if (columnCount > 1) {
            int dp12 = (int) dpToPx(context, 12);
            int padding = (columnCount + 1) * dp12;
            int width = (maxWidth - padding) / columnCount;
            view.setLayoutParams(new GridView.LayoutParams(width, width));
        }
        
    }


    private float dpToPx(Context context, int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
    
    public void setCallback(MediaGridAdapterCallback callback) {
        mCallback = callback;
    }
}