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
 * MediaPickerFragment tracks a collection of MediaSources and responds to changes via the
 * {@link org.wordpress.mediapicker.source.MediaSource.OnMediaChange} interface.
 *
 * If the host Activity implements {@link org.wordpress.mediapicker.MediaPickerFragment.OnMediaSelected}
 * it will automatically be set as the listener, otherwise a setter is provided. This interface is
 * how user intent is delivered, while not having a listener won't break anything it's not very useful.
 *
 * By default the {@link org.wordpress.mediapicker.MediaSourceAdapter} is shown within
 * a {@link android.widget.GridView}, but a subclass of {@link android.widget.AbsListView} may be provided
 * with id=media_adapter_view. A subclass of {@link android.widget.TextView} may also be provided
 * for the empty view; id=media_empty_view. You can even provide a subclass of
 * {@link org.wordpress.mediapicker.MediaSourceAdapter}.
 *
 * Menu items may be provided for Action Mode and their selection will be alerted with onMenuItemSelected.
 * A selection confirmation button is automatically added and will call onMediaSelectionConfirmed when selected.
 */

public class MediaPickerFragment extends Fragment
                              implements AdapterView.OnItemClickListener,
                                         AbsListView.MultiChoiceModeListener,
                                         MediaSource.OnMediaChange {
    public static final String KEY_SELECTED_CONTENT = "key-selected-content";
    public static final String KEY_MEDIA_SOURCES    = "key-media-sources";
    public static final String KEY_CUSTOM_LAYOUT    = "key-custom-view";
    public static final String KEY_ACTION_MODE_MENU = "key-action-mode-menu";
    public static final String KEY_LOADING_TEXT     = "key-loading-text";
    public static final String KEY_EMPTY_TEXT       = "key-empty-text";
    public static final String KEY_ERROR_TEXT       = "key-error-text";

    // Default layout to be used if a custom layout is not provided
    private static final int DEFAULT_VIEW = R.layout.media_picker_fragment;

    /**
     * Interface to respond to user intent and provide a caching mechanism for the fragment.
     */
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
        public boolean onMenuItemSelected(MenuItem menuItem, ArrayList<MediaItem> selectedContent);
        // Should handle null image cache
        public ImageLoader.ImageCache getImageCache();
    }

    // Current media sources and selected content from the sources
    private final ArrayList<MediaSource> mMediaSources;
    private final ArrayList<MediaItem>   mSelectedContent;

    // Callbacks for media selection events, use to track user intent
    private OnMediaSelected    mListener;

    // Required state tracking to prevent OnMediaSelectionCancelled from being called erroneously
    private boolean            mConfirmed;

    // Views utilized by this fragment
    private TextView           mEmptyView;
    private AbsListView        mAdapterView;

    // Adapter for showing MediaSource content in the AdapterView
    private MediaSourceAdapter mAdapter;

    // Customizable view resources, some default behavior is defined as described in the docs
    private int                mCustomLayout;
    private int                mActionModeMenu;

    // Customizable status text messages, default values are provided
    private String             mLoadingText;
    private String             mEmptyText;
    private String             mErrorText;

    public MediaPickerFragment() {
        super();

        mCustomLayout = -1;
        mActionModeMenu = -1;
        mMediaSources = new ArrayList<>();
        mSelectedContent = new ArrayList<>();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Per the documentation, the host Activity is the default listener
        if (activity instanceof OnMediaSelected) {
            mListener = (OnMediaSelected) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If this is not being restored from a previous state use arguments to set state
        if (savedInstanceState == null) {
            restoreFromBundle(getArguments());
        } else {
            restoreFromBundle(savedInstanceState);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        restoreFromBundle(savedInstanceState);

        int viewToInflate = mCustomLayout < 0 ? DEFAULT_VIEW : mCustomLayout;
        View mediaPickerView = inflater.inflate(viewToInflate, container, false);
        if (mediaPickerView != null) {
            mEmptyView = (TextView) mediaPickerView.findViewById(R.id.media_empty_view);
            if (mEmptyView != null) {
                if (mMediaSources.size() == 0) {
                    mEmptyView.setText(getString(R.string.no_media_sources));
                } else {
                    mEmptyView.setText(mLoadingText);
                }
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
        outState.putInt(KEY_CUSTOM_LAYOUT, mCustomLayout);
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

        mConfirmed = false;

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
            return mListener.onMenuItemSelected(menuItem, mSelectedContent);
        }

        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        if (!mConfirmed) {
            notifyMediaSelectionCancelled();
        }

        mSelectedContent.clear();

        getActivity().onActionModeFinished(mode);
    }

    @Override
    public void onMediaLoaded(boolean success) {
        if (success) {
            if (mAdapter.getCount() > 0) {
                toggleEmptyVisibility();
                mAdapter.notifyDataSetChanged();
            } else if (mEmptyView != null) {
                mEmptyView.setText(mEmptyText);
            }
        } else {
            if (mEmptyView != null) {
                mEmptyView.setText(mErrorText);
            }
        }
    }

    @Override
    public void onMediaAdded(MediaSource source, List<MediaItem> addedItems) {
        toggleEmptyVisibility();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onMediaRemoved(MediaSource source, List<MediaItem> removedItems) {
        toggleEmptyVisibility();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onMediaChanged(MediaSource source, List<MediaItem> changedItems) {
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Restores state from a given {@link android.os.Bundle}. Checks for media sources, selected
     * content, custom view, custom action mode menu, and custom empty text.
     *
     * @param bundle
     * Bundle containing all the data, can be null
     */
    private void restoreFromBundle(Bundle bundle) {
        if (bundle != null) {
            if (bundle.containsKey(KEY_MEDIA_SOURCES)) {
                ArrayList<MediaSource> mediaSources = bundle.getParcelableArrayList(KEY_MEDIA_SOURCES);
                setMediaSources(mediaSources);

                if (bundle.containsKey(KEY_SELECTED_CONTENT)) {
                    ArrayList<MediaItem> mediaItems = bundle.getParcelableArrayList(KEY_SELECTED_CONTENT);

                    if (mediaItems != null) {
                        mSelectedContent.addAll(mediaItems);
                    }
                }
            }

            if (bundle.containsKey(KEY_CUSTOM_LAYOUT)) {
                setCustomLayout(bundle.getInt(KEY_CUSTOM_LAYOUT, -1));
            }

            if (bundle.containsKey(KEY_ACTION_MODE_MENU)) {
                setActionModeMenu(bundle.getInt(KEY_ACTION_MODE_MENU, -1));
            }

            if (bundle.containsKey(KEY_LOADING_TEXT)) {
                if ((mLoadingText = bundle.getString(KEY_LOADING_TEXT)) == null) {
                    mLoadingText = getString(R.string.fetching_media);
                }
            }

            if (bundle.containsKey(KEY_EMPTY_TEXT)) {
                if ((mEmptyText = bundle.getString(KEY_EMPTY_TEXT)) == null) {
                    mEmptyText = getString(R.string.no_media);
                }
            }

            if (bundle.containsKey(KEY_ERROR_TEXT)) {
                if ((mErrorText = bundle.getString(KEY_ERROR_TEXT)) == null) {
                    mErrorText = getString(R.string.error_fetching_media);
                }
            }
        } else {
            setDefaultTextValues();
        }
    }

    /**
     * Sets the default empty text strings.
     */
    private void setDefaultTextValues() {
        setLoadingText(R.string.fetching_media);
        setEmptyText(R.string.no_media);
        setErrorText(R.string.error_fetching_media);
    }

    /**
     * Sets the listener. Calling this method will overwrite the current listener.
     *
     * @param listener
     * the new listener, can be null
     */
    public void setListener(OnMediaSelected listener) {
        mListener = listener;
    }

    /**
     * Sets the {@link org.wordpress.mediapicker.source.MediaSource}'s to be presented. The current
     * sources and selected content are cleaned up and cleared before the new list added.
     *
     * @param mediaSources
     * the new sources
     */
    public void setMediaSources(ArrayList<MediaSource> mediaSources) {
        mSelectedContent.clear();

        cleanupMediaSources();
        mMediaSources.clear();

        // a null parameter results in a NullPointerException
        if (mediaSources != null) {
            mMediaSources.addAll(mediaSources);
            generateAdapter();
        }
    }

    /**
     * Sets the menu resource to be inflated when in Action Mode.
     *
     * @param id
     * the ID of the menu resource, any value < 0 will use the default menu
     */
    public void setActionModeMenu(int id) {
        mActionModeMenu = id;
    }

    /**
     * Sets the layout resource to be used to display media content.
     *
     * @param customLayout
     * the ID of the layout resource, any value < 0 will use the default layout
     */
    public void setCustomLayout(int customLayout) {
        mCustomLayout = customLayout;
    }

    /**
     * Sets the text to be displayed while media content is loading.
     *
     * @param loadingText
     * the text to display while media is loading, can be null
     */
    public void setLoadingText(String loadingText) {
        mLoadingText = loadingText;
    }

    /**
     * Same as calling {@link #setLoadingText(String)} with {@link #getString(int)} for resId.
     * Passing resId < 0 will set the text to null.
     *
     * @param resId
     * resource ID of the string to display while media is loading
     */
    public void setLoadingText(int resId) {
        if (resId < 0) {
            setLoadingText(null);
        } else {
            setLoadingText(getString(resId));
        }
    }

    /**
     * Sets the text to be displayed if there is no media content to show.
     *
     * @param emptyText
     * the text to display when there is no media, can be null
     */
    public void setEmptyText(String emptyText) {
        mEmptyText = emptyText;
    }

    /**
     * Same as calling {@link #setEmptyText(String)} with {@link #getString(int)} for resId.
     * Passing resId < 0 will set the text to null.
     *
     * @param resId
     * resource ID of the string to display when there is no media
     */
    public void setEmptyText(int resId) {
        if (resId < 0) {
            setEmptyText(null);
        } else {
            setEmptyText(getString(resId));
        }
    }

    /**
     * Sets the text to be displayed if an error occurs while loading media.
     *
     * @param errorText
     * the text to display when an error occurs while loading, can be null
     */
    public void setErrorText(String errorText) {
        mErrorText = errorText;
    }

    /**
     * Same as calling {@link #setErrorText(String)} with {@link #getString(int)} for resId.
     * Passing resId < 0 will set the text to null.
     *
     * @param resId
     * resource ID of the string to display when there is an error loading media
     */
    public void setErrorText(int resId) {
        if (resId < 0) {
            setErrorText(null);
        } else {
            setErrorText(getString(resId));
        }
    }

    /**
     * Sets the adapter.
     *
     * @param adapter
     * the new adapter
     */
    public void setAdapter(MediaSourceAdapter adapter) {
        mAdapter = adapter;
    }

    /**
     * Calls {@link org.wordpress.mediapicker.source.MediaSource.OnMediaChange#cleanup()} on all
     * non-null sources.
     */
    private void cleanupMediaSources() {
        for (MediaSource source : mMediaSources) {
            if (source != null) {
                source.cleanup();
            }
        }
    }

    /**
     * Constructs the {@link org.wordpress.mediapicker.MediaSourceAdapter} and attaches it to the
     * adapter view if possible.
     */
    private void generateAdapter() {
        Activity activity = getActivity();

        if (activity != null) {
            ImageLoader.ImageCache imageCache = mListener != null ? mListener.getImageCache() : null;
            mAdapter = new MediaSourceAdapter(activity, mMediaSources, imageCache);
            mAdapter.gatherFromSources(this);

            if (mAdapterView != null) {
                mAdapterView.setAdapter(mAdapter);
            }
        }
    }

    /**
     * Creates the {@link org.wordpress.mediapicker.MediaSourceAdapter} and initializes the adapter
     * view to display it.
     */
    private void layoutAdapterView() {
        Activity activity = getActivity();
        Resources resources = activity.getResources();
        int paddingLeft = Math.round(resources.getDimension(R.dimen.media_padding_left));
        int paddingTop = Math.round(resources.getDimension(R.dimen.media_padding_top));
        int paddingRight = Math.round(resources.getDimension(R.dimen.media_padding_right));
        int paddingBottom = Math.round(resources.getDimension(R.dimen.media_padding_bottom));
        Drawable background = resources.getDrawable(R.drawable.media_picker_background);

        // Use setBackground(Drawable) when API min is >= 16
        mAdapterView.setBackgroundDrawable(background);
        mAdapterView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        mAdapterView.setClipToPadding(false);
        mAdapterView.setMultiChoiceModeListener(this);
        mAdapterView.setOnItemClickListener(this);
        mAdapterView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mAdapterView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);

        generateAdapter();

        toggleEmptyVisibility();
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

    /**
     * If the current adapter does not have any items the empty view will be shown and the adapter
     * view will be hidden. Otherwise the empty view will be hidden and the adapter view presented.
     */
    private void toggleEmptyVisibility() {
        if (mAdapter.getCount() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
            mAdapterView.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mAdapterView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Notifies non-null listener that media selection has started.
     */
    private void notifyMediaSelectionStarted() {
        if (mListener != null) {
            mListener.onMediaSelectionStarted();
        }
    }

    /**
     * Notifies non-null listener when selection state changes on a media item.
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
     * Notifies non-null listener that media selection has been confirmed.
     */
    private void notifyMediaSelectionConfirmed() {
        if (mListener != null) {
            mListener.onMediaSelectionConfirmed(mSelectedContent);
        }

        mConfirmed = true;
    }

    /**
     * Notifies non-null listener that media selection has been cancelled.
     */
    private void notifyMediaSelectionCancelled() {
        if (mListener != null) {
            mListener.onMediaSelectionCancelled();
        }
    }
}
