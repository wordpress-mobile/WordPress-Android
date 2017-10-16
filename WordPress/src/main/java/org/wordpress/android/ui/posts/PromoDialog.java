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
import android.widget.LinearLayout;

import org.wordpress.android.R;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.widgets.WPTextView;

public class PromoDialog extends AppCompatDialogFragment {
    protected static final String KEY_DRAWABLE_RES_ID = "drawableResId";
    protected static final String KEY_TITLE_RES_ID = "titleResId";
    protected static final String KEY_DESCRIPTION_RES_ID = "descriptionResId";
    protected static final String KEY_BUTTON_POSITIVE_RES_ID = "buttonPositiveResId";

    public static class Builder {
        @StringRes int buttonPositiveResId;
        @StringRes int descriptionResId;
        @DrawableRes int drawableResId;
        @StringRes int titleResId;

        public Builder(@DrawableRes int drawableResId, @StringRes int titleResId, @StringRes int descriptionResId,
                       @StringRes int buttonPositiveResId) {
            this.drawableResId = drawableResId;
            this.titleResId = titleResId;
            this.descriptionResId = descriptionResId;
            this.buttonPositiveResId = buttonPositiveResId;
        }

        public PromoDialog build() {
            PromoDialog fragment = new PromoDialog();
            Bundle args = new Bundle();
            args.putInt(KEY_DRAWABLE_RES_ID, drawableResId);
            args.putInt(KEY_TITLE_RES_ID, titleResId);
            args.putInt(KEY_DESCRIPTION_RES_ID, descriptionResId);
            args.putInt(KEY_BUTTON_POSITIVE_RES_ID, buttonPositiveResId);
            fragment.setArguments(args);
            return fragment;
        }
    }

    @StringRes protected int mButtonPositiveResId;
    @StringRes protected int mDescriptionResId;
    @DrawableRes protected int mDrawableResId;
    @StringRes protected int mTitleResId;

    protected View.OnClickListener mPositiveButtonOnClickListener;

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        mDrawableResId = getArguments().getInt(KEY_DRAWABLE_RES_ID);
        mTitleResId = getArguments().getInt(KEY_TITLE_RES_ID);
        mDescriptionResId = getArguments().getInt(KEY_DESCRIPTION_RES_ID);
        mButtonPositiveResId = getArguments().getInt(KEY_BUTTON_POSITIVE_RES_ID);
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
        image.setImageResource(mDrawableResId);
        WPTextView title = (WPTextView) view.findViewById(R.id.promo_dialog_title);
        title.setText(mTitleResId);
        WPTextView desc = (WPTextView) view.findViewById(R.id.promo_dialog_description);
        desc.setText(mDescriptionResId);

        Button btn = (Button) view.findViewById(R.id.promo_dialog_button_positive);
        btn.setText(mButtonPositiveResId);
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

    public void redrawForOrientationChange() {
        LinearLayout imageContainer = (LinearLayout) getView().findViewById(R.id.promo_dialog_image_container);
        imageContainer.setVisibility(DisplayUtils.isLandscape(getActivity()) ? View.GONE : View.VISIBLE);
    }
}
