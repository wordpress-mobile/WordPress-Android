package org.wordpress.android.ui.notifications;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;
import com.google.android.material.tabs.TabLayout.Tab;

import org.greenrobot.eventbus.EventBus;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.JetpackConnectionWebViewActivity;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.notifications.adapters.NotesAdapter;
import org.wordpress.android.ui.notifications.adapters.NotesAdapter.FILTERS;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.WPUrlUtils;
import org.wordpress.android.widgets.WPViewPager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import static org.wordpress.android.analytics.AnalyticsTracker.NOTIFICATIONS_SELECTED_FILTER;
import static org.wordpress.android.ui.JetpackConnectionSource.NOTIFICATIONS;
import static org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter.IS_TAPPED_ON_NOTIFICATION;
import static org.wordpress.android.ui.stats.StatsConnectJetpackActivity.FAQ_URL;

public class NotificationsListFragment extends Fragment {
    public static final String NOTE_ID_EXTRA = "noteId";
    public static final String NOTE_INSTANT_REPLY_EXTRA = "instantReply";
    public static final String NOTE_PREFILLED_REPLY_EXTRA = "prefilledReplyText";
    public static final String NOTE_MODERATE_ID_EXTRA = "moderateNoteId";
    public static final String NOTE_MODERATE_STATUS_EXTRA = "moderateNoteStatus";
    public static final String NOTE_CURRENT_LIST_FILTER_EXTRA = "currentFilter";

    protected static final int TAB_COUNT = 5;
    protected static final int TAB_POSITION_ALL = 0;
    protected static final int TAB_POSITION_UNREAD = 1;
    protected static final int TAB_POSITION_COMMENT = 2;
    protected static final int TAB_POSITION_FOLLOW = 3;
    protected static final int TAB_POSITION_LIKE = 4;

    private static final String KEY_LAST_TAB_POSITION = "lastTabPosition";

    private TabLayout mTabLayout;
    private ViewGroup mConnectJetpackView;
    private boolean mShouldRefreshNotifications;
    private int mLastTabPosition;

    @Nullable private Toolbar mToolbar = null;

    @Inject AccountStore mAccountStore;

