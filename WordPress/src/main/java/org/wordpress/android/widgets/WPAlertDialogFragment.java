package org.wordpress.android.widgets;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.StringUtils;

public class WPAlertDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
    private enum WPAlertDialogType {ALERT,    // simple ok dialog with error message
                                    CONFIRM,  // dialog with yes/no and callback when positive button clicked
                                    CUSTOM,   // dialog with custom buttons and callback when positive or negative button clicked
                                    URL_INFO} // info dialog that shows url when positive button clicked

    private static final String ARG_TITLE           = "title";
    private static final String ARG_MESSAGE         = "message";
    private static final String ARG_TYPE            = "type";
    private static final String ARG_INFO_TITLE      = "info-title";
    private static final String ARG_INFO_URL        = "info-url";
    private static final String ARG_POSITIVE_BUTTON = "positive-button";
    private static final String ARG_NEGATIVE_BUTTON = "negative-button";

    public interface OnDialogConfirmListener {
        void onDialogConfirm();
    }

    public interface OnDialogDismissListener {
        void onDialogDismiss();
    }

    public static WPAlertDialogFragment newAlertDialog(String message) {
        String title = WordPress.getContext().getString(R.string.error_generic);
        return newAlertDialog(title, message);
    }
    public static WPAlertDialogFragment newAlertDialog(String title, String message) {
        return newInstance(title, message, WPAlertDialogType.ALERT, null, null, null, null);
    }

    public static WPAlertDialogFragment newConfirmDialog(String title,
                                                         String message) {
        return newInstance(title, message, WPAlertDialogType.CONFIRM, null, null, null, null);
    }

    public static WPAlertDialogFragment newUrlInfoDialog(String title,
                                                         String message,
                                                         String infoTitle,
                                                         String infoUrl) {
        return newInstance(title, message, WPAlertDialogType.URL_INFO, infoTitle, infoUrl, null, null);
    }

    public static WPAlertDialogFragment newCustomDialog(String title,
                                                        String message,
                                                        String positiveButton,
                                                        String negativeButton) {
        return newInstance(title, message, WPAlertDialogType.CUSTOM, null, null, positiveButton, negativeButton);
    }

    private static WPAlertDialogFragment newInstance(String title,
                                                     String message,
                                                     WPAlertDialogType alertType,
                                                     String infoTitle,
                                                     String infoUrl,
                                                     String positiveButton,
                                                     String negativeButton) {
        WPAlertDialogFragment dialog = new WPAlertDialogFragment();

        Bundle bundle = new Bundle();

        bundle.putString(ARG_TITLE, StringUtils.notNullStr(title));
        bundle.putString(ARG_MESSAGE, StringUtils.notNullStr(message));
        bundle.putSerializable(ARG_TYPE, (alertType != null ? alertType : WPAlertDialogType.ALERT));

        if (alertType == WPAlertDialogType.URL_INFO) {
            bundle.putString(ARG_INFO_TITLE, StringUtils.notNullStr(infoTitle));
            bundle.putString(ARG_INFO_URL, StringUtils.notNullStr(infoUrl));
        }

        if (alertType == WPAlertDialogType.CUSTOM) {
            bundle.putString(ARG_POSITIVE_BUTTON, StringUtils.notNullStr(positiveButton));
            bundle.putString(ARG_NEGATIVE_BUTTON, StringUtils.notNullStr(negativeButton));
        }

        dialog.setArguments(bundle);

        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
        int style = DialogFragment.STYLE_NORMAL, theme = 0;
        setStyle(style, theme);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = getArguments();

        final String title = StringUtils.notNullStr(bundle.getString(ARG_TITLE));
        final String message = StringUtils.notNullStr(bundle.getString(ARG_MESSAGE));

        final WPAlertDialogType dialogType;
        if (bundle.containsKey(ARG_TYPE) && bundle.getSerializable(ARG_TYPE) instanceof WPAlertDialogType) {
            dialogType = (WPAlertDialogType) bundle.getSerializable(ARG_TYPE);
        } else {
            dialogType = WPAlertDialogType.ALERT;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(title);
        builder.setMessage(message);

        switch (dialogType) {
            case ALERT:
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setNeutralButton(R.string.ok, this);
                break;

            case CUSTOM:
                final String positiveButton = StringUtils.notNullStr(bundle.getString(ARG_POSITIVE_BUTTON));
                final String negativeButton = StringUtils.notNullStr(bundle.getString(ARG_NEGATIVE_BUTTON));

                builder.setPositiveButton(positiveButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (getActivity() instanceof OnDialogConfirmListener) {
                            OnDialogConfirmListener act = (OnDialogConfirmListener) getActivity();
                            act.onDialogConfirm();
                        }
                    }
                });
                builder.setNegativeButton(negativeButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (getActivity() instanceof OnDialogDismissListener) {
                            OnDialogDismissListener act = (OnDialogDismissListener) getActivity();
                            act.onDialogDismiss();
                        }
                    }
                });
                break;

            case CONFIRM:
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (getActivity() instanceof OnDialogConfirmListener) {
                            OnDialogConfirmListener act = (OnDialogConfirmListener) getActivity();
                            act.onDialogConfirm();
                        }
                    }
                });
                builder.setNegativeButton(R.string.no, this);
                break;

            case URL_INFO:
                final String infoTitle = StringUtils.notNullStr(bundle.getString(ARG_INFO_TITLE));
                final String infoURL = StringUtils.notNullStr(bundle.getString(ARG_INFO_URL));
                builder.setPositiveButton(infoTitle, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!TextUtils.isEmpty(infoURL)) {
                                ActivityLauncher.openUrlExternal(getActivity(), infoURL);
                            }
                        }
                });
                break;
        }

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
    }
}

