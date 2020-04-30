package org.wordpress.android.editor;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.SuggestionSpan;
import android.util.DisplayMetrics;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.BaseInputConnection;
import android.webkit.URLUtil;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;

import com.android.volley.toolbox.ImageLoader;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.editor.MetadataUtils.AttributesWithClass;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.ShortcodeUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;
import org.wordpress.aztec.AlignmentRendering;
import org.wordpress.aztec.Aztec;
import org.wordpress.aztec.AztecAttributes;
import org.wordpress.aztec.AztecContentChangeWatcher.AztecTextChangeObserver;
import org.wordpress.aztec.AztecExceptionHandler;
import org.wordpress.aztec.AztecParser;
import org.wordpress.aztec.AztecText;
import org.wordpress.aztec.AztecText.EditorHasChanges;
import org.wordpress.aztec.AztecTextFormat;
import org.wordpress.aztec.Html;
import org.wordpress.aztec.Html.ImageGetter.Callbacks;
import org.wordpress.aztec.IHistoryListener;
import org.wordpress.aztec.ITextFormat;
import org.wordpress.aztec.extensions.MediaLinkExtensionsKt;
import org.wordpress.aztec.plugins.IAztecPlugin;
import org.wordpress.aztec.plugins.IMediaToolbarButton;
import org.wordpress.aztec.plugins.shortcodes.AudioShortcodePlugin;
import org.wordpress.aztec.plugins.shortcodes.CaptionShortcodePlugin;
import org.wordpress.aztec.plugins.shortcodes.VideoShortcodePlugin;
import org.wordpress.aztec.plugins.shortcodes.extensions.CaptionExtensionsKt;
import org.wordpress.aztec.plugins.shortcodes.extensions.VideoPressExtensionsKt;
import org.wordpress.aztec.plugins.wpcomments.CommentsTextFormat;
import org.wordpress.aztec.plugins.wpcomments.HiddenGutenbergPlugin;
import org.wordpress.aztec.plugins.wpcomments.WordPressCommentsPlugin;
import org.wordpress.aztec.plugins.wpcomments.toolbar.MoreToolbarButton;
import org.wordpress.aztec.source.Format;
import org.wordpress.aztec.source.SourceViewEditText;
import org.wordpress.aztec.spans.AztecMediaSpan;
import org.wordpress.aztec.spans.IAztecAttributedSpan;
import org.wordpress.aztec.toolbar.AztecToolbar;
import org.wordpress.aztec.toolbar.IAztecToolbarClickListener;
import org.wordpress.aztec.util.AztecLog;
import org.wordpress.aztec.watchers.EndOfBufferMarkerAdder;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.wordpress.android.editor.EditorImageMetaData.ARG_EDITOR_IMAGE_METADATA;

