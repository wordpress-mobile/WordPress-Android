package org.wordpress.android.ui.posts;

import android.app.Dialog;
import android.content.Intent;
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

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.EditorReleaseNotesActivity;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.widgets.WPTextView;

public class PromoDialogEditor extends PromoDialogAdvanced {
    public static class Builder extends PromoDialogAdvanced.Builder {
        @StringRes private int titleBetaId;

        public Builder(@DrawableRes int drawableId, @StringRes int titleId, @StringRes int descriptionId,
                       @StringRes int buttonPositiveId) {
            super(drawableId, titleId, descriptionId, buttonPositiveId);
        }

        public Builder setLinkText(@StringRes int linkId) {
            return (Builder) super.setLinkText(linkId);
        }

        public Builder setNegativeButtonText(@StringRes int buttonNegativeId) {
            return (Builder) super.setNegativeButtonText(buttonNegativeId);
        }

        public Builder setTitleBetaText(@StringRes int titleBetaId) {
            this.titleBetaId = titleBetaId;
            return this;
        }

        @Override
        public PromoDialogEditor build() {
            return PromoDialogEditor.newInstance(this);
        }
    }

    protected int mTitleBetaId;

    protected static PromoDialogEditor newInstance(Builder builder) {
        PromoDialogEditor fragment = new PromoDialogEditor();
        Bundle args = new Bundle();
        args.putInt("drawableId", builder.drawableId);
        args.putInt("titleId", builder.titleId);
        args.putInt("titleBetaId", builder.titleBetaId);
        args.putInt("descriptionId", builder.descriptionId);
        args.putInt("linkId", builder.linkId);
        args.putInt("buttonNegativeId", builder.buttonNegativeId);
        args.putInt("buttonPositiveId", builder.buttonPositiveId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        mTitleBetaId = getArguments().getInt("titleBetaId");
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
        image.setImageResource(mDrawableId);
        image.setVisibility(DisplayUtils.isLandscape(getActivity()) ? View.GONE : View.VISIBLE);

        WPTextView title = (WPTextView) view.findViewById(R.id.promo_dialog_title);
        title.setText(mTitleId);

        WPTextView titleBeta = (WPTextView) view.findViewById(R.id.promo_dialog_title_beta);
        titleBeta.setText(mTitleBetaId);

        WPTextView description = (WPTextView) view.findViewById(R.id.promo_dialog_description);
        description.setText(mDescriptionId);

        WPTextView link = (WPTextView) view.findViewById(R.id.promo_dialog_link);
        link.setText(mLinkId);
        if (mLinkOnClickListener == null) {
            link.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(getActivity(), EditorReleaseNotesActivity.class));
                    AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_AZTEC_PROMO_LINK);
                }
            });
        } else {
            link.setOnClickListener(mLinkOnClickListener);
        }

        Button buttonNegative = (Button) view.findViewById(R.id.promo_dialog_button_negative);
        buttonNegative.setText(mButtonNegativeId);
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
        buttonPositive.setText(mButtonPositiveId);
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
