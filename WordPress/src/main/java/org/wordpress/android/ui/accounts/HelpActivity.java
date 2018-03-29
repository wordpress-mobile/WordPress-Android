package org.wordpress.android.ui.accounts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.AppLogViewerActivity;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.HelpshiftHelper.MetadataKey;
import org.wordpress.android.util.HelpshiftHelper.Tag;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.widgets.WPTextView;

import javax.inject.Inject;

public class HelpActivity extends AppCompatActivity {
    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

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

        WPTextView applogButton = findViewById(R.id.applog_button);
        applogButton.setOnClickListener(new OnClickListener() {
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
            @Override
            public void onClick(View v) {
                Bundle extras = getIntent().getExtras();
                Tag origin = Tag.ORIGIN_UNKNOWN;
                Tag[] extraTags = null;
                if (extras != null) {
                    // This could be moved to WelcomeFragmentSignIn directly, but better to have all Helpshift
                    // related code at the same place (Note: value can be null).
                    HelpshiftHelper.getInstance().addMetaData(MetadataKey.USER_ENTERED_URL, extras.getString(
                            HelpshiftHelper.ENTERED_URL_KEY));
                    HelpshiftHelper.getInstance().addMetaData(MetadataKey.USER_ENTERED_USERNAME, extras.getString(
                            HelpshiftHelper.ENTERED_USERNAME_KEY));
                    HelpshiftHelper.getInstance().addMetaData(MetadataKey.USER_ENTERED_EMAIL, extras.getString(
                            HelpshiftHelper.ENTERED_EMAIL_KEY));
                    origin = (Tag) extras.get(HelpshiftHelper.ORIGIN_KEY);
                    extraTags = (Tag[]) extras.get(HelpshiftHelper.EXTRA_TAGS_KEY);
                }

                HelpshiftHelper.getInstance().showConversation(HelpActivity.this, mSiteStore, origin,
                                                               mAccountStore.getAccount().getUserName(), extraTags);
            }
        });

        WPTextView faqbutton = findViewById(R.id.faq_button);
        faqbutton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle extras = getIntent().getExtras();
                Tag origin = Tag.ORIGIN_UNKNOWN;
                Tag[] extraTags = null;
                if (extras != null) {
                    origin = (Tag) extras.get(HelpshiftHelper.ORIGIN_KEY);
                    extraTags = (Tag[]) extras.get(HelpshiftHelper.EXTRA_TAGS_KEY);
                }
                HelpshiftHelper.getInstance().showFAQ(HelpActivity.this, mSiteStore, origin,
                                                      mAccountStore.getAccount().getUserName(), extraTags);
            }
        });
    }
}
