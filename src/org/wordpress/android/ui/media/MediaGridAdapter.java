package org.wordpress.android.ui.media;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.v4.widget.CursorAdapter;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.CheckableFrameLayout;
import org.wordpress.android.ui.CheckableFrameLayout.OnCheckedChangeListener;
import org.wordpress.android.util.ImageHelper.BitmapWorkerCallback;
import org.wordpress.android.util.ImageHelper.BitmapWorkerTask;
import org.wordpress.android.util.Utils;

public class MediaGridAdapter extends CursorAdapter {
    
    private MediaGridAdapterCallback mCallback;
    private ArrayList<String> mCheckedItems;
    private Set<String> mLocalImageRequestQueue;
    private boolean mHasRetrievedAll;
    
    public interface MediaGridAdapterCallback {
        public void fetchMoreData(int offset);
        public void onRetryUpload(String mediaId);
    }
    
    private static enum ViewTypes {
        LOCAL, NETWORK
    }
    
    public MediaGridAdapter(Context context, Cursor c, int flags, ArrayList<String> checkedItems) {
        super(context, c, flags);
        mCheckedItems = checkedItems;
        mLocalImageRequestQueue = new HashSet<String>();
    }
    
    public ArrayList<String> getCheckedItems() {
        return mCheckedItems;
    }
    
    @SuppressLint("DefaultLocale")
	@Override
    public void bindView(final View view, Context context, Cursor cursor) {
        final String mediaId = cursor.getString(cursor.getColumnIndex("mediaId"));

        // upload state
        String state = cursor.getString(cursor.getColumnIndex("uploadState"));
        TextView stateTextView = (TextView) view.findViewById(R.id.media_grid_item_upload_state);
        if (stateTextView != null) {
            if (state != null && state.length() > 0) {
                
                // add onclick to retry failed uploads 
                if (state.equals("failed")) {
                    state = "retry";
                    stateTextView.setOnClickListener(new OnClickListener() {
                        
                        @Override
                        public void onClick(View v) {
                            if (!inMultiSelect()) {
                                mCallback.onRetryUpload(mediaId);
                                notifyDataSetChanged();
                            }
                        }

                    });
                }
                
                stateTextView.setText(state);
                stateTextView.setVisibility(View.VISIBLE);
            } else {
                stateTextView.setVisibility(View.GONE);
            }
        }

        boolean isLocalFile = isLocalFile(state);

        // file name
        TextView filenameView = (TextView) view.findViewById(R.id.media_grid_item_filename);
        String fileName = cursor.getString(cursor.getColumnIndex("fileName"));
        if (filenameView != null) {
            filenameView.setText("File name: " + fileName);
        }
        
        // title of media
        TextView titleView = (TextView) view.findViewById(R.id.media_grid_item_name);
        String title = cursor.getString(cursor.getColumnIndex("title"));
        if (title == null || title.equals(""))
            title = fileName;
        titleView.setText(title);
        
        // upload date
        TextView uploadDateView = (TextView) view.findViewById(R.id.media_grid_item_upload_date);
        if (uploadDateView != null) {
            String date = MediaUtils.getDate(cursor.getLong(cursor.getColumnIndex("date_created_gmt")));
            uploadDateView.setText("Uploaded on: " + date);
        }

        // load image
        final ImageView imageView = (ImageView) view.findViewById(R.id.media_grid_item_image);
        if (isLocalFile) {
            loadLocalImage(cursor, imageView);
        } else {
            loadNetworkImage(cursor, (NetworkImageView) imageView);
        }
        
        String fileType = null;
        
        // get the file extension from the fileURL
        String filePath = cursor.getString(cursor.getColumnIndex("filePath"));
        if (filePath == null)
            filePath = cursor.getString(cursor.getColumnIndex("fileURL"));
            
        fileType = filePath.replaceAll(".*\\.(\\w+)$", "$1").toUpperCase();
        
        
        // file type
        TextView fileTypeView = (TextView) view.findViewById(R.id.media_grid_item_filetype);
        if  (Utils.isXLarge(context)) {
            fileTypeView.setText("File type: " + fileType);
        } else {
            fileTypeView.setText(fileType);
        }
        

        // dimensions
        TextView dimensionView = (TextView) view.findViewById(R.id.media_grid_item_dimension);
        if (dimensionView != null) {
            if( MediaUtils.isValidImage(filePath)) {
                int width = cursor.getInt(cursor.getColumnIndex("width"));
                int height = cursor.getInt(cursor.getColumnIndex("height"));
                
                if (width > 0 && height > 0) {
                    String dimensions = width + "x" + height;
                    dimensionView.setText("Dimensions: " + dimensions);
                    dimensionView.setVisibility(View.VISIBLE);
                }
            } else {
                dimensionView.setVisibility(View.GONE);
            }
        }

        
        // multi-select highlighting
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
        
        // resizing layout to fit nicely into grid view
        updateGridWidth(context, view);        
        
        // if we are near the end, make a call to fetch more
        int position = cursor.getPosition();
        if (position == cursor.getCount() - 1 && !mHasRetrievedAll) {
            if (mCallback != null)
                mCallback.fetchMoreData(cursor.getCount());
        }
    }

