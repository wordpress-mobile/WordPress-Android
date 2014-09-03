package org.wordpress.android.ui.media;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.mobeta.android.dslv.ResourceDragSortCursorAdapter;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.StringUtils;

/**
 * Adapter for a drag-sort listview where the user can drag media items to sort their order
 * for a media gallery
 */
class MediaGalleryAdapter extends ResourceDragSortCursorAdapter {
    private ImageLoader mImageLoader;

    public MediaGalleryAdapter(Context context, int layout, Cursor c, boolean autoRequery, ImageLoader imageLoader) {
        super(context, layout, c, autoRequery);
        setImageLoader(imageLoader);
    }

    void setImageLoader(ImageLoader imageLoader) {
        if (imageLoader != null) {
            mImageLoader = imageLoader;
        } else {
            mImageLoader = WordPress.imageLoader;
        }
    }

    private static class GridViewHolder {
        private final TextView filenameView;
        private final TextView titleView;
        private final TextView uploadDateView;
        private final ImageView imageView;
        private final TextView fileTypeView;
        private final TextView dimensionView;

        GridViewHolder(View view) {
            filenameView = (TextView) view.findViewById(R.id.media_grid_item_filename);
            titleView = (TextView) view.findViewById(R.id.media_grid_item_name);
            uploadDateView = (TextView) view.findViewById(R.id.media_grid_item_upload_date);
            imageView = (ImageView) view.findViewById(R.id.media_grid_item_image);
            fileTypeView = (TextView) view.findViewById(R.id.media_grid_item_filetype);
            dimensionView = (TextView) view.findViewById(R.id.media_grid_item_dimension);
        }
    }
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final GridViewHolder holder;
        if (view.getTag() instanceof GridViewHolder) {
            holder = (GridViewHolder) view.getTag();
        } else {
            holder = new GridViewHolder(view);
            view.setTag(holder);
        }

        String state = cursor.getString(cursor.getColumnIndex("uploadState"));
        boolean isLocalFile = MediaUtils.isLocalFile(state);

        // file name
        String fileName = cursor.getString(cursor.getColumnIndex("fileName"));
        if (holder.filenameView != null) {
            holder.filenameView.setText("File name: " + fileName);
        }

        // title of media
        String title = cursor.getString(cursor.getColumnIndex("title"));
        if (title == null || title.equals(""))
            title = fileName;
        holder.titleView.setText(title);

        // upload date
        if (holder.uploadDateView != null) {
            String date = MediaUtils.getDate(cursor.getLong(cursor.getColumnIndex("date_created_gmt")));
            holder.uploadDateView.setText("Uploaded on: " + date);
        }

        // load image
        if (isLocalFile) {
            // should not be local file
        } else {
            loadNetworkImage(cursor, (NetworkImageView) holder.imageView);
        }

        // get the file extension from the fileURL
        String filePath = StringUtils.notNullStr(cursor.getString(cursor.getColumnIndex("filePath")));
        if (filePath.isEmpty())
            filePath = StringUtils.notNullStr(cursor.getString(cursor.getColumnIndex("fileURL")));

        // file type
        String fileType = filePath.replaceAll(".*\\.(\\w+)$", "$1").toUpperCase();
        if  (DisplayUtils.isXLarge(context)) {
            holder.fileTypeView.setText("File type: " + fileType);
        } else {
            holder.fileTypeView.setText(fileType);
        }

        // dimensions
        if (holder.dimensionView != null) {
            if( MediaUtils.isValidImage(filePath)) {
                int width = cursor.getInt(cursor.getColumnIndex("width"));
                int height = cursor.getInt(cursor.getColumnIndex("height"));

                if (width > 0 && height > 0) {
                    String dimensions = width + "x" + height;
                    holder.dimensionView.setText("Dimensions: " + dimensions);
                    holder.dimensionView.setVisibility(View.VISIBLE);
                }
            } else {
                holder.dimensionView.setVisibility(View.GONE);
            }
        }

    }

    private void loadNetworkImage(Cursor cursor, NetworkImageView imageView) {
        String thumbnailURL = cursor.getString(cursor.getColumnIndex("thumbnailURL"));
        if (thumbnailURL == null) {
            imageView.setImageUrl(null, null);
            return;
        }

        Uri uri = Uri.parse(thumbnailURL);
        if (uri != null && MediaUtils.isValidImage(uri.getLastPathSegment())) {
            imageView.setTag(thumbnailURL);
            imageView.setImageUrl(thumbnailURL, mImageLoader);
        } else {
            imageView.setImageUrl(null, null);
        }
    }
}
