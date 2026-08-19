package org.wordpress.android.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.TaskStackBuilder;
import androidx.preference.PreferenceManager;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.ShareIntentReceiverFragment.ShareAction;
import org.wordpress.android.ui.ShareIntentReceiverFragment.ShareIntentFragmentListener;
import org.wordpress.android.ui.main.BaseAppCompatActivity;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.posts.EditorLauncher;
import org.wordpress.android.ui.posts.EditorLauncherParams;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import static org.wordpress.android.fluxc.utils.MediaUtils.getExtension;
import static org.wordpress.android.fluxc.utils.MediaUtils.getMimeTypeForExtension;
import static org.wordpress.android.fluxc.utils.MediaUtils.isSupportedImageMimeType;
import static org.wordpress.android.fluxc.utils.MediaUtils.isSupportedVideoMimeType;

/**
 * An activity to handle share intents, since there are multiple actions possible.
 * If the user is not logged in, redirects the user to the LoginFlow. When the user is logged in,
 * displays ShareIntentReceiverFragment. The fragment lets the user choose which blog to share to.
 * Moreover it lists what actions the user can perform and redirects the user to the activity,
 * along with the content passed in the intent.
 */
public class ShareIntentReceiverActivity extends BaseAppCompatActivity implements ShareIntentFragmentListener {
    private static final String SHARE_LAST_USED_BLOG_ID_KEY = "wp-settings-share-last-used-text-blogid";
    private static final String KEY_SELECTED_SITE_LOCAL_ID = "KEY_SELECTED_SITE_LOCAL_ID";
    private static final String KEY_SHARE_ACTION_ID = "KEY_SHARE_ACTION_ID";
    private static final String KEY_LOCAL_MEDIA_URIS = "KEY_LOCAL_MEDIA_URIS";

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    private int mClickedSiteLocalId;
    private String mShareActionName;
    private ArrayList<Uri> mLocalMediaUris = new ArrayList<>();

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        refreshContent();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        setContentView(R.layout.share_intent_receiver_activity);

