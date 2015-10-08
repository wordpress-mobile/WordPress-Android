package org.wordpress.android.ui.prefs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.widgets.WPSwitch;

public class RelatedPostsDialog extends DialogFragment
        implements CompoundButton.OnCheckedChangeListener {

    /**
     * boolean
     *
     * Sets the default state of the Show Related Posts switch.
     */
    public static final String SHOW_RELATED_POSTS_KEY = "related-posts";

    private WPSwitch mShowRelatedPosts;
    private CheckBox mShowHeader;
    private CheckBox mShowImages;
    private TextView mPreviewHeader;
    private TextView mRelatedPostsListHeader;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setStyle(STYLE_NORMAL, R.style.Calypso_SiteSettingsTheme);
        getDialog().setTitle(R.string.site_settings_related_posts_title);

        View v = inflater.inflate(R.layout.related_posts_dialog, container, false);
        mShowRelatedPosts = (WPSwitch) v.findViewById(R.id.toggle_related_posts_switch);
        mShowHeader = (CheckBox) v.findViewById(R.id.show_header_checkbox);
        mShowImages = (CheckBox) v.findViewById(R.id.show_images_checkbox);
        mPreviewHeader = (TextView) v.findViewById(R.id.preview_header);
        mRelatedPostsListHeader = (TextView) v.findViewById(R.id.related_posts_list_header);

        if (savedInstanceState != null) {
            mShowRelatedPosts.setChecked(savedInstanceState.getBoolean(SHOW_RELATED_POSTS_KEY));
        }

        mShowRelatedPosts.setOnCheckedChangeListener(this);
        mShowHeader.setOnCheckedChangeListener(this);
        mShowImages.setOnCheckedChangeListener(this);

        toggleViews(mShowRelatedPosts.isChecked());

        return v;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mShowRelatedPosts) {
            toggleViews(isChecked);
        } else if (buttonView == mShowHeader) {
        } else if (buttonView == mShowImages) {
        }
    }

    private void toggleViews(boolean enabled) {
        mShowHeader.setEnabled(enabled);
        mShowImages.setEnabled(enabled);
        mPreviewHeader.setEnabled(enabled);
        mRelatedPostsListHeader.setEnabled(enabled);
    }
}
