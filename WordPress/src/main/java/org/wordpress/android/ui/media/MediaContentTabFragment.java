package org.wordpress.android.ui.media;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
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
import org.wordpress.android.ui.media.content.CaptureMediaContent;
import org.wordpress.android.ui.media.content.DeviceImageMediaContent;
import org.wordpress.android.ui.media.content.MediaContent;

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
    private boolean mCapturingMedia;
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

        if (selectedContent instanceof CaptureMediaContent) {
            captureMediaContent((CaptureMediaContent)selectedContent);
            MediaUtils.launchCamera(this, this);
        } else if (selectedContent != null) {
            mSelectedContent.add(selectedContent);

            if (mListener != null) {
                mListener.onMediaContentSelectionConfirmed(mSelectedContent);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        MediaContent selectedContent = (MediaContent) mAdapter.getItem(position);

        if (selectedContent instanceof CaptureMediaContent) {
            captureMediaContent((CaptureMediaContent) selectedContent);
            MediaUtils.launchCamera(this, this);
        } else if (selectedContent != null) {
            if (checked && !mSelectedContent.contains(selectedContent)) {
                mSelectedContent.add(selectedContent);
            }
            else if (mSelectedContent.contains(selectedContent)) {
                mSelectedContent.remove(selectedContent);
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
            if (mListener != null) {
                if (mSelectedContent.size() == 0) {
                    mListener.onMediaContentSelectionCancelled();
                }
                else {
                    mListener.onMediaContentSelectionConfirmed(mSelectedContent);
                }
            }

            mSelectedContent.clear();
        }
    }

    @Override
    public void onMediaCapturePathReady(String mediaCapturePath) {
        DeviceImageMediaContent newContent = new DeviceImageMediaContent("");
        newContent.setImageUri(mediaCapturePath);
        newContent.setThumbUri(mediaCapturePath);
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
                DeviceImageMediaContent newContent = imageContentFromQuery(imageCursor, thumbCursor);

                if (newContent != null && !mImageIds.contains(newContent.getId())) {
                    mAdapter.addContent(newContent);
                    mImageIds.add(newContent.getId());
                }
            } while(imageCursor.moveToNext() && thumbCursor.moveToNext());
        }
    }

    private void addMediaStoreVideos() {
//        String[] columns = { MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA, MediaStore.Video.Thumbnails.VIDEO_ID };
//        Cursor imageCursor = MediaUtils.getDeviceMediaStoreVideoThumbnails(getActivity().getContentResolver(), columns);
//
//        if (imageCursor.moveToFirst()) {
//            do {
//                String id = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.MediaColumns._ID));
//                String data = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
//                String name = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.MediaColumns.TITLE));
//
//                if(!mImageIds.contains(id)) {
//                    DeviceImageMediaContent newContent = new DeviceImageMediaContent(data);
//                    newContent.setName(name);
//                    mGridView.addContent(newContent);
//                    mImageIds.add(name);
//                }
//            } while(imageCursor.moveToNext());
//        }
    }

    private DeviceImageMediaContent imageContentFromQuery(Cursor imageCursor, Cursor thumbnailCursor) {
        DeviceImageMediaContent content = null;

        int imageIdColumnIndex = imageCursor.getColumnIndex(MediaStore.Images.Media._ID);
        int imageDataColumnIndex = imageCursor.getColumnIndex(MediaStore.Images.Media.DATA);
        int dateTakenColumnIndex = imageCursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
        int thumbnailColumnIndex = thumbnailCursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA);

        if (imageIdColumnIndex != -1) {
            String imageId = imageCursor.getString(imageIdColumnIndex);
            content = new DeviceImageMediaContent(imageId);

            if (imageDataColumnIndex != -1) {
                content.setImageUri(imageCursor.getString(imageDataColumnIndex));
            }
            if (dateTakenColumnIndex != -1) {
                String dateTaken = imageCursor.getString(dateTakenColumnIndex);
                try {
                    content.setName(DATE_DISPLAY_FORMAT.format(new Date(Long.valueOf(dateTaken))));
                } catch (NumberFormatException numberFormatException) {
                    Log.w("TEST", "Error formatting DATE_TAKEN(" + dateTaken + "): " + numberFormatException);
                }
            }
            if (thumbnailColumnIndex != -1) {
                content.setThumbUri(thumbnailCursor.getString(thumbnailColumnIndex));
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
            mAdapter.addContent(new CaptureMediaContent(CaptureMediaContent.CAPTURE_TYPE_IMAGE));
        }
        if ((mFilter & CAPTURE_VIDEO) != 0) {
            mAdapter.addContent(new CaptureMediaContent(CaptureMediaContent.CAPTURE_TYPE_VIDEO));
        }
        if ((mFilter & DEVICE_IMAGES) != 0) {
            addMediaStoreImages();
        }
        if ((mFilter & DEVICE_VIDEOS) != 0) {
            addMediaStoreVideos();
        }
    }

    private void captureMediaContent(CaptureMediaContent mediaContent) {
        if (mediaContent.isImageCapture()) {
            mCapturingMedia = true;
            MediaUtils.launchCamera(this, this);
        } else if (mediaContent.isVideoCapture()) {
            mCapturingMedia = true;
            MediaUtils.launchVideoCamera(this);
        }
    }
}