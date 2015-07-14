package org.wordpress.android.ui.prefs;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.HashMap;
import java.util.Locale;

/**
 * Handles changes to WordPress site settings. Syncs with host automatically when user leaves.
 */

public class SiteSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener,
                   MenuItem.OnMenuItemClickListener {
    private HashMap<String, String> mLanguageCodes = new HashMap<>();
    private Blog mBlog;
    private MenuItem mUndoItem;
    private EditTextPreference mTitlePreference;
    private EditTextPreference mTaglinePreference;
    private EditTextPreference mAddressPreference;
    private ListPreference mLanguagePreference;
    private ListPreference mPrivacyPreference;

    // Most recent remote site data. Current local data is used if remote data cannot be fetched.
    private String mRemoteTitle;
    private String mRemoteTagline;
    private String mRemoteAddress;
    private int mRemotePrivacy;
    private String mRemoteLanguage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // make sure we have local site data
        mBlog = WordPress.getBlog(
                getArguments().getInt(BlogPreferencesActivity.ARG_LOCAL_BLOG_ID, -1));
        if (mBlog == null) return;

        // inflate Site Settings preferences from XML
        addPreferencesFromResource(R.xml.site_settings);

        // declare an options menu for this fragment
        setHasOptionsMenu(true);

        // set preference references, add change listeners, and setup various entries and values
        initPreferences();

        // fetch remote site data
        fetchRemoteData();
    }

    @Override
    public void onPause() {
        super.onPause();

        applyChanges();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        undoChanges();

        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);

        menuInflater.inflate(R.menu.site_settings, menu);

        if (menu == null || (mUndoItem = menu.findItem(R.id.save_site_settings)) == null) return;

        mUndoItem.setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue == null) return false;

        if (preference == mTitlePreference) {
            // update the Activity title to reflect the changes
            String titleFormat = getString(R.string.site_settings_title_format, "%s");
            getActivity().setTitle(String.format(titleFormat, newValue));

            changeEditTextPreferenceValue(mTitlePreference, newValue.toString());
            return true;
        } else if (preference == mTaglinePreference) {
            changeEditTextPreferenceValue(mTaglinePreference, newValue.toString());
            return true;
        } else if (preference == mAddressPreference) {
            changeEditTextPreferenceValue(mAddressPreference, newValue.toString());
            return true;
        } else if (preference == mLanguagePreference) {
            changeLanguageValue(newValue.toString());
            return true;
        } else if (preference == mPrivacyPreference) {
            mPrivacyPreference.setSummary(privacyStringForValue(Integer.valueOf(newValue.toString())));
            return true;
        }

        return false;
    }

    private String privacyStringForValue(int value) {
        switch (value) {
            case -1:
                return"I would like my site to be private, visible only to users I choose";
            case 0:
                return "Discourage search engines from indexing this site";
            case 1:
                return "Allow search engines to index this site";
        }
    }

    /**
     * Helper method to perform validation and set multiple properties on an EditTextPreference.
     * If newValue is equal to the current preference text no action will be taken.
     */
    private void changeEditTextPreferenceValue(EditTextPreference pref, String newValue) {
        if (pref != null && newValue != null && !newValue.equals(pref.getText())) {
            pref.setText(newValue);
            pref.setSummary(newValue);
        }
    }

    private void changeLanguageValue(String newValue) {
        if (mLanguagePreference != null && !mLanguagePreference.getValue().equals(newValue)) {
            mLanguagePreference.setValue(newValue);
            mLanguagePreference.setSummary(StringUtils.getLanguageString(newValue));
        }
    }

    private void changePrivacyValue(int newValue) {
        if (mPrivacyPreference != null && Integer.valueOf(mPrivacyPreference.getValue()) == newValue) {
            mPrivacyPreference.setValue(String.valueOf(newValue));
            mPrivacyPreference.setSummary(String.valueOf(newValue));
        }
    }

    /**
     * Request remote site data via the WordPress REST API.
     */
    private void fetchRemoteData() {
        WordPress.getRestClientUtils().getGeneralSettings(
                String.valueOf(mBlog.getRemoteBlogId()), new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        handleResponseToGeneralSettingsRequest(response);
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (isAdded()) {
                            ToastUtils.showToast(getActivity(), "Error getting site info");
                        }
                    }
                });
    }

    /**
     * Helper method to parse JSON response to REST request.
     */
    private void handleResponseToGeneralSettingsRequest(JSONObject response) {
        mRemoteTitle = response.optString(RestClientUtils.SITE_TITLE_KEY);
        changeEditTextPreferenceValue(mTitlePreference, mRemoteTitle);

        mRemoteTagline = response.optString(RestClientUtils.SITE_DESC_KEY);
        changeEditTextPreferenceValue(mTaglinePreference, mRemoteTagline);

        mRemoteAddress = response.optString(RestClientUtils.SITE_URL_KEY);
        changeEditTextPreferenceValue(mAddressPreference, mRemoteAddress);

        mRemoteLanguage = StringUtils.getLanguageString(response.optString(RestClientUtils.SITE_LANGUAGE_KEY));
        changeLanguageValue(mRemoteLanguage);
        if (mLanguagePreference != null
                && mRemoteLanguage != null
                && !mRemoteLanguage.equals(mLanguagePreference.getValue())) {
            mLanguagePreference.setValue(mRemoteLanguage);
            mLanguagePreference.setSummary(mRemoteLanguage);
        }

        mRemotePrivacy = response.optJSONObject("settings").optInt("blog_public");
        if (mPrivacyPreference != null) {
            mPrivacyPreference.setValue(String.valueOf(mRemotePrivacy));
        }
        changePrivacyValue(mRemotePrivacy);

//        if (mTitlePreference != null) {
//            mTitlePreference.setText(mRemoteTitle);
//            mTitlePreference.setSummary(mRemoteTitle);
//        }
//
//        if (mTaglinePreference != null) {
//            mTaglinePreference.setText(mRemoteTagline);
//            mTaglinePreference.setSummary(mRemoteTagline);
//        }
//
//        if (mAddressPreference != null) {
//            mAddressPreference.setText(mRemoteAddress);
//            mAddressPreference.setSummary(mRemoteAddress);
//        }
//
//        if (mLanguagePreference != null) {
//        }
//
//        if (mPrivacyPreference != null) {
//            mPrivacyPreference.setValue(String.valueOf(mRemotePrivacy));
//        }
    }

    /**
     * Helper method to create the parameters for the site settings POST request
     */
    private HashMap<String, String> generatePostParams() {
        HashMap<String, String> params = new HashMap<>();

        // Using undocumented endpoint WPCOM_JSON_API_Site_Settings_Endpoint
        // https://wpcom.trac.automattic.com/browser/trunk/public.api/rest/json-endpoints.php#L1903
        if (mTitlePreference != null && !mTitlePreference.getText().equals(mRemoteTitle)) {
            params.put("blogname", mTitlePreference.getText());
        }

        if (mTaglinePreference != null && !mTaglinePreference.getText().equals(mRemoteTagline)) {
            params.put("blogdescription", mTaglinePreference.getText());
        }

        if (mLanguagePreference != null &&
                mLanguageCodes.containsKey(mLanguagePreference.getValue()) &&
                !mRemoteLanguage.equals(mLanguageCodes.get(mLanguagePreference.getValue()))) {
            params.put("lang_id", String.valueOf(mLanguageCodes.get(mLanguagePreference.getValue())));
        }

        if (mPrivacyPreference != null && Integer.valueOf(mPrivacyPreference.getValue()) != mRemotePrivacy) {
            params.put("blog_public", mPrivacyPreference.getValue());
        }

        return params;
    }

    /**
     * Reverts changed preferences
     */
    private void undoChanges() {
        changeEditTextPreferenceValue(mTitlePreference, mRemoteTitle);
        changeEditTextPreferenceValue(mTaglinePreference, mRemoteTagline);
        changeEditTextPreferenceValue(mAddressPreference, mRemoteAddress);

        // Privacy must exist in the set {-1, 0, 1} to be valid
        if (mRemotePrivacy > -2 && mRemotePrivacy < 2 &&
                mRemotePrivacy != Integer.valueOf(mPrivacyPreference.getValue())) {
            mPrivacyPreference.setValue(String.valueOf(mRemotePrivacy));
        }

        if (mRemoteLanguage != null && !mRemoteLanguage.equals(StringUtils.getLanguageString(mLanguagePreference.getValue()))) {
            mLanguagePreference.setValue(mRemoteLanguage);
        }
    }

    /**
     * Persists changed settings remotely
     */
    private void applyChanges() {
        final HashMap<String, String> params = generatePostParams();

        if (params.size() > 0) {
            WordPress.getRestClientUtils().setGeneralSiteSettings(
                    String.valueOf(mBlog.getRemoteBlogId()), new RestRequest.Listener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // Update local Blog name
                            if (params.containsKey("blogname")) {
                                mBlog.setBlogName(params.get("blogname"));
                            }
                        }
                    }, new RestRequest.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (mTaglinePreference != null) {
                                mTaglinePreference.setEnabled(false);
                                mTaglinePreference.setSummary("Failed to retrieve :(");
                            }
                        }
                    }, params);
        }
    }

    /**
     * Helper method to setup preferences and set initial values
     */
    private void initPreferences() {
        mTitlePreference =
                (EditTextPreference) findPreference(getString(R.string.pref_key_site_title));
        mTaglinePreference =
                (EditTextPreference) findPreference(getString(R.string.pref_key_site_tagline));
        mAddressPreference =
                (EditTextPreference) findPreference(getString(R.string.pref_key_site_address));
        mPrivacyPreference =
                (ListPreference) findPreference(getString(R.string.pref_key_site_visibility));
        mLanguagePreference =
                (ListPreference) findPreference(getString(R.string.pref_key_site_language));

        if (mTitlePreference != null) {
            mTitlePreference.setOnPreferenceChangeListener(this);
        }

        if (mTaglinePreference != null) {
            mTaglinePreference.setOnPreferenceChangeListener(this);
        }

        if (mAddressPreference != null) {
            mAddressPreference.setOnPreferenceChangeListener(this);
        }

        if (mPrivacyPreference != null) {
            mRemotePrivacy = -2;
            mPrivacyPreference.setOnPreferenceChangeListener(this);
        }

        if (mLanguagePreference != null) {
            // Generate map of language codes
            String[] languageIds = getResources().getStringArray(R.array.lang_ids);
            String[] languageCodes = getResources().getStringArray(R.array.language_codes);
            for (int i = 0; i < languageIds.length && i < languageCodes.length; ++i) {
                mLanguageCodes.put(languageCodes[i], languageIds[i]);
            }

            mLanguagePreference.setEntries(
                    createLanguageDisplayStrings(mLanguagePreference.getEntryValues()));
            mLanguagePreference.setOnPreferenceChangeListener(this);
        }
    }

    /**
     * Generates display strings for given language codes. Used as entries in language preference.
     */
    private CharSequence[] createLanguageDisplayStrings(CharSequence[] languageCodes) {
        if (languageCodes == null || languageCodes.length < 1) return null;

        CharSequence[] displayStrings = new CharSequence[languageCodes.length];

        for (int i = 0; i < languageCodes.length; ++i) {
            displayStrings[i] = StringUtils.getLanguageString(String.valueOf(languageCodes[i]), Locale.getDefault());
        }

        return displayStrings;
    }

    /**
     * Return a non-null display string for a given language code.
     */
//    private String getLanguageString(String languagueCode) {
//        if (languagueCode == null || languagueCode.length() < 2) {
//            return "";
//        } else if (languagueCode.length() == 2) {
//            return new Locale(languagueCode).getDisplayLanguage();
//        } else {
//            return new Locale(languagueCode.substring(0, 2)).getDisplayLanguage() + languagueCode.substring(2);
//        }
//    }
}
