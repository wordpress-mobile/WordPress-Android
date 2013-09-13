package org.wordpress.android.ui.media;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mobeta.android.dslv.ResourceDragSortCursorAdapter;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.Utils;

/**
 * Adapter for a drag-sort listview where the user can drag media items to sort their order 
 * for a media gallery
 */
public class MediaGalleryAdapter extends ResourceDragSortCursorAdapter {

    public MediaGalleryAdapter(Context context, int layout, Cursor c, boolean autoRequery) {
        super(context, layout, c, autoRequery);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

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
            // should not be local file
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

    }
    
    private void loadNetworkImage(Cursor cursor, NetworkImageView imageView) {
        String thumbnailURL = cursor.getString(cursor.getColumnIndex("thumbnailURL"));
        
        Uri uri = Uri.parse(thumbnailURL);
        
        if (thumbnailURL != null && MediaUtils.isValidImage(uri.getLastPathSegment())) { 
            imageView.setTag(thumbnailURL);
            imageView.setImageUrl(thumbnailURL, WordPress.imageLoader);
        } else {
            imageView.setImageUrl(null, null);
        }
        
    }

}
