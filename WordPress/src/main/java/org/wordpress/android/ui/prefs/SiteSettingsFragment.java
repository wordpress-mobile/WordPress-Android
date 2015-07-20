package org.wordpress.android.ui.prefs;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.HashMap;
import java.util.Locale;

/**
 * Handles changes to WordPress site settings. Syncs with host automatically when user leaves.
 */

public class SiteSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private HashMap<String, String> mLanguageCodes = new HashMap<>();
    private Blog mBlog;
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

        if (!NetworkUtils.checkConnection(getActivity())) {
            getActivity().finish();
        }

        // make sure we have local site data
        mBlog = WordPress.getBlog(
                getArguments().getInt(BlogPreferencesActivity.ARG_LOCAL_BLOG_ID, -1));
        if (mBlog == null) return;

        // inflate Site Settings preferences from XML
        addPreferencesFromResource(R.xml.site_settings);

        // set preference references, add change listeners, and setup various entries and values
        initPreferences();

        // fetch remote site data
        fetchRemoteData();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Assume user wanted changes propagated when they leave
        applyChanges();
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
                return getString(R.string.site_privacy_private_desc);
            case 0:
                return getString(R.string.site_privacy_hidden_desc);
            case 1:
                return getString(R.string.site_privacy_public_desc);
            default:
                return "";
        }
    }

    /**
     * Helper method to perform validation and set multiple properties on an EditTextPreference.
     * If newValue is equal to the current preference text no action will be taken.
     */
    private void changeEditTextPreferenceValue(EditTextPreference pref, String newValue) {
        if (pref != null && newValue != null && !newValue.equals(pref.getSummary())) {
            pref.setText(newValue);
            pref.setSummary(newValue);
        }
    }

    private void changeLanguageValue(String newValue) {
        if (mLanguagePreference != null && !newValue.equals(mLanguagePreference.getValue())) {
            mLanguagePreference.setValue(newValue);
            mLanguagePreference.setSummary(getLanguageString(newValue, Locale.getDefault()));
        }
    }

    private void changePrivacyValue(int newValue) {
        if (mPrivacyPreference != null && Integer.valueOf(mPrivacyPreference.getValue()) == newValue) {
            mPrivacyPreference.setValue(String.valueOf(newValue));
            mPrivacyPreference.setSummary(privacyStringForValue(newValue));
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
                        AppLog.w(AppLog.T.API, "Error GETing site settings: " + error);
                        if (isAdded()) {
                            ToastUtils.showToast(getActivity(), getString(R.string.error_fetch_remote_site_settings));
                            getActivity().finish();
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

        mRemoteLanguage = response.optString(RestClientUtils.SITE_LANGUAGE_KEY);
        if (mLanguagePreference != null) {
            changeLanguageValue(mRemoteLanguage);
            mLanguagePreference.setSummary(getLanguageString(mRemoteLanguage, Locale.getDefault()));
        }

        mRemotePrivacy = response.optJSONObject("settings").optInt("blog_public");
        if (mPrivacyPreference != null) {
            mPrivacyPreference.setValue(String.valueOf(mRemotePrivacy));
        }
        changePrivacyValue(mRemotePrivacy);
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

        if (mAddressPreference != null && !mAddressPreference.getText().equals(mRemoteAddress)) {
            // TODO
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
                            AppLog.w(AppLog.T.API, "Error POSTing site settings changes: " + error);
                            if (isAdded()) {
                                ToastUtils.showToast(getActivity(), getString(R.string.error_post_remote_site_settings));
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
            displayStrings[i] = getLanguageString(String.valueOf(languageCodes[i]), Locale.getDefault());
        }

        return displayStrings;
    }

    /**
     * Return a non-null display string for a given language code.
     */
    private String getLanguageString(String languageCode, Locale displayLocale) {
        if (languageCode == null || languageCode.length() < 2 || languageCode.length() > 6) {
            return "";
        }

        Locale languageLocale = new Locale(languageCode.substring(0, 2));
        return languageLocale.getDisplayLanguage(displayLocale) + languageCode.substring(2);
    }
}
