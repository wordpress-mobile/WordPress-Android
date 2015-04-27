package org.wordpress.android.ui.media;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.mediapicker.MediaItem;
import org.wordpress.mediapicker.source.MediaSource;

import java.util.ArrayList;
import java.util.List;

public class MediaSourceWPVideos implements MediaSource {
    private static final String VIDEO_PRESS_HOST = "https://videos.files.wordpress.com/";
    private static final String VIDEO_PRESS_THUMBNAIL_APPEND = "_hd.thumbnail.jpg";

    private OnMediaChange mListener;
    private List<MediaItem> mMediaItems = new ArrayList<>();

    public MediaSourceWPVideos() {
    }

    @Override
    public void gather(Context context) {
        Blog blog = WordPress.getCurrentBlog();

        if (blog != null) {
            Cursor videoCursor = WordPressMediaUtils.getWordPressMediaVideos(String.valueOf(blog.getLocalTableBlogId()));

            if (videoCursor != null) {
                addWordPressVideosFromCursor(videoCursor);
                videoCursor.close();
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
        return mMediaItems.size();
    }

    @Override
    public MediaItem getMedia(int position) {
        return mMediaItems.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent, LayoutInflater inflater, ImageLoader.ImageCache cache) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.media_item_wp_video, parent, false);
        }

        if (convertView != null) {
            MediaItem mediaItem = mMediaItems.get(position);
            Uri imageSource = mediaItem.getPreviewSource();
            ImageView imageView = (ImageView) convertView.findViewById(R.id.wp_video_view_background);
            if (imageView != null) {
                if (imageSource != null) {
                    Bitmap imageBitmap = null;
                    if (cache != null) {
                        imageBitmap = cache.getBitmap(imageSource.toString());
                    }

                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    if (imageBitmap == null) {
                        imageView.setImageDrawable(placeholderDrawable(convertView.getContext()));
                        WordPressMediaUtils.BackgroundDownloadWebImage bgDownload =
                                new WordPressMediaUtils.BackgroundDownloadWebImage(imageView);
                        imageView.setTag(bgDownload);
                        bgDownload.execute(mediaItem.getPreviewSource());
                    } else {
                        org.wordpress.mediapicker.MediaUtils.fadeInImage(imageView, imageBitmap);
                    }
                } else {
                    imageView.setTag(null);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    imageView.setImageResource(R.drawable.video_thumbnail);
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

    /**
     * Helper method; removes unnecessary characters from videoPressShortcode cursor value
     *
     * @param cursorEntry
     *  the cursor value for the videoPressShortcode key
     * @return
     *  the VideoPress code
     */
    private String extractVideoPressCode(String cursorEntry) {
        cursorEntry = cursorEntry.replace("[wpvideo ", "");
        cursorEntry = cursorEntry.substring(0, cursorEntry.length() - 1);

        return cursorEntry;
    }

    private void addWordPressVideosFromCursor(Cursor cursor) {
        if (cursor.moveToFirst()) {
            do {
                int attachmentIdColumnIndex = cursor.getColumnIndex(WordPressDB.COLUMN_NAME_MEDIA_ID);
                int fileUrlColumnIndex = cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_URL);
                int fileNameColumnIndex = cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_NAME);
                int videoPressColumnIndex = cursor.getColumnIndex(WordPressDB.COLUMN_NAME_VIDEO_PRESS_SHORTCODE);

                String id = "";
                if (attachmentIdColumnIndex != -1) {
                    id = String.valueOf(cursor.getInt(attachmentIdColumnIndex));
                }
                MediaItem newContent = new MediaItem();
                newContent.setTag(id);
                newContent.setTitle("");

                if (fileUrlColumnIndex != -1) {
                    String fileUrl = cursor.getString(fileUrlColumnIndex);

                    if (fileUrl != null && MediaUtils.isVideo(fileUrl)) {
                        newContent.setSource(Uri.parse(fileUrl));
                    } else {
                        continue;
                    }
                }

                if (videoPressColumnIndex != -1 && fileNameColumnIndex != -1) {
                    String videoPressCode = cursor.getString(videoPressColumnIndex);
                    String fileName = cursor.getString(fileNameColumnIndex);

                    if (videoPressCode != null && !videoPressCode.isEmpty() && fileName != null && !fileName.isEmpty()) {
                        videoPressCode = extractVideoPressCode(videoPressCode);
                        newContent.setPreviewSource(VIDEO_PRESS_HOST + videoPressCode + "/" + fileName + VIDEO_PRESS_THUMBNAIL_APPEND);
                    }
                }

                mMediaItems.add(newContent);
            } while (cursor.moveToNext());
        }

        if (mListener != null) {
            mListener.onMediaLoaded(true);
        }
    }

    /**
     * {@link android.os.Parcelable} interface
     */

    public static final Creator<MediaSourceWPVideos> CREATOR =
            new Creator<MediaSourceWPVideos>() {
                public MediaSourceWPVideos createFromParcel(Parcel in) {
                    return new MediaSourceWPVideos();
                }

                public MediaSourceWPVideos[] newArray(int size) {
                    return new MediaSourceWPVideos[size];
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
