package org.wordpress.android.ui.prefs;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.generated.AccountActionBuilder;
import org.wordpress.android.stores.model.AccountModel;
import org.wordpress.android.stores.store.AccountStore;
import org.wordpress.android.stores.store.AccountStore.OnAccountChanged;
import org.wordpress.android.stores.store.AccountStore.PostAccountSettingsPayload;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPTextView;

import java.util.HashMap;

import javax.inject.Inject;

public class MyProfileActivity extends AppCompatActivity implements ProfileInputDialogFragment.Callback {
    private final String DIALOG_TAG = "DIALOG";

    private WPTextView mFirstName;
    private WPTextView mLastName;
    private WPTextView mDisplayName;
    private WPTextView mAboutMe;

    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

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
        mDispatcher.unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (NetworkUtils.isNetworkAvailable(this)) {
            mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
        }
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
        AccountModel account = mAccountStore.getAccount();
        updateLabel(mFirstName, account != null ? StringUtils.unescapeHTML(account.getFirstName()) : null);
        updateLabel(mLastName, account != null ? StringUtils.unescapeHTML(account.getLastName()) : null);
        updateLabel(mDisplayName, account != null ? StringUtils.unescapeHTML(account.getDisplayName()) : null);
        updateLabel(mAboutMe, account != null ? StringUtils.unescapeHTML(account.getAboutMe()) : null);
    }

    private void updateMyProfileForLabel(TextView textView) {
        PostAccountSettingsPayload payload = new PostAccountSettingsPayload();
        payload.params = new HashMap<>();
        payload.params.put(restParamForTextView(textView), textView.getText().toString());
        mDispatcher.dispatch(AccountActionBuilder.newPostSettingsAction(payload));
    }

    private void updateLabel(WPTextView textView, String text) {
        textView.setText(text);
        if (TextUtils.isEmpty(text)) {
            if (textView == mDisplayName) {
                mDisplayName.setText(mAccountStore.getAccount().getUserName());
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
                        textView.getText().toString(), hint, isMultiline, textView.getId());
                inputDialog.show(getFragmentManager(), DIALOG_TAG);
            }
        };
    }

    // helper method to get the rest parameter for a text view
    private String restParamForTextView(TextView textView) {
        if (textView == mFirstName) {
            return "first_name";
        } else if (textView == mLastName) {
            return "last_name";
        } else if (textView == mDisplayName) {
            return "display_name";
        } else if (textView == mAboutMe) {
            return "description";
        }
        return null;
    }

    @Subscribe
    public void onAccountChanged(OnAccountChanged event) {
        if (!isFinishing()) {
            // TODO: STORES: manage errors
            refreshDetails();
        }
    }

    @Override
    public void onSuccessfulInput(String input, int callbackId) {
        WPTextView textView = (WPTextView) findViewById(callbackId);
        updateLabel(textView, input);
        updateMyProfileForLabel(textView);
    }
}
