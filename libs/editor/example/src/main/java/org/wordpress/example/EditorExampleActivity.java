package org.wordpress.android.editor.example;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import org.wordpress.android.editor.EditorFragmentAbstract;
import org.wordpress.android.editor.EditorFragmentAbstract.EditorFragmentListener;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.MediaFile;

public class EditorExampleActivity extends ActionBarActivity implements EditorFragmentListener {
    public static final String EDITOR_PARAM = "EDITOR_PARAM";
    public static final String TITLE_PARAM = "TITLE_PARAM";
    public static final String CONTENT_PARAM = "CONTENT_PARAM";
    public static final int USE_NEW_EDITOR = 1;
    public static final int USE_LEGACY_EDITOR = 2;

    private EditorFragmentAbstract mEditorFragment;

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
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof EditorFragmentAbstract) {
            mEditorFragment = (EditorFragmentAbstract) fragment;
        }
    }

    @Override
    public void onSettingsClicked() {
        // TODO
    }

    @Override
    public void onAddMediaButtonClicked() {
        // TODO
    }

    @Override
    public void onEditorFragmentInitialized() {
        // arbitrary setup
        mEditorFragment.setLocalDraft(false);
        mEditorFragment.setFeaturedImageSupported(true);
        mEditorFragment.setBlogSettingMaxImageWidth("600");

        // set title and content
        String title = getIntent().getStringExtra(TITLE_PARAM);
        String content = getIntent().getStringExtra(CONTENT_PARAM);
        mEditorFragment.setTitle(title);
        mEditorFragment.setContent(content);
    }

    @Override
    public void saveMediaFile(MediaFile mediaFile) {
        // TODO
    }
}
