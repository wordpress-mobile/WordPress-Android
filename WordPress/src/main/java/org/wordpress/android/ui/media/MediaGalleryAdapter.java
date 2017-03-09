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
import com.wellsql.generated.MediaModelTable;

import org.wordpress.android.R;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.StringUtils;

/**
 * Adapter for a drag-sort listview where the user can drag media items to sort their order
 * for a media gallery
 */
class MediaGalleryAdapter extends ResourceDragSortCursorAdapter {
    private ImageLoader mImageLoader;

    MediaGalleryAdapter(Context context, int layout, Cursor c, boolean autoRequery, ImageLoader imageLoader) {
        super(context, layout, c, autoRequery);
        setImageLoader(imageLoader);
    }

    void setImageLoader(ImageLoader imageLoader) {
        mImageLoader = imageLoader;
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

        // file name
        String fileName = cursor.getString(cursor.getColumnIndex(MediaModelTable.FILE_NAME));
        if (holder.filenameView != null) {
            holder.filenameView.setText(String.format(context.getString(R.string.media_file_name), fileName));
        }

        // title of media
        String title = cursor.getString(cursor.getColumnIndex(MediaModelTable.TITLE));
        if (title == null || title.equals(""))
            title = fileName;
        holder.titleView.setText(title);

        // upload date
        if (holder.uploadDateView != null) {
            String date = MediaUtils.getDate(cursor.getLong(cursor.getColumnIndex(MediaModelTable.UPLOAD_DATE)));
            holder.uploadDateView.setText(String.format(context.getString(R.string.media_uploaded_on), date));
        }

        // load image
        String uploadState = cursor.getString(cursor.getColumnIndex(MediaModelTable.UPLOAD_STATE));
        if (!MediaUtils.isLocalFile(uploadState)) {
            loadNetworkImage(cursor, (NetworkImageView) holder.imageView);
        }

        // get the file extension from the fileURL
        String filePath = StringUtils.notNullStr(cursor.getString(cursor.getColumnIndex(MediaModelTable.FILE_PATH)));
        if (filePath.isEmpty()) {
            filePath = StringUtils.notNullStr(cursor.getString(cursor.getColumnIndex(MediaModelTable.URL)));
        }

        // file type
        String fileExtension = filePath.replaceAll(".*\\.(\\w+)$", "$1").toUpperCase();
        if (holder.fileTypeView != null) {
            holder.fileTypeView.setText(String.format(context.getString(R.string.media_file_type), fileExtension));
        }

        // dimensions
        if (holder.dimensionView != null) {
            if( MediaUtils.isValidImage(filePath)) {
                int width = cursor.getInt(cursor.getColumnIndex(MediaModelTable.WIDTH));
                int height = cursor.getInt(cursor.getColumnIndex(MediaModelTable.HEIGHT));

                if (width > 0 && height > 0) {
                    String dimensions = width + "x" + height;
                    holder.dimensionView.setText(String.format(context.getString(R.string.media_dimensions),
                            dimensions));
                    holder.dimensionView.setVisibility(View.VISIBLE);
                }
            } else {
                holder.dimensionView.setVisibility(View.GONE);
            }
        }

    }

    private void loadNetworkImage(Cursor cursor, NetworkImageView imageView) {
        String thumbnailURL = cursor.getString(cursor.getColumnIndex(MediaModelTable.THUMBNAIL_URL));
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
