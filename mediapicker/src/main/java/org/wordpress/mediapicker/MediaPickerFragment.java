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
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.mediapicker.source.MediaSource;

import java.util.ArrayList;
import java.util.List;

/**
 * By default the {@link org.wordpress.mediapicker.MediaSourceAdapter} is shown within
 * a {@link android.widget.GridView}, but a subclass of {@link android.widget.AbsListView} may be provided
 * with id=media_adapter_view. A subclass of {@link android.widget.TextView} may also be provided
 * for the empty view; id=media_empty_view.
 *
 * MediaPickerFragment tracks a collection of MediaSources and listens for changes via the
 * {@link org.wordpress.mediapicker.source.MediaSource.OnMediaChange} interface. While media is loading
 * the adapter is hidden and a text view indicates the current action. If there is no media content
 * in the adapter after a load or if the load fails the text will be updated.
 *
 * If the host {@link android.app.Activity} implements {@link org.wordpress.mediapicker.MediaPickerFragment.OnMediaSelected}
 * it will be set as the listener. Otherwise you can set it explicitly with
 * {@link org.wordpress.mediapicker.MediaPickerFragment#setListener(org.wordpress.mediapicker.MediaPickerFragment.OnMediaSelected)}.
 *
 * Menu items may be provided for Action Mode and their selection will be alerted with onMenuItemSelected.
 * A selection confirmation button is automatically added and will call onMediaSelectionConfirmed when selected.
 */

public class MediaPickerFragment extends Fragment
        implements AdapterView.OnItemClickListener,
                   AbsListView.MultiChoiceModeListener,
                   MediaSource.OnMediaChange {
    private static final String KEY_SELECTED_CONTENT = "selected-content";
    private static final String KEY_MEDIA_SOURCES    = "media-sources";
    private static final String KEY_CUSTOM_VIEW      = "custom-view";
    private static final String KEY_ACTION_MODE_MENU = "action-mode-menu";

    private static final int DEFAULT_VIEW = R.layout.media_picker_fragment;

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

    private final ArrayList<MediaSource> mMediaSources;
    private final ArrayList<MediaItem>   mSelectedContent;

    private OnMediaSelected        mListener;
    private TextView               mEmptyView;
    private AbsListView            mAdapterView;
    private MediaSourceAdapter     mAdapter;
    private MenuItem               mGalleryMenuItem;
    private int                    mCustomView;
    private int                    mActionModeMenu;

    public MediaPickerFragment() {
        super();

        mCustomView = -1;
        mActionModeMenu = -1;
        mMediaSources = new ArrayList<>();
        mSelectedContent = new ArrayList<>();
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
                ArrayList<MediaItem> mediaItems = savedInstanceState.getParcelableArrayList(KEY_SELECTED_CONTENT);
                mSelectedContent.addAll(mediaItems);
            }

            if (savedInstanceState.containsKey(KEY_MEDIA_SOURCES)) {
                ArrayList<MediaSource> mediaSources =  savedInstanceState.getParcelableArrayList(KEY_MEDIA_SOURCES);
                mMediaSources.addAll(mediaSources);
            }

            if (savedInstanceState.containsKey(KEY_CUSTOM_VIEW)) {
                mCustomView = savedInstanceState.getInt(KEY_CUSTOM_VIEW);
            }

            if (savedInstanceState.containsKey(KEY_ACTION_MODE_MENU)) {
                mActionModeMenu = savedInstanceState.getInt(KEY_ACTION_MODE_MENU);
            }
        }

        int viewToInflate = mCustomView < 0 ? DEFAULT_VIEW : mCustomView;
        View mediaPickerView = inflater.inflate(viewToInflate, container, false);
        if (mediaPickerView != null) {
            mEmptyView = (TextView) mediaPickerView.findViewById(R.id.media_empty_view);
            if (mEmptyView != null) {
                mEmptyView.setText(getString(R.string.fetching_media));
            }

            mAdapterView = (AbsListView) mediaPickerView.findViewById(R.id.media_adapter_view);
            if (mAdapterView != null) {
                layoutAdapterView();
            }
        }

        return mediaPickerView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(KEY_SELECTED_CONTENT, mSelectedContent);
        outState.putParcelableArrayList(KEY_MEDIA_SOURCES, mMediaSources);
        outState.putInt(KEY_CUSTOM_VIEW, mCustomView);
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

    public void setMediaSources(ArrayList<MediaSource> mediaSources) {
        mMediaSources.clear();
        mMediaSources.addAll(mediaSources);
    }

    public void setCustomView(int customView) {
        mCustomView = customView;
    }

    public void setListener(OnMediaSelected listener) {
        mListener = listener;
    }

    public void setAdapter(MediaSourceAdapter adapter) {
        mAdapter = adapter;
    }

    /**
     * Helper method; creates the adapter and initializes the AdapterView to display it
     */
    private void layoutAdapterView() {
        Activity activity = getActivity();
        Resources resources = activity.getResources();
        int paddingLeft = Math.round(resources.getDimension(R.dimen.media_padding_left));
        int paddingTop = Math.round(resources.getDimension(R.dimen.media_padding_top));
        int paddingRight = Math.round(resources.getDimension(R.dimen.media_padding_right));
        int paddingBottom = Math.round(resources.getDimension(R.dimen.media_padding_bottom));
        Drawable background = resources.getDrawable(R.drawable.media_picker_background);
        ImageLoader.ImageCache imageCache = mListener != null ? mListener.getImageCache() : null;

        if (mAdapter == null) {
            mAdapter = new MediaSourceAdapter(activity, mMediaSources, imageCache, this);
        }

        // Use setBackground(Drawable) when API min is >= 16
        mAdapterView.setBackgroundDrawable(background);
        mAdapterView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        mAdapterView.setClipToPadding(false);
        mAdapterView.setMultiChoiceModeListener(this);
        mAdapterView.setOnItemClickListener(this);
        mAdapterView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mAdapterView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        mAdapterView.setAdapter(mAdapter);
    }

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
}
