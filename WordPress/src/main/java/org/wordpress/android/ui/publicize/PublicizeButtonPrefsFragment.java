package org.wordpress.android.ui.publicize;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
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
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPPrefView;
import org.wordpress.android.widgets.WPPrefView.PrefListItem;
import org.wordpress.android.widgets.WPPrefView.PrefListItems;

import java.util.ArrayList;

import javax.inject.Inject;

public class PublicizeButtonPrefsFragment extends Fragment implements
        SiteSettingsInterface.SiteSettingsListener,
        WPPrefView.OnPrefChangedListener {

    private static final String TWITTER_PREFIX = "@";
    private static final String SHARING_BUTTONS_KEY = "sharing_buttons";
    private static final String SHARING_BUTTONS_UPDATED_KEY = "updated";
    private static final String TWITTER_ID = "twitter";

    private static final long FETCH_DELAY = 1000L;

    private final ArrayList<PublicizeButton> mPublicizeButtons = new ArrayList<>();

    private WPPrefView mPrefSharingButtons;
    private WPPrefView mPrefMoreButtons;
    private WPPrefView mPrefLabel;
    private WPPrefView mPrefButtonStyle;
    private WPPrefView mPrefShowReblog;
    private WPPrefView mPrefShowLike;
    private WPPrefView mPrefAllowCommentLikes;
    private WPPrefView mPrefTwitterName;

    private SiteModel mSite;
    private SiteSettingsInterface mSiteSettings;

    @Inject Dispatcher mDispatcher;

    public static PublicizeButtonPrefsFragment newInstance(@NonNull SiteModel site) {
        PublicizeButtonPrefsFragment fragment = new PublicizeButtonPrefsFragment();
        Bundle args = new Bundle();
        args.putSerializable(WordPress.SITE, site);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        setRetainInstance(true);

        if (!NetworkUtils.checkConnection(getActivity())) {
            getActivity().finish();
            return;
        }

        if (savedInstanceState == null) {
            mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }

        if (mSite == null) {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found, ToastUtils.Duration.SHORT);
            getActivity().finish();
            return;
        }

        // this creates a default site settings interface - the actual settings will
        // be retrieved when getSiteSettings() is called
        mSiteSettings = SiteSettingsInterface.getInterface(getActivity(), mSite, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.publicize_button_prefs_fragment, container, false);

        mPrefButtonStyle = (WPPrefView) view.findViewById(R.id.pref_button_style);
        mPrefSharingButtons = (WPPrefView) view.findViewById(R.id.pref_sharing_buttons);
        mPrefMoreButtons = (WPPrefView) view.findViewById(R.id.pref_more_button);
        mPrefLabel = (WPPrefView) view.findViewById(R.id.pref_label);
        mPrefShowReblog = (WPPrefView) view.findViewById(R.id.pref_show_reblog);
        mPrefShowLike = (WPPrefView) view.findViewById(R.id.pref_show_like);
        mPrefAllowCommentLikes = (WPPrefView) view.findViewById(R.id.pref_allow_comment_likes);
        mPrefTwitterName = (WPPrefView) view.findViewById(R.id.pref_twitter_name);

        return view;
    }

    private void assignPrefListeners(boolean assign) {
        WPPrefView.OnPrefChangedListener listener = assign ? this : null;

        mPrefButtonStyle.setOnPrefChangedListener(listener);
        mPrefSharingButtons.setOnPrefChangedListener(listener);
        mPrefMoreButtons.setOnPrefChangedListener(listener);
        mPrefLabel.setOnPrefChangedListener(listener);
        mPrefShowReblog.setOnPrefChangedListener(listener);
        mPrefShowLike.setOnPrefChangedListener(listener);
        mPrefAllowCommentLikes.setOnPrefChangedListener(listener);
        mPrefTwitterName.setOnPrefChangedListener(listener);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        boolean shouldFetchSettings = (savedInstanceState == null);
        getSiteSettings(shouldFetchSettings);

        configureSharingButtons();
    }

    /**
     * save both the sharing & more buttons
     * @param isSharingButtons true if called by mPrefSharingButtons, false if by mPrefMoreButtons
     */
    private void saveSharingButtons(boolean isSharingButtons) {
        PrefListItems sharingButtons = mPrefSharingButtons.getSelectedItems();
        PrefListItems moreButtons = mPrefMoreButtons.getSelectedItems();

        // sharing and more buttons are mutually exclusive
        if (isSharingButtons) {
             moreButtons.removeItems(sharingButtons);
        } else {
            sharingButtons.removeItems(moreButtons);
        }

        // sharing buttons are visible and enabled, more buttons are invisible and enabled,
        // all others are invisible and disabled
        JSONArray jsonArray = new JSONArray();
        for (PublicizeButton button: mPublicizeButtons) {
            if (sharingButtons.containsValue(button.getId())) {
                button.setVisibility(true);
                button.setEnabled(true);
            } else if (moreButtons.containsValue(button.getId())) {
                button.setVisibility(false);
                button.setEnabled(true);
            } else {
                button.setEnabled(false);
                button.setVisibility(false);
            }

            jsonArray.put(button.toJson());
        }

        toggleTwitterPreference();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(SHARING_BUTTONS_KEY, jsonArray);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.SETTINGS, e);
        }

        WordPress.getRestClientUtilsV1_1().setSharingButtons(Long.toString(mSite.getSiteId()), jsonObject, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                configureSharingButtonsFromResponse(response);
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                AppLog.e(AppLog.T.SETTINGS, error.getMessage());
            }
        });
    }

    /*
     * show the twitter username pref only if there's a twitter sharing button enabled
     */
    private void toggleTwitterPreference() {
        if (!isAdded()) return;

        View twitterCard = getView().findViewById(R.id.card_view_twitter);
        for (int i = 0; i < mPublicizeButtons.size(); i++) {
            PublicizeButton publicizeButton = mPublicizeButtons.get(i);
            if (publicizeButton.getId().equals(TWITTER_ID) && publicizeButton.isEnabled()) {
                twitterCard.setVisibility(View.VISIBLE);
                return;
            }
        }

        twitterCard.setVisibility(View.GONE);
    }

    /*
     * calls the backend to determine which sharing and more buttons are enabled
     */
    private void configureSharingButtons() {
        WordPress.getRestClientUtilsV1_1().getSharingButtons(Long.toString(mSite.getSiteId()), new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                configureSharingButtonsFromResponse(response);
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                AppLog.e(AppLog.T.SETTINGS, error);
            }
        });
    }

    private void configureSharingButtonsFromResponse(JSONObject response) {
        // the array of buttons is in SHARING_BUTTONS_KEY for the GET response,
        // or SHARING_BUTTONS_UPDATED_KEY for the POST response
        JSONArray jsonArray;
        if (response.has(SHARING_BUTTONS_KEY)) {
            jsonArray = response.optJSONArray(SHARING_BUTTONS_KEY);
        } else {
            jsonArray = response.optJSONArray(SHARING_BUTTONS_UPDATED_KEY);
        }
        if (jsonArray == null) {
            AppLog.w(AppLog.T.SETTINGS, "Publicize sharing buttons missing from response");
            return;
        }

        mPublicizeButtons.clear();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject object = jsonArray.optJSONObject(i);
            if (object != null) {
                PublicizeButton publicizeButton = new PublicizeButton(object);
                mPublicizeButtons.add(publicizeButton);
            }
        }

        PrefListItems sharingListItems = new PrefListItems();
        for (PublicizeButton button: mPublicizeButtons) {
            String itemName = button.getName();
            String itemValue = button.getId();
            boolean isChecked = button.isEnabled() && button.isVisible();
            PrefListItem item = new PrefListItem(itemName, itemValue, isChecked);
            sharingListItems.add(item);
        }
        mPrefSharingButtons.setListItems(sharingListItems);

        PrefListItems moreListItems = new PrefListItems();
        for (PublicizeButton button: mPublicizeButtons) {
            String itemName = button.getName();
            String itemValue = button.getId();
            boolean isChecked = button.isEnabled() && !button.isVisible();
            PrefListItem item = new PrefListItem(itemName, itemValue, isChecked);
            moreListItems.add(item);
        }
        mPrefMoreButtons.setListItems(moreListItems);

        toggleTwitterPreference();
    }

    /*
     * retrieves the site settings, first from the local cache and then optionally
     * from the backend - either way this will cause onSettingsUpdated() to be
     * called so the settings will be reflected here
     */
    private void getSiteSettings(boolean shouldFetchSettings) {
        mSiteSettings.init(false);

        if (shouldFetchSettings) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mSiteSettings.init(true);
                }
            }, FETCH_DELAY);
        }
    }

    /*
     * update the preference views from the site settings
     */
    private void setPreferencesFromSiteSettings() {
        assignPrefListeners(false);
        try {
            mPrefLabel.setTextEntry(mSiteSettings.getSharingLabel());
            mPrefButtonStyle.setSummary(mSiteSettings.getSharingButtonStyleDisplayText(getActivity()));

            mPrefShowReblog.setChecked(mSiteSettings.getAllowReblogButton());
            mPrefShowLike.setChecked(mSiteSettings.getAllowLikeButton());
            mPrefAllowCommentLikes.setChecked(mSiteSettings.getAllowCommentLikes());

            mPrefTwitterName.setTextEntry(mSiteSettings.getTwitterUsername());

            // configure the button style pref
            String selectedName = mSiteSettings.getSharingButtonStyleDisplayText(getActivity());
            String[] names = getResources().getStringArray(R.array.sharing_button_style_display_array);
            String[] values = getResources().getStringArray(R.array.sharing_button_style_array);
            PrefListItems listItems = new PrefListItems();
            for (int i = 0; i < names.length; i++) {
                PrefListItem item = new PrefListItem(names[i], values[i], false);
                listItems.add(item);
            }
            listItems.setSelectedName(selectedName);
            mPrefButtonStyle.setListItems(listItems);
            mPrefButtonStyle.setSummary(selectedName);
        } finally {
            assignPrefListeners(true);
        }
    }

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
        // noop
    }

    @Override
    public void onPrefChanged(@NonNull WPPrefView pref) {
        if (pref == mPrefSharingButtons) {
            saveSharingButtons(true);
        } else if (pref == mPrefMoreButtons) {
            saveSharingButtons(false);
        } else if (pref == mPrefButtonStyle) {
            PrefListItem item = pref.getSelectedItem();
            if (item != null) {
                mSiteSettings.setSharingButtonStyle(item.getItemValue());
            }
        } else if (pref == mPrefLabel) {
            mSiteSettings.setSharingLabel(pref.getTextEntry());
        } else if (pref == mPrefShowReblog) {
            mSiteSettings.setAllowReblogButton(pref.isChecked());
        } else if (pref == mPrefShowLike) {
            mSiteSettings.setAllowLikeButton(pref.isChecked());
        } else if (pref == mPrefAllowCommentLikes) {
            mSiteSettings.setAllowCommentLikes(pref.isChecked());
        } else if (pref == mPrefTwitterName) {
            String username = StringUtils.notNullStr(pref.getTextEntry());
            if (username.startsWith(TWITTER_PREFIX)) {
                username = username.substring(1, username.length());
            }
            mSiteSettings.setTwitterUsername(username);
        }

        mSiteSettings.saveSettings();
    }
}
