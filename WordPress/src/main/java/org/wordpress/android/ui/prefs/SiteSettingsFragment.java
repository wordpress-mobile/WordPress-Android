package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextThemeWrapper;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.NumberPicker.Formatter;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.DeleteSiteError;
import org.wordpress.android.support.ZendeskHelper;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.accounts.HelpActivity.Origin;
import org.wordpress.android.ui.prefs.SiteSettingsFormatDialog.FormatType;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.ValidationUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.WPPrefUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import static org.wordpress.android.ui.prefs.WPComSiteSettings.supportsJetpackSpeedUpSettings;

/**
 * Allows interfacing with WordPress site settings. Works with WP.com and WP.org v4.5+ (pending).
 * <p>
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
     * When the user removes a site (by selecting Delete Site) the parent {@link Activity} result
     * is set to this value and {@link Activity#finish()} is invoked.
     */
    public static final int RESULT_BLOG_REMOVED = Activity.RESULT_FIRST_USER;

    /**
     * Provides the regex to identify domain HTTP(S) protocol and/or 'www' sub-domain.
     * <p>
     * Used to format user-facing {@link String}'s in certain preferences.
     */
    public static final String ADDRESS_FORMAT_REGEX = "^(https?://(w{3})?|www\\.)";

    /**
     * url that points to wordpress.com purchases
     */
    public static final String WORDPRESS_PURCHASES_URL = "https://wordpress.com/purchases";

    /**
     * url for redirecting free users to empty their sites (start over)
     */
    public static final String WORDPRESS_EMPTY_SITE_SUPPORT_URL = "https://en.support.wordpress.com/empty-site/";

    /**
     * Used to move the Uncategorized category to the beginning of the category list.
     */
    private static final int UNCATEGORIZED_CATEGORY_ID = 1;

    /**
     * Request code used when creating the {@link RelatedPostsDialog}.
     */
    private static final int RELATED_POSTS_REQUEST_CODE = 1;
    private static final int THREADING_REQUEST_CODE = 2;
    private static final int PAGING_REQUEST_CODE = 3;
    private static final int CLOSE_AFTER_REQUEST_CODE = 4;
    private static final int MULTIPLE_LINKS_REQUEST_CODE = 5;
    private static final int DELETE_SITE_REQUEST_CODE = 6;
    private static final int DATE_FORMAT_REQUEST_CODE = 7;
    private static final int TIME_FORMAT_REQUEST_CODE = 8;
    private static final int POSTS_PER_PAGE_REQUEST_CODE = 9;
    private static final int TIMEZONE_REQUEST_CODE = 10;

    private static final String DELETE_SITE_TAG = "delete-site";
    private static final String PURCHASE_ORIGINAL_RESPONSE_KEY = "originalResponse";
    private static final String PURCHASE_ACTIVE_KEY = "active";
    private static final String ANALYTICS_ERROR_PROPERTY_KEY = "error";

    private static final long FETCH_DELAY = 1000;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject Dispatcher mDispatcher;
    @Inject ZendeskHelper mZendeskHelper;

    public SiteModel mSite;

    // Can interface with WP.com or WP.org
    public SiteSettingsInterface mSiteSettings;

    // Reference to the list of items being edited in the current list editor
    private List<String> mEditingList;

    // Used to ensure that settings are only fetched once throughout the lifecycle of the fragment
    private boolean mShouldFetch;

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
    private DetailListPreference mCategoryPref;
    private DetailListPreference mFormatPref;
    private WPPreference mDateFormatPref;
    private WPPreference mTimeFormatPref;
    private DetailListPreference mWeekStartPref;
    private Preference mRelatedPostsPref;
    private Preference mTagsPref;
    private Preference mTimezonePref;
    private Preference mPostsPerPagePref;
    private WPSwitchPreference mAmpPref;

    // Media settings
    private EditTextPreference mSiteQuotaSpacePref;

    // Discussion settings preview
    private WPSwitchPreference mAllowCommentsPref;
    private WPSwitchPreference mSendPingbacksPref;
    private WPSwitchPreference mReceivePingbacksPref;

    // Discussion settings -> Defaults for New Posts
    private WPSwitchPreference mAllowCommentsNested;
    private WPSwitchPreference mSendPingbacksNested;
    private WPSwitchPreference mReceivePingbacksNested;
    private PreferenceScreen mMorePreference;

    // Discussion settings -> Comments
    private WPSwitchPreference mIdentityRequiredPreference;
    private WPSwitchPreference mUserAccountRequiredPref;
    private Preference mCloseAfterPref;
    private DetailListPreference mSortByPref;
    private Preference mThreadingPref;
    private Preference mPagingPref;
    private DetailListPreference mWhitelistPref;
    private Preference mMultipleLinksPref;
    private Preference mModerationHoldPref;
    private Preference mBlacklistPref;

    // Advanced settings
    private Preference mStartOverPref;
    private Preference mExportSitePref;
    private Preference mDeleteSitePref;

    // Jetpack settings
    private PreferenceScreen mJpSecuritySettings;
    private WPSwitchPreference mJpMonitorActivePref;
    private WPSwitchPreference mJpMonitorEmailNotesPref;
    private WPSwitchPreference mJpMonitorWpNotesPref;
    private WPSwitchPreference mJpBruteForcePref;
    private WPPreference mJpWhitelistPref;
    private WPSwitchPreference mJpSsoPref;
    private WPSwitchPreference mJpMatchEmailPref;
    private WPSwitchPreference mJpUseTwoFactorPref;

    // Speed up settings
    private PreferenceScreen mSpeedUpYourSiteSettings;
    private WPSwitchPreference mServeImagesFromOurServers;
    private WPSwitchPreference mLazyLoadImages;

    public boolean mEditingEnabled = true;

    // Reference to the state of the fragment
    private boolean mIsFragmentPaused = false;

    // Hold for Moderation and Blacklist settings
    private Dialog mDialog;
    private ActionMode mActionMode;
    private MultiSelectRecyclerViewAdapter mAdapter;

    // Delete site
    private ProgressDialog mDeleteSiteProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity activity = getActivity();
        ((WordPress) activity.getApplication()).component().inject(this);

        // make sure we have local site data and a network connection, otherwise finish activity
        if (!NetworkUtils.checkConnection(activity)) {
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

        // track successful settings screen access
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.SITE_SETTINGS_ACCESSED, mSite);

        // setup state to fetch remote settings
        mShouldFetch = true;

        // initialize the appropriate settings interface (WP.com or WP.org)
        mSiteSettings = SiteSettingsInterface.getInterface(activity, mSite, this);

        setRetainInstance(true);
        addPreferencesFromResource();

        // toggle which preferences are shown and set references
        initPreferences();
    }

    public void addPreferencesFromResource() {
        addPreferencesFromResource(R.xml.site_settings);

        // add Disconnect option for Jetpack sites when running a debug build
        if (shouldShowDisconnect()) {
            PreferenceCategory parent =
                    (PreferenceCategory) findPreference(getString(R.string.pref_key_site_discussion));
            Preference disconnectPref = new Preference(getActivity());
            disconnectPref.setTitle(getString(R.string.jetpack_disconnect_pref_title));
            disconnectPref.setKey(getString(R.string.pref_key_site_disconnect));
            disconnectPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    disconnectFromJetpack();
                    return true;
                }
            });
            parent.addPreference(disconnectPref);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Locally save the site. mSite can be null after site deletion or site removal (.org sites)
        if (mSite != null) {
            mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(mSite));
        }
        mIsFragmentPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Fragment#onResume() is called after FragmentActivity#onPostResume().
        // The latter is the most secure way of keeping track of the activity's state, and
        // avoid calls to commitAllowingStateLoss.
        mIsFragmentPaused = false;

        // always load cached settings
        mSiteSettings.init(false);

        if (mShouldFetch) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // initialize settings with locally cached values, fetch remote on first pass
                    mSiteSettings.init(true);
                }
            }, FETCH_DELAY);
            // stop future calls from fetching remote settings
            mShouldFetch = false;
        }
    }

    @Override
    public void onDestroyView() {
        removeMoreScreenToolbar();
        removeJetpackSecurityScreenToolbar();
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            switch (requestCode) {
                case RELATED_POSTS_REQUEST_CODE:
                    // data is null if user cancelled editing Related Posts settings
                    mSiteSettings.setShowRelatedPosts(data.getBooleanExtra(
                            RelatedPostsDialog.SHOW_RELATED_POSTS_KEY, false));
                    mSiteSettings.setShowRelatedPostHeader(data.getBooleanExtra(
                            RelatedPostsDialog.SHOW_HEADER_KEY, false));
                    mSiteSettings.setShowRelatedPostImages(data.getBooleanExtra(
                            RelatedPostsDialog.SHOW_IMAGES_KEY, false));
                    onPreferenceChange(mRelatedPostsPref, mSiteSettings.getRelatedPostsDescription());
                    break;
                case THREADING_REQUEST_CODE:
                    int levels = data.getIntExtra(NumberPickerDialog.CUR_VALUE_KEY, -1);
                    mSiteSettings.setShouldThreadComments(levels > 1 && data.getBooleanExtra(
                            NumberPickerDialog.SWITCH_ENABLED_KEY, false));
                    onPreferenceChange(mThreadingPref, levels);
                    break;
                case PAGING_REQUEST_CODE:
                    mSiteSettings.setShouldPageComments(data.getBooleanExtra(NumberPickerDialog.SWITCH_ENABLED_KEY,
                                                                             false));
                    onPreferenceChange(mPagingPref, data.getIntExtra(
                            NumberPickerDialog.CUR_VALUE_KEY, -1));
                    break;
                case CLOSE_AFTER_REQUEST_CODE:
                    mSiteSettings.setShouldCloseAfter(data.getBooleanExtra(NumberPickerDialog.SWITCH_ENABLED_KEY,
                                                                           false));
                    onPreferenceChange(mCloseAfterPref, data.getIntExtra(NumberPickerDialog.CUR_VALUE_KEY, -1));
                    break;
                case MULTIPLE_LINKS_REQUEST_CODE:
                    int numLinks = data.getIntExtra(NumberPickerDialog.CUR_VALUE_KEY, -1);
                    if (numLinks < 0 || numLinks == mSiteSettings.getMultipleLinks()) {
                        return;
                    }
                    onPreferenceChange(mMultipleLinksPref, numLinks);
                    break;
                case DATE_FORMAT_REQUEST_CODE:
                    String dateFormatValue = data.getStringExtra(SiteSettingsFormatDialog.KEY_FORMAT_VALUE);
                    setDateTimeFormatPref(FormatType.DATE_FORMAT, mDateFormatPref, dateFormatValue);
                    onPreferenceChange(mDateFormatPref, dateFormatValue);
                    break;
                case TIME_FORMAT_REQUEST_CODE:
                    String timeFormatValue = data.getStringExtra(SiteSettingsFormatDialog.KEY_FORMAT_VALUE);
                    setDateTimeFormatPref(FormatType.TIME_FORMAT, mTimeFormatPref, timeFormatValue);
                    onPreferenceChange(mTimeFormatPref, timeFormatValue);
                    break;
                case POSTS_PER_PAGE_REQUEST_CODE:
                    int numPosts = data.getIntExtra(NumberPickerDialog.CUR_VALUE_KEY, -1);
                    if (numPosts > -1) {
                        onPreferenceChange(mPostsPerPagePref, numPosts);
                    }
                    break;
                case TIMEZONE_REQUEST_CODE:
                    String timezone = data.getStringExtra(SiteSettingsTimezoneDialog.KEY_TIMEZONE);
                    mSiteSettings.setTimezone(timezone);
                    onPreferenceChange(mTimezonePref, timezone);
                    break;
            }
        } else {
            switch (requestCode) {
                case DELETE_SITE_REQUEST_CODE:
                    deleteSite();
                    break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        // use a wrapper to apply the Calypso theme

        if (getActivity().getActionBar() != null) {
            getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
            getActivity().getActionBar().setDisplayShowHomeEnabled(true);
        }

        Context themer = new ContextThemeWrapper(getActivity(), R.style.Calypso_SiteSettingsTheme);
        LayoutInflater localInflater = inflater.cloneInContext(themer);
        View view = super.onCreateView(localInflater, container, savedInstanceState);

        if (view != null) {
            setupPreferenceList((ListView) view.findViewById(android.R.id.list), getResources());
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        removeMoreScreenToolbar();
        removeJetpackSecurityScreenToolbar();
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
        setupMorePreferenceScreen();
        setupJetpackSecurityScreen();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            setupMorePreferenceScreen();
            setupJetpackSecurityScreen();
        }
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        if (child.getId() == android.R.id.title && child instanceof TextView) {
            // style preference category title views
            TextView title = (TextView) child;
            WPPrefUtils.layoutAsBody2(title);
        } else {
            // style preference title views
            TextView title = child.findViewById(android.R.id.title);
            if (title != null) {
                WPPrefUtils.layoutAsSubhead(title);
            }
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
        if (preference == mMorePreference) {
            // track user accessing the full Discussion settings screen
            AnalyticsUtils.trackWithSiteDetails(
                    AnalyticsTracker.Stat.SITE_SETTINGS_ACCESSED_MORE_SETTINGS, mSite);

            return setupMorePreferenceScreen();
        } else if (preference == mJpSecuritySettings) {
            setupJetpackSecurityScreen();
        } else if (preference == mSpeedUpYourSiteSettings) {
            setupSpeedUpScreen();
        } else if (preference == findPreference(getString(R.string.pref_key_site_start_over_screen))) {
            Dialog dialog = ((PreferenceScreen) preference).getDialog();
            if (mSite == null || dialog == null) {
                return false;
            }

            AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.SITE_SETTINGS_START_OVER_ACCESSED, mSite);

            if (mSite.getHasFreePlan()) {
                // Don't show the start over detail screen for free users, instead show the support page
                dialog.dismiss();
                WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(getActivity(), WORDPRESS_EMPTY_SITE_SUPPORT_URL);
            } else {
                setupPreferenceList((ListView) dialog.findViewById(android.R.id.list), getResources());
                String title = getString(R.string.start_over);
                WPActivityUtils.addToolbarToDialog(this, dialog, title);
            }
        } else if (preference == mDateFormatPref) {
            showDateOrTimeFormatDialog(FormatType.DATE_FORMAT);
        } else if (preference == mTimeFormatPref) {
            showDateOrTimeFormatDialog(FormatType.TIME_FORMAT);
        } else if (preference == mPostsPerPagePref) {
            showPostsPerPageDialog();
        } else if (preference == mTimezonePref) {
            showTimezoneDialog();
        }

        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mRelatedPostsPref) {
            showRelatedPostsDialog();
        } else if (preference == mMultipleLinksPref) {
            showMultipleLinksDialog();
        } else if (preference == mModerationHoldPref) {
            mEditingList = mSiteSettings.getModerationKeys();
            showListEditorDialog(R.string.site_settings_moderation_hold_title,
                                 R.string.site_settings_hold_for_moderation_description);
        } else if (preference == mBlacklistPref) {
            mEditingList = mSiteSettings.getBlacklistKeys();
            showListEditorDialog(R.string.site_settings_blacklist_title,
                                 R.string.site_settings_blacklist_description);
        } else if (preference == mJpWhitelistPref) {
            mEditingList = mSiteSettings.getJetpackWhitelistKeys();
            showListEditorDialog(R.string.jetpack_brute_force_whitelist_title,
                                 R.string.site_settings_jetpack_whitelist_description);
        } else if (preference == mStartOverPref) {
            handleStartOver();
        } else if (preference == mCloseAfterPref) {
            showCloseAfterDialog();
        } else if (preference == mPagingPref) {
            showPagingDialog();
        } else if (preference == mThreadingPref) {
            showThreadingDialog();
        } else if (preference == mCategoryPref || preference == mFormatPref) {
            return !shouldShowListPreference((DetailListPreference) preference);
        } else if (preference == mExportSitePref) {
            showExportContentDialog();
        } else if (preference == mDeleteSitePref) {
            AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.SITE_SETTINGS_DELETE_SITE_ACCESSED, mSite);
            requestPurchasesForDeletionCheck();
        } else if (preference == mTagsPref) {
            SiteSettingsTagListActivity.showTagList(getActivity(), mSite);
        } else {
            return false;
        }

        return true;
    }

    private void disconnectFromJetpack() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(getActivity(), R.style.Calypso_Dialog_Alert));
        builder.setMessage(R.string.jetpack_disconnect_confirmation_message);
        builder.setPositiveButton(R.string.jetpack_disconnect_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String url = String.format(Locale.US, "jetpack-blogs/%d/mine/delete", mSite.getSiteId());
                WordPress.getRestClientUtilsV1_1().post(url, new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.v(AppLog.T.API, "Successfully disconnected Jetpack site");
                        ToastUtils.showToast(getActivity(), R.string.jetpack_disconnect_success_toast);
                        mDispatcher.dispatch(SiteActionBuilder.newRemoveSiteAction(mSite));
                        mSite = null;
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(AppLog.T.API, "Error disconnecting Jetpack site");
                        ToastUtils.showToast(getActivity(), R.string.jetpack_disconnect_error_toast);
                    }
                });
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue == null || !mEditingEnabled) {
            return false;
        }

        if (preference == mJpWhitelistPref) {
            mJpWhitelistPref.setSummary(mSiteSettings.getJetpackProtectWhitelistSummary());
        } else if (preference == mJpMonitorActivePref) {
            mJpMonitorActivePref.setChecked((Boolean) newValue);
            mSiteSettings.enableJetpackMonitor((Boolean) newValue);
        } else if (preference == mJpMonitorEmailNotesPref) {
            mJpMonitorEmailNotesPref.setChecked((Boolean) newValue);
            mSiteSettings.enableJetpackMonitorEmailNotifications((Boolean) newValue);
        } else if (preference == mJpMonitorWpNotesPref) {
            mJpMonitorWpNotesPref.setChecked((Boolean) newValue);
            mSiteSettings.enableJetpackMonitorWpNotifications((Boolean) newValue);
        } else if (preference == mJpBruteForcePref) {
            mJpBruteForcePref.setChecked((Boolean) newValue);
            mSiteSettings.enableJetpackProtect((Boolean) newValue);
        } else if (preference == mJpSsoPref) {
            mJpSsoPref.setChecked((Boolean) newValue);
            mSiteSettings.enableJetpackSso((Boolean) newValue);
        } else if (preference == mJpMatchEmailPref) {
            mJpMatchEmailPref.setChecked((Boolean) newValue);
            mSiteSettings.enableJetpackSsoMatchEmail((Boolean) newValue);
        } else if (preference == mJpUseTwoFactorPref) {
            mJpUseTwoFactorPref.setChecked((Boolean) newValue);
            mSiteSettings.enableJetpackSsoTwoFactor((Boolean) newValue);
        } else if (preference == mServeImagesFromOurServers) {
            mServeImagesFromOurServers.setChecked((Boolean) newValue);
            mSiteSettings.enableServeImagesFromOurServers((Boolean) newValue);
        } else if (preference == mLazyLoadImages) {
            mLazyLoadImages.setChecked((Boolean) newValue);
            mSiteSettings.enableLazyLoadImages((Boolean) newValue);
        } else if (preference == mTitlePref) {
            mSiteSettings.setTitle(newValue.toString());
            changeEditTextPreferenceValue(mTitlePref, mSiteSettings.getTitle());
        } else if (preference == mTaglinePref) {
            mSiteSettings.setTagline(newValue.toString());
            changeEditTextPreferenceValue(mTaglinePref, mSiteSettings.getTagline());
        } else if (preference == mAddressPref) {
            mSiteSettings.setAddress(newValue.toString());
            changeEditTextPreferenceValue(mAddressPref, mSiteSettings.getAddress());
        } else if (preference == mLanguagePref) {
            if (!mSiteSettings.setLanguageCode(newValue.toString())) {
                AppLog.w(AppLog.T.SETTINGS,
                         "Unknown language code " + newValue.toString() + " selected in Site Settings.");
                ToastUtils.showToast(getActivity(), R.string.site_settings_unknown_language_code_error);
            }
            changeLanguageValue(mSiteSettings.getLanguageCode());
        } else if (preference == mPrivacyPref) {
            mSiteSettings.setPrivacy(Integer.parseInt(newValue.toString()));
            setDetailListPreferenceValue(mPrivacyPref,
                                         String.valueOf(mSiteSettings.getPrivacy()),
                                         mSiteSettings.getPrivacyDescription());
        } else if (preference == mAllowCommentsPref || preference == mAllowCommentsNested) {
            setAllowComments((Boolean) newValue);
        } else if (preference == mSendPingbacksPref || preference == mSendPingbacksNested) {
            setSendPingbacks((Boolean) newValue);
        } else if (preference == mReceivePingbacksPref || preference == mReceivePingbacksNested) {
            setReceivePingbacks((Boolean) newValue);
        } else if (preference == mCloseAfterPref) {
            mSiteSettings.setCloseAfter(Integer.parseInt(newValue.toString()));
            mCloseAfterPref.setSummary(mSiteSettings.getCloseAfterDescription());
        } else if (preference == mSortByPref) {
            mSiteSettings.setCommentSorting(Integer.parseInt(newValue.toString()));
            setDetailListPreferenceValue(mSortByPref,
                                         newValue.toString(),
                                         mSiteSettings.getSortingDescription());
        } else if (preference == mThreadingPref) {
            mSiteSettings.setThreadingLevels(Integer.parseInt(newValue.toString()));
            mThreadingPref.setSummary(mSiteSettings.getThreadingDescription());
        } else if (preference == mPagingPref) {
            mSiteSettings.setPagingCount(Integer.parseInt(newValue.toString()));
            mPagingPref.setSummary(mSiteSettings.getPagingDescription());
        } else if (preference == mIdentityRequiredPreference) {
            mSiteSettings.setIdentityRequired((Boolean) newValue);
        } else if (preference == mUserAccountRequiredPref) {
            mSiteSettings.setUserAccountRequired((Boolean) newValue);
        } else if (preference == mWhitelistPref) {
            updateWhitelistSettings(Integer.parseInt(newValue.toString()));
        } else if (preference == mMultipleLinksPref) {
            mSiteSettings.setMultipleLinks(Integer.parseInt(newValue.toString()));
            String s = StringUtils.getQuantityString(getActivity(), R.string.site_settings_multiple_links_summary_zero,
                                                     R.string.site_settings_multiple_links_summary_one,
                                                     R.string.site_settings_multiple_links_summary_other,
                                                     mSiteSettings.getMultipleLinks());
            mMultipleLinksPref.setSummary(s);
        } else if (preference == mUsernamePref) {
            mSiteSettings.setUsername(newValue.toString());
            changeEditTextPreferenceValue(mUsernamePref, mSiteSettings.getUsername());
        } else if (preference == mPasswordPref) {
            mSiteSettings.setPassword(newValue.toString());
            changeEditTextPreferenceValue(mPasswordPref, mSiteSettings.getPassword());
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
        } else if (preference == mRelatedPostsPref) {
            mRelatedPostsPref.setSummary(newValue.toString());
        } else if (preference == mModerationHoldPref) {
            mModerationHoldPref.setSummary(mSiteSettings.getModerationHoldDescription());
        } else if (preference == mBlacklistPref) {
            mBlacklistPref.setSummary(mSiteSettings.getBlacklistDescription());
        } else if (preference == mWeekStartPref) {
            mSiteSettings.setStartOfWeek(newValue.toString());
            mWeekStartPref.setValue(newValue.toString());
            mWeekStartPref.setSummary(mWeekStartPref.getEntry());
        } else if (preference == mDateFormatPref) {
            mSiteSettings.setDateFormat(newValue.toString());
        } else if (preference == mTimeFormatPref) {
            mSiteSettings.setTimeFormat(newValue.toString());
        } else if (preference == mPostsPerPagePref) {
            mPostsPerPagePref.setSummary(newValue.toString());
            mSiteSettings.setPostsPerPage(Integer.parseInt(newValue.toString()));
        } else if (preference == mAmpPref) {
            mSiteSettings.setAmpEnabled((Boolean) newValue);
        } else if (preference == mTimezonePref) {
            setTimezonePref(newValue.toString());
            mSiteSettings.setTimezone(newValue.toString());
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
                    AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.SITE_SETTINGS_HINT_TOAST_SHOWN, mSite,
                                                        properties);
                    ToastUtils.showToast(getActivity(), hintObj.getHint(), ToastUtils.Duration.SHORT);
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mEditingList == mSiteSettings.getModerationKeys()) {
            onPreferenceChange(mModerationHoldPref, mEditingList.size());
        } else if (mEditingList == mSiteSettings.getBlacklistKeys()) {
            onPreferenceChange(mBlacklistPref, mEditingList.size());
        } else if (mEditingList == mSiteSettings.getJetpackWhitelistKeys()) {
            onPreferenceChange(mJpWhitelistPref, mEditingList.size());
        }
        mEditingList = null;
    }

    @Override
    public void onSaveError(Exception error) {
        if (!isAdded()) {
            return;
        }
        ToastUtils.showToast(getActivity(), R.string.error_post_remote_site_settings);
        getActivity().finish();
    }

    @Override
    public void onFetchError(Exception error) {
        if (!isAdded()) {
            return;
        }
        ToastUtils.showToast(getActivity(), R.string.error_fetch_remote_site_settings);
        getActivity().finish();
    }

    @Override
    public void onSettingsUpdated() {
        if (isAdded()) {
            setPreferencesFromSiteSettings();
        }
    }

    @Override
    public void onSettingsSaved() {
        mSite.setName(mSiteSettings.getTitle());
        // Locally save the site
        mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(mSite));
    }

    @Override
    public void onCredentialsValidated(Exception error) {
        if (!isAdded()) {
            return;
        }
        if (error != null) {
            ToastUtils.showToast(WordPress.getContext(), R.string.username_or_password_incorrect);
        }
    }

    private void setupPreferenceList(ListView prefList, Resources res) {
        if (prefList == null || res == null) {
            return;
        }

        // customize list dividers
        //noinspection deprecation
        prefList.setDivider(res.getDrawable(R.drawable.preferences_divider));
        prefList.setDividerHeight(res.getDimensionPixelSize(R.dimen.site_settings_divider_height));
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
    public void initPreferences() {
        mTitlePref = (EditTextPreference) getChangePref(R.string.pref_key_site_title);
        mTaglinePref = (EditTextPreference) getChangePref(R.string.pref_key_site_tagline);
        mAddressPref = (EditTextPreference) getChangePref(R.string.pref_key_site_address);
        mPrivacyPref = (DetailListPreference) getChangePref(R.string.pref_key_site_visibility);
        mLanguagePref = (DetailListPreference) getChangePref(R.string.pref_key_site_language);
        mUsernamePref = (EditTextPreference) getChangePref(R.string.pref_key_site_username);
        mPasswordPref = (EditTextPreference) getChangePref(R.string.pref_key_site_password);
        mCategoryPref = (DetailListPreference) getChangePref(R.string.pref_key_site_category);
        mTagsPref = getClickPref(R.string.pref_key_site_tags);
        mFormatPref = (DetailListPreference) getChangePref(R.string.pref_key_site_format);
        mAllowCommentsPref = (WPSwitchPreference) getChangePref(R.string.pref_key_site_allow_comments);
        mAllowCommentsNested = (WPSwitchPreference) getChangePref(R.string.pref_key_site_allow_comments_nested);
        mSendPingbacksPref = (WPSwitchPreference) getChangePref(R.string.pref_key_site_send_pingbacks);
        mSendPingbacksNested = (WPSwitchPreference) getChangePref(R.string.pref_key_site_send_pingbacks_nested);
        mReceivePingbacksPref = (WPSwitchPreference) getChangePref(R.string.pref_key_site_receive_pingbacks);
        mReceivePingbacksNested = (WPSwitchPreference) getChangePref(R.string.pref_key_site_receive_pingbacks_nested);
        mIdentityRequiredPreference = (WPSwitchPreference) getChangePref(R.string.pref_key_site_identity_required);
        mUserAccountRequiredPref = (WPSwitchPreference) getChangePref(R.string.pref_key_site_user_account_required);
        mSortByPref = (DetailListPreference) getChangePref(R.string.pref_key_site_sort_by);
        mWhitelistPref = (DetailListPreference) getChangePref(R.string.pref_key_site_whitelist);
        mMorePreference = (PreferenceScreen) getClickPref(R.string.pref_key_site_more_discussion);
        mRelatedPostsPref = getClickPref(R.string.pref_key_site_related_posts);
        mCloseAfterPref = getClickPref(R.string.pref_key_site_close_after);
        mPagingPref = getClickPref(R.string.pref_key_site_paging);
        mThreadingPref = getClickPref(R.string.pref_key_site_threading);
        mMultipleLinksPref = getClickPref(R.string.pref_key_site_multiple_links);
        mModerationHoldPref = getClickPref(R.string.pref_key_site_moderation_hold);
        mBlacklistPref = getClickPref(R.string.pref_key_site_blacklist);
        mStartOverPref = getClickPref(R.string.pref_key_site_start_over);
        mExportSitePref = getClickPref(R.string.pref_key_site_export_site);
        mDeleteSitePref = getClickPref(R.string.pref_key_site_delete_site);
        mJpSecuritySettings = (PreferenceScreen) getClickPref(R.string.pref_key_jetpack_security_screen);
        mJpMonitorActivePref = (WPSwitchPreference) getChangePref(R.string.pref_key_jetpack_monitor_uptime);
        mJpMonitorEmailNotesPref =
                (WPSwitchPreference) getChangePref(R.string.pref_key_jetpack_send_email_notifications);
        mJpMonitorWpNotesPref = (WPSwitchPreference) getChangePref(R.string.pref_key_jetpack_send_wp_notifications);
        mJpSsoPref = (WPSwitchPreference) getChangePref(R.string.pref_key_jetpack_allow_wpcom_sign_in);
        mJpBruteForcePref = (WPSwitchPreference) getChangePref(R.string.pref_key_jetpack_prevent_brute_force);
        mJpMatchEmailPref = (WPSwitchPreference) getChangePref(R.string.pref_key_jetpack_match_via_email);
        mJpUseTwoFactorPref = (WPSwitchPreference) getChangePref(R.string.pref_key_jetpack_require_two_factor);
        mJpWhitelistPref = (WPPreference) getClickPref(R.string.pref_key_jetpack_brute_force_whitelist);
        mWeekStartPref = (DetailListPreference) getChangePref(R.string.pref_key_site_week_start);
        mDateFormatPref = (WPPreference) getChangePref(R.string.pref_key_site_date_format);
        mTimeFormatPref = (WPPreference) getChangePref(R.string.pref_key_site_time_format);
        mPostsPerPagePref = getClickPref(R.string.pref_key_site_posts_per_page);
        mTimezonePref = getClickPref(R.string.pref_key_site_timezone);
        mAmpPref = (WPSwitchPreference) getChangePref(R.string.pref_key_site_amp);
        mSpeedUpYourSiteSettings = (PreferenceScreen) getClickPref(R.string.pref_key_speed_up_your_site_screen);
        mServeImagesFromOurServers =
                (WPSwitchPreference) getChangePref(R.string.pref_key_serve_images_from_our_servers);
        mLazyLoadImages = (WPSwitchPreference) getChangePref(R.string.pref_key_lazy_load_images);
        mSiteQuotaSpacePref = (EditTextPreference) getChangePref(R.string.pref_key_site_quota_space);
        sortLanguages();

        boolean isAccessedViaWPComRest = SiteUtils.isAccessedViaWPComRest(mSite);

        // .com sites hide the Account category, self-hosted sites hide the Related Posts preference
        if (!isAccessedViaWPComRest) {
            // self-hosted, non-jetpack site
            removeNonSelfHostedPreferences();
        } else if (mSite.isJetpackConnected()) {
            // jetpack site
            removeNonJetpackPreferences();
        } else {
            // wp.com site
            removeNonWPComPreferences();
        }

        // hide Admin options depending of capabilities on this site
        if ((!isAccessedViaWPComRest && !mSite.isSelfHostedAdmin())
            || (isAccessedViaWPComRest && !mSite.getHasCapabilityManageOptions())) {
            hideAdminRequiredPreferences();
        }

        // hide speed-up jetpack settings if plugin version < 5.8
        if (!supportsJetpackSpeedUpSettings(mSite)) {
            removeSpeedUpJetpackPreferences();
        }
    }

    public void setEditingEnabled(boolean enabled) {
        // excludes mAddressPref, mMorePreference, mJpSecuritySettings
        final Preference[] editablePreference = {
                mTitlePref, mTaglinePref, mPrivacyPref, mLanguagePref, mUsernamePref,
                mPasswordPref, mCategoryPref, mTagsPref, mFormatPref, mAllowCommentsPref,
                mAllowCommentsNested, mSendPingbacksPref, mSendPingbacksNested, mReceivePingbacksPref,
                mReceivePingbacksNested, mIdentityRequiredPreference, mUserAccountRequiredPref,
                mSortByPref, mWhitelistPref, mRelatedPostsPref, mCloseAfterPref, mPagingPref,
                mThreadingPref, mMultipleLinksPref, mModerationHoldPref, mBlacklistPref, mWeekStartPref,
                mDateFormatPref, mTimeFormatPref, mTimezonePref, mPostsPerPagePref, mAmpPref,
                mDeleteSitePref, mJpMonitorActivePref, mJpMonitorEmailNotesPref, mJpSsoPref,
                mJpMonitorWpNotesPref, mJpBruteForcePref, mJpWhitelistPref, mJpMatchEmailPref, mJpUseTwoFactorPref
        };

        for (Preference preference : editablePreference) {
            if (preference != null) {
                preference.setEnabled(enabled);
            }
        }

        mEditingEnabled = enabled;
    }

    private void showPostsPerPageDialog() {
        Bundle args = new Bundle();
        args.putBoolean(NumberPickerDialog.SHOW_SWITCH_KEY, false);
        args.putString(NumberPickerDialog.TITLE_KEY, getString(R.string.site_settings_posts_per_page_title));
        args.putInt(NumberPickerDialog.MIN_VALUE_KEY, 1);
        args.putInt(NumberPickerDialog.MAX_VALUE_KEY, getResources().getInteger(R.integer.posts_per_page_limit));
        args.putInt(NumberPickerDialog.CUR_VALUE_KEY, mSiteSettings.getPostsPerPage());
        showNumberPickerDialog(args, POSTS_PER_PAGE_REQUEST_CODE, "posts-per-page-dialog");
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

    private void showNumberPickerDialog(Bundle args, int requestCode, String tag) {
        showNumberPickerDialog(args, requestCode, tag, null);
    }

    private void showNumberPickerDialog(Bundle args, int requestCode, String tag, Formatter format) {
        NumberPickerDialog dialog = new NumberPickerDialog();
        dialog.setNumberFormat(format);
        dialog.setArguments(args);
        dialog.setTargetFragment(this, requestCode);
        dialog.show(getFragmentManager(), tag);
    }

    private void showPagingDialog() {
        Bundle args = new Bundle();
        args.putBoolean(NumberPickerDialog.SHOW_SWITCH_KEY, true);
        args.putBoolean(NumberPickerDialog.SWITCH_ENABLED_KEY, mSiteSettings.getShouldPageComments());
        args.putString(NumberPickerDialog.SWITCH_TITLE_KEY, getString(R.string.site_settings_paging_title));
        args.putString(NumberPickerDialog.SWITCH_DESC_KEY, getString(R.string.site_settings_paging_dialog_description));
        args.putString(NumberPickerDialog.TITLE_KEY, getString(R.string.site_settings_paging_title));
        args.putString(NumberPickerDialog.HEADER_TEXT_KEY, getString(R.string.site_settings_paging_dialog_header));
        args.putInt(NumberPickerDialog.MIN_VALUE_KEY, 1);
        args.putInt(NumberPickerDialog.MAX_VALUE_KEY, getResources().getInteger(R.integer.paging_limit));
        args.putInt(NumberPickerDialog.CUR_VALUE_KEY, mSiteSettings.getPagingCount());
        showNumberPickerDialog(args, PAGING_REQUEST_CODE, "paging-dialog");
    }

    private void showThreadingDialog() {
        Bundle args = new Bundle();
        args.putBoolean(NumberPickerDialog.SHOW_SWITCH_KEY, true);
        args.putBoolean(NumberPickerDialog.SWITCH_ENABLED_KEY, mSiteSettings.getShouldThreadComments());
        args.putString(NumberPickerDialog.SWITCH_TITLE_KEY, getString(R.string.site_settings_threading_title));
        args.putString(NumberPickerDialog.SWITCH_DESC_KEY,
                       getString(R.string.site_settings_threading_dialog_description));
        args.putString(NumberPickerDialog.TITLE_KEY, getString(R.string.site_settings_threading_title));
        args.putString(NumberPickerDialog.HEADER_TEXT_KEY, getString(R.string.site_settings_threading_dialog_header));
        args.putInt(NumberPickerDialog.MIN_VALUE_KEY, 2);
        args.putInt(NumberPickerDialog.MAX_VALUE_KEY, getResources().getInteger(R.integer.threading_limit));
        args.putInt(NumberPickerDialog.CUR_VALUE_KEY, mSiteSettings.getThreadingLevels());
        showNumberPickerDialog(args, THREADING_REQUEST_CODE, "threading-dialog", new Formatter() {
            @Override
            public String format(int value) {
                return mSiteSettings.getThreadingDescriptionForLevel(value);
            }
        });
    }

    private void showExportContentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(getActivity(), R.style.Calypso_Dialog_Alert));
        builder.setTitle(R.string.export_your_content);
        String email = mAccountStore.getAccount().getEmail();
        builder.setMessage(getString(R.string.export_your_content_message, email));
        builder.setPositiveButton(R.string.site_settings_export_content_title, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.SITE_SETTINGS_EXPORT_SITE_REQUESTED, mSite);
                exportSite();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);

        builder.show();
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.SITE_SETTINGS_EXPORT_SITE_ACCESSED, mSite);
    }

    private void showDateOrTimeFormatDialog(@NonNull FormatType formatType) {
        String formatString =
                formatType == FormatType.DATE_FORMAT ? mSiteSettings.getDateFormat() : mSiteSettings.getTimeFormat();
        SiteSettingsFormatDialog dialog = SiteSettingsFormatDialog.newInstance(formatType, formatString);
        int requestCode = formatType == FormatType.DATE_FORMAT ? DATE_FORMAT_REQUEST_CODE : TIME_FORMAT_REQUEST_CODE;
        dialog.setTargetFragment(this, requestCode);
        dialog.show(getFragmentManager(), "format-dialog-tag");
    }

    private void showTimezoneDialog() {
        SiteSettingsTimezoneDialog dialog = SiteSettingsTimezoneDialog.newInstance(mSiteSettings.getTimezone());
        dialog.setTargetFragment(this, TIMEZONE_REQUEST_CODE);
        dialog.show(getFragmentManager(), "timezone-dialog-tag");
    }

    private void dismissProgressDialog(ProgressDialog progressDialog) {
        if (progressDialog != null && progressDialog.isShowing()) {
            try {
                progressDialog.dismiss();
            } catch (IllegalArgumentException e) {
                // dialog doesn't exist
            }
        }
    }

    private void requestPurchasesForDeletionCheck() {
        final ProgressDialog progressDialog =
                ProgressDialog.show(getActivity(), "", getString(R.string.checking_purchases), true, false);
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.SITE_SETTINGS_DELETE_SITE_PURCHASES_REQUESTED, mSite);
        WordPress.getRestClientUtils().getSitePurchases(mSite.getSiteId(), new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                dismissProgressDialog(progressDialog);
                if (isAdded()) {
                    showPurchasesOrDeleteSiteDialog(response);
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgressDialog(progressDialog);
                if (isAdded()) {
                    ToastUtils.showToast(getActivity(), getString(R.string.purchases_request_error));
                    AppLog.e(AppLog.T.API,
                             "Error occurred while requesting purchases for deletion check: " + error.toString());
                }
            }
        });
    }

    private void showPurchasesOrDeleteSiteDialog(JSONObject response) {
        try {
            JSONArray purchases = response.getJSONArray(PURCHASE_ORIGINAL_RESPONSE_KEY);
            if (hasActivePurchases(purchases)) {
                showPurchasesDialog();
            } else {
                showDeleteSiteWarningDialog();
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.API, "Error occurred while trying to delete site: " + e.toString());
        }
    }

    private void showPurchasesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(getActivity(), R.style.Calypso_Dialog_Alert));
        builder.setTitle(R.string.premium_upgrades_title);
        builder.setMessage(R.string.premium_upgrades_message);
        builder.setPositiveButton(R.string.show_purchases, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AnalyticsUtils.trackWithSiteDetails(
                        AnalyticsTracker.Stat.SITE_SETTINGS_DELETE_SITE_PURCHASES_SHOW_CLICKED, mSite);
                WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(getActivity(), WORDPRESS_PURCHASES_URL);
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.SITE_SETTINGS_DELETE_SITE_PURCHASES_SHOWN, mSite);
    }

    private boolean hasActivePurchases(JSONArray purchases) throws JSONException {
        for (int i = 0; i < purchases.length(); i++) {
            JSONObject purchase = purchases.getJSONObject(i);
            int active = purchase.getInt(PURCHASE_ACTIVE_KEY);

            if (active == 1) {
                return true;
            }
        }

        return false;
    }

    private void showDeleteSiteWarningDialog() {
        if (!isAdded() || mIsFragmentPaused) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(getActivity(), R.style.Calypso_Dialog_Alert));
        builder.setTitle(R.string.delete_site_warning_title);
        String text = getString(R.string.delete_site_warning, "<b>" + UrlUtils.getHost(mSite.getUrl()) + "</b>")
                      + "<br><br>"
                      + "<i>" + getString(R.string.delete_site_warning_subtitle) + "</i>";
        builder.setMessage(HtmlUtils.fromHtml(text));
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showDeleteSiteDialog();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showDeleteSiteDialog() {
        if (mIsFragmentPaused) {
            return; // Do not show the DeleteSiteDialogFragment if the fragment was paused.
        }
        // DialogFragment internally uses commit(), and not commitAllowingStateLoss, crashing the app in case like that.
        Bundle args = new Bundle();
        args.putString(DeleteSiteDialogFragment.SITE_DOMAIN_KEY, UrlUtils.getHost(mSite.getUrl()));
        DeleteSiteDialogFragment deleteSiteDialogFragment = new DeleteSiteDialogFragment();
        deleteSiteDialogFragment.setArguments(args);
        deleteSiteDialogFragment.setTargetFragment(this, DELETE_SITE_REQUEST_CODE);
        deleteSiteDialogFragment.show(getFragmentManager(), DELETE_SITE_TAG);
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.SITE_SETTINGS_DELETE_SITE_ACCESSED, mSite);
    }

    private void showCloseAfterDialog() {
        Bundle args = new Bundle();
        args.putBoolean(NumberPickerDialog.SHOW_SWITCH_KEY, true);
        args.putBoolean(NumberPickerDialog.SWITCH_ENABLED_KEY, mSiteSettings.getShouldCloseAfter());
        args.putString(NumberPickerDialog.SWITCH_TITLE_KEY,
                       getString(R.string.site_settings_close_after_dialog_switch_text));
        args.putString(NumberPickerDialog.SWITCH_DESC_KEY,
                       getString(R.string.site_settings_close_after_dialog_description));
        args.putString(NumberPickerDialog.TITLE_KEY, getString(R.string.site_settings_close_after_dialog_title));
        args.putString(NumberPickerDialog.HEADER_TEXT_KEY, getString(R.string.site_settings_close_after_dialog_header));
        args.putInt(NumberPickerDialog.MIN_VALUE_KEY, 1);
        args.putInt(NumberPickerDialog.MAX_VALUE_KEY, getResources().getInteger(R.integer.close_after_limit));
        args.putInt(NumberPickerDialog.CUR_VALUE_KEY, mSiteSettings.getCloseAfter());
        showNumberPickerDialog(args, CLOSE_AFTER_REQUEST_CODE, "close-after-dialog");
    }

    private void showMultipleLinksDialog() {
        Bundle args = new Bundle();
        args.putBoolean(NumberPickerDialog.SHOW_SWITCH_KEY, false);
        args.putString(NumberPickerDialog.TITLE_KEY, getString(R.string.site_settings_multiple_links_title));
        args.putInt(NumberPickerDialog.MIN_VALUE_KEY, 0);
        args.putInt(NumberPickerDialog.MAX_VALUE_KEY, getResources().getInteger(R.integer.max_links_limit));
        args.putInt(NumberPickerDialog.CUR_VALUE_KEY, mSiteSettings.getMultipleLinks());
        showNumberPickerDialog(args, MULTIPLE_LINKS_REQUEST_CODE, "multiple-links-dialog");
    }

    public void setPreferencesFromSiteSettings() {
        changeEditTextPreferenceValue(mTitlePref, mSiteSettings.getTitle());
        changeEditTextPreferenceValue(mTaglinePref, mSiteSettings.getTagline());
        changeEditTextPreferenceValue(mAddressPref, mSiteSettings.getAddress());
        changeEditTextPreferenceValue(mUsernamePref, mSiteSettings.getUsername());
        changeEditTextPreferenceValue(mPasswordPref, mSiteSettings.getPassword());
        changeLanguageValue(mSiteSettings.getLanguageCode());
        setDetailListPreferenceValue(mPrivacyPref,
                                     String.valueOf(mSiteSettings.getPrivacy()),
                                     mSiteSettings.getPrivacyDescription());
        setCategories();
        setPostFormats();
        setAllowComments(mSiteSettings.getAllowComments());
        setSendPingbacks(mSiteSettings.getSendPingbacks());
        setReceivePingbacks(mSiteSettings.getReceivePingbacks());
        setDetailListPreferenceValue(mSortByPref,
                                     String.valueOf(mSiteSettings.getCommentSorting()),
                                     mSiteSettings.getSortingDescription());
        int approval = mSiteSettings.getManualApproval()
                ? mSiteSettings.getUseCommentWhitelist() ? 0
                        : -1 : 1;
        setDetailListPreferenceValue(mWhitelistPref, String.valueOf(approval), getWhitelistSummary(approval));
        String s = StringUtils.getQuantityString(getActivity(), R.string.site_settings_multiple_links_summary_zero,
                                                 R.string.site_settings_multiple_links_summary_one,
                                                 R.string.site_settings_multiple_links_summary_other,
                                                 mSiteSettings.getMultipleLinks());
        mMultipleLinksPref.setSummary(s);
        mIdentityRequiredPreference.setChecked(mSiteSettings.getIdentityRequired());
        mUserAccountRequiredPref.setChecked(mSiteSettings.getUserAccountRequired());
        mThreadingPref.setSummary(mSiteSettings.getThreadingDescription());
        mCloseAfterPref.setSummary(mSiteSettings.getCloseAfterDescriptionForPeriod());
        mPagingPref.setSummary(mSiteSettings.getPagingDescription());
        mRelatedPostsPref.setSummary(mSiteSettings.getRelatedPostsDescription());
        mModerationHoldPref.setSummary(mSiteSettings.getModerationHoldDescription());
        mBlacklistPref.setSummary(mSiteSettings.getBlacklistDescription());
        mJpMonitorActivePref.setChecked(mSiteSettings.isJetpackMonitorEnabled());
        mJpMonitorEmailNotesPref.setChecked(mSiteSettings.shouldSendJetpackMonitorEmailNotifications());
        mJpMonitorWpNotesPref.setChecked(mSiteSettings.shouldSendJetpackMonitorWpNotifications());
        mJpBruteForcePref.setChecked(mSiteSettings.isJetpackProtectEnabled());
        mJpSsoPref.setChecked(mSiteSettings.isJetpackSsoEnabled());
        mJpMatchEmailPref.setChecked(mSiteSettings.isJetpackSsoMatchEmailEnabled());
        mJpUseTwoFactorPref.setChecked(mSiteSettings.isJetpackSsoTwoFactorEnabled());
        mJpWhitelistPref.setSummary(mSiteSettings.getJetpackProtectWhitelistSummary());
        mWeekStartPref.setValue(mSiteSettings.getStartOfWeek());
        mWeekStartPref.setSummary(mWeekStartPref.getEntry());
        mServeImagesFromOurServers.setChecked(mSiteSettings.isServeImagesFromOurServersEnabled());
        mLazyLoadImages.setChecked(mSiteSettings.isLazyLoadImagesEnabled());

        if (mSiteSettings.getAmpSupported()) {
            mAmpPref.setChecked(mSiteSettings.getAmpEnabled());
        } else {
            WPPrefUtils.removePreference(this, R.string.pref_key_site_screen, R.string.pref_key_site_traffic);
        }

        setDateTimeFormatPref(FormatType.DATE_FORMAT, mDateFormatPref, mSiteSettings.getDateFormat());
        setDateTimeFormatPref(FormatType.TIME_FORMAT, mTimeFormatPref, mSiteSettings.getTimeFormat());

        mPostsPerPagePref.setSummary(String.valueOf(mSiteSettings.getPostsPerPage()));
        setTimezonePref(mSiteSettings.getTimezone());
        changeEditTextPreferenceValue(mSiteQuotaSpacePref, mSiteSettings.getQuotaDiskSpace());
    }

    private void setDateTimeFormatPref(FormatType formatType, WPPreference formatPref, String formatValue) {
        String[] entries = formatType.getEntries(getActivity());
        String[] values = formatType.getValues(getActivity());

        // return predefined format if there's a match
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(formatValue)) {
                formatPref.setSummary(entries[i]);
                return;
            }
        }

        // not a predefined format, so it must be custom
        formatPref.setSummary(R.string.site_settings_format_entry_custom);
    }

    private void setTimezonePref(String timezoneValue) {
        if (timezoneValue == null) {
            return;
        }

        String timezone = timezoneValue.replace("_", " ");
        int index = timezone.lastIndexOf("/");
        if (index > -1) {
            mTimezonePref.setSummary(timezone.substring(index + 1));
        } else {
            mTimezonePref.setSummary(timezone);
        }
    }

    private void setCategories() {
        // Ignore if there are no changes
        if (mSiteSettings.isSameCategoryList(mCategoryPref.getEntryValues())) {
            mCategoryPref.setValue(String.valueOf(mSiteSettings.getDefaultCategory()));
            mCategoryPref.setSummary(mSiteSettings.getDefaultCategoryForDisplay());
            return;
        }

        SparseArrayCompat<String> categories = mSiteSettings.getCategoryNames();
        CharSequence[] entries = new CharSequence[categories.size()];
        CharSequence[] values = new CharSequence[categories.size()];
        int i = 0;
        int numOfCategories = categories.size();
        for (int j = 0; j < numOfCategories; j++) {
            int key = categories.keyAt(j);
            entries[i] = categories.get(key);
            values[i] = String.valueOf(key);
            if (key == UNCATEGORIZED_CATEGORY_ID) {
                CharSequence temp = entries[0];
                entries[0] = entries[i];
                entries[i] = temp;
                temp = values[0];
                values[0] = values[i];
                values[i] = temp;
            }
            ++i;
        }

        mCategoryPref.setEntries(entries);
        mCategoryPref.setEntryValues(values);
        mCategoryPref.setValue(String.valueOf(mSiteSettings.getDefaultCategory()));
        mCategoryPref.setSummary(mSiteSettings.getDefaultCategoryForDisplay());
    }

    private void setPostFormats() {
        // Ignore if there are no changes
        if (mSiteSettings.isSameFormatList(mFormatPref.getEntryValues())) {
            mFormatPref.setValue(String.valueOf(mSiteSettings.getDefaultPostFormat()));
            mFormatPref.setSummary(mSiteSettings.getDefaultPostFormatDisplay());
            return;
        }

        // clone the post formats map
        final Map<String, String> postFormats = new HashMap<>(mSiteSettings.getFormats());

        // transform the keys and values into arrays and set the ListPreference's data
        mFormatPref.setEntries(postFormats.values().toArray(new String[0]));
        mFormatPref.setEntryValues(postFormats.keySet().toArray(new String[0]));
        mFormatPref.setValue(String.valueOf(mSiteSettings.getDefaultPostFormat()));
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

    public void setDetailListPreferenceValue(DetailListPreference pref, String value, String summary) {
        pref.setValue(value);
        pref.setSummary(summary);
        pref.refreshAdapter();
    }

    /**
     * Helper method to perform validation and set multiple properties on an EditTextPreference.
     * If newValue is equal to the current preference text no action will be taken.
     */
    public void changeEditTextPreferenceValue(EditTextPreference pref, String newValue) {
        if (newValue == null || pref == null || pref.getEditText().isInEditMode()) {
            return;
        }

        if (!newValue.equals(pref.getSummary())) {
            String formattedValue = StringEscapeUtils.unescapeHtml4(newValue.replaceFirst(ADDRESS_FORMAT_REGEX, ""));

            pref.setText(formattedValue);
            pref.setSummary(formattedValue);
        }
    }

    /**
     * Detail strings for the dialog are generated in the selected language.
     *
     * @param newValue languageCode
     */
    private void changeLanguageValue(String newValue) {
        if (mLanguagePref == null || newValue == null) {
            return;
        }

        if (TextUtils.isEmpty(mLanguagePref.getSummary())
            || !newValue.equals(mLanguagePref.getValue())) {
            mLanguagePref.setValue(newValue);
            String summary = LocaleManager.getLanguageString(newValue, LocaleManager.languageLocale(newValue));
            mLanguagePref.setSummary(summary);
            mLanguagePref.refreshAdapter();
        }
    }

    private void sortLanguages() {
        if (mLanguagePref == null) {
            return;
        }

        Pair<String[], String[]> pair = LocaleManager
                .createSortedLanguageDisplayStrings(mLanguagePref.getEntryValues(), LocaleManager.languageLocale(null));
        if (pair != null) {
            String[] sortedEntries = pair.first;
            String[] sortedValues = pair.second;

            mLanguagePref.setEntries(sortedEntries);
            mLanguagePref.setEntryValues(sortedValues);
            mLanguagePref.setDetails(LocaleManager.createLanguageDetailDisplayStrings(sortedValues));
        }
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
        mSiteSettings.setManualApproval(val == -1);
        mSiteSettings.setUseCommentWhitelist(val == 0);
        setDetailListPreferenceValue(mWhitelistPref,
                                     String.valueOf(val),
                                     getWhitelistSummary(val));
    }

    private void handleStartOver() {
        // Only paid plans should be handled here, free plans should be redirected to website from "Start Over" button
        if (mSite == null || mSite.getHasFreePlan()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"help@wordpress.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.start_over_email_subject,
                                                        SiteUtils.getHomeURLOrHostName(mSite)));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.start_over_email_body, mSite.getUrl()));
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.contact_support)));
        } catch (android.content.ActivityNotFoundException ex) {
            ToastUtils.showToast(getActivity(), R.string.start_over_email_intent_error);
        }
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.SITE_SETTINGS_START_OVER_CONTACT_SUPPORT_CLICKED,
                                            mSite);
    }

    private void showListEditorDialog(int titleRes, int headerRes) {
        mDialog = new Dialog(getActivity(), R.style.Calypso_SiteSettingsTheme);
        mDialog.setOnDismissListener(this);
        mDialog.setTitle(titleRes);
        mDialog.setContentView(getListEditorView(getString(headerRes)));
        mDialog.show();
        WPActivityUtils.addToolbarToDialog(this, mDialog, getString(titleRes));
    }

    private View getListEditorView(String headerText) {
        Context themer = new ContextThemeWrapper(getActivity(), R.style.Calypso_SiteSettingsTheme);
        View view = View.inflate(themer, R.layout.list_editor, null);
        ((TextView) view.findViewById(R.id.list_editor_header_text)).setText(headerText);

        mAdapter = null;
        final EmptyViewRecyclerView list = view.findViewById(android.R.id.list);
        list.setLayoutManager(
                new SmoothScrollLinearLayoutManager(
                        getActivity(),
                        LinearLayoutManager.VERTICAL,
                        false,
                        getResources().getInteger(android.R.integer.config_mediumAnimTime)
                )
                             );
        list.setAdapter(getAdapter());
        list.setEmptyView(view.findViewById(R.id.empty_view));
        list.addOnItemTouchListener(
                new RecyclerViewItemClickListener(
                        getActivity(),
                        list,
                        new RecyclerViewItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                if (mActionMode != null) {
                                    getAdapter().toggleItemSelected(position);
                                    mActionMode.invalidate();
                                    if (getAdapter().getItemsSelected().size() <= 0) {
                                        mActionMode.finish();
                                    }
                                }
                            }

                            @Override
                            public void onLongItemClick(View view, int position) {
                                if (mActionMode == null) {
                                    if (view.isHapticFeedbackEnabled()) {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                                    }

                                    mDialog.getWindow().getDecorView().startActionMode(new ActionModeCallback());
                                    getAdapter().setItemSelected(position);
                                    mActionMode.invalidate();
                                }
                            }
                        }
                )
        );
        view.findViewById(R.id.fab_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                                new ContextThemeWrapper(getActivity(), R.style.Calypso_Dialog_Alert));
                final EditText input = new EditText(getActivity());
                WPPrefUtils.layoutAsInput(input);
                input.setWidth(getResources().getDimensionPixelSize(R.dimen.list_editor_input_max_width));
                input.setHint(R.string.site_settings_list_editor_input_hint);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String entry = input.getText().toString();
                        if (!TextUtils.isEmpty(entry) && !mEditingList.contains(entry)) {
                            // don't modify mEditingList if it's not a reference to the JP whitelist keys
                            if (mEditingList == mSiteSettings.getJetpackWhitelistKeys() && !isValidIpOrRange(entry)) {
                                ToastUtils.showToast(getActivity(), R.string.invalid_ip_or_range);
                                return;
                            }

                            mEditingList.add(entry);
                            getAdapter().notifyItemInserted(getAdapter().getItemCount() - 1);
                            list.post(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            list.smoothScrollToPosition(getAdapter().getItemCount() - 1);
                                        }
                                    }
                            );
                            mSiteSettings.saveSettings();
                            AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.SITE_SETTINGS_ADDED_LIST_ITEM,
                                                                mSite);
                        }
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                final AlertDialog alertDialog = builder.create();
                int spacing = getResources().getDimensionPixelSize(R.dimen.dlp_padding_start);
                alertDialog.setView(input, spacing, spacing, spacing, 0);
                alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                alertDialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        alertDialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                    }
                });
                alertDialog.show();
                Button positive = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                Button negative = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                if (positive != null) {
                    WPPrefUtils.layoutAsFlatButton(positive);
                }
                if (negative != null) {
                    WPPrefUtils.layoutAsFlatButton(negative);
                }
            }
        });

        return view;
    }

    /**
     * Verifies that a given string can correctly be interpreted as an IP address or an IP range.
     */
    private boolean isValidIpOrRange(String entry) {
        // empty strings are not valid
        if (TextUtils.isEmpty(entry)) {
            return false;
        }

        // remove whitespace
        entry = entry.replaceAll("\\s", "");

        // if entry is a range it will be formatted as two IP addresses separated by a '-'
        String[] ipStrings = entry.split("-");

        // entry is not well-formed if there are more than 2 ipStrings (a range) or no ipStrings
        if (ipStrings.length > 2 || ipStrings.length < 1) {
            return false;
        }

        // if any IP string is not a valid IP address then entry is not valid
        for (String ip : ipStrings) {
            if (!ValidationUtils.validateIPv4(ip)) {
                return false;
            }
        }

        return true;
    }

    public boolean shouldShowListPreference(DetailListPreference preference) {
        return preference != null && preference.getEntries() != null && preference.getEntries().length > 0;
    }

    private void setupJetpackSecurityScreen() {
        if (mJpSecuritySettings == null || !isAdded()) {
            return;
        }
        String title = getString(R.string.jetpack_security_setting_title);
        Dialog dialog = mJpSecuritySettings.getDialog();
        if (dialog != null) {
            setupPreferenceList((ListView) dialog.findViewById(android.R.id.list), getResources());
            WPActivityUtils.addToolbarToDialog(this, dialog, title);
        }
    }

    private void setupSpeedUpScreen() {
        if (mSpeedUpYourSiteSettings == null || !isAdded()) {
            return;
        }
        String title = getString(R.string.site_settings_speed_up_your_site);
        Dialog dialog = mSpeedUpYourSiteSettings.getDialog();
        if (dialog != null) {
            setupPreferenceList((ListView) dialog.findViewById(android.R.id.list), getResources());
            WPActivityUtils.addToolbarToDialog(this, dialog, title);
        }
    }

    private boolean setupMorePreferenceScreen() {
        if (mMorePreference == null || !isAdded()) {
            return false;
        }
        String title = getString(R.string.site_settings_discussion_title);
        Dialog dialog = mMorePreference.getDialog();
        if (dialog != null) {
            dialog.setTitle(title);
            setupPreferenceList((ListView) dialog.findViewById(android.R.id.list), getResources());
            WPActivityUtils.addToolbarToDialog(this, dialog, title);
            return true;
        }
        return false;
    }

    private void removeMoreScreenToolbar() {
        if (mMorePreference == null || !isAdded()) {
            return;
        }
        Dialog moreDialog = mMorePreference.getDialog();
        WPActivityUtils.removeToolbarFromDialog(this, moreDialog);
    }

    private void removeJetpackSecurityScreenToolbar() {
        if (mJpSecuritySettings == null || !isAdded()) {
            return;
        }
        Dialog securityDialog = mJpSecuritySettings.getDialog();
        WPActivityUtils.removeToolbarFromDialog(this, securityDialog);
    }

    private void hideAdminRequiredPreferences() {
        WPPrefUtils.removePreference(this, R.string.pref_key_site_screen, R.string.pref_key_site_general);
        WPPrefUtils.removePreference(this, R.string.pref_key_site_screen, R.string.pref_key_site_discussion);
        WPPrefUtils.removePreference(this, R.string.pref_key_site_writing, R.string.pref_key_site_category);
        WPPrefUtils.removePreference(this, R.string.pref_key_site_writing, R.string.pref_key_site_format);
        WPPrefUtils.removePreference(this, R.string.pref_key_site_writing, R.string.pref_key_site_related_posts);
    }

    private void removeNonSelfHostedPreferences() {
        mUsernamePref.setEnabled(true);
        mPasswordPref.setEnabled(true);
        WPPrefUtils.removePreference(this, R.string.pref_key_site_screen, R.string.pref_key_site_general);
        WPPrefUtils.removePreference(this, R.string.pref_key_site_screen, R.string.pref_key_site_writing);
        WPPrefUtils.removePreference(this, R.string.pref_key_site_screen, R.string.pref_key_site_discussion);
        WPPrefUtils.removePreference(this, R.string.pref_key_site_screen, R.string.pref_key_site_advanced);
        WPPrefUtils.removePreference(this, R.string.pref_key_site_screen, R.string.pref_key_site_quota);
        WPPrefUtils.removePreference(this, R.string.pref_key_site_screen, R.string.pref_key_jetpack_settings);
    }

    private void removeNonJetpackPreferences() {
        removePrivateOptionFromPrivacySetting();
        WPPrefUtils.removePreference(this, R.string.pref_key_site_screen, R.string.pref_key_site_advanced);
        WPPrefUtils.removePreference(this, R.string.pref_key_site_screen, R.string.pref_key_site_account);
        WPPrefUtils.removePreference(this, R.string.pref_key_site_general, R.string.pref_key_site_language);
        if (!mSite.hasDiskSpaceQuotaInformation()) {
            WPPrefUtils.removePreference(this, R.string.pref_key_site_screen, R.string.pref_key_site_quota);
        }
    }

    private void removeSpeedUpJetpackPreferences() {
        WPPrefUtils.removePreference(this, R.string.pref_key_site_writing, R.string.pref_key_speed_up_your_site_screen);
    }

    private void removePrivateOptionFromPrivacySetting() {
        if (mPrivacyPref == null) {
            return;
        }

        final CharSequence[] entries = mPrivacyPref.getEntries();
        mPrivacyPref.remove(ArrayUtils.indexOf(entries, getString(R.string.site_settings_privacy_private_summary)));
    }

    private void removeNonWPComPreferences() {
        WPPrefUtils.removePreference(this, R.string.pref_key_site_screen, R.string.pref_key_site_account);
        WPPrefUtils.removePreference(this, R.string.pref_key_site_screen, R.string.pref_key_jetpack_settings);
    }

    private Preference getChangePref(int id) {
        return WPPrefUtils.getPrefAndSetChangeListener(this, id, this);
    }

    private Preference getClickPref(int id) {
        return WPPrefUtils.getPrefAndSetClickListener(this, id, this);
    }

    private void exportSite() {
        if (mSite.isWPCom()) {
            final ProgressDialog progressDialog = ProgressDialog
                    .show(getActivity(), "", getActivity().getString(R.string.exporting_content_progress), true, true);
            WordPress.getRestClientUtils().exportContentAll(mSite.getSiteId(), new RestRequest.Listener() {
                @Override
                public void onResponse(JSONObject response) {
                    if (isAdded()) {
                        AnalyticsUtils.trackWithSiteDetails(
                                AnalyticsTracker.Stat.SITE_SETTINGS_EXPORT_SITE_RESPONSE_OK, mSite);
                        dismissProgressDialog(progressDialog);
                        Snackbar.make(getView(), R.string.export_email_sent, Snackbar.LENGTH_LONG).show();
                    }
                }
            }, new RestRequest.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (isAdded()) {
                        HashMap<String, Object> errorProperty = new HashMap<>();
                        errorProperty.put(ANALYTICS_ERROR_PROPERTY_KEY, error.getMessage());
                        AnalyticsUtils.trackWithSiteDetails(
                                AnalyticsTracker.Stat.SITE_SETTINGS_EXPORT_SITE_RESPONSE_ERROR,
                                mSite, errorProperty);
                        dismissProgressDialog(progressDialog);
                    }
                }
            });
        }
    }

    private void deleteSite() {
        if (mSite.isWPCom()) {
            mDeleteSiteProgressDialog =
                    ProgressDialog.show(getActivity(), "", getString(R.string.delete_site_progress), true, false);
            AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.SITE_SETTINGS_DELETE_SITE_REQUESTED, mSite);
            mDispatcher.dispatch(SiteActionBuilder.newDeleteSiteAction(mSite));
        }
    }

    public void handleSiteDeleted() {
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat
                                                    .SITE_SETTINGS_DELETE_SITE_RESPONSE_OK, mSite);
        dismissProgressDialog(mDeleteSiteProgressDialog);
        mDeleteSiteProgressDialog = null;
        mSite = null;
    }

    public void handleDeleteSiteError(DeleteSiteError error) {
        AppLog.e(AppLog.T.SETTINGS, "SiteDeleted error: " + error.type);

        HashMap<String, Object> errorProperty = new HashMap<>();
        errorProperty.put(ANALYTICS_ERROR_PROPERTY_KEY, error.message);
        AnalyticsUtils.trackWithSiteDetails(
                AnalyticsTracker.Stat.SITE_SETTINGS_DELETE_SITE_RESPONSE_ERROR, mSite,
                errorProperty);
        dismissProgressDialog(mDeleteSiteProgressDialog);
        mDeleteSiteProgressDialog = null;

        showDeleteSiteErrorDialog();
    }

    private void showDeleteSiteErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(getActivity(), R.style.Calypso_Dialog_Alert));
        builder.setTitle(R.string.error_deleting_site);
        builder.setMessage(R.string.error_deleting_site_summary);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(R.string.contact_support, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mZendeskHelper.createNewTicket(getActivity(), Origin.DELETE_SITE, mSite);
            }
        });
        builder.show();
    }

    private MultiSelectRecyclerViewAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new MultiSelectRecyclerViewAdapter(getActivity(), mEditingList);
        }

        return mAdapter;
    }

    private final class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.menu_delete:
                    SparseBooleanArray checkedItems = getAdapter().getItemsSelected();

                    HashMap<String, Object> properties = new HashMap<>();
                    properties.put("num_items_deleted", checkedItems.size());
                    AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.SITE_SETTINGS_DELETED_LIST_ITEMS,
                                                        mSite, properties);

                    for (int i = checkedItems.size() - 1; i >= 0; i--) {
                        final int index = checkedItems.keyAt(i);

                        if (checkedItems.get(index)) {
                            mEditingList.remove(index);
                        }
                    }

                    mSiteSettings.saveSettings();
                    mActionMode.finish();
                    return true;
                case R.id.menu_select_all:
                    for (int i = 0; i < getAdapter().getItemCount(); i++) {
                        getAdapter().setItemSelected(i);
                    }

                    mActionMode.invalidate();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            WPActivityUtils.setStatusBarColor(mDialog.getWindow(), R.color.action_mode_status_bar_tint);
            mActionMode = actionMode;
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.list_editor, menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            WPActivityUtils.setStatusBarColor(mDialog.getWindow(), R.color.status_bar_tint);
            getAdapter().removeItemsSelected();
            mActionMode = null;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            actionMode.setTitle(getString(
                    R.string.site_settings_list_editor_action_mode_title,
                    getAdapter().getItemsSelected().size())
                               );
            return true;
        }
    }

    /**
     * Show Disconnect button for development purposes. Only available in debug builds on Jetpack sites.
     */
    private boolean shouldShowDisconnect() {
        return BuildConfig.DEBUG && mSite.isJetpackConnected() && mSite.isUsingWpComRestApi();
    }
}
