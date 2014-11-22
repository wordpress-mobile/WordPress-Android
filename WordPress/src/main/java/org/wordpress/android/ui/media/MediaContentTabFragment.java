package org.wordpress.android.ui.media;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AbsListView.MultiChoiceModeListener;

import org.wordpress.android.R;
import org.wordpress.android.util.AppLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Helper fragment for easy instantiation of common media sources. Will handle device sources
 * via MediaStore queries, capturing new media from device camera, and WordPress media assets.
 * Notifies results to its activity if it implements
 * {@link org.wordpress.android.ui.media.MediaContentTabFragment.OnMediaContentSelected}.
 */

public class MediaContentTabFragment extends Fragment implements OnItemClickListener,
                                                                 MultiChoiceModeListener,
                                                                 MediaUtils.LaunchCameraCallback {
    public interface OnMediaContentSelected {
        // Called when the first item is selected
        public void onMediaContentSelectionStarted();
        // Called when a new item is selected
        public void onMediaContentSelected(MediaContent mediaContent, boolean selected);
        // Called when the user confirms content selection
        public void onMediaContentSelectionConfirmed(ArrayList<MediaContent> mediaContent);
        // Called when the last selected item is deselected
        public void onMediaContentSelectionCancelled();
        public void onGalleryCreated(ArrayList<MediaContent> mediaContent);
        // TODO: public void onMenuItemClicked(int itemd);
    }

    public static final String FILTER_ARG = "KEY_FILTER";

    private static final SimpleDateFormat DATE_DISPLAY_FORMAT_THIS_YEAR = new SimpleDateFormat("MMM dd");
    private static final SimpleDateFormat DATE_DISPLAY_FORMAT_PREV_YEAR = new SimpleDateFormat("MMM dd, yyyy");

    // Bit flags for fragment filters
    public static final int NONE          = 0x00;
    public static final int CAPTURE_IMAGE = 0x01;
    public static final int CAPTURE_VIDEO = 0x02;
    public static final int DEVICE_IMAGES = 0x04;
    public static final int DEVICE_VIDEOS = 0x08;
    public static final int WP_IMAGES     = 0x10;
    public static final int WP_VIDEOS     = 0x20;

    private final ArrayList<MediaContent> mSelectedContent;

    private int                    mFilter;
    private boolean                mCapturingMedia;
    private OnMediaContentSelected mListener;
    private MediaContentAdapter    mAdapter;
    private GridView               mGridView;
    private List<String> mImageIds = new ArrayList<String>();
    private List<String> mVideoIds = new ArrayList<String>();

    public MediaContentTabFragment() {
        super();

        mSelectedContent = new ArrayList<MediaContent>();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof OnMediaContentSelected) {
            mListener = ((OnMediaContentSelected)activity);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mFilter = (args == null) ? NONE : args.getInt(FILTER_ARG);

        layoutGridView();
        applyFilters();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return mGridView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MediaContent selectedContent = (MediaContent)mAdapter.getItem(position);

        if (selectedContent != null) {
            if (selectedContent.getType() == MediaContent.MEDIA_TYPE.CAPTURE) {
                captureMediaContent(selectedContent);
            } else {
                mSelectedContent.add(selectedContent);
                notifyMediaSelectionConfirmed();
            }
        }
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        MediaContent selectedContent = (MediaContent) mAdapter.getItem(position);

        if (selectedContent != null) {
            if (selectedContent.getType() == MediaContent.MEDIA_TYPE.CAPTURE) {
                captureMediaContent(selectedContent);
            } else {
                if (checked && !mSelectedContent.contains(selectedContent)) {
                    mSelectedContent.add(selectedContent);
                    notifyMediaSelected(selectedContent, true);
                } else if (mSelectedContent.contains(selectedContent)) {
                    mSelectedContent.remove(selectedContent);
                    notifyMediaSelected(selectedContent, false);
                }
            }
        }

        mode.setTitle(mSelectedContent.size() + getString(R.string.media_selection_selected_text));
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle("Select content");

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        notifiyMediaSelectionStarted();
        getActivity().getMenuInflater().inflate(R.menu.media_content_selection, menu);

        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_media_content_selection_gallery) {
            notifyGalleryCreated();
            mode.finish();
            return true;
        } else if (menuItem.getItemId() == R.id.menu_media_content_selection_confirm) {
            if (mSelectedContent.size() > 0) {
                notifyMediaSelectionConfirmed();
            }
            mode.finish();
            return true;
        } else {
            notifyMediaSelectionConfirmed();
            return true;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        if (!mCapturingMedia) {
            notifiyMediaSelectionCancelled();
            mSelectedContent.clear();
        }
    }

    @Override
    public void onMediaCapturePathReady(String mediaCapturePath) {
        mCapturingMedia = false;

        if (mediaCapturePath != null && !mediaCapturePath.equals("")) {
            MediaContent newContent = new MediaContent(MediaContent.MEDIA_TYPE.DEVICE_IMAGE);
            newContent.setContentUri(Uri.parse(mediaCapturePath));
            newContent.setContentPreviewUri(Uri.parse(mediaCapturePath));
            mAdapter.addContent(newContent);
        }
    }

    private void notifiyMediaSelectionStarted() {
        if (mListener != null) {
            mListener.onMediaContentSelectionStarted();
        }
    }

    private void notifyMediaSelected(MediaContent content, boolean selected) {
        if (mListener != null) {
            mListener.onMediaContentSelected(content, selected);
        }
    }

    private void notifyMediaSelectionConfirmed() {
        if (mListener != null) {
            mListener.onMediaContentSelectionConfirmed(mSelectedContent);
        }
    }

    private void notifyGalleryCreated() {
        if (mListener != null) {
            mListener.onGalleryCreated(mSelectedContent);
        }
    }

    private void notifiyMediaSelectionCancelled() {
        if (mListener != null) {
            mListener.onMediaContentSelectionCancelled();
        }
    }

    /** Helper method to instantiate a GridView, adjust its layout, and give it an adapter. */
    private void layoutGridView() {
        Activity activity = getActivity();
        Resources resources = activity.getResources();
        int numColumns = resources.getInteger(R.integer.media_grid_num_columns);
        int gridPadding = Math.round(resources.getDimension(R.dimen.media_grid_padding));
        int columnSpacingY = Math.round(resources.getDimension(R.dimen.media_grid_column_spacing_vertical));
        int columnSpacingX = Math.round(resources.getDimension(R.dimen.media_grid_column_spacing_horizontal));

        mAdapter = new MediaContentAdapter(getActivity());
        mGridView = new GridView(activity);
        mGridView.setBackgroundColor(getResources().getColor(R.color.grey_extra_light));
        mGridView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        mGridView.setMultiChoiceModeListener(this);
        mGridView.setOnItemClickListener(this);
        mGridView.setNumColumns(numColumns);
        mGridView.setVerticalSpacing(columnSpacingY);
        mGridView.setHorizontalSpacing(columnSpacingX);
        mGridView.setPadding(gridPadding, 0, gridPadding, 0);
        mGridView.setAdapter(mAdapter);
    }

    /** Helper method to add content to the adapter based on the currently set filters. */
    private void applyFilters() {
        if ((mFilter & CAPTURE_IMAGE) != 0) {
            MediaContent captureImageContent = new MediaContent(MediaContent.MEDIA_TYPE.CAPTURE);
            captureImageContent.setTag("CaptureImage");
            mAdapter.addContent(captureImageContent);
        }
        if ((mFilter & CAPTURE_VIDEO) != 0) {
            MediaContent captureVideoContent = new MediaContent(MediaContent.MEDIA_TYPE.CAPTURE);
            captureVideoContent.setTag("CaptureVideo");
            mAdapter.addContent(captureVideoContent);
        }
        if ((mFilter & DEVICE_IMAGES) != 0) {
            addMediaStoreImages();
        }
        if ((mFilter & DEVICE_VIDEOS) != 0) {
            addMediaStoreVideos();
        }
        if ((mFilter & WP_IMAGES) != 0) {
            addWordPressImages();
        }
        if ((mFilter & WP_VIDEOS) != 0) {
            addWordPressVideos();
        }
    }

    private void captureMediaContent(MediaContent mediaContent) {
        String tag = mediaContent.getTag();

        if (tag != null && tag.equals("CaptureVideo")) {
            MediaUtils.launchVideoCamera(this);
        } else {
            MediaUtils.launchCamera(this, this);
        }

        mCapturingMedia = true;
    }

    /** Helper method to load image and image thumbnail data to add device media content to the adapter. */
    private void addMediaStoreImages() {
        ContentResolver contentResolver = getActivity().getContentResolver();
        String[] imageColumns= { MediaStore.Images.Media._ID,
                                 MediaStore.Images.Media.DATA,
                                 MediaStore.Images.Media.DATE_TAKEN };
        String[] thumbnailColumns = { MediaStore.Images.Thumbnails._ID,
                                      MediaStore.Images.Thumbnails.DATA };
        Cursor thumbCursor = MediaUtils.getDeviceMediaStoreImageThumbnails(contentResolver, thumbnailColumns);
        Cursor imageCursor = MediaUtils.getDeviceMediaStoreImages(contentResolver, imageColumns);

        if (imageCursor.moveToFirst() && thumbCursor.moveToFirst()) {
            do {
                MediaContent newContent = imageContentFromQuery(imageCursor, thumbCursor);

                if (newContent != null && !mImageIds.contains(newContent.getContentId())) {
                    mAdapter.addContent(newContent);
                    mImageIds.add(newContent.getContentId());
                }
            } while(imageCursor.moveToNext() && thumbCursor.moveToNext());
        }

        thumbCursor.close();
        imageCursor.close();
    }

    private void addMediaStoreVideos() {
        ContentResolver contentResolver = getActivity().getContentResolver();
        String[] videoColumns= { MediaStore.Video.Media._ID,
                                 MediaStore.Video.Media.DATA,
                                 MediaStore.Video.Media.DATE_TAKEN };
        Cursor videoCursor = MediaUtils.getDeviceMediaStoreVideos(contentResolver, videoColumns);

        if (videoCursor.moveToFirst()) {
            do {
                MediaContent newContent = videoContentFromQuery(videoCursor);

                if (newContent != null && !mVideoIds.contains(newContent.getContentId())) {
                    mAdapter.addContent(newContent);
                    mVideoIds.add(newContent.getContentId());
                }
            } while(videoCursor.moveToNext());
        }

        videoCursor.close();
    }

    private void addWordPressImages() {
        Cursor wordPressImages = MediaUtils.getWordPressMediaImages();

        if (wordPressImages != null) {
            addWordPressImagesFromCursor(wordPressImages);
            wordPressImages.close();
        }
    }

    private void addWordPressVideos() {
        Cursor wordPressVideos = MediaUtils.getWordPressMediaVideos();

        if (wordPressVideos != null) {
            addWordPressVideosFromCursor(wordPressVideos);
            wordPressVideos.close();
        }
    }

    private void addWordPressImagesFromCursor(Cursor cursor) {
        if (cursor.moveToFirst()) {
            do {
                int attachmentIdColumnIndex = cursor.getColumnIndex("mediaId");
                int fileUrlColumnIndex = cursor.getColumnIndex("fileURL");
                int thumbnailColumnIndex = cursor.getColumnIndex("thumbnailURL");
                int dateCreatedColumnIndex = cursor.getColumnIndex("date_created_gmt");

                String id = "";
                if (attachmentIdColumnIndex != -1) {
                    id = String.valueOf(cursor.getInt(attachmentIdColumnIndex));
                }
                MediaContent newContent = new MediaContent(MediaContent.MEDIA_TYPE.WEB_IMAGE);
                newContent.setContentId(id);

                if (fileUrlColumnIndex != -1) {
                    String fileUrl = cursor.getString(fileUrlColumnIndex);
                    newContent.setContentUri(Uri.parse(fileUrl));
                }

                if (dateCreatedColumnIndex != -1) {
                    String dateTaken = cursor.getString(dateCreatedColumnIndex);
                    newContent.setContentTitle(formattedDateFromEpoch(Long.valueOf(dateTaken)));
                }
                if (thumbnailColumnIndex != -1) {
                    newContent.setContentPreviewUri(Uri.parse(cursor.getString(thumbnailColumnIndex)));
                }
                mAdapter.addContent(newContent);
            } while (cursor.moveToNext());
        }
    }

    private void addWordPressVideosFromCursor(Cursor cursor) {
        if (cursor.moveToFirst()) {
            do {
                int attachmentIdColumnIndex = cursor.getColumnIndex("mediaId");
                int fileUrlColumnIndex = cursor.getColumnIndex("fileURL");
                int thumbnailColumnIndex = cursor.getColumnIndex("thumbnailURL");
                int dateCreatedColumnIndex = cursor.getColumnIndex("date_created_gmt");

                String id = "";
                if (attachmentIdColumnIndex != -1) {
                    id = String.valueOf(cursor.getInt(attachmentIdColumnIndex));
                }
                MediaContent newContent = new MediaContent(MediaContent.MEDIA_TYPE.WEB_VIDEO);
                newContent.setContentId(id);

                if (fileUrlColumnIndex != -1) {
                    String fileUrl = cursor.getString(fileUrlColumnIndex);
                    newContent.setContentUri(Uri.parse(fileUrl));
                }

                if (dateCreatedColumnIndex != -1) {
                    String dateTaken = cursor.getString(dateCreatedColumnIndex);
                    newContent.setContentTitle(formattedDateFromEpoch(Long.valueOf(dateTaken)));
                }
                if (thumbnailColumnIndex != -1) {
                    newContent.setContentPreviewUri(Uri.parse(cursor.getString(thumbnailColumnIndex)));
                }
                if (newContent.getContentUri().toString().endsWith(".mp4")) {
                    mAdapter.addContent(newContent);
                }
            } while (cursor.moveToNext());
        }
    }

    private MediaContent imageContentFromQuery(Cursor imageCursor, Cursor thumbnailCursor) {
        MediaContent content = null;

        int imageIdColumnIndex = imageCursor.getColumnIndex(MediaStore.Images.Media._ID);
        int imageDataColumnIndex = imageCursor.getColumnIndex(MediaStore.Images.Media.DATA);
        int dateTakenColumnIndex = imageCursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
        int thumbnailColumnIndex = thumbnailCursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA);

        if (imageIdColumnIndex != -1) {
            content = new MediaContent(MediaContent.MEDIA_TYPE.DEVICE_IMAGE);
            content.setContentId(imageCursor.getString(imageIdColumnIndex));

            if (imageDataColumnIndex != -1) {
                content.setContentUri(Uri.parse(imageCursor.getString(imageDataColumnIndex)));
            }
            if (dateTakenColumnIndex != -1) {
                String dateTaken = imageCursor.getString(dateTakenColumnIndex);
                content.setContentTitle(formattedDateFromEpoch(Long.valueOf(dateTaken)));
            }
            if (thumbnailColumnIndex != -1) {
                content.setContentPreviewUri(Uri.parse(thumbnailCursor.getString(thumbnailColumnIndex)));
            }
        }

        return content;
    }

    private MediaContent videoContentFromQuery(Cursor videoCursor) {
        MediaContent content = null;

        int videoIdColumnIndex = videoCursor.getColumnIndex(MediaStore.Video.Media._ID);
        int videoDataColumnIndex = videoCursor.getColumnIndex(MediaStore.Video.Media.DATA);
        int dateTakenColumnIndex = videoCursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN);

        if (videoIdColumnIndex != -1) {
            content = new MediaContent(MediaContent.MEDIA_TYPE.DEVICE_VIDEO);
            content.setContentId(videoCursor.getString(videoIdColumnIndex));

            if (videoDataColumnIndex != -1) {
                content.setContentUri(Uri.parse(videoCursor.getString(videoDataColumnIndex)));
                content.setContentPreviewUri(content.getContentUri());
            }
            if (dateTakenColumnIndex != -1) {
                String dateTaken = videoCursor.getString(dateTakenColumnIndex);
                try {
                    content.setContentTitle(formattedDateFromEpoch(Long.valueOf(dateTaken)));
                } catch (NumberFormatException numberFormatException) {
                    AppLog.d(AppLog.T.MEDIA, "Error formatting DATE_TAKEN(" + dateTaken + "): " + numberFormatException);
                }
            }
        }

        return content;
    }

    private String formattedDateFromEpoch(long timeSinceEpoch) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        long startOfThisYear = calendar.getTimeInMillis();

        try {
            if (timeSinceEpoch < startOfThisYear) {
                return DATE_DISPLAY_FORMAT_PREV_YEAR.format(new Date(timeSinceEpoch));
            } else {
                return DATE_DISPLAY_FORMAT_THIS_YEAR.format(new Date(timeSinceEpoch));
            }
        } catch (NumberFormatException numberFormatException) {
            AppLog.d(AppLog.T.MEDIA, "Error formatting DATE_TAKEN(" + timeSinceEpoch + "): " + numberFormatException);
        }

        return null;
    }
}
