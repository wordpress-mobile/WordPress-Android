package org.wordpress.android.ui.prefs;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.StringMap;

import org.wordpress.android.util.StringUtils;
import org.xmlrpc.android.WPComXMLRPCApi;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.passcodelock.AppLockManager;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.accounts.AccountSetupActivity;
import org.wordpress.android.ui.accounts.NewAccountActivity;
import org.wordpress.android.util.DeviceUtils;

@SuppressWarnings("deprecation")
public class PreferencesActivity extends SherlockPreferenceActivity {

    EditTextPreference taglineTextPreference;
    OnPreferenceChangeListener preferenceChangeListener;
    
    private Object[] mTypeList;
    private ArrayList<StringMap<Double>> mMutedBlogsList;
    private Map<String, Object> mNotificationSettings;
    private SharedPreferences mSettings;
    
    private PreferenceGroup mNotificationsGroup;
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        overridePendingTransition(R.anim.slide_up, R.anim.do_nothing);

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
        
        mNotificationsGroup = (PreferenceGroup)findPreference("wp_pref_notifications_category");

        preferenceChangeListener = new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                // Set summary to changed value
                preference.setSummary(newValue.toString());
                return true;
            }
        };

        taglineTextPreference = (EditTextPreference) findPreference("wp_pref_post_signature");
        taglineTextPreference.setOnPreferenceChangeListener(preferenceChangeListener);
        
        Preference signOutPreference = (Preference) findPreference("wp_pref_sign_out");
        signOutPreference.setOnPreferenceClickListener(signOutPreferenceClickListener);
        
        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Request notification settings if needed
        if (WordPress.hasValidWPComCredentials(PreferencesActivity.this)) {
            String settingsJson = mSettings.getString("wp_pref_notification_settings", null);
            if (settingsJson == null) {
                new WPComXMLRPCApi().getNotificationSettings(new XMLRPCCallback() {
                    public void onSuccess(long id, Object result) {
                        refreshWPComAuthCategory();
                    }

                    public void onFailure(long id, XMLRPCException error) {
                        // prompt?
                    }
                }, this);
            }
        }
        
        //Passcode Lock not supported
        if( AppLockManager.getInstance().isAppLockFeatureEnabled() == false ) {
            PreferenceScreen rootScreen = (PreferenceScreen)findPreference("wp_pref_root");
            PreferenceGroup passcodeGroup = (PreferenceGroup)findPreference("wp_passcode_lock_category");
            rootScreen.removePreference(passcodeGroup);
        } else {
            final CheckBoxPreference passcodeEnabledCheckBoxPreference = (CheckBoxPreference) findPreference("wp_pref_passlock_enabled");
            //disable on-click changes on the property
            passcodeEnabledCheckBoxPreference.setOnPreferenceClickListener(
                    new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            passcodeEnabledCheckBoxPreference.setChecked( AppLockManager.getInstance().getCurrentAppLock().isPasswordLocked() ); 
                            return false;
                        }
                    }
                    );
        }
        
        displayPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();

        // the set of blogs may have changed while we were away
        updateBlogsPreferenceCategory();
        
        //update Passcode lock row if available
        if( AppLockManager.getInstance().isAppLockFeatureEnabled() ) {
            CheckBoxPreference passcodeEnabledCheckBoxPreference = (CheckBoxPreference) findPreference("wp_pref_passlock_enabled");
            if ( AppLockManager.getInstance().getCurrentAppLock().isPasswordLocked() ) { 
                passcodeEnabledCheckBoxPreference.setChecked(true);
            } else {
                passcodeEnabledCheckBoxPreference.setChecked(false);
            }
        }
    }
    
    @Override
    protected void onPause() {
        overridePendingTransition(R.anim.do_nothing, R.anim.slide_down);
        setResult(RESULT_OK);
        super.onPause();
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
        int order = 0;
        for (Map<String, Object> account : accounts) {
            String blogName = StringUtils.unescapeHTML(account.get("blogName").toString());
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
            blogSettingsPreference.setOrder(order++);
            blogsCategory.addPreference(blogSettingsPreference);
        }

        Preference addBlogPreference = new Preference(this);
        addBlogPreference.setTitle(R.string.add_account);
        Intent intent = new Intent(this, NewAccountActivity.class);
        addBlogPreference.setIntent(intent);
        addBlogPreference.setOrder(order++);
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
        
        // WordPress.com auth area and notifications
        refreshWPComAuthCategory();
        
        // Post signature
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
         
        if (DeviceUtils.getInstance().isBlackBerry()) {
            PreferenceCategory appAboutSectionName = (PreferenceCategory) findPreference("wp_pref_app_about_section");
            appAboutSectionName.setTitle(getString(R.string.app_title_blackberry));
            Preference appName = (Preference) findPreference("wp_pref_app_title");
            appName.setTitle(getString(R.string.app_title_blackberry));
        }
    }
    
    /**
     * Listens for changes to notification type settings
     */
    private OnPreferenceChangeListener mTypeChangeListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            // Update the mNoteSettings map with the new value
            if (preference instanceof CheckBoxPreference) {
                CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
                boolean isChecked = (Boolean) newValue;
                String key = preference.getKey();
                StringMap<Integer> typeMap = (StringMap<Integer>) mNotificationSettings.get(key);
                typeMap.put("value", (isChecked) ? 1 : 0);
                mNotificationSettings.put(key, typeMap);
                checkBoxPreference.setChecked(isChecked);
                new sendNotificationSettingsTask().execute();
            }
            return false;
        }
    };
    
    /**
     * Listens for changes to notification blogs settings
     */
    private OnPreferenceChangeListener mMuteBlogChangeListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference instanceof CheckBoxPreference) {
                CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
                boolean isChecked = (Boolean) newValue;
                int id = checkBoxPreference.getOrder();
                StringMap<Double> blogMap = (StringMap<Double>) mMutedBlogsList.get(id);
                blogMap.put("value", (!isChecked) ? 1.0 : 0.0);
                mMutedBlogsList.set(id, blogMap);
                StringMap<ArrayList> mutedBlogsMap = (StringMap<ArrayList>) mNotificationSettings.get("muted_blogs");
                mutedBlogsMap.put("value", mMutedBlogsList);
                mNotificationSettings.put("muted_blogs", mutedBlogsMap);
                checkBoxPreference.setChecked(isChecked);
                new sendNotificationSettingsTask().execute();
            }
            return false;
        }
    };
    
    /**
     * Listens for changes to notification enabled toggle
     */
    private OnPreferenceChangeListener mNotificationsEnabledChangeListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference instanceof CheckBoxPreference) {
                final boolean isChecked = (Boolean) newValue;
                if (isChecked) {
                    StringMap<String> muteUntilMap = (StringMap<String>) mNotificationSettings.get("mute_until");
                    muteUntilMap.put("value", "0");
                    mNotificationSettings.put("mute_until", muteUntilMap);
                    new sendNotificationSettingsTask().execute();
                    return true;
                } else {
                    final Dialog dialog = new Dialog(PreferencesActivity.this);
                    dialog.setContentView(R.layout.notifications_enabled_dialog);
                    dialog.setTitle(R.string.notifications);
                    dialog.setCancelable(true);
            
                    Button offButton = (Button) dialog.findViewById(R.id.notificationsOff);
                    offButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            enabledButtonClick(v);
                            dialog.dismiss();
                        }   
                    });
                    Button oneHourButton = (Button) dialog.findViewById(R.id.notifications1Hour);
                    oneHourButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            enabledButtonClick(v);
                            dialog.dismiss();
                        }   
                    });
                    Button eightHoursButton = (Button) dialog.findViewById(R.id.notifications8Hours);
                    eightHoursButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            enabledButtonClick(v);
                            dialog.dismiss();
                        }   
                    });
                    dialog.show();
                }
                
            }
            return false;
        }
    };
    
    private void enabledButtonClick(View v) {
        StringMap<String> muteUntilMap = (StringMap<String>) mNotificationSettings
                .get("mute_until");
        if (muteUntilMap != null) {
            if (v.getId() == R.id.notificationsOff) {
                muteUntilMap.put("value", "forever");
            } else if (v.getId() == R.id.notifications1Hour) {
                muteUntilMap.put("value",
                        String.valueOf((System.currentTimeMillis() / 1000) + 3600));
            } else if (v.getId() == R.id.notifications8Hours) {
                muteUntilMap.put("value",
                        String.valueOf((System.currentTimeMillis() / 1000) + (3600 * 8)));
            }
            CheckBoxPreference enabledCheckBoxPreference = (CheckBoxPreference) findPreference("wp_pref_notifications_enabled");
            enabledCheckBoxPreference.setChecked(false);
            mNotificationSettings.put("mute_until", muteUntilMap);
            new sendNotificationSettingsTask().execute();
        }
    }
    
    /**
     * Performs the notification settings save in the background
     */
    private class sendNotificationSettingsTask extends AsyncTask<Object, Object, Object> {
        
        // Sends updated notification settings to WP.com

        @Override
        protected Object doInBackground(Object... params) {
            
            if (mNotificationSettings != null) {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(PreferencesActivity.this);
                SharedPreferences.Editor editor = settings.edit();
                Gson gson = new Gson();
                String settingsJson = gson.toJson(mNotificationSettings);
                editor.putString("wp_pref_notification_settings", settingsJson);
                editor.commit();
                new WPComXMLRPCApi().setNotificationSettings(PreferencesActivity.this);
            } 
            return null;
        } 
    }

    private void refreshWPComAuthCategory() {
        PreferenceCategory wpcomCategory = (PreferenceCategory) findPreference("wp_pref_wpcom_auth");
        wpcomCategory.removeAll();
        
        if (WordPress.hasValidWPComCredentials(PreferencesActivity.this)) {
            String username = mSettings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
            Preference usernamePref = new Preference(this);
            usernamePref.setTitle(getString(R.string.username));
            usernamePref.setSummary(username);
            usernamePref.setSelectable(false);
            
            wpcomCategory.addPreference(usernamePref);
            
            loadNotifications();
        } else {
            Preference signInPref = new Preference(this);
            signInPref.setTitle(getString(R.string.sign_in));
            signInPref.setOnPreferenceClickListener(signInPreferenceClickListener);
            wpcomCategory.addPreference(signInPref);
            
            PreferenceScreen rootScreen = (PreferenceScreen)findPreference("wp_pref_root");
            rootScreen.removePreference(mNotificationsGroup);
        }
    }

    
    private static Comparator<StringMap<?>> BlogNameComparatorForMutedBlogsList = new Comparator<StringMap<?>>() {
        
        public int compare(StringMap<?> blog1, StringMap<?> blog2) {
 
            StringMap<?> blogMap1 = (StringMap<?>)blog1;
            StringMap<?> blogMap2 = (StringMap<?>)blog2;
            
            String blogName1 = blogMap1.get("blog_name").toString();
            if (blogName1.length() == 0) {
                blogName1 = blogMap1.get("url").toString();
            }
            
            String blogName2 = blogMap2.get("blog_name").toString();
            if (blogName2.length() == 0) {
                blogName2 = blogMap2.get("url").toString();
            }
            
          return blogName1.compareToIgnoreCase(blogName2);
 
        }
 
    };
    
    private void loadNotifications() {
        
        // Add notifications group back in case it was previously removed from being logged out
        PreferenceScreen rootScreen = (PreferenceScreen)findPreference("wp_pref_root");
        rootScreen.addPreference(mNotificationsGroup);
        
        PreferenceCategory notificationTypesCategory = (PreferenceCategory) findPreference("wp_pref_notification_types");
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        String settingsJson = settings.getString("wp_pref_notification_settings", null);
        if (settingsJson == null) {
            rootScreen.removePreference(mNotificationsGroup);
            return;
        } else {
            try {
                Gson gson = new Gson();
                mNotificationSettings = gson.fromJson(settingsJson, HashMap.class);
                StringMap<?> mutedBlogsMap = (StringMap<?>) mNotificationSettings.get("muted_blogs");
                mMutedBlogsList = (ArrayList<StringMap<Double>>) mutedBlogsMap.get("value");
                Collections.sort(mMutedBlogsList, this.BlogNameComparatorForMutedBlogsList);
                               
                mTypeList = mNotificationSettings.keySet().toArray();
                
                for (int i = 0; i < mTypeList.length; i++) {
                    if (!mTypeList[i].equals("muted_blogs") && !mTypeList[i].equals("mute_until")) {
                        StringMap<?> typeMap = (StringMap<?>) mNotificationSettings
                                .get(mTypeList[i].toString());
                        CheckBoxPreference typePreference = new CheckBoxPreference(this);
                        typePreference.setKey(mTypeList[i].toString());
                        typePreference.setChecked(typeMap.get("value").toString().equals("1"));
                        typePreference.setTitle(typeMap.get("desc").toString());
                        typePreference.setOnPreferenceChangeListener(mTypeChangeListener);
                        notificationTypesCategory.addPreference(typePreference);
                    }
                }

                PreferenceCategory selectBlogsCategory = (PreferenceCategory) findPreference("wp_pref_notification_blogs");
                for (int i = 0; i < mMutedBlogsList.size(); i++) {
                    StringMap<?> blogMap = (StringMap<?>) mMutedBlogsList.get(i);
                    String blogName = (String) blogMap.get("blog_name");
                    if (blogName == null || blogName.trim().equals(""))
                        blogName = (String) blogMap.get("url");
                    CheckBoxPreference blogPreference = new CheckBoxPreference(this);
                    blogPreference.setChecked(!blogMap.get("value").toString().equals("1"));
                    blogPreference.setTitle(StringUtils.unescapeHTML(blogName));
                    blogPreference.setOnPreferenceChangeListener(mMuteBlogChangeListener);
                    selectBlogsCategory.addPreference(blogPreference);
                }

            } catch (JsonSyntaxException e) {
                Log.v("WORDPRESS", "Notification Settings Json could not be parsed.");
                return;
            } catch (Exception e) {
                Log.v("WORDPRESS", "Failed to load notification settings.");
                return;
            }
            
            CheckBoxPreference notificationsEnabledCheckBox = (CheckBoxPreference) findPreference("wp_pref_notifications_enabled");
            notificationsEnabledCheckBox.setOnPreferenceChangeListener(mNotificationsEnabledChangeListener);
            
        }
    }

    private OnPreferenceClickListener signInPreferenceClickListener = new OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            Intent i = new Intent(PreferencesActivity.this, AccountSetupActivity.class);
            i.putExtra("wpcom", true);
            i.putExtra("auth-only", true);
            startActivityForResult(i, 0);
            return true;
        }
    };
    
    private OnPreferenceClickListener signOutPreferenceClickListener = new OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(PreferencesActivity.this);
            dialogBuilder.setTitle(getResources().getText(R.string.sign_out));
            dialogBuilder.setMessage(getString(R.string.sign_out_confirm));
            dialogBuilder.setPositiveButton(R.string.sign_out,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                            WordPress.signOut(PreferencesActivity.this);
                            finish();
                        }
                    });
            dialogBuilder.setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                            // Just close the window.
                        }
                    });
            dialogBuilder.setCancelable(true);
            if (!isFinishing())
                dialogBuilder.create().show();
            return true;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        refreshWPComAuthCategory();
        super.onActivityResult(requestCode, resultCode, data);
    }
}
