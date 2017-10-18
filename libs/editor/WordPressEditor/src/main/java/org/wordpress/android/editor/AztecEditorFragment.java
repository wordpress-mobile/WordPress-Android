package org.wordpress.android.editor;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.editor.MetadataUtils.AttributesWithClass;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;
import org.wordpress.aztec.Aztec;
import org.wordpress.aztec.AztecAttributes;
import org.wordpress.aztec.AztecParser;
import org.wordpress.aztec.AztecText;
import org.wordpress.aztec.AztecTextFormat;
import org.wordpress.aztec.Html;
import org.wordpress.aztec.IHistoryListener;
import org.wordpress.aztec.ITextFormat;
import org.wordpress.aztec.plugins.IAztecPlugin;
import org.wordpress.aztec.plugins.shortcodes.AudioShortcodePlugin;
import org.wordpress.aztec.plugins.shortcodes.CaptionShortcodePlugin;
import org.wordpress.aztec.plugins.shortcodes.VideoShortcodePlugin;
import org.wordpress.aztec.plugins.shortcodes.handlers.CaptionHandler;
import org.wordpress.aztec.plugins.wpcomments.CommentsTextFormat;
import org.wordpress.aztec.plugins.wpcomments.WordPressCommentsPlugin;
import org.wordpress.aztec.plugins.wpcomments.toolbar.MoreToolbarButton;
import org.wordpress.aztec.source.SourceViewEditText;
import org.wordpress.aztec.spans.AztecMediaSpan;
import org.wordpress.aztec.spans.IAztecAttributedSpan;
import org.wordpress.aztec.toolbar.AztecToolbar;
import org.wordpress.aztec.toolbar.IAztecToolbarClickListener;
import org.wordpress.aztec.watchers.BlockElementWatcher;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AztecEditorFragment extends EditorFragmentAbstract implements
        AztecText.OnImeBackListener,
        AztecText.OnImageTappedListener,
        AztecText.OnVideoTappedListener,
        AztecText.OnMediaDeletedListener,
        View.OnTouchListener,
        EditorMediaUploadListener,
        IAztecToolbarClickListener,
        IHistoryListener {

    private static final String ATTR_ALIGN_DASH = "align-";
    private static final String ATTR_CLASS = "class";
    private static final String ATTR_ID_WP = "data-wpid";
    private static final String ATTR_IMAGE_WP_DASH = "wp-image-";
    private static final String ATTR_SIZE = "size";
    private static final String ATTR_SIZE_DASH = "size-";
    private static final String TEMP_IMAGE_ID = "data-temp-aztec-id";
    private static final String TEMP_VIDEO_UPLOADING_CLASS = "data-temp-aztec-video";

    private static final int MIN_BITMAP_DIMENSION_DP = 48;
    public static final int DEFAULT_MEDIA_PLACEHOLDER_DIMENSION_DP = 196;

    private static final int MAX_ACTION_TIME_MS = 2000;

    private static final MediaFile DEFAULT_MEDIA = new MediaFile();
    private static final int DEFAULT_MEDIA_HEIGHT = DEFAULT_MEDIA.getHeight();
    private static final int DEFAULT_MEDIA_WIDTH = DEFAULT_MEDIA.getWidth();

    private static final List<String> DRAGNDROP_SUPPORTED_MIMETYPES_TEXT = Arrays.asList(ClipDescription
            .MIMETYPE_TEXT_PLAIN, ClipDescription.MIMETYPE_TEXT_HTML);
    private static final List<String> DRAGNDROP_SUPPORTED_MIMETYPES_IMAGE = Arrays.asList("image/jpeg", "image/png");

    private static boolean mIsToolbarExpanded = false;

    private boolean mEditorWasPaused = false;
    private boolean mHideActionBarOnSoftKeyboardUp = false;

    private AztecText title;
    private AztecText content;
    private boolean mAztecReady;
    private SourceViewEditText source;
    private AztecToolbar formattingToolbar;
    private Html.ImageGetter aztecImageLoader;
    private Html.VideoThumbnailGetter aztecVideoLoader;

    private Handler invalidateOptionsHandler;
    private Runnable invalidateOptionsRunnable;

    private HashMap<String, Float> mUploadingMediaProgressMax;
    private Set<String> mFailedMediaIds;

    private long mActionStartedAt = -1;

    private MediaPredicate mTappedMediaPredicate;

    private EditorBetaClickListener mEditorBetaClickListener;

    private Drawable loadingImagePlaceholder;
    private Drawable loadingVideoPlaceholder;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_aztec_editor, container, false);

        // request dependency injection
        if (getActivity() instanceof EditorFragmentActivity) {
            ((EditorFragmentActivity)getActivity()).initializeEditorFragment();
        }

        mUploadingMediaProgressMax = new HashMap<>();
        mFailedMediaIds = new HashSet<>();

        title = (AztecText) view.findViewById(R.id.title);
        content = (AztecText)view.findViewById(R.id.aztec);
        source = (SourceViewEditText) view.findViewById(R.id.source);

        title.setOnTouchListener(this);
        content.setOnTouchListener(this);
        source.setOnTouchListener(this);

        title.setOnImeBackListener(this);
        content.setOnImeBackListener(this);
        source.setOnImeBackListener(this);

        source.setHint("<p>" + getString(R.string.editor_content_hint) + "</p>");

        formattingToolbar = (AztecToolbar) view.findViewById(R.id.formatting_toolbar);
        formattingToolbar.setExpanded(mIsToolbarExpanded);

        title.setOnFocusChangeListener(
                new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean hasFocus) {
                        formattingToolbar.enableFormatButtons(!hasFocus);
                    }
                }
        );

        content.setOnDragListener(mOnDragListener);
        source.setOnDragListener(mOnDragListener);

        setHasOptionsMenu(true);

        invalidateOptionsHandler = new Handler();
        invalidateOptionsRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAdded()) {
                    getActivity().invalidateOptionsMenu();
                }
            }
        };

        content.refreshText();

        mAztecReady = true;

        ImageButton titleBeta = (ImageButton) view.findViewById(R.id.title_beta);
        titleBeta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEditorBetaClickListener.onBetaClicked();
            }
        });

        Aztec.Factory.with(content, source, formattingToolbar, this)
                .setImageGetter(aztecImageLoader)
                .setVideoThumbnailGetter(aztecVideoLoader)
                .setOnImeBackListener(this)
                .setHistoryListener(this)
                .setOnImageTappedListener(this)
                .setOnVideoTappedListener(this)
                .setOnMediaDeletedListener(this)
                .addPlugin(new WordPressCommentsPlugin(content))
                .addPlugin(new MoreToolbarButton(content))
                .addPlugin(new CaptionShortcodePlugin())
                .addPlugin(new VideoShortcodePlugin())
                .addPlugin(new AudioShortcodePlugin());

        new BlockElementWatcher(content)
                .add(new CaptionHandler())
                .install(content);

        mEditorFragmentListener.onEditorFragmentInitialized();

        return view;
    }

    public void setEditorBetaClickListener(EditorBetaClickListener listener) {
        mEditorBetaClickListener = listener;
    }

    public void setAztecImageLoader(Html.ImageGetter imageLoader) {
        this.aztecImageLoader = imageLoader;
    }

    public void setAztecVideoLoader(Html.VideoThumbnailGetter videoLoader) {
        this.aztecVideoLoader = videoLoader;
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
        outState.putCharSequence(ATTR_TITLE, getTitle());
        outState.putCharSequence(ATTR_CONTENT, getContent());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_aztec, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // TODO: disable undo/redo in media mode
        boolean canRedo = content.history.redoValid();
        boolean canUndo = content.history.undoValid();

        if (menu != null && menu.findItem(R.id.redo) != null) {
            menu.findItem(R.id.redo).setEnabled(canRedo);
        }

        if (menu != null && menu.findItem(R.id.undo) != null) {
            menu.findItem(R.id.undo).setEnabled(canUndo);
        }

        super.onPrepareOptionsMenu(menu);
    }

    public boolean hasHistory() {
        return (content.history.getHistoryEnabled() && !content.history.getHistoryList().isEmpty());
    }

    public boolean isHistoryEnabled() {
        return content.history.getHistoryEnabled();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.undo) {
            if (content.getVisibility() == View.VISIBLE) {
                content.undo();
                mEditorFragmentListener.onUndoMediaCheck(content.toHtml(false));
            } else {
                source.undo();
            }
            return true;
        } else if (item.getItemId() == R.id.redo) {
            if (content.getVisibility() == View.VISIBLE) {
                content.redo();
            } else {
                source.redo();
            }
            return true;
        }

        return false;
    }

    @Override
    public void onRedoEnabled() {
        invalidateOptionsHandler.removeCallbacks(invalidateOptionsRunnable);
        invalidateOptionsHandler.postDelayed(invalidateOptionsRunnable, getResources().getInteger(android.R.integer.config_mediumAnimTime) );
    }

    @Override
    public void onUndoEnabled() {
        invalidateOptionsHandler.removeCallbacks(invalidateOptionsRunnable);
        invalidateOptionsHandler.postDelayed(invalidateOptionsRunnable, getResources().getInteger(android.R.integer.config_mediumAnimTime) );
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

        if (title == null) {
            return;
        }
        title.setText(text);
    }

    @Override
    public void setContent(CharSequence text) {
        if (text == null) {
            text = "";
        }

        if (content == null) {
            return;
        }

        content.fromHtml(text.toString());

        updateFailedMediaList();
        overlayFailedMedia();

        updateUploadingMediaList();
        overlayProgressingMedia();

        mAztecReady = true;
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
        return StringUtils.notNullStr(title.getText().toString().replaceAll("&nbsp;$", "").replaceAll("\u200B", ""));
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
        } else if (format.equals(AztecTextFormat.FORMAT_BOLD)) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.BOLD_BUTTON_TAPPED);
        } else if (format.equals(AztecTextFormat.FORMAT_ITALIC)) {
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
            new AlertDialog.Builder(getActivity())
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

    private void toggleHtmlMode() {
        mEditorFragmentListener.onTrackableEvent(TrackableEvent.HTML_BUTTON_TAPPED);

        // Don't switch to HTML mode if currently uploading media
        if (!mUploadingMediaProgressMax.isEmpty() || isActionInProgress()) {
            ToastUtils.showToast(getActivity(), R.string.alert_action_while_uploading, ToastUtils.Duration.LONG);
            return;
        }

        formattingToolbar.toggleEditorMode();

        if (source.getVisibility() == View.VISIBLE) {
            updateFailedMediaList();
        }
    }

    public void enableMediaMode(boolean enable) {
        formattingToolbar.enableMediaMode(enable);
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
        for (Attributes attrs : content.getAllElementAttributes(uploadingPredicate)) {
            String localMediaId = attrs.getValue(ATTR_ID_WP);
            if (!TextUtils.isEmpty(localMediaId)) {
                mUploadingMediaProgressMax.put(localMediaId, 0f);
            }
        }
    }

    private void safeAddMediaIdToSet(Set<String> setToAddTo, String wpId){
        if (!TextUtils.isEmpty(wpId)) {
            setToAddTo.add(wpId);
        }
    }

    static private AztecText.AttributePredicate getPredicateWithClass(final String classToUse) {
        AztecText.AttributePredicate predicate = new AztecText.AttributePredicate() {
            @Override
            public boolean matches(@NonNull Attributes attrs) {
                AttributesWithClass attributesWithClass = getAttributesWithClass(attrs);
                return attributesWithClass.hasClass(classToUse);
            }
        };

        return predicate;
    }

    static private AttributesWithClass getAttributesWithClass(@NonNull Attributes attrs) {
        return new AttributesWithClass(attrs);
    }

    private void updateFailedMediaList() {
        AztecText.AttributePredicate failedPredicate = getPredicateWithClass(ATTR_STATUS_FAILED);

        mFailedMediaIds.clear();

        for (Attributes attrs : content.getAllElementAttributes(failedPredicate)) {
            String localMediaId = attrs.getValue(ATTR_ID_WP);
            safeAddMediaIdToSet(mFailedMediaIds, localMediaId);
        }
    }

    private void overlayProgressingMedia() {
        for (String localMediaId : mUploadingMediaProgressMax.keySet()) {
            overlayProgressingMediaForMediaId(localMediaId);
        }
    }

    private void overlayProgressingMediaForMediaId(String localMediaId) {
        MediaPredicate predicate = MediaPredicate.getLocalMediaIdPredicate(localMediaId);
        overlayProgressingMedia(predicate);
        // here check if this is a video uploading in progress or not; if it is, show the video play icon
        for (Attributes attrs : content.getAllElementAttributes(predicate)) {
            AttributesWithClass attributesWithClass = getAttributesWithClass(attrs);
            if (attributesWithClass.hasClass(TEMP_VIDEO_UPLOADING_CLASS)) {
                overlayVideoIcon(2, predicate);
            }
        }
    }


    private void overlayVideoIcon(int overlayLevel, AztecText.AttributePredicate predicate) {
        Drawable videoDrawable = getResources().getDrawable(R.drawable.ic_overlay_video);
        content.setOverlay(predicate, overlayLevel, videoDrawable, Gravity.BOTTOM | Gravity.START);
    }

    private void overlayProgressingMedia(MediaPredicate predicate) {
        // set intermediate shade overlay
        content.setOverlay(predicate, 0,
                new ColorDrawable(getResources().getColor(R.color.media_shade_overlay_color)),
                Gravity.FILL);

        Drawable progressDrawable = getResources().getDrawable(android.R.drawable.progress_horizontal);
        // set the height of the progress bar to 2 (it's in dp since the drawable will be adjusted by the span)
        progressDrawable.setBounds(0, 0, 0, 4);

        content.setOverlay(predicate, 1, progressDrawable,
                Gravity.FILL_HORIZONTAL | Gravity.TOP);

        content.resetAttributedMediaSpan(predicate);
    }

    private void overlayFailedMedia() {
        for (String localMediaId : mFailedMediaIds) {
            Attributes attributes = content.getElementAttributes(MediaPredicate.getLocalMediaIdPredicate(localMediaId));
            overlayFailedMedia(localMediaId, attributes);
        }
    }

    private void overlayFailedMedia(String localMediaId, Attributes attributes) {
        // set intermediate shade overlay
        AztecText.AttributePredicate localMediaIdPredicate = MediaPredicate.getLocalMediaIdPredicate(localMediaId);
        content.setOverlay(localMediaIdPredicate, 0,
                new ColorDrawable(getResources().getColor(R.color.media_shade_overlay_error_color)),
                Gravity.FILL);

        Drawable alertDrawable = getResources().getDrawable(R.drawable.media_retry_image);
        content.setOverlay(localMediaIdPredicate, 1, alertDrawable, Gravity.CENTER);
        content.updateElementAttributes(localMediaIdPredicate, new AztecAttributes(attributes));
    }

    /**
     * Returns the contents of the content field from the JavaScript editor. Should be called from a background thread
     * where possible.
     */
    @Override
    public CharSequence getContent() {
        if (!isAdded()) {
            return "";
        }

        if (content.getVisibility() == View.VISIBLE) {
            return content.toHtml(false);
        } else {
            return source.getPureHtml(false);
        }
    }

    @Override
    public void appendMediaFile(final MediaFile mediaFile, final String mediaUrl, ImageLoader imageLoader) {
        // load a scaled version of the image to prevent OOM exception
        final int maxWidth = ImageUtils.getMaximumThumbnailWidthForEditor(getActivity());

        if (URLUtil.isNetworkUrl(mediaUrl)) {


            AztecAttributes attributes = new AztecAttributes();
            attributes.setValue(ATTR_SRC, mediaUrl);
            setAttributeValuesIfNotDefault(attributes, mediaFile);
            if(mediaFile.isVideo()) {
                addVideoUploadingClassIfMissing(attributes);
                content.insertVideo(getLoadingVideoPlaceholder(), attributes);
                overlayVideoIcon(0, new MediaPredicate(mediaUrl, ATTR_SRC));
            } else {
                content.insertImage(getLoadingImagePlaceholder(), attributes);
            }

            final String posterURL = mediaFile.isVideo() ? Utils.escapeQuotes(StringUtils.notNullStr(mediaFile.getThumbnailURL())) : mediaUrl;
            imageLoader.get(posterURL, new ImageLoader.ImageListener() {

                private void replaceDrawable(Drawable newDrawable){
                    AztecMediaSpan[] imageOrVideoSpans = content.getText().getSpans(0, content.getText().length(), AztecMediaSpan.class);
                    for (AztecMediaSpan currentClass: imageOrVideoSpans) {
                        if (currentClass.getAttributes().hasAttribute(ATTR_SRC) &&
                                mediaUrl.equals(currentClass.getAttributes().getValue(ATTR_SRC))) {
                            currentClass.setDrawable(newDrawable);
                        }
                    }
                    content.refreshText();
                }

                private void showErrorPlaceholder() {
                    // Show failed placeholder.
                    ToastUtils.showToast(getActivity(), R.string.error_media_load);
                    Drawable drawable = getResources().getDrawable(R.drawable.ic_image_failed_grey_a_40_48dp);
                    replaceDrawable(drawable);
                }

                @Override
                public void onErrorResponse(VolleyError error) {
                    if (!isAdded()) {
                        // the fragment is detached
                        return;
                    }
                    showErrorPlaceholder();
                }

                @Override
                public void onResponse(ImageLoader.ImageContainer container, boolean isImmediate) {
                    if (!isAdded()) {
                        // the fragment is detached
                        return;
                    }
                    Bitmap downloadedBitmap = container.getBitmap();
                    if (downloadedBitmap == null) {
                        if (isImmediate) {
                            // Bitmap is null but isImmediate is true (as soon as the request starts).
                            return;
                        }
                        showErrorPlaceholder();
                        return;
                    }

                    AztecAttributes attributes = new AztecAttributes();
                    attributes.setValue(ATTR_SRC, mediaUrl);
                    setAttributeValuesIfNotDefault(attributes, mediaFile);

                    int minimumDimension = DisplayUtils.dpToPx(getActivity(), MIN_BITMAP_DIMENSION_DP);

                    if (downloadedBitmap.getHeight() < minimumDimension || downloadedBitmap.getWidth() < minimumDimension) {
                        // Bitmap is too small.  Show image placeholder.
                        ToastUtils.showToast(getActivity(), R.string.error_media_small);
                        Drawable drawable = getResources().getDrawable(R.drawable.ic_image_loading_grey_a_40_48dp);
                        replaceDrawable(drawable);
                        return;
                    }

                    Bitmap resizedBitmap = ImageUtils.getScaledBitmapAtLongestSide(downloadedBitmap, maxWidth);
                    replaceDrawable(new BitmapDrawable(getResources(), resizedBitmap));
                }
            }, maxWidth, 0);

            mActionStartedAt = System.currentTimeMillis();
        } else {
            String localMediaId = String.valueOf(mediaFile.getId());
            final String safeMediaPreviewUrl = mediaFile.isVideo() ?
                    Utils.escapeQuotes(StringUtils.notNullStr(mediaFile.getThumbnailURL())) :
                    Utils.escapeQuotes(mediaUrl);

            AztecAttributes attrs = new AztecAttributes();
            attrs.setValue(ATTR_ID_WP, localMediaId);
            attrs.setValue(ATTR_SRC, Utils.escapeQuotes(mediaUrl));
            attrs.setValue(ATTR_CLASS, ATTR_STATUS_UPLOADING);

            addDefaultSizeClassIfMissing(attrs);

            Bitmap bitmapToShow = ImageUtils.getWPImageSpanThumbnailFromFilePath(getActivity(), safeMediaPreviewUrl, maxWidth);
            MediaPredicate localMediaIdPredicate = MediaPredicate.getLocalMediaIdPredicate(localMediaId);

            if (bitmapToShow != null) {
                if(mediaFile.isVideo()) {
                    addVideoUploadingClassIfMissing(attrs);
                    content.insertVideo(new BitmapDrawable(getResources(), bitmapToShow), attrs);
                } else {
                    content.insertImage(new BitmapDrawable(getResources(), bitmapToShow), attrs);
                }
            } else {
                // Failed to retrieve bitmap.  Show failed placeholder.
                ToastUtils.showToast(getActivity(), R.string.error_media_load);
                Drawable drawable = getResources().getDrawable(R.drawable.ic_image_failed_grey_a_40_48dp);
                drawable.setBounds(0, 0, maxWidth, maxWidth);
                content.insertImage(drawable, attrs);
            }

            // set intermediate shade overlay
            overlayProgressingMedia(localMediaIdPredicate);

            mUploadingMediaProgressMax.put(localMediaId, 0f);

            if (mediaFile.isVideo()) {
                overlayVideoIcon(2, localMediaIdPredicate);
            }

            content.updateElementAttributes(localMediaIdPredicate, attrs);

            content.resetAttributedMediaSpan(localMediaIdPredicate);

        }
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
        content.getText().insert(content.getSelectionEnd(), shortcode);
    }

    @Override
    public void setUrlForVideoPressId(final String videoId, final String videoUrl, final String posterUrl) {
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
        content.removeMedia(new AztecText.AttributePredicate() {
            @Override
            public boolean matches(@NonNull Attributes attrs) {
                return getAttributesWithClass(attrs).hasClass(ATTR_STATUS_FAILED);
            }
        });
        mFailedMediaIds.clear();
    }

    @Override
    public void removeMedia(String mediaId) {
        content.removeMedia(MediaPredicate.getLocalMediaIdPredicate(mediaId));
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
        mUploadingMediaProgressMax.put(localId, currentProgress);
        overlayProgressingMediaForMediaId(localId);
    }

    @Override
    public void onMediaUploadSucceeded(final String localMediaId, final MediaFile mediaFile) {
        if (!isAdded() || content == null || !mAztecReady) {
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

                // remove the uploading class
                AttributesWithClass attributesWithClass = getAttributesWithClass(
                        content.getElementAttributes(predicate));
                attributesWithClass.removeClass(ATTR_STATUS_UPLOADING);
                if (mediaFile.isVideo()) {
                    attributesWithClass.removeClass(TEMP_VIDEO_UPLOADING_CLASS);
                }

                // add then new src property with the remoteUrl
                AztecAttributes attrs = attributesWithClass.getAttributes();
                attrs.setValue("src", remoteUrl);

                /* TODO add video press attribute -> value here
                if (mediaType.equals(MediaType.VIDEO)) {
                    String videoPressId = ShortcodeUtils.getVideoPressIdFromShortCode(
                            mediaFile.getVideoPressShortCode());
                    attrs.setValue( ?? , videoPressId);
                }
                */

                addDefaultSizeClassIfMissing(attrs);

                // clear overlay
                content.clearOverlays(predicate);
                if (mediaType.equals(MediaType.VIDEO)) {
                    overlayVideoIcon(0, predicate);
                }
                content.updateElementAttributes(predicate, attrs);
                content.resetAttributedMediaSpan(predicate);

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

    private static class MediaPredicate implements AztecText.AttributePredicate {
        private final String mId;
        private final String mAttributeName;

        static MediaPredicate getLocalMediaIdPredicate(String id) {
            return new MediaPredicate(id, ATTR_ID_WP);
        }

        static MediaPredicate getTempMediaIdPredicate(String id) {
            return new MediaPredicate(id, TEMP_IMAGE_ID);
        }

        MediaPredicate(String id, String attributeName) {
            mId = id;
            mAttributeName = attributeName;
        }

        @Override
        public boolean matches(@NonNull Attributes attrs) {
            return attrs.getIndex(mAttributeName) > -1 && attrs.getValue(mAttributeName).equals(mId);
        }
    }

    @Override
    public void onMediaUploadProgress(final String localMediaId, final float progress) {
        if(!isAdded() || content == null || !mAztecReady || TextUtils.isEmpty(localMediaId)) {
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
                    AztecText.AttributePredicate localMediaIdPredicate = MediaPredicate.getLocalMediaIdPredicate(localMediaId);
                    content.setOverlayLevel(localMediaIdPredicate, 1, (int) (progress * 10000));
                    content.resetAttributedMediaSpan(localMediaIdPredicate);
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
        if(!isAdded() || content == null) {
            return;
        }
        if (mediaType != null) {
            switch (mediaType) {
                case IMAGE:
                case VIDEO:
                    MediaPredicate localMediaIdPredicate = MediaPredicate.getLocalMediaIdPredicate(localMediaId);
                    AttributesWithClass attributesWithClass = getAttributesWithClass(
                            content.getElementAttributes(localMediaIdPredicate));

                    attributesWithClass.removeClass(ATTR_STATUS_UPLOADING);
                    attributesWithClass.addClass(ATTR_STATUS_FAILED);

                    content.clearOverlays(localMediaIdPredicate);
                    overlayFailedMedia(localMediaId, attributesWithClass.getAttributes());
                    content.resetAttributedMediaSpan(localMediaIdPredicate);
                    break;
            }
            mFailedMediaIds.add(localMediaId);
            mUploadingMediaProgressMax.remove(localMediaId);
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
     *   - a hardware keyboard is connected.
     *   - the soft keyboard is not visible.
     *   - it's not visible.
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
                    return isSupported(dragEvent.getClipDescription(), DRAGNDROP_SUPPORTED_MIMETYPES_TEXT) ||
                            isSupported(dragEvent.getClipDescription(), DRAGNDROP_SUPPORTED_MIMETYPES_IMAGE);
                case DragEvent.ACTION_DRAG_ENTERED:
                    // would be nice to start marking the place the item will drop
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                    int x = DisplayUtils.pxToDp(getActivity(), (int) dragEvent.getX());
                    int y = DisplayUtils.pxToDp(getActivity(), (int) dragEvent.getY());

                    content.setSelection(content.getOffsetForPosition(x, y));
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    // clear any drop marking maybe
                    break;
                case DragEvent.ACTION_DROP:
                    if (source.getVisibility() == View.VISIBLE) {
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

                    if (isSupported(dragEvent.getClipDescription(), DRAGNDROP_SUPPORTED_MIMETYPES_IMAGE) &&
                            isTitleFocused()) {
                        // don't allow dropping images into the title field
                        ToastUtils.showToast(getActivity(), R.string.editor_dropped_title_images_not_allowed,
                                ToastUtils.Duration.LONG);
                        return true;
                    }

                    if (isAdded()) {
                        mEditorDragAndDropListener.onRequestDragAndDropPermissions(dragEvent);
                    }

                    ClipDescription clipDescription = dragEvent.getClipDescription();
                    if (clipDescription.getMimeTypeCount() < 1) {
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
                content.getText().insert(content.getSelectionStart(), reformatVisually(Utils.escapeHtml(text)));
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
            return title.isFocused();
        }
    };

    @Override
    public void onToolbarMediaButtonClicked() {
        mEditorFragmentListener.onTrackableEvent(TrackableEvent.MEDIA_BUTTON_TAPPED);

        if (isActionInProgress()) {
            ToastUtils.showToast(getActivity(), R.string.alert_action_while_uploading, ToastUtils.Duration.LONG);
            return;
        }

        if (source.isFocused()) {
            ToastUtils.showToast(getActivity(), R.string.alert_insert_image_html_mode, ToastUtils.Duration.LONG);
        } else {
            mEditorFragmentListener.onAddMediaClicked();
        }
    }

    @Override
    public void onImageTapped(@NonNull AztecAttributes attrs, int naturalWidth, int naturalHeight) {
        onMediaTapped(attrs, naturalWidth, naturalHeight, MediaType.IMAGE);
    }

    @Override
    public void onVideoTapped(@NonNull AztecAttributes attrs) {
        onMediaTapped(attrs, 0, 0, MediaType.VIDEO);
    }


    private void onMediaTapped(@NonNull AztecAttributes attrs, int naturalWidth, int naturalHeight, final MediaType mediaType) {
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

        attrs.setValue(idName, localMediaId);
        mTappedMediaPredicate = new MediaPredicate(localMediaId, idName);

        switch (uploadStatus) {
            case ATTR_STATUS_UPLOADING:
                // Display 'cancel upload' dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getString(R.string.stop_upload_dialog_title));
                builder.setPositiveButton(R.string.stop_upload_dialog_button_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        if (mUploadingMediaProgressMax.containsKey(localMediaId)) {
                            mEditorFragmentListener.onMediaUploadCancelClicked(localMediaId);

                            switch (mediaType) {
                                case IMAGE:
                                    content.removeMedia(mTappedMediaPredicate);
                                    break;
                                case VIDEO:
                                    content.removeMedia(mTappedMediaPredicate);
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
                                    content.getElementAttributes(mTappedMediaPredicate));

                            // remove the failed class
                            attributesWithClass = addFailedStatusToMediaIfLocalSrcPresent(attributesWithClass);

                            if (!attributesWithClass.hasClass(ATTR_STATUS_FAILED)) {
                                // just save the item and leave
                                content.clearOverlays(mTappedMediaPredicate);
                                content.resetAttributedMediaSpan(mTappedMediaPredicate);
                                return;
                            }

                            attributesWithClass.addClass(ATTR_STATUS_UPLOADING);
                            if (mediaType.equals(MediaType.VIDEO)) {
                                attributesWithClass.addClass(TEMP_VIDEO_UPLOADING_CLASS);
                            }

                            // set intermediate shade overlay
                            content.setOverlay(mTappedMediaPredicate, 0,
                                    new ColorDrawable(getResources().getColor(R.color.media_shade_overlay_color)), Gravity.FILL);

                            Drawable progressDrawable = getResources().getDrawable(android.R.drawable.progress_horizontal);
                            // set the height of the progress bar to 2 (it's in dp since the drawable will be adjusted by the span)
                            progressDrawable.setBounds(0, 0, 0, 4);

                            content.setOverlay(mTappedMediaPredicate, 1, progressDrawable, Gravity.FILL_HORIZONTAL | Gravity.TOP);
                            content.updateElementAttributes(mTappedMediaPredicate, attributesWithClass.getAttributes());

                            if (mediaType.equals(MediaType.VIDEO)) {
                                overlayVideoIcon(2, mTappedMediaPredicate);
                            }

                            content.resetAttributedMediaSpan(mTappedMediaPredicate);
                            break;
                    }
                    mFailedMediaIds.remove(localMediaId);
                    mUploadingMediaProgressMax.put(localMediaId, 0f);
                }
                break;
            default:
                if (mediaType.equals(MediaType.VIDEO)) {
                    try{
                        // Open the video preview in the default browser for now.
                        // TODO open the preview activity already available in media?
                        final String imageSrc = meta.getString(ATTR_SRC);
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(imageSrc));
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

                // Only show image options fragment for image taps
                FragmentManager fragmentManager = getFragmentManager();

                if (fragmentManager.findFragmentByTag(ImageSettingsDialogFragment.IMAGE_SETTINGS_DIALOG_TAG) != null) {
                    return;
                }
                mEditorFragmentListener.onTrackableEvent(TrackableEvent.IMAGE_EDITED);
                ImageSettingsDialogFragment imageSettingsDialogFragment = new ImageSettingsDialogFragment();
                imageSettingsDialogFragment.setImageLoader(mImageLoader);
                imageSettingsDialogFragment.setTargetFragment(this,
                        ImageSettingsDialogFragment.IMAGE_SETTINGS_DIALOG_REQUEST_CODE);

                Bundle dialogBundle = new Bundle();

                dialogBundle.putString(EXTRA_MAX_WIDTH, mBlogSettingMaxImageWidth);
                dialogBundle.putBoolean(EXTRA_IMAGE_FEATURED, mFeaturedImageSupported);
                dialogBundle.putBoolean(EXTRA_ENABLED_AZTEC, true);

                try {
                    // Use https:// when requesting the auth header, in case the image is incorrectly using http://
                    // If an auth header is returned, force https:// for the actual HTTP request
                    final String imageSrc = meta.getString(ATTR_SRC);
                    String authHeader = mEditorFragmentListener.onAuthHeaderRequested(UrlUtils.makeHttps(imageSrc));
                    if (authHeader.length() > 0) {
                        meta.put(ATTR_SRC, UrlUtils.makeHttps(imageSrc));
                    }
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.EDITOR, "Could not retrieve image url from JSON metadata");
                }

                dialogBundle.putString(EXTRA_IMAGE_META, meta.toString());

                String imageId = JSONUtils.getString(meta, ATTR_ID_ATTACHMENT);
                if (!imageId.isEmpty()) {
                    dialogBundle.putBoolean(EXTRA_FEATURED, mFeaturedImageId == Integer.parseInt(imageId));
                }

                imageSettingsDialogFragment.setArguments(dialogBundle);

                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

                fragmentTransaction.add(android.R.id.content, imageSettingsDialogFragment,
                        ImageSettingsDialogFragment.IMAGE_SETTINGS_DIALOG_TAG)
                        .addToBackStack(null)
                        .commit();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ImageSettingsDialogFragment.IMAGE_SETTINGS_DIALOG_REQUEST_CODE) {
            if (mTappedMediaPredicate != null) {
                AztecAttributes attributes = content.getElementAttributes(mTappedMediaPredicate);
                attributes.removeAttribute(TEMP_IMAGE_ID);

                content.updateElementAttributes(mTappedMediaPredicate, attributes);

                if (data == null || data.getExtras() == null) {
                    return;
                }

                Bundle extras = data.getExtras();
                JSONObject meta;

                try {
                    meta = new JSONObject(StringUtils.notNullStr(extras.getString(EXTRA_IMAGE_META)));
                } catch (JSONException e) {
                    return;
                }

                attributes.setValue(ATTR_SRC, JSONUtils.getString(meta, ATTR_SRC));

                if (!TextUtils.isEmpty(JSONUtils.getString(meta, ATTR_TITLE))) {
                    attributes.setValue(ATTR_TITLE, JSONUtils.getString(meta, ATTR_TITLE));
                }

                attributes.setValue(ATTR_DIMEN_WIDTH, JSONUtils.getString(meta, ATTR_DIMEN_WIDTH));
                attributes.setValue(ATTR_DIMEN_HEIGHT, JSONUtils.getString(meta, ATTR_DIMEN_HEIGHT));

                if (!TextUtils.isEmpty(JSONUtils.getString(meta, ATTR_ALT))) {
                    attributes.setValue(ATTR_ALT, JSONUtils.getString(meta, ATTR_ALT));
                }

                AttributesWithClass attributesWithClass = getAttributesWithClass(attributes);

                // remove previously set class attributes to add updated values
                attributesWithClass.removeClassStartingWith(ATTR_ALIGN_DASH);
                attributesWithClass.removeClassStartingWith(ATTR_SIZE_DASH);
                attributesWithClass.removeClassStartingWith(ATTR_IMAGE_WP_DASH);

                // only add align attribute if there is no caption since alignment is sent with shortcode
                if (!TextUtils.isEmpty(JSONUtils.getString(meta, ATTR_ALIGN)) &&
                        TextUtils.isEmpty(JSONUtils.getString(meta, ATTR_CAPTION))) {
                    attributesWithClass.addClass(ATTR_ALIGN_DASH + JSONUtils.getString(meta, ATTR_ALIGN));
                }

                if (!TextUtils.isEmpty(JSONUtils.getString(meta, ATTR_SIZE))) {
                    attributesWithClass.addClass(ATTR_SIZE_DASH + JSONUtils.getString(meta, ATTR_SIZE));
                }

                if (!TextUtils.isEmpty(JSONUtils.getString(meta, ATTR_ID_ATTACHMENT))) {
                    attributesWithClass.addClass(ATTR_IMAGE_WP_DASH + JSONUtils.getString(meta, ATTR_ID_ATTACHMENT));
                }

//                TODO: Add shortcode support to allow captions.
//                https://github.com/wordpress-mobile/AztecEditor-Android/issues/17
//                String caption = JSONUtils.getString(meta, ATTR_CAPTION);

//                TODO: Fix issue with image inside link.
//                https://github.com/wordpress-mobile/AztecEditor-Android/issues/196
//                String link = JSONUtils.getString(meta, ATTR_URL_LINK);

                final int imageRemoteId = extras.getInt(ATTR_ID_IMAGE_REMOTE);
                final boolean isFeaturedImage = extras.getBoolean(EXTRA_FEATURED);

                if (imageRemoteId != 0) {
                    if (isFeaturedImage) {
                        mFeaturedImageId = imageRemoteId;
                        mEditorFragmentListener.onFeaturedImageChanged(mFeaturedImageId);
                    } else {
                        // if this image was unset as featured, clear the featured image id
                        if (mFeaturedImageId == imageRemoteId) {
                            mFeaturedImageId = 0;
                            mEditorFragmentListener.onFeaturedImageChanged(mFeaturedImageId);
                        }
                    }
                }

                mTappedMediaPredicate = null;
            }
        }
    }

    private void setAttributeValuesIfNotDefault(AztecAttributes attributes, MediaFile mediaFile) {
        if (mediaFile.getWidth() != DEFAULT_MEDIA_WIDTH) {
            attributes.setValue(ATTR_DIMEN_WIDTH, String.valueOf(mediaFile.getWidth()));
        }

        if (mediaFile.getHeight() != DEFAULT_MEDIA_HEIGHT) {
            attributes.setValue(ATTR_DIMEN_HEIGHT, String.valueOf(mediaFile.getHeight()));
        }

        addDefaultSizeClassIfMissing(attributes);
    }

    private static void addDefaultSizeClassIfMissing(AztecAttributes attributes) {
        AttributesWithClass attrs = getAttributesWithClass(attributes);
        if (!attrs.hasClassStartingWith("size")) {
            attrs.addClass("size-full");
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
        }
        else {
            return null;
        }
    }

    private static @NonNull List<Attributes> getAllElementAttributes(Spanned content,
                                                                  AztecText.AttributePredicate predicate) {
        return getElementAttributes(content, predicate, false);
    }

    private static @NonNull List<Attributes> getElementAttributes(Spanned content,
                                                                  AztecText.AttributePredicate predicate,
                                                                  boolean returnFirstFoundOnly) {
        IAztecAttributedSpan[] spans = content.getSpans(0, content.length(), IAztecAttributedSpan.class);
        List<Attributes> allAttrs = new ArrayList<>();
        for (IAztecAttributedSpan span : spans) {
            if (predicate.matches(span.getAttributes())) {
                allAttrs.add(span.getAttributes());
                if (returnFirstFoundOnly) return allAttrs;
            }
        }
        return allAttrs;
    }

    private static @NonNull List<IAztecAttributedSpan> getSpansForPredicate(Spanned content,
                                                                            AztecText.AttributePredicate predicate,
                                                                            boolean returnFirstFoundOnly) {
        IAztecAttributedSpan[] spans = content.getSpans(0, content.length(), IAztecAttributedSpan.class);
        List<IAztecAttributedSpan> allMatchingSpans = new ArrayList<>();
        for (IAztecAttributedSpan span : spans) {
            if (predicate.matches(span.getAttributes())) {
                allMatchingSpans.add(span);
                if (returnFirstFoundOnly) return allMatchingSpans;
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

    public static String replaceMediaFileWithUrl(Context context, @NonNull String postContent,
                                                 String localMediaId, MediaFile mediaFile) {
        if (mediaFile != null) {
            String remoteUrl = Utils.escapeQuotes(mediaFile.getFileURL());
            // fill in Aztec with the post's content
            AztecParser parser = getAztecParserWithPlugins();
            Spanned content = parser.fromHtml(postContent, context);

            MediaPredicate predicate = MediaPredicate.getLocalMediaIdPredicate(localMediaId);

            // remove the uploading class
            Attributes firstElementAttributes = getFirstElementAttributes(content, predicate);
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

                updateElementAttributes(content, predicate, attrs);

                // re-set the post content
                postContent = parser.toHtml(content, false);
            }
        }
        return postContent;
    }

    public static String markMediaFailed(Context context, @NonNull String postContent,
                                                 String localMediaId, MediaFile mediaFile) {
        if (mediaFile != null) {
            // fill in Aztec with the post's content
            AztecParser parser = getAztecParserWithPlugins();
            Spanned content = parser.fromHtml(postContent, context);

            MediaPredicate predicate = MediaPredicate.getLocalMediaIdPredicate(localMediaId);

            // remove the uploading class
            Attributes firstElementAttributes = getFirstElementAttributes(content, predicate);
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

                updateElementAttributes(content, predicate, attributesWithClass.getAttributes());

                // re-set the post content
                postContent = parser.toHtml(content, false);
            }
        }
        return postContent;
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
        Spanned content = parser.fromHtml(postContent, context);

        // get all items with the class in the "tag" param
        AztecText.AttributePredicate uploadingPredicate = getPredicateWithClass(tag);


        return getFirstElementAttributes(content, uploadingPredicate) != null;
    }

    public static String resetUploadingMediaToFailed(Context context, @NonNull String postContent) {
        // fill in Aztec with the post's content
        AztecParser parser = getAztecParserWithPlugins();
        Spanned content = parser.fromHtml(postContent, context);

        // get all items with "failed" class, and make sure they are still failed
        // i.e. if they have a local src, then they are failed.
        resetMediaWithStatus(content, ATTR_STATUS_FAILED);
        // get all items with "uploading" class, and make sure they are either already uploaded
        // (that is, they have a remote src), and mark them "failed" if not.
        resetMediaWithStatus(content, ATTR_STATUS_UPLOADING);

        // re-set the post content
        postContent = parser.toHtml(content, false);
        return postContent;
    }

    public static List<String> getMediaMarkedUploadingInPostContent(Context context, @NonNull String postContent) {
        ArrayList<String> mediaMarkedUploading = new ArrayList<>();
        // fill in Aztec with the post's content
        AztecParser parser = getAztecParserWithPlugins();
        Spanned content = parser.fromHtml(postContent, context);
        AztecText.AttributePredicate uploadingPredicate = getPredicateWithClass(ATTR_STATUS_UPLOADING);
        for (Attributes attrs : getAllElementAttributes(content, uploadingPredicate)) {
            String itemId = attrs.getValue(ATTR_ID_WP);
            if (!TextUtils.isEmpty(itemId)) {
                mediaMarkedUploading.add(itemId);
            }
        }
        return mediaMarkedUploading;
    }

    public void setMediaToFailed(@NonNull String mediaId) {
        AztecText.AttributePredicate localMediaIdPredicate = MediaPredicate.getLocalMediaIdPredicate(mediaId);
        // we should be obtaining just one span for this media Id predicate, but just in case something
        // weird happened we make sure we run through all obtained spans and mark them failed if a local src found
        List<IAztecAttributedSpan> spans = getSpansForPredicate(content.getText(), localMediaIdPredicate, false);
        for (IAztecAttributedSpan span : spans) {
            clearMediaUploadingAndSetToFailedIfLocal(span);
        }
        content.clearOverlays(localMediaIdPredicate);
        overlayFailedMedia(mediaId, content.getElementAttributes(localMediaIdPredicate));
        safeAddMediaIdToSet(mFailedMediaIds, mediaId);
        content.resetAttributedMediaSpan(localMediaIdPredicate);
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

    private static void clearMediaUploadingAndSetToFailedIfLocal(IAztecAttributedSpan span) {
        // remove the uploading class
        AttributesWithClass attributesWithClass = getAttributesWithClass(span.getAttributes());
        attributesWithClass.removeClass(ATTR_STATUS_UPLOADING);

        attributesWithClass = addFailedStatusToMediaIfLocalSrcPresent(attributesWithClass);

        span.setAttributes(attributesWithClass.getAttributes());
    }

    private static AttributesWithClass addFailedStatusToMediaIfLocalSrcPresent(AttributesWithClass attributesWithClass) {
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
        return new AztecParser(plugins);
    }

    private Drawable getLoadingImagePlaceholder() {
        if (loadingImagePlaceholder != null) {
            return  loadingImagePlaceholder;
        }

        // Use default loading placeholder if none was set by the host activity
        Drawable defaultLoadingImagePlaceholder = getResources().getDrawable(R.drawable.ic_gridicons_image);
        defaultLoadingImagePlaceholder.setBounds(0, 0, DEFAULT_MEDIA_PLACEHOLDER_DIMENSION_DP, DEFAULT_MEDIA_PLACEHOLDER_DIMENSION_DP);
        return defaultLoadingImagePlaceholder;
    }

    private Drawable getLoadingVideoPlaceholder() {
        if (loadingVideoPlaceholder != null) {
            return  loadingVideoPlaceholder;
        }

        // Use default loading placeholder if none was set by the host activity
        Drawable defaultLoadingImagePlaceholder = getResources().getDrawable(R.drawable.ic_gridicons_video_camera);
        defaultLoadingImagePlaceholder.setBounds(0, 0, DEFAULT_MEDIA_PLACEHOLDER_DIMENSION_DP, DEFAULT_MEDIA_PLACEHOLDER_DIMENSION_DP);
        return defaultLoadingImagePlaceholder;
    }

    public void setLoadingImagePlaceholder(Drawable loadingImagePlaceholder) {
        this.loadingImagePlaceholder = loadingImagePlaceholder;
    }

    public void setLoadingVideoPlaceholder(Drawable loadingVideoPlaceholder) {
        this.loadingVideoPlaceholder = loadingVideoPlaceholder;
    }
}
