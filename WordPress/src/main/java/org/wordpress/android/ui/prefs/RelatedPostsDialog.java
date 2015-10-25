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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.widgets.WPSwitch;

import java.util.ArrayList;
import java.util.List;

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
    private LinearLayout mRelatedPostsList;
    private List<ImageView> mPreviewImages;

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
        mRelatedPostsList = (LinearLayout) v.findViewById(R.id.related_posts_list);

        Bundle args = getArguments();
        if (args != null) {
            mShowRelatedPosts.setChecked(args.getBoolean(SHOW_RELATED_POSTS_KEY));
            mShowHeader.setChecked(args.getBoolean(SHOW_HEADER_KEY));
            mShowImages.setChecked(args.getBoolean(SHOW_IMAGES_KEY));
        }

        mPreviewImages = new ArrayList<>();
        mPreviewImages.add((ImageView) v.findViewById(R.id.related_post_image1));
        mPreviewImages.add((ImageView) v.findViewById(R.id.related_post_image2));
        mPreviewImages.add((ImageView) v.findViewById(R.id.related_post_image3));

//        RelatedPostsAdapter adapter = new RelatedPostsAdapter(getActivity(), R.layout.related_post);
//        adapter.add(new RelatedPostItem("", R.drawable.rppreview1));Big iPhone/iPad Update Now Available", "
//        adapter.add(new RelatedPostItem("The WordPress for Android App Gets a Big Facelift", "in \"Apps\"", R.drawable.rppreview2));
//        adapter.add(new RelatedPostItem("Upgrade Focus: VideoPress For Weddings", "in \"Upgrade\"", R.drawable.rppreview3));
//        mRelatedPostsList.setAdapter(adapter);

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
            toggleShowHeader(isChecked);
        } else if (buttonView == mShowImages) {
            toggleShowImages(isChecked);
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

    private void toggleShowHeader(boolean show) {
        if (show) {
            mRelatedPostsListHeader.setVisibility(View.VISIBLE);
        } else {
            mRelatedPostsListHeader.setVisibility(View.GONE);
        }
    }

    private void toggleShowImages(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        for (ImageView view : mPreviewImages) {
            view.setVisibility(visibility);
        }
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
        mRelatedPostsList.setEnabled(enabled);
    }
}
