package org.wordpress.android.ui.posts;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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

public class PromoDialogEditor extends PromoDialog {
    protected int mButtonNegativeId;
    protected int mLinkId;
    protected int mTitleBetaId;

    public static PromoDialogEditor newInstance(int drawableId, int titleId, int titleBetaId, int descriptionId, int linkId, int buttonNegativeId, int buttonPositiveId) {
        PromoDialogEditor fragment = new PromoDialogEditor();
        Bundle args = new Bundle();
        args.putInt("drawableId", drawableId);
        args.putInt("titleId", titleId);
        args.putInt("titleBetaId", titleBetaId);
        args.putInt("descriptionId", descriptionId);
        args.putInt("linkId", linkId);
        args.putInt("buttonNegativeId", buttonNegativeId);
        args.putInt("buttonPositiveId", buttonPositiveId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        mDrawableId = getArguments().getInt("drawableId");
        mTitleId = getArguments().getInt("titleId");
        mTitleBetaId = getArguments().getInt("titleBetaId");
        mDescriptionId = getArguments().getInt("descriptionId");
        mLinkId = getArguments().getInt("linkId");
        mButtonNegativeId = getArguments().getInt("buttonNegativeId");
        mButtonPositiveId = getArguments().getInt("buttonPositiveId");
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);  // Request window without title.
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        return dialog;
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
        link.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(getActivity(), EditorReleaseNotesActivity.class));
                    AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_AZTEC_PROMO_LINK);
                }
            }
        );

        Button buttonNegative = (Button) view.findViewById(R.id.promo_dialog_button_negative);
        buttonNegative.setText(mButtonNegativeId);
        buttonNegative.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getDialog().cancel();
                    AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_AZTEC_PROMO_NEGATIVE);
                }
            }
        );

        Button buttonPositive = (Button) view.findViewById(R.id.promo_dialog_button_positive);
        buttonPositive.setText(mButtonPositiveId);
        buttonPositive.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getDialog().cancel();

                    // Set Aztec enabled and Visual disabled if Aztec is not already enabled.
                    if (!AppPrefs.isAztecEditorEnabled()) {
                        AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_AZTEC_PROMO_POSITIVE);
                        AppPrefs.setAztecEditorEnabled(true);
                        AppPrefs.setVisualEditorEnabled(false);
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        preferences.edit().putString(getString(R.string.pref_key_editor_type), "2").apply();
                    }

                    ActivityLauncher.addNewPostOrPageForResult(getActivity(), ((WPMainActivity) getActivity()).getSelectedSite(), false, true);
                }
            }
        );
    }
}
