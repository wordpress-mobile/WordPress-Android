package org.wordpress.android.ui.publicize;

import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import org.wordpress.android.R;
import org.wordpress.android.ui.prefs.SiteSettingsFragment;
import org.wordpress.android.ui.prefs.SummaryEditTextPreference;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPPrefUtils;


public class PublicizeManageConnectionsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    private PublicizePreferenceListener mListener;
    private SummaryEditTextPreference mLabelPreference;

    public PublicizeManageConnectionsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.publicize_preferences);

        mLabelPreference = (SummaryEditTextPreference) getChangePref(R.string.publicize_label);
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

    private void changeEditTextPreferenceValue(EditTextPreference pref, String newValue) {
        if (newValue == null || pref == null || pref.getEditText().isInEditMode()) return;

        if (!newValue.equals(pref.getSummary())) {
            String formattedValue = StringUtils.unescapeHTML(newValue.replaceFirst(SiteSettingsFragment.ADDRESS_FORMAT_REGEX, ""));

            pref.setText(formattedValue);
            pref.setSummary(formattedValue);
        }
    }

    public interface PublicizePreferenceListener {
        void onPreferenceUpdated();
    }
}
