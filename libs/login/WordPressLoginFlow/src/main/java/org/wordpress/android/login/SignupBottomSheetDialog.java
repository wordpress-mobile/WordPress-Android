package org.wordpress.android.login;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.wordpress.android.login.widgets.WPBottomSheetDialog;

public class SignupBottomSheetDialog extends WPBottomSheetDialog {
    public SignupBottomSheetDialog(@NonNull final Context context,
                                   @NonNull final SignupSheetListener signupSheetListener) {
        super(context);
        //noinspection InflateParams
        final View layout = LayoutInflater.from(context).inflate(R.layout.signup_bottom_sheet_dialog, null);

        Button termsOfServiceText = layout.findViewById(R.id.signup_tos);
        termsOfServiceText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signupSheetListener.onSignupSheetTermsOfServiceClicked();
            }
        });
        termsOfServiceText.setText(Html.fromHtml(String.format(
                context.getResources().getString(R.string.signup_terms_of_service_text), "<u>", "</u>")));

        Button signupWithEmailButton = layout.findViewById(R.id.signup_email);
        signupWithEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signupSheetListener.onSignupSheetEmailClicked();
            }
        });

        Button signupWithGoogleButton = layout.findViewById(R.id.signup_google);
        signupWithGoogleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signupSheetListener.onSignupSheetGoogleClicked();
            }
        });

        setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                signupSheetListener.onSignupSheetCanceled();
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

    @Override
    public void onStart() {
        super.onStart();
        // Fix to show navigation bar buttons when the signup bottom sheet is shown
        // on Android versions API 27 and above. They get shadowed or invisible otherwise.
        if (getWindow() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            Window window = getWindow();

            // You can find the bottom sheet coordinator layout container id here: https://git.io/JejbO
            View containerView = window.findViewById(com.google.android.material.R.id.container);
            if (containerView != null) {
                containerView.setFitsSystemWindows(false);

                // dark navigation bar icons
                View decorView = window.getDecorView();
                decorView.setSystemUiVisibility(
                    decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                );
            }
        }
    }

    public interface SignupSheetListener {
        void onSignupSheetCanceled();
        void onSignupSheetEmailClicked();
        void onSignupSheetGoogleClicked();
        void onSignupSheetTermsOfServiceClicked();
    }
}
