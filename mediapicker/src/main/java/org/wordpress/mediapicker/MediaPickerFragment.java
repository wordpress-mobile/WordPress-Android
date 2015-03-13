package org.wordpress.mediapicker;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.mediapicker.source.MediaSource;

import java.util.ArrayList;

/**
 * A custom view may be provided. In order to work properly an AdapterView with id=media_adapter_view
 * needs to be provided. A TextView may also be provided with id=media_empty_view to show loading
 * and empty status.
 */

public class MediaPickerFragment extends Fragment
        implements AdapterView.OnItemClickListener,
                   AbsListView.MultiChoiceModeListener,
                   MediaSource.OnMediaChange {
    private static final String KEY_SELECTED_CONTENT = "selected-content";
    private static final String KEY_MEDIA_SOURCES    = "media-sources";
    private static final String KEY_ACTION_MODE_MENU = "action-mode-menu";

    public interface OnMediaSelected {
        // Called when the first item is selected
        public void onMediaSelectionStarted();
        // Called when a new item is selected
        public void onMediaSelected(MediaItem mediaContent, boolean selected);
        // Called when the user confirms content selection
        public void onMediaSelectionConfirmed(ArrayList<MediaItem> mediaContent);
        // Called when the last selected item is deselected
        public void onMediaSelectionCancelled();
        // Called when a menu item has been tapped
        public boolean onMenuItemSelected(MenuItem menuItem);
        // Can handle null image cache
        public ImageLoader.ImageCache getImageCache();
    }

    private ArrayList<MediaItem>   mSelectedContent;
    private ArrayList<MediaSource> mMediaSources;
    private OnMediaSelected        mListener;
    private MediaSourceAdapter     mAdapter;
    private GridView               mGridView;
    private MenuItem               mGalleryMenuItem;
    private int                    mActionModeMenu;

    public MediaPickerFragment() {
        super();

        mActionModeMenu = -1;
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

            if (savedInstanceState.containsKey(KEY_ACTION_MODE_MENU)) {
                mActionModeMenu = savedInstanceState.getInt(KEY_ACTION_MODE_MENU);
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
        outState.putInt(KEY_ACTION_MODE_MENU, mActionModeMenu);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (!notifyMediaSelected(position, true)) {
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

        if (mGalleryMenuItem != null) {
            mGalleryMenuItem.setVisible(mSelectedContent.size() > 0);
        }

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

        MenuInflater menuInflater = getActivity().getMenuInflater();

        if (mActionModeMenu != -1) {
            menuInflater.inflate(mActionModeMenu, menu);
            addSelectionConfirmationButtonMenuItem(menu);
        } else {
            menuInflater.inflate(R.menu.media_picker_action_mode, menu);
        }

        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_media_selection_confirmed) {
            notifyMediaSelectionConfirmed();
            mode.finish();
            return true;
        } else if (mListener != null) {
            return mListener.onMenuItemSelected(menuItem);
        }

        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        notifyMediaSelectionCancelled();

        mSelectedContent.clear();
        mGalleryMenuItem = null;

        getActivity().onActionModeFinished(mode);
    }

    @Override
    public void onMediaLoaded(boolean success) {
    }

    @Override
    public void onMediaAdded(MediaSource source, List<MediaItem> addedItems) {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onMediaRemoved(MediaSource source, List<MediaItem> removedItems) {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onMediaChanged(MediaSource source, List<MediaItem> changedItems) {
        mAdapter.notifyDataSetChanged();
    }

    public void setActionModeMenu(int id) {
        mActionModeMenu = id;
    }

    public void setListener(OnMediaSelected listener) {
        mListener = listener;
    }

    public void setAdapter(MediaSourceAdapter adapter) {
        mAdapter = adapter;
    }

    /**
    /**
     * Adds a menu item to confirm media selection during Action Mode. Only adds one if one is not
     * defined.
     *
     * @param menu
     * the menu to add a confirm option to
     */
    private void addSelectionConfirmationButtonMenuItem(Menu menu) {
        if (menu != null && menu.findItem(R.id.menu_media_selection_confirmed) == null) {
            menu.add(Menu.NONE, R.id.menu_media_selection_confirmed, Menu.FIRST, R.string.confirm)
                    .setIcon(R.drawable.action_mode_confirm_checkmark);
        }
    }

     * Helper method; notifies listener that media selection has started
     */
    private void notifyMediaSelectionStarted() {
        if (mListener != null) {
            mListener.onMediaSelectionStarted();
        }
    }

    /**
     * Helper method; notifies listener of media selection changes
     */
    private boolean notifyMediaSelected(int position, boolean selected) {
        MediaItem mediaItem = mAdapter.getItem(position);

        if (mediaItem != null) {
            MediaSource mediaSource = mAdapter.sourceAtPosition(position);

            if (mediaSource == null || !mediaSource.onMediaItemSelected(mediaItem, selected)) {
                if (mListener != null) {
                    mListener.onMediaSelected(mediaItem, selected);
                }

                mSelectedContent.add(mediaItem);

                return false;
            }
        }

        return true;
    }

    /**
     * Helper method; notifies listener of media selection confirmation
     */
    private void notifyMediaSelectionConfirmed() {
        if (mListener != null) {
            mListener.onMediaSelectionConfirmed(mSelectedContent);
        }
    }

    /**
     * Helper method; notifies listener of media selection cancellation
     */
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

        if (mAdapter == null) {
            mAdapter = new MediaSourceAdapter(activity, mMediaSources, imageCache, this);
        }

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
    mAdapter = new MediaSourceAdapter(activity, mMediaSources, imageCache, this);
}
