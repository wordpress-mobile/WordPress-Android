package org.wordpress.android.ui.publicize;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.models.PublicizeButton;
import org.wordpress.android.ui.prefs.DetailListPreference;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.HashSet;

import javax.inject.Inject;


public class PublicizePrefsFragment extends Fragment
         implements SiteSettingsInterface.SiteSettingsListener {
    private static final String TWITTER_PREFIX = "@";
    private static final String SHARING_BUTTONS_KEY = "sharing_buttons";
    private static final String TWITTER_ID = "twitter";

    private final ArrayList<PublicizeButton> mPublicizeButtons = new ArrayList<>();

    private SiteModel mSite;
    private SiteSettingsInterface mSiteSettings;

    @Inject Dispatcher mDispatcher;

    public static PublicizePrefsFragment newInstance(@NonNull SiteModel site) {
        PublicizePrefsFragment fragment = new PublicizePrefsFragment();
        Bundle args = new Bundle();
        args.putSerializable(WordPress.SITE, site);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (!NetworkUtils.checkConnection(getActivity())) {
            getActivity().finish();
            return;
        }

        if (savedInstanceState == null) {
            mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }

        mSiteSettings = SiteSettingsInterface.getInterface(getActivity(), mSite, this);
        if (mSiteSettings == null) {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found, ToastUtils.Duration.SHORT);
            getActivity().finish();
            return;
        }

        setRetainInstance(true);
        initPreferences();
        setPreferencesFromSiteSettings();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.publicize_prefs_fragment, container, false);
        return view;
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

        toggleTwitterPreferenceVisibility();

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
    }

    // TODO
    private void toggleTwitterPreferenceVisibility() {
        for (int i = 0; i < mPublicizeButtons.size(); i++) {
            PublicizeButton publicizeButton = mPublicizeButtons.get(i);
            if (publicizeButton.getId().equals(TWITTER_ID) && publicizeButton.isEnabled()) {
                //mTwitterPreferenceCategory.setTitle(R.string.twitter);
                //mTwitterPreferenceCategory.removeAll();
                //mTwitterPreferenceCategory.addPreference(mTwitterUsernamePreference);
                return;
            }
        }

        //mTwitterPreferenceCategory.setTitle("");
        //mTwitterPreferenceCategory.removePreference(mTwitterUsernamePreference);
    }

    private void configureSharingAndMoreButtonsPreferences() {
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

        // TODO
        /*mSharingButtonsPreference.setEntries(entries);
        mSharingButtonsPreference.setEntryValues(entryValues);
        mSharingButtonsPreference.setValues(selectedSharingButtons);

        mMoreButtonsPreference.setEntries(entries);
        mMoreButtonsPreference.setEntryValues(entryValues);
        mMoreButtonsPreference.setValues(selectedMoreButtons);*/

        toggleTwitterPreferenceVisibility();
    }

    private void initPreferences() {
        configureSharingAndMoreButtonsPreferences();

        // TODO
    }

    private void setDetailListPreferenceValue(DetailListPreference pref, String value, String summary) {
        pref.setValue(value);
        pref.setSummary(summary);
        pref.refreshAdapter();
    }

    // TODO
    private void setPreferencesFromSiteSettings() {
        /*changeEditTextPreferenceValue(mLabelPreference, mSiteSettings.getSharingLabel());
        setDetailListPreferenceValue(mButtonStylePreference,
                mSiteSettings.getSharingButtonStyle(getActivity()),
                mSiteSettings.getSharingButtonStyleDisplayText(getActivity()));
        mReblogButtonPreference.setChecked(mSiteSettings.getAllowReblogButton());
        mLikeButtonPreference.setChecked(mSiteSettings.getAllowLikeButton());
        mCommentLikesPreference.setChecked(mSiteSettings.getAllowCommentLikes());
        changeEditTextPreferenceValue(mTwitterUsernamePreference, mSiteSettings.getTwitterUsername());*/
    }

    /*@Override
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
    }*/

    @Override
    public void onSettingsUpdated(Exception error) {
        if (!isAdded()) return;

        if (error != null) {
            ToastUtils.showToast(getActivity(), R.string.error_fetch_remote_site_settings);
            getActivity().finish();
        } else {
            setPreferencesFromSiteSettings();
        }
    }
    @Override
    public void onSettingsSaved(Exception error) {
        if (isAdded() && error != null) {
            ToastUtils.showToast(WordPress.getContext(), R.string.error_post_remote_site_settings);
        }
    }
    @Override
    public void onCredentialsValidated(Exception error) {
        if (isAdded() && error != null) {
            ToastUtils.showToast(getActivity(), R.string.username_or_password_incorrect);
        }
    }
}