    private boolean inMultiSelect() {
        return mCheckedItems.size() > 0;
    }
    
    private boolean isLocalFile(String state) {
        if (state == null)
            return false;
        
        if (state.equals("queued") || state.equals("uploading") || state.equals("retry") || state.equals("failed"))
            return true;
        
        return false;
    }
    
    private void loadNetworkImage(Cursor cursor, NetworkImageView imageView) {
        String thumbnailURL = cursor.getString(cursor.getColumnIndex("thumbnailURL"));
        
        if (thumbnailURL != null) { 
            imageView.setTag(thumbnailURL);
            imageView.setImageUrl(thumbnailURL, WordPress.imageLoader);
        }
        
    }

    private synchronized void loadLocalImage(Cursor cursor, final ImageView imageView) {

        final String filePath = cursor.getString(cursor.getColumnIndex("filePath"));
        if (MediaUtils.isValidImage(filePath)) {
            imageView.setTag(filePath);
            
            Bitmap bitmap = WordPress.localImageCache.get(filePath); 
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                
                if (mLocalImageRequestQueue.contains(filePath))
                    return;
                
                mLocalImageRequestQueue.add(filePath);
            
                int maxWidth = mContext.getResources().getDisplayMetrics().widthPixels;
                int width = (int) (maxWidth * 11.0f / 24.0f);
                BitmapWorkerTask task = new BitmapWorkerTask(imageView, width, width, new BitmapWorkerCallback() {
                    
                    @Override
                    public void onBitmapReady(String path, Bitmap bitmap) {
                        WordPress.localImageCache.put(path, bitmap);
                    }
                });
                task.execute(filePath);
            }
        }        
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup root) {
        LayoutInflater inflater = LayoutInflater.from(context);
        
        View view =  inflater.inflate(R.layout.media_grid_item, root, false);
        
        ViewStub imageStub = (ViewStub) view.findViewById(R.id.media_grid_image_stub);
        
        // We need to use viewstubs to inflate the image to either:
        // - a regular ImageView (for local images)
        // - a FadeInNetworkImageView (for network images)
        // This is because the NetworkImageView can't load local images.
        // The other option would be to inflate multiple layouts, but that would lead
        // to extra near-duplicate xml files that would need to be maintained.
        
        if (getItemViewType(cursor.getPosition()) == ViewTypes.LOCAL.ordinal())
            imageStub.setLayoutResource(R.layout.media_grid_image_local);
        else
            imageStub.setLayoutResource(R.layout.media_grid_image_network);
        
        imageStub.inflate();
        
        return view;
    }

    @Override
    public int getViewTypeCount() {
        return ViewTypes.values().length;
    }
    
    @Override
    public int getItemViewType(int position) {
        Cursor cursor = getCursor();
        cursor.moveToPosition(position);
        String state = cursor.getString(cursor.getColumnIndex("uploadState"));
        if (isLocalFile(state))
            return ViewTypes.LOCAL.ordinal();
        else
            return ViewTypes.NETWORK.ordinal();
    }
    
    public void setCallback(MediaGridAdapterCallback callback) {
        mCallback = callback;
    }

    public void setHasRetrieviedAll(boolean b) {
        mHasRetrievedAll = b;
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
}