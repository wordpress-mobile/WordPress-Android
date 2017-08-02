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
    public static class Builder extends PromoDialog.Builder {
        @StringRes int linkId;
        @StringRes int buttonNegativeId;

        public Builder(@DrawableRes int drawableId, @StringRes int titleId, @StringRes int descriptionId,
                       @StringRes int buttonPositiveId) {
            super(drawableId, titleId, descriptionId, buttonPositiveId);
        }

        public Builder setLinkText(@StringRes int linkId) {
            this.linkId = linkId;
            return this;
        }

        public Builder setNegativeButtonText(@StringRes int buttonNegativeId) {
            this.buttonNegativeId = buttonNegativeId;
            return this;
        }

        @Override
        public PromoDialogAdvanced build() {
            return PromoDialogAdvanced.newInstance(this);
        }
    }

    protected int mButtonNegativeId;
    protected int mLinkId;

    protected View.OnClickListener mNegativeButtonOnClickListener;
    protected View.OnClickListener mLinkOnClickListener;

    protected static PromoDialogAdvanced newInstance(Builder builder) {
        PromoDialogAdvanced fragment = new PromoDialogAdvanced();
        Bundle args = new Bundle();
        args.putInt("drawableId", builder.drawableId);
        args.putInt("titleId", builder.titleId);
        args.putInt("descriptionId", builder.descriptionId);
        args.putInt("linkId", builder.linkId);
        args.putInt("buttonNegativeId", builder.buttonNegativeId);
        args.putInt("buttonPositiveId", builder.buttonPositiveId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        mLinkId = getArguments().getInt("linkId");
        mButtonNegativeId = getArguments().getInt("buttonNegativeId");
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
        image.setImageResource(mDrawableId);
        LinearLayout imageContainer = (LinearLayout) view.findViewById(R.id.promo_dialog_image_container);
        imageContainer.setVisibility(DisplayUtils.isLandscape(getActivity()) ? View.GONE : View.VISIBLE);

        WPTextView title = (WPTextView) view.findViewById(R.id.promo_dialog_title);
        title.setText(mTitleId);

        WPTextView description = (WPTextView) view.findViewById(R.id.promo_dialog_description);
        description.setText(mDescriptionId);

        WPTextView link = (WPTextView) view.findViewById(R.id.promo_dialog_link);
        if (mLinkId != 0) {
            link.setText(mLinkId);
            link.setOnClickListener(mLinkOnClickListener);
        } else {
            link.setVisibility(View.GONE);
        }

        Button buttonNegative = (Button) view.findViewById(R.id.promo_dialog_button_negative);
        if (mButtonNegativeId != 0) {
            buttonNegative.setText(mButtonNegativeId);
            buttonNegative.setOnClickListener(mNegativeButtonOnClickListener);
        } else {
            buttonNegative.setVisibility(View.GONE);
        }

        Button buttonPositive = (Button) view.findViewById(R.id.promo_dialog_button_positive);
        buttonPositive.setText(mButtonPositiveId);
        buttonPositive.setOnClickListener(mPositiveButtonOnClickListener);
    }

    public void setNegativeButtonOnClickListener(View.OnClickListener listener) {
        mNegativeButtonOnClickListener = listener;
    }

    public void setLinkOnClickListener(View.OnClickListener listener) {
        mLinkOnClickListener = listener;
    }
}
