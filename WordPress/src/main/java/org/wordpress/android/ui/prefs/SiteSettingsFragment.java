package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
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
import org.wordpress.android.models.CategoryNode;
import org.wordpress.android.models.SiteSettingsModel;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.TypefaceCache;

import java.util.ArrayList;
import java.util.Locale;

import de.greenrobot.event.EventBus;

/**
 * Handles changes to WordPress site settings. Syncs with host automatically when user leaves.
 */

public class SiteSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener,
                   AdapterView.OnItemLongClickListener,
                   SiteSettings.SiteSettingsListener {
    private static final String ADDRESS_FORMAT_REGEX = "^(https?://(w{3})?|www\\.)";

    public interface HasHint {
        boolean hasHint();
        String getHintText();
    }

    private Blog mBlog;

    private EditTextPreference mTitlePreference;
    private EditTextPreference mTaglinePreference;
    private EditTextPreference mAddressPreference;
    private DetailListPreference mLanguagePreference;
    private DetailListPreference mPrivacyPreference;
    private EditTextPreference mUsernamePreference;
    private EditTextPreference mPasswordPreference;
    private SwitchPreference mLocationPreference;
    private DetailListPreference mCategoryPreference;
    private DetailListPreference mFormatPreference;

    private SiteSettings mSiteSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // make sure we have local site data
        mBlog = WordPress.getBlog(
                getArguments().getInt(BlogPreferencesActivity.ARG_LOCAL_BLOG_ID, -1));

        if (!NetworkUtils.checkConnection(getActivity()) || mBlog == null) {
            getActivity().finish();
            return;
        }

        mSiteSettings = new SiteSettings(getActivity(), mBlog, this);

        setRetainInstance(true);
        addPreferencesFromResource(R.xml.site_settings);
        initPreferences();
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

    private SharedPreferences siteSettingsPreferences() {
        return getActivity().getSharedPreferences(SiteSettings.SITE_SETTINGS_PREFS, Context.MODE_PRIVATE);
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
        } else if (preference == mUsernamePreference) {
            mSiteSettings.setUsername(newValue.toString());
            changeEditTextPreferenceValue(mUsernamePreference, mSiteSettings.getUsername());
            return true;
        } else if (preference == mPasswordPreference) {
            mSiteSettings.setPassword(newValue.toString());
            changeEditTextPreferenceValue(mPasswordPreference, mSiteSettings.getPassword());
            return true;
        } else if (preference == mLocationPreference) {
            mSiteSettings.setLocation((Boolean) newValue);
            SharedPreferences prefs = siteSettingsPreferences();
            prefs.edit().putBoolean(SiteSettings.LOCATION_PREF_KEY, mSiteSettings.getLocation()).apply();
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

    @Override
    public void onSettingsUpdated(Exception error, SiteSettingsModel container) {
        if (error != null) {
            ToastUtils.showToast(getActivity(), R.string.error_fetch_remote_site_settings);
            return;
        }

        changeEditTextPreferenceValue(mTitlePreference, container.title);
        changeEditTextPreferenceValue(mTaglinePreference, container.tagline);
        changeEditTextPreferenceValue(mAddressPreference, container.address);
        changeEditTextPreferenceValue(mUsernamePreference, container.username);
        changeEditTextPreferenceValue(mPasswordPreference, container.password);
        changePrivacyValue(container.privacy);
        changeLanguageValue(container.language);

        if (mFormatPreference != null && container.postFormats != null && container.postFormats.length > 0) {
            mFormatPreference.setEntries(container.postFormats);
            mFormatPreference.setEntryValues(container.postFormats);
        }
    }

    @Override
    public void onSettingsSaved(Exception error, SiteSettingsModel container) {
        int message = error == null ?
                R.string.site_settings_save_success : R.string.error_post_remote_site_settings;

        ToastUtils.showToast(WordPress.getContext(), message);
        mBlog.setBlogName(container.title);
        WordPress.wpDB.saveBlog(mBlog);
        EventBus.getDefault().post(new CoreEvents.BlogListChanged());
    }

    @Override
    public void onCredentialsValidated(boolean valid) {
        if (!valid) {
            ToastUtils.showToast(WordPress.getContext(), R.string.username_or_password_incorrect);
        }
    }

    public void allowEditing(boolean allow) {
        // Address won't be editable until the app supports domain name changes
        if (mAddressPreference != null) mAddressPreference.setEnabled(false);
        if (mTitlePreference != null) mTitlePreference.setEnabled(allow);
        if (mTaglinePreference != null) mTaglinePreference.setEnabled(allow);
        if (mPrivacyPreference != null) mPrivacyPreference.setEnabled(allow);
        if (mLanguagePreference != null) mLanguagePreference.setEnabled(allow);
        if (mLanguagePreference != null) mLanguagePreference.setEnabled(allow);
    }

    /**
     * Helper method to retrieve {@link Preference} references and initialize any data.
     */
    private void initPreferences() {
        mTitlePreference =
                (EditTextPreference) findPreference(getString(R.string.pref_key_site_title));
        mTaglinePreference =
                (EditTextPreference) findPreference(getString(R.string.pref_key_site_tagline));
        mAddressPreference =
                (EditTextPreference) findPreference(getString(R.string.pref_key_site_address));
        mPrivacyPreference =
                (DetailListPreference) findPreference(getString(R.string.pref_key_site_visibility));
        mLanguagePreference =
                (DetailListPreference) findPreference(getString(R.string.pref_key_site_language));
        mUsernamePreference =
                (EditTextPreference) findPreference(getString(R.string.pref_key_site_username));
        mPasswordPreference =
                (EditTextPreference) findPreference(getString(R.string.pref_key_site_password));
        mLocationPreference =
                (SwitchPreference) findPreference(getString(R.string.pref_key_site_location));
        mCategoryPreference =
                (DetailListPreference) findPreference(getString(R.string.pref_key_site_category));
        mFormatPreference =
                (DetailListPreference) findPreference(getString(R.string.pref_key_site_format));

        changeEditTextPreferenceValue(mTitlePreference, mSiteSettings.getTitle());
        changeEditTextPreferenceValue(mTaglinePreference, mSiteSettings.getTagline());
        changeEditTextPreferenceValue(mAddressPreference, mSiteSettings.getAddress());
        mLocationPreference.setChecked(siteSettingsPreferences().getBoolean(SiteSettings.LOCATION_PREF_KEY, false));

        loadCategories();

        String[] details2 = new String[Math.max(mFormatPreference.getEntries().length, 1)];
        mFormatPreference.setDetails(details2);

        mTitlePreference.setOnPreferenceChangeListener(this);
        mTaglinePreference.setOnPreferenceChangeListener(this);
        mAddressPreference.setOnPreferenceChangeListener(this);
        mLocationPreference.setOnPreferenceChangeListener(this);
        mCategoryPreference.setOnPreferenceChangeListener(this);
        mFormatPreference.setOnPreferenceChangeListener(this);

        if (mBlog.isDotcomFlag()) {
            PreferenceScreen screen =
                    (PreferenceScreen) findPreference(getString(R.string.pref_key_site_screen));
            if (screen != null) {
                screen.removePreference(findPreference(getString(R.string.pref_key_site_account)));
            }
            changeLanguageValue(mSiteSettings.getLanguageCode());
            changePrivacyValue(mSiteSettings.getPrivacy());
            mPrivacyPreference.setOnPreferenceChangeListener(this);
            mLanguagePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(R.string.pref_key_site_general, mPrivacyPreference);
            removePreference(R.string.pref_key_site_general, mLanguagePreference);
            changeEditTextPreferenceValue(mUsernamePreference, mSiteSettings.getUsername());
            changeEditTextPreferenceValue(mPasswordPreference, mSiteSettings.getPassword());
            mUsernamePreference.setOnPreferenceChangeListener(this);
            mPasswordPreference.setOnPreferenceChangeListener(this);
        }
    }

    private void loadCategories() {
        CategoryNode rootCategory = CategoryNode.createCategoryTreeFromDB(mBlog.getLocalTableBlogId());
        ArrayList<CategoryNode> categoryLevels = CategoryNode.getSortedListOfCategoriesFromRoot(rootCategory);
        String[] categories = new String[categoryLevels.size()];

        for (int i = 0; i < categoryLevels.size(); ++i) {
            categories[i] = categoryLevels.get(i).getName();
        }

        mCategoryPreference.setEntries(categories);
        mCategoryPreference.setEntryValues(categories);
    }
    /**
     * Helper method to perform validation and set multiple properties on an EditTextPreference.
     * If newValue is equal to the current preference text no action will be taken.
     */
    private void changeEditTextPreferenceValue(EditTextPreference pref, String newValue) {
        if (newValue == null || pref == null || pref.getEditText().isInEditMode()) return;

        if (!pref.getSummary().equals(newValue)) {
            String formattedValue = StringUtils.unescapeHTML(newValue.replaceFirst(ADDRESS_FORMAT_REGEX, ""));

            pref.setText(formattedValue);
            pref.setSummary(formattedValue);
        }
    }

    private void changeLanguageValue(String newValue) {
        if (mLanguagePreference == null) return;
        String expectedSummary = firstLetterCapitalized(getLanguageString(newValue, new Locale(localeInput(newValue))));

        if (!expectedSummary.equals(mLanguagePreference.getSummary())) {
            mLanguagePreference.setValue(newValue);
            mLanguagePreference.setSummary(expectedSummary);

            // update details to display in selected locale
            String[] languageCodes = getResources().getStringArray(R.array.language_codes);
            mLanguagePreference.setEntries(createLanguageDisplayStrings(languageCodes));
            mLanguagePreference.setDetails(createLanguageDetailDisplayStrings(languageCodes, newValue));
            mLanguagePreference.refreshAdapter();
        }
    }

    private void changePrivacyValue(int newValue) {
        if (mPrivacyPreference == null) return;
        String expectedSummary = privacyStringForValue(newValue);

        if (!expectedSummary.equals(mPrivacyPreference.getSummary())) {
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
