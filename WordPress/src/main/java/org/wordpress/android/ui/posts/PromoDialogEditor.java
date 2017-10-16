package org.wordpress.android.ui.posts;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.widgets.WPTextView;

public class PromoDialogEditor extends PromoDialogAdvanced {
    protected static final String KEY_TITLE_BETA_RES_ID = "titleBetaResId";

    public static class Builder extends PromoDialogAdvanced.Builder {
        @StringRes private int titleBetaResId;

        public Builder(@DrawableRes int drawableResId, @StringRes int titleResId, @StringRes int descriptionResId,
                       @StringRes int buttonPositiveResId) {
            super(drawableResId, titleResId, descriptionResId, buttonPositiveResId);
        }

        public Builder setLinkText(@StringRes int linkResId) {
            return (Builder) super.setLinkText(linkResId);
        }

        public Builder setNegativeButtonText(@StringRes int buttonNegativeResId) {
            return (Builder) super.setNegativeButtonText(buttonNegativeResId);
        }

        public Builder setTitleBetaText(@StringRes int titleBetaResId) {
            this.titleBetaResId = titleBetaResId;
            return this;
        }

        @Override
        public PromoDialogEditor build() {
            return PromoDialogEditor.newInstance(this);
        }
    }

    @StringRes protected int mTitleBetaId;

    protected static PromoDialogEditor newInstance(Builder builder) {
        PromoDialogEditor fragment = new PromoDialogEditor();
        Bundle args = new Bundle();
        args.putInt(KEY_DRAWABLE_RES_ID, builder.drawableResId);
        args.putInt(KEY_TITLE_RES_ID, builder.titleResId);
        args.putInt(KEY_TITLE_BETA_RES_ID, builder.titleBetaResId);
        args.putInt(KEY_DESCRIPTION_RES_ID, builder.descriptionResId);
        args.putInt(KEY_LINK_RES_ID, builder.linkResId);
        args.putInt(KEY_BUTTON_NEGATIVE_RES_ID, builder.buttonNegativeResId);
        args.putInt(KEY_BUTTON_POSITIVE_RES_ID, builder.buttonPositiveResId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        mTitleBetaId = getArguments().getInt(KEY_TITLE_BETA_RES_ID);
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.promo_dialog_editor, container);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView image = (ImageView) view.findViewById(R.id.promo_dialog_image);
        image.setImageResource(mDrawableResId);
        LinearLayout imageContainer = (LinearLayout) view.findViewById(R.id.promo_dialog_image_container);
        imageContainer.setVisibility(DisplayUtils.isLandscape(getActivity()) ? View.GONE : View.VISIBLE);

        WPTextView title = (WPTextView) view.findViewById(R.id.promo_dialog_title);
        title.setText(mTitleResId);

        WPTextView titleBeta = (WPTextView) view.findViewById(R.id.promo_dialog_title_beta);
        titleBeta.setText(mTitleBetaId);

        WPTextView description = (WPTextView) view.findViewById(R.id.promo_dialog_description);
        description.setText(mDescriptionResId);

        WPTextView link = (WPTextView) view.findViewById(R.id.promo_dialog_link);
        link.setText(mLinkResId);
        if (mLinkOnClickListener == null) {
            link.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActivityLauncher.showAztecEditorReleaseNotes(getActivity());
                    AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_AZTEC_PROMO_LINK);
                }
            });
        } else {
            link.setOnClickListener(mLinkOnClickListener);
        }

        Button buttonNegative = (Button) view.findViewById(R.id.promo_dialog_button_negative);
        buttonNegative.setText(mButtonNegativeResId);
        if (mNegativeButtonOnClickListener == null) {
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getDialog().cancel();
                    AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_AZTEC_PROMO_NEGATIVE);
                }
            });
        } else {
            buttonNegative.setOnClickListener(mNegativeButtonOnClickListener);
        }

        Button buttonPositive = (Button) view.findViewById(R.id.promo_dialog_button_positive);
        buttonPositive.setText(mButtonPositiveResId);
        if (mPositiveButtonOnClickListener == null) {
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getDialog().cancel();

                    // Set Aztec enabled and Visual disabled if Aztec is not already enabled.
                    if (!AppPrefs.isAztecEditorEnabled()) {
                        AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_AZTEC_PROMO_POSITIVE);
                        AppPrefs.setAztecEditorEnabled(true);
                        AppPrefs.setVisualEditorEnabled(false);
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        preferences.edit().putString(getString(R.string.pref_key_editor_type), "2").apply();
                    }

                    ActivityLauncher.addNewPostOrPageForResult(getActivity(),
                            ((WPMainActivity) getActivity()).getSelectedSite(), false, true);
                }
            });
        } else {
            buttonPositive.setOnClickListener(mPositiveButtonOnClickListener);
        }
    }
}
