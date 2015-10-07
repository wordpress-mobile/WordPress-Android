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
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.TypefaceCache;

import java.util.Locale;

import de.greenrobot.event.EventBus;

/**
 * Handles changes to WordPress site settings. Syncs with host automatically when user leaves.
 */

public class SiteSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener,
        AdapterView.OnItemLongClickListener,
        SiteSettingsInterface.SiteSettingsListener {
    public interface HasHint {
        boolean hasHint();
        String getHint();
        void setHint(String hint);
    }

    private Blog mBlog;
    private SiteSettingsInterface mSiteSettings;

    private EditTextPreference mTitlePreference;
    private EditTextPreference mTaglinePreference;
    private EditTextPreference mAddressPreference;
    private DetailListPreference mLanguagePreference;
    private DetailListPreference mPrivacyPreference;


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

        mSiteSettings = SiteSettingsInterface.getInterface(getActivity(), mBlog, this);

        // inflate Site Settings preferences from XML
        addPreferencesFromResource(R.xml.site_settings);

        // set preference references, add change listeners, and setup various entries and values
        initPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();

        mSiteSettings.init(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Assume user wanted changes propagated when they leave
        mSiteSettings.saveSettings();
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
                            title.setTextSize(TypedValue.COMPLEX_UNIT_PX,
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
            mSiteSettings.setTitle(newValue.toString());
            changeEditTextPreferenceValue(mTitlePreference, mSiteSettings.getTitle());
            return true;
        } else if (preference == mTaglinePreference) {
            mSiteSettings.setTagline(newValue.toString());
            changeEditTextPreferenceValue(mTaglinePreference, mSiteSettings.getTagline());
            return true;
        } else if (preference == mAddressPreference) {
            mSiteSettings.setAddress(newValue.toString());
            changeEditTextPreferenceValue(mAddressPreference, mSiteSettings.getAddress());
            return true;
        } else if (preference == mLanguagePreference) {
            mSiteSettings.setLanguageCode(newValue.toString());
            changeLanguageValue(mSiteSettings.getLanguageCode());
            return true;
        } else if (preference == mPrivacyPreference) {
            mSiteSettings.setPrivacy(Integer.valueOf(newValue.toString()));
            changePrivacyValue(mSiteSettings.getPrivacy());
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
                ToastUtils.showToast(getActivity(), ((HasHint)obj).getHint(), ToastUtils.Duration.SHORT);
                return true;
            }
        }

        return false;
    }

    @Override
    public void onSettingsUpdated(Exception error) {
        if (error != null) {
            ToastUtils.showToast(getActivity(), R.string.error_fetch_remote_site_settings);
            return;
        }

        setPreferencesFromSiteSettings();
    }

    @Override
    public void onSettingsSaved(Exception error) {
        int message = error == null ?
                R.string.site_settings_save_success : R.string.error_post_remote_site_settings;

        ToastUtils.showToast(WordPress.getContext(), message);
        mBlog.setBlogName(mSiteSettings.getTitle());
        WordPress.wpDB.saveBlog(mBlog);
        EventBus.getDefault().post(new CoreEvents.BlogListChanged());
    }

    @Override
    public void onCredentialsValidated(Exception error) {
        if (error != null) {
            ToastUtils.showToast(WordPress.getContext(), R.string.username_or_password_incorrect);
        }
    }

    public void allowEditing(boolean allow) {
        if (mTitlePreference != null) mTitlePreference.setEnabled(allow);
        if (mTaglinePreference != null) mTaglinePreference.setEnabled(allow);
        if (mAddressPreference != null) mAddressPreference.setEnabled(false);
        if (mPrivacyPreference != null) mPrivacyPreference.setEnabled(allow);
        if (mLanguagePreference != null) mLanguagePreference.setEnabled(allow);
        if (mLanguagePreference != null) mLanguagePreference.setEnabled(allow);
    }

    /**
     * Helper method to retrieve {@link Preference} references and initialize any data.
     */
    private void initPreferences() {
        // Title preference
        if (null != (mTitlePreference =
                (EditTextPreference) findPreference(getString(R.string.pref_key_site_title)))) {
            mTitlePreference.setOnPreferenceChangeListener(this);
            changeEditTextPreferenceValue(mTitlePreference, StringUtils.unescapeHTML(mSiteSettings.getTitle()));
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
            }
        }
    }

    private void setPreferencesFromSiteSettings() {
        changeEditTextPreferenceValue(mTitlePreference, mSiteSettings.getTitle());
        changeEditTextPreferenceValue(mTaglinePreference, mSiteSettings.getTagline());
        changeEditTextPreferenceValue(mAddressPreference, mSiteSettings.getAddress());
        changePrivacyValue(mSiteSettings.getPrivacy());
        changeLanguageValue(mSiteSettings.getLanguageCode());
    }

    /**
     * Helper method to perform validation and set multiple properties on an EditTextPreference.
     * If newValue is equal to the current preference text no action will be taken.
     */
    private void changeEditTextPreferenceValue(EditTextPreference pref, String newValue) {
        if (pref != null && newValue != null && !newValue.equals(pref.getSummary())) {
            pref.setText(StringUtils.unescapeHTML(newValue));
            pref.setSummary(StringUtils.unescapeHTML(newValue));
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

    private String localeInput(String languageCode) {
        if (TextUtils.isEmpty(languageCode)) return "";

        return languageCode.substring(0, 2);
    }

    private String firstLetterCapitalized(String input) {
        if (TextUtils.isEmpty(input)) return "";

        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}
