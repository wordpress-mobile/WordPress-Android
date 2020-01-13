package org.wordpress.android.login;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.wordpress.android.login.widgets.WPBottomSheetDialogFragment;

public class SignupBottomSheetDialogFragment extends WPBottomSheetDialogFragment {
    public static final String TAG = SignupBottomSheetDialogFragment.class.getSimpleName();
    private SignupSheetListener mSignupSheetListener;

    public static SignupBottomSheetDialogFragment newInstance() {
        return new SignupBottomSheetDialogFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof SignupSheetListener)) {
            throw new IllegalStateException("Parent activity doesn't implement SignupSheetListener");
        }
        mSignupSheetListener = (SignupSheetListener) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.signup_bottom_sheet_dialog, container, false);

        Button termsOfServiceText = layout.findViewById(R.id.signup_tos);
        termsOfServiceText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSignupSheetListener.onSignupSheetTermsOfServiceClicked();
            }
        });
        termsOfServiceText.setText(Html.fromHtml(String.format(
                getDialog()
                    .getContext()
                    .getResources()
                    .getString(R.string.signup_terms_of_service_text), "<u>", "</u>")));

        Button signupWithEmailButton = layout.findViewById(R.id.signup_email);
        signupWithEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSignupSheetListener.onSignupSheetEmailClicked();
            }
        });

        Button signupWithGoogleButton = layout.findViewById(R.id.signup_google);
        signupWithGoogleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSignupSheetListener.onSignupSheetGoogleClicked();
            }
        });

        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mSignupSheetListener.onSignupSheetCanceled();
                }
            });

            dialog.setContentView(layout);

            // Set peek height to full height of view to avoid signup buttons being off screen when
            // bottom sheet is shown with small screen height (e.g. landscape orientation).
            final BottomSheetBehavior behavior = BottomSheetBehavior.from((View) layout.getParent());
            dialog.setOnShowListener(new OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    behavior.setPeekHeight(layout.getHeight());
                }
            });
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        mSignupSheetListener.onSignupSheetCanceled();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mSignupSheetListener = null;
    }

    public interface SignupSheetListener {
        void onSignupSheetCanceled();
        void onSignupSheetEmailClicked();
        void onSignupSheetGoogleClicked();
        void onSignupSheetTermsOfServiceClicked();
    }
}
