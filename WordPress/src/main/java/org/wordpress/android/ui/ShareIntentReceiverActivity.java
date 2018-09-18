package org.wordpress.android.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.ShareIntentReceiverFragment.ShareAction;
import org.wordpress.android.ui.ShareIntentReceiverFragment.ShareIntentFragmentListener;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPPermissionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * An activity to handle share intents, since there are multiple actions possible.
 * If the user is not logged in, redirects the user to the LoginFlow. When the user is logged in,
 * displays ShareIntentReceiverFragment. The fragment lets the user choose which blog to share to.
 * Moreover it lists what actions the user can perform and redirects the user to the activity,
 * along with the content passed in the intent.
 */
public class ShareIntentReceiverActivity extends AppCompatActivity implements ShareIntentFragmentListener {
    private static final String SHARE_LAST_USED_BLOG_ID_KEY = "wp-settings-share-last-used-text-blogid";
    private static final String KEY_SELECTED_SITE_LOCAL_ID = "KEY_SELECTED_SITE_LOCAL_ID";
    private static final String KEY_SHARE_ACTION_ID = "KEY_SHARE_ACTION_ID";

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    private int mClickedSiteLocalId;
    private String mShareActionName;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        refreshContent();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        setContentView(R.layout.share_intent_receiver_activity);

        if (savedInstanceState == null) {
            refreshContent();
        } else {
            loadState(savedInstanceState);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // keep setBackground in the onResume, otherwise the transition between activities is visible to the user when
        // sharing text with an account with one visible site
        findViewById(R.id.main_view).setBackgroundResource(R.color.login_background_color);
    }

    private void refreshContent() {
        if (FluxCUtils.isSignedInWPComOrHasWPOrgSite(mAccountStore, mSiteStore)) {
            List<SiteModel> visibleSites = mSiteStore.getVisibleSites();
            if (visibleSites.size() == 0) {
                ToastUtils.showToast(this, R.string.cant_share_no_visible_blog, ToastUtils.Duration.LONG);
                finish();
            } else if (visibleSites.size() == 1 && isSharingText()) {
                // if text/plain and only one blog, then don't show the fragment, share it directly to a new post
                share(ShareAction.SHARE_TO_POST, visibleSites.get(0).getId());
            } else {
                // display a fragment with list of sites and list of actions the user can perform
                initShareFragment(false);
            }
        } else {
            // start the login flow and wait onActivityResult
            ActivityLauncher.loginForShareIntent(this);
        }
    }

