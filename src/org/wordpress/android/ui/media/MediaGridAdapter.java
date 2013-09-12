package org.wordpress.android.ui.media;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.CheckableFrameLayout;
import org.wordpress.android.ui.CheckableFrameLayout.OnCheckedChangeListener;
import org.wordpress.android.util.ImageHelper.BitmapWorkerCallback;
import org.wordpress.android.util.ImageHelper.BitmapWorkerTask;
import org.wordpress.android.util.Utils;

/**
 * An adapter for the media gallery listviews.
 */
public class MediaGridAdapter extends CursorAdapter {
    
    private MediaGridAdapterCallback mCallback;
    private ArrayList<String> mCheckedItems;
    private boolean mHasRetrievedAll;
    private boolean mIsRefreshing;
    private int mCursorDataCount;
    private Map<String, List<BitmapReadyCallback>> mFilePathToCallbackMap;
    private Handler mHandler;
    
    public interface MediaGridAdapterCallback {
        public void fetchMoreData(int offset);
        public void onRetryUpload(String mediaId);
        public boolean isInMultiSelect();
    }

    interface BitmapReadyCallback {
        void onBitmapReady(Bitmap bitmap);
    }
    
    private static enum ViewTypes {
        LOCAL, NETWORK, PROGRESS, SPACER
    }

    public MediaGridAdapter(Context context, Cursor c, int flags, ArrayList<String> checkedItems) {
        super(context, c, flags);
        mCheckedItems = checkedItems;
        mFilePathToCallbackMap = new HashMap<String, List<BitmapReadyCallback>>();
        mHandler = new Handler();
    }
    
    public ArrayList<String> getCheckedItems() {
        return mCheckedItems;
    }
    
