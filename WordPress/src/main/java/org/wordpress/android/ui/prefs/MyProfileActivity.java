package org.wordpress.android.ui.prefs;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.Account;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.DialogUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPTextView;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;

public class MyProfileActivity extends AppCompatActivity {

    private WPTextView mFirstName;
    private WPTextView mLastName;
    private WPTextView mDisplayName;
    private WPTextView mAboutMe;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AccountHelper.getDefaultAccount().fetchAccountSettings();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setContentView(R.layout.my_profile_activity);

        mFirstName = (WPTextView) findViewById(R.id.first_name);
        mLastName = (WPTextView) findViewById(R.id.last_name);
        mDisplayName = (WPTextView) findViewById(R.id.display_name);
        mAboutMe = (WPTextView) findViewById(R.id.about_me);

        refreshDetails();

        findViewById(R.id.first_name_row).setOnClickListener(
                createOnClickListener(
                        getString(R.string.first_name),
                        null,
                        mFirstName,
                        false));
        findViewById(R.id.last_name_row).setOnClickListener(
                createOnClickListener(
                        getString(R.string.last_name),
                        null,
                        mLastName,
                        false));
        findViewById(R.id.display_name_row).setOnClickListener(
                createOnClickListener(
                        getString(R.string.public_display_name),
                        getString(R.string.public_display_name_hint),
                        mDisplayName,
                        false));
        findViewById(R.id.about_me_row).setOnClickListener(
                createOnClickListener(
                        getString(R.string.about_me),
                        getString(R.string.about_me_hint),
                        mAboutMe,
                        true));
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshDetails() {
        Account account = AccountHelper.getDefaultAccount();
        updateLabel(mFirstName, account != null ? StringUtils.unescapeHTML(account.getFirstName()) : null);
        updateLabel(mLastName, account != null ? StringUtils.unescapeHTML(account.getLastName()) : null);
        updateLabel(mDisplayName, account != null ? StringUtils.unescapeHTML(account.getDisplayName()) : null);
        updateLabel(mAboutMe, account != null ? StringUtils.unescapeHTML(account.getAboutMe()) : null);
    }

    private void updateMyProfileForLabel(TextView textView) {
        Map<String, String> params = new HashMap<>();
        params.put(restParamForTextView(textView), textView.getText().toString());
        AccountHelper.getDefaultAccount().postAccountSettings(params);
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
                DialogUtils.showMyProfileDialog(MyProfileActivity.this,
                        dialogTitle,
                        textView.getText().toString(),
                        hint,
                        isMultiline,
                        new DialogUtils.Callback() {
                            @Override
                            public void onSuccessfulInput(String input) {
                                updateLabel(textView, input);
                                updateMyProfileForLabel(textView);
                            }
                        });
            }
        };
    }

    // helper method to get the rest parameter for a text view
    private String restParamForTextView(TextView textView) {
        Account.RestParam param = null;
        if (textView == mFirstName) {
            param = Account.RestParam.FIRST_NAME;
        } else if (textView == mLastName) {
            param = Account.RestParam.LAST_NAME;
        } else if (textView == mDisplayName) {
            param = Account.RestParam.DISPLAY_NAME;
        } else if (textView == mAboutMe) {
            param = Account.RestParam.ABOUT_ME;
        }
        return Account.RestParam.toString(param);
    }

    public void onEventMainThread(PrefsEvents.MyProfileDetailsChanged event) {
        if (!isFinishing()) {
            refreshDetails();
        }
    }
}
