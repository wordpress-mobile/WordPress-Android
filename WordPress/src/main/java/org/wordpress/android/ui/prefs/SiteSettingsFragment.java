package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
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
import org.wordpress.android.util.widgets.WPEditText;
import org.wordpress.android.widgets.TypefaceCache;

import java.util.List;
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
                   Dialog.OnDismissListener,
                   SiteSettingsInterface.SiteSettingsListener {

    public static final int RESULT_BLOG_REMOVED = Activity.RESULT_FIRST_USER;

    private static final String ADDRESS_FORMAT_REGEX = "^(https?://(w{3})?|www\\.)";
    private static final int RELATED_POSTS_REQUEST_CODE = 1;
    private static final int NO_REGION_LANG_CODE_LEN = 2;
    private static final int REGION_SUBSTRING_INDEX = 3;

    private Blog mBlog;
    private SiteSettingsInterface mSiteSettings;
    private List<String> mEditingList;
    private boolean mBlogDeleted;

    private EditTextPreference mTitlePref;
    private EditTextPreference mTaglinePref;
    private EditTextPreference mAddressPref;
    private DetailListPreference mLanguagePref;
    private DetailListPreference mPrivacyPref;
    private EditTextPreference mUsernamePref;
    private EditTextPreference mPasswordPref;
    private WPSwitchPreference mLocationPref;
    private DetailListPreference mCategoryPref;
    private DetailListPreference mFormatPref;
    private Preference mRelatedPostsPref;
    private WPSwitchPreference mAllowCommentsNested;
    private WPSwitchPreference mAllowCommentsPref;
    private WPSwitchPreference mSendPingbacksNested;
    private WPSwitchPreference mSendPingbacksPref;
    private WPSwitchPreference mReceivePingbacksNested;
    private WPSwitchPreference mReceivePingbacksPref;
    private WPSwitchPreference mIdentityRequiredPreference;
    private WPSwitchPreference mUserAccountRequiredPref;
    private DetailListPreference mCloseAfterPref;
    private DetailListPreference mSortByPref;
    private DetailListPreference mThreadingPref;
    private DetailListPreference mPagingPref;
    private DetailListPreference mWhitelistPref;
    private Preference mMultipleLinksPref;
    private Preference mModerationHoldPref;
    private Preference mBlacklistPref;
    private Preference mRemoveSitePref;
    private Preference mDeleteSitePref;
    private Preference mStartOverPref;

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

        // TODO: no!
        mSiteSettings.init(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Assume user wanted changes propagated when they leave
        if (!mBlogDeleted) {
            mSiteSettings.saveSettings();
        }
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
                    R.string.hold_for_moderation_description,
                    mEditingList);
            return true;
        } else if (preference == mBlacklistPref) {
            mEditingList = mSiteSettings.getBlacklistKeys();
            showListEditorDialog(R.string.site_settings_blacklist_title,
                    R.string.blacklist_description,
                    mEditingList);
            return true;
        } else if (preference == mDeleteSitePref) {
            removeBlogWithConfirmation();
        } else if (preference == mRemoveSitePref) {
        } else if (preference == mStartOverPref) {
        }

        return false;
    }

    private void removeBlog() {
        if (WordPress.wpDB.deleteBlog(getActivity(), mBlog.getLocalTableBlogId())) {
            StatsTable.deleteStatsForBlog(getActivity(), mBlog.getLocalTableBlogId()); // Remove stats data
            AnalyticsUtils.refreshMetadata();
            ToastUtils.showToast(getActivity(), R.string.blog_removed_successfully);
            WordPress.wpDB.deleteLastBlogId();
            WordPress.currentBlog = null;
            mBlogDeleted = true;
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

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        super.onPreferenceTreeClick(screen, preference);

        // Add Action Bar to sub-screens
        if (preference == findPreference(getString(R.string.pref_key_site_more_discussion))) {
            Dialog dialog = ((PreferenceScreen) preference).getDialog();
            if (dialog != null) {
                ListView prefList = (ListView) dialog.findViewById(android.R.id.list);
                prefList.setOnItemLongClickListener(this);
                String title = getString(R.string.site_settings_discussion_title);
                WPActivityUtils.addToolbarToDialog(this, dialog, title);
            }
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue == null) return false;

        if (preference == mTitlePref) {
            mSiteSettings.setTitle(newValue.toString());
            changeEditTextPreferenceValue(mTitlePref, mSiteSettings.getTitle());
            return true;
        } else if (preference == mTaglinePref) {
            mSiteSettings.setTagline(newValue.toString());
            changeEditTextPreferenceValue(mTaglinePref, mSiteSettings.getTagline());
            return true;
        } else if (preference == mAddressPref) {
            mSiteSettings.setAddress(newValue.toString());
            changeEditTextPreferenceValue(mAddressPref, mSiteSettings.getAddress());
            return true;
        } else if (preference == mLanguagePref) {
            mSiteSettings.setLanguageCode(newValue.toString());
            changeLanguageValue(mSiteSettings.getLanguageCode());
            return true;
        } else if (preference == mPrivacyPref) {
            mSiteSettings.setPrivacy(Integer.valueOf(newValue.toString()));
            setDetailListPreferenceValue(mPrivacyPref,
                    String.valueOf(mSiteSettings.getPrivacy()),
                    getPrivacySummary(mSiteSettings.getPrivacy()));
            return true;
        } else if (preference == mAllowCommentsPref || preference == mAllowCommentsNested) {
            setAllowComments((Boolean) newValue);
            return true;
        } else if (preference == mSendPingbacksPref || preference == mSendPingbacksNested) {
            setSendPingbacks((Boolean) newValue);
            return true;
        } else if (preference == mReceivePingbacksPref || preference == mReceivePingbacksNested) {
            setReceivePingbacks((Boolean) newValue);
            return true;
        } else if (preference == mCloseAfterPref) {
            mSiteSettings.setCloseAfter(Integer.parseInt(newValue.toString()));
            setDetailListPreferenceValue(mCloseAfterPref,
                    newValue.toString(),
                    getCloseAfterSummary(mSiteSettings.getCloseAfter()));
            return true;
        } else if (preference == mSortByPref) {
            mSiteSettings.setCommentSorting(Integer.parseInt(newValue.toString()));
            setDetailListPreferenceValue(mSortByPref,
                    newValue.toString(),
                    getSortOrderSummary(mSiteSettings.getCommentSorting()));
            return true;
        } else if (preference == mThreadingPref) {
            mSiteSettings.setThreadingLevels(Integer.parseInt(newValue.toString()));
            setDetailListPreferenceValue(mThreadingPref,
                    newValue.toString(),
                    getThreadingSummary(mSiteSettings.getThreadingLevels()));
            return true;
        } else if (preference == mPagingPref) {
            mSiteSettings.setPagingCount(Integer.parseInt(newValue.toString()));
            setDetailListPreferenceValue(mPagingPref,
                    newValue.toString(),
                    getPagingSummary(mSiteSettings.getPagingCount()));
            return true;
        } else if (preference == mIdentityRequiredPreference) {
            mSiteSettings.setIdentityRequired((Boolean) newValue);
            return true;
        } else if (preference == mUserAccountRequiredPref) {
            mSiteSettings.setUserAccountRequired((Boolean) newValue);
            return true;
        } else if (preference == mWhitelistPref) {
            updateWhitelistSettings(Integer.parseInt(newValue.toString()));
            return true;
        } else if (preference == mMultipleLinksPref) {
            mSiteSettings.setMultipleLinks(Integer.parseInt(newValue.toString()));
            mMultipleLinksPref.setSummary(getResources()
                    .getQuantityString(R.plurals.multiple_links_summary,
                            mSiteSettings.getMultipleLinks(),
                            mSiteSettings.getMultipleLinks()));
            return true;
        } else if (preference == mUsernamePref) {
            mSiteSettings.setUsername(newValue.toString());
            changeEditTextPreferenceValue(mUsernamePref, mSiteSettings.getUsername());
            return true;
        } else if (preference == mPasswordPref) {
            mSiteSettings.setPassword(newValue.toString());
            changeEditTextPreferenceValue(mPasswordPref, mSiteSettings.getPassword());
            return true;
        } else if (preference == mLocationPref) {
            mSiteSettings.setLocation((Boolean) newValue);
            return true;
        } else if (preference == mCategoryPref) {
            mSiteSettings.setDefaultCategory(Integer.parseInt(newValue.toString()));
            setDetailListPreferenceValue(mCategoryPref,
                    newValue.toString(),
                    mSiteSettings.getDefaultCategoryForDisplay());
            return true;
        } else if (preference == mFormatPref) {
            mSiteSettings.setDefaultFormat(newValue.toString());
            setDetailListPreferenceValue(mFormatPref,
                    newValue.toString(),
                    mSiteSettings.getDefaultPostFormatDisplay());
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
    public void onDismiss(DialogInterface dialog) {
        mEditingList = null;
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
        if (mAddressPref != null) mAddressPref.setEnabled(false);
        if (mTitlePref != null) mTitlePref.setEnabled(allow);
        if (mTaglinePref != null) mTaglinePref.setEnabled(allow);
        if (mPrivacyPref != null) mPrivacyPref.setEnabled(allow);
        if (mLanguagePref != null) mLanguagePref.setEnabled(allow);
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
        mCloseAfterPref = (DetailListPreference) getPref(R.string.pref_key_site_close_after);
        mSortByPref = (DetailListPreference) getPref(R.string.pref_key_site_sort_by);
        mThreadingPref = (DetailListPreference) getPref(R.string.pref_key_site_threading);
        mPagingPref = (DetailListPreference) getPref(R.string.pref_key_site_paging);
        mWhitelistPref = (DetailListPreference) getPref(R.string.pref_key_site_whitelist);
        mMultipleLinksPref = getPref(R.string.pref_key_site_multiple_links);
        mModerationHoldPref = getPref(R.string.pref_key_site_moderation_hold);
        mBlacklistPref = getPref(R.string.pref_key_site_blacklist);
        mRelatedPostsPref = findPreference(getString(R.string.pref_key_site_related_posts));
        mRemoveSitePref = findPreference(getString(R.string.pref_key_site_remove_site));
        mDeleteSitePref = findPreference(getString(R.string.pref_key_site_delete_site));
        mStartOverPref = findPreference(getString(R.string.pref_key_site_start_over));
        mRelatedPostsPref.setOnPreferenceClickListener(this);
        mMultipleLinksPref.setOnPreferenceClickListener(this);
        mModerationHoldPref.setOnPreferenceClickListener(this);
        mBlacklistPref.setOnPreferenceClickListener(this);
        mRemoveSitePref.setOnPreferenceClickListener(this);
        mDeleteSitePref.setOnPreferenceClickListener(this);
        mStartOverPref.setOnPreferenceClickListener(this);

        // .com sites hide the Account category, self-hosted sites hide the Related Posts preference
        if (mBlog.isDotcomFlag()) {
            removePreference(R.string.pref_key_site_screen, R.string.pref_key_site_account);
            removePreference(R.string.pref_key_site_screen, R.string.pref_key_site_remove_site);
        } else {
            removePreference(R.string.pref_key_site_general, R.string.pref_key_site_language);
            removePreference(R.string.pref_key_site_writing, R.string.pref_key_site_related_posts);
            removePreference(R.string.pref_key_site_screen, R.string.pref_key_site_start_over);
            removePreference(R.string.pref_key_site_screen, R.string.pref_key_site_delete_site);
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

    private void showMultipleLinksDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Calypso_AlertDialog);
        View view = View.inflate(getActivity(), R.layout.number_picker_dialog, null);
        final NumberPicker numberPicker = (NumberPicker) view.findViewById(R.id.number_picker);
        TextView detailText = (TextView) view.findViewById(R.id.number_picker_text);
        detailText.setTextColor(getResources().getColor(R.color.grey_darken_10));
        detailText.setText(R.string.multiple_links_description);
        numberPicker.setMaxValue(getResources().getInteger(R.integer.max_links_limit));
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View titleView = inflater.inflate(R.layout.detail_list_preference_title, null);
        TextView titleText = ((TextView) titleView.findViewById(R.id.title));
        titleText.setText(R.string.site_settings_multiple_links_title);
        titleText.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));
        builder.setCustomTitle(titleView);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mSiteSettings.getMultipleLinks() != numberPicker.getValue()) {
                    onPreferenceChange(mMultipleLinksPref, numberPicker.getValue());
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.setView(view);
        builder.create().show();
    }

    private void setPreferencesFromSiteSettings() {
        mLocationPref.setChecked(mSiteSettings.getLocation());
        changeEditTextPreferenceValue(mTitlePref, mSiteSettings.getTitle());
        changeEditTextPreferenceValue(mTaglinePref, mSiteSettings.getTagline());
        changeEditTextPreferenceValue(mAddressPref, mSiteSettings.getAddress());
        changeEditTextPreferenceValue(mUsernamePref, mSiteSettings.getUsername());
        changeEditTextPreferenceValue(mPasswordPref, mSiteSettings.getPassword());
        setDetailListPreferenceValue(mPrivacyPref,
                String.valueOf(mSiteSettings.getPrivacy()),
                getPrivacySummary(mSiteSettings.getPrivacy()));
        setCategories();
        setPostFormats();
        setAllowComments(mSiteSettings.getAllowComments());
        setSendPingbacks(mSiteSettings.getSendPingbacks());
        setReceivePingbacks(mSiteSettings.getReceivePingbacks());
        setDetailListPreferenceValue(mCloseAfterPref,
                String.valueOf(mSiteSettings.getCloseAfter()),
                getCloseAfterSummary(mSiteSettings.getCloseAfter()));
        setDetailListPreferenceValue(mSortByPref,
                String.valueOf(mSiteSettings.getCommentSorting()),
                getSortOrderSummary(mSiteSettings.getCommentSorting()));
        setDetailListPreferenceValue(mThreadingPref,
                String.valueOf(mSiteSettings.getThreadingLevels()),
                getThreadingSummary(mSiteSettings.getThreadingLevels()));
        setDetailListPreferenceValue(mPagingPref,
                String.valueOf(mSiteSettings.getPagingCount()),
                getPagingSummary(mSiteSettings.getPagingCount()));
        int approval = mSiteSettings.getManualApproval() ?
                mSiteSettings.getUseCommentWhitelist() ? 0
                        : -1 : 1;
        setDetailListPreferenceValue(mWhitelistPref, String.valueOf(approval), getWhitelistSummary(approval));
        mMultipleLinksPref.setSummary(getResources()
                .getQuantityString(R.plurals.multiple_links_summary,
                mSiteSettings.getMultipleLinks(),
                mSiteSettings.getMultipleLinks()));
        mIdentityRequiredPreference.setChecked(mSiteSettings.getIdentityRequired());
        mUserAccountRequiredPref.setChecked(mSiteSettings.getUserAccountRequired());
        mThreadingPref.setValue(String.valueOf(mSiteSettings.getThreadingLevels()));
        mPagingPref.setValue(String.valueOf(mSiteSettings.getPagingCount()));
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
        if (mSendPingbacksPref.isChecked() != newValue) mSendPingbacksPref.setChecked(newValue);
        if (mSendPingbacksNested.isChecked() != newValue) mSendPingbacksNested.setChecked(newValue);
    }

    private void setReceivePingbacks(boolean newValue) {
        mSiteSettings.setReceivePingbacks(newValue);
        if (mReceivePingbacksPref.isChecked() != newValue) mReceivePingbacksPref.setChecked(newValue);
        if (mReceivePingbacksNested.isChecked() != newValue) mReceivePingbacksNested.setChecked(newValue);
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
        switch (privacy) {
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

    private String getCloseAfterSummary(int period) {
        if (period == 0) return getString(R.string.never);
        return getResources().getQuantityString(R.plurals.days_quantity, period, period);
    }

    private String getSortOrderSummary(int order) {
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
        if (levels <= 1) return getString(R.string.none);
        return String.format(getString(R.string.levels_quantity), levels);
    }

    private String getPagingSummary(int count) {
        if (count == 0) return getString(R.string.none);
        return getResources().getQuantityString(R.plurals.paging_quantity, count, count);
    }

    private String getWhitelistSummary(int value) {
        switch (value) {
            case -1:
                return getString(R.string.whitelist_summary_no_users);
            case 0:
                return getString(R.string.whitelist_summary_known_users);
            case 1:
                return getString(R.string.whitelist_summary_all_users);
            default:
                return "";
        }
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

    private void showListEditorDialog(int titleRes, int footerRes, List<String> items) {
        Dialog dialog = new Dialog(getActivity(), R.style.Calypso_SiteSettingsTheme);
        dialog.setOnDismissListener(this);
        dialog.setContentView(getListEditorView(items, getString(footerRes)));
        dialog.show();
        WPActivityUtils.addToolbarToDialog(this, dialog, getString(titleRes));
    }

    private void setEditorListEntries(ListView list, List<String> items) {
        if (list == null || items == null) return;
        list.setAdapter(new ArrayAdapter<>(getActivity(),
                R.layout.wp_simple_list_item_1,
                items));
    }

    private View getListEditorView(List<String> items, String footerText) {
        View view = View.inflate(getActivity(), R.layout.list_editor, null);
        ((TextView) view.findViewById(R.id.list_editor_footer_text)).setText(footerText);

        final ListView list = (ListView) view.findViewById(android.R.id.list);
        list.setEmptyView(view.findViewById(R.id.empty_view));
        setEditorListEntries(list, items);
        view.findViewById(R.id.fab_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder =
                        new AlertDialog.Builder(getActivity(), R.style.Calypso_AlertDialog);
                final EditText input = new WPEditText(getActivity());
                input.setHint("Enter a word or phrase");
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String entry = input.getText().toString();
                        if (!mEditingList.contains(entry)) {
                            mEditingList.add(entry);
                            setEditorListEntries(list, mEditingList);
                        }
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                AlertDialog alertDialog = builder.create();
                alertDialog.setView(input, 64, 64, 64, 0);
                alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                alertDialog.show();
            }
        });

        return view;
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