	@SuppressLint("DefaultLocale")
	@Override
    public void bindView(final View view, Context context, Cursor cursor) {
        
        int itemViewType = getItemViewType(cursor.getPosition());
        
        if (itemViewType == ViewTypes.PROGRESS.ordinal()) {
            if (mIsRefreshing) {
                int height = mContext.getResources().getDimensionPixelSize(R.dimen.media_grid_progress_height);
                view.setLayoutParams(new GridView.LayoutParams(GridView.LayoutParams.MATCH_PARENT, height));
                view.setVisibility(View.VISIBLE);
            } else { 
                view.setLayoutParams(new GridView.LayoutParams(0, 0));
                view.setVisibility(View.GONE);
            }
            return;
        } else if (itemViewType == ViewTypes.SPACER.ordinal()) {
            CheckableFrameLayout frameLayout = (CheckableFrameLayout) view.findViewById(R.id.media_grid_frame_layout);
            updateGridWidth(context, frameLayout);
            view.setVisibility(View.INVISIBLE);
            return;
        }
        
        final String mediaId = cursor.getString(cursor.getColumnIndex("mediaId"));

        String state = cursor.getString(cursor.getColumnIndex("uploadState"));
        boolean isLocalFile = MediaUtils.isLocalFile(state);

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
        
        if (filePath != null) {
            fileType = filePath.replaceAll(".*\\.(\\w+)$", "$1").toUpperCase();
            
            // file type
            TextView fileTypeView = (TextView) view.findViewById(R.id.media_grid_item_filetype);
            if  (Utils.isXLarge(context)) {
                fileTypeView.setText("File type: " + fileType);
            } else {
                fileTypeView.setText(fileType);
            }
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
        CheckableFrameLayout frameLayout = (CheckableFrameLayout) view.findViewById(R.id.media_grid_frame_layout);
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
        updateGridWidth(context, frameLayout);        
        
        // show upload state
        final TextView stateTextView = (TextView) view.findViewById(R.id.media_grid_item_upload_state);
        final ProgressBar progressUpload = (ProgressBar) view.findViewById(R.id.media_grid_item_upload_progress);
        final RelativeLayout uploadStateView = (RelativeLayout) view.findViewById(R.id.media_grid_item_upload_state_container);
        
        if (stateTextView != null) {
            
            
            if (state != null && state.length() > 0) {
                
                // show the progressbar only when the state is uploading
                if (state.equals("uploading")) {
                    progressUpload.setVisibility(View.VISIBLE);
                } else {
                    progressUpload.setVisibility(View.GONE);
                }

                // add onclick to retry failed uploads 
                if (state.equals("failed")) {
                    
                    state = "retry";
                    stateTextView.setOnClickListener(new OnClickListener() {
                        
                        @Override
                        public void onClick(View v) {
                            if (!inMultiSelect()) {
                                ((TextView) v).setText("queued");
                                v.setOnClickListener(null);
                                mCallback.onRetryUpload(mediaId);
                            }
                        }

                    });
                }
                
                stateTextView.setText(state);
                uploadStateView.setVisibility(View.VISIBLE);
            } else {
                uploadStateView.setVisibility(View.GONE);
            }
        }
        
        
        // if we are near the end, make a call to fetch more
        int position = cursor.getPosition();
        if (position == mCursorDataCount - 1 && !mHasRetrievedAll) {
            if (mCallback != null) {
                mCallback.fetchMoreData(mCursorDataCount);
            }
        }
    }

    private boolean inMultiSelect() {
        return mCallback.isInMultiSelect();
    }
    
    private void loadNetworkImage(Cursor cursor, NetworkImageView imageView) {
        String thumbnailURL = cursor.getString(cursor.getColumnIndex("thumbnailURL"));
        
        if (thumbnailURL != null) {
            Uri uri = Uri.parse(thumbnailURL);
            String filepath = uri.getLastPathSegment();
            
    
            int placeholderResId = MediaUtils.getPlaceholder(filepath);
            imageView.setImageResource(0);
            imageView.setErrorImageResId(placeholderResId);
            imageView.setDefaultImageResId(placeholderResId);

            if (MediaUtils.isValidImage(filepath)) { 
                imageView.setTag(thumbnailURL);
                imageView.setImageUrl(thumbnailURL, WordPress.imageLoader);
            } else {
                imageView.setImageResource(placeholderResId);
            }
        } else {
            imageView.setImageResource(0);
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
                imageView.setImageBitmap(null);
                
                boolean shouldFetch = false;
                
                List<BitmapReadyCallback> list;
                if (mFilePathToCallbackMap.containsKey(filePath)) {
                    list = mFilePathToCallbackMap.get(filePath);
                } else {
                    list = new ArrayList<MediaGridAdapter.BitmapReadyCallback>();
                    shouldFetch = true;
                    mFilePathToCallbackMap.put(filePath, list);
                }
                list.add(new BitmapReadyCallback() {
                    
                    @Override
                    public void onBitmapReady(Bitmap bitmap) {
                        if (imageView.getTag().equals(filePath))
                            imageView.setImageBitmap(bitmap);
                    }
                });
                

                if (shouldFetch) {
                    fetchBitmap(filePath);
                }
            }
        } else {
            // if not image, for now show no image.
            imageView.setImageBitmap(null);
        }
    }

