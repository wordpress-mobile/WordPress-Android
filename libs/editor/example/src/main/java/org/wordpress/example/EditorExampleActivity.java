package org.wordpress.android.editor.example;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import org.wordpress.android.editor.EditorFragmentAbstract.EditorFragmentListener;
import org.wordpress.android.util.ToastUtils;

public class EditorExampleActivity extends ActionBarActivity implements EditorFragmentListener {
    public static final String EDITOR_CHOICE = "EDITOR_CHOICE";
    public static final int USE_NEW_EDITOR = 1;
    public static final int USE_LEGACY_EDITOR = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getIntExtra(EDITOR_CHOICE, USE_NEW_EDITOR) == USE_NEW_EDITOR) {
            ToastUtils.showToast(this, R.string.starting_new_editor);
            setContentView(R.layout.activity_new_editor);
        } else {
            ToastUtils.showToast(this, R.string.starting_legacy_editor);
            setContentView(R.layout.activity_legacy_editor);
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
}
