package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.AppLogViewerActivity;
import org.wordpress.android.util.ABTestingUtils;
import org.wordpress.android.util.ABTestingUtils.Feature;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.HelpshiftHelper.TAGS;
import org.wordpress.android.widgets.WPTextView;

public class NuxHelpActivity extends Activity {
    final private static String FAQ_URL = "http://android.wordpress.org/faq/";
    final private static String FORUM_URL = "http://android.forums.wordpress.org/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (ABTestingUtils.isFeatureEnabled(Feature.HELPSHIFT)) {
            initHelpshiftLayout();
        } else {
            initDefaultLayout();
        }

        // Init common elements
        WPTextView version = (WPTextView) findViewById(R.id.nux_help_version);
        version.setText(getString(R.string.version) + " " + WordPress.versionName);

        WPTextView applogButton = (WPTextView) findViewById(R.id.applog_button);
        applogButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(v.getContext(), AppLogViewerActivity.class));
            }
        });
    }

    private void initHelpshiftLayout() {
        setContentView(R.layout.activity_nux_help_with_helpshift);

        WPTextView version = (WPTextView) findViewById(R.id.nux_help_version);
        version.setText(getString(R.string.version) + " " + WordPress.versionName);


        WPTextView helpCenterButton = (WPTextView) findViewById(R.id.contact_us_button);
        helpCenterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle extras = getIntent().getExtras();
                if (extras != null) {
                    // This could be moved to WelcomeFragmentSignIn directly, but better to have all Helpshift
                    // related code at the same place (Note: value can be null).
                    HelpshiftHelper.getInstance().addMetaData("user-entered-url", extras.getString(
                            WelcomeFragmentSignIn.ENTERED_URL_KEY));
                    HelpshiftHelper.getInstance().addMetaData("user-entered-username", extras.getString(
                            WelcomeFragmentSignIn.ENTERED_USERNAME_KEY));
                }
                HelpshiftHelper.getInstance().setTags(new TAGS[] {TAGS.LOGIN_SCREEN});
                HelpshiftHelper.getInstance().showConversation(NuxHelpActivity.this);
            }
        });
    }

    private void initDefaultLayout() {
        setContentView(R.layout.activity_nux_help);

        WPTextView helpCenterButton = (WPTextView) findViewById(R.id.help_button);
        helpCenterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(FAQ_URL)));
            }
        });

        WPTextView forumButton = (WPTextView) findViewById(R.id.forum_button);
        forumButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(FORUM_URL)));
            }
        });
   }
}