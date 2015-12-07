package org.wordpress.android.ui.me;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.models.Account;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.DialogUtils;
import org.wordpress.android.widgets.WPTextView;
import org.xmlrpc.android.ApiHelper;

public class MyProfileActivity extends AppCompatActivity {

    private WPTextView mFirstName;
    private WPTextView mLastName;
    private WPTextView mDisplayName;
    private WPTextView mAboutMe;
    private ProgressDialog mProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AccountHelper.getDefaultAccount().fetchAccountSettings(new ApiHelper.GenericCallback() {
            @Override
            public void onSuccess() {
                refreshDetails();
            }

            @Override
            public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setContentView(R.layout.my_profile_activity);

        setTitle(R.string.my_profile);

        mFirstName = (WPTextView) findViewById(R.id.first_name);
        mLastName = (WPTextView) findViewById(R.id.last_name);
        mDisplayName = (WPTextView) findViewById(R.id.display_name);
        mAboutMe = (WPTextView) findViewById(R.id.about_me);

        refreshDetails();

        findViewById(R.id.first_name_row).setOnClickListener(createOnClickListener(getString(org.wordpress.android.R.string.first_name), mFirstName));
        findViewById(R.id.last_name_row).setOnClickListener(createOnClickListener(getString(R.string.last_name), mLastName));
        findViewById(R.id.display_name_row).setOnClickListener(createOnClickListener(getString(R.string.public_display_name), mDisplayName));
        findViewById(R.id.about_me_row).setOnClickListener(createOnClickListener(getString(R.string.about_me), getString(R.string.about_me_hint), mAboutMe));
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.menu_save) {
            saveMyProfile();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.my_profile, menu);
        return true;
    }

    private void refreshDetails() {
        Account account = AccountHelper.getDefaultAccount();
        updateLabel(mFirstName, account != null ? account.getFirstName() : null);
        updateLabel(mLastName, account != null ? account.getLastName() : null);
        updateLabel(mDisplayName, account != null ? account.getDisplayName() : null);
        updateLabel(mAboutMe, account != null ? account.getAboutMe() : null);
    }

    private void saveMyProfile() {
        String firstName = mFirstName.getText().toString();
        String lastName = mLastName.getText().toString();
        String displayName = mDisplayName.getText().toString();
        String aboutMe = mAboutMe.getText().toString();

        mProgressDialog = ProgressDialog.show(this, null, getText(R.string.saving_changes), false);

        AccountHelper.getDefaultAccount().postAccountSettings(firstName, lastName, displayName, aboutMe, new ApiHelper.GenericCallback() {
            @Override
            public void onSuccess() {
                refreshDetails();
                dismissProgressDialog();
            }

            @Override
            public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                dismissProgressDialog();
            }
        });
    }

    private void updateLabel(WPTextView textView, String text) {
        if (text == null || text.isEmpty()) {
            textView.setVisibility(View.GONE);
        }
        else {
            textView.setVisibility(View.VISIBLE);
            textView.setText(text);
        }
    }

    private View.OnClickListener createOnClickListener(final String dialogTitle, final WPTextView textView) {
        return createOnClickListener(dialogTitle, null, textView);
    }

    // helper method to create onClickListener to avoid code duplication
    private View.OnClickListener createOnClickListener(final String dialogTitle, final String hint, final WPTextView textView) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogUtils.showMyProfileDialog(MyProfileActivity.this, dialogTitle, hint, new DialogUtils.Callback() {
                    @Override
                    public void onSuccessfulInput(String input) {
                        updateLabel(textView, input);
                    }
                });
            }
        };
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = null;
    }
}
