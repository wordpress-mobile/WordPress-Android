package org.wordpress.android.editor.example;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

import org.wordpress.android.editor.EditorFragmentAbstract;
import org.wordpress.android.editor.EditorFragmentAbstract.EditorFragmentListener;
import org.wordpress.android.editor.EditorMediaUploadListener;
import org.wordpress.android.editor.ImageSettingsDialogFragment;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.MediaFile;

import java.util.HashMap;
import java.util.Map;

public class EditorExampleActivity extends AppCompatActivity implements EditorFragmentListener {
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

    public static final String MEDIA_REMOTE_ID_SAMPLE = "123";

    private static final int SELECT_PHOTO_MENU_POSITION = 0;
    private static final int SELECT_PHOTO_FAIL_MENU_POSITION = 1;

    private EditorFragmentAbstract mEditorFragment;

    private Map<String, String> mFailedUploads;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getIntExtra(EDITOR_PARAM, USE_NEW_EDITOR) == USE_NEW_EDITOR) {
            ToastUtils.showToast(this, R.string.starting_new_editor);
            setContentView(R.layout.activity_new_editor);
        } else {
            ToastUtils.showToast(this, R.string.starting_legacy_editor);
            setContentView(R.layout.activity_legacy_editor);
        }

        mFailedUploads = new HashMap<>();
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof EditorFragmentAbstract) {
            mEditorFragment = (EditorFragmentAbstract) fragment;
        }
    }

    @Override
    public void onBackPressed() {
        Fragment fragment =  getFragmentManager()
                .findFragmentByTag(ImageSettingsDialogFragment.IMAGE_SETTINGS_DIALOG_TAG);
        if (fragment != null && fragment.isVisible()) {
            ((ImageSettingsDialogFragment) fragment).dismissFragment();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(0, SELECT_PHOTO_MENU_POSITION, 0, getString(R.string.select_photo));
        menu.add(0, SELECT_PHOTO_FAIL_MENU_POSITION, 0, getString(R.string.select_photo_fail));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Intent intent = new Intent(Intent.ACTION_PICK);

        switch (item.getItemId()) {
            case SELECT_PHOTO_MENU_POSITION:
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent = Intent.createChooser(intent, getString(R.string.select_photo));

                startActivityForResult(intent, ADD_MEDIA_ACTIVITY_REQUEST_CODE);
                return true;
            case SELECT_PHOTO_FAIL_MENU_POSITION:
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent = Intent.createChooser(intent, getString(R.string.select_photo_fail));

                startActivityForResult(intent, ADD_MEDIA_FAIL_ACTIVITY_REQUEST_CODE);
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

        Uri imageUri = data.getData();

        MediaFile mediaFile = new MediaFile();
        String mediaId = String.valueOf(System.currentTimeMillis());
        mediaFile.setMediaId(mediaId);

        switch (requestCode) {
            case ADD_MEDIA_ACTIVITY_REQUEST_CODE:
                mEditorFragment.appendMediaFile(mediaFile, imageUri.toString(), null);

                if (mEditorFragment instanceof EditorMediaUploadListener) {
                    simulateFileUpload(mediaId, imageUri.toString());
                }
                break;
            case ADD_MEDIA_FAIL_ACTIVITY_REQUEST_CODE:
                mEditorFragment.appendMediaFile(mediaFile, imageUri.toString(), null);

                if (mEditorFragment instanceof EditorMediaUploadListener) {
                    simulateFileUploadFail(mediaId, imageUri.toString());
                }
        }
    }

    @Override
    public void onSettingsClicked() {
        // TODO
    }

    @Override
    public void onAddMediaClicked() {
        // TODO
    }

    @Override
    public void onMediaRetryClicked(String mediaId) {
        if (mFailedUploads.containsKey(mediaId)) {
            simulateFileUpload(mediaId, mFailedUploads.get(mediaId));
        }
    }

    @Override
    public void onMediaUploadCancelClicked(String mediaId, boolean delete) {

    }

    @Override
    public void onFeaturedImageChanged(int mediaId) {

    }

    @Override
    public void onVideoPressInfoRequested(String videoId) {

    }

    @Override
    public String onAuthHeaderRequested(String url) {
        return "";
    }

    @Override
    public void onEditorFragmentInitialized() {
        // arbitrary setup
        mEditorFragment.setFeaturedImageSupported(true);
        mEditorFragment.setBlogSettingMaxImageWidth("600");
        mEditorFragment.setDebugModeEnabled(true);

        // get title and content and draft switch
        String title = getIntent().getStringExtra(TITLE_PARAM);
        String content = getIntent().getStringExtra(CONTENT_PARAM);
        boolean isLocalDraft = getIntent().getBooleanExtra(DRAFT_PARAM, true);
        mEditorFragment.setTitle(title);
        mEditorFragment.setContent(content);
        mEditorFragment.setTitlePlaceholder(getIntent().getStringExtra(TITLE_PLACEHOLDER_PARAM));
        mEditorFragment.setContentPlaceholder(getIntent().getStringExtra(CONTENT_PLACEHOLDER_PARAM));
        mEditorFragment.setLocalDraft(isLocalDraft);
    }

    @Override
    public void saveMediaFile(MediaFile mediaFile) {
        // TODO
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

                    ((EditorMediaUploadListener) mEditorFragment).onMediaUploadSucceeded(mediaId,
                            MEDIA_REMOTE_ID_SAMPLE, mediaUrl);

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

                    ((EditorMediaUploadListener) mEditorFragment).onMediaUploadFailed(mediaId);

                    mFailedUploads.put(mediaId, mediaUrl);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }
}