        if (savedInstanceState == null) {
            refreshContent();
        } else {
            loadState(savedInstanceState);
        }
    }

    private void refreshContent() {
        if (FluxCUtils.isSignedInWPComOrHasWPOrgSite(mAccountStore, mSiteStore)) {
            List<SiteModel> sites = mSiteStore.getSites();
            downloadExternalMedia();
            if (sites.isEmpty()) {
                ToastUtils.showToast(this, R.string.cant_share_no_blog, ToastUtils.Duration.LONG);
                finish();
            } else if (sites.size() == 1 && isSharingText()) {
                // if text/plain and only one blog, then don't show the fragment, share it directly to a new post
                share(ShareAction.SHARE_TO_POST, sites.get(0).getId());
            } else {
                // display a fragment with list of sites and list of actions the user can perform
                initShareFragment();
            }
        } else {
            // start the login flow and wait onActivityResult
            ActivityLauncher.loginForShareIntent(this);
        }
    }

    private void downloadExternalMedia() {
        // This activity is singleTask, so a second share reuses the same instance via onNewIntent().
        // Start from an empty list, otherwise media from a previous, abandoned share is sent instead.
        mLocalMediaUris.clear();

        boolean anyDownloadFailed = false;
        try {
            if (Intent.ACTION_SEND_MULTIPLE.equals(getIntent().getAction())) {
                ArrayList<Uri> externalUris = getIntent().getParcelableArrayListExtra((Intent.EXTRA_STREAM));
                for (Uri uri : externalUris) {
                    if (uri != null && isAllowedMediaType(uri)) {
                        anyDownloadFailed |= !addLocalMediaUri(uri);
                    }
                }
            } else if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
                Uri externalUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
                if (externalUri != null && isAllowedMediaType(externalUri)) {
                    anyDownloadFailed = !addLocalMediaUri(externalUri);
                }
            }
        } catch (Exception e) {
            ToastUtils.showToast(this,
                    R.string.error_media_could_not_share_media_from_device, ToastUtils.Duration.LONG);
            AppLog.e(T.MEDIA, "ShareIntentReceiver failed to download media ", e);
            return;
        }

        if (anyDownloadFailed) {
            ToastUtils.showToast(this,
                    R.string.error_media_could_not_share_media_from_device, ToastUtils.Duration.LONG);
        }
    }

    /**
     * Copies the shared media to local storage and queues it for sharing.
     *
     * @return false when the copy failed, so the caller can tell the user instead of silently
     * dropping the media. A null entry would otherwise reach the target activity as a null
     * EXTRA_STREAM and be skipped without any feedback.
     */
    private boolean addLocalMediaUri(@NonNull Uri uri) {
        Uri localUri = MediaUtils.downloadExternalMedia(this, uri);
        if (localUri == null) {
            AppLog.e(T.MEDIA, "ShareIntentReceiver failed to download media " + uri);
            return false;
        }
        mLocalMediaUris.add(withFileExtension(localUri, getContentResolver().getType(uri)));
        return true;
    }

    /**
     * Renames the cached copy so its name carries a file extension, using the MIME type the provider
     * reported for the shared URI.
     *
     * <p>downloadExternalMedia() names the copy after the provider's display name, falling back to a
     * MIME type parsed out of the URI string - which a content:// URI never carries. A provider that
     * reports no display name, or one without an extension, therefore leaves the copy with no
     * extension at all. Every MIME check downstream reads the file name, so the media ends up
     * uploaded as image/jpeg no matter what was actually shared.
     *
     * @return the renamed URI, or the original one when there is nothing to repair or the rename
     * fails. This only ever improves on what we already have, so it must not introduce a failure.
     */
    @NonNull
    private Uri withFileExtension(@NonNull Uri localUri, @Nullable String mimeType) {
        String path = localUri.getPath();
        if (path == null || mimeType == null) {
            return localUri;
        }

        File localFile = new File(path);
        if (getExtension(localFile.getName()) != null) {
            return localUri;
        }

        // getExtensionForMimeType() never fails: when the MIME type is unknown it returns the
        // subtype, so "application/octet-stream" would name the file ".octet-stream". That reads as
        // a real extension downstream and suppresses the image/jpeg fallback in
        // FluxCUtils.mediaModelFromLocalUri() that would otherwise have made the upload work, so
        // only rename when the extension maps back to a MIME type. Check that against the same
        // table isAllowedMediaType() accepts from, rather than MimeTypeMap, whose coverage of
        // heic/heif/ogv/3g2 varies by API level.
        String extension = MediaUtils.getExtensionForMimeType(mimeType);
        if (TextUtils.isEmpty(extension) || getMimeTypeForExtension(extension) == null) {
            return localUri;
        }

        // an extension-less name still ends in a dot when downloadExternalMedia() had none to append
        String renamedPath = (path.endsWith(".") ? path.substring(0, path.length() - 1) : path) + "." + extension;
        // renameTo() overwrites, and an earlier share may still be uploading from that name
        File renamedFile = new File(renamedPath);
        if (renamedFile.exists() || !localFile.renameTo(renamedFile)) {
            AppLog.w(T.MEDIA, "ShareIntentReceiver could not rename " + path + " to " + renamedPath);
            return localUri;
        }
        return Uri.fromFile(renamedFile);
    }

    private boolean isAllowedMediaType(@NonNull Uri uri) {
        // Try the MIME type reported by the provider first: photo picker URIs have no file extension
        // and no readable _data column, so the path-based check below can't recognize them. A provider
        // may still report an unhelpful type (application/octet-stream), so treat this as an early
        // accept rather than a rejection and fall through to the path check when it doesn't match.
        String mimeType = getContentResolver().getType(uri);
        if (mimeType != null && (isSupportedImageMimeType(mimeType) || isSupportedVideoMimeType(mimeType))) {
            return true;
        }

        String filePath = MediaUtils.getRealPathFromURI(this, uri);
        // For cases when getRealPathFromURI returns an empty string
        if (TextUtils.isEmpty(filePath)) {
            filePath = String.valueOf(uri);
        }
        return MediaUtils.isValidImage(filePath) || MediaUtils.isVideo(filePath);
    }

    private void initShareFragment() {
        ShareIntentReceiverFragment shareIntentReceiverFragment = ShareIntentReceiverFragment
                .newInstance(!isSharingText(), loadLastUsedBlogLocalId());
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, shareIntentReceiverFragment, ShareIntentReceiverFragment.TAG)
                .commit();
    }

    private void loadState(Bundle savedInstanceState) {
        mClickedSiteLocalId = savedInstanceState.getInt(KEY_SELECTED_SITE_LOCAL_ID);
        mShareActionName = savedInstanceState.getString(KEY_SHARE_ACTION_ID);
        mLocalMediaUris = savedInstanceState.getParcelableArrayList(KEY_LOCAL_MEDIA_URIS);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_SITE_LOCAL_ID, mClickedSiteLocalId);
        outState.putString(KEY_SHARE_ACTION_ID, mShareActionName);
        outState.putParcelableArrayList(KEY_LOCAL_MEDIA_URIS, mLocalMediaUris);
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
    public void share(ShareAction shareAction, int selectedSiteLocalId) {
        mShareActionName = shareAction.name();
        mClickedSiteLocalId = selectedSiteLocalId;

        bumpAnalytics(shareAction, selectedSiteLocalId);

        SiteModel selectedSite = mSiteStore.getSiteByLocalId(selectedSiteLocalId);
        if (selectedSite == null) {
            ToastUtils.showToast(this, R.string.cant_share_no_blog, ToastUtils.Duration.LONG);
            finish();
            return;
        }

        Intent intent;

        switch (shareAction) {
            case SHARE_TO_POST:
                EditorLauncherParams params = EditorLauncherParams.Builder.forSite(selectedSite)
                        .build();
                intent = EditorLauncher.getInstance().createEditorIntent(this, params);
                break;
            case SHARE_TO_MEDIA_LIBRARY:
                intent = new Intent(this, MediaBrowserActivity.class);
                intent.putExtra(WordPress.SITE, selectedSite);
                break;
            default:
                throw new IllegalArgumentException("Unknown share action: " + shareAction);
        }

        startActivityAndFinish(intent, selectedSiteLocalId);
    }

    private boolean isSharingText() {
        return "text/plain".equals(getIntent().getType());
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
            intent.putExtra(Intent.EXTRA_STREAM, mLocalMediaUris);
        } else if (Intent.ACTION_SEND.equals(action) && !mLocalMediaUris.isEmpty()) {
            intent.putExtra(Intent.EXTRA_STREAM, mLocalMediaUris.get(0));
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
        for (Uri uri : mLocalMediaUris) {
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
                mediaShared = mLocalMediaUris.size();
            } else {
                mediaShared = 1;
            }
        }
        return mediaShared;
    }
}
