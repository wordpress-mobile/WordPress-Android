package org.wordpress.android.ui.prefs;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.CommentBroadcastReceiver;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.accounts.NewAccountActivity;
import org.wordpress.android.util.CommentService;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.EscapeUtils;

public class PreferencesActivity extends SherlockPreferenceActivity {

    ListPreference notificationIntervalPreference;
    EditTextPreference taglineTextPreference;
    OnPreferenceChangeListener preferenceChangeListener;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setTitle(getResources().getText(R.string.settings));
        
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        if (WordPress.currentBlog == null) {
            try {
                WordPress.currentBlog = new Blog(WordPress.wpDB.getLastBlogId());
            } catch (Exception e) {
                Toast.makeText(this, getResources().getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        addPreferencesFromResource(R.xml.preferences);

        preferenceChangeListener = new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                // Set summary to changed value
                preference.setSummary(newValue.toString());
                return true;
            }
        };

        notificationIntervalPreference = (ListPreference) findPreference("wp_pref_notifications_interval");
        notificationIntervalPreference.setOnPreferenceChangeListener(preferenceChangeListener);
        taglineTextPreference = (EditTextPreference) findPreference("wp_pref_post_signature");
        taglineTextPreference.setOnPreferenceChangeListener(preferenceChangeListener);

        displayPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();

        // the set of blogs may have changed while we were away
        updateBlogsPreferenceCategory();
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Update the "blogs" preference category to contain a preference for each blog to configure
     * blog-specific settings. This also adds an "add blog" preference for setting up new blogs.
     */
    protected void updateBlogsPreferenceCategory() {
        PreferenceCategory blogsCategory = (PreferenceCategory) findPreference("wp_pref_category_blogs");
        blogsCategory.removeAll();

        List<Map<String, Object>> accounts = WordPress.wpDB.getAccounts();
        for (Map<String, Object> account : accounts) {
            String blogName = EscapeUtils.unescapeHtml(account.get("blogName").toString());
            int accountId = (Integer) account.get("id");

            Preference blogSettingsPreference = new Preference(this);
            blogSettingsPreference.setTitle(blogName);

            try {
                // set blog hostname as preference summary if it differs from the blog name
                URL blogUrl = new URL(account.get("url").toString());
                if (!blogName.equals(blogUrl.getHost())) {
                    blogSettingsPreference.setSummary(blogUrl.getHost());
                }
            } catch (MalformedURLException e) {
                // do nothing
            }

            Intent intent = new Intent(this, BlogPreferencesActivity.class);
            intent.putExtra("id", accountId);
            blogSettingsPreference.setIntent(intent);
            blogSettingsPreference.setOrder(0);
            blogsCategory.addPreference(blogSettingsPreference);
        }

        Preference addBlogPreference = new Preference(this);
        addBlogPreference.setTitle(R.string.add_account);
        Intent intent = new Intent(this, NewAccountActivity.class);
        addBlogPreference.setIntent(intent);
        addBlogPreference.setOrder(1);
        blogsCategory.addPreference(addBlogPreference);
    }

    protected int getEnabledBlogsCount() {
        PreferenceScreen selectBlogsCategory = (PreferenceScreen) findPreference("wp_pref_notification_blogs");
        int enabledBlogCtr = 0;
        for (int i = 0; i < selectBlogsCategory.getPreferenceCount(); i++) {
            CheckBoxPreference blogPreference = (CheckBoxPreference) selectBlogsCategory.getPreference(i);
            if (blogPreference.isChecked())
                enabledBlogCtr++;
        }
        return enabledBlogCtr;
    }

    public void displayPreferences() {
        List<Map<String, Object>> accounts = WordPress.wpDB.getAccounts();
        if (accounts.size() > 0) {

            for (int i = 0; i < accounts.size(); i++) {

                Map<String, Object> curHash = accounts.get(i);
                String curBlogName = curHash.get("blogName").toString();
                String accountID = curHash.get("id").toString();
                int runService = Integer.valueOf(curHash.get("runService").toString());

                PreferenceScreen selectBlogsCategory = (PreferenceScreen) findPreference("wp_pref_notification_blogs");

                CheckBoxPreference blogNotificationPreference = new CheckBoxPreference(this);
                blogNotificationPreference.setKey(accountID);
                blogNotificationPreference.setTitle(EscapeUtils.unescapeHtml(curBlogName));
                blogNotificationPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        int blogID;
                        try {
                            blogID = Integer.valueOf(preference.getKey());
                            WordPress.wpDB.updateNotificationFlag(blogID, (Boolean) newValue);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                });
                if (runService == 1)
                    blogNotificationPreference.setChecked(true);
                else
                    blogNotificationPreference.setChecked(false);

                selectBlogsCategory.addPreference(blogNotificationPreference);
            }
        }

        if (taglineTextPreference.getText() == null || taglineTextPreference.getText().equals("")) {
            if (DeviceUtils.getInstance().isBlackBerry()) {
                taglineTextPreference.setSummary(R.string.posted_from_blackberry);
                taglineTextPreference.setText(getString(R.string.posted_from_blackberry));
            } else {
                taglineTextPreference.setSummary(R.string.posted_from);
                taglineTextPreference.setText(getString(R.string.posted_from));
            }
        } else {
            taglineTextPreference.setSummary(taglineTextPreference.getText());
        }

        if (notificationIntervalPreference.getValue() == null || notificationIntervalPreference.getValue().equals("")) {
            notificationIntervalPreference.setValue("10 Minutes");
            notificationIntervalPreference.setSummary("10 Minutes");
        } else {
            notificationIntervalPreference.setSummary(notificationIntervalPreference.getValue());
        }
         
        if (DeviceUtils.getInstance().isBlackBerry()) {
            PreferenceCategory appAboutSectionName = (PreferenceCategory) findPreference("wp_pref_app_about_section");
            appAboutSectionName.setTitle(getString(R.string.app_title_blackberry));
            Preference appName = (Preference) findPreference("wp_pref_app_title");
            appName.setTitle(getString(R.string.app_title_blackberry));
        }
    }

