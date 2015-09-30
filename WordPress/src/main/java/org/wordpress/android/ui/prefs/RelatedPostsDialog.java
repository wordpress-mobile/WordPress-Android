package org.wordpress.android.ui.prefs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;

public class RelatedPostsDialog extends DialogFragment {
    public RelatedPostsDialog() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.related_posts_dialog, container, false);

        return v;
    }
}
