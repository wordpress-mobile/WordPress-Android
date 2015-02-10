package org.wordpress.android.editor;

import android.app.Activity;
import android.app.Fragment;

public abstract class EditorFragmentAbstract extends Fragment {
    public abstract void setTitle(CharSequence text);
    public abstract void setContent(CharSequence text);
    public abstract CharSequence getTitle();
    public abstract CharSequence getContent();

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

    public interface EditorFragmentListener {
        public void onSettingsClicked();
        public void onAddMediaButtonClicked();
    }
}
