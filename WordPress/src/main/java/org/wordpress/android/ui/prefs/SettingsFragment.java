package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
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
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPPrefUtils;

import java.util.HashMap;

import de.greenrobot.event.EventBus;

public abstract class SettingsFragment extends PreferenceFragment implements
            ViewGroup.OnHierarchyChangeListener,
            Preference.OnPreferenceChangeListener,
            Preference.OnPreferenceClickListener,
            AdapterView.OnItemLongClickListener,
            SiteSettingsInterface.SiteSettingsListener {
    protected abstract void initPreferences();
    protected abstract void setEditingEnabled(boolean enabled);
    protected abstract void setPreferencesFromSiteSettings();
    protected abstract void addPreferencesFromResource();

    /**
     * Use this argument to pass the {@link Integer} local blog ID to this fragment.
     */
    public static final String ARG_LOCAL_BLOG_ID = "local_blog_id";

    /**
     * Provides the regex to identify domain HTTP(S) protocol and/or 'www' sub-domain.
     *
     * Used to format user-facing {@link String}'s in certain preferences.
     */
    public static final String ADDRESS_FORMAT_REGEX = "^(https?://(w{3})?|www\\.)";

    private static final long FETCH_DELAY = 1000;

    protected SiteSettingsInterface mSiteSettings;
    protected boolean mShouldFetch;

    // Reference to blog obtained from passed ID (ARG_LOCAL_BLOG_ID)
    protected Blog mBlog;

    // Reference to the state of the fragment
    protected boolean mIsFragmentPaused = false;

    protected boolean mEditingEnabled = true;

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
        addPreferencesFromResource();

        // toggle which preferences are shown and set references
        initPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Fragment#onResume() is called after FragmentActivity#onPostResume().
        // The latter is the most secure way of keeping track of the activity's state, and avoid calls to commitAllowingStateLoss.
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
    public void onPause() {
        super.onPause();
        WordPress.wpDB.saveBlog(mBlog);
        mIsFragmentPaused = true;
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

    protected void setupPreferenceList(ListView prefList, Resources res) {
        if (prefList == null || res == null) return;

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

    protected void setDetailListPreferenceValue(DetailListPreference pref, String value, String summary) {
        pref.setValue(value);
        pref.setSummary(summary);
        pref.refreshAdapter();
    }

    protected boolean shouldShowListPreference(DetailListPreference preference) {
        return preference != null && preference.getEntries() != null && preference.getEntries().length > 0;
    }

    protected Preference getChangePref(int id) {
        return WPPrefUtils.getPrefAndSetChangeListener(this, id, this);
    }

    protected Preference getClickPref(int id) {
        return WPPrefUtils.getPrefAndSetClickListener(this, id, this);
    }

    /**
     * Helper method to perform validation and set multiple properties on an EditTextPreference.
     * If newValue is equal to the current preference text no action will be taken.
     */
    protected void changeEditTextPreferenceValue(EditTextPreference pref, String newValue) {
        if (newValue == null || pref == null || pref.getEditText().isInEditMode()) return;

        if (!newValue.equals(pref.getSummary())) {
            String formattedValue = StringUtils.unescapeHTML(newValue.replaceFirst(ADDRESS_FORMAT_REGEX, ""));

            pref.setText(formattedValue);
            pref.setSummary(formattedValue);
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
            TextView title = (TextView) child.findViewById(android.R.id.title);
            if (title != null) WPPrefUtils.layoutAsSubhead(title);
        }
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
        // NOP
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
    public void onSettingsUpdated(Exception error) {
        if (error != null) {
            ToastUtils.showToast(getActivity(), R.string.error_fetch_remote_site_settings);
            getActivity().finish();
            return;
        }

        if (isAdded()) setPreferencesFromSiteSettings();
    }

    @Override
    public void onCredentialsValidated(Exception error) {
        if (error != null) {
            ToastUtils.showToast(WordPress.getContext(), R.string.username_or_password_incorrect);
        }
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
}
