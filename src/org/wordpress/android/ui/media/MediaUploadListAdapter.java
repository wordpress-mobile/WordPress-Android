package org.wordpress.android.ui.media;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.BitmapLruCache;
import org.wordpress.android.util.ImageHelper.BitmapWorkerCallback;
import org.wordpress.android.util.ImageHelper.BitmapWorkerTask;

public class MediaUploadListAdapter extends CursorAdapter implements OnClickListener {

    public interface OnButtonClickListener {
        public void onRetryClicked(String mediaId);
        public void onEditClicked(String mediaId);
    }

    private OnButtonClickListener mOnButtonClickListener;
    
    public MediaUploadListAdapter(Context context, Cursor c, int i, OnButtonClickListener onButtonClickListener) {
        super(context, c, i);
        mOnButtonClickListener = onButtonClickListener;
    }

    

    @Override
    public void bindView(final View view, Context context, final Cursor cursor) {
        TextView textView = (TextView) view.findViewById(R.id.media_upload_listitem_filename);
        textView.setText(cursor.getString(cursor.getColumnIndex("fileName")));

        ImageView imageView = (ImageView) view.findViewById(R.id.media_upload_listitem_image);
        final String filePath = cursor.getString(cursor.getColumnIndex("filePath"));
        
        if (MediaUtils.isValidImage(filePath)) {
            
            Bitmap bitmap = WordPress.localImageCache.get(filePath); 
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
            
                int width = imageView.getLayoutParams().width;
                int height = imageView.getLayoutParams().height;
                
                BitmapWorkerTask task = new BitmapWorkerTask(imageView, width, height, new BitmapWorkerCallback() {
                    
                    @Override
                    public void onBitmapReady(Bitmap bitmap) {
                        WordPress.localImageCache.put(filePath, bitmap);
                    }
                });
                task.execute(filePath);
            }
        } else {
            imageView.setImageBitmap(null);
        }
        
        String uploadState = cursor.getString(cursor.getColumnIndex("uploadState"));
        
        ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.media_upload_listitem_progress);
        if (uploadState.equals("uploading")) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
        
        Button retryButton = (Button) view.findViewById(R.id.media_upload_listitem_button_retry);
        Button editButton = (Button) view.findViewById(R.id.media_upload_listitem_button_edit);
        Button queuedButton = (Button) view.findViewById(R.id.media_upload_listitem_button_queued);
        Button uploadingButton = (Button) view.findViewById(R.id.media_upload_listitem_button_uploading);
        
        if (uploadState.equals("failed")) {
            retryButton.setVisibility(View.VISIBLE);
            editButton.setVisibility(View.GONE);
            queuedButton.setVisibility(View.GONE);
            uploadingButton.setVisibility(View.GONE);
        } else if (uploadState.equals("queued")) {
            retryButton.setVisibility(View.GONE);
            editButton.setVisibility(View.GONE);
            queuedButton.setVisibility(View.VISIBLE);
            uploadingButton.setVisibility(View.GONE);
        } else if (uploadState.equals("uploading")) {
            retryButton.setVisibility(View.GONE);
            editButton.setVisibility(View.GONE);
            queuedButton.setVisibility(View.GONE);
            uploadingButton.setVisibility(View.VISIBLE);
        } else if (uploadState.equals("uploaded")) {
            retryButton.setVisibility(View.GONE);
            editButton.setVisibility(View.VISIBLE);
            queuedButton.setVisibility(View.GONE);
            uploadingButton.setVisibility(View.GONE);
        }
        
        retryButton.setOnClickListener(this);
        editButton.setOnClickListener(this);
        queuedButton.setOnClickListener(this);
        uploadingButton.setOnClickListener(this);
        
        String mediaId = cursor.getString(cursor.getColumnIndex("mediaId"));
        retryButton.setTag(mediaId);
        editButton.setTag(mediaId);
        queuedButton.setTag(mediaId);
        uploadingButton.setTag(mediaId);
        
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup root) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.media_upload_listitem, root, false);
    }

    @Override
    public void onClick(View v) {
        String mediaId = (String) v.getTag();
        
        switch (v.getId()) {
            case R.id.media_upload_listitem_button_retry:
                mOnButtonClickListener.onRetryClicked(mediaId);
                break;
            case R.id.media_upload_listitem_button_edit:
                mOnButtonClickListener.onEditClicked(mediaId);
                break;
            default:
        }
    }

}
