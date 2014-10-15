package org.wordpress.android.ui.accounts;

import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.wordpress.android.R;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.HelpshiftHelper.MetadataKey;
import org.wordpress.android.widgets.WPTextView;

public class NUXDialogFragment extends DialogFragment {
    private static String ARG_TITLE = "title";
    private static String ARG_DESCRIPTION = "message";
    private static String ARG_FOOTER = "footer";
    private static String ARG_IMAGE = "image";
    private static String ARG_NUMBER_OF_BUTTONS = "number-of-buttons";
    private static String ARG_FIRST_BUTTON_LABEL = "first-btn-label";
    private static String ARG_SECOND_BUTTON_LABEL = "second-btn-label";
    private static String ARG_THIRD_BUTTON_LABEL = "third-btn-label";
    private static String ARG_SECOND_BUTTON_ACTION = "second-btn-action";
    private static String ARG_THIRD_BUTTON_ACTION = "third-btn-action";
    public static String ARG_OPEN_URL_PARAM = "open-url-param";

    private ImageView mImageView;
    private WPTextView mTitleTextView;
    private WPTextView mDescriptionTextView;
    private WPTextView mFooterBottomButton;
    private WPTextView mFooterCenterButton;
    private WPTextView mFooterTopButton;

    public static final int ACTION_FINISH = 1;
    public static final int ACTION_OPEN_URL = 2;
    public static final int ACTION_OPEN_SUPPORT_CHAT = 3;

    public NUXDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    public static NUXDialogFragment newInstance(String title, String message, int imageSource, String buttonLabel) {
        return newInstance(title, message, imageSource, 1, buttonLabel, "", "", 0, 0);
    }

    public static NUXDialogFragment newInstance(String title, String message, int imageSource, int numberOfButtons,
                                                String firstButtonLabel, String secondButtonLabel,
                                                String thirdButtonLabel, int secondButtonAction,
                                                int thirdButtonAction) {
        NUXDialogFragment adf = new NUXDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_TITLE, title);
        bundle.putString(ARG_DESCRIPTION, message);
        bundle.putInt(ARG_IMAGE, imageSource);
        bundle.putInt(ARG_NUMBER_OF_BUTTONS, numberOfButtons);
        bundle.putString(ARG_FIRST_BUTTON_LABEL, firstButtonLabel);
        bundle.putString(ARG_SECOND_BUTTON_LABEL, secondButtonLabel);
        bundle.putString(ARG_THIRD_BUTTON_LABEL, thirdButtonLabel);
        bundle.putInt(ARG_SECOND_BUTTON_ACTION, secondButtonAction);
        bundle.putInt(ARG_THIRD_BUTTON_ACTION, thirdButtonAction);

        adf.setArguments(bundle);
        adf.setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme);
        return adf;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().getWindow().setBackgroundDrawable(getResources().getDrawable(R.color.nux_alert_bg));
        View v = inflater.inflate(R.layout.nux_dialog_fragment, container, false);

        mImageView = (ImageView) v.findViewById(R.id.nux_dialog_image);
        mTitleTextView = (WPTextView) v.findViewById(R.id.nux_dialog_title);
        mDescriptionTextView = (WPTextView) v.findViewById(R.id.nux_dialog_description);
        mFooterBottomButton = (WPTextView) v.findViewById(R.id.nux_dialog_left_button);
        mFooterCenterButton = (WPTextView) v.findViewById(R.id.nux_dialog_center_button);
        mFooterTopButton = (WPTextView) v.findViewById(R.id.nux_dialog_right_button);
        final Bundle arguments = getArguments();

        mTitleTextView.setText(arguments.getString(ARG_TITLE));
        mDescriptionTextView.setText(arguments.getString(ARG_DESCRIPTION));
        mImageView.setImageResource(arguments.getInt(ARG_IMAGE));

        View.OnClickListener clickListenerDismiss = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissAllowingStateLoss();
            }
        };

        View.OnClickListener clickListenerSecondButton = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickAction(v, arguments.getInt(ARG_SECOND_BUTTON_ACTION, 0), arguments);
            }
        };

        View.OnClickListener clickListenerThirdButton = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickAction(v, arguments.getInt(ARG_THIRD_BUTTON_ACTION, 0), arguments);
            }
        };

        switch (arguments.getInt(ARG_NUMBER_OF_BUTTONS, 1)) {
            case 1:
                // One button: we keep only the centered button
                mFooterCenterButton.setText(arguments.getString(ARG_FIRST_BUTTON_LABEL));
                mFooterCenterButton.setOnClickListener(clickListenerDismiss);
                mFooterBottomButton.setVisibility(View.GONE);
                mFooterTopButton.setVisibility(View.GONE);
                break;
            case 2:
                // Two buttons: we keep only the left and right buttons
                mFooterBottomButton.setText(arguments.getString(ARG_FIRST_BUTTON_LABEL));
                mFooterTopButton.setText(arguments.getString(ARG_SECOND_BUTTON_LABEL));
                mFooterCenterButton.setVisibility(View.GONE);
                mFooterTopButton.setOnClickListener(clickListenerSecondButton);
                break;
            case 3:
                mFooterBottomButton.setText(arguments.getString(ARG_FIRST_BUTTON_LABEL));
                mFooterCenterButton.setText(arguments.getString(ARG_SECOND_BUTTON_LABEL));
                mFooterCenterButton.setOnClickListener(clickListenerSecondButton);
                mFooterTopButton.setText(arguments.getString(ARG_THIRD_BUTTON_LABEL));
                mFooterTopButton.setOnClickListener(clickListenerThirdButton);
                break;
        }
        v.setClickable(true);
        v.setOnClickListener(clickListenerDismiss);
        mFooterBottomButton.setOnClickListener(clickListenerDismiss);
        return v;
    }

    private void onClickAction(View v, int action, Bundle arguments) {
        if (!isAdded()) {
            return;
        }
        switch (action) {
            case ACTION_OPEN_URL:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(arguments.getString(ARG_OPEN_URL_PARAM)));
                startActivity(intent);
                dismissAllowingStateLoss();
                break;
            case ACTION_OPEN_SUPPORT_CHAT:
                HelpshiftHelper.getInstance().addMetaData(MetadataKey.USER_ENTERED_URL, arguments.getString(
                        WelcomeFragmentSignIn.ENTERED_URL_KEY));
                HelpshiftHelper.getInstance().addMetaData(MetadataKey.USER_ENTERED_USERNAME, arguments.getString(
                        WelcomeFragmentSignIn.ENTERED_USERNAME_KEY));
                HelpshiftHelper.getInstance().showConversation(getActivity());
                dismissAllowingStateLoss();
                break;
            default:
            case ACTION_FINISH:
                getActivity().finish();
                break;
        }
    }
}