    @Override
    protected void onPause() {
        if (getEnabledBlogsCount() > 0) {

            int UPDATE_INTERVAL = 3600000;
            ListPreference notificationIntervalPreference = (ListPreference) findPreference("wp_pref_notifications_interval");
            String notificationInterval = notificationIntervalPreference.getValue();
            // configure time interval
            if (notificationInterval.equals("5 Minutes")) {
                UPDATE_INTERVAL = 300000;
            } else if (notificationInterval.equals("10 Minutes")) {
                UPDATE_INTERVAL = 600000;
            } else if (notificationInterval.equals("15 Minutes")) {
                UPDATE_INTERVAL = 900000;
            } else if (notificationInterval.equals("30 Minutes")) {
                UPDATE_INTERVAL = 1800000;
            } else if (notificationInterval.equals("1 Hour")) {
                UPDATE_INTERVAL = 3600000;
            } else if (notificationInterval.equals("3 Hours")) {
                UPDATE_INTERVAL = 10800000;
            } else if (notificationInterval.equals("6 Hours")) {
                UPDATE_INTERVAL = 21600000;
            } else if (notificationInterval.equals("12 Hours")) {
                UPDATE_INTERVAL = 43200000;
            } else if (notificationInterval.equals("Daily")) {
                UPDATE_INTERVAL = 86400000;
            }

            // TODO: start service after reboot?
            Intent intent = new Intent(PreferencesActivity.this, CommentBroadcastReceiver.class);
            PendingIntent pIntent = PendingIntent.getBroadcast(PreferencesActivity.this, 0, intent, 0);

            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (5 * 1000), UPDATE_INTERVAL, pIntent);

        } else {
            Intent stopIntent = new Intent(PreferencesActivity.this, CommentBroadcastReceiver.class);
            PendingIntent stopPIntent = PendingIntent.getBroadcast(PreferencesActivity.this, 0, stopIntent, 0);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.cancel(stopPIntent);

            Intent service = new Intent(PreferencesActivity.this, CommentService.class);
            stopService(service);
        }
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        super.onPause();
    }

}
