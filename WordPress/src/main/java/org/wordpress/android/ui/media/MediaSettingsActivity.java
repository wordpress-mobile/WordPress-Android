package org.wordpress.android.ui.media;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IntegerRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.databinding.MediaSettingsInfoRowBinding;
import org.wordpress.android.databinding.MediaSettingsRowBinding;
import org.wordpress.android.editor.EditorImageMetaData;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.main.BaseAppCompatActivity;
import org.wordpress.android.ui.media.MediaFieldEditBottomSheet.Field;
import org.wordpress.android.ui.media.MediaPreviewActivity.MediaPreviewSwiped;
import org.wordpress.android.ui.utils.AuthenticationUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ColorUtils;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.util.WPPermissionUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.extensions.CompatExtensionsKt;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageManager.RequestListener;
import org.wordpress.android.util.image.ImageType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import static org.wordpress.android.editor.EditorImageMetaData.ARG_EDITOR_IMAGE_METADATA;

public class MediaSettingsActivity extends BaseAppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final int TOOLBAR_HEIGHT_MULTIPLIER = 3;
    private static final float IMAGE_HEIGHT_RATIO = 0.4f;
    private static final String ARG_MEDIA_LOCAL_ID = "media_local_id";
    private static final String ARG_ID_LIST = "id_list";
    private static final String ARG_DELETE_MEDIA_DIALOG_VISIBLE = "delete_media_dialog_visible";
    private static final String STATE_FIELD_PREFIX = "state_field_";
    public static final int RESULT_MEDIA_DELETED = RESULT_FIRST_USER;

    private long mDownloadId;
    private String mTitle;
    private boolean mDidRegisterEventBus;

    private SiteModel mSite;
    private MediaModel mMedia;
    private EditorImageMetaData mEditorImageMetaData;
    private ArrayList<String> mMediaIdList;
    private String[] mAlignmentKeyArray;

    private MediaSettingsImageSize mImageSize = MediaSettingsImageSize.FULL;

    private ImageView mImageView;
    private ImageView mImagePlay;
    // in-memory values of the editable fields, read by saveChanges()
    private final EnumMap<Field, String> mFieldValues = new EnumMap<>(Field.class);
    private final EnumMap<Field, MediaSettingsRowBinding> mDetailRows = new EnumMap<>(Field.class);

    private EditText mLinkView;
    private CheckBox mLinkTargetNewWindowView;
    private TextView mImageSizeView;
    private SeekBar mImageSizeSeekBarView;
    private Spinner mAlignmentSpinnerView;
    private View mFullScreenButton;
    @Nullable private View mRowToRefocus;

    private AlertDialog mDeleteMediaConfirmationDialog;

    private ProgressDialog mProgressDialog;

    private enum MediaType {
        IMAGE,
        VIDEO,
        AUDIO,
        DOCUMENT
    }

    private MediaType mMediaType;

    @Inject UserAgent mUserAgent;

    @Inject MediaStore mMediaStore;
    @Inject Dispatcher mDispatcher;
    @Inject ImageManager mImageManager;
    @Inject AuthenticationUtils mAuthenticationUtils;

    /**
     * @param activity    calling activity
     * @param site        site this media is associated with
     * @param media       media model to display
     * @param mediaIdList optional list of media IDs to page through in preview screen
     */
    public static void showForResult(@NonNull Activity activity,
                                     @NonNull SiteModel site,
                                     @NonNull MediaModel media,
                                     @Nullable ArrayList<String> mediaIdList) {
        // go directly to preview for local images, videos and audio (do nothing for local documents)
        if (MediaUtils.isLocalFile(media.getUploadState())) {
            if (MediaUtils.isValidImage(media.getFilePath())
                || MediaUtils.isAudio(media.getFilePath())
                || media.isVideo()) {
                MediaPreviewActivity.showPreview(activity, site, media.getFilePath());
            }
            return;
        }

        Intent intent = new Intent(activity, MediaSettingsActivity.class);
        intent.putExtra(ARG_MEDIA_LOCAL_ID, media.getId());
        intent.putExtra(WordPress.SITE, site);

        if (mediaIdList != null) {
            intent.putExtra(ARG_ID_LIST, mediaIdList);
        }

        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                activity,
                R.anim.activity_slide_up_from_bottom,
                R.anim.do_nothing);
        ActivityCompat.startActivityForResult(activity, intent, RequestCodes.MEDIA_SETTINGS, options.toBundle());
    }

    /**
     * @param activity    calling activity
     * @param site        site this media is associated with
     * @param editorMedia editor image metadata
     */
    public static void showForResult(@NonNull Activity activity,
                                     @NonNull SiteModel site,
                                     @NonNull EditorImageMetaData editorMedia) {
        Intent intent = new Intent(activity, MediaSettingsActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(ARG_EDITOR_IMAGE_METADATA, editorMedia);

        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                activity,
                R.anim.activity_slide_up_from_bottom,
                R.anim.do_nothing);

        ActivityCompat.startActivityForResult(activity, intent, RequestCodes.MEDIA_SETTINGS, options.toBundle());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.media_settings_activity);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                saveChanges();
                CompatExtensionsKt.onBackPressedCompat(getOnBackPressedDispatcher(), this);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        }

        mImageView = findViewById(R.id.image_preview);
        mImagePlay = findViewById(R.id.image_play);
        mLinkView = findViewById(R.id.edit_link);
        mLinkTargetNewWindowView = findViewById(R.id.edit_link_target_new_widnow_checkbox);
        mImageSizeView = findViewById(R.id.image_size_hint);
        mImageSizeSeekBarView = findViewById(R.id.image_size_seekbar);
        mAlignmentSpinnerView = findViewById(org.wordpress.android.editor.R.id.alignment_spinner);
        mFullScreenButton = findViewById(R.id.button_fullscreen);

        mDetailRows.put(Field.TITLE, MediaSettingsRowBinding.bind(findViewById(R.id.row_title)));
        mDetailRows.put(Field.CAPTION, MediaSettingsRowBinding.bind(findViewById(R.id.row_caption)));
        mDetailRows.put(Field.DESCRIPTION, MediaSettingsRowBinding.bind(findViewById(R.id.row_description)));
        mDetailRows.put(Field.ALT_TEXT, MediaSettingsRowBinding.bind(findViewById(R.id.row_alt_text)));
        for (Map.Entry<Field, MediaSettingsRowBinding> entry : mDetailRows.entrySet()) {
            Field field = entry.getKey();
            MediaSettingsRowBinding row = entry.getValue();
            row.rowLabel.setText(field.getTitleRes());
            row.getRoot().setOnClickListener(v -> showFieldEditor(field));
            ViewCompat.replaceAccessibilityAction(row.getRoot(), AccessibilityActionCompat.ACTION_CLICK,
                    getString(R.string.edit), null);
        }

        // clip row ripples to the rounded cards
        for (int id : new int[]{R.id.media_customisation_options, R.id.card_details, R.id.card_file_info}) {
            findViewById(id).setClipToOutline(true);
        }

        getSupportFragmentManager().setFragmentResultListener(MediaFieldEditBottomSheet.REQUEST_KEY, this,
                (requestKey, result) -> {
                    String fieldName = result.getString(MediaFieldEditBottomSheet.KEY_FIELD);
                    if (fieldName != null) {
                        mFieldValues.put(Field.valueOf(fieldName),
                                StringUtils.notNullStr(result.getString(MediaFieldEditBottomSheet.KEY_VALUE)));
                        bindDetailRows();
                    }
                });

        int mediaId;
        if (savedInstanceState != null) {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mEditorImageMetaData = savedInstanceState.getParcelable(ARG_EDITOR_IMAGE_METADATA);
            mediaId = savedInstanceState.getInt(ARG_MEDIA_LOCAL_ID);
            if (savedInstanceState.containsKey(ARG_ID_LIST)) {
                mMediaIdList = savedInstanceState.getStringArrayList(ARG_ID_LIST);
            }

            if (savedInstanceState.getBoolean(ARG_DELETE_MEDIA_DIALOG_VISIBLE, false)) {
                deleteMediaWithConfirmation();
            }
        } else {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            mEditorImageMetaData = getIntent().getParcelableExtra(ARG_EDITOR_IMAGE_METADATA);
            mediaId = getIntent().getIntExtra(ARG_MEDIA_LOCAL_ID, 0);
            if (getIntent().hasExtra(ARG_ID_LIST)) {
                mMediaIdList = getIntent().getStringArrayListExtra(ARG_ID_LIST);
            }
        }

        if (isMediaFromEditor() ? !loadMediaFromEditor() : !loadMediaWithId(mediaId)) {
            delayedFinishWithError();
            return;
        }

        if (savedInstanceState != null) {
            for (Field field : Field.values()) {
                mFieldValues.put(field,
                        StringUtils.notNullStr(savedInstanceState.getString(STATE_FIELD_PREFIX + field.name())));
            }
            bindDetailRows();
        }

        // only show title when toolbar is collapsed
        final CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            int mScrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (mScrollRange == -1) {
                    mScrollRange = appBarLayout.getTotalScrollRange();
                }
                if (mScrollRange + verticalOffset == 0) {
                    collapsingToolbar.setTitle(mTitle);
                } else {
                    collapsingToolbar.setTitle(" "); // space between double quotes is on purpose
                }
            }
        });

        // make image 40% of screen height
        mImageView.getLayoutParams().height = (int) (DisplayUtils.getWindowPixelHeight(this) * IMAGE_HEIGHT_RATIO);

        // set the height of the gradient scrim that appears atop the image
        int toolbarHeight = DisplayUtils.getActionBarHeight(this);
        findViewById(R.id.image_gradient_scrim).getLayoutParams().height = toolbarHeight * TOOLBAR_HEIGHT_MULTIPLIER;

        // tap to show full screen view (not supported for documents)
        if (!isDocument()) {
            View.OnClickListener listener = v -> showFullScreen();
            mImageView.setOnClickListener(listener);
            mImagePlay.setOnClickListener(listener);
            mFullScreenButton.setOnClickListener(listener);
        }

        // keep the last rows clear of the navigation bar when drawing edge-to-edge
        View scrollContent = findViewById(R.id.scroll_content);
        int scrollContentPaddingBottom = scrollContent.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(scrollContent, (v, insets) -> {
            int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    scrollContentPaddingBottom + navBarHeight);
            return insets;
        });

        enableEdgeToEdge();
    }

    /**
     * Enable edge-to-edge API 33+, but only hide the status bar and not the navigation bar
     */
    private void enableEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Window window = getWindow();
            window.setDecorFitsSystemWindows(false);
            WindowInsetsControllerCompat controller =
                    new WindowInsetsControllerCompat(window, window.getDecorView());
            controller.hide(WindowInsetsCompat.Type.statusBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }
    }

    private boolean isMediaFromEditor() {
        return mEditorImageMetaData != null;
    }

    private void reloadMedia() {
        loadMediaWithId(mMedia.getId());
    }

    private boolean loadMediaWithId(int mediaId) {
        MediaModel media = mMediaStore.getMediaWithLocalId(mediaId);
        return loadMedia(media);
    }

    private boolean loadMediaFromEditor() {
        MediaModel media = getMediaModelFromEditorImageMetaData();
        return loadMedia(media);
    }

    private boolean loadMedia(MediaModel media) {
        if (media == null) {
            return false;
        }

        mMedia = media;

        // try to get a file without parameters so we can more reliably determine media type
        String uriFilePath = !TextUtils.isEmpty(mMedia.getUrl()) ? Uri.parse(mMedia.getUrl()).getPath() : "";

        // determine media type up front, default to DOCUMENT if we can't detect it's an image, video, or audio file
        if (MediaUtils.isValidImage(uriFilePath)) {
            mMediaType = MediaType.IMAGE;
            mTitle = getString(R.string.media_title_image_details);
        } else if (mMedia.isVideo()) {
            mMediaType = MediaType.VIDEO;
            mTitle = getString(R.string.media_title_video_details);
        } else if (MediaUtils.isAudio(uriFilePath)) {
            mMediaType = MediaType.AUDIO;
            mTitle = getString(R.string.media_title_audio_details);
        } else {
            mMediaType = MediaType.DOCUMENT;
            mTitle = getString(R.string.media_title_document_details);
        }

        mImagePlay.setVisibility(isVideo() || isAudio() ? View.VISIBLE : View.GONE);
        mFullScreenButton.setVisibility(shouldShowFullScreenButton() ? View.VISIBLE : View.GONE);
        int altTextVisibility = isVideo() || isAudio() || isDocument() ? View.GONE : View.VISIBLE;
        mDetailRows.get(Field.ALT_TEXT).getRoot().setVisibility(altTextVisibility);
        findViewById(R.id.divider_alt_text).setVisibility(altTextVisibility);

        showMetaData();

        // audio & documents show a placeholder on top of a gradient, otherwise we show a thumbnail
        if (isAudio() || isDocument()) {
            int padding = getResources().getDimensionPixelSize(R.dimen.margin_extra_extra_large);
            ColorUtils.INSTANCE.setImageResourceWithTint(mImageView,
                    WPMediaUtils.getPlaceholder(uriFilePath, mMedia.getMimeType()), R.color.neutral_30);
            mImageView.setPadding(padding, padding * 2, padding, padding);
        } else {
            loadImage();
        }

        return true;
    }

    @NonNull
    private MediaModel getMediaModelFromEditorImageMetaData() {
        String source = mEditorImageMetaData.getSrc();
        String filename = null;
        if (!TextUtils.isEmpty(source)) {
            filename = source.substring(source.lastIndexOf("/") + 1);
        }
        return new MediaModel(
                source,
                filename,
                org.wordpress.android.fluxc.utils.MediaUtils.getExtension(source),
                mEditorImageMetaData.getTitle(),
                mEditorImageMetaData.getCaption(),
                mEditorImageMetaData.getAlt(),
                mEditorImageMetaData.getWidthInt(),
                mEditorImageMetaData.getHeightInt()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null && !actionBar.isShowing()) {
            actionBar.show();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_MEDIA_LOCAL_ID, mMedia.getId());
        outState.putParcelable(ARG_EDITOR_IMAGE_METADATA, mEditorImageMetaData);
        for (Field field : Field.values()) {
            outState.putString(STATE_FIELD_PREFIX + field.name(), getFieldValue(field));
        }

        if (mDeleteMediaConfirmationDialog != null) {
            outState.putBoolean(ARG_DELETE_MEDIA_DIALOG_VISIBLE, mDeleteMediaConfirmationDialog.isShowing());
        }

        if (mSite != null) {
            outState.putSerializable(WordPress.SITE, mSite);
        }
        if (mMediaIdList != null) {
            outState.putStringArrayList(ARG_ID_LIST, mMediaIdList);
        }
    }

    @Override
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(
                mDownloadReceiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextWrapper.RECEIVER_EXPORTED
            );
        } else {
            registerReceiver(mDownloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
        mDispatcher.register(this);

        // we only register with EventBus the first time - necessary since we don't unregister in onStop()
        // because we want to keep receiving events while the preview is showing
        if (!mDidRegisterEventBus) {
            EventBus.getDefault().register(this);
            mDidRegisterEventBus = true;
        }
    }

    @Override
    public void onStop() {
        unregisterReceiver(mDownloadReceiver);
        mDispatcher.unregister(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mDidRegisterEventBus) {
            EventBus.getDefault().unregister(this);
        }
        super.onDestroy();
    }

    private void delayedFinishWithError() {
        ToastUtils.showToast(this, R.string.error_media_not_found);
        new Handler().postDelayed(this::finish, 1500);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.do_nothing, R.anim.activity_slide_out_to_bottom);
    }


    private boolean shouldShowFullScreenButton() {
        // the full screen button only shows for images
        return mMedia != null && isImage();
    }

    private void showProgress(boolean show) {
        findViewById(R.id.progress).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.media_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        boolean showSaveMenu = mSite != null && !mSite.isPrivate() && !isMediaFromEditor();
        boolean showShareMenu = mSite != null && !mSite.isPrivate() && !isMediaFromEditor();
        boolean showTrashMenu = mSite != null && !isMediaFromEditor();
        boolean showRemoveImage = mSite != null && isMediaFromEditor();

        MenuItem mnuSave = menu.findItem(R.id.menu_save);
        mnuSave.setVisible(showSaveMenu);
        mnuSave.setEnabled(mDownloadId == 0);

        MenuItem mnuShare = menu.findItem(R.id.menu_share);
        mnuShare.setVisible(showShareMenu);

        MenuItem mnuTrash = menu.findItem(R.id.menu_trash);
        mnuTrash.setVisible(showTrashMenu);

        MenuItem mnuRemove = menu.findItem(R.id.menu_remove_image);
        mnuRemove.setVisible(showRemoveImage);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.menu_save) {
            saveMediaToDevice();
            return true;
        } else if (item.getItemId() == R.id.menu_share) {
            shareMedia();
            return true;
        } else if (item.getItemId() == R.id.menu_trash || item.getItemId() == R.id.menu_remove_image) {
            deleteMediaWithConfirmation();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean isImage() {
        return mMediaType == MediaType.IMAGE;
    }

    private boolean isVideo() {
        return mMediaType == MediaType.VIDEO;
    }

    private boolean isAudio() {
        return mMediaType == MediaType.AUDIO;
    }

    private boolean isDocument() {
        return mMediaType == MediaType.DOCUMENT;
    }

    private void showMetaData() {
        mFieldValues.put(Field.TITLE, StringUtils.notNullStr(mMedia.getTitle()));
        mFieldValues.put(Field.ALT_TEXT, StringUtils.notNullStr(mMedia.getAlt()));
        mFieldValues.put(Field.CAPTION, StringUtils.notNullStr(mMedia.getCaption()));

        if (isMediaFromEditor()) {
            mLinkView.setText(mEditorImageMetaData.getLinkUrl());
            mLinkTargetNewWindowView.setChecked(mEditorImageMetaData.isLinkTargetBlank());

            mDetailRows.get(Field.DESCRIPTION).getRoot().setVisibility(View.GONE);
            findViewById(R.id.divider_description).setVisibility(View.GONE);

            setupAlignmentSpinner();
            setupImageSizeSeekBar();
        } else {
            mFieldValues.put(Field.DESCRIPTION, StringUtils.notNullStr(mMedia.getDescription()));

            findViewById(R.id.media_customisation_options).setVisibility(View.GONE);
        }

        bindDetailRows();
        showFileInfo();

        // tapping the URL copies it
        String url = StringUtils.notNullStr(mMedia.getUrl());
        ((TextView) findViewById(R.id.text_url)).setText(url);
        View urlRow = findViewById(R.id.row_url);
        urlRow.setContentDescription(getString(R.string.media_settings_row_value,
                getString(R.string.media_edit_url_caption), url));
        if (url.isEmpty()) {
            urlRow.setOnClickListener(null);
            urlRow.setClickable(false);
            ViewCompat.replaceAccessibilityAction(urlRow, AccessibilityActionCompat.ACTION_CLICK, null, null);
        } else {
            urlRow.setOnClickListener(v -> copyMediaUrlToClipboard());
            ViewCompat.replaceAccessibilityAction(urlRow, AccessibilityActionCompat.ACTION_CLICK,
                    getString(R.string.media_settings_copy_url), null);
        }
    }

    private void showFieldEditor(@NonNull Field field) {
        if (getSupportFragmentManager().findFragmentByTag(MediaFieldEditBottomSheet.TAG) != null) {
            return;
        }
        mRowToRefocus = mDetailRows.get(field).getRoot();
        MediaFieldEditBottomSheet.newInstance(field, getFieldValue(field))
                                 .showNow(getSupportFragmentManager(), MediaFieldEditBottomSheet.TAG);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // when the edit sheet closes after keyboard input, focus would otherwise land on the scroll view,
        // which then draws as a gray overlay across the screen; give it back to the row that opened the sheet
        if (hasFocus && mRowToRefocus != null) {
            mRowToRefocus.requestFocus();
            mRowToRefocus = null;
        }
    }

    @NonNull
    private String getFieldValue(@NonNull Field field) {
        return StringUtils.notNullStr(mFieldValues.get(field));
    }

    private void bindDetailRows() {
        for (Map.Entry<Field, MediaSettingsRowBinding> entry : mDetailRows.entrySet()) {
            MediaSettingsRowBinding row = entry.getValue();
            String label = getString(entry.getKey().getTitleRes());
            String value = getFieldValue(entry.getKey());
            row.rowValue.setText(value);
            row.getRoot().setContentDescription(value.isEmpty()
                    ? getString(R.string.media_settings_row_not_set, label)
                    : getString(R.string.media_settings_row_value, label, value));
        }
    }

    /*
     * shows the read-only file info rows below the URL, skipping any value that's unknown
     */
    private void showFileInfo() {
        // dimensions aren't shown for images in a post since the size settings determine them
        String dimensions = !isMediaFromEditor() && mMedia.getWidth() > 0 && mMedia.getHeight() > 0
                ? mMedia.getWidth() + " × " + mMedia.getHeight() : null;
        String duration = mMedia.getLength() > 0 ? DateUtils.formatElapsedTime(mMedia.getLength()) : null;

        LinearLayout container = findViewById(R.id.file_info_rows);
        container.removeAllViews();
        addFileInfoRow(container, R.string.media_settings_file_name, mMedia.getFileName());
        addFileInfoRow(container, R.string.media_settings_file_type,
                StringUtils.notNullStr(mMedia.getFileExtension()).toUpperCase(Locale.ROOT));
        addFileInfoRow(container, R.string.media_settings_file_size, getFormattedFileSize());
        addFileInfoRow(container, R.string.media_settings_dimensions, dimensions);
        addFileInfoRow(container, R.string.media_edit_duration_caption, duration);
        addFileInfoRow(container, R.string.media_settings_uploaded, getFormattedUploadDate());
    }

    private void addFileInfoRow(@NonNull LinearLayout container, @StringRes int labelRes, @Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        LayoutInflater inflater = getLayoutInflater();
        inflater.inflate(R.layout.media_settings_divider, container, true);
        MediaSettingsInfoRowBinding row = MediaSettingsInfoRowBinding.inflate(inflater, container, false);
        String label = getString(labelRes);
        row.rowLabel.setText(label);
        row.rowValue.setText(value);
        row.getRoot().setContentDescription(getString(R.string.media_settings_row_value, label, value));
        container.addView(row.getRoot());
    }

    @Nullable
    private String getFormattedFileSize() {
        if (mMedia.getFileSize() > 0) {
            final String[] units = new String[] {
                    getString(R.string.file_size_in_bytes),
                    getString(R.string.file_size_in_kilobytes),
                    getString(R.string.file_size_in_megabytes),
                    getString(R.string.file_size_in_gigabytes),
                    getString(R.string.file_size_in_terabytes)
            };
            return FormatUtils.formatFileSize(mMedia.getFileSize(), units);
        }
        return mMedia.getFileSizeFormatted();
    }

    @Nullable
    private String getFormattedUploadDate() {
        if (mMedia.getUploadDate() == null) {
            return null;
        }
        Date date = DateTimeUtils.dateFromIso8601(mMedia.getUploadDate());
        return date != null ? SimpleDateFormat.getDateInstance().format(date) : null;
    }

    /**
     * Initialize the image width SeekBar and accompanying EditText
     */
    private void setupImageSizeSeekBar() {
        // image size is parsed from html, so we can get non standard values (anything that matches ^size-.*)
        // in this case we will default to the full size
        mImageSize = MediaSettingsImageSize.fromCssClass(this, mEditorImageMetaData.getSize());

        mImageSizeSeekBarView.setMax(MediaSettingsImageSize.values().length - 1);
        mImageSizeSeekBarView.setProgress(Arrays.asList(MediaSettingsImageSize.values()).indexOf(mImageSize));

        mImageSizeView.setText(mImageSize.getLabel());

        mImageSizeSeekBarView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mImageSize = MediaSettingsImageSize.values()[progress];
                mImageSizeView.setText(mImageSize.getLabel());
            }
        });
    }

    /**
     * Initialize the image alignment spinner
     */
    private void setupAlignmentSpinner() {
        String alignment = mEditorImageMetaData.getAlign();
        mAlignmentKeyArray = getResources().getStringArray(R.array.alignment_key_array);
        int alignmentIndex = Arrays.asList(mAlignmentKeyArray).indexOf(alignment);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.alignment_array, R.layout.media_settings_alignment_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mAlignmentSpinnerView.setAdapter(adapter);
        mAlignmentSpinnerView.setSelection(alignmentIndex == -1 ? 0 : alignmentIndex);
    }

    /*
     * loads and displays a remote or local image
     */
    private void loadImage() {
        int width = DisplayUtils.getWindowPixelWidth(this);
        int height = DisplayUtils.getWindowPixelHeight(this);
        int size = Math.max(width, height);

        String mediaUri;
        if (isVideo()) {
            mediaUri = mMedia.getThumbnailUrl();
        } else {
            mediaUri = mMedia.getUrl();
        }

        if (TextUtils.isEmpty(mediaUri)) {
            if (isVideo()) {
                downloadVideoThumbnail();
            } else {
                ToastUtils.showToast(this, R.string.error_media_load);
            }
            return;
        }

        showProgress(true);
        String imageUrl = mediaUri;
        if (SiteUtils.isPhotonCapable(mSite)) {
            imageUrl = PhotonUtils.getPhotonImageUrl(mediaUri, size, 0, mSite.isPrivateWPComAtomic());
        }
        mImageManager.loadWithResultListener(mImageView, ImageType.IMAGE, imageUrl, ScaleType.CENTER, null,
                new RequestListener<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Object model) {
                        if (!isFinishing()) {
                            showProgress(false);
                        }
                    }

                    @Override
                    public void onLoadFailed(@Nullable Exception e, @Nullable Object model) {
                        if (!isFinishing()) {
                            if (e != null) {
                                AppLog.e(T.MEDIA, e);
                            }
                            showProgress(false);
                            if (isVideo()) {
                                // for videos it's ok if we fail to load the thumbnail - can happen.
                                // let's show a toast but let the user edit the media settings!
                                ToastUtils.showToast(MediaSettingsActivity.this,
                                        R.string.error_media_thumbnail_not_loaded);
                            } else {
                                delayedFinishWithError();
                            }
                        }
                    }
                });
    }

    /*
     * downloads and displays the thumbnail for a video that doesn't already have a thumbnail assigned (seen most
     * often with .org and JP sites)
     */
    private void downloadVideoThumbnail() {
        new Thread() {
            @Override
            public void run() {
                int width = DisplayUtils.getWindowPixelWidth(MediaSettingsActivity.this);
                final Bitmap thumb = ImageUtils.getVideoFrameFromVideo(
                        mMedia.getUrl(),
                        width,
                        mAuthenticationUtils.getAuthHeaders(mMedia.getUrl())
                );
                if (thumb != null) {
                    runOnUiThread(() -> {
                        if (!isFinishing()) {
                            WordPress.getBitmapCache().put(mMedia.getUrl(), thumb);
                            mImageView.setImageBitmap(thumb);
                        }
                    });
                }
            }
        }.start();
    }

    private void showFullScreen() {
        saveChanges();

        // show fullscreen preview after a brief delay so the actionBar animation doesn't stutter
        new Handler().postDelayed(() -> {
            if (isMediaFromEditor()) {
                MediaPreviewActivity.showPreview(MediaSettingsActivity.this, mSite, mEditorImageMetaData.getSrc());
            } else {
                MediaPreviewActivity.showPreview(MediaSettingsActivity.this, mSite, mMedia, mMediaIdList);
            }
        }, 200);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean allGranted = WPPermissionUtils.setPermissionListAsked(
                this, requestCode, permissions, grantResults, true);
        if (allGranted && requestCode == WPPermissionUtils.MEDIA_PREVIEW_PERMISSION_REQUEST_CODE) {
            saveMediaToDevice();
        }
    }

    /*
     * receives download completion broadcasts from the DownloadManager
     */
    private final BroadcastReceiver mDownloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long thisId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (thisId == mDownloadId) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(mDownloadId);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                Cursor cursor = dm.query(query);
                if (cursor != null && cursor.moveToFirst()) {
                    // meaning of `reason` depends on the value of COLUMN_STATUS
                    int reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                    int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_FAILED) {
                        ToastUtils.showToast(MediaSettingsActivity.this, R.string.error_media_save);
                        // If an HTTP error occurred, this will hold the HTTP status code as defined in RFC 2616.
                        // Otherwise, it will hold one of the ERROR_* constants
                        AppLog.e(AppLog.T.MEDIA, "MediaSettingsActivity > save > STATUS_FAILED - reason: " + reason);
                    }
                }
                mDownloadId = 0;
                invalidateOptionsMenu();
            }
        }
    };

    private void saveChanges() {
        if (isFinishing()) {
            return;
        }

        String thisTitle = getFieldValue(Field.TITLE);
        String thisCaption = getFieldValue(Field.CAPTION);
        String thisAltText = getFieldValue(Field.ALT_TEXT);
        String thisDescription = getFieldValue(Field.DESCRIPTION);

        if (!isMediaFromEditor()) {
            MediaModel media = mMediaStore.getMediaWithLocalId(mMedia.getId());
            if (media == null) {
                AppLog.w(AppLog.T.MEDIA, "MediaSettingsActivity > Cannot save null media");
                ToastUtils.showToast(this, R.string.media_edit_failure);
                return;
            }

            boolean hasChanged = !StringUtils.equals(media.getTitle(), thisTitle)
                                 || !StringUtils.equals(media.getCaption(), thisCaption)
                                 || !StringUtils.equals(media.getAlt(), thisAltText)
                                 || !StringUtils.equals(media.getDescription(), thisDescription);

            if (hasChanged) {
                AppLog.d(AppLog.T.MEDIA, "MediaSettingsActivity > Saving changes");
                media.setTitle(thisTitle);
                media.setCaption(thisCaption);
                media.setAlt(thisAltText);
                media.setDescription(thisDescription);
                mDispatcher.dispatch(MediaActionBuilder.newPushMediaAction(new MediaPayload(mSite, media)));
            }
        } else {
            String alignment = mAlignmentKeyArray[mAlignmentSpinnerView.getSelectedItemPosition()];
            String size = getString(mImageSize.getCssClass());
            String linkUrl = EditTextUtils.getText(mLinkView);
            boolean linkTargetBlank = mLinkTargetNewWindowView.isChecked();
            boolean hasSizeChanged = !StringUtils.equals(mEditorImageMetaData.getSize(), size);

            boolean hasChanged = hasSizeChanged
                                 || !StringUtils.equals(mEditorImageMetaData.getTitle(), thisTitle)
                                 || !StringUtils.equals(mEditorImageMetaData.getAlt(), thisAltText)
                                 || !StringUtils.equals(mEditorImageMetaData.getCaption(), thisCaption)
                                 || !StringUtils.equals(mEditorImageMetaData.getAlign(), alignment)
                                 || !StringUtils.equals(mEditorImageMetaData.getLinkUrl(), linkUrl)
                                 || linkTargetBlank != mEditorImageMetaData.isLinkTargetBlank();

            if (hasChanged) {
                mEditorImageMetaData.setTitle(thisTitle);
                mEditorImageMetaData.setSize(size);
                mEditorImageMetaData.setAlt(thisAltText);
                mEditorImageMetaData.setAlign(alignment);
                mEditorImageMetaData.setCaption(thisCaption);
                mEditorImageMetaData.setLinkUrl(linkUrl);
                mEditorImageMetaData.setLinkTargetBlank(linkTargetBlank);

                // we only update width and height attributes on self hosted and Jetpack sites
                // because css image size classes wont have any effect there
                if (!mSite.isWPCom() && hasSizeChanged) {
                    updateImageSizeParameters();
                }

                Intent intent = new Intent();
                intent.putExtra(ARG_EDITOR_IMAGE_METADATA, mEditorImageMetaData);

                this.setResult(Activity.RESULT_OK, intent);
            } else {
                this.setResult(Activity.RESULT_CANCELED);
            }
        }
    }

    private void updateImageSizeParameters() {
        // if caption is empty we can safely remove width and height attributes
        if (mImageSize == MediaSettingsImageSize.FULL && TextUtils.isEmpty(mEditorImageMetaData.getCaption())) {
            mEditorImageMetaData.setWidth(null);
            mEditorImageMetaData.setHeight(null);
            return;
        }

        int imageWidth = mEditorImageMetaData.getWidthInt();
        int imageHeight = mEditorImageMetaData.getHeightInt();
        int newImageSize = getResources().getInteger(mImageSize.getSize());

        float aspectRatio = ((float) imageWidth / (float) imageHeight);

        if (imageWidth > imageHeight) {
            imageHeight = Math.round(newImageSize / aspectRatio);
            imageWidth = newImageSize;
        } else if (imageWidth < imageHeight) {
            imageWidth = Math.round(newImageSize * aspectRatio);
            imageHeight = newImageSize;
        } else {
            // image is square
            imageHeight = newImageSize;
            imageWidth = newImageSize;
        }

        mEditorImageMetaData.setWidth(Integer.toString(imageWidth));
        mEditorImageMetaData.setHeight(Integer.toString(imageHeight));
    }

    /*
     * saves the media to the local device using the Android DownloadManager
     */
    private void saveMediaToDevice() {
        // must request the permission even though it's already defined in the manifest
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            String[] permissionList = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (!PermissionUtils.checkAndRequestPermissions(this,
                    WPPermissionUtils.MEDIA_PREVIEW_PERMISSION_REQUEST_CODE,
                    permissionList)) {
                return;
            }
        }

        if (!NetworkUtils.checkConnection(this)) {
            return;
        }

        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mMedia.getUrl()));
        try {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, mMedia.getFileName());
        } catch (IllegalStateException error) {
            AppLog.e(AppLog.T.MEDIA, error);
            ToastUtils.showToast(MediaSettingsActivity.this, R.string.error_media_save);
            return;
        }
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.addRequestHeader("User-Agent", mUserAgent.getApiUserAgent());

        mDownloadId = dm.enqueue(request);
        invalidateOptionsMenu();
        ToastUtils.showToast(this, R.string.media_downloading);
    }

    private void shareMedia() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, mMedia.getUrl());
        if (!TextUtils.isEmpty(mMedia.getTitle())) {
            intent.putExtra(Intent.EXTRA_SUBJECT, mMedia.getTitle());
        } else if (!TextUtils.isEmpty(mMedia.getDescription())) {
            intent.putExtra(Intent.EXTRA_SUBJECT, mMedia.getDescription());
        }
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share_link)));
        } catch (android.content.ActivityNotFoundException ex) {
            ToastUtils.showToast(this, R.string.reader_toast_err_share_intent);
        }
    }

    /*
     * Depending on the media source it either removes it from post or deletes it from MediaBrowser
     */
    private void deleteMediaWithConfirmation() {
        if (mDeleteMediaConfirmationDialog != null) {
            mDeleteMediaConfirmationDialog.show();
            return;
        }

        @StringRes int resId;

        if (isMediaFromEditor()) {
            resId = R.string.confirm_remove_media_image;
        } else if (isVideo()) {
            resId = R.string.confirm_delete_media_video;
        } else {
            resId = R.string.confirm_delete_media_image;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setMessage(resId)
                .setCancelable(true).setPositiveButton(
                        isMediaFromEditor() ? R.string.remove : R.string.delete, (dialog, which) -> {
                            if (isMediaFromEditor()) {
                                removeMediaFromPost();
                            } else {
                                deleteMedia();
                            }
                        }).setNegativeButton(R.string.cancel, null);

        mDeleteMediaConfirmationDialog = builder.create();
        mDeleteMediaConfirmationDialog.show();
    }

    private void deleteMedia() {
        if (!NetworkUtils.checkConnection(this)) {
            return;
        }

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getString(R.string.deleting_media_dlg));
        mProgressDialog.show();

        AppLog.v(AppLog.T.MEDIA, "Deleting " + mMedia.getTitle() + " (id=" + mMedia.getMediaId() + ")");
        MediaPayload payload = new MediaPayload(mSite, mMedia);
        mDispatcher.dispatch(MediaActionBuilder.newDeleteMediaAction(payload));
    }


    private void removeMediaFromPost() {
        mEditorImageMetaData.markAsRemoved();

        Intent intent = new Intent();
        intent.putExtra(ARG_EDITOR_IMAGE_METADATA, mEditorImageMetaData);

        this.setResult(Activity.RESULT_OK, intent);

        finish();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChanged(OnMediaChanged event) {
        if (event.cause == MediaAction.DELETE_MEDIA) {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            if (event.isError()) {
                ToastUtils.showToast(this, R.string.error_generic);
            } else {
                setResult(RESULT_MEDIA_DELETED);
                finish();
            }
        } else if (!event.isError()) {
            reloadMedia();
        }
    }

    /*
     * user swiped to another media item in the preview activity, so update this one to show the same media
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaPreviewSwiped(MediaPreviewSwiped event) {
        if (event.mediaId != mMedia.getId()) {
            loadMediaWithId(event.mediaId);
        }
    }

    private void copyMediaUrlToClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), mMedia.getUrl()));
            ToastUtils.showToast(this, R.string.media_edit_copy_url_toast);
        } catch (Exception e) {
            AppLog.e(AppLog.T.UTILS, e);
            ToastUtils.showToast(this, R.string.error_copy_to_clipboard);
        }
    }

    public enum MediaSettingsImageSize {
        THUMBNAIL(R.string.image_size_thumbnail_label, R.string.image_size_thumbnail_css_class,
                R.integer.image_size_thumbnail_px),
        MEDIUM(R.string.image_size_medium_label, R.string.image_size_medium_css_class, R.integer.image_size_medium_px),
        LARGE(R.string.image_size_large_label, R.string.image_size_large_css_class, R.integer.image_size_large_px),
        FULL(R.string.image_size_full_label, R.string.image_size_full_css_class, R.integer.image_size_large_px);

        private final int mLabel;
        private final int mCssClass;
        private final int mSize;

        MediaSettingsImageSize(@StringRes int label, @StringRes int cssClass, @IntegerRes int size) {
            mLabel = label;
            mCssClass = cssClass;
            mSize = size;
        }

        public int getSize() {
            return mSize;
        }

        public int getLabel() {
            return mLabel;
        }

        public int getCssClass() {
            return mCssClass;
        }

        public static MediaSettingsImageSize fromCssClass(Context context, String cssClass) {
            for (MediaSettingsImageSize mediaSettingsImageSize : values()) {
                if (context.getString(mediaSettingsImageSize.mCssClass).equals(cssClass)) {
                    return mediaSettingsImageSize;
                }
            }
            return FULL;
        }
    }
}
