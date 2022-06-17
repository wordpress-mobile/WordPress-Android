package org.wordpress.android.editor.example;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.DragEvent;
import android.view.MenuItem;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import org.wordpress.android.editor.AztecEditorFragment;
import org.wordpress.android.editor.EditorFragmentAbstract;
import org.wordpress.android.editor.EditorFragmentAbstract.EditorDragAndDropListener;
import org.wordpress.android.editor.EditorFragmentAbstract.EditorFragmentListener;
import org.wordpress.android.editor.EditorFragmentAbstract.TrackableEvent;
import org.wordpress.android.editor.EditorMediaUploadListener;
import org.wordpress.android.editor.GutenbergEditorFragment;
import org.wordpress.android.editor.ImageSettingsDialogFragment;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.aztec.Html.ImageGetter;
import org.wordpress.aztec.Html.VideoThumbnailGetter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"checkstyle:RegexpMultiline", "checkstyle:LineLength"})
public class EditorExampleActivity extends FragmentActivity implements EditorFragmentListener,
        EditorDragAndDropListener {
    public static final String EDITOR_PARAM = "EDITOR_PARAM";
    public static final String TITLE_PARAM = "TITLE_PARAM";
    public static final String CONTENT_PARAM = "CONTENT_PARAM";
    public static final String DRAFT_PARAM = "DRAFT_PARAM";
    public static final String TITLE_PLACEHOLDER_PARAM = "TITLE_PLACEHOLDER_PARAM";
    public static final String CONTENT_PLACEHOLDER_PARAM = "CONTENT_PLACEHOLDER_PARAM";
    public static final int USE_NEW_EDITOR = 1;
    public static final int USE_LEGACY_EDITOR = 2;

    public static final int ADD_MEDIA_ACTIVITY_REQUEST_CODE = 1111;
    public static final int ADD_MEDIA_FAIL_ACTIVITY_REQUEST_CODE = 1112;
    public static final int ADD_MEDIA_SLOW_NETWORK_REQUEST_CODE = 1113;

    public static final String MEDIA_REMOTE_ID_SAMPLE = "123";

    private static final int SELECT_IMAGE_MENU_POSITION = 0;
    private static final int SELECT_IMAGE_FAIL_MENU_POSITION = 1;
    private static final int SELECT_VIDEO_MENU_POSITION = 2;
    private static final int SELECT_VIDEO_FAIL_MENU_POSITION = 3;
    private static final int SELECT_IMAGE_SLOW_MENU_POSITION = 4;

    private EditorFragmentAbstract mEditorFragment;

    private Map<String, String> mFailedUploads;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getIntExtra(EDITOR_PARAM, USE_NEW_EDITOR) == USE_NEW_EDITOR) {
            ToastUtils.showToast(this, R.string.starting_aztec_editor);
            setContentView(R.layout.activity_aztec_editor);
        } else {
            Fragment fragment = GutenbergEditorFragment.newInstance("", "", true, "us-en");
            ToastUtils.showToast(this, R.string.starting_gutenberg_editor);
            setContentView(R.layout.activity_gutenberg_editor);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.postEditor, fragment);
            ft.commit();
        }

        mFailedUploads = new HashMap<>();
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof EditorFragmentAbstract) {
            mEditorFragment = (EditorFragmentAbstract) fragment;
        }
        if (fragment instanceof AztecEditorFragment) {
            // Fake loaders
            ((AztecEditorFragment) mEditorFragment).setAztecImageLoader(new ImageGetter() {
                @Override
                public void loadImage(String source, Callbacks callbacks, int maxWidth) {

                }

                @Override
                public void loadImage(String source, Callbacks callbacks, int maxWidth,
                                      int minWidth) {

                }
            });
            ((AztecEditorFragment) mEditorFragment).setAztecVideoLoader(new VideoThumbnailGetter() {
                @Override
                public void loadVideoThumbnail(String source, Callbacks callbacks, int maxWidth) {

                }

                @Override
                public void loadVideoThumbnail(String source, Callbacks callbacks, int maxWidth,
                                               int minWidth) {

                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        Fragment fragment =
                getSupportFragmentManager().findFragmentByTag(ImageSettingsDialogFragment.IMAGE_SETTINGS_DIALOG_TAG);
        if (fragment != null && fragment.isVisible()) {
            ((ImageSettingsDialogFragment) fragment).dismissFragment();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(0, SELECT_IMAGE_MENU_POSITION, 0, getString(R.string.select_image));
        menu.add(0, SELECT_IMAGE_FAIL_MENU_POSITION, 0, getString(R.string.select_image_fail));
        menu.add(0, SELECT_VIDEO_MENU_POSITION, 0, getString(R.string.select_video));
        menu.add(0, SELECT_VIDEO_FAIL_MENU_POSITION, 0, getString(R.string.select_video_fail));
        menu.add(0, SELECT_IMAGE_SLOW_MENU_POSITION, 0, getString(R.string.select_image_slow_network));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Intent intent = new Intent(Intent.ACTION_PICK);

        switch (item.getItemId()) {
            case SELECT_IMAGE_MENU_POSITION:
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent = Intent.createChooser(intent, getString(R.string.select_image));

                startActivityForResult(intent, ADD_MEDIA_ACTIVITY_REQUEST_CODE);
                return true;
            case SELECT_IMAGE_FAIL_MENU_POSITION:
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent = Intent.createChooser(intent, getString(R.string.select_image_fail));

                startActivityForResult(intent, ADD_MEDIA_FAIL_ACTIVITY_REQUEST_CODE);
                return true;
            case SELECT_VIDEO_MENU_POSITION:
                intent.setType("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent = Intent.createChooser(intent, getString(R.string.select_video));

                startActivityForResult(intent, ADD_MEDIA_ACTIVITY_REQUEST_CODE);
                return true;
            case SELECT_VIDEO_FAIL_MENU_POSITION:
                intent.setType("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent = Intent.createChooser(intent, getString(R.string.select_video_fail));

                startActivityForResult(intent, ADD_MEDIA_FAIL_ACTIVITY_REQUEST_CODE);
                return true;
            case SELECT_IMAGE_SLOW_MENU_POSITION:
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent = Intent.createChooser(intent, getString(R.string.select_image_slow_network));

                startActivityForResult(intent, ADD_MEDIA_SLOW_NETWORK_REQUEST_CODE);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null) {
            return;
        }

        Uri mediaUri = data.getData();

        MediaFile mediaFile = new MediaFile();
        String mediaId = String.valueOf(System.currentTimeMillis());
        mediaFile.setMediaId(mediaId);
        mediaFile.setVideo(mediaUri.toString().contains("video"));

        switch (requestCode) {
            case ADD_MEDIA_ACTIVITY_REQUEST_CODE:
                mEditorFragment.appendMediaFile(mediaFile, mediaUri.toString(), null);

                if (mEditorFragment instanceof EditorMediaUploadListener) {
                    simulateFileUpload(mediaId, mediaUri.toString());
                }
                break;
            case ADD_MEDIA_FAIL_ACTIVITY_REQUEST_CODE:
                mEditorFragment.appendMediaFile(mediaFile, mediaUri.toString(), null);

                if (mEditorFragment instanceof EditorMediaUploadListener) {
                    simulateFileUploadFail(mediaId, mediaUri.toString());
                }
                break;
            case ADD_MEDIA_SLOW_NETWORK_REQUEST_CODE:
                mEditorFragment.appendMediaFile(mediaFile, mediaUri.toString(), null);

                if (mEditorFragment instanceof EditorMediaUploadListener) {
                    simulateSlowFileUpload(mediaId, mediaUri.toString());
                }
                break;
        }
    }

    private void simulateFileUpload(final String mediaId, final String mediaUrl) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    float count = (float) 0.1;
                    while (count < 1.1) {
                        sleep(500);

                        ((EditorMediaUploadListener) mEditorFragment).onMediaUploadProgress(mediaId, count);

                        count += 0.1;
                    }

                    MediaFile mediaFile = new MediaFile();
                    mediaFile.setMediaId(MEDIA_REMOTE_ID_SAMPLE);
                    mediaFile.setFileURL(mediaUrl);

                    ((EditorMediaUploadListener) mEditorFragment).onMediaUploadSucceeded(mediaId, mediaFile);

                    if (mFailedUploads.containsKey(mediaId)) {
                        mFailedUploads.remove(mediaId);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }

    private void simulateFileUploadFail(final String mediaId, final String mediaUrl) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    float count = (float) 0.1;
                    while (count < 0.6) {
                        sleep(500);

                        ((EditorMediaUploadListener) mEditorFragment).onMediaUploadProgress(mediaId, count);

                        count += 0.1;
                    }

                    ((EditorMediaUploadListener) mEditorFragment).onMediaUploadFailed(mediaId, EditorFragmentAbstract.MediaType.IMAGE,
                            getString(R.string.tap_to_try_again));

                    mFailedUploads.put(mediaId, mediaUrl);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }

    private void simulateSlowFileUpload(final String mediaId, final String mediaUrl) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(5000);
                    float count = (float) 0.1;
                    while (count < 1.1) {
                        sleep(2000);

                        ((EditorMediaUploadListener) mEditorFragment).onMediaUploadProgress(mediaId, count);

                        count += 0.1;
                    }

                    MediaFile mediaFile = new MediaFile();
                    mediaFile.setMediaId(MEDIA_REMOTE_ID_SAMPLE);
                    mediaFile.setFileURL(mediaUrl);

                    ((EditorMediaUploadListener) mEditorFragment).onMediaUploadSucceeded(mediaId, mediaFile);

                    if (mFailedUploads.containsKey(mediaId)) {
                        mFailedUploads.remove(mediaId);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }

    @Override
    public void onEditorFragmentInitialized() {

    }

    @Override
    public void onEditorFragmentContentReady(ArrayList<Object> unsupportedBlocks) {

    }

    @Override
    public void onAddMediaClicked() {

    }

    @Override
    public void onAddMediaImageClicked() {

    }

    @Override
    public void onAddMediaVideoClicked() {

    }

    @Override
    public void onAddPhotoClicked() {

    }

    @Override
    public void onCapturePhotoClicked() {

    }

    @Override
    public void onAddVideoClicked() {

    }

    @Override
    public void onCaptureVideoClicked() {

    }

    @Override
    public boolean onMediaRetryClicked(String mediaId) {
        return false;
    }

    @Override
    public void onMediaRetryAllClicked(Set<String> mediaIdSet) {

    }

    @Override
    public void onMediaUploadCancelClicked(String mediaId) {

    }

    @Override
    public void onMediaDeleted(String mediaId) {

    }

    @Override
    public void onUndoMediaCheck(String undoedContent) {

    }

    @Override
    public void onVideoPressInfoRequested(String videoId) {

    }

    @Override
    public String onAuthHeaderRequested(String url) {
        return null;
    }

    @Override
    public void onTrackableEvent(TrackableEvent event) {

    }

    @Override
    public void onHtmlModeToggledInToolbar() {

    }

    @Override
    public void onMediaDropped(ArrayList<Uri> mediaUri) {

    }

    @Override
    public void onRequestDragAndDropPermissions(DragEvent dragEvent) {

    }
}
