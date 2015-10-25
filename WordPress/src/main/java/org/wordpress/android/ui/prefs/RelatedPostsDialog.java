package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
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
     * Sets the default state of the Show Related Posts switch. The switch is off by default.
     */
    public static final String SHOW_RELATED_POSTS_KEY = "related-posts";

    /**
     * boolean
     *
     * Sets the default state of the Show Headers checkbox. The checkbox is off by default.
     */
    public static final String SHOW_HEADER_KEY = "show-header";

    /**
     * boolean
     *
     * Sets the default state of the Show Images checkbox. The checkbox is off by default.
     */
    public static final String SHOW_IMAGES_KEY = "show-images";

    private WPSwitch mShowRelatedPosts;
    private CheckBox mShowHeader;
    private CheckBox mShowImages;
    private TextView mPreviewHeader;
    private TextView mRelatedPostsListHeader;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        setStyle(STYLE_NORMAL, R.style.Calypso_SiteSettingsTheme);
        getDialog().setTitle(R.string.site_settings_related_posts_title);

        View v = inflater.inflate(R.layout.related_posts_dialog, root, false);
        mShowRelatedPosts = (WPSwitch) v.findViewById(R.id.toggle_related_posts_switch);
        mShowHeader = (CheckBox) v.findViewById(R.id.show_header_checkbox);
        mShowImages = (CheckBox) v.findViewById(R.id.show_images_checkbox);
        mPreviewHeader = (TextView) v.findViewById(R.id.preview_header);
        mRelatedPostsListHeader = (TextView) v.findViewById(R.id.related_posts_list_header);

        Bundle args = getArguments();
        if (args != null) {
            mShowRelatedPosts.setChecked(args.getBoolean(SHOW_RELATED_POSTS_KEY));
            mShowHeader.setChecked(args.getBoolean(SHOW_HEADER_KEY));
            mShowImages.setChecked(args.getBoolean(SHOW_IMAGES_KEY));
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
            // TODO update preview
        } else if (buttonView == mShowImages) {
            // TODO update preview
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Fragment target = getTargetFragment();
        if (target != null) {
            target.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, getResultIntent());
        }

        super.onDismiss(dialog);
    }

    private Intent getResultIntent() {
        Intent intent = new Intent();
        intent.putExtra(SHOW_RELATED_POSTS_KEY, mShowRelatedPosts.isChecked());
        intent.putExtra(SHOW_HEADER_KEY, mShowHeader.isChecked());
        intent.putExtra(SHOW_IMAGES_KEY, mShowImages.isChecked());
        return intent;
    }

    private void toggleViews(boolean enabled) {
        mShowHeader.setEnabled(enabled);
        mShowImages.setEnabled(enabled);
        mPreviewHeader.setEnabled(enabled);
        mRelatedPostsListHeader.setEnabled(enabled);
    }
}