    private void fetchBitmap(final String filePath) {
        int width = mContext.getResources().getDimensionPixelSize(R.dimen.media_grid_local_image_width);
        
        BitmapWorkerTask task = new BitmapWorkerTask(null, width, width, new BitmapWorkerCallback() {
            
            @Override
            public void onBitmapReady(final String path, ImageView imageView, final Bitmap bitmap) {
                mHandler.post(new Runnable() {
                    
                    @Override
                    public void run() {
                        List<BitmapReadyCallback> callbacks = mFilePathToCallbackMap.get(path);
                        for (BitmapReadyCallback callback : callbacks) {
                            callback.onBitmapReady(bitmap);
                        }
                        
                        WordPress.localImageCache.put(path, bitmap);
                        callbacks.clear();
                        mFilePathToCallbackMap.remove(path);        
                    }
                });
                
            }
        });
        task.execute(filePath);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup root) {
        LayoutInflater inflater = LayoutInflater.from(context);
        
        int itemViewType = getItemViewType(cursor.getPosition());
        
        // spacer and progress spinner views
        if (itemViewType == ViewTypes.PROGRESS.ordinal()) {
            return inflater.inflate(R.layout.media_grid_progress, root, false);
        } else if (itemViewType == ViewTypes.SPACER.ordinal()) {
            return inflater.inflate(R.layout.media_grid_item, root, false);
        }
        
        View view =  inflater.inflate(R.layout.media_grid_item, root, false);
        
        ViewStub imageStub = (ViewStub) view.findViewById(R.id.media_grid_image_stub);
        
        // We need to use viewstubs to inflate the image to either:
        // - a regular ImageView (for local images)
        // - a FadeInNetworkImageView (for network images)
        // This is because the NetworkImageView can't load local images.
        // The other option would be to inflate multiple layouts, but that would lead
        // to extra near-duplicate xml files that would need to be maintained.
        
        if (itemViewType == ViewTypes.LOCAL.ordinal())
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
        
        // spacer / progress cells
        int _id = cursor.getInt(cursor.getColumnIndex("_id"));
        if (_id < 0) {
            if (_id == Integer.MIN_VALUE)
                return ViewTypes.PROGRESS.ordinal();
            else 
                return ViewTypes.SPACER.ordinal();
        }
        
        // regular cells
        String state = cursor.getString(cursor.getColumnIndex("uploadState"));
        if (MediaUtils.isLocalFile(state))
            return ViewTypes.LOCAL.ordinal();
        else
            return ViewTypes.NETWORK.ordinal();
    }
    
    /** Updates the width of a cell to max out the space available, for phones **/
    private void updateGridWidth(Context context, View view) {

        int maxWidth = context.getResources().getDisplayMetrics().widthPixels;
        int columnCount = getColumnCount(context);
        
        if (columnCount > 1) {

            // use 8 dp as padding on the left, right and in between columns
            int dp8 = (int) Utils.dpToPx(8);
            int padding = (columnCount + 1) * dp8;
            int width = (maxWidth - padding) / columnCount;
            
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, width);
            int margins = (int) Utils.dpToPx(8);
            params.setMargins(0, margins, 0, margins);
            params.addRule(RelativeLayout.CENTER_IN_PARENT, 1);
            view.setLayoutParams(params);
        }
        
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == null) {
            mCursorDataCount = 0;
            return super.swapCursor(newCursor);
        }
        
        mCursorDataCount = newCursor.getCount();

        // to mimic the infinite the notification's infinite scroll ui 
        // (with a progress spinner on the bottom of the list), we'll need to add
        // extra cells in the gridview:
        // - spacer cells as fillers to place the progress spinner on the first cell (_id < 0)
        // - progress spinner cell (_id = Integer.MIN_VALUE)

        // use a matrix cursor to create the extra rows
        MatrixCursor matrixCursor = new MatrixCursor(new String[] { "_id" });

        // add spacer cells
        int columnCount = getColumnCount(mContext);
        int remainder = newCursor.getCount() % columnCount;
        if (remainder > 0) {
            int spaceCount = columnCount - remainder; 
            for (int i = 0; i < spaceCount; i++ ) {
                int id = i - spaceCount;
                matrixCursor.addRow(new Object[] {id + ""});
            }
        }

        // add progress spinner cell
        matrixCursor.addRow(new Object[] { Integer.MIN_VALUE });
        
        // use a merge cursor to place merge the extra rows at the bottom of the newly swapped cursor
        MergeCursor mergeCursor = new MergeCursor(new Cursor[] { newCursor, matrixCursor });
        return super.swapCursor(mergeCursor);
    }

    /** Return the number of columns in the media grid **/
    private int getColumnCount(Context context) {
        return context.getResources().getInteger(R.integer.media_grid_num_columns);
    }
    
    public void setCallback(MediaGridAdapterCallback callback) {
        mCallback = callback;
    }

    public void setHasRetrieviedAll(boolean b) {
        mHasRetrievedAll = b;
    }
    
    public void setRefreshing(boolean refreshing) {
        mIsRefreshing = refreshing;
        notifyDataSetChanged();
    }
    
    public int getDataCount() {
        return mCursorDataCount;
    }
}