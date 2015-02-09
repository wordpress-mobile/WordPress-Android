package org.wordpress.android.editor;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import org.wordpress.android.editor.EditorFragmentInterface.EditorFragmentListener;

public class EditorExampleActivity extends ActionBarActivity implements EditorFragmentListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
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
