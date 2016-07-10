package org.wordpress.android.ui.publicize;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.prefs.BlogPreferencesActivity;
import org.wordpress.android.ui.prefs.SiteSettingsFragment;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.ui.prefs.SummaryEditTextPreference;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPPrefUtils;



public class PublicizeManageConnectionsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, SiteSettingsInterface.SiteSettingsListener {
    private PublicizePreferenceListener mListener;
    private SummaryEditTextPreference mLabelPreference;
    private SiteSettingsInterface mSiteSettings;
    private Blog mBlog;

    public PublicizeManageConnectionsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.publicize_preferences);

        mBlog = WordPress.currentBlog;
        if (mBlog == null || !NetworkUtils.checkConnection(getActivity())) {
            getActivity().finish();
            return;
        }

        mLabelPreference = (SummaryEditTextPreference) getChangePref(R.string.publicize_label);
        mSiteSettings = SiteSettingsInterface.getInterface(getActivity(), mBlog, this);
    }

    @Override
    public void onResume() {
        super.onResume();

        // always load cached settings
        mSiteSettings.init(false);

        if (true) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // initialize settings with locally cached values, fetch remote on first pass
                    mSiteSettings.init(true);
                }
            }, 1000);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof PublicizePreferenceListener) {
            mListener = (PublicizePreferenceListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement PublicizePreferenceListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private Preference getChangePref(int id) {
        return WPPrefUtils.getPrefAndSetChangeListener(this, id, this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference == mLabelPreference) {
            changeEditTextPreferenceValue(mLabelPreference, o.toString());
        }
        return false;
    }

    private void syncSettings() {

    }

    private void changeEditTextPreferenceValue(EditTextPreference pref, String newValue) {
        if (newValue == null || pref == null || pref.getEditText().isInEditMode()) return;

        if (!newValue.equals(pref.getSummary())) {
            String formattedValue = StringUtils.unescapeHTML(newValue.replaceFirst(SiteSettingsFragment.ADDRESS_FORMAT_REGEX, ""));

            pref.setText(formattedValue);
            pref.setSummary(formattedValue);
        }
    }

    private void setPreferencesFromSiteSettings() {
        changeEditTextPreferenceValue(mLabelPreference, mSiteSettings.getSharingLabel());
//        mLocationPref.setChecked(mSiteSettings.getLocation());
//        changeEditTextPreferenceValue(mTitlePref, mSiteSettings.getTitle());
//        changeEditTextPreferenceValue(mTaglinePref, mSiteSettings.getTagline());
//        changeEditTextPreferenceValue(mAddressPref, mSiteSettings.getAddress());
//        changeEditTextPreferenceValue(mUsernamePref, mSiteSettings.getUsername());
//        changeEditTextPreferenceValue(mPasswordPref, mSiteSettings.getPassword());
//        changeLanguageValue(mSiteSettings.getLanguageCode());
//        setDetailListPreferenceValue(mPrivacyPref,
//                String.valueOf(mSiteSettings.getPrivacy()),
//                mSiteSettings.getPrivacyDescription());
//        setDetailListPreferenceValue(mImageWidthPref,
//                mBlog.getMaxImageWidth(),
//                mBlog.getMaxImageWidth());
//        setCategories();
//        setPostFormats();
//        setAllowComments(mSiteSettings.getAllowComments());
//        setSendPingbacks(mSiteSettings.getSendPingbacks());
//        setReceivePingbacks(mSiteSettings.getReceivePingbacks());
//        setDetailListPreferenceValue(mSortByPref,
//                String.valueOf(mSiteSettings.getCommentSorting()),
//                mSiteSettings.getSortingDescription());
//        int approval = mSiteSettings.getManualApproval() ?
//                mSiteSettings.getUseCommentWhitelist() ? 0
//                        : -1 : 1;
//        setDetailListPreferenceValue(mWhitelistPref, String.valueOf(approval), getWhitelistSummary(approval));
//        String s = StringUtils.getQuantityString(getActivity(), R.string.site_settings_multiple_links_summary_zero,
//                R.string.site_settings_multiple_links_summary_one,
//                R.string.site_settings_multiple_links_summary_other, mSiteSettings.getMultipleLinks());
//        mMultipleLinksPref.setSummary(s);
//        mUploadAndLinkPref.setChecked(mBlog.isFullSizeImage());
//        mIdentityRequiredPreference.setChecked(mSiteSettings.getIdentityRequired());
//        mUserAccountRequiredPref.setChecked(mSiteSettings.getUserAccountRequired());
//        mThreadingPref.setSummary(mSiteSettings.getThreadingDescription());
//        mCloseAfterPref.setSummary(mSiteSettings.getCloseAfterDescriptionForPeriod());
//        mPagingPref.setSummary(mSiteSettings.getPagingDescription());
//        mRelatedPostsPref.setSummary(mSiteSettings.getRelatedPostsDescription());
//        mModerationHoldPref.setSummary(mSiteSettings.getModerationHoldDescription());
//        mBlacklistPref.setSummary(mSiteSettings.getBlacklistDescription());
    }

    @Override
    public void onSettingsUpdated(Exception error) {
        if (isAdded()) setPreferencesFromSiteSettings();
    }

    @Override
    public void onSettingsSaved(Exception error) {

    }

    @Override
    public void onCredentialsValidated(Exception error) {

    }

    public interface PublicizePreferenceListener {
        void onPreferenceUpdated();
    }
}
