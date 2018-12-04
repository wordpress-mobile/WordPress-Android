package org.wordpress.android.editor;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;
import org.wordpress.aztec.IHistoryListener;
import org.wordpress.aztec.source.SourceViewEditText;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnGetContentTimeout;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnMediaLibraryButtonListener;

public class GutenbergEditorFragment extends EditorFragmentAbstract implements
        View.OnTouchListener,
        EditorMediaUploadListener,
        IHistoryListener {
    private static final String GUTENBERG_BLOCK_START = "<!-- wp:";

    private static boolean mIsToolbarExpanded = false;

    private boolean mEditorWasPaused = false;
    private boolean mHideActionBarOnSoftKeyboardUp = false;

    private EditTextWithKeyBackListener mTitle;
    private SourceViewEditText mSource;

    private Handler mInvalidateOptionsHandler;
    private Runnable mInvalidateOptionsRunnable;

    private LiveTextWatcher mTextWatcher = new LiveTextWatcher();

    private WPAndroidGlueCode mWPAndroidGlueCode;

    public GutenbergEditorFragment() {
        mWPAndroidGlueCode = new WPAndroidGlueCode();
    }

    public static GutenbergEditorFragment newInstance(String title, String content, boolean isExpanded) {
        mIsToolbarExpanded = isExpanded;
        GutenbergEditorFragment fragment = new GutenbergEditorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM_TITLE, title);
        args.putString(ARG_PARAM_CONTENT, content);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWPAndroidGlueCode.onCreate(getContext());

        ProfilingUtils.start("Visual Editor Startup");
        ProfilingUtils.split("EditorFragment.onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gutenberg_editor, container, false);

        mTitle = view.findViewById(R.id.title);
        mWPAndroidGlueCode.onCreateView(
                view.findViewById(R.id.gutenberg),
                new OnMediaLibraryButtonListener() {
                    @Override public void onMediaLibraryButtonClick() {
                        onToolbarMediaButtonClicked();
                    }
                },
                getActivity().getApplication(),
                BuildConfig.DEBUG,
                BuildConfig.BUILD_GUTENBERG_FROM_SOURCE);
        mSource = view.findViewById(R.id.source);

        mTitle.addTextChangedListener(mTextWatcher);

        // request dependency injection. Do this after setting min/max dimensions
        if (getActivity() instanceof EditorFragmentActivity) {
            ((EditorFragmentActivity) getActivity()).initializeEditorFragment();
        }

        mTitle.setOnTouchListener(this);
        mSource.setOnTouchListener(this);

        mTitle.setOnImeBackListener(new OnImeBackListener() {
            public void onImeBack() {
                showActionBarIfNeeded();
            }
        });

        mTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                for (int i = s.length(); i > 0; i--) {
                    if (s.subSequence(i - 1, i).toString().equals("\n")) {
                        s.replace(i - 1, i, " ");
                    }
                }
            }
        });

        // We need to intercept the "Enter" key on the title field, and replace it with a space instead
        mSource.setHint("<p>" + getString(R.string.editor_content_hint) + "</p>");

        setHasOptionsMenu(true);

        mInvalidateOptionsHandler = new Handler();
        mInvalidateOptionsRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAdded()) {
                    getActivity().invalidateOptionsMenu();
                }
            }
        };

        mEditorFragmentListener.onEditorFragmentInitialized();

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        mEditorWasPaused = true;

        mWPAndroidGlueCode.onPause(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        // If the editor was previously paused and the current orientation is landscape,
        // hide the actionbar because the keyboard is going to appear (even if it was hidden
        // prior to being paused).
        if (mEditorWasPaused
                && (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                && !getResources().getBoolean(R.bool.is_large_tablet_landscape)) {
            mHideActionBarOnSoftKeyboardUp = true;
            hideActionBarIfNeeded();
        }

        mWPAndroidGlueCode.onResume(this, getActivity());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mEditorDragAndDropListener = (EditorDragAndDropListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement EditorDragAndDropListener");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mWPAndroidGlueCode.onDestroy(getActivity());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_gutenberg, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.debugmenu) {
            mWPAndroidGlueCode.showDevOptionsDialog();
            return true;
        }

        return false;
    }

    @Override
    public void onRedoEnabled() {
        if (!isAdded()) {
            return;
        }

        mInvalidateOptionsHandler.removeCallbacks(mInvalidateOptionsRunnable);
        mInvalidateOptionsHandler.postDelayed(mInvalidateOptionsRunnable,
                                              getResources().getInteger(android.R.integer.config_mediumAnimTime));
    }

    @Override
    public void onUndoEnabled() {
        if (!isAdded()) {
            return;
        }

        mInvalidateOptionsHandler.removeCallbacks(mInvalidateOptionsRunnable);
        mInvalidateOptionsHandler.postDelayed(mInvalidateOptionsRunnable,
                                              getResources().getInteger(android.R.integer.config_mediumAnimTime));
    }

    private ActionBar getActionBar() {
        if (!isAdded()) {
            return null;
        }

        if (getActivity() instanceof AppCompatActivity) {
            return ((AppCompatActivity) getActivity()).getSupportActionBar();
        } else {
            return null;
        }
    }

    @Override
    public void setTitle(CharSequence text) {
        if (text == null) {
            text = "";
        }

        if (mTitle == null) {
            return;
        }
        mTitle.setText(text);
    }

    @Override
    public void setContent(CharSequence text) {
        if (text == null) {
            text = "";
        }

        if (!mWPAndroidGlueCode.hasReactRootView() || mSource == null) {
            return;
        }

        String postContent = removeVisualEditorProgressTag(text.toString());
        if (contentContainsGutenbergBlocks(postContent)) {
            mSource.setCalypsoMode(false);
        } else {
            mSource.setCalypsoMode(true);
        }

        // Initialize both editors (visual, source) with the same content. Need to do that so the starting point used in
        //  their content diffing algorithm is the same. That's assumed by the Toolbar's mode-switching logic too.
        mSource.displayStyledAndFormattedHtml(postContent);

        mWPAndroidGlueCode.setContent(postContent);
    }


    /*
        Note the way we detect we're in presence of Gutenberg blocks logic is taken from
        https://github.com/WordPress/gutenberg/blob/5a6693589285363341bebad15bd56d9371cf8ecc/lib/register.php#L331-L345

        * Determine whether a content string contains blocks. This test optimizes for
        * performance rather than strict accuracy, detecting the pattern of a block
        * but not validating its structure. For strict accuracy, you should use the
        * block parser on post content.
        *
        * @since 1.6.0
        * @see gutenberg_parse_blocks()
        *
        * @param string $content Content to test.
        * @return bool Whether the content contains blocks.

        function gutenberg_content_has_blocks( $content ) {
            return false !== strpos( $content, '<!-- wp:' );
        }
     */
    public static boolean contentContainsGutenbergBlocks(String postContent) {
        return (postContent != null && postContent.contains(GUTENBERG_BLOCK_START));
    }

    /*
    * TODO: REMOVE THIS ONCE AZTEC COMPLETELY REPLACES THE VISUAL EDITOR IN WPANDROID APP
     */
    private String removeVisualEditorProgressTag(String originalText) {
        // this regex picks any <progress> tags and any opening <span> tags for image containers
        // as produced by the Visual Editor. Note that we don't care about closing </span> tags
        // as the AztecParser takes care of that, and it would be very difficult to accomplish with a
        // regex (and using a proper XML crawler would be particularly overkill)
        if (originalText != null && originalText.contains("<progress")) {
            String regex = "<progress.*?><\\/progress>|<span id=\"img_container.*?"
                           + " class=\"img_container\" contenteditable=\"false\">";
            return originalText.replaceAll(regex, "");
        } else {
            return originalText;
        }
    }

    /**
     * Returns the contents of the title field from the JavaScript editor. Should be called from a background thread
     * where possible.
     */
    @Override
    public CharSequence getTitle() {
        if (!isAdded()) {
            return "";
        }

        // TODO: Aztec returns a ZeroWidthJoiner when empty so, strip it. Aztec needs fixing to return empty string.
        return StringUtils.notNullStr(mTitle.getText().toString().replaceAll("&nbsp;$", "").replaceAll("\u200B", ""));
    }

    @Override
    public boolean isActionInProgress() {
        return false;
    }

    /**
     * Returns the contents of the content field from the JavaScript editor. Should be called from a background thread
     * where possible.
     */
    @Override
    public CharSequence getContent(CharSequence originalContent) {
        return mWPAndroidGlueCode.getContent(originalContent, new OnGetContentTimeout() {
            @Override public void onGetContentTimeout(InterruptedException ie) {
                AppLog.e(T.EDITOR, ie);
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    public LiveData<Editable> getTitleOrContentChanged() {
        return mTextWatcher.getAfterTextChanged();
    }

    @Override
    public void appendMediaFile(final MediaFile mediaFile, final String mediaUrl, ImageLoader imageLoader) {
        if (getActivity() == null) {
            // appendMediaFile may be called from a background thread (example: EditPostActivity.java#L2165) and
            // Activity may have already be gone.
            // Ticket: https://github.com/wordpress-mobile/WordPress-Android/issues/7386
            AppLog.d(T.MEDIA, "appendMediaFile() called but Activity is null! mediaUrl: " + mediaUrl);
            return;
        }

        mWPAndroidGlueCode.appendMediaFile(mediaUrl);
    }

    @Override
    public void appendGallery(MediaGallery mediaGallery) {
    }

    @Override
    public void setUrlForVideoPressId(final String videoId, final String videoUrl, final String posterUrl) {
    }

    @Override
    public boolean isUploadingMedia() {
        return false;
    }

    @Override
    public boolean hasFailedMediaUploads() {
        return false;
    }

    @Override
    public void removeAllFailedMediaUploads() {
    }

    @Override
    public void removeMedia(String mediaId) {
    }

    @Override
    public Spanned getSpannedContent() {
        return null;
    }

    @Override
    public void setTitlePlaceholder(CharSequence placeholderText) {
    }

    @Override
    public void setContentPlaceholder(CharSequence placeholderText) {
    }

    @Override
    public void onMediaUploadReattached(String localId, float currentProgress) {
    }

    @Override
    public void onMediaUploadRetry(String localId, MediaType mediaType) {
    }

    @Override
    public void onMediaUploadSucceeded(final String localMediaId, final MediaFile mediaFile) {
    }

    @Override
    public void onMediaUploadProgress(final String localMediaId, final float progress) {
    }

    @Override
    public void onMediaUploadFailed(final String localMediaId, final MediaType
            mediaType, final String errorMessage) {
    }

    @Override
    public void onGalleryMediaUploadSucceeded(final long galleryId, long remoteMediaId, int remaining) {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Toggle action bar auto-hiding for the new orientation
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
                && !getResources().getBoolean(R.bool.is_large_tablet_landscape)) {
            mHideActionBarOnSoftKeyboardUp = true;
            hideActionBarIfNeeded();
        } else {
            mHideActionBarOnSoftKeyboardUp = false;
            showActionBarIfNeeded();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        // In landscape mode, if the title or content view has received a touch event, the keyboard will be
        // displayed and the action bar should hide
        if (event.getAction() == MotionEvent.ACTION_UP
                && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mHideActionBarOnSoftKeyboardUp = true;
            hideActionBarIfNeeded();
        }
        return false;
    }

    /**
     * Hide the action bar if needed. Don't hide it if
     * - a hardware keyboard is connected.
     * - the soft keyboard is not visible.
     * - it's not visible.
     */
    private void hideActionBarIfNeeded() {
        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            return;
        }
        if (!isHardwareKeyboardPresent()
                && mHideActionBarOnSoftKeyboardUp
                && actionBar.isShowing()) {
            getActionBar().hide();
        }
    }

    /**
     * Show the action bar if needed.
     */
    private void showActionBarIfNeeded() {
        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            return;
        }
        if (!actionBar.isShowing()) {
            actionBar.show();
        }
    }

    /**
     * Returns true if a hardware keyboard is detected, otherwise false.
     */
    private boolean isHardwareKeyboardPresent() {
        Configuration config = getResources().getConfiguration();
        boolean returnValue = false;
        if (config.keyboard != Configuration.KEYBOARD_NOKEYS) {
            returnValue = true;
        }
        return returnValue;
    }

    public boolean onToolbarMediaButtonClicked() {
        mEditorFragmentListener.onTrackableEvent(TrackableEvent.MEDIA_BUTTON_TAPPED);

        if (isActionInProgress()) {
            ToastUtils.showToast(getActivity(), R.string.alert_action_while_uploading, ToastUtils.Duration.LONG);
        }

        if (mSource.isFocused()) {
            ToastUtils.showToast(getActivity(), R.string.alert_insert_image_html_mode, ToastUtils.Duration.LONG);
        } else {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mEditorFragmentListener.onAddMediaClicked();
                }
            });
        }

        return true;
    }
}
