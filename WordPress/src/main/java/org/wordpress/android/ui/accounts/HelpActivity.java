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
import org.wordpress.android.support.ZendeskHelper;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.AppLogViewerActivity;
import org.wordpress.android.util.HelpshiftHelper;
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
        ZendeskHelper.createNewTicket(this, mAccountStore, mSiteStore, getOriginTagFromExtras());
    }

    private void showZendeskTickets() {
        ZendeskHelper.showAllTickets(this, mAccountStore, mSiteStore, getOriginTagFromExtras());
    }

    private void showZendeskFaq() {
        ZendeskHelper.showZendeskHelpCenter(this, mAccountStore);
    }

    private Tag getOriginTagFromExtras() {
        Bundle extras = getIntent().getExtras();
        Tag origin = Tag.ORIGIN_UNKNOWN;
        if (extras != null) {
            origin = (Tag) extras.get(HelpshiftHelper.ORIGIN_KEY);
        }
        return origin;
    }
}
