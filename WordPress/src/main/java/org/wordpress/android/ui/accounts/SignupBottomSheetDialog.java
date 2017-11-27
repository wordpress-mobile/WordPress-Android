package org.wordpress.android.ui.accounts;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.WPBottomSheetDialog;

public class SignupBottomSheetDialog extends WPBottomSheetDialog {
    private Resources mResources;
    private TextView mTermsOfServiceText;

    public SignupBottomSheetDialog(@NonNull final AppCompatActivity activity, @NonNull final SignupSheetListener signupSheetListener) {
        super(activity);
        final View layout = LayoutInflater.from(activity).inflate(R.layout.signup_bottom_sheet_dialog, null, false);

        mResources = activity.getResources();

        mTermsOfServiceText = (TextView) layout.findViewById(R.id.signup_tos);
        mTermsOfServiceText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signupSheetListener.onSignupSheetTermsOfServiceClicked();
            }
        });

        Button signupWithEmailButton = (Button) layout.findViewById(R.id.signup_email);
        signupWithEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signupSheetListener.onSignupSheetEmailClicked();
            }
        });

        Button signupWithGoogleButton = (Button) layout.findViewById(R.id.signup_google);
        signupWithGoogleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signupSheetListener.onSignupSheetGoogleClicked();
            }
        });

        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                signupSheetListener.onSignupSheetDismissed();
            }
        });

        setContentView(layout);

        // Set peek height to full height of view to avoid signup buttons being off screen when
        // bottom sheet is shown with small screen height (e.g. landscape orientation).
        final BottomSheetBehavior behavior = BottomSheetBehavior.from((View) layout.getParent());
        setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                behavior.setPeekHeight(layout.getHeight());
            }
        });
    }

    public void show() {
        if (mResources != null) {
            mTermsOfServiceText.setText(Html.fromHtml(String.format(
                    mResources.getString(R.string.signup_terms_of_service_text), "<u>", "</u>")));
            super.show();
        }
    }

    public interface SignupSheetListener {
        void onSignupSheetDismissed();
        void onSignupSheetEmailClicked();
        void onSignupSheetGoogleClicked();
        void onSignupSheetTermsOfServiceClicked();
    }
}
