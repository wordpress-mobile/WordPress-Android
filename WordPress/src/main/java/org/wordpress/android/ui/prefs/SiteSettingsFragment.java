package org.wordpress.android.ui.prefs;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
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
import org.wordpress.android.util.ToastUtils;

import java.util.HashMap;
import java.util.Locale;

/**
 * Handles changes to WordPress site settings. Syncs with host automatically when user leaves.
 */

public class SiteSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private static final HashMap<String, Integer> LANGUAGE_CODES = new HashMap<>();

    static {
        LANGUAGE_CODES.put("az", 79);
        LANGUAGE_CODES.put("en", 1);
        LANGUAGE_CODES.put("de", 15);
        LANGUAGE_CODES.put("el", 17);
        LANGUAGE_CODES.put("es", 19);
        LANGUAGE_CODES.put("fr", 24);
        LANGUAGE_CODES.put("gd", 476);
        LANGUAGE_CODES.put("hi", 30);
        LANGUAGE_CODES.put("hu", 31);
        LANGUAGE_CODES.put("id", 33);
        LANGUAGE_CODES.put("it", 35);
        LANGUAGE_CODES.put("ja", 36);
        LANGUAGE_CODES.put("ko", 40);
        LANGUAGE_CODES.put("nb", -1);// ??
        LANGUAGE_CODES.put("nl", 49);
        LANGUAGE_CODES.put("pl", 58);
        LANGUAGE_CODES.put("ru", 62);
        LANGUAGE_CODES.put("sv", 68);
        LANGUAGE_CODES.put("th", 71);
        LANGUAGE_CODES.put("uz", 458);
        LANGUAGE_CODES.put("zh-CN", 449);
        LANGUAGE_CODES.put("zh-TW", 452);
        LANGUAGE_CODES.put("zh-HK", 452);// ??
        LANGUAGE_CODES.put("en-GB", 482);
        LANGUAGE_CODES.put("tr", 78);
        LANGUAGE_CODES.put("eu", 429);
        LANGUAGE_CODES.put("he", 29);
        LANGUAGE_CODES.put("pt-BR", 438);
        LANGUAGE_CODES.put("ar", 3);
        LANGUAGE_CODES.put("ro", 61);
        LANGUAGE_CODES.put("mk", 435);
        LANGUAGE_CODES.put("en-AU", 482);// ??
        LANGUAGE_CODES.put("sr", 67);
        LANGUAGE_CODES.put("sk", 64);
        LANGUAGE_CODES.put("cy", 13);
        LANGUAGE_CODES.put("da", 14);
    }

    private Blog mBlog;
    private EditTextPreference mTitlePreference;
    private EditTextPreference mTaglinePreference;
    private EditTextPreference mAddressPreference;
    private ListPreference mLanguagePreference;
    private MultiSelectListPreference mCategoryPreference;
    private ListPreference mFormatPreference;
    private ListPreference mVisibilityPreference;
    private MenuItem mSaveItem;
    private HashMap<String, String> mLanguageCodes = new HashMap<>();

    private String mRemoteTitle;
    private String mRemoteTagline;
    private String mRemoteAddress;
    private String mRemoteLanguage;
    private int mRemotePrivacy;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().remove(getString(R.string.pref_key_site_language)).commit();

        addPreferencesFromResource(R.xml.site_settings);

        Integer id = getArguments().getInt(BlogPreferencesActivity.ARG_LOCAL_BLOG_ID, -1);
        mBlog = WordPress.getBlog(id);

        if (mBlog == null) return;

        initPreferences();

        WordPress.getRestClientUtils().getGeneralSettings(
                String.valueOf(mBlog.getRemoteBlogId()), new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (mTitlePreference != null) {
                            mRemoteTitle = response.optString(RestClientUtils.SITE_TITLE_KEY);
                            mTitlePreference.setText(mRemoteTitle);
                            mTitlePreference.setSummary(mRemoteTitle);
                        }

                        if (mTaglinePreference != null) {
                            mRemoteTagline = response.optString(RestClientUtils.SITE_DESC_KEY);
                            mTaglinePreference.setText(mRemoteTagline);
                            mTaglinePreference.setSummary(mRemoteTagline);
                        }

                        if (mAddressPreference != null) {
                            mRemoteAddress = response.optString(RestClientUtils.SITE_URL_KEY);
                            mAddressPreference.setText(mRemoteAddress);
                            mAddressPreference.setSummary(mRemoteAddress);
                        }

                        if (mLanguagePreference != null) {
                            mRemoteLanguage = getLanguageString(response.optString(RestClientUtils.SITE_LANGUAGE_KEY));
                            mLanguagePreference.setDefaultValue(mRemoteLanguage);
                            mLanguagePreference.setSummary(mRemoteLanguage);
                        }

                        if (mVisibilityPreference != null) {
                            mRemotePrivacy = response.optJSONObject("settings").optInt("blog_public");
                            mVisibilityPreference.setValue(String.valueOf(mRemotePrivacy));
                        }
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (isAdded()) {
                            ToastUtils.showToast(getActivity(), "Error getting site info");
                        }
                    }
                });

        setHasOptionsMenu(true);

        String[] languageIds = getResources().getStringArray(R.array.lang_ids);
        String[] languageCodes = getResources().getStringArray(R.array.language_codes);

        for (int i = 0; i < languageIds.length && i < languageCodes.length; ++i) {
            mLanguageCodes.put(languageCodes[i], languageIds[i]);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);

        menuInflater.inflate(R.menu.site_settings, menu);

        if (menu == null || (mSaveItem = menu.findItem(R.id.save_site_settings)) == null) return;

        mSaveItem.setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        applySettings();
                        return true;
                    }
                });
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue == null) return false;

        if (preference == mTitlePreference &&
                !newValue.equals(mTitlePreference.getText())) {
            mTitlePreference.setText(newValue.toString());
            mTitlePreference.setSummary(newValue.toString());
            getActivity().setTitle(newValue.toString());

            return true;
        } else if (preference == mTaglinePreference &&
                !newValue.equals(mTaglinePreference.getText())) {
            mTaglinePreference.setText(newValue.toString());
            mTaglinePreference.setSummary(newValue.toString());

            return true;
        } else if (preference == mLanguagePreference &&
                !newValue.equals(mLanguagePreference.getSummary())) {
            mLanguagePreference.setSummary(getLanguageString(newValue.toString()));

            return true;
        } else if (preference == mVisibilityPreference) {
            switch (Integer.valueOf(newValue.toString())) {
                case -1:
                    mVisibilityPreference.setSummary("I would like my site to be private, visible only to users I choose");
                    break;
                case 0:
                    mVisibilityPreference.setSummary("Discourage search engines from indexing this site");
                    break;
                case 1:
                    mVisibilityPreference.setSummary("Allow search engines to index this site");
                    break;
            }

            return true;
        }

        return false;
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
            params.put("lang_id", String.valueOf(LANGUAGE_CODES.get(mLanguagePreference.getValue())));
        }

        if (mVisibilityPreference != null && Integer.valueOf(mVisibilityPreference.getValue()) != mRemotePrivacy) {
            params.put("blog_public", mVisibilityPreference.getValue());
        }

        return params;
    }

    /**
     * Sends a REST POST request to update site settings
     */
    private void applySettings() {
        final HashMap<String, String> params = generatePostParams();

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

    /**
     * Helper method to set preference references
     */
    private void initPreferences() {
        mTitlePreference =
                (EditTextPreference) findPreference(getString(R.string.pref_key_site_title));
        mTaglinePreference =
                (EditTextPreference) findPreference(getString(R.string.pref_key_site_tagline));
        mLanguagePreference =
                (ListPreference) findPreference(getString(R.string.pref_key_site_language));
        mVisibilityPreference =
                (ListPreference) findPreference(getString(R.string.pref_key_site_visibility));
        mCategoryPreference =
                (MultiSelectListPreference) findPreference(getString(R.string.pref_key_site_category));
        mFormatPreference =
                (ListPreference) findPreference(getString(R.string.pref_key_site_format));
        mAddressPreference =
                (EditTextPreference) findPreference(getString(R.string.pref_key_site_address));

        if (mTitlePreference != null) {
            mTitlePreference.setOnPreferenceChangeListener(this);
        }

        if (mTaglinePreference != null) {
            mTaglinePreference.setOnPreferenceChangeListener(this);
        }

        if (mLanguagePreference != null) {
            mLanguagePreference.setEntries(createLanguageDisplayStrings(mLanguagePreference.getEntryValues()));
            mLanguagePreference.setOnPreferenceChangeListener(this);
        }

        if (mVisibilityPreference != null) {
            mVisibilityPreference.setOnPreferenceChangeListener(this);
        }

        if (mFormatPreference != null) {
            mFormatPreference.setOnPreferenceChangeListener(this);
        }
    }

    private CharSequence[] createLanguageDisplayStrings(CharSequence[] languageCodes) {
        if (languageCodes == null || languageCodes.length < 1) return null;

        CharSequence[] displayStrings = new CharSequence[languageCodes.length];

        for (int i = 0; i < languageCodes.length; ++i) {
            displayStrings[i] = getLanguageString(String.valueOf(languageCodes[i]));
        }

        return displayStrings;
    }

    /**
     * Return a non-null display string for a given language code.
     */
    private String getLanguageString(String languagueCode) {
        if (languagueCode == null || languagueCode.length() < 2) {
            return "";
        } else if (languagueCode.length() == 2) {
            return new Locale(languagueCode).getDisplayLanguage();
        } else {
            return new Locale(languagueCode.substring(0, 2)).getDisplayLanguage() + languagueCode.substring(2);
        }
    }

    private void toggleSaveItemVisibility(boolean on) {
        if (mSaveItem != null) {
            mSaveItem.setVisible(on);
            mSaveItem.setEnabled(on);
        }
    }
}