    public static NotificationsListFragment newInstance() {
        return new NotificationsListFragment();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            setSelectedTab(savedInstanceState.getInt(KEY_LAST_TAB_POSITION, TAB_POSITION_ALL));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) requireActivity().getApplication()).component().inject(this);
        mShouldRefreshNotifications = true;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.notifications_list_fragment, container, false);
        setHasOptionsMenu(true);

        mConnectJetpackView = view.findViewById(R.id.connect_jetpack);

        mToolbar = view.findViewById(R.id.toolbar_main);
        mToolbar.setTitle(R.string.notifications_screen_title);
        ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);

        mTabLayout = view.findViewById(R.id.tab_layout);
        mTabLayout.addOnTabSelectedListener(new OnTabSelectedListener() {
            @Override
            public void onTabSelected(Tab tab) {
                Map<String, String> properties = new HashMap<>(1);

                switch (tab.getPosition()) {
                    case TAB_POSITION_ALL:
                        properties.put(NOTIFICATIONS_SELECTED_FILTER, FILTERS.FILTER_ALL.toString());
                        break;
                    case TAB_POSITION_COMMENT:
                        properties.put(NOTIFICATIONS_SELECTED_FILTER, FILTERS.FILTER_COMMENT.toString());
                        break;
                    case TAB_POSITION_FOLLOW:
                        properties.put(NOTIFICATIONS_SELECTED_FILTER, FILTERS.FILTER_FOLLOW.toString());
                        break;
                    case TAB_POSITION_LIKE:
                        properties.put(NOTIFICATIONS_SELECTED_FILTER, FILTERS.FILTER_LIKE.toString());
                        break;
                    case TAB_POSITION_UNREAD:
                        properties.put(NOTIFICATIONS_SELECTED_FILTER, FILTERS.FILTER_UNREAD.toString());
                        break;
                    default:
                        properties.put(NOTIFICATIONS_SELECTED_FILTER, FILTERS.FILTER_ALL.toString());
                        break;
                }

                AnalyticsTracker.track(Stat.NOTIFICATION_TAPPED_SEGMENTED_CONTROL, properties);
                mLastTabPosition = tab.getPosition();
            }

            @Override
            public void onTabUnselected(Tab tab) {
            }

            @Override
            public void onTabReselected(Tab tab) {
            }
        });

        WPViewPager viewPager = view.findViewById(R.id.view_pager);
        viewPager.setAdapter(new NotificationsFragmentAdapter(getChildFragmentManager(), buildTitles()));
        viewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.margin_extra_large));
        mTabLayout.setupWithViewPager(viewPager);

        TextView jetpackTermsAndConditions = view.findViewById(R.id.jetpack_terms_and_conditions);
        jetpackTermsAndConditions.setText(Html.fromHtml(String.format(
                getResources().getString(R.string.jetpack_connection_terms_and_conditions), "<u>", "</u>")));
        jetpackTermsAndConditions.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View view) {
                WPWebViewActivity.openURL(requireContext(), WPUrlUtils.buildTermsOfServiceUrl(getContext()));
            }
        });

        Button jetpackFaq = view.findViewById(R.id.jetpack_faq);
        jetpackFaq.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View view) {
                WPWebViewActivity.openURL(requireContext(), FAQ_URL);
            }
        });

        return view;
    }

    private List<String> buildTitles() {
        ArrayList<String> result = new ArrayList<>(TAB_COUNT);
        result.add(TAB_POSITION_ALL, getString(R.string.notifications_tab_title_all));
        result.add(TAB_POSITION_UNREAD, getString(R.string.notifications_tab_title_unread_notifications));
        result.add(TAB_POSITION_COMMENT, getString(R.string.notifications_tab_title_comments));
        result.add(TAB_POSITION_FOLLOW, getString(R.string.notifications_tab_title_follows));
        result.add(TAB_POSITION_LIKE, getString(R.string.notifications_tab_title_likes));
        return result;
    }

    @Override
    public void onPause() {
        super.onPause();
        mShouldRefreshNotifications = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().post(new NotificationEvents.NotificationsUnseenStatus(false));

        if (!mAccountStore.hasAccessToken()) {
            showConnectJetpackView();
            mTabLayout.setVisibility(View.GONE);
        } else {
            if (mShouldRefreshNotifications) {
                fetchNotesFromRemote();
            }
        }

        setSelectedTab(mLastTabPosition);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(KEY_LAST_TAB_POSITION, mLastTabPosition);
        super.onSaveInstanceState(outState);
    }

    private void clearToolbarScrollFlags() {
        if (mToolbar != null && mToolbar.getLayoutParams() instanceof AppBarLayout.LayoutParams) {
            AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
            params.setScrollFlags(0);
        }
    }

    private void fetchNotesFromRemote() {
        if (!isAdded()) {
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            return;
        }

        NotificationsUpdateServiceStarter.startService(getActivity());
    }

    private static Intent getOpenNoteIntent(Activity activity, String noteId) {
        Intent detailIntent = new Intent(activity, NotificationsDetailActivity.class);
        detailIntent.putExtra(NOTE_ID_EXTRA, noteId);
        return detailIntent;
    }

    public SiteModel getSelectedSite() {
        if (getActivity() instanceof WPMainActivity) {
            WPMainActivity mainActivity = (WPMainActivity) getActivity();
            return mainActivity.getSelectedSite();
        }

        return null;
    }

    public static void openNoteForReply(Activity activity, String noteId, boolean shouldShowKeyboard, String replyText,
                                        NotesAdapter.FILTERS filter, boolean isTappedFromPushNotification) {
        if (noteId == null || activity == null) {
            return;
        }

        if (activity.isFinishing()) {
            return;
        }

        Intent detailIntent = getOpenNoteIntent(activity, noteId);
        detailIntent.putExtra(NOTE_INSTANT_REPLY_EXTRA, shouldShowKeyboard);

        if (!TextUtils.isEmpty(replyText)) {
            detailIntent.putExtra(NOTE_PREFILLED_REPLY_EXTRA, replyText);
        }

        detailIntent.putExtra(NOTE_CURRENT_LIST_FILTER_EXTRA, filter);
        detailIntent.putExtra(IS_TAPPED_ON_NOTIFICATION, isTappedFromPushNotification);
        openNoteForReplyWithParams(detailIntent, activity);
    }

    private static void openNoteForReplyWithParams(Intent detailIntent, Activity activity) {
        activity.startActivityForResult(detailIntent, RequestCodes.NOTE_DETAIL);
    }

    private void setSelectedTab(int position) {
        mLastTabPosition = position;

        if (mTabLayout != null) {
            TabLayout.Tab tab = mTabLayout.getTabAt(mLastTabPosition);

            if (tab != null) {
                tab.select();
            }
        }
    }

    private void showConnectJetpackView() {
        if (isAdded() && mConnectJetpackView != null) {
            mConnectJetpackView.setVisibility(View.VISIBLE);
            mTabLayout.setVisibility(View.GONE);
            clearToolbarScrollFlags();

            Button setupButton = mConnectJetpackView.findViewById(R.id.jetpack_setup);
            setupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SiteModel siteModel = getSelectedSite();
                    JetpackConnectionWebViewActivity
                            .startJetpackConnectionFlow(getActivity(), NOTIFICATIONS, siteModel, false);
                }
            });
        }
    }

    private static class NotificationsFragmentAdapter extends FragmentPagerAdapter {

        private List<String> mTitles;

        public NotificationsFragmentAdapter(@NonNull FragmentManager fm, List<String> titles) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            mTitles = titles;
        }

        @Override
        public int getCount() {
            return TAB_COUNT;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return NotificationsListFragmentPage.newInstance(position);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            if (mTitles.size() > position && position >= 0) {
                return mTitles.get(position);
            }
            return super.getPageTitle(position);
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            try {
                super.restoreState(state, loader);
            } catch (IllegalStateException exception) {
                AppLog.e(T.NOTIFS, exception);
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem notificationSettings = menu.findItem(R.id.notifications_settings);
        notificationSettings.setVisible(mAccountStore.hasAccessToken());

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.notifications_list_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.notifications_settings) {
            ActivityLauncher.viewNotificationsSettings(getActivity());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
