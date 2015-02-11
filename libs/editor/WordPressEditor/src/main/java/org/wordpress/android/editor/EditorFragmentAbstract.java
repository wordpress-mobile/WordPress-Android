package org.wordpress.android.editor;

import android.app.Activity;
import android.app.Fragment;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.helpers.MediaFile;

public abstract class EditorFragmentAbstract extends Fragment {
    public abstract void setTitle(CharSequence text);
    public abstract void setContent(CharSequence text);
    public abstract CharSequence getTitle();
    public abstract CharSequence getContent();
    public abstract void appendMediaFile(MediaFile mediaFile, String imageUrl, ImageLoader imageLoader);
    public abstract void createLinkFromSelection(String link, String text);

    protected EditorFragmentListener mEditorFragmentListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mEditorFragmentListener = (EditorFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement EditorFragmentListener");
        }
    }

    /**
     * Called by the activity when back button is pressed.
     */
    public boolean onBackPressed() {
        return false;
    }

    /**
     * The editor may need to differentiate local draft and published articles
     *
     * @param isLocalDraft edited post is a local draft
     */
    public void setLocalDraft(boolean isLocalDraft) {
        // Not unused in the new editor
    }

    public interface EditorFragmentListener {
        public void onSettingsClicked();
        public void onAddMediaButtonClicked();
    }
}
