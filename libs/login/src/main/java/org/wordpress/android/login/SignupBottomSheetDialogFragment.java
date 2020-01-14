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
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

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
        return inflater.inflate(R.layout.signup_bottom_sheet_dialog, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button termsOfServiceText = view.findViewById(R.id.signup_tos);
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

        Button signupWithEmailButton = view.findViewById(R.id.signup_email);
        signupWithEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSignupSheetListener.onSignupSheetEmailClicked();
            }
        });

        Button signupWithGoogleButton = view.findViewById(R.id.signup_google);
        signupWithGoogleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSignupSheetListener.onSignupSheetGoogleClicked();
            }
        });

        Dialog dialog = getDialog();
        if (dialog != null) {
            // Set peek height to full height of view to avoid signup buttons being off screen when
            // bottom sheet is shown with small screen height (e.g. landscape orientation).
            dialog.setOnShowListener(new OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    BottomSheetDialog sheetDialog = (BottomSheetDialog) dialogInterface;
                    FrameLayout bottomSheet = sheetDialog
                            .findViewById(com.google.android.material.R.id.design_bottom_sheet);

                    if (bottomSheet != null) {
                        BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                }
            });
        }
    }

    @Override public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mSignupSheetListener != null) {
            mSignupSheetListener.onSignupSheetCanceled();
        }
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