public class AztecEditorFragment extends EditorFragmentAbstract implements
        AztecText.OnImeBackListener,
        AztecText.OnImageTappedListener,
        AztecText.OnVideoTappedListener,
        AztecText.OnMediaDeletedListener,
        AztecText.OnVideoInfoRequestedListener,
        View.OnTouchListener,
        EditorMediaUploadListener,
        IAztecToolbarClickListener,
        AztecTextChangeObserver,
        IHistoryListener {
    public static class AztecLoggingException extends Exception {
        public AztecLoggingException(String message) {
            super(message);
        }

        public AztecLoggingException(Throwable originalException) {
            super(originalException);
        }
    }

    private static final String AZTEC_EDITOR_NAME = "aztec";
    private static final String ATTR_TAPPED_MEDIA_PREDICATE = "tapped_media_predicate";

    private static final String ATTR_ALIGN = "align";
    private static final String ATTR_TARGET = "target";
    private static final String ATTR_CLASS = "class";
    private static final String ATTR_ID_WP = "data-wpid";
    private static final String ATTR_IMAGE_WP_DASH = "wp-image-";
    private static final String ATTR_SIZE_DASH = "size-";
    private static final String TEMP_IMAGE_ID = "data-temp-aztec-id";
    private static final String ANIMATED_MEDIA = "animated-media";
    private static final String TEMP_VIDEO_UPLOADING_CLASS = "data-temp-aztec-video";
    private static final String GUTENBERG_BLOCK_START = "<!-- wp:";

    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";

    private static final int MIN_BITMAP_DIMENSION_DP = 48;
    public static final int DEFAULT_MEDIA_PLACEHOLDER_DIMENSION_DP = 196;

    public static final int EDITOR_MEDIA_SETTINGS = 55;

    private static final int MAX_ACTION_TIME_MS = 2000;

    private static final List<String> DRAGNDROP_SUPPORTED_MIMETYPES_TEXT = Arrays.asList(ClipDescription
            .MIMETYPE_TEXT_PLAIN, ClipDescription.MIMETYPE_TEXT_HTML);
    private static final List<String> DRAGNDROP_SUPPORTED_MIMETYPES_IMAGE = Arrays.asList("image/jpeg", "image/png");

    private static boolean mIsToolbarExpanded = false;

    // 1-deep cache of html parsed to spans, as a speed optimization instead of parsing the same content again and again
    private static int sPostContentHash;
    private static Spanned sParsedContentCached;

    private boolean mEditorWasPaused = false;
    private boolean mHideActionBarOnSoftKeyboardUp = false;

    private EditTextWithKeyBackListener mTitle;
    private AztecText mContent;
    private boolean mAztecReady;
    private SourceViewEditText mSource;
    private AztecToolbar mFormattingToolbar;
    private View mContentAndSourceContainer;
    private Html.ImageGetter mAztecImageLoader;
    private Html.VideoThumbnailGetter mAztecVideoLoader;

    private Handler mInvalidateOptionsHandler;
    private Runnable mInvalidateOptionsRunnable;

    private HashMap<String, Float> mUploadingMediaProgressMax = new HashMap<>();
    private Set<String> mFailedMediaIds = new HashSet<>();

    private long mActionStartedAt = -1;

    private MediaPredicate mTappedMediaPredicate;

    private EditorImageSettingsListener mEditorImageSettingsListener;
    private MediaToolbarAction.MediaToolbarButtonClickListener mMediaToolbarButtonClickListener;

    private Drawable mLoadingImagePlaceholder;
    private Drawable mLoadingVideoPlaceholder;

    private int maxMediaSize;
    private int minMediaSize;

    private LiveTextWatcher mTextWatcher = new LiveTextWatcher();

    private View mFragmentView;

    public static AztecEditorFragment newInstance(String title, String content, boolean isExpanded) {
        mIsToolbarExpanded = isExpanded;
        AztecEditorFragment fragment = new AztecEditorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM_TITLE, title);
        args.putString(ARG_PARAM_CONTENT, content);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ProfilingUtils.start("Visual Editor Startup");
        ProfilingUtils.split("EditorFragment.onCreate");

        if (savedInstanceState != null) {
            mTappedMediaPredicate = savedInstanceState.getParcelable(ATTR_TAPPED_MEDIA_PREDICATE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_aztec_editor, container, false);
        mFragmentView = view;

        mTitle = view.findViewById(R.id.title);
        mContent = view.findViewById(R.id.aztec);
        mSource = view.findViewById(R.id.source);
        mContentAndSourceContainer = view.findViewById(R.id.aztec_content_and_source_container);

        mTitle.addTextChangedListener(mTextWatcher);
        mContent.getContentChangeWatcher().registerObserver(this);
        mSource.addTextChangedListener(mTextWatcher);

        // Set the default value for max and min picture sizes.
        maxMediaSize = EditorMediaUtils.getMaximumThumbnailSizeForEditor(getActivity());
        minMediaSize = DisplayUtils.dpToPx(getActivity(), MIN_BITMAP_DIMENSION_DP);
        mContent.setMinImagesWidth(minMediaSize);
        mContent.setMaxImagesWidth(maxMediaSize);

        // request dependency injection. Do this after setting min/max dimensions
        if (getActivity() instanceof EditorFragmentActivity) {
            ((EditorFragmentActivity) getActivity()).initializeEditorFragment();
        }

        mTitle.setOnTouchListener(this);
        mContent.setOnTouchListener(this);
        mSource.setOnTouchListener(this);

        mTitle.setOnImeBackListener(new org.wordpress.android.editor.OnImeBackListener() {
            public void onImeBack() {
                showActionBarIfNeeded();
            }
        });
        mContent.setOnImeBackListener(this);
        mSource.setOnImeBackListener(this);

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

        mFormattingToolbar = (AztecToolbar) view.findViewById(R.id.formatting_toolbar);
        mFormattingToolbar.setExpanded(mIsToolbarExpanded);

        View mediaCollapseButton = mFormattingToolbar.findViewById(R.id.format_bar_button_media_collapsed);
        View mediaExpandButton = mFormattingToolbar.findViewById(R.id.format_bar_button_media_expanded);

        mediaCollapseButton.setBackgroundTintList(ContextCompat
                .getColorStateList(mediaExpandButton.getContext(), R.color.media_button_background_tint_selector));
        mediaExpandButton.setBackgroundTintList(ContextCompat
                .getColorStateList(mediaExpandButton.getContext(), R.color.media_button_background_tint_selector));

        mTitle.setOnFocusChangeListener(
                new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean hasFocus) {
                        mFormattingToolbar.enableFormatButtons(!hasFocus);
                    }
                }
                                       );

        mContent.setOnDragListener(mOnDragListener);
        mSource.setOnDragListener(mOnDragListener);

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

        mContent.refreshText();

        mAztecReady = true;

        MediaToolbarGalleryButton mediaToolbarGalleryButton = new MediaToolbarGalleryButton(mFormattingToolbar);
        mediaToolbarGalleryButton.setMediaToolbarButtonClickListener(
                new IMediaToolbarButton.IMediaToolbarClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mMediaToolbarButtonClickListener != null) {
                            mMediaToolbarButtonClickListener.onMediaToolbarButtonClicked(MediaToolbarAction.GALLERY);
                        }
                    }
                });

        MediaToolbarCameraButton mediaToolbarCameraButton = new MediaToolbarCameraButton(mFormattingToolbar);
        mediaToolbarCameraButton.setMediaToolbarButtonClickListener(
                new IMediaToolbarButton.IMediaToolbarClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mMediaToolbarButtonClickListener != null) {
                            mMediaToolbarButtonClickListener.onMediaToolbarButtonClicked(MediaToolbarAction.CAMERA);
                        }
                    }
                });

        MediaToolbarLibraryButton mediaToolbarLibraryButton = new MediaToolbarLibraryButton(mFormattingToolbar);
        mediaToolbarLibraryButton.setMediaToolbarButtonClickListener(
                new IMediaToolbarButton.IMediaToolbarClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mMediaToolbarButtonClickListener != null) {
                            mMediaToolbarButtonClickListener.onMediaToolbarButtonClicked(MediaToolbarAction.LIBRARY);
                        }
                    }
                });

        Aztec.Factory.with(mContent, mSource, mFormattingToolbar, this)
                     .setImageGetter(mAztecImageLoader)
                     .setVideoThumbnailGetter(mAztecVideoLoader)
                     .setOnImeBackListener(this)
                     .setHistoryListener(this)
                     .setOnImageTappedListener(this)
                     .setOnVideoTappedListener(this)
                     .setOnMediaDeletedListener(this)
                     .setOnVideoInfoRequestedListener(this)
                     .addPlugin(new WordPressCommentsPlugin(mContent))
                     .addPlugin(new MoreToolbarButton(mContent))
                     .addPlugin(new CaptionShortcodePlugin(mContent))
                     .addPlugin(new VideoShortcodePlugin())
                     .addPlugin(new AudioShortcodePlugin())
                     .addPlugin(new HiddenGutenbergPlugin(mContent))
                     .addPlugin(mediaToolbarGalleryButton)
                     .addPlugin(mediaToolbarCameraButton)
                     .addPlugin(mediaToolbarLibraryButton);

        mEditorFragmentListener.onEditorFragmentInitialized();

        return view;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        mContent.getContentChangeWatcher().unregisterObserver(this);
    }

    public void setEditorImageSettingsListener(EditorImageSettingsListener listener) {
        mEditorImageSettingsListener = listener;
    }

    public void setMediaToolbarButtonClickListener(MediaToolbarAction.MediaToolbarButtonClickListener listener) {
        mMediaToolbarButtonClickListener = listener;
    }

    public void setAztecImageLoader(Html.ImageGetter imageLoader) {
        this.mAztecImageLoader = imageLoader;
    }

    public void setAztecVideoLoader(Html.VideoThumbnailGetter videoLoader) {
        this.mAztecVideoLoader = videoLoader;
    }

    @Override
    public void onPause() {
        super.onPause();
        mEditorWasPaused = true;
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

        addOverlayToGifs();
        updateFailedAndUploadingMedia();
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
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(ATTR_TAPPED_MEDIA_PREDICATE, mTappedMediaPredicate);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_aztec, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // TODO: disable undo/redo in media mode
        boolean canRedo = mContent.history.redoValid();
        boolean canUndo = mContent.history.undoValid();

        if (menu != null) {
            MenuItem redoItem = menu.findItem(R.id.redo);
            if (redoItem != null) {
                setUndoRedoAppearance(redoItem, canRedo);
            }

            MenuItem undoItem = menu.findItem(R.id.undo);
            if (undoItem != null) {
                setUndoRedoAppearance(undoItem, canUndo);
            }
        }


        super.onPrepareOptionsMenu(menu);
    }

    private void setUndoRedoAppearance(MenuItem menuItem, boolean enabled) {
        menuItem.setEnabled(enabled);

        SpannableString s = new SpannableString(menuItem.getTitle());
        s.setSpan(new ForegroundColorSpan(enabled ? Color.BLACK : Color.GRAY), 0, s.length(), 0);
        menuItem.setTitle(s);
    }

    public boolean hasHistory() {
        return (mContent.history.getHistoryEnabled() && !mContent.history.getHistoryList().isEmpty());
    }

    public boolean canUndo() {
        return (hasHistory() && mContent.history.undoValid());
    }

    public boolean isHistoryEnabled() {
        return mContent.history.getHistoryEnabled();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.undo) {
            if (mContent.getVisibility() == View.VISIBLE) {
                mContent.undo();
                mEditorFragmentListener.onUndoMediaCheck(mContent.toHtml(false));
            } else {
                mSource.undo();
            }
            return true;
        } else if (item.getItemId() == R.id.redo) {
            if (mContent.getVisibility() == View.VISIBLE) {
                mContent.redo();
            } else {
                mSource.redo();
            }
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

    @Override
    public void onUndo() {
        if (!isAdded()) {
            return;
        }
        mEditorFragmentListener.onTrackableEvent(TrackableEvent.UNDO_TAPPED);
    }

    @Override
    public void onRedo() {
        if (!isAdded()) {
            return;
        }
        mEditorFragmentListener.onTrackableEvent(TrackableEvent.REDO_TAPPED);
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

    /**
     * Intercept back button press while soft keyboard is visible.
     */
    @Override
    public void onImeBack() {
        showActionBarIfNeeded();
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

    @NonNull
    @Override
    public String getEditorName() {
        return AZTEC_EDITOR_NAME;
    }

    @Override
    public void setContent(CharSequence text) {
        if (text == null) {
            text = "";
        }

        if (mContent == null || mSource == null) {
            return;
        }

        String postContent = removeVisualEditorProgressTag(text.toString());
        if (contentContainsGutenbergBlocks(postContent)) {
            mContent.setCalypsoMode(false);
            mSource.setCalypsoMode(false);
        } else {
            mContent.setCalypsoMode(true);
            mSource.setCalypsoMode(true);
        }

        // Initialize both editors (visual, source) with the same content. Need to do that so the starting point used in
        //  their content diffing algorithm is the same. That's assumed by the Toolbar's mode-switching logic too.
        mSource.displayStyledAndFormattedHtml(postContent);
        mContent.fromHtml(postContent, true);

        updateFailedAndUploadingMedia();

        mAztecReady = true;
    }


    private void addOverlayToGifs() {
        AztecMediaSpan[] imageOrVideoSpans =
                mContent.getText().getSpans(0, mContent.getText().length(), AztecMediaSpan.class);

        // scans through all the MediaSpans and adds ANIMATED_MEDIA attribute to GIF images
        for (AztecMediaSpan currentClass : imageOrVideoSpans) {
            AttributesWithClass classes = getAttributesWithClass(currentClass.getAttributes());
            AztecAttributes attributes = currentClass.getAttributes();

            if (attributes.hasAttribute(ATTR_SRC)
                && !attributes.hasAttribute(ANIMATED_MEDIA)
                && !classes.hasClass(ATTR_STATUS_FAILED)
                && !classes.hasClass(ATTR_STATUS_UPLOADING)
                && MediaUtils.isGif(attributes.getValue(ATTR_SRC))) {
                attributes.setValue(ANIMATED_MEDIA, ANIMATED_MEDIA);
            }
        }
        overlayGifImages();
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
    private boolean contentContainsGutenbergBlocks(String postContent) {
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
    public CharSequence getTitle() throws EditorFragmentNotAddedException {
        if (!isAdded()) {
            throw new EditorFragmentNotAddedException();
        }

        // TODO: Aztec returns a ZeroWidthJoiner when empty so, strip it. Aztec needs fixing to return empty string.
        return StringUtils.notNullStr(mTitle.getText().toString().replaceAll("&nbsp;$", "").replaceAll("\u200B", ""));
    }

    @Override
    public void onToolbarCollapseButtonClicked() {
        mEditorFragmentListener.onTrackableEvent(TrackableEvent.ELLIPSIS_COLLAPSE_BUTTON_TAPPED);
    }

    @Override
    public void onToolbarExpandButtonClicked() {
        mEditorFragmentListener.onTrackableEvent(TrackableEvent.ELLIPSIS_EXPAND_BUTTON_TAPPED);
    }

    @Override
    public void onToolbarFormatButtonClicked(ITextFormat format, boolean isKeyboardShortcut) {
        if (format.equals(AztecTextFormat.FORMAT_PARAGRAPH)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.PARAGRAPH_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_PREFORMAT)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.PREFORMAT_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_HEADING_1)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.HEADING_1_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_HEADING_2)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.HEADING_2_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_HEADING_3)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.HEADING_3_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_HEADING_4)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.HEADING_4_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_HEADING_5)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.HEADING_5_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_HEADING_6)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.HEADING_6_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_ORDERED_LIST)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.LIST_ORDERED_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_UNORDERED_LIST)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.LIST_UNORDERED_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_BOLD)
                   || format.equals(AztecTextFormat.FORMAT_STRONG)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.BOLD_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_ITALIC)
                   || format.equals(AztecTextFormat.FORMAT_EMPHASIS)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.ITALIC_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_STRIKETHROUGH)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.STRIKETHROUGH_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_UNDERLINE)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.UNDERLINE_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_QUOTE)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.BLOCKQUOTE_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_LINK)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.LINK_ADDED_BUTTON_TAPPED);
        } else if (format.equals(CommentsTextFormat.FORMAT_MORE)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.READ_MORE_BUTTON_TAPPED);
        } else if (format.equals(CommentsTextFormat.FORMAT_PAGE)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.NEXT_PAGE_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_HORIZONTAL_RULE)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.HORIZONTAL_RULE_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_ALIGN_LEFT)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.FORMAT_ALIGN_LEFT_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_ALIGN_CENTER)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.FORMAT_ALIGN_CENTER_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_ALIGN_RIGHT)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.FORMAT_ALIGN_RIGHT_BUTTON_TAPPED);
        } else {
            AppLog.w(T.EDITOR, "Aztec ToolbarFormatButtonClick not tracked in Analytics: " + format.toString());
        }
    }

    @Override
    public void onToolbarHeadingButtonClicked() {
        mEditorFragmentListener.onTrackableEvent(TrackableEvent.HEADING_BUTTON_TAPPED);
    }

    @Override
    public void onToolbarHtmlButtonClicked() {
        if (!isAdded()) {
            return;
        }

        checkForFailedUploadAndSwitchToHtmlMode();
    }

    @Override
    public void onToolbarListButtonClicked() {
        mEditorFragmentListener.onTrackableEvent(TrackableEvent.LIST_BUTTON_TAPPED);
    }

    private void checkForFailedUploadAndSwitchToHtmlMode() {
        // Show an Alert Dialog asking the user if he wants to remove all failed media before upload
        if (hasFailedMediaUploads()) {
            new MaterialAlertDialogBuilder(getActivity())
                    .setMessage(R.string.editor_failed_uploads_switch_html)
                    .setPositiveButton(R.string.editor_remove_failed_uploads, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Clear failed uploads and switch to HTML mode
                            removeAllFailedMediaUploads();
                            toggleHtmlMode();
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // nothing special to do
                }
            })
                    .create()
                    .show();
        } else {
            toggleHtmlMode();
        }
    }

    public void requestContentAreaFocus() {
        if (isAdded() && mContentAndSourceContainer != null) {
            mContentAndSourceContainer.requestFocus();
        }
    }

    private void toggleHtmlMode() {
        mEditorFragmentListener.onTrackableEvent(TrackableEvent.HTML_BUTTON_TAPPED);
        mEditorFragmentListener.onHtmlModeToggledInToolbar();

        // Don't switch to HTML mode if currently uploading media
        if (!mUploadingMediaProgressMax.isEmpty() || isActionInProgress()) {
            ToastUtils.showToast(getActivity(), R.string.alert_action_while_uploading, ToastUtils.Duration.LONG);
            return;
        }

        mFormattingToolbar.toggleEditorMode();

        if (mSource.getVisibility() == View.VISIBLE) {
            updateFailedMediaList();
        } else {
            addOverlayToGifs();
        }
    }

    private void updateFailedAndUploadingMedia() {
        updateFailedMediaList();
        overlayFailedMedia();

        updateUploadingMediaList();
        overlayProgressingMedia();
    }

    public void enableMediaMode(boolean enable) {
        mFormattingToolbar.enableMediaMode(enable);

        if (enable) {
            mFormattingToolbar.showMediaToolbar();
        } else {
            mFormattingToolbar.hideMediaToolbar();
        }

        getActivity().invalidateOptionsMenu();
    }

    @Override
    public boolean isActionInProgress() {
        return System.currentTimeMillis() - mActionStartedAt < MAX_ACTION_TIME_MS;
    }

    private void updateUploadingMediaList() {
        AztecText.AttributePredicate uploadingPredicate = getPredicateWithClass(ATTR_STATUS_UPLOADING);

        mUploadingMediaProgressMax.clear();

        // update all items with upload progress of zero
        for (Attributes attrs : mContent.getAllElementAttributes(uploadingPredicate)) {
            String localMediaId = attrs.getValue(ATTR_ID_WP);
            if (!TextUtils.isEmpty(localMediaId)) {
                mUploadingMediaProgressMax.put(localMediaId, 0f);
            }
        }
    }

    private void safeAddMediaIdToSet(Set<String> setToAddTo, String wpId) {
        if (!TextUtils.isEmpty(wpId)) {
            setToAddTo.add(wpId);
        }
    }

    private static AztecText.AttributePredicate getPredicateWithClass(final String classToUse) {
        AztecText.AttributePredicate predicate = new AztecText.AttributePredicate() {
            @Override
            public boolean matches(@NonNull Attributes attrs) {
                AttributesWithClass attributesWithClass = getAttributesWithClass(attrs);
                return attributesWithClass.hasClass(classToUse);
            }
        };

        return predicate;
    }

    private static AttributesWithClass getAttributesWithClass(@NonNull Attributes attrs) {
        return new AttributesWithClass(attrs);
    }

    private void updateFailedMediaList() {
        AztecText.AttributePredicate failedPredicate = getPredicateWithClass(ATTR_STATUS_FAILED);

        mFailedMediaIds.clear();

        for (Attributes attrs : mContent.getAllElementAttributes(failedPredicate)) {
            String localMediaId = attrs.getValue(ATTR_ID_WP);
            safeAddMediaIdToSet(mFailedMediaIds, localMediaId);
        }
    }

    private void overlayGifImages() {
        if (mContent == null) {
            // discard any events if Aztec hasn't been initialized
            return;
        }

        MediaPredicate predicate = MediaPredicate.getAnimatedMediaPredicate();

        Drawable gifOverlay = getResources().getDrawable(R.drawable.gif_overlay_vector);
        mContent.setOverlay(predicate, 0, gifOverlay, Gravity.TOP | Gravity.START);
    }

    private void overlayProgressingMedia() {
        for (String localMediaId : mUploadingMediaProgressMax.keySet()) {
            overlayProgressingMediaForMediaId(localMediaId);
        }
    }

    private void overlayProgressingMediaForMediaId(String localMediaId) {
        if (mContent == null) {
            // discard any events if Aztec hasn't been initialized
            return;
        }

        MediaPredicate predicate = MediaPredicate.getLocalMediaIdPredicate(localMediaId);
        overlayProgressingMedia(predicate);
        // here check if this is a video uploading in progress or not; if it is, show the video play icon
        for (Attributes attrs : mContent.getAllElementAttributes(predicate)) {
            AttributesWithClass attributesWithClass = getAttributesWithClass(attrs);
            if (attributesWithClass.hasClass(TEMP_VIDEO_UPLOADING_CLASS)) {
                overlayVideoIcon(2, predicate);
            }
        }
    }


    private void overlayVideoIcon(int overlayLevel, AztecText.AttributePredicate predicate) {
        Drawable videoDrawable = getResources().getDrawable(R.drawable.ic_overlay_video);
        mContent.setOverlay(predicate, overlayLevel, videoDrawable, Gravity.BOTTOM | Gravity.START);
    }

    private void overlayProgressingMedia(MediaPredicate predicate) {
        // set intermediate shade overlay
        mContent.setOverlay(predicate, 0,
                new ColorDrawable(getResources().getColor(R.color.media_shade_overlay_color)),
                Gravity.FILL);

        Drawable progressDrawable = getResources().getDrawable(android.R.drawable.progress_horizontal);
        // set the height of the progress bar to 2 (it's in dp since the drawable will be adjusted by the span)
        progressDrawable.setBounds(0, 0, 0, 4);

        mContent.setOverlay(predicate, 1, progressDrawable,
                Gravity.FILL_HORIZONTAL | Gravity.TOP);

        mContent.resetAttributedMediaSpan(predicate);
    }

    private void overlayFailedMedia() {
        for (String localMediaId : mFailedMediaIds) {
            Attributes attributes =
                    mContent.getElementAttributes(MediaPredicate.getLocalMediaIdPredicate(localMediaId));
            overlayFailedMedia(localMediaId, attributes);
        }
    }

    private void overlayFailedMedia(String localMediaId, Attributes attributes) {
        // set intermediate shade overlay
        AztecText.AttributePredicate localMediaIdPredicate = MediaPredicate.getLocalMediaIdPredicate(localMediaId);

        Drawable iconDrawable = getResources().getDrawable(R.drawable.media_retry_image);
        float textSize = getResources().getDimension(R.dimen.text_header);
        TextDrawable textDrawable = new TextDrawable(getResources(),
                getString(R.string.editor_failed_to_insert_media_tap_to_retry), textSize);
        // Divide icon height by 2 and shift the text vertically (note: both elements are vertically centered)
        textDrawable.setTranslateY(iconDrawable.getIntrinsicHeight() / 2);

        mContent.setOverlay(localMediaIdPredicate, 0,
                new ColorDrawable(getResources().getColor(R.color.media_shade_overlay_color)),
                Gravity.FILL);
        mContent.setOverlay(localMediaIdPredicate, 1, iconDrawable, Gravity.CENTER);
        mContent.setOverlay(localMediaIdPredicate, 2, textDrawable, Gravity.CENTER);
        mContent.updateElementAttributes(localMediaIdPredicate, new AztecAttributes(attributes));
    }

    /**
     * Returns the contents of the content field from the JavaScript editor. Should be called from a background thread
     * where possible.
     */
    @Override
    public CharSequence getContent(CharSequence originalContent) throws EditorFragmentNotAddedException {
        if (!isAdded()) {
            throw new EditorFragmentNotAddedException();
        }

        if (mContent.getVisibility() == View.VISIBLE) {
            return mContent.hasChanges() == EditorHasChanges.NO_CHANGES ? originalContent : mContent.toHtml(false);
        } else {
            return mSource.hasChanges() == EditorHasChanges.NO_CHANGES ? originalContent : mSource.getPureHtml(false);
        }
    }

    @Override
    public LiveData<Editable> getTitleOrContentChanged() {
        return mTextWatcher.getAfterTextChanged();
    }

    @Override
    public void onContentChanged() {
        mTextWatcher.postTextChanged();
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

        if (URLUtil.isNetworkUrl(mediaUrl)) {
            final AztecAttributes attributes = new AztecAttributes();
            attributes.setValue(ATTR_SRC, mediaUrl);

            if (mediaFile.isVideo()) {
                // VideoPress special case here
                if (!TextUtils.isEmpty(mediaFile.getVideoPressShortCode())) {
                    attributes.removeAttribute(ATTR_SRC);
                    String videoPressId = ShortcodeUtils.getVideoPressIdFromShortCode(
                            mediaFile.getVideoPressShortCode());
                    attributes.setValue(VideoPressExtensionsKt.getATTRIBUTE_VIDEOPRESS_HIDDEN_ID(), videoPressId);
                    attributes.setValue(VideoPressExtensionsKt.getATTRIBUTE_VIDEOPRESS_HIDDEN_SRC(), mediaUrl);
                }
                // Do not set default attributes here like for pictures
                addVideoUploadingClassIfMissing(attributes);
                mContent.insertVideo(getLoadingVideoPlaceholder(), attributes);
                overlayVideoIcon(0, new MediaPredicate(mediaUrl, ATTR_SRC));
            } else {
                setDefaultAttributes(attributes, mediaFile);
                mContent.insertImage(getLoadingImagePlaceholder(), attributes);
            }

            final String posterURL = mediaFile.isVideo() ? Utils.escapeQuotes(StringUtils.notNullStr(
                    mediaFile.getThumbnailURL())) : mediaUrl;
            mAztecImageLoader.loadImage(posterURL, new Callbacks() {
                private void replaceDrawable(Drawable newDrawable) {
                    AztecMediaSpan[] imageOrVideoSpans =
                            mContent.getText().getSpans(0, mContent.getText().length(), AztecMediaSpan.class);
                    for (AztecMediaSpan currentClass : imageOrVideoSpans) {
                        if (currentClass.getAttributes().hasAttribute(ATTR_SRC)
                            && mediaUrl.equals(currentClass.getAttributes().getValue(ATTR_SRC))) {
                            currentClass.setDrawable(newDrawable);
                        } else if (currentClass.getAttributes().hasAttribute(
                                VideoPressExtensionsKt.getATTRIBUTE_VIDEOPRESS_HIDDEN_SRC())
                                   && mediaUrl.equals(currentClass.getAttributes().getValue(
                                VideoPressExtensionsKt.getATTRIBUTE_VIDEOPRESS_HIDDEN_SRC()))) {
                            currentClass.setDrawable(newDrawable);
                        }
                    }
                    addOverlayToGifs();
                    mContent.refreshText();
                }

                @Override public void onImageFailed() {
                    if (!isAdded()) {
                        // the fragment is detached
                        return;
                    }
                    replaceDrawable(getLoadingMediaErrorPlaceholder(null));
                }

                @Override public void onImageLoaded(Drawable drawable) {
                    if (!isAdded()) {
                        // the fragment is detached
                        return;
                    }
                    if (drawable == null) {
                        replaceDrawable(getLoadingMediaErrorPlaceholder(null));
                        return;
                    }

                    if (drawable.getIntrinsicHeight() < minMediaSize || drawable.getIntrinsicWidth() < minMediaSize) {
                        // Bitmap is too small. Show image placeholder.
                        replaceDrawable(getLoadingMediaErrorPlaceholder(getString(R.string.error_media_small)));
                        return;
                    }
                    replaceDrawable(drawable);
                }

                @Override public void onImageLoading(Drawable drawable) {
                }
            }, maxMediaSize);
            mActionStartedAt = System.currentTimeMillis();
        } else {
            String localMediaId = String.valueOf(mediaFile.getId());
            final String safeMediaPreviewUrl = mediaFile.isVideo()
                    ? Utils.escapeQuotes(StringUtils.notNullStr(mediaFile.getThumbnailURL()))
                    : Utils.escapeQuotes(mediaUrl);

            AztecAttributes attrs = new AztecAttributes();
            attrs.setValue(ATTR_ID_WP, localMediaId);
            attrs.setValue(ATTR_SRC, Utils.escapeQuotes(mediaUrl));
            attrs.setValue(ATTR_CLASS, ATTR_STATUS_UPLOADING);

            addDefaultSizeClassIfMissing(attrs);

            Bitmap bitmapToShow = ImageUtils.getWPImageSpanThumbnailFromFilePath(
                    getActivity(), safeMediaPreviewUrl, maxMediaSize
                                                                                );
            MediaPredicate localMediaIdPredicate = MediaPredicate.getLocalMediaIdPredicate(localMediaId);
            if (bitmapToShow != null) {
                // By default, BitmapFactory.decodeFile sets the bitmap's density to the device default so, we need
                // to correctly set the input density to 160 ourselves.
                bitmapToShow.setDensity(DisplayMetrics.DENSITY_DEFAULT);
                if (mediaFile.isVideo()) {
                    addVideoUploadingClassIfMissing(attrs);
                    mContent.insertVideo(new BitmapDrawable(getResources(), bitmapToShow), attrs);
                } else {
                    mContent.insertImage(new BitmapDrawable(getResources(), bitmapToShow), attrs);
                }
            } else {
                // Failed to retrieve bitmap. Show failed placeholder.
                Drawable drawable = getLoadingMediaErrorPlaceholder(null);
                mContent.insertImage(drawable, attrs);
            }

            // set intermediate shade overlay
            overlayProgressingMedia(localMediaIdPredicate);

            mUploadingMediaProgressMax.put(localMediaId, 0f);

            if (mediaFile.isVideo()) {
                overlayVideoIcon(2, localMediaIdPredicate);
            }

            mContent.updateElementAttributes(localMediaIdPredicate, attrs);

            mContent.resetAttributedMediaSpan(localMediaIdPredicate);
        }
    }

    @Override public void appendMediaFiles(Map<String, MediaFile> mediaList) {
        for (Map.Entry<String, MediaFile> pair : mediaList.entrySet()) {
            appendMediaFile(pair.getValue(), pair.getKey(), null);
        }
    }

    private Drawable getLoadingMediaErrorPlaceholder(String msg) {
        if (TextUtils.isEmpty(msg)) {
            ToastUtils.showToast(getActivity(), R.string.error_media_load);
        } else {
            ToastUtils.showToast(getActivity(), msg);
        }
        return EditorMediaUtils.getAztecPlaceholderDrawableFromResID(this.getActivity(),
                R.drawable.ic_image_failed_grey_a_40_48dp,
                maxMediaSize);
    }

    @Override
    public void appendGallery(MediaGallery mediaGallery) {
        String shortcode = "[gallery %s=\"%s\" ids=\"%s\"]";
        if (TextUtils.isEmpty(mediaGallery.getType())) {
            shortcode = String.format(shortcode, "columns",
                    mediaGallery.getNumColumns(),
                    mediaGallery.getIdsStr());
        } else {
            shortcode = String.format(shortcode, "type",
                    mediaGallery.getType(),
                    mediaGallery.getIdsStr());
        }
        mContent.getText().insert(mContent.getSelectionEnd(), shortcode);
    }

    @Override
    public void setUrlForVideoPressId(final String videoId, final String videoUrl, final String posterUrl) {
        VideoPressExtensionsKt.updateVideoPressThumb(mContent, posterUrl, videoUrl, videoId);
    }

    @Override
    public boolean isUploadingMedia() {
        return (mUploadingMediaProgressMax.size() > 0);
    }

    @Override
    public boolean hasFailedMediaUploads() {
        return (mFailedMediaIds.size() > 0);
    }

    @Override
    public void removeAllFailedMediaUploads() {
        mContent.removeMedia(new AztecText.AttributePredicate() {
            @Override
            public boolean matches(@NonNull Attributes attrs) {
                return getAttributesWithClass(attrs).hasClass(ATTR_STATUS_FAILED);
            }
        });
        mFailedMediaIds.clear();
    }

    @Override
    public void removeMedia(String mediaId) {
        mContent.removeMedia(MediaPredicate.getLocalMediaIdPredicate(mediaId));
    }

    @Override
    public boolean showSavingProgressDialogIfNeeded() {
        return false;
    }

    @Override
    public boolean hideSavingProgressDialog() {
        return false;
    }

    @Override public void mediaSelectionCancelled() {
        // noop implementation for shared interface with block editor
    }

    @Override
    public void onMediaUploadReattached(String localId, float currentProgress) {
        mUploadingMediaProgressMax.put(localId, currentProgress);
        overlayProgressingMediaForMediaId(localId);
    }

    @Override
    public void onMediaUploadRetry(String localId, MediaType mediaType) {
        mFailedMediaIds.remove(localId);
        onMediaUploadReattached(localId, 0);
    }

    @Override
    public void onMediaUploadSucceeded(final String localMediaId, final MediaFile mediaFile) {
        if (!isAdded() || mContent == null || !mAztecReady) {
            return;
        }

        if (mediaFile != null) {
            String remoteUrl = Utils.escapeQuotes(mediaFile.getFileURL());
            AppLog.i(T.MEDIA, "onMediaUploadSucceeded - Remote URL: " + remoteUrl + ", Filename: "
                              + mediaFile.getFileName());

            // we still need to refresh the screen visually, no matter whether the service already
            // saved the post to Db or not
            MediaType mediaType = EditorFragmentAbstract.getEditorMimeType(mediaFile);
            if (mediaType.equals(MediaType.IMAGE) || mediaType.equals(MediaType.VIDEO)) {
                // clear overlay
                MediaPredicate predicate = MediaPredicate.getLocalMediaIdPredicate(localMediaId);


                AztecAttributes attrs = mContent.getElementAttributes(predicate);
                attrs.setValue("src", remoteUrl);

                if (mediaType.equals(MediaType.IMAGE)) {
                    setDefaultAttributes(attrs, mediaFile);
                }

                // remove the uploading class
                AttributesWithClass attributesWithClass = getAttributesWithClass(attrs);
                attributesWithClass.removeClass(ATTR_STATUS_UPLOADING);

                if (mediaFile.isVideo()) {
                    attributesWithClass.removeClass(TEMP_VIDEO_UPLOADING_CLASS);
                }

                attrs.setValue(ATTR_CLASS, attributesWithClass.getAttributes().getValue(ATTR_CLASS));

                // VideoPress special case here
                if (mediaType.equals(MediaType.VIDEO) && !TextUtils.isEmpty(mediaFile.getVideoPressShortCode())) {
                    String videoPressId = ShortcodeUtils.getVideoPressIdFromShortCode(
                            mediaFile.getVideoPressShortCode());
                    attrs.setValue(VideoPressExtensionsKt.getATTRIBUTE_VIDEOPRESS_HIDDEN_ID(), videoPressId);
                    attrs.setValue(VideoPressExtensionsKt.getATTRIBUTE_VIDEOPRESS_HIDDEN_SRC(), remoteUrl);
                    attrs.removeAttribute("src");
                    attrs.removeAttribute(ATTR_CLASS);
                }

                // clear overlay
                mContent.clearOverlays(predicate);
                if (mediaType.equals(MediaType.VIDEO)) {
                    overlayVideoIcon(0, predicate);
                } else {
                    addOverlayToGifs();
                }
                mContent.resetAttributedMediaSpan(predicate);
                // finally remove the local id as it won't be necessary anynmore
                attrs.removeAttribute(ATTR_ID_WP);
                attrs.removeAttribute(TEMP_IMAGE_ID);
                mContent.updateElementAttributes(predicate, attrs);

                mUploadingMediaProgressMax.remove(localMediaId);
            }
        }
    }

    @Override
    public void onMediaDeleted(AztecAttributes aztecAttributes) {
        String localMediaId = aztecAttributes.getValue(ATTR_ID_WP);
        mUploadingMediaProgressMax.remove(localMediaId);
        if (!TextUtils.isEmpty(localMediaId)) {
            mEditorFragmentListener.onMediaDeleted(localMediaId);
        }
    }

    private static class MediaPredicate implements AztecText.AttributePredicate, Parcelable {
        private final String mId;
        private final String mAttributeName;

        static MediaPredicate getLocalMediaIdPredicate(String id) {
            return new MediaPredicate(id, ATTR_ID_WP);
        }

        static MediaPredicate getTempMediaIdPredicate(String id) {
            return new MediaPredicate(id, TEMP_IMAGE_ID);
        }

        static MediaPredicate getAnimatedMediaPredicate() {
            return new MediaPredicate(ANIMATED_MEDIA, ANIMATED_MEDIA);
        }

        MediaPredicate(String id, String attributeName) {
            mId = id;
            mAttributeName = attributeName;
        }

        @Override
        public boolean matches(@NonNull Attributes attrs) {
            return attrs.getIndex(mAttributeName) > -1 && attrs.getValue(mAttributeName).equals(mId);
        }

        protected MediaPredicate(Parcel in) {
            mId = in.readString();
            mAttributeName = in.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mId);
            dest.writeString(mAttributeName);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<MediaPredicate> CREATOR = new Parcelable.Creator<MediaPredicate>() {
            @Override
            public MediaPredicate createFromParcel(Parcel in) {
                return new MediaPredicate(in);
            }

            @Override
            public MediaPredicate[] newArray(int size) {
                return new MediaPredicate[size];
            }
        };
    }

    @Override
    public void onMediaUploadProgress(final String localMediaId, final float progress) {
        if (!isAdded() || mContent == null || !mAztecReady || TextUtils.isEmpty(localMediaId)) {
            return;
        }

        // check a previous maximum for this localMediaId exists
        // if there is not, we've probably already gotten the upload fail/success signal, thus
        // we already removed this id from the array. Nothing left to do, disregard this event.
        if (mUploadingMediaProgressMax.get(localMediaId) == null) {
            return;
        }

        // first obtain the latest maximum
        float maxProgressForLocalMediaId = mUploadingMediaProgressMax.get(localMediaId);

        // only update if the new progress value is greater than the latest maximum reflected on the
        // screen
        if (progress > maxProgressForLocalMediaId) {
            synchronized (AztecEditorFragment.this) {
                maxProgressForLocalMediaId = progress;
                mUploadingMediaProgressMax.put(localMediaId, maxProgressForLocalMediaId);

                try {
                    AztecText.AttributePredicate localMediaIdPredicate =
                            MediaPredicate.getLocalMediaIdPredicate(localMediaId);
                    mContent.setOverlayLevel(localMediaIdPredicate, 1, (int) (progress * 10000));
                    mContent.resetAttributedMediaSpan(localMediaIdPredicate);
                } catch (IndexOutOfBoundsException ex) {
                    /*
                     * it could happen that the localMediaId is not found, because FluxC events are not
                     * guaranteed to be received in order, so we might have received the `upload
                     * finished` event (thus clearing the id from within the Post html), and then
                     * still receive some more progress events for the same file, which we can't
                     * avoid but disregard.
                     * ex.printStackTrace();
                     */
                    AppLog.d(AppLog.T.EDITOR, localMediaId + " - not found trying to update progress ");
                }
            }
        }
    }

    @Override
    public void onMediaUploadFailed(final String localMediaId, final EditorFragmentAbstract.MediaType
            mediaType, final String errorMessage) {
        if (!isAdded() || mContent == null) {
            return;
        }
        if (mediaType != null) {
            switch (mediaType) {
                case IMAGE:
                case VIDEO:
                    MediaPredicate localMediaIdPredicate = MediaPredicate.getLocalMediaIdPredicate(localMediaId);
                    AttributesWithClass attributesWithClass = getAttributesWithClass(
                            mContent.getElementAttributes(localMediaIdPredicate));

                    attributesWithClass.removeClass(ATTR_STATUS_UPLOADING);
                    attributesWithClass.addClass(ATTR_STATUS_FAILED);

                    mContent.clearOverlays(localMediaIdPredicate);
                    overlayFailedMedia(localMediaId, attributesWithClass.getAttributes());
                    mContent.resetAttributedMediaSpan(localMediaIdPredicate);
                    break;
            }
            mFailedMediaIds.add(localMediaId);
            mUploadingMediaProgressMax.remove(localMediaId);
        }
    }

    @Override
    public void onVideoInfoRequested(final AztecAttributes attrs) {
        // VideoPress special case here
        if (attrs.hasAttribute(VideoPressExtensionsKt.getATTRIBUTE_VIDEOPRESS_HIDDEN_ID())) {
            mEditorFragmentListener.onVideoPressInfoRequested(attrs.getValue(
                    VideoPressExtensionsKt.getATTRIBUTE_VIDEOPRESS_HIDDEN_ID()));
        }
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

    private final View.OnDragListener mOnDragListener = new View.OnDragListener() {
        private boolean isSupported(ClipDescription clipDescription, List<String> mimeTypesToCheck) {
            if (clipDescription == null) {
                return false;
            }

            for (String supportedMimeType : mimeTypesToCheck) {
                if (clipDescription.hasMimeType(supportedMimeType)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            switch (dragEvent.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return isSupported(dragEvent.getClipDescription(), DRAGNDROP_SUPPORTED_MIMETYPES_TEXT)
                           || isSupported(dragEvent.getClipDescription(), DRAGNDROP_SUPPORTED_MIMETYPES_IMAGE);
                case DragEvent.ACTION_DRAG_ENTERED:
                    // would be nice to start marking the place the item will drop
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                    int x = DisplayUtils.pxToDp(getActivity(), (int) dragEvent.getX());
                    int y = DisplayUtils.pxToDp(getActivity(), (int) dragEvent.getY());

                    mContent.setSelection(mContent.getOffsetForPosition(x, y));
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    // clear any drop marking maybe
                    break;
                case DragEvent.ACTION_DROP:
                    if (mSource.getVisibility() == View.VISIBLE) {
                        if (isSupported(dragEvent.getClipDescription(), DRAGNDROP_SUPPORTED_MIMETYPES_IMAGE)) {
                            // don't allow dropping images into the HTML source
                            ToastUtils.showToast(getActivity(), R.string.editor_dropped_html_images_not_allowed,
                                    ToastUtils.Duration.LONG);
                            return true;
                        } else {
                            // let the system handle the text drop
                            return false;
                        }
                    }

                    if (isSupported(dragEvent.getClipDescription(), DRAGNDROP_SUPPORTED_MIMETYPES_IMAGE)
                        && isTitleFocused()) {
                        // don't allow dropping images into the title field
                        ToastUtils.showToast(getActivity(), R.string.editor_dropped_title_images_not_allowed,
                                ToastUtils.Duration.LONG);
                        return true;
                    }

                    if (isAdded()) {
                        mEditorDragAndDropListener.onRequestDragAndDropPermissions(dragEvent);
                    }

                    ClipDescription clipDescription = dragEvent.getClipDescription();
                    if (clipDescription == null || clipDescription.getMimeTypeCount() < 1) {
                        break;
                    }

                    ContentResolver contentResolver = getActivity().getContentResolver();
                    ArrayList<Uri> uris = new ArrayList<>();
                    boolean unsupportedDropsFound = false;

                    for (int i = 0; i < dragEvent.getClipData().getItemCount(); i++) {
                        ClipData.Item item = dragEvent.getClipData().getItemAt(i);
                        Uri uri = item.getUri();

                        final String uriType = uri != null ? contentResolver.getType(uri) : null;
                        if (uriType != null && DRAGNDROP_SUPPORTED_MIMETYPES_IMAGE.contains(uriType)) {
                            uris.add(uri);
                            continue;
                        } else if (item.getText() != null) {
                            insertTextToEditor(item.getText().toString());
                            continue;
                        } else if (item.getHtmlText() != null) {
                            insertTextToEditor(item.getHtmlText());
                            continue;
                        }

                        // any other drop types are not supported, including web URLs. We cannot proactively
                        // determine their mime type for filtering
                        unsupportedDropsFound = true;
                    }

                    if (unsupportedDropsFound) {
                        ToastUtils.showToast(getActivity(), R.string.editor_dropped_unsupported_files, ToastUtils
                                .Duration.LONG);
                    }

                    if (uris.size() > 0) {
                        mEditorDragAndDropListener.onMediaDropped(uris);
                    }

                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    // clear any drop marking maybe
                default:
                    break;
            }
            return true;
        }

        private void insertTextToEditor(String text) {
            if (text != null) {
                mContent.getText().insert(mContent.getSelectionStart(), reformatVisually(Utils.escapeHtml(text)));
            } else {
                ToastUtils.showToast(getActivity(), R.string.editor_dropped_text_error, ToastUtils.Duration.SHORT);
                AppLog.d(AppLog.T.EDITOR, "Dropped text was null!");
            }
        }

        private String reformatVisually(String text) {
            // TODO: implement wp.loadText (see wpload.js)
            return text;
        }

        private boolean isTitleFocused() {
            return mTitle.isFocused();
        }
    };

    @Override
    public boolean onToolbarMediaButtonClicked() {
        mEditorFragmentListener.onTrackableEvent(TrackableEvent.MEDIA_BUTTON_TAPPED);

        if (isActionInProgress()) {
            ToastUtils.showToast(getActivity(), R.string.alert_action_while_uploading, ToastUtils.Duration.LONG);
        }

        if (mSource.isFocused()) {
            ToastUtils.showToast(getActivity(), R.string.alert_insert_image_html_mode, ToastUtils.Duration.LONG);
        } else {
            mEditorFragmentListener.onAddMediaClicked();
        }

        return true;
    }

    @Override
    public void onImageTapped(@NonNull AztecAttributes attrs, int naturalWidth, int naturalHeight) {
        onMediaTapped(attrs, naturalWidth, naturalHeight, MediaType.IMAGE);
    }

    @Override
    public void onVideoTapped(@NonNull AztecAttributes attrs) {
        onMediaTapped(attrs, 0, 0, MediaType.VIDEO);
    }

    private void setIdAttributeOnMedia(AztecAttributes attrs, String idName, String localMediaId) {
        attrs.setValue(idName, localMediaId);
        mTappedMediaPredicate = new MediaPredicate(localMediaId, idName);
    }

    private void onMediaTapped(@NonNull final AztecAttributes attrs, int naturalWidth, int naturalHeight,
                               final MediaType mediaType) {
        if (mediaType == null || !isAdded()) {
            return;
        }

        Set<String> classes = MetadataUtils.getClassAttribute(attrs);
        String idName;
        String uploadStatus = "";
        final JSONObject meta = MetadataUtils.getMetadata(getAttributesWithClass(attrs), naturalWidth, naturalHeight);
        if (classes.contains(ATTR_STATUS_UPLOADING)) {
            uploadStatus = ATTR_STATUS_UPLOADING;
            idName = ATTR_ID_WP;
        } else if (classes.contains(ATTR_STATUS_FAILED)) {
            uploadStatus = ATTR_STATUS_FAILED;
            idName = ATTR_ID_WP;
        } else {
            idName = ATTR_ID;
        }

        final String localMediaId;
        // generate the element ID if ATTR_ID or ATTR_ID_WP are missing
        if (!attrs.hasAttribute(idName) || TextUtils.isEmpty(attrs.getValue(idName))) {
            idName = TEMP_IMAGE_ID;
            localMediaId = UUID.randomUUID().toString();
        } else {
            localMediaId = attrs.getValue(idName);
        }

        setIdAttributeOnMedia(attrs, idName, localMediaId);

        switch (uploadStatus) {
            case ATTR_STATUS_UPLOADING:
                // Display 'cancel upload' dialog
                AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
                builder.setTitle(getString(R.string.stop_upload_dialog_title));
                builder.setPositiveButton(R.string.stop_upload_dialog_button_yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (mUploadingMediaProgressMax.containsKey(localMediaId)) {
                                    mEditorFragmentListener.onMediaUploadCancelClicked(localMediaId);

                                    switch (mediaType) {
                                        case IMAGE:
                                            mContent.removeMedia(mTappedMediaPredicate);
                                            break;
                                        case VIDEO:
                                            mContent.removeMedia(mTappedMediaPredicate);
                                    }
                                    mUploadingMediaProgressMax.remove(localMediaId);
                                } else {
                                    ToastUtils.showToast(getActivity(), R.string.upload_finished_toast).show();
                                }

                                dialog.dismiss();
                            }
                        });

                builder.setNegativeButton(R.string.stop_upload_dialog_button_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
                break;
            case ATTR_STATUS_FAILED:
                // Retry media upload
                boolean successfullyRetried = true;
                if (mFailedMediaIds.contains(localMediaId)) {
                    successfullyRetried = mEditorFragmentListener.onMediaRetryClicked(localMediaId);
                }
                if (successfullyRetried) {
                    switch (mediaType) {
                        case IMAGE:
                        case VIDEO:
                            AttributesWithClass attributesWithClass = getAttributesWithClass(
                                    mContent.getElementAttributes(mTappedMediaPredicate));

                            // add or remove the failed class depending on whether the media was uploaded or not
                            attributesWithClass = addFailedStatusToMediaIfLocalSrcPresent(attributesWithClass);

                            // clear overlays
                            mContent.clearOverlays(mTappedMediaPredicate);

                            if (!attributesWithClass.hasClass(ATTR_STATUS_FAILED)) {
                                // just save the item and leave
                                mContent.resetAttributedMediaSpan(mTappedMediaPredicate);
                                return;
                            }

                            attributesWithClass.addClass(ATTR_STATUS_UPLOADING);
                            if (mediaType.equals(MediaType.VIDEO)) {
                                attributesWithClass.addClass(TEMP_VIDEO_UPLOADING_CLASS);
                            }

                            // set intermediate shade overlay
                            overlayProgressingMedia(mTappedMediaPredicate);

                            mContent.updateElementAttributes(mTappedMediaPredicate,
                                    attributesWithClass.getAttributes());

                            if (mediaType.equals(MediaType.VIDEO)) {
                                overlayVideoIcon(2, mTappedMediaPredicate);
                            }

                            mContent.resetAttributedMediaSpan(mTappedMediaPredicate);
                            break;
                    }
                    mFailedMediaIds.remove(localMediaId);
                    mUploadingMediaProgressMax.put(localMediaId, 0f);
                }
                break;
            default:
                if (mediaType.equals(MediaType.VIDEO)) {
                    try {
                        // Open the video preview in the default browser for now.
                        // TODO open the preview activity already available in media?
                        final String videoURL = meta.getString(ATTR_SRC);
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoURL));
                        startActivity(browserIntent);
                    } catch (JSONException e) {
                        AppLog.e(AppLog.T.EDITOR, "Could not retrieve image url from JSON metadata");
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(this.getActivity(),
                                "No application can handle this request." + " Please install a Web browser",
                                Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                // If it's not a picture skip the click
                if (!mediaType.equals(MediaType.IMAGE)) {
                    return;
                }

                Gson gson = new Gson();

                EditorImageMetaData metaData = gson.fromJson(meta.toString(), EditorImageMetaData.class);

                AztecAttributes captionAttributes =
                        CaptionExtensionsKt.getImageCaptionAttributes(mContent, mTappedMediaPredicate);

                if (captionAttributes.hasAttribute(ATTR_ALIGN)) {
                    metaData.setAlign(captionAttributes.getValue(ATTR_ALIGN));
                }

                metaData.setCaption(CaptionExtensionsKt.getImageCaption(mContent, mTappedMediaPredicate));

                String mediaLink = MediaLinkExtensionsKt.getMediaLink(mContent, mTappedMediaPredicate);
                if (!TextUtils.isEmpty(mediaLink)) {
                    AztecAttributes linkAttributes =
                            MediaLinkExtensionsKt.getMediaLinkAttributes(mContent, mTappedMediaPredicate);

                    metaData.setLinkUrl(mediaLink);

                    String linkTarget = linkAttributes.getValue(ATTR_TARGET);
                    metaData.setLinkTargetBlank(!TextUtils.isEmpty(linkTarget) && linkTarget.equals("_blank"));
                }

                // Use https:// when requesting the auth header, in case the image is incorrectly using http://
                // If an auth header is returned, force https:// for the actual HTTP request
                final String imageSrc = metaData.getSrc();
                Map<String, String> authHeaders =
                        mEditorFragmentListener.onAuthHeaderRequested(UrlUtils.makeHttps(imageSrc));
                if (authHeaders != null && authHeaders.containsKey(AUTHORIZATION_HEADER_NAME)) {
                    metaData.setSrc(UrlUtils.makeHttps(imageSrc));
                }

                if (mEditorImageSettingsListener != null) {
                    mEditorImageSettingsListener.onImageSettingsRequested(metaData);
                }

                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDITOR_MEDIA_SETTINGS) {
            if (mTappedMediaPredicate != null) {
                if (data == null || data.getExtras() == null) {
                    return;
                }

                EditorImageMetaData metaData = data.getParcelableExtra(ARG_EDITOR_IMAGE_METADATA);

                if (metaData.isRemoved()) {
                    CaptionExtensionsKt.removeImageCaption(mContent, mTappedMediaPredicate);
                    mContent.removeMedia(mTappedMediaPredicate);

                    if (isHistoryEnabled()) {
                        mContent.history.beforeTextChanged(mContent);
                    }
                } else {
                    // changing image settings should be recorded in history
                    if (isHistoryEnabled()) {
                        mContent.history.beforeTextChanged(mContent);
                    }

                    AztecAttributes attributes = mContent.getElementAttributes(mTappedMediaPredicate);
                    attributes.setValue(ATTR_SRC, metaData.getSrc());

                    if (!TextUtils.isEmpty(metaData.getTitle())) {
                        attributes.setValue(ATTR_TITLE, metaData.getTitle());
                    } else {
                        attributes.removeAttribute(ATTR_TITLE);
                    }

                    if (!TextUtils.isEmpty(metaData.getAlt())) {
                        attributes.setValue(ATTR_ALT, metaData.getAlt());
                    } else {
                        attributes.removeAttribute(ATTR_ALT);
                    }

                    if (!TextUtils.isEmpty(metaData.getWidth())) {
                        attributes.setValue(ATTR_DIMEN_WIDTH, metaData.getWidth());
                    } else {
                        attributes.removeAttribute(ATTR_DIMEN_WIDTH);
                    }

                    if (!TextUtils.isEmpty(metaData.getHeight())) {
                        attributes.setValue(ATTR_DIMEN_HEIGHT, metaData.getHeight());
                    } else {
                        attributes.removeAttribute(ATTR_DIMEN_HEIGHT);
                    }

                    if (!TextUtils.isEmpty(metaData.getLinkUrl())) {
                        AztecAttributes linkAttributes =
                                MediaLinkExtensionsKt.getMediaLinkAttributes(mContent, mTappedMediaPredicate);

                        // without "noopener" img tag around img might be removed by calypso editor
                        linkAttributes.setValue("rel", "noopener");

                        // when reusing attributes do not forget to remove href
                        linkAttributes.removeAttribute("href");

                        if (metaData.isLinkTargetBlank()) {
                            linkAttributes.setValue(ATTR_TARGET, "_blank");
                        } else {
                            linkAttributes.removeAttribute(ATTR_TARGET);
                        }

                        MediaLinkExtensionsKt.addLinkToMedia(mContent, mTappedMediaPredicate,
                                UrlUtils.addUrlSchemeIfNeeded(metaData.getLinkUrl(),
                                        false),
                                linkAttributes);
                    } else {
                        MediaLinkExtensionsKt.removeLinkFromMedia(mContent, mTappedMediaPredicate);
                    }

                    AttributesWithClass attributesWithClass = getAttributesWithClass(attributes);

                    // remove previously set class attributes to add updated values
                    attributesWithClass.removeClassStartingWith(ATTR_ALIGN);
                    attributesWithClass.removeClassStartingWith(ATTR_SIZE_DASH);
                    attributesWithClass.removeClassStartingWith(ATTR_IMAGE_WP_DASH);


                    if (!TextUtils.isEmpty(metaData.getCaption())) {
                        AztecAttributes captionAttributes =
                                CaptionExtensionsKt.getImageCaptionAttributes(mContent, mTappedMediaPredicate);

                        // if caption is present apply align attribute to it instead of image
                        if (!TextUtils.isEmpty(metaData.getAlign())) {
                            captionAttributes.setValue(ATTR_ALIGN, ATTR_ALIGN + metaData.getAlign());
                        } else {
                            captionAttributes.removeAttribute(ATTR_ALIGN);
                        }

                        // without width attribute caption will not render on the web
                        captionAttributes.setValue(ATTR_DIMEN_WIDTH, metaData.getWidth());

                        CaptionExtensionsKt.setImageCaption(mContent, mTappedMediaPredicate, metaData.getCaption(),
                                captionAttributes);

                        // setting caption causes rendering issue in some cases, reset content to avoid them
                        mContent.fromHtml(mContent.toHtml(false), false);
                    } else {
                        // if no caption present apply align attribute directly to image
                        if (!TextUtils.isEmpty(metaData.getAlign())) {
                            attributesWithClass.addClass(ATTR_ALIGN + metaData.getAlign());
                        }

                        CaptionExtensionsKt.removeImageCaption(mContent, mTappedMediaPredicate);
                    }

                    if (!TextUtils.isEmpty(metaData.getSize())) {
                        attributesWithClass.addClass(metaData.getSize());
                    }

                    if (!TextUtils.isEmpty(metaData.getAttachmentId())) {
                        attributesWithClass.addClass(ATTR_IMAGE_WP_DASH + metaData.getAttachmentId());
                    }

                    attributes.setValue(ATTR_CLASS, attributesWithClass.getAttributes().getValue(ATTR_CLASS));

                    attributes.removeAttribute(TEMP_IMAGE_ID);
                    mContent.updateElementAttributes(mTappedMediaPredicate, attributes);
                }

                mTappedMediaPredicate = null;
            }
        }
    }


    private static void setDefaultAttributes(AztecAttributes attributes, MediaFile mediaFile) {
        if (mediaFile.getWidth() > 0) {
            attributes.setValue(ATTR_DIMEN_WIDTH, String.valueOf(mediaFile.getWidth()));
        }

        if (mediaFile.getHeight() > 0) {
            attributes.setValue(ATTR_DIMEN_HEIGHT, String.valueOf(mediaFile.getHeight()));
        }

        AttributesWithClass attributesWithClass = getAttributesWithClass(attributes);

        if (!TextUtils.isEmpty(mediaFile.getMediaId())) {
            attributesWithClass.addClass(ATTR_IMAGE_WP_DASH + mediaFile.getMediaId());
        }
        attributesWithClass.addClass(ATTR_ALIGN + "none");

        if (!attributesWithClass.hasClassStartingWith(ATTR_SIZE_DASH)) {
            attributesWithClass.addClass(ATTR_SIZE_DASH + "full");
        }
        attributes.setValue(ATTR_CLASS, attributesWithClass.getAttributes().getValue(ATTR_CLASS));
    }

    private static void addDefaultSizeClassIfMissing(AztecAttributes attributes) {
        AttributesWithClass attrs = getAttributesWithClass(attributes);
        if (!attrs.hasClassStartingWith(ATTR_SIZE_DASH)) {
            attrs.addClass(ATTR_SIZE_DASH + "full");
        }
        attributes.setValue(ATTR_CLASS, attrs.getAttributes().getValue(ATTR_CLASS));
    }

    // this is used for reattachment: when the editor is opened again on a Post that has in-progress
    // video uploads, we need to show the progress bar and the video play icon to differentiate from images
    private static void addVideoUploadingClassIfMissing(AztecAttributes attributes) {
        AttributesWithClass attrs = getAttributesWithClass(attributes);
        if (!attrs.hasClass(TEMP_VIDEO_UPLOADING_CLASS)) {
            attrs.addClass(TEMP_VIDEO_UPLOADING_CLASS);
        }
        attributes.setValue(ATTR_CLASS, attrs.getAttributes().getValue(ATTR_CLASS));
    }

    private static Attributes getFirstElementAttributes(Spanned content, AztecText.AttributePredicate predicate) {
        List<Attributes> firstAttrs = getElementAttributes(content, predicate, true);
        if (firstAttrs.size() == 1) {
            return firstAttrs.get(0);
        } else {
            return null;
        }
    }

    @NonNull
    private static List<Attributes> getAllElementAttributes(Spanned content,
                                                            AztecText.AttributePredicate predicate) {
        return getElementAttributes(content, predicate, false);
    }

    @NonNull
    private static List<Attributes> getElementAttributes(Spanned content,
                                                         AztecText.AttributePredicate predicate,
                                                         boolean returnFirstFoundOnly) {
        IAztecAttributedSpan[] spans = content.getSpans(0, content.length(), IAztecAttributedSpan.class);
        List<Attributes> allAttrs = new ArrayList<>();
        for (IAztecAttributedSpan span : spans) {
            if (predicate.matches(span.getAttributes())) {
                allAttrs.add(span.getAttributes());
                if (returnFirstFoundOnly) {
                    return allAttrs;
                }
            }
        }
        return allAttrs;
    }

    @NonNull
    private static List<IAztecAttributedSpan> getSpansForPredicate(Spanned content,
                                                                   AztecText.AttributePredicate predicate,
                                                                   boolean returnFirstFoundOnly) {
        IAztecAttributedSpan[] spans = content.getSpans(0, content.length(), IAztecAttributedSpan.class);
        List<IAztecAttributedSpan> allMatchingSpans = new ArrayList<>();
        for (IAztecAttributedSpan span : spans) {
            if (predicate.matches(span.getAttributes())) {
                allMatchingSpans.add(span);
                if (returnFirstFoundOnly) {
                    return allMatchingSpans;
                }
            }
        }
        return allMatchingSpans;
    }

    private static void updateElementAttributes(Spanned content,
                                                AztecText.AttributePredicate predicate,
                                                AztecAttributes attrs) {
        IAztecAttributedSpan[] spans = content.getSpans(0, content.length(), IAztecAttributedSpan.class);
        for (IAztecAttributedSpan span : spans) {
            if (predicate.matches(span.getAttributes())) {
                span.setAttributes(attrs);
                return;
            }
        }
    }

    private static SpannableStringBuilder getCalypsoCompatibleStringBuilder(Context context, String postContent,
                                                                            AztecParser parser) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        String cleanSource = Format.removeSourceEditorFormatting(postContent, true, false);
        builder.append(parser.parseHtmlForInspection(cleanSource, context));
        Format.preProcessSpannedText(builder, true);
        return builder;
    }

    private static String toHtml(SpannableStringBuilder builder,
                                 AztecParser parser) {
        clearMetaSpans(builder);
        parser.syncVisualNewlinesOfBlockElements(builder);
        Format.postProcessSpannedText(builder, true);
        return EndOfBufferMarkerAdder.removeEndOfTextMarker(parser.toHtml(builder, false));
    }

    private static void clearMetaSpans(Spannable text) {
        BaseInputConnection.removeComposingSpans(text);
        SuggestionSpan[] spans = text.getSpans(0, text.length(), SuggestionSpan.class);
        for (SuggestionSpan span : spans) {
            text.removeSpan(span);
        }
    }

    public static String replaceMediaFileWithUrl(Context context, @NonNull String postContent,
                                                 String localMediaId, MediaFile mediaFile) {
        if (mediaFile != null) {
            String remoteUrl = StringUtils.notNullStr(Utils.escapeQuotes(mediaFile.getFileURL()));
            // fill in Aztec with the post's content
            AztecParser parser = getAztecParserWithPlugins();
            SpannableStringBuilder builder = getCalypsoCompatibleStringBuilder(context, postContent, parser);

            MediaPredicate predicate = MediaPredicate.getLocalMediaIdPredicate(localMediaId);

            // remove the uploading class
            // Attributes firstElementAttributes = getFirstElementAttributes(content, predicate);
            Attributes firstElementAttributes = getFirstElementAttributes(builder, predicate);
            // let's make sure the element is still there within the content. Sometimes it may happen
            // this method is called but the element doesn't exist in the post content anymore
            if (firstElementAttributes != null) {
                AttributesWithClass attributesWithClass = getAttributesWithClass(firstElementAttributes);
                attributesWithClass.removeClass(ATTR_STATUS_UPLOADING);
                if (mediaFile.isVideo()) {
                    attributesWithClass.removeClass(TEMP_VIDEO_UPLOADING_CLASS);
                }

                // add then new src property with the remoteUrl
                AztecAttributes attrs = attributesWithClass.getAttributes();
                attrs.setValue("src", remoteUrl);

                addDefaultSizeClassIfMissing(attrs);

                updateElementAttributes(builder, predicate, attrs);

                // re-set the post content
                postContent = toHtml(builder, parser);
            }
        }
        return postContent;
    }

    public static String markMediaFailed(Context context, @NonNull String postContent,
                                         String localMediaId, MediaFile mediaFile) {
        if (mediaFile != null) {
            // fill in Aztec with the post's content
            AztecParser parser = getAztecParserWithPlugins();
            SpannableStringBuilder builder = getCalypsoCompatibleStringBuilder(context, postContent, parser);

            MediaPredicate predicate = MediaPredicate.getLocalMediaIdPredicate(localMediaId);

            // remove the uploading class
            Attributes firstElementAttributes = getFirstElementAttributes(builder, predicate);
            // let's make sure the element is still there within the content. Sometimes it may happen
            // this method is called but the element doesn't exist in the post content anymore
            if (firstElementAttributes != null) {
                AttributesWithClass attributesWithClass = getAttributesWithClass(
                        firstElementAttributes);
                attributesWithClass.removeClass(ATTR_STATUS_UPLOADING);
                if (mediaFile.isVideo()) {
                    attributesWithClass.removeClass(TEMP_VIDEO_UPLOADING_CLASS);
                }

                // mark failed
                attributesWithClass.addClass(ATTR_STATUS_FAILED);

                updateElementAttributes(builder, predicate, attributesWithClass.getAttributes());

                // re-set the post content
                postContent = toHtml(builder, parser);
            }
        }
        return postContent;
    }

    public static boolean isMediaInPostBody(Context context, @NonNull String postContent,
                                            String localMediaId) {
        // fill in Aztec with the post's content
        AztecParser parser = getAztecParserWithPlugins();
        SpannableStringBuilder builder = getCalypsoCompatibleStringBuilder(context, postContent, parser);

        MediaPredicate predicate = MediaPredicate.getLocalMediaIdPredicate(localMediaId);

        Attributes firstElementAttributes = getFirstElementAttributes(builder, predicate);
        // is this media item not there anymore within the Post's content?
        return (firstElementAttributes != null);
    }

    public static boolean hasMediaItemsMarkedUploading(Context context, @NonNull String postContent) {
        return hasMediaItemsMarkedWithTag(context, postContent, ATTR_STATUS_UPLOADING);
    }

    public static boolean hasMediaItemsMarkedFailed(Context context, @NonNull String postContent) {
        return hasMediaItemsMarkedWithTag(context, postContent, ATTR_STATUS_FAILED);
    }

    private static boolean hasMediaItemsMarkedWithTag(Context context, @NonNull String postContent, String tag) {
        // fill in Aztec with the post's content
        AztecParser parser = getAztecParserWithPlugins();
        Spanned content = parseContent(context, parser, postContent);

        // get all items with the class in the "tag" param
        AztecText.AttributePredicate uploadingPredicate = getPredicateWithClass(tag);


        return getFirstElementAttributes(content, uploadingPredicate) != null;
    }

    public static String resetUploadingMediaToFailed(Context context, @NonNull String postContent) {
        // fill in Aztec with the post's content
        AztecParser parser = getAztecParserWithPlugins();
        SpannableStringBuilder builder = getCalypsoCompatibleStringBuilder(context, postContent, parser);

        // get all items with "failed" class, and make sure they are still failed
        // i.e. if they have a local src, then they are failed.
        resetMediaWithStatus(builder, ATTR_STATUS_FAILED);
        // get all items with "uploading" class, and make sure they are either already uploaded
        // (that is, they have a remote src), and mark them "failed" if not.
        resetMediaWithStatus(builder, ATTR_STATUS_UPLOADING);

        // re-set the post content
        postContent = toHtml(builder, parser);
        return postContent;
    }

    public static List<String> getMediaMarkedUploadingInPostContent(Context context, @NonNull String postContent) {
        return getMediaMarkedAsClassInPostContent(context, postContent, ATTR_STATUS_UPLOADING);
    }

    public static List<String> getMediaMarkedFailedInPostContent(Context context, @NonNull String postContent) {
        return getMediaMarkedAsClassInPostContent(context, postContent, ATTR_STATUS_FAILED);
    }

    private static List<String> getMediaMarkedAsClassInPostContent(Context context, @NonNull String postContent,
                                                                   String classToUse) {
        ArrayList<String> mediaMarkedUploading = new ArrayList<>();
        // fill in Aztec with the post's content
        AztecParser parser = getAztecParserWithPlugins();
        Spanned content = parseContent(context, parser, postContent);
        AztecText.AttributePredicate uploadingPredicate = getPredicateWithClass(classToUse);
        for (Attributes attrs : getAllElementAttributes(content, uploadingPredicate)) {
            String itemId = attrs.getValue(ATTR_ID_WP);
            if (!TextUtils.isEmpty(itemId)) {
                mediaMarkedUploading.add(itemId);
            }
        }
        return mediaMarkedUploading;
    }

    public static Spanned parseContent(Context context, AztecParser parser, @NonNull String postContent) {
        // parsing is an expensive operation (especially if the content is big) so, return previous result if matching.
        if (sParsedContentCached != null && postContent.hashCode() == sPostContentHash) {
            return new SpannableString(sParsedContentCached);
        }

        // cache the post's content hash to compare next time
        sPostContentHash = postContent.hashCode();

        // fill in Aztec with the post's content
        sParsedContentCached = parser.parseHtmlForInspection(postContent, context);
        return sParsedContentCached;
    }

    public void setMediaToFailed(@NonNull String mediaId) {
        AztecText.AttributePredicate localMediaIdPredicate = MediaPredicate.getLocalMediaIdPredicate(mediaId);
        // we should be obtaining just one span for this media Id predicate, but just in case something
        // weird happened we make sure we run through all obtained spans and mark them failed if a local src found
        List<IAztecAttributedSpan> spans = getSpansForPredicate(mContent.getText(), localMediaIdPredicate, false);
        for (IAztecAttributedSpan span : spans) {
            clearMediaUploadingAndSetToFailedIfLocal(span);
        }
        mContent.clearOverlays(localMediaIdPredicate);
        overlayFailedMedia(mediaId, mContent.getElementAttributes(localMediaIdPredicate));
        safeAddMediaIdToSet(mFailedMediaIds, mediaId);
        mContent.resetAttributedMediaSpan(localMediaIdPredicate);
    }

    private static void resetMediaWithStatus(Spanned content, String status) {
        // get all items with class defined by the "status" variable
        AztecText.AttributePredicate statusPredicate = getPredicateWithClass(status);

        // update all items to failed, unless they already have a remote URL, in which case
        // it means the upload completed, but the item remained inconsistently marked as uploading
        // (for example after an app crash)
        for (IAztecAttributedSpan span : getSpansForPredicate(content, statusPredicate, false)) {
            clearMediaUploadingAndSetToFailedIfLocal(span);
        }
    }

    public static String restartFailedMediaToUploading(Context context, String postContent) {
        // fill in Aztec with the post's content
        AztecParser parser = getAztecParserWithPlugins();
        SpannableStringBuilder builder = getCalypsoCompatibleStringBuilder(context, postContent, parser);

        // get all items with class defined by the "status" variable
        AztecText.AttributePredicate statusPredicate = getPredicateWithClass(ATTR_STATUS_FAILED);

        // update all these items to UPLOADING
        for (IAztecAttributedSpan span : getSpansForPredicate(builder, statusPredicate, false)) {
            AttributesWithClass attributesWithClass = getAttributesWithClass(span.getAttributes());
            attributesWithClass.removeClass(ATTR_STATUS_FAILED);
            attributesWithClass.addClass(ATTR_STATUS_UPLOADING);
            span.setAttributes(attributesWithClass.getAttributes());
        }

        // re-set the post content
        postContent = toHtml(builder, parser);
        return postContent;
    }

    private static void clearMediaUploadingAndSetToFailedIfLocal(IAztecAttributedSpan span) {
        // remove the uploading class
        AttributesWithClass attributesWithClass = getAttributesWithClass(span.getAttributes());
        attributesWithClass.removeClass(ATTR_STATUS_UPLOADING);

        attributesWithClass = addFailedStatusToMediaIfLocalSrcPresent(attributesWithClass);

        span.setAttributes(attributesWithClass.getAttributes());
    }

    private static AttributesWithClass addFailedStatusToMediaIfLocalSrcPresent(
            AttributesWithClass attributesWithClass) {
        // check if "src" value is remote or local, it only makes sense to mark failed local files
        AztecAttributes attrsOneItem = attributesWithClass.getAttributes();
        String mediaPath = attrsOneItem.getValue("src");
        if (!TextUtils.isEmpty(mediaPath) && URLUtil.isNetworkUrl(mediaPath)) {
            // it's already been uploaded! we have an http remoteUrl in the src attribute
            attributesWithClass.removeClass(ATTR_STATUS_FAILED);
        } else {
            attributesWithClass.addClass(ATTR_STATUS_FAILED);
        }

        return attributesWithClass;
    }

    private static AztecParser getAztecParserWithPlugins() {
        List<IAztecPlugin> plugins = new ArrayList<>();
        plugins.add(new CaptionShortcodePlugin());
        plugins.add(new VideoShortcodePlugin());
        plugins.add(new AudioShortcodePlugin());
        return new AztecParser(AlignmentRendering.SPAN_LEVEL, plugins);
    }

    private Drawable getLoadingImagePlaceholder() {
        if (mLoadingImagePlaceholder != null) {
            return mLoadingImagePlaceholder;
        }

        // Use default loading placeholder if none was set by the host activity
        Drawable defaultLoadingImagePlaceholder = getResources().getDrawable(R.drawable.ic_gridicons_image);
        defaultLoadingImagePlaceholder.setBounds(0, 0, DEFAULT_MEDIA_PLACEHOLDER_DIMENSION_DP,
                DEFAULT_MEDIA_PLACEHOLDER_DIMENSION_DP);
        return defaultLoadingImagePlaceholder;
    }

    private Drawable getLoadingVideoPlaceholder() {
        if (mLoadingVideoPlaceholder != null) {
            return mLoadingVideoPlaceholder;
        }

        // Use default loading placeholder if none was set by the host activity
        Drawable defaultLoadingImagePlaceholder = getResources().getDrawable(R.drawable.ic_gridicons_video_camera);
        defaultLoadingImagePlaceholder.setBounds(0, 0, DEFAULT_MEDIA_PLACEHOLDER_DIMENSION_DP,
                DEFAULT_MEDIA_PLACEHOLDER_DIMENSION_DP);
        return defaultLoadingImagePlaceholder;
    }

    public void setLoadingImagePlaceholder(Drawable loadingImagePlaceholder) {
        this.mLoadingImagePlaceholder = loadingImagePlaceholder;
    }

    public void setLoadingVideoPlaceholder(Drawable loadingVideoPlaceholder) {
        this.mLoadingVideoPlaceholder = loadingVideoPlaceholder;
    }

    public int getMaxMediaSize() {
        return maxMediaSize;
    }

    public void enableContentLogOnCrashes(AztecExceptionHandler.ExceptionHandlerHelper helper) {
        mContent.enableCrashLogging(helper);
    }

    public void disableContentLogOnCrashes() {
        mContent.disableCrashLogging();
    }

    public void setExternalLogger(AztecLog.ExternalLogger logger) {
        mContent.setExternalLogger(logger);
    }

    public void disableHWAcceleration() {
        mFragmentView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
}
