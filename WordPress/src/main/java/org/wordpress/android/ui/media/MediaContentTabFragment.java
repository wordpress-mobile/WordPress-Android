package org.wordpress.android.ui.media;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;

import org.wordpress.android.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Helper fragment for easy instantiation of common media sources. Will handle device sources
 * via MediaStore queries, capturing new media from device camera, and WordPress media assets.
 * Notifies results to its activity if it implements
 * {@link org.wordpress.android.ui.media.MediaContentTabFragment.OnMediaContentSelected}.
 */

public class MediaContentTabFragment extends Fragment implements AdapterView.OnItemClickListener,
                                                                 AbsListView.MultiChoiceModeListener, MediaUtils.LaunchCameraCallback {
    public interface OnMediaContentSelected {
        // Called when the first item is selected
        public void onMediaContentSelectionStarted();
        // Called when a new item is selected
        public void onMediaContentSelected(MediaContent mediaContent, boolean selected);
        // Called when the user confirms content selection
        public void onMediaContentSelectionConfirmed(ArrayList<MediaContent> mediaContent);
        // Called when the last selected item is deselected
        public void onMediaContentSelectionCancelled();
        // TODO: public void onMenuItemClicked(int itemd);
    }

    public static final String FILTER_ARG = "KEY_FILTER";

    private static final SimpleDateFormat DATE_DISPLAY_FORMAT = new SimpleDateFormat("MMMM dd, yyyy");

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        layoutGridView();
        applyFilters();

        return mGridView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MediaContent selectedContent = (MediaContent) mAdapter.getItem(position);

        if (selectedContent != null) {
            if (selectedContent.getType() == MediaContent.MEDIA_TYPE.CAPTURE) {
                captureMediaContent(selectedContent);
                MediaUtils.launchCamera(this, this);
            }
            else {
                mSelectedContent.add(selectedContent);

                if (mListener != null) {
                    mListener.onMediaContentSelectionConfirmed(mSelectedContent);
                }
            }
        }
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        MediaContent selectedContent = (MediaContent) mAdapter.getItem(position);

        if (selectedContent != null) {
            if (selectedContent.getType() == MediaContent.MEDIA_TYPE.CAPTURE) {
                captureMediaContent(selectedContent);
                MediaUtils.launchCamera(this, this);
            }
            else {
                if (checked && !mSelectedContent.contains(selectedContent)) {
                    mSelectedContent.add(selectedContent);
                }
                else if (mSelectedContent.contains(selectedContent)) {
                    mSelectedContent.remove(selectedContent);
                }
            }
        }

        mode.setTitle(mSelectedContent.size() + " items selected");
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle("Select content");

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        if (mListener != null) {
            mListener.onMediaContentSelectionStarted();
        }

        getActivity().getMenuInflater().inflate(R.menu.media_content_selection, menu);

        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_media_content_selection_gallery) {
            return true;
        } else if (menuItem.getItemId() == R.id.menu_media_content_selection_cancel) {
            mSelectedContent.clear();
            mode.finish();
            return true;
        } else if (mListener != null) {
            mListener.onMediaContentSelectionConfirmed(mSelectedContent);
            return true;
        }

        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        if (!mCapturingMedia) {
            if (mSelectedContent.size() > 0) {
                if (mListener != null) {
                    mListener.onMediaContentSelectionConfirmed(mSelectedContent);
                }
            } else {
                if (mListener != null) {
                    mListener.onMediaContentSelectionCancelled();
                }
            }

            mSelectedContent.clear();
        }
    }

    @Override
    public void onMediaCapturePathReady(String mediaCapturePath) {
        MediaContent newContent = new MediaContent(MediaContent.MEDIA_TYPE.CAPTURE);
        newContent.setContentUri(Uri.parse(mediaCapturePath));
        newContent.setContentPreviewUri(Uri.parse(mediaCapturePath));
        mAdapter.addContent(newContent);
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
    }

    private void addMediaStoreVideos() {
    }

    private void addWordPressImagesFromCursor(Cursor cursor) {
        if (cursor.moveToFirst()) {
            do {
                int attachmentIdColumnIndex = cursor.getColumnIndex("mediaId");
                int titleIdColumnIndex = cursor.getColumnIndex("title");
                int fileUrlColumnIndex = cursor.getColumnIndex("fileURL");
                int thumbnailColumnIndex = cursor.getColumnIndex("thumbnailURL");
                int dateCreatedColumnIndex = cursor.getColumnIndex("date_created_gmt");
                int widthColumnIndex = cursor.getColumnIndex("width");
                int heightColumnIndex = cursor.getColumnIndex("height");

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
                    try {
                        newContent.setContentTitle(DATE_DISPLAY_FORMAT.format(new Date(Long.valueOf(dateTaken))));
                    } catch (NumberFormatException numberFormatException) {
                        Log.w("TEST", "Error formatting DATE_TAKEN(" + dateTaken + "): " + numberFormatException);
                    }
                }
                if (thumbnailColumnIndex != -1) {
                    newContent.setContentPreviewUri(Uri.parse(cursor.getString(thumbnailColumnIndex)));
                }
                mAdapter.addContent(newContent);
            } while (cursor.moveToNext());
        }
    }

    private void addWordPressImages() {
        Cursor wordPressImages = MediaUtils.getWordPressMediaImages();

        if (wordPressImages != null) {
            addWordPressImagesFromCursor(wordPressImages);
        }
    }

    private void addWordPressVideos() {
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
                try {
                    content.setContentTitle(DATE_DISPLAY_FORMAT.format(new Date(Long.valueOf(dateTaken))));
                } catch (NumberFormatException numberFormatException) {
                    Log.w("TEST", "Error formatting DATE_TAKEN(" + dateTaken + "): " + numberFormatException);
                }
            }
            if (thumbnailColumnIndex != -1) {
                content.setContentPreviewUri(Uri.parse(thumbnailCursor.getString(thumbnailColumnIndex)));
            }
        }

        return content;
    }

    /** Helper method to instantiate a GridView, adjust its layout, and give it an adapter. */
    private void layoutGridView() {
        Activity activity = getActivity();
        Resources resources = activity.getResources();
        int numColumns = resources.getInteger(R.integer.media_grid_num_columns);
        int gridPadding = Math.round(resources.getDimension(R.dimen.media_grid_padding));
        int columnSpacingY = Math.round(resources.getDimension(R.dimen.media_grid_column_spacing_vertical));
        int columnSpacingX = Math.round(resources.getDimension(R.dimen.media_grid_column_spacing_horizontal));

        mGridView = new GridView(activity);
        mGridView.setBackgroundColor(getResources().getColor(R.color.grey_extra_light));
        mGridView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        mGridView.setOnItemClickListener(this);
        mGridView.setNumColumns(numColumns);
        mGridView.setVerticalSpacing(columnSpacingY);
        mGridView.setHorizontalSpacing(columnSpacingX);
        mGridView.setPadding(gridPadding, gridPadding, gridPadding, gridPadding);

        mAdapter = new MediaContentAdapter(getActivity());
        mGridView.setAdapter(mAdapter);
        mGridView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mGridView.setMultiChoiceModeListener(this);
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
}
