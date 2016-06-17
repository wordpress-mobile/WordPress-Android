package org.wordpress.android.editor;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;

import org.wordpress.android.editor.EditorFragmentAbstract.TrackableEvent;
import org.wordpress.android.util.helpers.MediaFile;

public class MockEditorActivity extends AppCompatActivity implements EditorFragmentAbstract.EditorFragmentListener {
    public static final int LAYOUT_ID = 999;

    EditorFragment mEditorFragment;

    @SuppressWarnings("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setId(LAYOUT_ID);
        setContentView(linearLayout);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        mEditorFragment = new EditorFragmentForTests();
        fragmentTransaction.add(linearLayout.getId(), mEditorFragment, "editorFragment");
        fragmentTransaction.commit();
    }

    @Override
    public void onEditorFragmentInitialized() {
        mEditorFragment.setTitle("A title");
        mEditorFragment.setContent(Utils.getHtmlFromFile(this, "example-content.html"));
    }

    @Override
    public void onSettingsClicked() {

    }

    @Override
    public void onAddMediaClicked() {

    }

    @Override
    public void onMediaRetryClicked(String mediaId) {

    }

    @Override
    public void onMediaUploadCancelClicked(String mediaId, boolean delete) {

    }

    @Override
    public void onFeaturedImageChanged(long mediaId) {

    }

    @Override
    public void onVideoPressInfoRequested(String videoId) {

    }

    @Override
    public String onAuthHeaderRequested(String url) {
        return "";
    }

    @Override
    public void saveMediaFile(MediaFile mediaFile) {

    }

    @Override
    public void onTrackableEvent(TrackableEvent event) {

    }
}

