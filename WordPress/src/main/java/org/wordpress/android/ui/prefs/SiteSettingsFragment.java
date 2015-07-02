package org.wordpress.android.ui.prefs;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.MultiSelectListPreference;
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
import org.wordpress.android.util.ToastUtils;

import java.util.HashMap;

public class SiteSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener{
    private Blog mBlog;
    private EditTextPreference mTitlePreference;
    private EditTextPreference mTaglinePreference;
    private MultiSelectListPreference mLanguagePreference;
    private SeekBarPreference mVisibilityPreference;
    private MenuItem mSaveItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

                        if (mLanguagePreference != null) {
                            mLanguagePreference.setDefaultValue(response.optString(RestClientUtils.SITE_LANGUAGE_KEY));
                            mLanguagePreference.setSummary(response.optString(RestClientUtils.SITE_LANGUAGE_KEY));
                        }

                        if (mVisibilityPreference != null) {
                            int visibility = response.optJSONObject("settings").optInt("blog_public");

                            switch (visibility) {
                                case -1:
                                    mVisibilityPreference.setProgress(0);
                                    break;
                                case 0:
                                    mVisibilityPreference.setProgress(50);
                                    break;
                                case 1:
                                    mVisibilityPreference.setProgress(100);
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

        if (preference == mTitlePreference) {
            if (newValue.equals(mTitlePreference.getText())) return false;

            mTitlePreference.setText(newValue.toString());
            mTitlePreference.setSummary(newValue.toString());
            toggleSaveItemVisibility(true);

            return true;
        } else if (preference == mTaglinePreference) {
            if (newValue.equals(mTaglinePreference.getText())) return false;

            mTaglinePreference.setText(newValue.toString());
            mTaglinePreference.setSummary(newValue.toString());
            toggleSaveItemVisibility(true);

            return true;
        } else if (preference == mLanguagePreference) {
            if (newValue.equals(mLanguagePreference.getSummary())) return false;

            mLanguagePreference.setSummary(newValue.toString());
            toggleSaveItemVisibility(true);

            return true;
        } else if (preference == mVisibilityPreference) {
            int progress = convertProgressToVisibility(mVisibilityPreference.getProgress());

            if (newValue.equals(progress)) return false;

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
//            params.put("lang", mLanguagePreference.getSummary().toString());
        }

        if (mVisibilityPreference != null) {
            params.put("blog_public", String.valueOf(
                    convertProgressToVisibility(mVisibilityPreference.getProgress())));
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
                (MultiSelectListPreference) findPreference(getString(R.string.pref_key_site_language));
        mVisibilityPreference =
                (SeekBarPreference) findPreference(getString(R.string.pref_key_site_visibility));

        if (mTitlePreference != null) {
            mTitlePreference.setOnPreferenceChangeListener(this);
        }

        if (mTaglinePreference != null) {
            mTaglinePreference.setOnPreferenceChangeListener(this);
        }

        if (mLanguagePreference != null) {
            mLanguagePreference.setOnPreferenceChangeListener(this);
        }

        if (mVisibilityPreference != null) {
            mVisibilityPreference.setOnPreferenceChangeListener(this);
        }
    }

    private void toggleSaveItemVisibility(boolean on) {
        if (mSaveItem != null) {
            mSaveItem.setVisible(on);
            mSaveItem.setEnabled(on);
        }
    }

    private int convertProgressToVisibility(int progress) {
        if (progress < 33) return -1;
        else if (progress < 67) return 0;
        else return 1;
    }
}
