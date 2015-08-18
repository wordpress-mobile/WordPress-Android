package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.TypefaceCache;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Handles changes to WordPress site settings. Syncs with host automatically when user leaves.
 */

public class SiteSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener, AdapterView.OnItemLongClickListener {
    public interface HasHint {
        boolean hasHint();
        String getHintText();
    }

    private HashMap<String, String> mLanguageCodes = new HashMap<>();
    private Blog mBlog;
    private EditTextPreference mTitlePreference;
    private EditTextPreference mTaglinePreference;
    private EditTextPreference mAddressPreference;
    private DetailListPreference mLanguagePreference;
    private DetailListPreference mPrivacyPreference;

    // Most recent remote site data. Current local data is used if remote data cannot be fetched.
    private String mRemoteTitle;
    private String mRemoteTagline;
    private String mRemoteAddress;
    private int mRemotePrivacy;
    private String mRemoteLanguage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

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
    public void onDestroy() {
        super.onDestroy();

        // Assume user wanted changes propagated when they leave
        applyChanges();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.Calypso_SiteSettingsTheme);
        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
        View view = super.onCreateView(localInflater, container, savedInstanceState);

        // Setup the preferences to handled long clicks
        if (view != null) {
            ListView prefList = (ListView) view.findViewById(android.R.id.list);
            Resources res = getResources();

            if (prefList != null && res != null) {
                prefList.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                    @Override
                    public void onChildViewAdded(View parent, View child) {
                        if (child.getId() == android.R.id.title && child instanceof TextView) {
                            Resources res = getResources();
                            TextView title = (TextView) child;
                            title.setTypeface(TypefaceCache.getTypeface(getActivity(),
                                    TypefaceCache.FAMILY_OPEN_SANS,
                                    Typeface.BOLD,
                                    TypefaceCache.VARIATION_LIGHT));
                            title.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                                    res.getDimensionPixelSize(R.dimen.text_sz_medium));
                            title.setTextColor(res.getColor(R.color.orange_jazzy));
                        }
                    }

                    @Override
                    public void onChildViewRemoved(View parent, View child) {
                    }
                });
                prefList.setOnItemLongClickListener(this);
                prefList.setFooterDividersEnabled(false);
                prefList.setOverscrollFooter(res.getDrawable(R.color.transparent));
            }
        }

        return view;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue == null) return false;

        if (preference == mTitlePreference) {
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
            changePrivacyValue(Integer.valueOf(newValue.toString()));
            return true;
        }

        return false;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        ListView listView = (ListView) parent;
        ListAdapter listAdapter = listView.getAdapter();
        Object obj = listAdapter.getItem(position);

        if (obj != null) {
            if (obj instanceof View.OnLongClickListener) {
                View.OnLongClickListener longListener = (View.OnLongClickListener) obj;
                return longListener.onLongClick(view);
            } else if (obj instanceof HasHint && ((HasHint) obj).hasHint()) {
                ToastUtils.showToast(getActivity(), ((HasHint)obj).getHintText(), ToastUtils.Duration.SHORT);
                return true;
            }
        }

        return false;
    }

    /**
     * Helper method to retrieve {@link Preference} references and initialize any data.
     */
    private void initPreferences() {
        // Title preference
        if (null != (mTitlePreference =
                (EditTextPreference) findPreference(getString(R.string.pref_key_site_title)))) {
            mTitlePreference.setOnPreferenceChangeListener(this);
        }

        // Tagline preference
        if (null != (mTaglinePreference =
                (EditTextPreference) findPreference(getString(R.string.pref_key_site_tagline)))) {
            mTaglinePreference.setOnPreferenceChangeListener(this);
        }

        // Address preferences
        if (null != (mAddressPreference =
                (EditTextPreference) findPreference(getString(R.string.pref_key_site_address)))) {
            mAddressPreference.setOnPreferenceChangeListener(this);
        }

        // Privacy preference, removed for self-hosted sites
        if (null != (mPrivacyPreference =
                (DetailListPreference) findPreference(getString(R.string.pref_key_site_visibility)))) {
            if (!mBlog.isDotcomFlag()) {
                removePreference(R.string.pref_key_site_general, mPrivacyPreference);
            } else {
                mPrivacyPreference.setOnPreferenceChangeListener(this);
            }
        }

        // Language preference, removed for self-hosted sites
        if (null != (mLanguagePreference =
                (DetailListPreference) findPreference(getString(R.string.pref_key_site_language)))) {
            if (!mBlog.isDotcomFlag()) {
                removePreference(R.string.pref_key_site_general, mLanguagePreference);
            } else {
                mLanguagePreference.setOnPreferenceChangeListener(this);

                // Generate map of language codes
                String[] languageIds = getResources().getStringArray(R.array.lang_ids);
                String[] languageCodes = getResources().getStringArray(R.array.language_codes);
                for (int i = 0; i < languageIds.length && i < languageCodes.length; ++i) {
                    mLanguageCodes.put(languageCodes[i], languageIds[i]);
                }
            }
        }
    }

    /**
     * Request remote site data via the WordPress REST API.
     */
    private void fetchRemoteData() {
        if (mBlog.isDotcomFlag()) {
            WordPress.getRestClientUtils().getGeneralSettings(
                    String.valueOf(mBlog.getRemoteBlogId()), new RestRequest.Listener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            if (isAdded()) {
                                handleResponseToWPComSettingsRequest(response);
                            }
                        }
                    }, new RestRequest.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            handleSettingsFetchError(error.toString());
                        }
                    });
        } else {
            // self-hosted settings
            Object[] params = {
                    mBlog.getRemoteBlogId(), mBlog.getUsername(), mBlog.getPassword()
            };

            instantiateInterface().callAsync(mXmlRpcFetchCallback, ApiHelper.Methods.GET_OPTIONS, params);
        }
    }

    private XMLRPCClientInterface instantiateInterface() {
        return XMLRPCFactory.instantiate(mBlog.getUri(),  mBlog.getHttpuser(),  mBlog.getHttppassword());
    }

    private void handleSettingsFetchError(String error) {
        AppLog.w(AppLog.T.API, "Error GETing site settings: " + error);
        if (isAdded()) {
            ToastUtils.showToast(getActivity(), getString(R.string.error_fetch_remote_site_settings));
            getActivity().finish();
        }
    }

    private void handleResponseToSelfHostedSettingsRequest(Map result) {
        if (mTitlePreference != null) {
            mTitlePreference.setEnabled(true);
            mRemoteTitle = getNestedMapValue(result, "blog_title");
            changeEditTextPreferenceValue(mTitlePreference, mRemoteTitle);
        }

        if (mTaglinePreference != null) {
            mTaglinePreference.setEnabled(true);
            mRemoteTagline = getNestedMapValue(result, "blog_tagline");
            changeEditTextPreferenceValue(mTaglinePreference, mRemoteTagline);
        }

        if (mAddressPreference != null) {
            mRemoteAddress = getNestedMapValue(result, "blog_url");
            changeEditTextPreferenceValue(mAddressPreference, mRemoteAddress);
        }
    }

    /**
     * Helper method to parse JSON response to REST request.
     */
    private void handleResponseToWPComSettingsRequest(JSONObject response) {
        if (mTitlePreference != null) {
            mTitlePreference.setEnabled(true);
            mRemoteTitle = response.optString(RestClientUtils.SITE_TITLE_KEY);
            changeEditTextPreferenceValue(mTitlePreference, mRemoteTitle);
        }

        if (mTaglinePreference != null) {
            mTaglinePreference.setEnabled(true);
            mRemoteTagline = response.optString(RestClientUtils.SITE_DESC_KEY);
            changeEditTextPreferenceValue(mTaglinePreference, mRemoteTagline);
        }

        mRemoteAddress = response.optString(RestClientUtils.SITE_URL_KEY);
        changeEditTextPreferenceValue(mAddressPreference, mRemoteAddress);

        JSONObject settingsObject = response.optJSONObject("settings");

        if (settingsObject != null) {
            mRemoteLanguage = convertLanguageIdToLanguageCode(settingsObject.optString("lang_id"));
            if (mLanguagePreference != null) {
                String[] languageCodes = getResources().getStringArray(R.array.language_codes);
                mLanguagePreference.setEntries(createLanguageDisplayStrings(languageCodes));
                mLanguagePreference.setDetails(
                        createLanguageDetailDisplayStrings(languageCodes, mRemoteLanguage));
                mLanguagePreference.setEnabled(true);
                changeLanguageValue(mRemoteLanguage);
                mLanguagePreference.setSummary(
                        firstLetterCapitalized(
                                getLanguageString(mRemoteLanguage, new Locale(localeInput(mRemoteLanguage)))));
            }

            mRemotePrivacy = settingsObject.optInt("blog_public");
            if (mPrivacyPreference != null) {
                mPrivacyPreference.setEnabled(true);
                mPrivacyPreference.setValue(String.valueOf(mRemotePrivacy));
                mPrivacyPreference.setSummary(privacyStringForValue(mRemotePrivacy));
            }
        }
    }

    /**
     * Helper method to create the parameters for the site settings POST request
     *
     * Using undocumented endpoint WPCOM_JSON_API_Site_Settings_Endpoint
     * https://wpcom.trac.automattic.com/browser/trunk/public.api/rest/json-endpoints.php#L1903
     */
    private HashMap<String, String> generateDotComPostParams() {
        HashMap<String, String> params = new HashMap<>();

        if (mTitlePreference != null && !mTitlePreference.getText().equals(mRemoteTitle)) {
            params.put("blogname", mTitlePreference.getText());
        }

        if (mTaglinePreference != null && !mTaglinePreference.getText().equals(mRemoteTagline)) {
            params.put("blogdescription", mTaglinePreference.getText());
        }

        if (mAddressPreference != null && !mAddressPreference.getText().equals(mRemoteAddress)) {
        }

        if (mLanguagePreference != null && mLanguageCodes != null && mRemoteLanguage != null &&
                mLanguageCodes.containsKey(mLanguagePreference.getValue()) &&
                !mRemoteLanguage.equals(mLanguageCodes.get(mLanguagePreference.getValue()))) {
            params.put("lang_id", String.valueOf(mLanguageCodes.get(mLanguagePreference.getValue())));
        }

        if (mPrivacyPreference != null &&
                Integer.valueOf(mPrivacyPreference.getValue()) != mRemotePrivacy) {
            params.put("blog_public", mPrivacyPreference.getValue());
        }

        return params;
    }

    private HashMap<String, String> generateSelfHostedParams() {
        HashMap<String, String> params = new HashMap<>();

        if (mTitlePreference != null && !mTitlePreference.getText().equals(mRemoteTitle)) {
            params.put("blog_title", mTitlePreference.getText());
        }

        if (mTaglinePreference != null && !mTaglinePreference.getText().equals(mRemoteTagline)) {
            params.put("blog_tagline", mTaglinePreference.getText());
        }

        return params;
    }

    /**
     * Persists changed settings remotely
     */
    private void applyChanges() {
        if (mBlog.isDotcomFlag()) {
            postDotComChanges(generateDotComPostParams());
        } else {
            postSelfHostedChanges(generateSelfHostedParams());
        }
    }

    private void postSelfHostedChanges(final HashMap<String, String> params) {
        if (params == null || params.size() == 0) return;

        Object[] callParams = {
                mBlog.getRemoteBlogId(), mBlog.getUsername(), mBlog.getPassword(), params
        };

        instantiateInterface().callAsync(mXmlRpcSetCallback, ApiHelper.Methods.SET_OPTIONS, callParams);
    }

    private void postDotComChanges(final HashMap<String, String> params) {
        if (params == null || params.size() == 0) return;

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
            mLanguagePreference.setSummary(getLanguageString(newValue, new Locale(localeInput(newValue))));

            // update details to display in selected locale
            String[] languageCodes = getResources().getStringArray(R.array.language_codes);
            mLanguagePreference.setEntries(createLanguageDisplayStrings(languageCodes));
            mLanguagePreference.setDetails(createLanguageDetailDisplayStrings(languageCodes, newValue));
            mLanguagePreference.refreshAdapter();
        }
    }

    private void changePrivacyValue(int newValue) {
        if (mPrivacyPreference != null && Integer.valueOf(mPrivacyPreference.getValue()) != newValue) {
            mPrivacyPreference.setValue(String.valueOf(newValue));
            mPrivacyPreference.setSummary(privacyStringForValue(newValue));
            mPrivacyPreference.refreshAdapter();
        }
    }

    /**
     * Returns non-null String representation of WordPress.com privacy value.
     */
    private String privacyStringForValue(int value) {
        switch (value) {
            case -1:
                return getString(R.string.privacy_private);
            case 0:
                return getString(R.string.privacy_hidden);
            case 1:
                return getString(R.string.privacy_public);
            default:
                return "";
        }
    }

    /**
     * Generates display strings for given language codes. Used as entries in language preference.
     */
    private String[] createLanguageDisplayStrings(CharSequence[] languageCodes) {
        if (languageCodes == null || languageCodes.length < 1) return null;

        String[] displayStrings = new String[languageCodes.length];

        for (int i = 0; i < languageCodes.length; ++i) {
            displayStrings[i] = firstLetterCapitalized(getLanguageString(
                    String.valueOf(languageCodes[i]), new Locale(localeInput(languageCodes[i].toString()))));
        }

        return displayStrings;
    }

    /**
     * Generates detail display strings in the currently selected locale. Used as detail text
     * in language preference dialog.
     */
    public String[] createLanguageDetailDisplayStrings(String[] languageCodes, String locale) {
        if (languageCodes == null || languageCodes.length < 1) return null;

        String[] detailStrings = new String[languageCodes.length];
        for (int i = 0; i < languageCodes.length; ++i) {
            detailStrings[i] = firstLetterCapitalized(
                    getLanguageString(languageCodes[i], new Locale(localeInput(locale))));
        }

        return detailStrings;
    }

    /**
     * Return a non-null display string for a given language code.
     */
    private String getLanguageString(String languageCode, Locale displayLocale) {
        if (languageCode == null || languageCode.length() < 2 || languageCode.length() > 6) {
            return "";
        }

        Locale languageLocale = new Locale(localeInput(languageCode));
        return languageLocale.getDisplayLanguage(displayLocale) + languageCode.substring(2);
    }

    /**
     * Removes a {@link Preference} from the {@link PreferenceCategory} with the given key.
     */
    private void removePreference(int categoryKey, Preference preference) {
        if (preference == null) return;

        PreferenceCategory category = (PreferenceCategory) findPreference(getString(categoryKey));

        if (category != null) {
            category.removePreference(preference);
        }
    }

    /**
     * Converts a language ID (WordPress defined) to a language code (i.e. en, es, gb, etc...).
     */
    private String convertLanguageIdToLanguageCode(String id) {
        if (id != null) {
            for (String key : mLanguageCodes.keySet()) {
                if (id.equals(mLanguageCodes.get(key))) {
                    return key;
                }
            }
        }

        return "";
    }

    private String localeInput(String languageCode) {
        if (TextUtils.isEmpty(languageCode)) return "";

        return languageCode.substring(0, 2);
    }

    /**
     * Helper method to get a value from a nested Map. Used to parse self-hosted response object.
     */
    private String getNestedMapValue(Map map, String key) {
        if (map != null && key != null) {
            return MapUtils.getMapStr((Map) map.get(key), "value");
        }

        return "";
    }

    private String firstLetterCapitalized(String input) {
        if (TextUtils.isEmpty(input)) return "";

        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    /**
     * Handles response to XML-RPC settings fetch
     */
    private final XMLRPCCallback mXmlRpcFetchCallback = new XMLRPCCallback() {
        @Override
        public void onSuccess(long id, final Object result) {
            if (isAdded() && result instanceof Map) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handleResponseToSelfHostedSettingsRequest((Map) result);
                    }
                });
            } else {
                // Response is considered an error if we are unable to parse it
                handleSettingsFetchError("Invalid response object (expected Map): " + result);
            }
        }

        @Override
        public void onFailure(long id, final Exception error) {
            if (isAdded()) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handleSettingsFetchError(error.toString());
                    }
                });
            }
        }
    };

    private final XMLRPCCallback mXmlRpcSetCallback = new XMLRPCCallback() {
        @Override
        public void onSuccess(long id, final Object result) {
            if (result instanceof Map) {
                handleResponseToSelfHostedSettingsSetRequest((Map) result);
            } else {
                // Response is considered an error if we are unable to parse it
                handleSettingsSetError("Invalid response object (expected Map): " + result);
            }
        }

        @Override
        public void onFailure(long id, final Exception error) {
            if (isAdded()) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handleSettingsSetError(error.toString());
                    }
                });
            }
        }
    };

    private void handleResponseToSelfHostedSettingsSetRequest(Map result) {
        AppLog.w(AppLog.T.API, "Site settings saved");
        for (Object key : result.keySet()) {
            Log.d("", "key=" + key + "; value=" + result.get(key));
        }
    }

    private void handleSettingsSetError(String error) {
        AppLog.w(AppLog.T.API, "Error setting site settings: " + error);
        if (isAdded()) {
            ToastUtils.showToast(getActivity(), getString(R.string.error_post_remote_site_settings));
            getActivity().finish();
        }
    }
}
