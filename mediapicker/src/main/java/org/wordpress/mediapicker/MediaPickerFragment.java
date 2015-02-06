package org.wordpress.mediapicker;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.mediapicker.source.MediaSource;

import java.util.ArrayList;

public class MediaPickerFragment extends Fragment
        implements AdapterView.OnItemClickListener,
        AbsListView.MultiChoiceModeListener {
    private static final String KEY_SELECTED_CONTENT = "selected-content";
    private static final String KEY_MEDIA_SOURCES    = "media-sources";

    public interface OnMediaSelected {
        // Called when the first item is selected
        public void onMediaSelectionStarted();
        // Called when a new item is selected
        public void onMediaSelected(MediaItem mediaContent, boolean selected);
        // Called when the user confirms content selection
        public void onMediaSelectionConfirmed(ArrayList<MediaItem> mediaContent);
        // Called when the last selected item is deselected
        public void onMediaSelectionCancelled();
        // Called when Gallery menu option has been selected
        public void onGalleryCreated(ArrayList<MediaItem> mediaContent);
        // Can handle null image cache
        public ImageLoader.ImageCache getImageCache();
    }

    private ArrayList<MediaItem>   mSelectedContent;
    private ArrayList<MediaSource> mMediaSources;
    private OnMediaSelected        mListener;
    private MediaSourceAdapter     mAdapter;
    private GridView               mGridView;
    private MenuItem               mGalleryMenuItem;

    public MediaPickerFragment() {
        super();

        mMediaSources = new ArrayList<>();
        mSelectedContent = new ArrayList<>();
    }

    public void setMediaSources(ArrayList<MediaSource> mediaSources) {
        mMediaSources = mediaSources;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof OnMediaSelected) {
            mListener = (OnMediaSelected) activity;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_SELECTED_CONTENT)) {
                mSelectedContent = savedInstanceState.getParcelableArrayList(KEY_SELECTED_CONTENT);
            }
            if (savedInstanceState.containsKey(KEY_MEDIA_SOURCES)) {
                mMediaSources = savedInstanceState.getParcelableArrayList(KEY_MEDIA_SOURCES);
            }
        }

        View mediaPickerView = inflater.inflate(R.layout.media_picker_fragment, container, false);
        if (mediaPickerView != null) {
            mGridView = (GridView) mediaPickerView.findViewById(R.id.mediaGridView);
            if (mGridView != null) {
                layoutGridView();
            }
        }

        return mediaPickerView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(KEY_SELECTED_CONTENT, mSelectedContent);
        outState.putParcelableArrayList(KEY_MEDIA_SOURCES, mMediaSources);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (notifyMediaSelected(position, true)) {
            notifyMediaSelectionConfirmed();
        }
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        notifyMediaSelected(position, checked);

        if (checked) {
            if (!mSelectedContent.contains(mAdapter.getItem(position))) {
                mSelectedContent.add(mAdapter.getItem(position));
            }
        } else {
            mSelectedContent.remove(mAdapter.getItem(position));
        }

        mGalleryMenuItem.setVisible(mSelectedContent.size() > 0);

        mode.setTitle(getActivity().getTitle() + " (" + mSelectedContent.size() + ")");
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle(getActivity().getTitle());
        getActivity().onActionModeStarted(mode);

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        notifyMediaSelectionStarted();
        getActivity().getMenuInflater().inflate(R.menu.media_picker_action_mode, menu);

        mGalleryMenuItem = menu.findItem(R.id.menu_media_content_selection_gallery);
        mGalleryMenuItem.setVisible(false);

        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_media_content_selection_gallery) {
            notifyGalleryCreated();
            mode.finish();
        } else if (menuItem.getItemId() == R.id.menu_media_selection_confirmed) {
            if (mSelectedContent.size() > 0) {
                notifyMediaSelectionConfirmed();
            }
            mode.finish();
        }

        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        notifyMediaSelectionCancelled();

        mSelectedContent.clear();
        mGalleryMenuItem = null;

        getActivity().onActionModeFinished(mode);
    }

    private void notifyMediaSelectionStarted() {
        if (mListener != null) {
            mListener.onMediaSelectionStarted();
        }
    }

    private boolean notifyMediaSelected(int position, boolean selected) {
        MediaItem mediaItem = mAdapter.getItem(position);

        if (mediaItem != null) {
            MediaSource mediaSource = mAdapter.sourceAtPosition(position);

            if (mediaSource == null || !mediaSource.onMediaItemSelected(mediaItem, selected)) {
                if (mListener != null) {
                    mListener.onMediaSelected(mediaItem, selected);
                }

                mSelectedContent.add(mediaItem);

                return true;
            }
        }

        return false;
    }

    private void notifyMediaSelectionConfirmed() {
        if (mListener != null) {
            mListener.onMediaSelectionConfirmed(mSelectedContent);
        }
    }

    private void notifyGalleryCreated() {
        if (mListener != null) {
            mListener.onGalleryCreated(mSelectedContent);
        }
    }

    private void notifyMediaSelectionCancelled() {
        if (mListener != null) {
            mListener.onMediaSelectionCancelled();
        }
    }

    /**
     * Helper method; creates the adapter and initializes the GridView to display it
     */
    private void layoutGridView() {
        Activity activity = getActivity();
        Resources resources = activity.getResources();
        int numColumns = resources.getInteger(R.integer.num_media_columns);
        int paddingLeft = Math.round(resources.getDimension(R.dimen.media_padding_left));
        int paddingTop = Math.round(resources.getDimension(R.dimen.media_padding_top));
        int paddingRight = Math.round(resources.getDimension(R.dimen.media_padding_right));
        int paddingBottom = Math.round(resources.getDimension(R.dimen.media_padding_bottom));
        int columnSpacingY = Math.round(resources.getDimension(R.dimen.media_spacing_vertical));
        int columnSpacingX = Math.round(resources.getDimension(R.dimen.media_spacing_horizontal));
        Drawable background = resources.getDrawable(R.drawable.media_picker_background);
        ImageLoader.ImageCache imageCache = mListener != null ? mListener.getImageCache() : null;

        mAdapter = new MediaSourceAdapter(activity, mMediaSources, imageCache);

        mGridView.setBackgroundDrawable(background);
        mGridView.setNumColumns(numColumns);
        mGridView.setVerticalSpacing(columnSpacingY);
        mGridView.setHorizontalSpacing(columnSpacingX);
        mGridView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        mGridView.setClipToPadding(false);
        mGridView.setMultiChoiceModeListener(this);
        mGridView.setOnItemClickListener(this);
        mGridView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mGridView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        mGridView.setAdapter(mAdapter);
    }
}