    private void initShareFragment(boolean afterLogin) {
        ShareIntentReceiverFragment shareIntentReceiverFragment = ShareIntentReceiverFragment
                .newInstance(!isSharingText(), loadLastUsedBlogLocalId(), afterLogin);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, shareIntentReceiverFragment, ShareIntentReceiverFragment.TAG)
                .commit();
    }

    private void loadState(Bundle savedInstanceState) {
        mClickedSiteLocalId = savedInstanceState.getInt(KEY_SELECTED_SITE_LOCAL_ID);
        mShareActionName = savedInstanceState.getString(KEY_SHARE_ACTION_ID);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putInt(KEY_SELECTED_SITE_LOCAL_ID, mClickedSiteLocalId);
        outState.putString(KEY_SHARE_ACTION_ID, mShareActionName);
    }

    private int loadLastUsedBlogLocalId() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        return settings.getInt(SHARE_LAST_USED_BLOG_ID_KEY, -1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCodes.DO_LOGIN) {
            if (resultCode == RESULT_OK) {
                // login successful
                refreshContent();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        boolean allGranted = WPPermissionUtils.setPermissionListAsked(
                this, requestCode, permissions, grantResults, true);
        if (allGranted && requestCode == WPPermissionUtils.SHARE_MEDIA_PERMISSION_REQUEST_CODE) {
            // permissions granted
            share(ShareAction.valueOf(mShareActionName), mClickedSiteLocalId);
        } else {
            Toast.makeText(this, R.string.share_media_permission_required, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void share(ShareAction shareAction, int selectedSiteLocalId) {
        if (checkAndRequestPermissions()) {
            bumpAnalytics(shareAction, selectedSiteLocalId);
            Intent intent = new Intent(this, shareAction.targetClass);
            startActivityAndFinish(intent, selectedSiteLocalId);
        } else {
            mShareActionName = shareAction.name();
            mClickedSiteLocalId = selectedSiteLocalId;
        }
    }

    private boolean isSharingText() {
        return "text/plain".equals(getIntent().getType());
    }

    private boolean checkAndRequestPermissions() {
        if (!isSharingText()) {
            // If we're sharing media, we must check we have Storage permission (needed for media upload).
            if (!PermissionUtils
                    .checkAndRequestStoragePermission(this, WPPermissionUtils.SHARE_MEDIA_PERMISSION_REQUEST_CODE)) {
                return false;
            }
        }
        return true;
    }

    private void startActivityAndFinish(@NonNull Intent intent, int mSelectedSiteLocalId) {
        String action = getIntent().getAction();
        intent.setAction(action);
        intent.setType(getIntent().getType());

        intent.putExtra(WordPress.SITE, mSiteStore.getSiteByLocalId(mSelectedSiteLocalId));

        intent.putExtra(MediaBrowserActivity.ARG_BROWSER_TYPE, MediaBrowserType.BROWSER);
        intent.putExtra(Intent.EXTRA_TEXT, getIntent().getStringExtra(Intent.EXTRA_TEXT));
        intent.putExtra(Intent.EXTRA_SUBJECT, getIntent().getStringExtra(Intent.EXTRA_SUBJECT));

        if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Uri> extra = getIntent().getParcelableArrayListExtra((Intent.EXTRA_STREAM));
            intent.putExtra(Intent.EXTRA_STREAM, extra);
        } else {
            Uri extra = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            intent.putExtra(Intent.EXTRA_STREAM, extra);
        }

        // save preferences
        PreferenceManager.getDefaultSharedPreferences(this)
                         .edit()
                         .putInt(SHARE_LAST_USED_BLOG_ID_KEY, mSelectedSiteLocalId)
                         .apply();

        startActivityWithSyntheticBackstack(intent);
        finish();
    }

    private void startActivityWithSyntheticBackstack(@NonNull Intent intent) {
        Intent parentIntent = new Intent(this, WPMainActivity.class);
        parentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        parentIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        parentIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        TaskStackBuilder.create(this).addNextIntent(parentIntent).addNextIntent(intent).startActivities();
    }

    private void bumpAnalytics(ShareAction shareAction, int selectedSiteLocalId) {
        SiteModel selectedSite = mSiteStore.getSiteByLocalId(selectedSiteLocalId);
        int numberOfMediaShared = countMedia();

        Map<String, Object> analyticsProperties = new HashMap<>();
        analyticsProperties.put("number_of_media_shared", numberOfMediaShared);
        analyticsProperties.put("share_to", shareAction.analyticsName);

        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.SHARE_TO_WP_SUCCEEDED,
                                            selectedSite,
                                            analyticsProperties);

        if (doesContainMediaAndWasSharedToMediaLibrary(shareAction, numberOfMediaShared)) {
            trackMediaAddedToMediaLibrary(selectedSite);
        }
    }

    private void trackMediaAddedToMediaLibrary(SiteModel selectedSite) {
        ArrayList<Uri> mediaUrls = new ArrayList<>();
        if (countMedia() == 1) {
            Uri singleMedia = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            mediaUrls.add(singleMedia);
        } else {
            ArrayList<Uri> imageUris = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            mediaUrls.addAll(imageUris);
        }

        for (Uri uri : mediaUrls) {
            if (uri != null) {
                String mimeType = getContentResolver().getType(uri);
                boolean isVideo = mimeType != null && mimeType.startsWith("video");
                Map<String, Object> properties = AnalyticsUtils.getMediaProperties(this, isVideo, uri, null);

                AnalyticsTracker.Stat mediaTypeTrack = isVideo ? AnalyticsTracker.Stat.MEDIA_LIBRARY_ADDED_VIDEO
                        : AnalyticsTracker.Stat.MEDIA_LIBRARY_ADDED_PHOTO;
                AnalyticsUtils.trackWithSiteDetails(mediaTypeTrack, selectedSite, properties);
            }
        }
    }

    private boolean doesContainMediaAndWasSharedToMediaLibrary(ShareAction shareAction, int numberOfMediaShared) {
        return shareAction != null && shareAction.analyticsName.equals(ShareAction.SHARE_TO_MEDIA_LIBRARY.analyticsName)
               && numberOfMediaShared > 0;
    }

    private int countMedia() {
        int mediaShared = 0;
        if (!isSharingText()) {
            String action = getIntent().getAction();
            if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                // Multiple pictures share to WP
                ArrayList<Uri> mediaUrls = getIntent().getParcelableArrayListExtra((Intent.EXTRA_STREAM));
                if (mediaUrls != null) {
                    mediaShared = mediaUrls.size();
                }
            } else {
                mediaShared = 1;
            }
        }
        return mediaShared;
    }
}
