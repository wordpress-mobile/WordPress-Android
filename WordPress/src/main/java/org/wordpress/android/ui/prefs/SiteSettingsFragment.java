package org.wordpress.android.ui.prefs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.stats.StatsWidgetProvider;
import org.wordpress.android.ui.stats.datasets.StatsTable;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.WPPrefUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * Allows interfacing with WordPress site settings. Works with WP.com and WP.org v4.5+ (pending).
 *
 * Settings are synced automatically when local changes are made.
 */

public class SiteSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener,
                   Preference.OnPreferenceClickListener,
                   AdapterView.OnItemLongClickListener,
                   ViewGroup.OnHierarchyChangeListener,
                   Dialog.OnDismissListener,
                   SiteSettingsInterface.SiteSettingsListener {

    /**
     * Use this argument to pass the {@link Integer} local blog ID to this fragment.
     */
    public static final String ARG_LOCAL_BLOG_ID = "local_blog_id";

    /**
     * When the user removes a site (by selecting Delete Site) the parent {@link Activity} result
     * is set to this value and {@link Activity#finish()} is invoked.
     */
    public static final int RESULT_BLOG_REMOVED = Activity.RESULT_FIRST_USER;

    /**
     * Provides the regex to identify domain HTTP(S) protocol and/or 'www' sub-domain.
     *
     * Used to format user-facing {@link String}'s in certain preferences.
     */
    private static final String ADDRESS_FORMAT_REGEX = "^(https?://(w{3})?|www\\.)";

    /**
     * Request code used when creating the {@link RelatedPostsDialog}.
     */
    private static final int RELATED_POSTS_REQUEST_CODE = 1;

    /**
     * Length of a {@link String} (representing a language code) when there is no region included.
     * For example: "en" contains no region, "en_US" contains a region (US)
     *
     * Used to parse a language code {@link String} when creating a {@link Locale}.
     */
    private static final int NO_REGION_LANG_CODE_LEN = 2;

    /**
     * Index of a language code {@link String} where the region code begins. The language code
     * format is cc_rr, where cc is the country code (e.g. en, es, az) and rr is the region code
     * (e.g. us, au, gb).
     */
    private static final int REGION_SUBSTRING_INDEX = 3;

    // Reference to blog obtained from passed ID (ARG_LOCAL_BLOG_ID)
    private Blog mBlog;

    // Can interface with WP.com or WP.org
    private SiteSettingsInterface mSiteSettings;

    // Reference to the list of items being edited in the current list editor
    private List<String> mEditingList;

    // Used to ensure that settings are only fetched once throughout the lifecycle of the fragment
    private boolean mShouldFetch;

    // Used with Close After and Paging dialogs to determine settings values
    private boolean mSwitchChecked;

    // Used to customize Close After, Paging, and Multiple Links dialog views
    private NumberPicker mNumberPicker;

    // General settings
    private EditTextPreference mTitlePref;
    private EditTextPreference mTaglinePref;
    private EditTextPreference mAddressPref;
    private DetailListPreference mPrivacyPref;
    private DetailListPreference mLanguagePref;

    // Account settings (NOTE: only for WP.org)
    private EditTextPreference mUsernamePref;
    private EditTextPreference mPasswordPref;

    // Writing settings
    private WPSwitchPreference mLocationPref;
    private DetailListPreference mCategoryPref;
    private DetailListPreference mFormatPref;
    private Preference mRelatedPostsPref;

    // Discussion settings preview
    private WPSwitchPreference mAllowCommentsPref;
    private WPSwitchPreference mSendPingbacksPref;
    private WPSwitchPreference mReceivePingbacksPref;

    // Discussion settings -> Defaults for New Posts
    private WPSwitchPreference mAllowCommentsNested;
    private WPSwitchPreference mSendPingbacksNested;
    private WPSwitchPreference mReceivePingbacksNested;

    // Discussion settings -> Comments
    private WPSwitchPreference mIdentityRequiredPreference;
    private WPSwitchPreference mUserAccountRequiredPref;
    private Preference mCloseAfterPref;
    private DetailListPreference mSortByPref;
    private DetailListPreference mThreadingPref;
    private Preference mPagingPref;
    private DetailListPreference mWhitelistPref;
    private Preference mMultipleLinksPref;
    private Preference mModerationHoldPref;
    private Preference mBlacklistPref;

    // Delete site option (NOTE: only for WP.org)
    private Preference mDeleteSitePref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();

        // make sure we have local site data and a network connection, otherwise finish activity
        mBlog = WordPress.getBlog(getArguments().getInt(ARG_LOCAL_BLOG_ID, -1));
        if (mBlog == null || !NetworkUtils.checkConnection(activity)) {
            getActivity().finish();
            return;
        }

        // track successful settings screen access
        AnalyticsUtils.trackWithCurrentBlogDetails(
                AnalyticsTracker.Stat.SITE_SETTINGS_ACCESSED);

        // setup state to fetch remote settings
        mShouldFetch = true;

        // initialize the appropriate settings interface (WP.com or WP.org)
        mSiteSettings = SiteSettingsInterface.getInterface(activity, mBlog, this);

        setRetainInstance(true);
        addPreferencesFromResource(R.xml.site_settings);

        // toggle which preferences are shown and set references
        initPreferences();
    }

    @Override
    public void onPause() {
        super.onPause();
        WordPress.wpDB.saveBlog(mBlog);
    }

    @Override
    public void onResume() {
        super.onResume();

        // initialize settings with locally cached values, fetch remote on first pass
        mSiteSettings.init(mShouldFetch);
        // stop future calls from fetching remote settings
        mShouldFetch = false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RELATED_POSTS_REQUEST_CODE:
                // data is null if user cancelled editing Related Posts settings
                if (data == null) break;
                mSiteSettings.setShowRelatedPosts(data.getBooleanExtra(
                        RelatedPostsDialog.SHOW_RELATED_POSTS_KEY, false));
                mSiteSettings.setShowRelatedPostHeader(data.getBooleanExtra(
                        RelatedPostsDialog.SHOW_HEADER_KEY, false));
                mSiteSettings.setShowRelatedPostImages(data.getBooleanExtra(
                        RelatedPostsDialog.SHOW_IMAGES_KEY, false));
                mSiteSettings.saveSettings();
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        // use a wrapper to apply the Calypso theme
        Context themer = new ContextThemeWrapper(getActivity(), R.style.Calypso_SiteSettingsTheme);
        LayoutInflater localInflater = inflater.cloneInContext(themer);
        View view = super.onCreateView(localInflater, container, savedInstanceState);

        if (view != null) {
            setupPreferenceList((ListView) view.findViewById(android.R.id.list), getResources());
        }

        return view;
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        if (child.getId() == android.R.id.title && child instanceof TextView) {
            // style preference category title views
            TextView title = (TextView) child;
            WPPrefUtils.layoutAsBody2(title);
        } else {
            // style preference title views
            TextView title = (TextView) child.findViewById(android.R.id.title);
            if (title != null) WPPrefUtils.layoutAsSubhead(title);
        }
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
        // NOP
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        super.onPreferenceTreeClick(screen, preference);

        // More preference selected, style the Discussion screen
        if (preference == findPreference(getString(R.string.pref_key_site_more_discussion))) {
            Dialog dialog = ((PreferenceScreen) preference).getDialog();
            if (dialog == null) return false;

            setupPreferenceList((ListView) dialog.findViewById(android.R.id.list), getResources());

            // add Action Bar
            String title = getString(R.string.site_settings_discussion_title);
            WPActivityUtils.addToolbarToDialog(this, dialog, title);

            // track user accessing the full Discussion settings screen
            AnalyticsUtils.trackWithCurrentBlogDetails(
                    AnalyticsTracker.Stat.SITE_SETTINGS_ACCESSED_MORE_SETTINGS);
        }

        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mRelatedPostsPref) {
            showRelatedPostsDialog();
            return true;
        } else if (preference == mMultipleLinksPref) {
            showMultipleLinksDialog();
            return true;
        } else if (preference == mModerationHoldPref) {
            mEditingList = mSiteSettings.getModerationKeys();
            showListEditorDialog(R.string.site_settings_moderation_hold_title,
                    R.string.site_settings_hold_for_moderation_description);
            return true;
        } else if (preference == mBlacklistPref) {
            mEditingList = mSiteSettings.getBlacklistKeys();
            showListEditorDialog(R.string.site_settings_blacklist_title,
                    R.string.site_settings_blacklist_description);
            return true;
        } else if (preference == mDeleteSitePref) {
            removeBlogWithConfirmation();
            return true;
        } else if (preference == mCloseAfterPref) {
            showCloseAfterDialog();
            return true;
        } else if (preference == mPagingPref) {
            showPagingDialog();
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue == null) return false;

        if (preference == mTitlePref) {
            mSiteSettings.setTitle(newValue.toString());
            changeEditTextPreferenceValue(mTitlePref, mSiteSettings.getTitle());
        } else if (preference == mTaglinePref) {
            mSiteSettings.setTagline(newValue.toString());
            changeEditTextPreferenceValue(mTaglinePref, mSiteSettings.getTagline());
        } else if (preference == mAddressPref) {
            mSiteSettings.setAddress(newValue.toString());
            changeEditTextPreferenceValue(mAddressPref, mSiteSettings.getAddress());
        } else if (preference == mLanguagePref) {
            mSiteSettings.setLanguageCode(newValue.toString());
            changeLanguageValue(mSiteSettings.getLanguageCode());
        } else if (preference == mPrivacyPref) {
            mSiteSettings.setPrivacy(Integer.valueOf(newValue.toString()));
            setDetailListPreferenceValue(mPrivacyPref,
                    String.valueOf(mSiteSettings.getPrivacy()),
                    getPrivacySummary(mSiteSettings.getPrivacy()));
        } else if (preference == mAllowCommentsPref || preference == mAllowCommentsNested) {
            setAllowComments((Boolean) newValue);
        } else if (preference == mSendPingbacksPref || preference == mSendPingbacksNested) {
            setSendPingbacks((Boolean) newValue);
        } else if (preference == mReceivePingbacksPref || preference == mReceivePingbacksNested) {
            setReceivePingbacks((Boolean) newValue);
        } else if (preference == mCloseAfterPref) {
            mSiteSettings.setCloseAfter(Integer.parseInt(newValue.toString()));
            mCloseAfterPref.setSummary(getCloseAfterSummary(mSiteSettings.getCloseAfter()));
        } else if (preference == mSortByPref) {
            mSiteSettings.setCommentSorting(Integer.parseInt(newValue.toString()));
            setDetailListPreferenceValue(mSortByPref,
                    newValue.toString(),
                    getSortOrderSummary(mSiteSettings.getCommentSorting()));
        } else if (preference == mThreadingPref) {
            mSiteSettings.setThreadingLevels(Integer.parseInt(newValue.toString()));
            setDetailListPreferenceValue(mThreadingPref,
                    newValue.toString(),
                    getThreadingSummary(mSiteSettings.getThreadingLevels()));
        } else if (preference == mPagingPref) {
            mSiteSettings.setPagingCount(Integer.parseInt(newValue.toString()));
            mPagingPref.setSummary(getPagingSummary(mSiteSettings.getPagingCount()));
        } else if (preference == mIdentityRequiredPreference) {
            mSiteSettings.setIdentityRequired((Boolean) newValue);
        } else if (preference == mUserAccountRequiredPref) {
            mSiteSettings.setUserAccountRequired((Boolean) newValue);
        } else if (preference == mWhitelistPref) {
            updateWhitelistSettings(Integer.parseInt(newValue.toString()));
        } else if (preference == mMultipleLinksPref) {
            mSiteSettings.setMultipleLinks(Integer.parseInt(newValue.toString()));
            mMultipleLinksPref.setSummary(getResources()
                    .getQuantityString(R.plurals.site_settings_multiple_links_summary,
                            mSiteSettings.getMultipleLinks(),
                            mSiteSettings.getMultipleLinks()));
        } else if (preference == mUsernamePref) {
            mSiteSettings.setUsername(newValue.toString());
            changeEditTextPreferenceValue(mUsernamePref, mSiteSettings.getUsername());
        } else if (preference == mPasswordPref) {
            mSiteSettings.setPassword(newValue.toString());
            changeEditTextPreferenceValue(mPasswordPref, mSiteSettings.getPassword());
        } else if (preference == mLocationPref) {
            mSiteSettings.setLocation((Boolean) newValue);
        } else if (preference == mCategoryPref) {
            mSiteSettings.setDefaultCategory(Integer.parseInt(newValue.toString()));
            setDetailListPreferenceValue(mCategoryPref,
                    newValue.toString(),
                    mSiteSettings.getDefaultCategoryForDisplay());
        } else if (preference == mFormatPref) {
            mSiteSettings.setDefaultFormat(newValue.toString());
            setDetailListPreferenceValue(mFormatPref,
                    newValue.toString(),
                    mSiteSettings.getDefaultPostFormatDisplay());
        } else {
            return false;
        }

        mSiteSettings.saveSettings();

        return true;
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
                    HashMap<String, Object> properties = new HashMap<>();
                    properties.put("hint_shown", hintObj.getHint());
                    AnalyticsUtils.trackWithCurrentBlogDetails(
                            AnalyticsTracker.Stat.SITE_SETTINGS_HINT_TOAST_SHOWN, properties);
                    ToastUtils.showToast(getActivity(), hintObj.getHint(), ToastUtils.Duration.SHORT);
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mSiteSettings.saveSettings();
        mEditingList = null;
    }

    @Override
    public void onSettingsUpdated(Exception error) {
        if (error != null) {
            ToastUtils.showToast(getActivity(), R.string.error_fetch_remote_site_settings);
            getActivity().finish();
            return;
        }

        if (isAdded()) setPreferencesFromSiteSettings();
    }

    @Override
    public void onSettingsSaved(Exception error) {
        if (error != null) {
            ToastUtils.showToast(WordPress.getContext(), R.string.error_post_remote_site_settings);
            return;
        }
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
        if (mAddressPref != null) mAddressPref.setEnabled(false);
        if (mTitlePref != null) mTitlePref.setEnabled(allow);
        if (mTaglinePref != null) mTaglinePref.setEnabled(allow);
        if (mPrivacyPref != null) mPrivacyPref.setEnabled(allow);
        if (mLanguagePref != null) mLanguagePref.setEnabled(allow);
    }

    private void setupPreferenceList(ListView prefList, Resources res) {
        if (prefList == null || res == null) return;

        // handle long clicks on preferences to display hints
        prefList.setOnItemLongClickListener(this);
        // required to customize (Calypso) preference views
        prefList.setOnHierarchyChangeListener(this);
        // remove footer divider bar
        prefList.setFooterDividersEnabled(false);
        //noinspection deprecation
        prefList.setOverscrollFooter(res.getDrawable(R.color.transparent));
    }

    /**
     * Helper method to retrieve {@link Preference} references and initialize any data.
     */
    private void initPreferences() {
        mTitlePref = (EditTextPreference) getPref(R.string.pref_key_site_title);
        mTaglinePref = (EditTextPreference) getPref(R.string.pref_key_site_tagline);
        mAddressPref = (EditTextPreference) getPref(R.string.pref_key_site_address);
        mPrivacyPref = (DetailListPreference) getPref(R.string.pref_key_site_visibility);
        mLanguagePref = (DetailListPreference) getPref(R.string.pref_key_site_language);
        mUsernamePref = (EditTextPreference) getPref(R.string.pref_key_site_username);
        mPasswordPref = (EditTextPreference) getPref(R.string.pref_key_site_password);
        mLocationPref = (WPSwitchPreference) getPref(R.string.pref_key_site_location);
        mCategoryPref = (DetailListPreference) getPref(R.string.pref_key_site_category);
        mFormatPref = (DetailListPreference) getPref(R.string.pref_key_site_format);
        mAllowCommentsPref = (WPSwitchPreference) getPref(R.string.pref_key_site_allow_comments);
        mAllowCommentsNested = (WPSwitchPreference) getPref(R.string.pref_key_site_allow_comments_nested);
        mSendPingbacksPref = (WPSwitchPreference) getPref(R.string.pref_key_site_send_pingbacks);
        mSendPingbacksNested = (WPSwitchPreference) getPref(R.string.pref_key_site_send_pingbacks_nested);
        mReceivePingbacksPref = (WPSwitchPreference) getPref(R.string.pref_key_site_receive_pingbacks);
        mReceivePingbacksNested = (WPSwitchPreference) getPref(R.string.pref_key_site_receive_pingbacks_nested);
        mIdentityRequiredPreference = (WPSwitchPreference) getPref(R.string.pref_key_site_identity_required);
        mUserAccountRequiredPref = (WPSwitchPreference) getPref(R.string.pref_key_site_user_account_required);
        mCloseAfterPref = findPreference(getString(R.string.pref_key_site_close_after));
        mSortByPref = (DetailListPreference) getPref(R.string.pref_key_site_sort_by);
        mThreadingPref = (DetailListPreference) getPref(R.string.pref_key_site_threading);
        mPagingPref = findPreference(getString(R.string.pref_key_site_paging));
        mWhitelistPref = (DetailListPreference) getPref(R.string.pref_key_site_whitelist);
        mMultipleLinksPref = getPref(R.string.pref_key_site_multiple_links);
        mModerationHoldPref = getPref(R.string.pref_key_site_moderation_hold);
        mBlacklistPref = getPref(R.string.pref_key_site_blacklist);
        mRelatedPostsPref = findPreference(getString(R.string.pref_key_site_related_posts));
        mDeleteSitePref = findPreference(getString(R.string.pref_key_site_delete_site));
        mRelatedPostsPref.setOnPreferenceClickListener(this);
        mMultipleLinksPref.setOnPreferenceClickListener(this);
        mModerationHoldPref.setOnPreferenceClickListener(this);
        mBlacklistPref.setOnPreferenceClickListener(this);
        mDeleteSitePref.setOnPreferenceClickListener(this);
        mCloseAfterPref.setOnPreferenceClickListener(this);
        mPagingPref.setOnPreferenceClickListener(this);

        // .com sites hide the Account category, self-hosted sites hide the Related Posts preference
        if (mBlog.isDotcomFlag()) {
            removePreference(R.string.pref_key_site_screen, R.string.pref_key_site_account);
            removePreference(R.string.pref_key_site_screen, R.string.pref_key_site_delete_site);
        } else {
            removePreference(R.string.pref_key_site_general, R.string.pref_key_site_language);
            removePreference(R.string.pref_key_site_writing, R.string.pref_key_site_related_posts);
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

    private View getDialogTitleView(int title) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams")
        View titleView = inflater.inflate(R.layout.detail_list_preference_title, null);
        TextView titleText = ((TextView) titleView.findViewById(R.id.title));
        titleText.setText(title);
        titleText.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));
        return titleView;
    }

    private void showPagingDialog() {
        mSwitchChecked = mSiteSettings.getPagingCount() > 0;
        getNumberPickerDialog(true,
                mSwitchChecked,
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mSwitchChecked = isChecked;
                    }
                },
                R.string.site_settings_paging_title,
                R.string.site_settings_paging_dialog_description,
                R.string.site_settings_paging_dialog_header,
                1,
                getResources().getInteger(R.integer.paging_limit),
                mSiteSettings.getPagingCount(),
                R.string.site_settings_paging_title,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mSwitchChecked && mSiteSettings.getPagingCount() != mNumberPicker.getValue()) {
                            onPreferenceChange(mPagingPref, mNumberPicker.getValue());
                        } else {
                            onPreferenceChange(mPagingPref, 0);
                        }
                    }
                }, null);
    }

    private void showCloseAfterDialog() {
        mSwitchChecked = mSiteSettings.getCloseAfter() > 0;
        getNumberPickerDialog(true,
                mSwitchChecked,
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mSwitchChecked = isChecked;
                    }
                },
                R.string.site_settings_close_after_dialog_switch_text,
                R.string.site_settings_close_after_dialog_description,
                R.string.site_settings_close_after_dialog_header,
                1,
                getResources().getInteger(R.integer.close_after_limit),
                mSiteSettings.getCloseAfter(),
                R.string.site_settings_close_after_dialog_title,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mSwitchChecked && mSiteSettings.getCloseAfter() != mNumberPicker.getValue()) {
                            onPreferenceChange(mCloseAfterPref, mNumberPicker.getValue());
                        } else {
                            onPreferenceChange(mCloseAfterPref, 0);
                        }
                    }
                }, null);
        mNumberPicker.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return getResources().getQuantityString(R.plurals.days_quantity, value, value);
            }
        });
    }

    private void showMultipleLinksDialog() {
        getNumberPickerDialog(false,
                false,
                null,
                -1,
                R.string.site_settings_multiple_links_dialog_description,
                -1,
                0,
                getResources().getInteger(R.integer.max_links_limit),
                mSiteSettings.getMultipleLinks(),
                R.string.site_settings_multiple_links_title,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mSiteSettings.getMultipleLinks() != mNumberPicker.getValue()) {
                            onPreferenceChange(mMultipleLinksPref, mNumberPicker.getValue());
                        }
                    }
                }, null);
    }

    private void getNumberPickerDialog(boolean showSwitch,
                                       boolean switchChecked,
                                       final CompoundButton.OnCheckedChangeListener switchListener,
                                       int switchText,
                                       int detail,
                                       int header,
                                       int min,
                                       int max,
                                       int cur,
                                       int title,
                                       DialogInterface.OnClickListener positive,
                                       DialogInterface.OnClickListener negative) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Calypso_AlertDialog);
        View view = View.inflate(getActivity(), R.layout.number_picker_dialog, null);
        mNumberPicker = (NumberPicker) view.findViewById(R.id.number_picker);
        SwitchCompat pickerSwitch = (SwitchCompat) view.findViewById(R.id.number_picker_switch);
        if (showSwitch) {
            pickerSwitch.setVisibility(View.VISIBLE);
            pickerSwitch.setText(switchText);
            pickerSwitch.setChecked(switchChecked);
            final View toggleContainer = view.findViewById(R.id.number_picker_toggleable);
            toggleContainer.setEnabled(switchChecked);
            mNumberPicker.setEnabled(switchChecked);
            pickerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    toggleContainer.setEnabled(isChecked);
                    mNumberPicker.setEnabled(isChecked);

                    if (switchListener != null) {
                        switchListener.onCheckedChanged(buttonView, isChecked);
                    }
                }
            });
        } else {
            pickerSwitch.setVisibility(View.GONE);
        }
        TextView detailText = (TextView) view.findViewById(R.id.number_picker_text);
        TextView headerText = (TextView) view.findViewById(R.id.number_picker_header);
        if (header >= 0) {
            headerText.setText(header);
        } else {
            headerText.setVisibility(View.GONE);
        }
        if (detail >= 0) {
            detailText.setText(detail);
        } else {
            detailText.setVisibility(View.GONE);
        }
        mNumberPicker.setMinValue(min);
        mNumberPicker.setMaxValue(max);
        mNumberPicker.setValue(cur);
        builder.setCustomTitle(getDialogTitleView(title));
        builder.setPositiveButton(R.string.ok, positive);
        builder.setNegativeButton(R.string.cancel, negative);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();
        WPPrefUtils.layoutAsFlatButton(dialog.getButton(DialogInterface.BUTTON_POSITIVE));
        WPPrefUtils.layoutAsFlatButton(dialog.getButton(DialogInterface.BUTTON_NEGATIVE));
    }

    private void setPreferencesFromSiteSettings() {
        mLocationPref.setChecked(mSiteSettings.getLocation());
        changeEditTextPreferenceValue(mTitlePref, mSiteSettings.getTitle());
        changeEditTextPreferenceValue(mTaglinePref, mSiteSettings.getTagline());
        changeEditTextPreferenceValue(mAddressPref, mSiteSettings.getAddress());
        changeEditTextPreferenceValue(mUsernamePref, mSiteSettings.getUsername());
        changeEditTextPreferenceValue(mPasswordPref, mSiteSettings.getPassword());
        changeLanguageValue(mSiteSettings.getLanguageCode());
        setDetailListPreferenceValue(mPrivacyPref,
                String.valueOf(mSiteSettings.getPrivacy()),
                getPrivacySummary(mSiteSettings.getPrivacy()));
        setCategories();
        setPostFormats();
        setAllowComments(mSiteSettings.getAllowComments());
        setSendPingbacks(mSiteSettings.getSendPingbacks());
        setReceivePingbacks(mSiteSettings.getReceivePingbacks());
        setDetailListPreferenceValue(mSortByPref,
                String.valueOf(mSiteSettings.getCommentSorting()),
                getSortOrderSummary(mSiteSettings.getCommentSorting()));
        setDetailListPreferenceValue(mThreadingPref,
                String.valueOf(mSiteSettings.getThreadingLevels()),
                getThreadingSummary(mSiteSettings.getThreadingLevels()));
        int approval = mSiteSettings.getManualApproval() ?
                mSiteSettings.getUseCommentWhitelist() ? 0
                        : -1 : 1;
        setDetailListPreferenceValue(mWhitelistPref, String.valueOf(approval), getWhitelistSummary(approval));
        mMultipleLinksPref.setSummary(getResources()
                .getQuantityString(R.plurals.site_settings_multiple_links_summary,
                        mSiteSettings.getMultipleLinks(),
                        mSiteSettings.getMultipleLinks()));
        mIdentityRequiredPreference.setChecked(mSiteSettings.getIdentityRequired());
        mUserAccountRequiredPref.setChecked(mSiteSettings.getUserAccountRequired());
        mThreadingPref.setValue(String.valueOf(mSiteSettings.getThreadingLevels()));
        mCloseAfterPref.setSummary(getCloseAfterSummary(mSiteSettings.getCloseAfter()));
        mPagingPref.setSummary(getPagingSummary(mSiteSettings.getPagingCount()));
    }

    private void setCategories() {
        // Ignore if there are no changes
        if (mSiteSettings.isSameCategoryList(mCategoryPref.getEntryValues())) {
            mCategoryPref.setValue(String.valueOf(mSiteSettings.getDefaultCategory()));
            mCategoryPref.setSummary(mSiteSettings.getDefaultCategoryForDisplay());
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

        mCategoryPref.setEntries(entries);
        mCategoryPref.setEntryValues(values);
        mCategoryPref.setValue(String.valueOf(mSiteSettings.getDefaultCategory()));
        mCategoryPref.setSummary(mSiteSettings.getDefaultCategoryForDisplay());
    }

    private void setPostFormats() {
        // Ignore if there are no changes
        if (mSiteSettings.isSameFormatList(mFormatPref.getEntryValues())) {
            mFormatPref.setValue(String.valueOf(mSiteSettings.getDefaultFormat()));
            mFormatPref.setSummary(mSiteSettings.getDefaultPostFormatDisplay());
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

        mFormatPref.setEntries(entries);
        mFormatPref.setEntryValues(values);
        mFormatPref.setValue(String.valueOf(mSiteSettings.getDefaultFormat()));
        mFormatPref.setSummary(mSiteSettings.getDefaultPostFormatDisplay());
    }

    private void setAllowComments(boolean newValue) {
        mSiteSettings.setAllowComments(newValue);
        mAllowCommentsPref.setChecked(newValue);
        mAllowCommentsNested.setChecked(newValue);
    }

    private void setSendPingbacks(boolean newValue) {
        mSiteSettings.setSendPingbacks(newValue);
        mSendPingbacksPref.setChecked(newValue);
        mSendPingbacksNested.setChecked(newValue);
    }

    private void setReceivePingbacks(boolean newValue) {
        mSiteSettings.setReceivePingbacks(newValue);
        mReceivePingbacksPref.setChecked(newValue);
        mReceivePingbacksNested.setChecked(newValue);
    }

    private void setDetailListPreferenceValue(DetailListPreference pref, String value, String summary) {
        pref.setValue(value);
        pref.setSummary(summary);
        pref.refreshAdapter();
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
     *
     * @param newValue
     * languageCode
     */
    private void changeLanguageValue(String newValue) {
        if (mLanguagePref == null || newValue == null) return;

        if (TextUtils.isEmpty(mLanguagePref.getSummary()) ||
                !newValue.equals(mLanguagePref.getValue())) {
            mLanguagePref.setValue(newValue);
            String summary = getLanguageString(newValue, languageLocale(newValue));
            mLanguagePref.setSummary(summary);

            // update details to display in selected locale
            CharSequence[] languageCodes = mLanguagePref.getEntryValues();
            mLanguagePref.setEntries(createLanguageDisplayStrings(languageCodes));
            mLanguagePref.setDetails(createLanguageDetailDisplayStrings(languageCodes, newValue));
            mLanguagePref.refreshAdapter();
        }
    }

    private String getPrivacySummary(int privacy) {
        if (isAdded()) {
            switch (privacy) {
                case -1:
                    return getString(R.string.site_settings_privacy_private_summary);
                case 0:
                    return getString(R.string.site_settings_privacy_hidden_summary);
                case 1:
                    return getString(R.string.site_settings_privacy_public_summary);
            }
        }
        return "";
    }

    private String getCloseAfterSummary(int period) {
        if (!isAdded()) return "";
        if (period == 0) return getString(R.string.never);
        return getResources().getQuantityString(R.plurals.days_quantity, period, period);
    }

    private String getSortOrderSummary(int order) {
        if (!isAdded()) return "";
        switch (order) {
            case SiteSettingsInterface.ASCENDING_SORT:
                return getString(R.string.oldest_first);
            case SiteSettingsInterface.DESCENDING_SORT:
                return getString(R.string.newest_first);
            default:
                return getString(R.string.unknown);
        }
    }

    private String getThreadingSummary(int levels) {
        if (!isAdded()) return "";
        if (levels <= 1) return getString(R.string.none);
        return String.format(getString(R.string.site_settings_threading_summary), levels);
    }

    private String getPagingSummary(int count) {
        if (!isAdded()) return "";
        if (count == 0) return getString(R.string.none);
        return getResources().getQuantityString(R.plurals.site_settings_paging_summary, count, count);
    }

    private String getWhitelistSummary(int value) {
        if (isAdded()) {
            switch (value) {
                case -1:
                    return getString(R.string.site_settings_whitelist_none_summary);
                case 0:
                    return getString(R.string.site_settings_whitelist_known_summary);
                case 1:
                    return getString(R.string.site_settings_whitelist_all_summary);
            }
        }
        return "";
    }

    private void updateWhitelistSettings(int val) {
        switch (val) {
            case -1:
                mSiteSettings.setManualApproval(true);
                mSiteSettings.setUseCommentWhitelist(false);
                break;
            case 0:
                mSiteSettings.setManualApproval(true);
                mSiteSettings.setUseCommentWhitelist(true);
                break;
            case 1:
                mSiteSettings.setManualApproval(false);
                mSiteSettings.setUseCommentWhitelist(false);
                break;
        }
        setDetailListPreferenceValue(mWhitelistPref,
                String.valueOf(val),
                getWhitelistSummary(val));
    }

    private void showListEditorDialog(int titleRes, int footerRes) {
        Dialog dialog = new Dialog(getActivity(), R.style.Calypso_SiteSettingsTheme);
        dialog.setOnDismissListener(this);
        dialog.setContentView(getListEditorView(dialog, getString(footerRes)));
        dialog.show();
        WPActivityUtils.addToolbarToDialog(this, dialog, getString(titleRes));
    }

    private View getListEditorView(final Dialog dialog, String footerText) {
        Context themer = new ContextThemeWrapper(getActivity(), R.style.Calypso_SiteSettingsTheme);
        View view = View.inflate(themer, R.layout.list_editor, null);
        ((TextView) view.findViewById(R.id.list_editor_footer_text)).setText(footerText);

        final MultiSelectListView list = (MultiSelectListView) view.findViewById(android.R.id.list);
        list.setEnterMultiSelectListener(new MultiSelectListView.OnEnterMultiSelect() {
            @Override
            public void onEnterMultiSelect() {
                WPActivityUtils.setStatusBarColor(dialog.getWindow(), R.color.action_mode_status_bar_tint);
            }
        });
        list.setExitMultiSelectListener(new MultiSelectListView.OnExitMultiSelect() {
            @Override
            public void onExitMultiSelect() {
                WPActivityUtils.setStatusBarColor(dialog.getWindow(), R.color.status_bar_tint);
            }
        });
        list.setDeleteRequestListener(new MultiSelectListView.OnDeleteRequested() {
            @Override
            public boolean onDeleteRequested() {
                SparseBooleanArray checkedItems = list.getCheckedItemPositions();

                HashMap<String, Object> properties = new HashMap<>();
                properties.put("num_items_deleted", checkedItems.size());
                AnalyticsUtils.trackWithCurrentBlogDetails(
                        AnalyticsTracker.Stat.SITE_SETTINGS_DELETED_LIST_ITEMS, properties);

                ListAdapter adapter = list.getAdapter();
                List<String> itemsToRemove = new ArrayList<>();
                for (int i = 0; i < checkedItems.size(); i++) {
                    final int index = checkedItems.keyAt(i);
                    if (checkedItems.get(index)) {
                        itemsToRemove.add(adapter.getItem(index).toString());
                    }
                }
                mEditingList.removeAll(itemsToRemove);
                list.setAdapter(new ArrayAdapter<>(getActivity(),
                        R.layout.wp_simple_list_item_1,
                        mEditingList));
                mSiteSettings.saveSettings();
                return true;
            }
        });
        list.setEmptyView(view.findViewById(R.id.empty_view));
        list.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        list.setAdapter(new ArrayAdapter<>(getActivity(),
                R.layout.wp_simple_list_item_1,
                mEditingList));
        view.findViewById(R.id.fab_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder =
                        new AlertDialog.Builder(getActivity(), R.style.Calypso_AlertDialog);
                final EditText input = new EditText(getActivity());
                WPPrefUtils.layoutAsInput(input);
                input.setHint(R.string.site_settings_list_editor_input_hint);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String entry = input.getText().toString();
                        if (!mEditingList.contains(entry)) {
                            mEditingList.add(entry);
                            list.setAdapter(new ArrayAdapter<>(getActivity(),
                                    R.layout.wp_simple_list_item_1,
                                    mEditingList));
                            mSiteSettings.saveSettings();
                            AnalyticsUtils.trackWithCurrentBlogDetails(
                                    AnalyticsTracker.Stat.SITE_SETTINGS_ADDED_LIST_ITEM);
                        }
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                AlertDialog alertDialog = builder.create();
                int spacing = getResources().getDimensionPixelSize(R.dimen.dlp_padding_start);
                alertDialog.setView(input, spacing, spacing, spacing, 0);
                alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                alertDialog.show();
                alertDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                Button positive = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                Button negative = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                if (positive != null) WPPrefUtils.layoutAsFlatButton(positive);
                if (negative != null) WPPrefUtils.layoutAsFlatButton(negative);
                WPActivityUtils.showKeyboard(input);
            }
        });

        return view;
    }

    private void removeBlog() {
        if (WordPress.wpDB.deleteBlog(getActivity(), mBlog.getLocalTableBlogId())) {
            StatsTable.deleteStatsForBlog(getActivity(), mBlog.getLocalTableBlogId()); // Remove stats data
            AnalyticsUtils.refreshMetadata();
            ToastUtils.showToast(getActivity(), R.string.blog_removed_successfully);
            WordPress.wpDB.deleteLastBlogId();
            WordPress.currentBlog = null;
            getActivity().setResult(RESULT_BLOG_REMOVED);

            // If the last blog is removed and the user is not signed in wpcom, broadcast a UserSignedOut event
            if (!AccountHelper.isSignedIn()) {
                EventBus.getDefault().post(new CoreEvents.UserSignedOutCompletely());
            }

            // Checks for stats widgets that were synched with a blog that could be gone now.
            StatsWidgetProvider.updateWidgetsOnLogout(getActivity());

            getActivity().finish();
        } else {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            dialogBuilder.setTitle(getResources().getText(R.string.error));
            dialogBuilder.setMessage(getResources().getText(R.string.could_not_remove_account));
            dialogBuilder.setPositiveButton(R.string.ok, null);
            dialogBuilder.setCancelable(true);
            dialogBuilder.create().show();
        }
    }

    private void removeBlogWithConfirmation() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle(getResources().getText(R.string.remove_account));
        dialogBuilder.setMessage(getResources().getText(R.string.sure_to_remove_account));
        dialogBuilder.setPositiveButton(getResources().getText(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                removeBlog();
            }
        });
        dialogBuilder.setNegativeButton(getResources().getText(R.string.no), null);
        dialogBuilder.setCancelable(false);
        dialogBuilder.create().show();
    }

    /**
     * Generates display strings for given language codes. Used as entries in language preference.
     */
    private String[] createLanguageDisplayStrings(CharSequence[] languageCodes) {
        if (languageCodes == null || languageCodes.length < 1) return null;

        String[] displayStrings = new String[languageCodes.length];

        for (int i = 0; i < languageCodes.length; ++i) {
            displayStrings[i] = StringUtils.capitalize(getLanguageString(
                    String.valueOf(languageCodes[i]), languageLocale(languageCodes[i].toString())));
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
            detailStrings[i] = StringUtils.capitalize(
                    getLanguageString(languageCodes[i].toString(), languageLocale(locale)));
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
     * Gets a locale for the given language code.
     */
    private Locale languageLocale(String languageCode) {
        if (TextUtils.isEmpty(languageCode)) return Locale.getDefault();

        if (languageCode.length() > NO_REGION_LANG_CODE_LEN) {
            return new Locale(languageCode.substring(0, NO_REGION_LANG_CODE_LEN),
                    languageCode.substring(REGION_SUBSTRING_INDEX));
        }

        return new Locale(languageCode);
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
}
