package org.wordpress.android.ui.prefs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
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
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.widgets.TypefaceCache;

import java.util.Locale;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * Handles changes to WordPress site settings. Syncs with host automatically when user leaves.
 */

public class SiteSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener,
                   Preference.OnPreferenceClickListener,
                   AdapterView.OnItemLongClickListener,
                   SiteSettingsInterface.SiteSettingsListener {

    private static final String ADDRESS_FORMAT_REGEX = "^(https?://(w{3})?|www\\.)";
    private static final int RELATED_POSTS_REQUEST_CODE = 1;
    private static final int NO_REGION_LANG_CODE_LEN = 2;
    private static final int REGION_SUBSTRING_INDEX = 3;

    private Blog mBlog;
    private SiteSettingsInterface mSiteSettings;

    private EditTextPreference mTitlePreference;
    private EditTextPreference mTaglinePreference;
    private EditTextPreference mAddressPreference;
    private DetailListPreference mLanguagePreference;
    private DetailListPreference mPrivacyPreference;
    private EditTextPreference mUsernamePreference;
    private EditTextPreference mPasswordPreference;
    private WPSwitchPreference mLocationPreference;
    private DetailListPreference mCategoryPreference;
    private DetailListPreference mFormatPreference;
    private Preference mRelatedPostsPreference;
    private WPSwitchPreference mAllowComments;
    private WPSwitchPreference mSendPingbacks;
    private WPSwitchPreference mReceivePingbacks;
    private WPSwitchPreference mIdentityRequiredPreference;
    private WPSwitchPreference mUserAccountRequiredPreference;
    private DetailListPreference mCloseAfterPreference;
    private DetailListPreference mSortByPreference;
    private DetailListPreference mThreadingPreference;
    private DetailListPreference mPagingPreference;
    private DetailListPreference mWhitelistPreference;
    private WPPreference mMultipleLinksPreference;
    private DetailListPreference mModerationHoldPreference;
    private DetailListPreference mBlacklistPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // make sure we have local site data
        mBlog = WordPress.getBlog(
                getArguments().getInt(BlogPreferencesActivity.ARG_LOCAL_BLOG_ID, -1));

        // TODO: offline editing
        if (!NetworkUtils.checkConnection(getActivity()) || mBlog == null) {
            getActivity().finish();
            return;
        }

        mSiteSettings = SiteSettingsInterface.getInterface(getActivity(), mBlog, this);

        setRetainInstance(true);
        addPreferencesFromResource(R.xml.site_settings);
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
                            title.setTextColor(ContextCompat.getColor(getActivity(),
                                                                      R.color.orange_jazzy));
                        }
                    }

                    @Override
                    public void onChildViewRemoved(View parent, View child) {
                    }
                });
                prefList.setOnItemLongClickListener(this);
                prefList.setFooterDividersEnabled(false);
                //noinspection deprecation
                prefList.setOverscrollFooter(res.getDrawable(R.color.transparent));
            }
        }

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RELATED_POSTS_REQUEST_CODE:
                if (data == null) return;
                mSiteSettings.setShowRelatedPosts(data.getBooleanExtra(RelatedPostsDialog.SHOW_RELATED_POSTS_KEY, false));
                mSiteSettings.setShowRelatedPostHeader(data.getBooleanExtra(RelatedPostsDialog.SHOW_HEADER_KEY, false));
                mSiteSettings.setShowRelatedPostImages(data.getBooleanExtra(RelatedPostsDialog.SHOW_IMAGES_KEY, false));
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mRelatedPostsPreference) {
            showRelatedPostsDialog();
            return true;
        }

        return false;
    }

     @Override
     public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        // If the user has clicked on a preference screen, set up the screen
        if (preference instanceof PreferenceScreen) {
            Dialog prefDialog = ((PreferenceScreen) preference).getDialog();
            if (prefDialog != null) {
                String title = String.valueOf(preference.getTitle());
                WPActivityUtils.addToolbarToDialog(this, prefDialog, title);
            }
        }

        return false;
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
        } else if (preference == mAllowComments) {
            mSiteSettings.setAllowComments((Boolean) newValue);
            return true;
        } else if (preference == mSendPingbacks) {
            mSiteSettings.setSendPingbacks((Boolean) newValue);
            return true;
        } else if (preference == mReceivePingbacks) {
            mSiteSettings.setReceivePingbacks((Boolean) newValue);
            return true;
        } else if (preference == mCloseAfterPreference) {
            return true;
        } else if (preference == mSortByPreference) {
            return true;
        } else if (preference == mThreadingPreference) {
            return true;
        } else if (preference == mPagingPreference) {
            return true;
        } else if (preference == mIdentityRequiredPreference) {
            mSiteSettings.setIdentityRequired((Boolean) newValue);
            return true;
        } else if (preference == mUserAccountRequiredPreference) {
            mSiteSettings.setUserAccountRequired((Boolean) newValue);
            return true;
        } else if (preference == mWhitelistPreference) {
//            mSiteSettings.setUseCommentWhitelist((Boolean) newValue);
            return true;
        } else if (preference == mMultipleLinksPreference) {
//            mSiteSettings.setMultipleLinks((Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mModerationHoldPreference) {
            return true;
        } else if (preference == mBlacklistPreference) {
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
            return true;
        } else if (preference == mCategoryPreference) {
            mSiteSettings.setDefaultCategory(Integer.parseInt(newValue.toString()));
            mCategoryPreference.setValue(newValue.toString());
            mCategoryPreference.setSummary(mSiteSettings.getDefaultCategoryForDisplay());
            mCategoryPreference.refreshAdapter();
            return true;
        } else if (preference == mFormatPreference) {
            mSiteSettings.setDefaultFormat(newValue.toString());
            mFormatPreference.setValue(newValue.toString());
            mFormatPreference.setSummary(mSiteSettings.getDefaultPostFormatDisplay());
            mFormatPreference.refreshAdapter();
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
            } else if (obj instanceof PreferenceHint) {
                PreferenceHint hintObj = (PreferenceHint) obj;
                if (hintObj.hasHint()) {
                    ToastUtils.showToast(getActivity(), hintObj.getHint(), ToastUtils.Duration.SHORT);
                }
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
        // Address won't be editable until the app supports domain name changes
        if (mAddressPreference != null) mAddressPreference.setEnabled(false);
        if (mTitlePreference != null) mTitlePreference.setEnabled(allow);
        if (mTaglinePreference != null) mTaglinePreference.setEnabled(allow);
        if (mPrivacyPreference != null) mPrivacyPreference.setEnabled(allow);
        if (mLanguagePreference != null) mLanguagePreference.setEnabled(allow);
    }

    /**
     * Helper method to retrieve {@link Preference} references and initialize any data.
     */
    private void initPreferences() {
        mTitlePreference = (EditTextPreference) getPref(R.string.pref_key_site_title);
        mTaglinePreference = (EditTextPreference) getPref(R.string.pref_key_site_tagline);
        mAddressPreference = (EditTextPreference) getPref(R.string.pref_key_site_address);
        mPrivacyPreference = (DetailListPreference) getPref(R.string.pref_key_site_visibility);
        mLanguagePreference = (DetailListPreference) getPref(R.string.pref_key_site_language);
        mUsernamePreference = (EditTextPreference) getPref(R.string.pref_key_site_username);
        mPasswordPreference = (EditTextPreference) getPref(R.string.pref_key_site_password);
        mLocationPreference = (WPSwitchPreference) getPref(R.string.pref_key_site_location);
        mCategoryPreference = (DetailListPreference) getPref(R.string.pref_key_site_category);
        mFormatPreference = (DetailListPreference) getPref(R.string.pref_key_site_format);
        mAllowComments = (WPSwitchPreference) getPref(R.string.pref_key_site_allow_comments);
        mSendPingbacks = (WPSwitchPreference) getPref(R.string.pref_key_site_send_pingbacks);
        mReceivePingbacks = (WPSwitchPreference) getPref(R.string.pref_key_site_receive_pingbacks);
        mIdentityRequiredPreference = (WPSwitchPreference) getPref(R.string.pref_key_site_identity_required);
        mUserAccountRequiredPreference = (WPSwitchPreference) getPref(R.string.pref_key_site_user_account_required);
        mCloseAfterPreference = (DetailListPreference) getPref(R.string.pref_key_site_close_after);
        mSortByPreference = (DetailListPreference) getPref(R.string.pref_key_site_sort_by);
        mThreadingPreference = (DetailListPreference) getPref(R.string.pref_key_site_threading);
        mPagingPreference = (DetailListPreference) getPref(R.string.pref_key_site_paging);
        mWhitelistPreference = (DetailListPreference) getPref(R.string.pref_key_site_whitelist);
        mMultipleLinksPreference = (WPPreference) getPref(R.string.pref_key_site_multiple_links);
        mModerationHoldPreference = (DetailListPreference) getPref(R.string.pref_key_site_moderation_hold);
        mBlacklistPreference = (DetailListPreference) getPref(R.string.pref_key_site_blacklist);
        mRelatedPostsPreference = findPreference(getString(R.string.pref_key_site_related_posts));
        mRelatedPostsPreference.setOnPreferenceClickListener(this);

        // .com sites hide the Account category, self-hosted sites hide the Related Posts preference
        if (mBlog.isDotcomFlag()) {
            removePreference(R.string.pref_key_site_screen, R.string.pref_key_site_account);
        } else {
            removePreference(R.string.pref_key_site_general, R.string.pref_key_site_visibility);
            removePreference(R.string.pref_key_site_general, R.string.pref_key_site_language);
            removePreference(R.string.pref_key_site_writing, R.string.pref_key_site_related_posts);
            removePreference(R.string.pref_key_site_writing, R.string.pref_key_site_category);
            removePreference(R.string.pref_key_site_writing, R.string.pref_key_site_format);
        }
    }

    private void showRelatedPostsDialog() {
        DialogFragment relatedPosts = new RelatedPostsDialog();
        Bundle args = new Bundle();
        args.putBoolean(RelatedPostsDialog.SHOW_RELATED_POSTS_KEY, mSiteSettings.getShowRelatedPosts());
        args.putBoolean(RelatedPostsDialog.SHOW_HEADER_KEY, mSiteSettings.getShowRelatedPostHeader());
        args.putBoolean(RelatedPostsDialog.SHOW_IMAGES_KEY, mSiteSettings.getShowRelatedPostImages());
        relatedPosts.setArguments(args);
        relatedPosts.setTargetFragment(this, RELATED_POSTS_REQUEST_CODE);
        relatedPosts.show(getFragmentManager(), "related-posts");
    }

    private void setPreferencesFromSiteSettings() {
        mLocationPreference.setChecked(mSiteSettings.getLocation());
        changeEditTextPreferenceValue(mTitlePreference, mSiteSettings.getTitle());
        changeEditTextPreferenceValue(mTaglinePreference, mSiteSettings.getTagline());
        changeEditTextPreferenceValue(mAddressPreference, mSiteSettings.getAddress());
        changeEditTextPreferenceValue(mUsernamePreference, mSiteSettings.getUsername());
        changeEditTextPreferenceValue(mPasswordPreference, mSiteSettings.getPassword());
        changePrivacyValue(mSiteSettings.getPrivacy());
        changeLanguageValue(mSiteSettings.getLanguageCode());
        setCategories();
        setPostFormats();
        mAllowComments.setChecked(mSiteSettings.getAllowComments());
        mSendPingbacks.setChecked(mSiteSettings.getSendPingbacks());
        mReceivePingbacks.setChecked(mSiteSettings.getReceivePingbacks());
        mIdentityRequiredPreference.setChecked(mSiteSettings.getIdentityRequired());
        mUserAccountRequiredPreference.setChecked(mSiteSettings.getUserAccountRequired());
        mCloseAfterPreference.setValue(String.valueOf(mSiteSettings.getCloseAfter()));
        mSortByPreference.setValue(String.valueOf(mSiteSettings.getCommentSorting()));
        mThreadingPreference.setValue(String.valueOf(mSiteSettings.getThreadingLevels()));
        mPagingPreference.setValue(String.valueOf(mSiteSettings.getPagingCount()));
//        mWhitelistPreference.setChecked(mSiteSettings.getUseCommentWhitelist());
//        mMultipleLinksPreference.setChecked(mSiteSettings.getMultipleLinks() >= 0);
        mModerationHoldPreference.setValue(mSiteSettings.getModerationKeys().toString());
        mBlacklistPreference.setValue(mSiteSettings.getBlacklistKeys().toString());
    }

    private void setCategories() {
        // Ignore if there are no changes
        if (mSiteSettings.isSameCategoryList(mCategoryPreference.getEntryValues())) {
            mCategoryPreference.setValue(String.valueOf(mSiteSettings.getDefaultCategory()));
            mCategoryPreference.setSummary(mSiteSettings.getDefaultCategoryForDisplay());
            return;
        }

        Map<Integer, String> categories = mSiteSettings.getCategoryNames();
        CharSequence[] entries = new CharSequence[categories.size()];
        CharSequence[] values = new CharSequence[categories.size()];
        int i = 0;
        for (Integer key : categories.keySet()) {
            entries[i] = categories.get(key);
            values[i++] = String.valueOf(key);
        }

        mCategoryPreference.setEntries(entries);
        mCategoryPreference.setEntryValues(values);
        mCategoryPreference.setValue(String.valueOf(mSiteSettings.getDefaultCategory()));
        mCategoryPreference.setSummary(mSiteSettings.getDefaultCategoryForDisplay());
    }

    private void setPostFormats() {
        // Ignore if there are no changes
        if (mSiteSettings.isSameFormatList(mFormatPreference.getEntryValues())) {
            mFormatPreference.setValue(String.valueOf(mSiteSettings.getDefaultFormat()));
            mFormatPreference.setSummary(mSiteSettings.getDefaultPostFormatDisplay());
            return;
        }

        Map<String, String> formats = mSiteSettings.getFormats();
        String[] formatKeys = mSiteSettings.getFormatKeys();
        if (formats == null || formatKeys == null) return;
        String[] entries = new String[formatKeys.length];
        String[] values = new String[formatKeys.length];

        for (int i = 0; i < entries.length; ++i) {
            entries[i] = formats.get(formatKeys[i]);
            values[i] = formatKeys[i];
        }

        mFormatPreference.setEntries(entries);
        mFormatPreference.setEntryValues(values);
        mFormatPreference.setValue(String.valueOf(mSiteSettings.getDefaultFormat()));
        mFormatPreference.setSummary(mSiteSettings.getDefaultPostFormatDisplay());
    }

    /**
     * Helper method to perform validation and set multiple properties on an EditTextPreference.
     * If newValue is equal to the current preference text no action will be taken.
     */
    private void changeEditTextPreferenceValue(EditTextPreference pref, String newValue) {
        if (newValue == null || pref == null || pref.getEditText().isInEditMode()) return;

        if (!newValue.equals(pref.getSummary())) {
            String formattedValue = StringUtils.unescapeHTML(newValue.replaceFirst(ADDRESS_FORMAT_REGEX, ""));

            pref.setText(formattedValue);
            pref.setSummary(formattedValue);
        }
    }

    /**
     * Detail strings for the dialog are generated in the selected language.
     */
    private void changeLanguageValue(String newValue) {
        if (mLanguagePreference == null || newValue == null) return;

        if (TextUtils.isEmpty(mLanguagePreference.getSummary()) ||
                !newValue.equals(mLanguagePreference.getValue())) {
            mLanguagePreference.setValue(newValue);
            String summary = getLanguageString(newValue, languageLocale(newValue));
            mLanguagePreference.setSummary(summary);

            // update details to display in selected locale
            CharSequence[] languageCodes = mLanguagePreference.getEntryValues();
            mLanguagePreference.setEntries(createLanguageDisplayStrings(languageCodes));
            mLanguagePreference.setDetails(createLanguageDetailDisplayStrings(languageCodes, newValue));
            mLanguagePreference.refreshAdapter();
        }
    }

    /**
     * Updates the privacy preference summary.
     */
    private void changePrivacyValue(int newValue) {
        if (mPrivacyPreference == null) return;

        if (TextUtils.isEmpty(mPrivacyPreference.getSummary()) ||
                newValue != Integer.valueOf(mPrivacyPreference.getValue())) {
            mPrivacyPreference.setValue(String.valueOf(newValue));
            mPrivacyPreference.setSummary(mSiteSettings.getPrivacyForDisplay());
            mPrivacyPreference.refreshAdapter();
        }
    }

    /**
     * Generates display strings for given language codes. Used as entries in language preference.
     */
    private String[] createLanguageDisplayStrings(CharSequence[] languageCodes) {
        if (languageCodes == null || languageCodes.length < 1) return null;

        String[] displayStrings = new String[languageCodes.length];

        for (int i = 0; i < languageCodes.length; ++i) {
            displayStrings[i] = capitalizeFrontOfString(getLanguageString(
                    String.valueOf(languageCodes[i]), languageLocale(languageCodes[i].toString())), 1);
        }

        return displayStrings;
    }

    /**
     * Generates detail display strings in the currently selected locale. Used as detail text
     * in language preference dialog.
     */
    public String[] createLanguageDetailDisplayStrings(CharSequence[] languageCodes, String locale) {
        if (languageCodes == null || languageCodes.length < 1) return null;

        String[] detailStrings = new String[languageCodes.length];
        for (int i = 0; i < languageCodes.length; ++i) {
            detailStrings[i] = capitalizeFrontOfString(
                    getLanguageString(languageCodes[i].toString(), languageLocale(locale)), 1);
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

        Locale languageLocale = languageLocale(languageCode);
        String displayLanguage = languageLocale.getDisplayLanguage(displayLocale);
        String displayCountry = languageLocale.getDisplayCountry(displayLocale);

        if (!TextUtils.isEmpty(displayCountry)) {
            return displayLanguage + " (" + displayCountry + ")";
        }
        return displayLanguage;
    }

    /**
     * Gets a preference and sets the {@link android.preference.Preference.OnPreferenceChangeListener}.
     */
    private Preference getPref(int id) {
        Preference pref = findPreference(getString(id));
        pref.setOnPreferenceChangeListener(this);
        return pref;
    }

    /**
     * Removes a {@link Preference} from the {@link PreferenceCategory} with the given key.
     */
    private void removePreference(int parentKey, int preference) {
        PreferenceGroup parent = (PreferenceGroup) findPreference(getString(parentKey));

        if (parent != null) {
            parent.removePreference(findPreference(getString(preference)));
        }
    }

    private Locale languageLocale(String languageCode) {
        if (TextUtils.isEmpty(languageCode)) return Locale.getDefault();

        if (languageCode.length() > NO_REGION_LANG_CODE_LEN) {
            return new Locale(languageCode.substring(0, NO_REGION_LANG_CODE_LEN),
                    languageCode.substring(REGION_SUBSTRING_INDEX));
        }

        return new Locale(languageCode);
    }

    private String capitalizeFrontOfString(String input, int numToCap) {
        if (TextUtils.isEmpty(input)) return "";
        return input.substring(0, numToCap).toUpperCase() + input.substring(numToCap);
    }
}
