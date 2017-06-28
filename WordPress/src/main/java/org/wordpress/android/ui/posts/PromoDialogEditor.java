package org.wordpress.android.ui.posts;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

import org.wordpress.android.R;
import org.wordpress.android.widgets.WPTextView;

public class PromoDialogEditor extends PromoDialog {
    protected int mButtonNegativeLabelId;
    protected int mButtonPositiveLabelId;
    protected int mLinkId;
    protected int mTitleBetaId;

    public static PromoDialogEditor newInstance(int drawableId, int titleId, int titleBetaId, int descriptionId, int linkId, int buttonNegativeLabelId, int buttonPositiveLabelId) {
        PromoDialogEditor fragment = new PromoDialogEditor();
        Bundle args = new Bundle();
        args.putInt("drawableId", drawableId);
        args.putInt("titleId", titleId);
        args.putInt("titleBetaId", titleBetaId);
        args.putInt("descriptionId", descriptionId);
        args.putInt("linkId", linkId);
        args.putInt("buttonNegativeLabelId", buttonNegativeLabelId);
        args.putInt("buttonPositiveLabelId", buttonPositiveLabelId);
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
        mButtonNegativeLabelId = getArguments().getInt("buttonNegativeLabelId");
        mButtonPositiveLabelId = getArguments().getInt("buttonPositiveLabelId");
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

        WPTextView title = (WPTextView) view.findViewById(R.id.promo_dialog_title);
        title.setText(mTitleId);

        WPTextView titleBeta = (WPTextView) view.findViewById(R.id.promo_dialog_title_beta);
        titleBeta.setText(mTitleBetaId);

        WPTextView description = (WPTextView) view.findViewById(R.id.promo_dialog_description);
        description.setText(mDescriptionId);

        WPTextView link = (WPTextView) view.findViewById(R.id.promo_dialog_link);
        link.setText(mLinkId);
        link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        Button buttonNegative = (Button) view.findViewById(R.id.promo_dialog_button_negative);
        buttonNegative.setText(mButtonNegativeLabelId);
        buttonNegative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().cancel();
            }
        });

        Button buttonPositive = (Button) view.findViewById(R.id.promo_dialog_button_positive);
        buttonPositive.setText(mButtonPositiveLabelId);
        buttonPositive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().cancel();
            }
        });
    }
}
