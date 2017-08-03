package org.wordpress.android.ui.posts;

import android.app.Dialog;
import android.os.Bundle;
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
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.widgets.WPTextView;

/**
 * Similar to {@link PromoDialog}, but with an optional link field and negative button.
 */
public class PromoDialogAdvanced extends PromoDialog {
    protected static final String KEY_LINK_RES_ID = "linkResId";
    protected static final String KEY_BUTTON_NEGATIVE_RES_ID = "buttonNegativeResId";

    public static class Builder extends PromoDialog.Builder {
        @StringRes int linkResId;
        @StringRes int buttonNegativeResId;

        public Builder(@DrawableRes int drawableResId, @StringRes int titleResId, @StringRes int descriptionResId,
                       @StringRes int buttonPositiveResId) {
            super(drawableResId, titleResId, descriptionResId, buttonPositiveResId);
        }

        public Builder setLinkText(@StringRes int linkResId) {
            this.linkResId = linkResId;
            return this;
        }

        public Builder setNegativeButtonText(@StringRes int buttonNegativeResId) {
            this.buttonNegativeResId = buttonNegativeResId;
            return this;
        }

        @Override
        public PromoDialogAdvanced build() {
            return PromoDialogAdvanced.newInstance(this);
        }
    }

    @StringRes protected int mButtonNegativeResId;
    @StringRes protected int mLinkResId;

    protected View.OnClickListener mNegativeButtonOnClickListener;
    protected View.OnClickListener mLinkOnClickListener;

    protected static PromoDialogAdvanced newInstance(Builder builder) {
        PromoDialogAdvanced fragment = new PromoDialogAdvanced();
        Bundle args = new Bundle();
        args.putInt(KEY_DRAWABLE_RES_ID, builder.drawableResId);
        args.putInt(KEY_TITLE_RES_ID, builder.titleResId);
        args.putInt(KEY_DESCRIPTION_RES_ID, builder.descriptionResId);
        args.putInt(KEY_LINK_RES_ID, builder.linkResId);
        args.putInt(KEY_BUTTON_NEGATIVE_RES_ID, builder.buttonNegativeResId);
        args.putInt(KEY_BUTTON_POSITIVE_RES_ID, builder.buttonPositiveResId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        mLinkResId = getArguments().getInt(KEY_LINK_RES_ID);
        mButtonNegativeResId = getArguments().getInt(KEY_BUTTON_NEGATIVE_RES_ID);
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.promo_dialog_advanced, container);
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

        WPTextView description = (WPTextView) view.findViewById(R.id.promo_dialog_description);
        description.setText(mDescriptionResId);

        WPTextView link = (WPTextView) view.findViewById(R.id.promo_dialog_link);
        if (mLinkResId != 0) {
            link.setText(mLinkResId);
            link.setOnClickListener(mLinkOnClickListener);
        } else {
            link.setVisibility(View.GONE);
        }

        Button buttonNegative = (Button) view.findViewById(R.id.promo_dialog_button_negative);
        if (mButtonNegativeResId != 0) {
            buttonNegative.setText(mButtonNegativeResId);
            buttonNegative.setOnClickListener(mNegativeButtonOnClickListener);
        } else {
            buttonNegative.setVisibility(View.GONE);
        }

        Button buttonPositive = (Button) view.findViewById(R.id.promo_dialog_button_positive);
        buttonPositive.setText(mButtonPositiveResId);
        buttonPositive.setOnClickListener(mPositiveButtonOnClickListener);
    }

    public void setNegativeButtonOnClickListener(View.OnClickListener listener) {
        mNegativeButtonOnClickListener = listener;
    }

    public void setLinkOnClickListener(View.OnClickListener listener) {
        mLinkOnClickListener = listener;
    }
}
