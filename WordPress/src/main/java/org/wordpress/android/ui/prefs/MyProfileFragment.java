package org.wordpress.android.ui.prefs;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.Account;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.AccountModel;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPTextView;

import java.util.HashMap;
import java.util.Map;

public class MyProfileFragment extends Fragment implements ProfileInputDialogFragment.Callback {
    private final String DIALOG_TAG = "DIALOG";

    private final String FIRST_NAME_TAG = "first_name";
    private final String LAST_NAME_TAG = "last_name";
    private final String DISPLAY_NAME_TAG = "display_name";
    private final String ABOUT_ME_TAG = "about_me";

    private WPTextView mFirstName;
    private WPTextView mLastName;
    private WPTextView mDisplayName;
    private WPTextView mAboutMe;

    public static MyProfileFragment newInstance() {
        return new MyProfileFragment();
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshDetails();
        if (NetworkUtils.isNetworkAvailable(getActivity())) {
            AccountHelper.getDefaultAccount().fetchAccountSettings();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.my_profile_fragment, container, false);

        mFirstName = (WPTextView) rootView.findViewById(R.id.first_name);
        mFirstName.setTag(FIRST_NAME_TAG);
        mLastName = (WPTextView) rootView.findViewById(R.id.last_name);
        mLastName.setTag(LAST_NAME_TAG);
        mDisplayName = (WPTextView) rootView.findViewById(R.id.display_name);
        mDisplayName.setTag(DISPLAY_NAME_TAG);
        mAboutMe = (WPTextView) rootView.findViewById(R.id.about_me);
        mAboutMe.setTag(ABOUT_ME_TAG);

        rootView.findViewById(R.id.first_name_row).setOnClickListener(
                createOnClickListener(
                        getString(R.string.first_name),
                        null,
                        mFirstName,
                        false));
        rootView.findViewById(R.id.last_name_row).setOnClickListener(
                createOnClickListener(
                        getString(R.string.last_name),
                        null,
                        mLastName,
                        false));
        rootView.findViewById(R.id.display_name_row).setOnClickListener(
                createOnClickListener(
                        getString(R.string.public_display_name),
                        getString(R.string.public_display_name_hint),
                        mDisplayName,
                        false));
        rootView.findViewById(R.id.about_me_row).setOnClickListener(
                createOnClickListener(
                        getString(R.string.about_me),
                        getString(R.string.about_me_hint),
                        mAboutMe,
                        true));

        return rootView;
    }

    private void refreshDetails() {
        if (!isAdded()) return;

        Account account = AccountHelper.getDefaultAccount();
        updateLabel(mFirstName, account != null ? StringUtils.unescapeHTML(account.getFirstName()) : null);
        updateLabel(mLastName, account != null ? StringUtils.unescapeHTML(account.getLastName()) : null);
        updateLabel(mDisplayName, account != null ? StringUtils.unescapeHTML(account.getDisplayName()) : null);
        updateLabel(mAboutMe, account != null ? StringUtils.unescapeHTML(account.getAboutMe()) : null);
    }

    private void updateLabel(WPTextView textView, String text) {
        textView.setText(text);
        if (TextUtils.isEmpty(text)) {
            if (textView == mDisplayName) {
                Account account = AccountHelper.getDefaultAccount();
                mDisplayName.setText(account.getUserName());
            } else {
                textView.setVisibility(View.GONE);
            }
        }
        else {
            textView.setVisibility(View.VISIBLE);
        }
    }

    // helper method to create onClickListener to avoid code duplication
    private View.OnClickListener createOnClickListener(final String dialogTitle,
                                                       final String hint,
                                                       final WPTextView textView,
                                                       final boolean isMultiline) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProfileInputDialogFragment inputDialog = ProfileInputDialogFragment.newInstance(dialogTitle,
                        textView.getText().toString(), hint, isMultiline, (String)textView.getTag());
                inputDialog.show(getFragmentManager(), DIALOG_TAG);
            }
        };
    }

    @Override
    public void onSuccessfulInput(String input, String callbackTag) {
        WPTextView textView = getTextView(callbackTag);
        if (textView == null) return;

        updateLabel(textView, input);
        updateMyProfileForLabel(textView);
    }

    @Nullable
    private WPTextView getTextView(String tag) {
        switch (tag) {
            case FIRST_NAME_TAG:
                return mFirstName;
            case LAST_NAME_TAG:
                return mLastName;
            case DISPLAY_NAME_TAG:
                return mDisplayName;
            case ABOUT_ME_TAG:
                return mAboutMe;
            default:
                return null;
        }
    }

    private void updateMyProfileForLabel(TextView textView) {
        Map<String, String> params = new HashMap<>();
        params.put(restParamForTextView(textView), textView.getText().toString());
        AccountHelper.getDefaultAccount().postAccountSettings(params);
    }

    // helper method to get the rest parameter for a text view
    private String restParamForTextView(TextView textView) {
        if (textView == mFirstName) {
            return AccountModel.RestParam.FIRST_NAME.getDescription();
        } else if (textView == mLastName) {
            return AccountModel.RestParam.LAST_NAME.getDescription();
        } else if (textView == mDisplayName) {
            return AccountModel.RestParam.DISPLAY_NAME.getDescription();
        } else if (textView == mAboutMe) {
            return AccountModel.RestParam.ABOUT_ME.getDescription();
        }
        return null;
    }

    public void onEventMainThread(PrefsEvents.AccountSettingsFetchSuccess event) {
        refreshDetails();
    }
}
