package org.wordpress.android.editor;

import android.app.Fragment;

public abstract class EditorFragmentInterface extends Fragment {
    public abstract void setTitle(CharSequence text);
    public abstract void setContent(CharSequence text);
}
