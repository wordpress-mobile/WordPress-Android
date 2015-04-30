package org.wordpress.android.ui.media;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcel;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.Blog;
import org.wordpress.mediapicker.MediaItem;
import org.wordpress.mediapicker.source.MediaSource;
import org.wordpress.mediapicker.MediaUtils.LimitedBackgroundOperation;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MediaSourceWPImages implements MediaSource {
    private final List<MediaItem> mVerifiedItems = new ArrayList<>();
    private final List<MediaItem> mMediaItems = new ArrayList<>();

    private OnMediaChange mListener;

    public MediaSourceWPImages() {
    }

    @Override
    public void gather(Context context) {
        mMediaItems.clear();

        Blog blog = WordPress.getCurrentBlog();

        if (blog != null) {
            Cursor imageCursor = WordPressMediaUtils.getWordPressMediaImages(String.valueOf(blog.getLocalTableBlogId()));

            if (imageCursor != null) {
                addWordPressImagesFromCursor(imageCursor);
                imageCursor.close();
            } else if (mListener != null){
                mListener.onMediaLoaded(false);
            }
        } else if (mListener != null){
            mListener.onMediaLoaded(false);
        }
    }

    @Override
    public void cleanup() {
        mMediaItems.clear();
    }

    @Override
    public void setListener(OnMediaChange listener) {
        mListener = listener;
    }

    @Override
    public int getCount() {
        return mVerifiedItems.size();
    }

    @Override
    public MediaItem getMedia(int position) {
        return mVerifiedItems.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent, LayoutInflater inflater, ImageLoader.ImageCache cache) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.media_item_wp_image, parent, false);
        }

        if (convertView != null) {
            MediaItem mediaItem = mVerifiedItems.get(position);
            Uri imageSource = mediaItem.getPreviewSource();
            ImageView imageView = (ImageView) convertView.findViewById(R.id.wp_image_view_background);
            if (imageView != null) {
                if (imageSource != null) {
                    Bitmap imageBitmap = null;
                    if (cache != null) {
                        imageBitmap = cache.getBitmap(imageSource.toString());
                    }

                    if (imageBitmap == null) {
                        imageView.setImageDrawable(placeholderDrawable(convertView.getContext()));
                        WordPressMediaUtils.BackgroundDownloadWebImage bgDownload =
                                new WordPressMediaUtils.BackgroundDownloadWebImage(imageView);
                        imageView.setTag(bgDownload);
                        bgDownload.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, mediaItem.getPreviewSource());
                    } else {
                        imageView.setImageBitmap(imageBitmap);
                    }
                } else {
                    imageView.setTag(null);
                    imageView.setImageResource(R.color.grey_darken_10);
                }
            }
        }

        return convertView;
    }

    @Override
    public boolean onMediaItemSelected(MediaItem mediaItem, boolean selected) {
        return !selected;
    }

    private Drawable placeholderDrawable(Context context) {
        if (context != null && context.getResources() != null) {
            return context.getResources().getDrawable(R.drawable.media_item_placeholder);
        }

        return null;
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

            removeDeletedEntries();
        } else if (mListener != null) {
            mListener.onMediaLoaded(true);
        }
    }

    private void removeDeletedEntries() {
        final List<MediaItem> existingItems = new ArrayList<>(mMediaItems);
        final List<MediaItem> failedItems = new ArrayList<>();

        for (final MediaItem mediaItem : existingItems) {
            LimitedBackgroundOperation<MediaItem, Void, MediaItem> backgroundCheck =
                    new LimitedBackgroundOperation<MediaItem, Void, MediaItem>() {
                private int responseCode;

                @Override
                protected MediaItem performBackgroundOperation(MediaItem[] params) {
                    MediaItem mediaItem = params[0];
                        try {
                            URL mediaUrl = new URL(mediaItem.getSource().toString());
                            HttpURLConnection connection = (HttpURLConnection) mediaUrl.openConnection();
                            connection.setRequestMethod("GET");
                            connection.connect();
                            responseCode = connection.getResponseCode();
                        } catch (IOException ioException) {
                            Log.e("", "Error reading from " + mediaItem.getSource() + "\nexception:" + ioException);

                            return null;
                        }

                    return mediaItem;
                }

                @Override
                public void performPostExecute(MediaItem result) {
                    if (mListener != null && result != null) {
                        if (responseCode == 200) {
                            mVerifiedItems.add(result);
                            List<MediaItem> resultList = new ArrayList<>();
                            resultList.add(result);

                            // Only signal newly loaded data every 3 images
                            if ((existingItems.size() - mVerifiedItems.size()) % 3 == 0) {
                                mListener.onMediaAdded(MediaSourceWPImages.this, resultList);
                            }
                        } else {
                            failedItems.add(result);
                        }

                        // Notify of all media loaded if all have been processed
                        if ((failedItems.size() + mVerifiedItems.size()) == existingItems.size()) {
                            mListener.onMediaLoaded(true);
                        }
                    }
                }

                @Override
                public void startExecution(Object params) {
                    if (!(params instanceof MediaItem)) {
                        throw new IllegalArgumentException("Params must be of type MediaItem");
                    }
                    executeOnExecutor(THREAD_POOL_EXECUTOR, (MediaItem) params);
                }
            };
            backgroundCheck.executeWithLimit(mediaItem);
        }
    }

    /**
     * {@link android.os.Parcelable} interface
     */

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
}
