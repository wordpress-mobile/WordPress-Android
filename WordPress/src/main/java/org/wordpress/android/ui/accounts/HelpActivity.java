package org.wordpress.android.ui.accounts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.support.ZendeskHelper;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.AppLogViewerActivity;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.widgets.WPTextView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class HelpActivity extends AppCompatActivity {
    private static final String ORIGIN_KEY = "ORIGIN_KEY";
    private static final String EXTRA_TAGS_KEY = "EXTRA_TAGS_KEY";

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    public static Intent createIntent(Context context, @NonNull Origin origin, @Nullable SiteModel selectedSite,
                                      @Nullable List<String> extraSupportTags) {
        Intent intent = new Intent(context, HelpActivity.class);
        intent.putExtra(HelpActivity.ORIGIN_KEY, origin);
        if (selectedSite != null) {
            intent.putExtra(WordPress.SITE, selectedSite);
        }
        if (extraSupportTags != null && !extraSupportTags.isEmpty()) {
            intent.putStringArrayListExtra(HelpActivity.EXTRA_TAGS_KEY, (ArrayList<String>) extraSupportTags);
        }
        return intent;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        initHelpshiftLayout();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_cross_white_24dp);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setElevation(0); // remove shadow
            actionBar.setDisplayShowTitleEnabled(false);
        }

        // Init common elements
        WPTextView version = findViewById(R.id.nux_help_version);
        version.setText(getString(R.string.version_with_name_param, WordPress.versionName));

        WPTextView appLogButton = findViewById(R.id.applog_button);
        appLogButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(v.getContext(), AppLogViewerActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActivityId.trackLastActivity(ActivityId.HELP_SCREEN);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initHelpshiftLayout() {
        setContentView(R.layout.help_activity_with_helpshift);

        WPTextView version = findViewById(R.id.nux_help_version);
        version.setText(getString(R.string.version_with_name_param, WordPress.versionName));
        WPTextView contactUsButton = findViewById(R.id.contact_us_button);
        contactUsButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View view) {
                createNewZendeskTicket();
            }
        });

        WPTextView faqButton = findViewById(R.id.faq_button);
        faqButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showZendeskFaq();
            }
        });

        WPTextView myTicketsButton = findViewById(R.id.my_tickets_button);
        myTicketsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showZendeskTickets();
            }
        });
    }

    private void createNewZendeskTicket() {
        ZendeskHelper.createNewTicket(this, mAccountStore, mSiteStore, getOriginFromExtras(),
                getSelectedSiteFromExtras(), getExtraTagsFromExtras());
    }

    private void showZendeskTickets() {
        ZendeskHelper.showAllTickets(this, mAccountStore, mSiteStore, getOriginFromExtras(),
                getSelectedSiteFromExtras(), getExtraTagsFromExtras());
    }

    private void showZendeskFaq() {
        ZendeskHelper.showZendeskHelpCenter(this, mAccountStore, getSelectedSiteFromExtras());
    }

    private Origin getOriginFromExtras() {
        Bundle extras = getIntent().getExtras();
        Origin origin = Origin.UNKNOWN;
        if (extras != null) {
            origin = (Origin) extras.get(HelpActivity.ORIGIN_KEY);
        }
        return origin;
    }

    private @Nullable List<String> getExtraTagsFromExtras() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            return extras.getStringArrayList(HelpActivity.EXTRA_TAGS_KEY);
        }
        return null;
    }

    private @Nullable SiteModel getSelectedSiteFromExtras() {
        Bundle extras = getIntent().getExtras();
        return extras != null ? (SiteModel) extras.get(WordPress.SITE) : null;
    }

    public enum Origin {
        UNKNOWN("origin:unknown"),
        LOGIN_SCREEN_WPCOM("origin:wpcom-login-screen"),
        LOGIN_SCREEN_SELF_HOSTED("origin:wporg-login-screen"),
        LOGIN_SCREEN_JETPACK("origin:jetpack-login-screen"),
        SIGNUP_SCREEN("origin:signup-screen"),
        ME_SCREEN_HELP("origin:me-screen-help"),
        DELETE_SITE("origin:delete-site"),
        FEEDBACK_AZTEC("origin:aztec-feedback"),
        LOGIN_EMAIL("origin:login-email"),
        LOGIN_MAGIC_LINK("origin:login-magic-link"),
        LOGIN_EMAIL_PASSWORD("origin:login-wpcom-password"),
        LOGIN_2FA("origin:login-2fa"),
        LOGIN_SITE_ADDRESS("origin:login-site-address"),
        LOGIN_SOCIAL("origin:login-social"),
        LOGIN_USERNAME_PASSWORD("origin:login-username-password"),
        RELEASE_NOTES("origin:release-notes"),
        SIGNUP_EMAIL("origin:signup-email"),
        SIGNUP_MAGIC_LINK("origin:signup-magic-link"),
        SITE_CREATION_CATEGORY("origin:site-create-site-category"),
        SITE_CREATION_THEME("origin:site-create-site-theme"),
        SITE_CREATION_DETAILS("origin:site-create-site-details"),
        SITE_CREATION_DOMAIN("origin:site-create-site-domain"),
        SITE_CREATION_CREATING("origin:site-create-creating");

        private final String mStringValue;

        Origin(final String stringValue) {
            mStringValue = stringValue;
        }

        public String toString() {
            return mStringValue;
        }

        public static String[] toString(Origin[] tags) {
            if (tags == null) {
                return null;
            }
            String[] res = new String[tags.length];
            for (int i = 0; i < res.length; i++) {
                res[i] = tags[i].toString();
            }
            return res;
        }
    }
}
