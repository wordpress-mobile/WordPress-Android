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
        implements Preference.OnPreferenceChangeListener{
    private Blog mBlog;
    private EditTextPreference mTitlePreference;
    private EditTextPreference mTaglinePreference;
    private EditTextPreference mAddressPreference;
    private ListPreference mLanguagePreference;
    private MultiSelectListPreference mCategoryPreference;
    private ListPreference mFormatPreference;
    private SeekBarPreference mVisibilityPreference;
    private MenuItem mSaveItem;

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
                            mTitlePreference.setText(response.optString(RestClientUtils.SITE_TITLE_KEY));
                            mTitlePreference.setSummary(response.optString(RestClientUtils.SITE_TITLE_KEY));
                        }

                        if (mTaglinePreference != null) {
                            mTaglinePreference.setText(response.optString(RestClientUtils.SITE_DESC_KEY));
                            mTaglinePreference.setSummary(response.optString(RestClientUtils.SITE_DESC_KEY));
                        }

                        if (mAddressPreference != null) {
                            mAddressPreference.setText(response.optString(RestClientUtils.SITE_URL_KEY));
                            mAddressPreference.setSummary(response.optString(RestClientUtils.SITE_URL_KEY));
                            // Disabled until implemented
                            mAddressPreference.setEnabled(false);
                        }

                        if (mLanguagePreference != null) {
                            String languageString = getLanguageString(response.optString(RestClientUtils.SITE_LANGUAGE_KEY));
                            mLanguagePreference.setDefaultValue(languageString);
                            mLanguagePreference.setSummary(languageString);
                        }

                        if (mVisibilityPreference != null) {
                            int visibility = response.optJSONObject("settings").optInt("blog_public");

                            switch (visibility) {
                                case -1:
                                    mVisibilityPreference.setProgress(0);
                                    break;
                                case 0:
                                    mVisibilityPreference.setProgress(1);
                                    break;
                                case 1:
                                    mVisibilityPreference.setProgress(2);
                                    break;
                                default:
                                    mVisibilityPreference.setSummary("Unknown privacy setting");
                                    mVisibilityPreference.setEnabled(false);
                                    break;
                            }
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
            toggleSaveItemVisibility(true);

            return true;
        } else if (preference == mTaglinePreference &&
                !newValue.equals(mTaglinePreference.getText())) {
            mTaglinePreference.setText(newValue.toString());
            mTaglinePreference.setSummary(newValue.toString());
            toggleSaveItemVisibility(true);

            return true;
        } else if (preference == mLanguagePreference &&
                !newValue.equals(mLanguagePreference.getSummary())) {
            mLanguagePreference.setSummary(getLanguageString(newValue.toString()));
            toggleSaveItemVisibility(true);

            return true;
        } else if (preference == mVisibilityPreference &&
                !newValue.equals(mVisibilityPreference.getProgress() - 1)) {
            switch ((Integer)newValue) {
                case 0:
                    mVisibilityPreference.setSummary("I would like my site to be private, visible only to users I choose");
                    break;
                case 1:
                    mVisibilityPreference.setSummary("Discourage search engines from indexing this site");
                    break;
                case 2:
                    mVisibilityPreference.setSummary("Allow search engines to index this site");
                    break;
            }

            toggleSaveItemVisibility(true);

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
        if (mTitlePreference != null) {
            params.put("blogname", mTitlePreference.getText());
        }

        if (mTaglinePreference != null) {
            params.put("blogdescription", mTaglinePreference.getText());
        }

        if (mLanguagePreference != null) {
            params.put("lang_id", mLanguagePreference.getValue());
//            params.put("lang", mLanguagePreference.getSummary().toString());
        }

        if (mVisibilityPreference != null) {
            params.put("blog_public", String.valueOf(mVisibilityPreference.getProgress() - 1));
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
                toggleSaveItemVisibility(false);

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
                (SeekBarPreference) findPreference(getString(R.string.pref_key_site_visibility));
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
