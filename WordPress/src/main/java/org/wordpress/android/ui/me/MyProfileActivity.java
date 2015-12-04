package org.wordpress.android.ui.me;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.models.Account;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.widgets.OpenSansEditText;
import org.wordpress.android.widgets.WPTextView;
import org.xmlrpc.android.ApiHelper;

public class MyProfileActivity extends AppCompatActivity {

    private WPTextView mFirstName;
    private WPTextView mLastName;
    private WPTextView mDisplayName;
    private WPTextView mAboutMe;

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

        findViewById(R.id.first_name_row).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog();
            }
        });
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
        }
        return super.onOptionsItemSelected(item);
    }

    protected void showInputDialog() {
        LayoutInflater layoutInflater = LayoutInflater.from(MyProfileActivity.this);
        View promptView = layoutInflater.inflate(R.layout.my_profile_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MyProfileActivity.this);
        alertDialogBuilder.setView(promptView);

        final WPTextView textView = (WPTextView) promptView.findViewById(R.id.my_profile_dialog_label);
        final OpenSansEditText editText = (OpenSansEditText) promptView.findViewById(R.id.my_profile_dialog_input);
        textView.setText(R.string.first_name);
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mFirstName.setVisibility(View.VISIBLE);
                        mFirstName.setText(editText.getText());
                    }
                })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private void refreshDetails() {
        Account account = AccountHelper.getDefaultAccount();
        updateLabel(mFirstName, account != null ? account.getFirstName() : null);
        updateLabel(mLastName, account != null ? account.getLastName() : null);
        updateLabel(mDisplayName, account != null ? account.getDisplayName() : null);
        updateLabel(mAboutMe, account != null ? account.getAboutMe() : null);
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
}
