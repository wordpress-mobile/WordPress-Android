package org.wordpress.android.ui.publicize;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.support.annotation.NonNull;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.models.PublicizeButton;
import org.wordpress.android.ui.prefs.DetailListPreference;
import org.wordpress.android.ui.prefs.SiteSettingsFragment;
import org.wordpress.android.ui.prefs.SummaryEditTextPreference;
import org.wordpress.android.ui.prefs.WPSwitchPreference;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.HashSet;


public class PublicizeManageConnectionsFragment extends SiteSettingsFragment {
    private static final String TWITTER_PREFIX = "@";
    private static final String SHARING_BUTTONS_KEY = "sharing_buttons";
    private static final String TWITTER_ID = "twitter";

    private MultiSelectListPreference mSharingButtonsPreference;
    private MultiSelectListPreference mMoreButtonsPreference;
    private SummaryEditTextPreference mLabelPreference;
    private DetailListPreference mButtonStylePreference;
    private WPSwitchPreference mReblogButtonPreference;
    private WPSwitchPreference mLikeButtonPreference;
    private WPSwitchPreference mCommentLikesPreference;
    private SummaryEditTextPreference mTwitterUsernamePreference;
    private PreferenceCategory mTwitterPreferenceCategory;
    private ArrayList<PublicizeButton> mPublicizeButtons;

    public static PublicizeManageConnectionsFragment newInstance(@NonNull SiteModel site) {
        PublicizeManageConnectionsFragment fragment = new PublicizeManageConnectionsFragment();
        Bundle args = new Bundle();
        args.putSerializable(WordPress.SITE, site);
        fragment.setArguments(args);
        return fragment;
    }

    private void saveSharingButtons(HashSet<String> values, boolean isVisible) {
        JSONArray jsonArray = new JSONArray();
        for (PublicizeButton button: mPublicizeButtons) {
            if (values.contains(button.getId())) {
                button.setVisibility(isVisible);
                button.setEnabled(true);
            } else {
                button.setEnabled(false);
                button.setVisibility(false);
            }

            jsonArray.put(button.toJson());
        }

        toggleTwitterPreferenceVisiblity();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(SHARING_BUTTONS_KEY, jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        WordPress.getRestClientUtilsV1_1().setSharingButtons(Long.toString(mSite.getSiteId()), jsonObject, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    configureSharingButtons(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                AppLog.e(AppLog.T.SETTINGS, error.getMessage());
            }
        });
    }

    private void saveAndSetTwitterUsername(String username) {
        if (username == null) {
            return;
        }

        if (username.startsWith(TWITTER_PREFIX)) {
            username = username.substring(1, username.length());
        }

        mSiteSettings.setTwitterUsername(username);
        changeEditTextPreferenceValue(mTwitterUsernamePreference, username);
    }

    private void toggleTwitterPreferenceVisiblity() {
        for (int i = 0; i < mPublicizeButtons.size(); i++) {
            PublicizeButton publicizeButton = mPublicizeButtons.get(i);
            if (publicizeButton.getId().equals(TWITTER_ID) && publicizeButton.isEnabled()) {
                mTwitterPreferenceCategory.setTitle(R.string.twitter);
                mTwitterPreferenceCategory.removeAll();
                mTwitterPreferenceCategory.addPreference(mTwitterUsernamePreference);
                return;
            }
        }

        mTwitterPreferenceCategory.setTitle("");
        mTwitterPreferenceCategory.removePreference(mTwitterUsernamePreference);
    }

