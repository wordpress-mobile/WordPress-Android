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
import org.wordpress.android.ui.prefs.DetailListPreference;
import org.wordpress.android.ui.prefs.SiteSettingsFragment;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.ui.prefs.SummaryEditTextPreference;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPPrefUtils;

import de.greenrobot.event.EventBus;


public class PublicizeManageConnectionsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener, SiteSettingsInterface.SiteSettingsListener {
    private PublicizePreferenceListener mListener;
    private SummaryEditTextPreference mLabelPreference;
    private SiteSettingsInterface mSiteSettings;
    private DetailListPreference mButtonStylePreference;
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
        mButtonStylePreference = (DetailListPreference) getChangePref(R.string.publicize_button_style);
        mButtonStylePreference.setEntries(getResources().getStringArray(R.array.sharing_button_style_display_array));
        mButtonStylePreference.setEntryValues(getResources().getStringArray(R.array.sharing_button_style_array));

        mSiteSettings = SiteSettingsInterface.getInterface(getActivity(), mBlog, this);
    }

    private void setDetailListPreferenceValue(DetailListPreference pref, String value, String summary) {
        pref.setValue(value);
        pref.setSummary(summary);
        pref.refreshAdapter();
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
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mLabelPreference) {
            mSiteSettings.setSharingLabel(newValue.toString());
            changeEditTextPreferenceValue(mLabelPreference, newValue.toString());
        } else if (preference == mButtonStylePreference) {
            mSiteSettings.setDefaultFormat(newValue.toString());
            setDetailListPreferenceValue(mButtonStylePreference,
                    newValue.toString(),
                    mSiteSettings.getSharingButtonStyle(getActivity()));
        } else {
            return false;
        }

        mSiteSettings.saveSettings();

        return true;
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

    }

    private boolean shouldShowListPreference(DetailListPreference preference) {
        return preference != null && preference.getEntries() != null && preference.getEntries().length > 0;
    }

    @Override
    public void onSettingsUpdated(Exception error) {
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

        // update the global current Blog so WordPress.getCurrentBlog() callers will get the updated object
        WordPress.setCurrentBlog(mBlog.getLocalTableBlogId());

        EventBus.getDefault().post(new CoreEvents.BlogListChanged());
    }

    @Override
    public void onCredentialsValidated(Exception error) {

    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mButtonStylePreference) {
            return !shouldShowListPreference((DetailListPreference) preference);
        } else {
            return false;
        }
    }

    public interface PublicizePreferenceListener {
        void onPreferenceUpdated();
    }
}
