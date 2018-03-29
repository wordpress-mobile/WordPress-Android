package org.wordpress.android.ui.accounts;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.helpshift.support.Support;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.AppLogViewerActivity;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.HelpshiftHelper.MetadataKey;
import org.wordpress.android.util.HelpshiftHelper.Tag;
import org.wordpress.android.widgets.WPTextView;

import javax.inject.Inject;

public class SignInDialogFragment extends DialogFragment {
    private static final String ARG_TITLE = "title";
    private static final String ARG_DESCRIPTION = "message";
    private static final String ARG_IMAGE = "image";
    private static final String ARG_NUMBER_OF_BUTTONS = "number-of-buttons";
    private static final String ARG_FIRST_BUTTON_LABEL = "first-btn-label";
    private static final String ARG_SECOND_BUTTON_LABEL = "second-btn-label";
    private static final String ARG_THIRD_BUTTON_LABEL = "third-btn-label";
    private static final String ARG_SECOND_BUTTON_ACTION = "second-btn-action";
    private static final String ARG_THIRD_BUTTON_ACTION = "third-btn-action";
    private static final String ARG_TELL_ME_MORE_BUTTON_PARAM_NAME_FAQ_ID = "tell-me-more-btn-param-name-faq-id";
    private static final String ARG_TELL_ME_MORE_BUTTON_PARAM_NAME_SECTION_ID =
            "tell-me-more-btn-param-name-section-id";
    public static final String ARG_OPEN_URL_PARAM = "open-url-param";

