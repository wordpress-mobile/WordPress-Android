package org.wordpress.android.ui.media;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcel;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageView;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.Post;
import org.wordpress.mediapicker.MediaItem;
import org.wordpress.mediapicker.source.MediaSource;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MediaSourcePostImages implements MediaSource {
    private final List<MediaItem> mVerifiedItems = new ArrayList<>();
    private final List<MediaItem> mMediaItems = new ArrayList<>();

    private OnMediaChange mListener;

    List<String> mDuplicateChecker = new ArrayList<>();

    public MediaSourcePostImages() {
    }

    @Override
    public void gather(Context context) {
        mMediaItems.clear();
        mDuplicateChecker.clear();

        Cursor allImages = WordPressMediaUtils.getWordPressMediaImages();

        // Todo: It would be much better if we can get images from the following cursor
        // Cursor allImages = WordPress.wpDB.getMediaImagesForPost(WordPress.currentPost.getLocalTablePostId());
        // Log.i("media", "postimages: " + postImages);

        if (allImages != null) {
            addWordPressImagesFromCursor(allImages);
            allImages.close();
        } else if (mListener != null) {
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
                        imageView.setImageResource(R.color.grey_darken_10);
                        WordPressMediaUtils.BackgroundDownloadWebImage bgDownload = new WordPressMediaUtils.BackgroundDownloadWebImage(imageView);
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

            getVerifiedEntries();
        } else if (mListener != null) {
            mListener.onMediaLoaded(true);
        }
    }

    private void getVerifiedEntries() {
        List<MediaItem> existingItems = new ArrayList<>(mMediaItems);

        for (MediaItem mediaItem : existingItems) {
            final boolean callLoaded = existingItems.indexOf(mediaItem) == existingItems.size() - 1;

            AsyncTask<MediaItem, Void, MediaItem> backgroundCheck = new AsyncTask<MediaItem, Void, MediaItem>() {
                int responseCode;

                @Override
                protected MediaItem doInBackground(MediaItem[] params) {
                    MediaItem mediaItem = params[0];
                    Post post = WordPress.currentPost;
                    String path = mediaItem.getSource().toString();
                    String noHttpsPath = path.replace("https", "http");

                    if (post.getPostImages().contains(path) || post.getPostImages().contains(noHttpsPath)) {
                        if (URLUtil.isNetworkUrl(path)) {
                            try {
                                URL mediaUrl = new URL(path);
                                HttpURLConnection connection = (HttpURLConnection) mediaUrl.openConnection();
                                connection.setRequestMethod("GET");
                                connection.connect();
                                responseCode = connection.getResponseCode();
                            } catch (IOException ioException) {
                                Log.e("", "Error reading from " + mediaItem.getSource() + "\nexception:" + ioException);

                                return null;
                            }
                        }

                        return mediaItem;
                    }

                    return null;
                }

                @Override
                public void onPostExecute(MediaItem result) {
                    if (mListener != null && result != null) {
                        List<MediaItem> resultList = new ArrayList<>();
                        String path = result.getSource().toString();
                        resultList.add(result);

                        if (URLUtil.isNetworkUrl(path)) {
                            if (responseCode == 200 && !mDuplicateChecker.contains(path)) {
                                mVerifiedItems.add(result);
                                mDuplicateChecker.add(path);
                            }
                        } else {
                            if (!mDuplicateChecker.contains(path)) {
                                mVerifiedItems.add(result);
                                mDuplicateChecker.add(path);
                            }
                        }

                        mListener.onMediaAdded(MediaSourcePostImages.this, mVerifiedItems);

                        if (callLoaded) {
                            mListener.onMediaLoaded(true);
                        }
                    }
                }
            };
            backgroundCheck.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mediaItem);
        }
    }

    /**
     * {@link android.os.Parcelable} interface
     */

    public static final Creator<MediaSourcePostImages> CREATOR =
            new Creator<MediaSourcePostImages>() {
                public MediaSourcePostImages createFromParcel(Parcel in) {
                    return new MediaSourcePostImages();
                }

                public MediaSourcePostImages[] newArray(int size) {
                    return new MediaSourcePostImages[size];
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