    private void configureSharingAndMoreButtonsPreferences() {
        mPublicizeButtons = new ArrayList<>();
        mSharingButtonsPreference = (MultiSelectListPreference) findPreference(getString(R.string.pref_key_sharing_buttons));
        mMoreButtonsPreference = (MultiSelectListPreference) findPreference(getString(R.string.pref_key_more_buttons));
        WordPress.getRestClientUtilsV1_1().getSharingButtons(Long.toString(mSite.getSiteId()), new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    configureSharingButtons(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                AppLog.e(AppLog.T.SETTINGS, error);
            }
        });
    }

    private void configureSharingButtons(JSONObject response) throws JSONException {
        JSONArray jsonArray = response.getJSONArray(SHARING_BUTTONS_KEY);
        for (int i = 0; i < jsonArray.length(); i++) {
            PublicizeButton publicizeButton = new PublicizeButton(jsonArray.getJSONObject(i));
            mPublicizeButtons.add(publicizeButton);
        }

        String[] entries = new String[mPublicizeButtons.size()];
        String[] entryValues = new String[mPublicizeButtons.size()];
        HashSet<String> selectedSharingButtons = new HashSet<>();
        HashSet<String> selectedMoreButtons = new HashSet<>();

        for (int i = 0; i < mPublicizeButtons.size(); i++) {
            PublicizeButton publicizeButton = mPublicizeButtons.get(i);
            entries[i] = publicizeButton.getName();
            entryValues[i] = publicizeButton.getId();
            if (publicizeButton.isEnabled() && publicizeButton.isVisible()) {
                selectedSharingButtons.add(publicizeButton.getId());
            }
            if (publicizeButton.isEnabled() && !publicizeButton.isVisible()) {
                selectedMoreButtons.add(publicizeButton.getId());
            }
        }

        mSharingButtonsPreference.setEntries(entries);
        mSharingButtonsPreference.setEntryValues(entryValues);
        mSharingButtonsPreference.setValues(selectedSharingButtons);

        mMoreButtonsPreference.setEntries(entries);
        mMoreButtonsPreference.setEntryValues(entryValues);
        mMoreButtonsPreference.setValues(selectedMoreButtons);

        toggleTwitterPreferenceVisiblity();
    }

    @Override
    public void initPreferences() {
        configureSharingAndMoreButtonsPreferences();
        mLabelPreference = (SummaryEditTextPreference) findPreference(getString(R.string.publicize_label));
        mButtonStylePreference = (DetailListPreference) findPreference(getString(R.string.publicize_button_style));
        setDetailListPreferenceValue(mButtonStylePreference, mSiteSettings.getSharingButtonStyle(getActivity()), mSiteSettings.getSharingButtonStyleDisplayText(getActivity()));
        mButtonStylePreference.setEntries(getResources().getStringArray(R.array.sharing_button_style_display_array));
        mButtonStylePreference.setEntryValues(getResources().getStringArray(R.array.sharing_button_style_array));
        mReblogButtonPreference = (WPSwitchPreference) findPreference(getString(R.string.pref_key_reblog));
        mLikeButtonPreference = (WPSwitchPreference) findPreference(getString(R.string.pref_key_like));
        mCommentLikesPreference = (WPSwitchPreference) findPreference(getString(R.string.pref_key_comment_likes));
        mTwitterUsernamePreference = (SummaryEditTextPreference) findPreference(getString(R.string.pref_key_twitter_username));
        mTwitterPreferenceCategory = (PreferenceCategory) findPreference(getString(R.string.pref_key_twitter_category));
    }

    @Override
    public void setEditingEnabled(boolean enabled) {
        final Preference[] editablePreference = {
                mSharingButtonsPreference, mMoreButtonsPreference, mLabelPreference, mButtonStylePreference,
                mReblogButtonPreference, mLikeButtonPreference, mCommentLikesPreference,
                mTwitterUsernamePreference
        };

        for(Preference preference : editablePreference) {
            if(preference != null) preference.setEnabled(enabled);
        }

        mEditingEnabled = enabled;
    }

    @Override
    public void setPreferencesFromSiteSettings() {
        changeEditTextPreferenceValue(mLabelPreference, mSiteSettings.getSharingLabel());
        setDetailListPreferenceValue(mButtonStylePreference, mSiteSettings.getSharingButtonStyle(getActivity()), mSiteSettings.getSharingButtonStyleDisplayText(getActivity()));
        mReblogButtonPreference.setChecked(mSiteSettings.getAllowReblogButton());
        mLikeButtonPreference.setChecked(mSiteSettings.getAllowLikeButton());
        mCommentLikesPreference.setChecked(mSiteSettings.getAllowCommentLikes());
        changeEditTextPreferenceValue(mTwitterUsernamePreference, mSiteSettings.getTwitterUsername());
    }

    @Override
    public void addPreferencesFromResource() {
        addPreferencesFromResource(R.xml.publicize_preferences);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mLabelPreference) {
            mSiteSettings.setSharingLabel(newValue.toString());
            changeEditTextPreferenceValue(mLabelPreference, newValue.toString());
        } else if (preference == mButtonStylePreference) {
            mSiteSettings.setSharingButtonStyle(newValue.toString());
            setDetailListPreferenceValue(mButtonStylePreference,
                    mSiteSettings.getSharingButtonStyle(getActivity()),
                    mSiteSettings.getSharingButtonStyleDisplayText(getActivity()));
        } else if (preference == mReblogButtonPreference) {
            mSiteSettings.setAllowReblogButton((Boolean) newValue);
        } else if (preference == mLikeButtonPreference) {
            mSiteSettings.setAllowLikeButton((Boolean) newValue);
        } else if (preference == mCommentLikesPreference) {
            mSiteSettings.setAllowCommentLikes((Boolean) newValue);
        } else if (preference == mTwitterUsernamePreference) {
            saveAndSetTwitterUsername(newValue.toString());
        } else if (preference == mSharingButtonsPreference) {
            saveSharingButtons((HashSet<String>) newValue, true);
        } else if (preference == mMoreButtonsPreference) {
            saveSharingButtons((HashSet<String>) newValue, false);
        } else {
            return false;
        }

        mSiteSettings.saveSettings();

        return true;
    }

    @Override
    public void changeEditTextPreferenceValue(EditTextPreference pref, String newValue) {
        if (pref != null && pref == mTwitterUsernamePreference && newValue != null && !newValue.isEmpty()) {
            newValue = TWITTER_PREFIX + newValue;
        }

        super.changeEditTextPreferenceValue(pref, newValue);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return preference == mButtonStylePreference && !shouldShowListPreference((DetailListPreference) preference);
    }
}