    public static final int ACTION_FINISH = 1;
    public static final int ACTION_OPEN_URL = 2;
    public static final int ACTION_OPEN_SUPPORT_CHAT = 3;
    public static final int ACTION_OPEN_APPLICATION_LOG = 4;
    public static final int ACTION_OPEN_FAQ_PAGE = 5;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    public SignInDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
    }

    public static SignInDialogFragment newInstance(String title, String message, int imageSource, String buttonLabel) {
        return newInstance(title, message, imageSource, 1, buttonLabel, "", "", 0, 0, "", "");
    }

    public static SignInDialogFragment newInstance(String title, String message, int imageSource, int numberOfButtons,
                                                   String firstButtonLabel, String secondButtonLabel,
                                                   String thirdButtonLabel, int secondButtonAction,
                                                   int thirdButtonAction) {
        return newInstance(title, message, imageSource, numberOfButtons, firstButtonLabel, secondButtonLabel,
                           thirdButtonLabel, secondButtonAction, thirdButtonAction, "", "");
    }

    public static SignInDialogFragment newInstance(String title, String message, int imageSource, int numberOfButtons,
                                                   String firstButtonLabel, String secondButtonLabel,
                                                   String thirdButtonLabel,
                                                   int secondButtonAction, int thirdButtonAction,
                                                   String tellMeMoreButtonFaqId, String tellMeMoreButtonSectionId) {
        SignInDialogFragment adf = new SignInDialogFragment();
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
        bundle.putString(ARG_TELL_ME_MORE_BUTTON_PARAM_NAME_FAQ_ID, tellMeMoreButtonFaqId);
        bundle.putString(ARG_TELL_ME_MORE_BUTTON_PARAM_NAME_SECTION_ID, tellMeMoreButtonSectionId);

        adf.setArguments(bundle);
        adf.setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme);
        return adf;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(getResources().getDrawable(R.color.nux_alert_bg));
        }
        View v = inflater.inflate(R.layout.signin_dialog_fragment, container, false);

        ImageView imageView = (ImageView) v.findViewById(R.id.nux_dialog_image);
        WPTextView titleTextView = (WPTextView) v.findViewById(R.id.nux_dialog_title);
        WPTextView descriptionTextView = (WPTextView) v.findViewById(R.id.nux_dialog_description);
        WPTextView footerBottomButton = (WPTextView) v.findViewById(R.id.nux_dialog_first_button);
        WPTextView footerTopButton = (WPTextView) v.findViewById(R.id.nux_dialog_third_button);
        WPTextView footerCenterButton = (WPTextView) v.findViewById(R.id.nux_dialog_second_button);
        final Bundle arguments = getArguments();

        titleTextView.setText(arguments.getString(ARG_TITLE));
        descriptionTextView.setText(arguments.getString(ARG_DESCRIPTION));
        imageView.setImageResource(arguments.getInt(ARG_IMAGE));

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
                footerCenterButton.setText(arguments.getString(ARG_FIRST_BUTTON_LABEL));
                footerCenterButton.setOnClickListener(clickListenerDismiss);
                footerBottomButton.setVisibility(View.GONE);
                footerTopButton.setVisibility(View.GONE);
                break;
            case 2:
                // Two buttons: we keep only the left and right buttons
                footerBottomButton.setText(arguments.getString(ARG_FIRST_BUTTON_LABEL));
                footerTopButton.setText(arguments.getString(ARG_SECOND_BUTTON_LABEL));
                footerCenterButton.setVisibility(View.GONE);
                footerTopButton.setOnClickListener(clickListenerSecondButton);
                break;
            case 3:
                footerBottomButton.setText(arguments.getString(ARG_FIRST_BUTTON_LABEL));
                footerCenterButton.setText(arguments.getString(ARG_SECOND_BUTTON_LABEL));
                footerCenterButton.setOnClickListener(clickListenerSecondButton);
                footerTopButton.setText(arguments.getString(ARG_THIRD_BUTTON_LABEL));
                footerTopButton.setOnClickListener(clickListenerThirdButton);
                break;
        }
        v.setClickable(true);
        v.setOnClickListener(clickListenerDismiss);
        footerBottomButton.setOnClickListener(clickListenerDismiss);

        return v;
    }

    private void onClickAction(View v, int action, Bundle arguments) {
        if (!isAdded()) {
            return;
        }
        switch (action) {
            case ACTION_OPEN_URL:
                String url = arguments.getString(ARG_OPEN_URL_PARAM);
                if (TextUtils.isEmpty(url)) {
                    return;
                }
                ActivityLauncher.openUrlExternal(getContext(), url);
                break;
            case ACTION_OPEN_SUPPORT_CHAT:
                HelpshiftHelper.getInstance().addMetaData(MetadataKey.USER_ENTERED_URL, arguments.getString(
                        HelpshiftHelper.ENTERED_URL_KEY));
                HelpshiftHelper.getInstance().addMetaData(MetadataKey.USER_ENTERED_USERNAME, arguments.getString(
                        HelpshiftHelper.ENTERED_USERNAME_KEY));
                HelpshiftHelper.getInstance().addMetaData(MetadataKey.USER_ENTERED_EMAIL, arguments.getString(
                        HelpshiftHelper.ENTERED_EMAIL_KEY));
                Tag origin = (Tag) arguments.getSerializable(HelpshiftHelper.ORIGIN_KEY);
                HelpshiftHelper.getInstance().showConversation(getActivity(), mSiteStore,
                                                               origin, mAccountStore.getAccount().getUserName());
                dismissAllowingStateLoss();
                break;
            case ACTION_OPEN_APPLICATION_LOG:
                startActivity(new Intent(v.getContext(), AppLogViewerActivity.class));
                dismissAllowingStateLoss();
                break;
            case ACTION_OPEN_FAQ_PAGE:
                String faqId = arguments.getString(ARG_TELL_ME_MORE_BUTTON_PARAM_NAME_FAQ_ID);
                String sectionId = arguments.getString(ARG_TELL_ME_MORE_BUTTON_PARAM_NAME_SECTION_ID);
                if (faqId != null) {
                    Support.showSingleFAQ(getActivity(), faqId);
                } else if (sectionId != null) {
                    Support.showFAQSection(getActivity(), sectionId);
                }
                break;
            default:
            case ACTION_FINISH:
                getActivity().finish();
                break;
        }
    }
}

