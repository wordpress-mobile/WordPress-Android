package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.wordpress.android.R;

import java.util.ArrayList;
import java.util.List;

public class RelatedPostsDialog extends DialogFragment
        implements DialogInterface.OnClickListener,
        CompoundButton.OnCheckedChangeListener {
    /**
     * boolean
     * <p>
     * Sets the default state of the Show Related Posts switch. The switch is off by default.
     */
    public static final String SHOW_RELATED_POSTS_KEY = "related-posts";

    /**
     * boolean
     * <p>
     * Sets the default state of the Show Headers checkbox. The checkbox is off by default.
     */
    public static final String SHOW_HEADER_KEY = "show-header";

    /**
     * boolean
     * <p>
     * Sets the default state of the Show Images checkbox. The checkbox is off by default.
     */
    public static final String SHOW_IMAGES_KEY = "show-images";

    private SwitchCompat mShowRelatedPosts;
    private CheckBox mShowHeader;
    private CheckBox mShowImages;
    private TextView mPreviewHeader;
    private TextView mRelatedPostsListHeader;
    private LinearLayout mRelatedPostsList;
    private List<ImageView> mPreviewImages;
    private boolean mConfirmed;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        //noinspection InflateParams
        View v = inflater.inflate(R.layout.related_posts_dialog, null);

        mShowRelatedPosts = v.findViewById(R.id.toggle_related_posts_switch);
        mShowHeader = v.findViewById(R.id.show_header_checkbox);
        mShowImages = v.findViewById(R.id.show_images_checkbox);
        mPreviewHeader = v.findViewById(R.id.preview_header);
        mRelatedPostsListHeader = v.findViewById(R.id.related_posts_list_header);
        mRelatedPostsList = v.findViewById(R.id.related_posts_list);

        mPreviewImages = new ArrayList<>();
        mPreviewImages.add(v.findViewById(R.id.related_post_image1));
        mPreviewImages.add(v.findViewById(R.id.related_post_image2));
        mPreviewImages.add(v.findViewById(R.id.related_post_image3));

        Bundle args = getArguments();
        if (args != null) {
            mShowRelatedPosts.setChecked(args.getBoolean(SHOW_RELATED_POSTS_KEY));
            mShowHeader.setChecked(args.getBoolean(SHOW_HEADER_KEY));
            mShowImages.setChecked(args.getBoolean(SHOW_IMAGES_KEY));
        }

        toggleShowHeader(mShowHeader.isChecked());
        toggleShowImages(mShowImages.isChecked());

        mShowRelatedPosts.setOnCheckedChangeListener(this);
        mShowHeader.setOnCheckedChangeListener(this);
        mShowImages.setOnCheckedChangeListener(this);

        toggleViews(mShowRelatedPosts.isChecked());

        int topOffset = getResources().getDimensionPixelOffset(R.dimen.settings_fragment_dialog_vertical_inset);

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity())
                .setBackgroundInsetTop(topOffset)
                .setBackgroundInsetBottom(topOffset);
        //noinspection InflateParams
        View titleView = inflater.inflate(R.layout.detail_list_preference_title, null);
        TextView titleText = titleView.findViewById(R.id.title);
        titleText.setText(R.string.site_settings_related_posts_title);
        titleText.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));
        builder.setCustomTitle(titleView);
        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNegativeButton(R.string.cancel, this);
        builder.setView(v);

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mConfirmed = which == DialogInterface.BUTTON_POSITIVE;
        dismiss();
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
        // TODO: android.app.Fragment  is deprecated since Android P.
        // Needs to be replaced with android.support.v4.app.Fragment
        // See https://developer.android.com/reference/android/app/Fragment
        android.app.Fragment target = getTargetFragment();
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
        if (mConfirmed) {
            return new Intent()
                    .putExtra(SHOW_RELATED_POSTS_KEY, mShowRelatedPosts.isChecked())
                    .putExtra(SHOW_HEADER_KEY, mShowHeader.isChecked())
                    .putExtra(SHOW_IMAGES_KEY, mShowImages.isChecked());
        }

        return null;
    }

    private void toggleViews(boolean enabled) {
        mShowHeader.setEnabled(enabled);
        mShowImages.setEnabled(enabled);
        mPreviewHeader.setEnabled(enabled);
        mRelatedPostsListHeader.setEnabled(enabled);

        if (enabled) {
            mRelatedPostsList.setAlpha(1.0f);
        } else {
            mRelatedPostsList.setAlpha(0.5f);
        }
    }
}
