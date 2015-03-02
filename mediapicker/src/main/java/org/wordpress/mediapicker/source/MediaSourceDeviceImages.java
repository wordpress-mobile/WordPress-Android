package org.wordpress.mediapicker.source;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcel;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.mediapicker.MediaItem;
import org.wordpress.mediapicker.MediaUtils;
import org.wordpress.mediapicker.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link org.wordpress.mediapicker.source.MediaSource} that loads images from the device
 * {@link android.provider.MediaStore}.
 */

public class MediaSourceDeviceImages implements MediaSource {
    private static final String[] THUMBNAIL_QUERY_COLUMNS = {
            MediaStore.Images.Thumbnails._ID,
            MediaStore.Images.Thumbnails.DATA,
            MediaStore.Images.Thumbnails.IMAGE_ID
    };
    private static final String[] IMAGE_QUERY_COLUMNS = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.ORIENTATION
    };

    private ContentResolver mContentResolver;
    private List<MediaItem> mMediaItems;

    public MediaSourceDeviceImages() {
    }

    public MediaSourceDeviceImages(final ContentResolver contentResolver) {
        mContentResolver = contentResolver;
        createMediaItems();
    }

    @Override
    public void setListener(final OnMediaChange listener) {
        // Ignored
    }

    @Override
    public int getCount() {
        return mMediaItems != null ? mMediaItems.size() : 0;
    }

    @Override
    public MediaItem getMedia(int position) {
        return mMediaItems != null ? mMediaItems.get(position) : null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent, LayoutInflater inflater, final ImageLoader.ImageCache cache) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.media_item_image, parent, false);
        }

        if (convertView != null) {
            final MediaItem mediaItem = mMediaItems.get(position);
            final Uri imageSource = (mediaItem.getPreviewSource() != null && !mediaItem.getPreviewSource().toString().isEmpty())
                    ? mediaItem.getPreviewSource() : mediaItem.getSource();
            final ImageView imageView = (ImageView) convertView.findViewById(R.id.image_view_background);
            if (imageView != null) {
                int width = imageView.getWidth();
                int height = imageView.getHeight();

                if (width <= 0 || height <= 0) {
                    imageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            int width = imageView.getWidth();
                            int height = imageView.getHeight();
                            setImage(imageSource, cache, imageView, mediaItem, width, height);
                            imageView.getViewTreeObserver().removeOnPreDrawListener(this);
                            return true;
                        }
                    });
                } else {
                    setImage(imageSource, cache, imageView, mediaItem, width, height);
                }
            }
        }

        return convertView;
    }

    @Override
    public boolean onMediaItemSelected(MediaItem mediaItem, boolean selected) {
        return !selected;
    }

    private void setMediaItems(List<MediaItem> mediaItems) {
        mMediaItems = mediaItems;
    }

    private void setImage(Uri imageSource, ImageLoader.ImageCache cache, ImageView imageView, MediaItem mediaItem, int width, int height) {
        if (imageSource != null) {
            Bitmap imageBitmap = null;
            if (cache != null) {
                imageBitmap = cache.getBitmap(imageSource.toString());
            }

            if (imageBitmap == null) {
                imageView.setImageResource(R.drawable.media_item_placeholder);
                MediaUtils.BackgroundFetchThumbnail bgDownload =
                        new MediaUtils.BackgroundFetchThumbnail(imageView,
                                cache,
                                MediaUtils.BackgroundFetchThumbnail.TYPE_IMAGE,
                                width,
                                height,
                                mediaItem.getRotation());
                imageView.setTag(bgDownload);
                bgDownload.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imageSource);
            } else {
                MediaUtils.fadeInImage(imageView, imageBitmap);
            }
        } else {
            imageView.setTag(null);
            imageView.setImageResource(R.drawable.media_item_placeholder);
        }
    }

    /**
     * Helper method; creates {@link org.wordpress.mediapicker.MediaItem}'s from MediaStore data
     */
    private void createMediaItems() {
        mMediaItems = new ArrayList<>();
        final List<String> imageIds = new ArrayList<>();
        final Map<String, String> thumbnailData = getImageThumbnailData();

        Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = MediaStore.Images.Media.query(mContentResolver, imageUri, IMAGE_QUERY_COLUMNS);

        if (cursor.moveToFirst()) {
            do {
                MediaItem newContent = getMediaItemFromCursor(cursor, thumbnailData);

                if (newContent != null && !imageIds.contains(newContent.getTag())) {
                    mMediaItems.add(newContent);
                    imageIds.add(newContent.getTag());
                }
            } while(cursor.moveToNext());
        }

        cursor.close();
    }

    /**
     * Helper method; creates a {@link java.util.Map} of media IDs to thumbnail data
     * @return
     *  thumbnail data map
     */
    private Map<String, String> getImageThumbnailData() {
        final Map<String, String> data = new HashMap<>();
        Cursor thumbnailCursor = MediaUtils.getMediaStoreThumbnails(mContentResolver, THUMBNAIL_QUERY_COLUMNS);

        if (thumbnailCursor != null) {
            if (thumbnailCursor.moveToFirst()) {
                do {
                    int thumbnailColumnIndex = thumbnailCursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA);
                    int imageIdColumnIndex = thumbnailCursor.getColumnIndex(MediaStore.Images.Thumbnails.IMAGE_ID);

                    if (thumbnailColumnIndex != -1 && imageIdColumnIndex != -1) {
                        data.put(thumbnailCursor.getString(imageIdColumnIndex), thumbnailCursor.getString(thumbnailColumnIndex));
                    }
                } while (thumbnailCursor.moveToNext());
            }

            thumbnailCursor.close();
        }

        return data;
    }

    /**
     * Helper method; creates a {@link org.wordpress.mediapicker.MediaItem} from cursor data
     *
     * @param imageCursor
     * @param thumbnailData
     * @return
     */
    private MediaItem getMediaItemFromCursor(Cursor imageCursor, Map<String, String> thumbnailData) {
        MediaItem newContent = null;

        int imageIdColumnIndex = imageCursor.getColumnIndex(MediaStore.Images.Media._ID);
        int imageDataColumnIndex = imageCursor.getColumnIndex(MediaStore.Images.Media.DATA);
        int imageOrientationColumnIndex = imageCursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION);

        if (imageIdColumnIndex != -1) {
            newContent = new MediaItem();
            newContent.setTag(imageCursor.getString(imageIdColumnIndex));
            newContent.setTitle("");

            if (imageDataColumnIndex != -1) {
                newContent.setSource(Uri.parse(imageCursor.getString(imageDataColumnIndex)));
            }
            if (thumbnailData.containsKey(newContent.getTag())) {
                newContent.setPreviewSource(Uri.parse(thumbnailData.get(newContent.getTag())));
            }
            if (imageOrientationColumnIndex != -1) {
                newContent.setRotation(imageCursor.getInt(imageOrientationColumnIndex));
            }
        }

        return newContent;
    }

    /*
        Parcelable interface
    */

    public static final Creator<MediaSourceDeviceImages> CREATOR =
            new Creator<MediaSourceDeviceImages>() {
                public MediaSourceDeviceImages createFromParcel(Parcel in) {
                    List<MediaItem> parcelData = new ArrayList<>();
                    in.readTypedList(parcelData, MediaItem.CREATOR);

                    if (parcelData.size() > 0) {
                        MediaSourceDeviceImages newItem = new MediaSourceDeviceImages();
                        newItem.setMediaItems(parcelData);

                        return newItem;
                    }

                    return null;
                }

                public MediaSourceDeviceImages[] newArray(int size) {
                    return new MediaSourceDeviceImages[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags) {
        if (mMediaItems != null) {
            destination.writeTypedList(mMediaItems);
        }
    }
}
