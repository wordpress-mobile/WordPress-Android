package org.wordpress.android.ui.prefs;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;

public class SiteSettingsFragment extends PreferenceFragment {
    private Blog mBlog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.site_settings);

        Integer id = getArguments().getInt(BlogPreferencesActivity.ARG_LOCAL_BLOG_ID, -1);
        mBlog = WordPress.getBlog(id);

        if (mBlog != null) {
            final EditTextPreference titlePreference =
                    (EditTextPreference) findPreference(getString(R.string.pref_key_site_title));
            final EditTextPreference taglinePreference =
                    (EditTextPreference) findPreference(getString(R.string.pref_key_site_tagline));
            final MultiSelectListPreference languagePreference =
                    (MultiSelectListPreference) findPreference(getString(R.string.pref_key_site_language));
            final SeekBarPreference visibilityPreference =
                    (SeekBarPreference) findPreference(getString(R.string.pref_key_site_visibility));

            WordPress.getRestClientUtils().getGeneralSettings(
                    String.valueOf(mBlog.getRemoteBlogId()), new RestRequest.Listener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            if (titlePreference != null) {
                                titlePreference.setText(response.optString("name"));
                                titlePreference.setSummary(response.optString("name"));
                            }

                            if (taglinePreference != null) {
                                taglinePreference.setText(response.optString("description"));
                                taglinePreference.setSummary(response.optString("description"));
                            }

                            if (languagePreference != null) {
                                languagePreference.setDefaultValue(response.optString("lang"));
                                languagePreference.setSummary(response.optString("lang"));
                            }

                            if (visibilityPreference != null) {
                            }
                        }
                    }, new RestRequest.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (taglinePreference != null) {
                                taglinePreference.setEnabled(false);
                                taglinePreference.setSummary("Failed to retrieve :(");
                            }
                        }
                    });

            if (titlePreference != null) {
                titlePreference.setText(mBlog.getBlogName());
                titlePreference.setSummary(mBlog.getBlogName());
                titlePreference.setOnPreferenceChangeListener(mTitleChangeListener);
            }

            if (taglinePreference != null) {
                taglinePreference.setOnPreferenceChangeListener(mTaglineChangeListener);
            }

            if (languagePreference != null) {
                languagePreference.setOnPreferenceChangeListener(mLanguageChangeListener);
            }

            if (visibilityPreference != null) {
                visibilityPreference.setOnPreferenceChangeListener(mVisibilityChangeListener);
            }
        }
    }

    private final Preference.OnPreferenceChangeListener mTitleChangeListener =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue == null || newValue.equals("")) return false;

                    ((EditTextPreference) preference).setText(newValue.toString());
                    preference.setSummary(newValue.toString());
                    mBlog.setBlogName(newValue.toString());
                    return true;
                }
            };

    private final Preference.OnPreferenceChangeListener mTaglineChangeListener =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    ((EditTextPreference) preference).setText(newValue.toString());
                    preference.setSummary(newValue.toString());
                    return true;
                }
            };

    private final Preference.OnPreferenceChangeListener mLanguageChangeListener =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(newValue.toString());
                    return true;
                }
            };

    private final Preference.OnPreferenceChangeListener mVisibilityChangeListener =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // TODO: set Visibility
                    return true;
                }
            };
}
