package org.wordpress.android.ui.media;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.R;
import org.wordpress.android.WordPressDB;
import org.wordpress.mediapicker.MediaItem;
import org.wordpress.mediapicker.source.MediaSource;

import java.util.ArrayList;
import java.util.List;

public class MediaSourceWPImages implements MediaSource {
    private List<MediaItem> mMediaItems = new ArrayList<>();

    public MediaSourceWPImages() {
        fetchImageData();
    }

    @Override
    public int getCount() {
        return mMediaItems.size();
    }

    @Override
    public MediaItem getMedia(int position) {
        return mMediaItems.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent, LayoutInflater inflater, ImageLoader.ImageCache cache) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.media_item_wp_image, parent, false);
        }

        if (convertView != null) {
            MediaItem mediaItem = mMediaItems.get(position);
            Uri imageSource = mediaItem.getPreviewSource();
            ImageView imageView = (ImageView) convertView.findViewById(R.id.wp_image_view_background);
            if (imageView != null) {
                if (imageSource != null) {
                    Bitmap imageBitmap = null;
                    if (cache != null) {
                        imageBitmap = cache.getBitmap(imageSource.toString());
                    }

                    if (imageBitmap == null) {
                        imageView.setImageResource(R.color.grey_medium);
                        MediaUtils.BackgroundDownloadWebImage bgDownload = new MediaUtils.BackgroundDownloadWebImage(imageView);
                        imageView.setTag(bgDownload);
                        bgDownload.execute(mediaItem.getPreviewSource());
                    } else {
                        org.wordpress.mediapicker.MediaUtils.fadeInImage(imageView, imageBitmap);
                    }
                } else {
                    imageView.setTag(null);
                    imageView.setImageResource(R.color.grey_medium);
                }
            }
        }

        return convertView;
    }

    @Override
    public boolean onMediaItemSelected(MediaItem mediaItem, boolean selected) {
        return false;
    }

    public static final Creator<MediaSourceWPImages> CREATOR =
            new Creator<MediaSourceWPImages>() {
                public MediaSourceWPImages createFromParcel(Parcel in) {
                    return new MediaSourceWPImages();
                }

                public MediaSourceWPImages[] newArray(int size) {
                    return new MediaSourceWPImages[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    private void fetchImageData() {
        Cursor imageCursor = MediaUtils.getWordPressMediaImages();

        if (imageCursor != null) {
            addWordPressImagesFromCursor(imageCursor);
            imageCursor.close();
        }
    }

    private void addWordPressImagesFromCursor(Cursor cursor) {
        if (cursor.moveToFirst()) {
            do {
                int attachmentIdColumnIndex = cursor.getColumnIndex(WordPressDB.COLUMN_NAME_MEDIA_ID);
                int fileUrlColumnIndex = cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_URL);
                int filePathColumnIndex = cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_PATH);
                int thumbnailColumnIndex = cursor.getColumnIndex(WordPressDB.COLUMN_NAME_THUMBNAIL_URL);

                String id = "";
                if (attachmentIdColumnIndex != -1) {
                    id = String.valueOf(cursor.getInt(attachmentIdColumnIndex));
                }
                MediaItem newContent = new MediaItem();
                newContent.setTag(id);
                newContent.setTitle("");

                if (fileUrlColumnIndex != -1) {
                    String fileUrl = cursor.getString(fileUrlColumnIndex);

                    if (fileUrl != null) {
                        newContent.setSource(Uri.parse(fileUrl));
                        newContent.setPreviewSource(Uri.parse(fileUrl));
                    } else if (filePathColumnIndex != -1) {
                        String filePath = cursor.getString(filePathColumnIndex);

                        if (filePath != null) {
                            newContent.setSource(Uri.parse(filePath));
                            newContent.setPreviewSource(Uri.parse(filePath));
                        }
                    }
                }

                if (thumbnailColumnIndex != -1) {
                    String preview = cursor.getString(thumbnailColumnIndex);

                    if (preview != null) {
                        newContent.setPreviewSource(Uri.parse(preview));
                    }
                }

                mMediaItems.add(newContent);
            } while (cursor.moveToNext());
        }
    }
}
