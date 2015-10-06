package org.wordpress.android.ui.prefs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;

public class RelatedPostsDialog extends DialogFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setStyle(STYLE_NORMAL, R.style.Calypso_SiteSettingsTheme);
        getDialog().setTitle(R.string.site_settings_related_posts_title);

        return inflater.inflate(R.layout.related_posts_dialog, container, false);
    }
}
