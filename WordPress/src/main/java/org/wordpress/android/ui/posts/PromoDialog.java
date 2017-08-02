package org.wordpress.android.ui.posts;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

import org.wordpress.android.R;
import org.wordpress.android.widgets.WPTextView;

public class PromoDialog extends AppCompatDialogFragment {
    public static class Builder {
        @StringRes int buttonPositiveId;
        @StringRes int descriptionId;
        @DrawableRes int drawableId;
        @StringRes int titleId;

        public Builder(@DrawableRes int drawableId, @StringRes int titleId, @StringRes int descriptionId,
                       @StringRes int buttonPositiveId) {
            this.drawableId = drawableId;
            this.titleId = titleId;
            this.descriptionId = descriptionId;
            this.buttonPositiveId = buttonPositiveId;
        }

        public PromoDialog build() {
            PromoDialog fragment = new PromoDialog();
            Bundle args = new Bundle();
            args.putInt("drawableId", drawableId);
            args.putInt("titleId", titleId);
            args.putInt("descriptionId", descriptionId);
            args.putInt("buttonPositiveId", buttonPositiveId);
            fragment.setArguments(args);
            return fragment;
        }
    }

    protected int mButtonPositiveId;
    protected int mDescriptionId;
    protected int mDrawableId;
    protected int mTitleId;

    protected View.OnClickListener mPositiveButtonOnClickListener;

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        mDrawableId = getArguments().getInt("drawableId");
        mTitleId = getArguments().getInt("titleId");
        mDescriptionId = getArguments().getInt("descriptionId");
        mButtonPositiveId = getArguments().getInt("buttonPositiveId");
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        return dialog;
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        ((AppCompatDialog) dialog).supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.promo_dialog, container);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageView image = (ImageView) view.findViewById(R.id.promo_dialog_image);
        image.setImageResource(mDrawableId);
        WPTextView title = (WPTextView) view.findViewById(R.id.promo_dialog_title);
        title.setText(mTitleId);
        WPTextView desc = (WPTextView) view.findViewById(R.id.promo_dialog_description);
        desc.setText(mDescriptionId);

        Button btn = (Button) view.findViewById(R.id.promo_dialog_button_positive);
        btn.setText(mButtonPositiveId);
        if (mPositiveButtonOnClickListener == null) {
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getDialog().cancel();
                }
            });
        } else {
            btn.setOnClickListener(mPositiveButtonOnClickListener);
        }
    }

    public void setPositiveButtonOnClickListener(View.OnClickListener listener) {
        mPositiveButtonOnClickListener = listener;
    }
}
