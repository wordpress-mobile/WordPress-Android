package org.wordpress.android.ui.main;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Outline;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Request;
import com.github.xizzhu.simpletooltip.ToolTip;
import com.github.xizzhu.simpletooltip.ToolTipView;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.networking.GravatarApi;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.HelpshiftHelper.Tag;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.passcodelock.AppLockManager;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class MeFragment extends Fragment {
    private static final String IS_DISCONNECTING = "IS_DISCONNECTING";
    private static final String IS_UPDATING_GRAVATAR = "IS_UPDATING_GRAVATAR";

    private static final int CAMERA_AND_MEDIA_PERMISSION_REQUEST_CODE = 1;

    private ViewGroup mAvatarFrame;
    private View mProgressBar;
    private ToolTipView mGravatarToolTipView;
    private View mAvatarTooltipAnchor;
    private ViewGroup mAvatarContainer;
    private WPNetworkImageView mAvatarImageView;
    private TextView mDisplayNameTextView;
    private TextView mUsernameTextView;
    private TextView mLoginLogoutTextView;
    private View mMyProfileView;
    private View mAccountSettingsView;
    private View mNotificationsView;
    private View mNotificationsDividerView;
    private ProgressDialog mDisconnectProgressDialog;

    // setUserVisibleHint is not available so we need to manually handle the UserVisibleHint state
    private boolean mIsUserVisible;

    private boolean mIsUpdatingGravatar;

    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    public static MeFragment newInstance() {
        return new MeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (savedInstanceState != null) {
            mIsUpdatingGravatar = savedInstanceState.getBoolean(IS_UPDATING_GRAVATAR);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        mIsUserVisible = isVisibleToUser;

        if (isResumed()) {
            showGravatarTooltipIfNeeded();
        }
    }

    private void showGravatarTooltipIfNeeded() {
        if (!isAdded() || !mAccountStore.hasAccessToken() || !AppPrefs.isGravatarChangePromoRequired() ||
                !mIsUserVisible || mGravatarToolTipView != null) {
            return;
        }

        ToolTip toolTip = createGravatarPromoToolTip(getString(R.string.gravatar_tip), ContextCompat.getColor
                (getActivity(), R.color.color_primary));
        mGravatarToolTipView = new ToolTipView.Builder(getActivity())
                .withAnchor(mAvatarTooltipAnchor)
                .withToolTip(toolTip)
                .withGravity(Gravity.END)
                .build();
        mGravatarToolTipView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnalyticsTracker.track(AnalyticsTracker.Stat.ME_GRAVATAR_TOOLTIP_TAPPED);

                mGravatarToolTipView.remove();
                AppPrefs.setGravatarChangePromoRequired(false);
            }
        });
        mGravatarToolTipView.showDelayed(500);
    }

    private ToolTip createGravatarPromoToolTip(CharSequence text, int backgroundColor) {
        Resources resources = getResources();
        int padding = resources.getDimensionPixelSize(R.dimen.tooltip_padding);
        int textSize = resources.getDimensionPixelSize(R.dimen.tooltip_text_size);
        int radius = resources.getDimensionPixelSize(R.dimen.tooltip_radius);
        return new ToolTip.Builder()
                .withText(text)
                .withTextColor(Color.WHITE)
                .withTextSize(textSize)
                .withBackgroundColor(backgroundColor)
                .withPadding(padding, padding, padding, padding)
                .withCornerRadius(radius)
                .build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.me_fragment, container, false);

        mAvatarFrame = (ViewGroup) rootView.findViewById(R.id.frame_avatar);
        mAvatarContainer = (ViewGroup) rootView.findViewById(R.id.avatar_container);
        mAvatarImageView = (WPNetworkImageView) rootView.findViewById(R.id.me_avatar);
        mAvatarTooltipAnchor = rootView.findViewById(R.id.avatar_tooltip_anchor);
        mProgressBar = rootView.findViewById(R.id.avatar_progress);
        mDisplayNameTextView = (TextView) rootView.findViewById(R.id.me_display_name);
        mUsernameTextView = (TextView) rootView.findViewById(R.id.me_username);
        mLoginLogoutTextView = (TextView) rootView.findViewById(R.id.me_login_logout_text_view);
        mMyProfileView = rootView.findViewById(R.id.row_my_profile);
        mAccountSettingsView = rootView.findViewById(R.id.row_account_settings);
        mNotificationsView = rootView.findViewById(R.id.row_notifications);
        mNotificationsDividerView = rootView.findViewById(R.id.me_notifications_divider);

        addDropShadowToAvatar();

        mAvatarContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnalyticsTracker.track(AnalyticsTracker.Stat.ME_GRAVATAR_TAPPED);

                // User tapped the Gravatar so dismiss the tooltip
                if (mGravatarToolTipView != null) {
                    mGravatarToolTipView.remove();
                }
                // and no need to promote the feature any more
                AppPrefs.setGravatarChangePromoRequired(false);

                if (PermissionUtils.checkAndRequestCameraAndStoragePermissions(MeFragment.this,
                        CAMERA_AND_MEDIA_PERMISSION_REQUEST_CODE)) {
                    showPhotoPickerForGravatar();
                } else {
                    AppLockManager.getInstance().setExtendedTimeout();
                }
            }
        });
        mMyProfileView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewMyProfile(getActivity());
            }
        });

        mAccountSettingsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewAccountSettings(getActivity());
            }
        });

        rootView.findViewById(R.id.row_app_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewAppSettings(getActivity());
            }
        });

        mNotificationsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewNotificationsSettings(getActivity());
            }
        });

        rootView.findViewById(R.id.row_support).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewHelpAndSupport(getActivity(), Tag.ORIGIN_ME_SCREEN_HELP);
            }
        });

        rootView.findViewById(R.id.row_logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAccountStore.hasAccessToken()) {
                    signOutWordPressComWithConfirmation();
                } else {
                    ActivityLauncher.showSignInForResult(getActivity());
                }
            }
        });

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(IS_DISCONNECTING, false)) {
                showDisconnectDialog(getActivity());
            }

            if (savedInstanceState.getBoolean(IS_UPDATING_GRAVATAR, false)) {
                showGravatarProgressBar(true);
            }
        }

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mDisconnectProgressDialog != null) {
            outState.putBoolean(IS_DISCONNECTING, true);
        }
        outState.putBoolean(IS_UPDATING_GRAVATAR, mIsUpdatingGravatar);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        mDispatcher.unregister(this);
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAccountDetails();
        showGravatarTooltipIfNeeded();
    }

    @Override
    public void onDestroy() {
        if (mDisconnectProgressDialog != null) {
            mDisconnectProgressDialog.dismiss();
            mDisconnectProgressDialog = null;
        }
        super.onDestroy();
    }

    /**
     * adds a circular drop shadow to the avatar's parent view (Lollipop+ only)
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void addDropShadowToAvatar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mAvatarContainer.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    int padding = (mAvatarContainer.getWidth() - mAvatarImageView.getWidth()) / 2;
                    outline.setOval(padding, padding, view.getWidth() - padding, view.getHeight() - padding);
                }
            });
            mAvatarContainer.setElevation(mAvatarContainer.getResources().getDimensionPixelSize(R.dimen.card_elevation));
        }
    }

    private void refreshAccountDetails() {
        if (!FluxCUtils.isSignedInWPComOrHasWPOrgSite(mAccountStore, mSiteStore)) {
            return;
        }
        // we only want to show user details for WordPress.com users
        if (mAccountStore.hasAccessToken()) {
            AccountModel defaultAccount = mAccountStore.getAccount();

            mDisplayNameTextView.setVisibility(View.VISIBLE);
            mUsernameTextView.setVisibility(View.VISIBLE);
            mAvatarFrame.setVisibility(View.VISIBLE);
            mMyProfileView.setVisibility(View.VISIBLE);
            mNotificationsView.setVisibility(View.VISIBLE);
            mNotificationsDividerView.setVisibility(View.VISIBLE);

            final String avatarUrl = constructGravatarUrl(mAccountStore.getAccount());
            loadAvatar(avatarUrl, null);

            mUsernameTextView.setText("@" + defaultAccount.getUserName());
            mLoginLogoutTextView.setText(R.string.me_disconnect_from_wordpress_com);

            String displayName = StringUtils.unescapeHTML(defaultAccount.getDisplayName());
            if (!TextUtils.isEmpty(displayName)) {
                mDisplayNameTextView.setText(displayName);
            } else {
                mDisplayNameTextView.setText(defaultAccount.getUserName());
            }
        } else {
            mDisplayNameTextView.setVisibility(View.GONE);
            mUsernameTextView.setVisibility(View.GONE);
            mAvatarFrame.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.GONE);
            mMyProfileView.setVisibility(View.GONE);
            mAccountSettingsView.setVisibility(View.GONE);
            mNotificationsView.setVisibility(View.GONE);
            mNotificationsDividerView.setVisibility(View.GONE);
            mLoginLogoutTextView.setText(R.string.me_connect_to_wordpress_com);
        }
    }

    private void showGravatarProgressBar(boolean isUpdating) {
        mProgressBar.setVisibility(isUpdating ? View.VISIBLE : View.GONE);
        mIsUpdatingGravatar = isUpdating;
    }

    private String constructGravatarUrl(AccountModel account) {
        int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
        return GravatarUtils.fixGravatarUrl(account.getAvatarUrl(), avatarSz);
    }

    private void loadAvatar(String avatarUrl, String injectFilePath) {
        if (injectFilePath != null && !injectFilePath.isEmpty()) {
            // invalidate the specific gravatar entry from the bitmap cache. It will be updated via the injected
            // request cache.
            WordPress.getBitmapCache().removeSimilar(avatarUrl);

            try {
                // fool the network requests cache by injecting the new image. The Gravatar backend (plus CDNs)
                // can't be trusted to have updated the image quick enough.
                injectCache(new File(injectFilePath), avatarUrl);
            } catch (IOException e) {
                EventBus.getDefault().post(new GravatarLoadFinished(false));
            }

            // reset the WPNetworkImageView
            mAvatarImageView.resetImage();
            mAvatarImageView.removeCurrentUrlFromSkiplist();
        }

        mAvatarImageView.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR, new WPNetworkImageView
                .ImageLoadListener() {
            @Override
            public void onLoaded() {
                EventBus.getDefault().post(new GravatarLoadFinished(true));
            }

            @Override
            public void onError() {
                EventBus.getDefault().post(new GravatarLoadFinished(false));
            }
        });
    }

    private void signOutWordPressComWithConfirmation() {
        String message = String.format(getString(R.string.sign_out_wpcom_confirm),
                mAccountStore.getAccount().getUserName());

        new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton(R.string.signout, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        signOutWordPressCom();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
                .create().show();
    }

    private void signOutWordPressCom() {
        // note that signing out sends a CoreEvents.UserSignedOutWordPressCom EventBus event,
        // which will cause the main activity to recreate this fragment
        (new SignOutWordPressComAsync(getActivity())).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void showDisconnectDialog(Context context) {
        mDisconnectProgressDialog = ProgressDialog.show(context, null, context.getText(R.string.signing_out), false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[]
            grantResults) {
        switch (requestCode) {
            case CAMERA_AND_MEDIA_PERMISSION_REQUEST_CODE:
                if (permissions.length == 0) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.ME_GRAVATAR_PERMISSIONS_INTERRUPTED);
                }  else {
                    List<String> granted = new ArrayList<>();
                    List<String> denied = new ArrayList<>();

                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            granted.add(permissions[i]);
                        } else {
                            denied.add(permissions[i]);
                        }
                    }

                    if (denied.size() == 0) {
                        AnalyticsTracker.track(AnalyticsTracker.Stat.ME_GRAVATAR_PERMISSIONS_ACCEPTED);
                        showPhotoPickerForGravatar();
                    } else {
                        ToastUtils.showToast(this.getActivity(), getString(R.string
                                .gravatar_camera_and_media_permission_required), ToastUtils.Duration.LONG);
                        Map<String, Object> properties = new HashMap<>();
                        properties.put("permissions granted", granted.size() == 0 ? "[none]" : TextUtils
                                .join(",", granted));
                        properties.put("permissions denied", TextUtils.join(",", denied));
                        AnalyticsTracker.track(AnalyticsTracker.Stat.ME_GRAVATAR_PERMISSIONS_DENIED, properties);
                    }
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RequestCodes.PHOTO_PICKER:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String strMediaUri = data.getStringExtra(PhotoPickerActivity.EXTRA_MEDIA_URI);
                    if (strMediaUri == null) {
                        AppLog.e(AppLog.T.UTILS, "Can't resolve picked or captured image");
                        return;
                    }
                    PhotoPickerMediaSource source = PhotoPickerMediaSource.fromString(
                            data.getStringExtra(PhotoPickerActivity.EXTRA_MEDIA_SOURCE));
                    AnalyticsTracker.Stat stat =
                            source == PhotoPickerMediaSource.ANDROID_CAMERA
                                    ? AnalyticsTracker.Stat.ME_GRAVATAR_SHOT_NEW
                                    : AnalyticsTracker.Stat.ME_GRAVATAR_GALLERY_PICKED;
                    AnalyticsTracker.track(stat);
                    Uri imageUri = Uri.parse(strMediaUri);
                    startCropActivity(imageUri);
                }
                break;
            case UCrop.REQUEST_CROP:
                AnalyticsTracker.track(AnalyticsTracker.Stat.ME_GRAVATAR_CROPPED);

                if (resultCode == Activity.RESULT_OK) {
                    fetchMedia(UCrop.getOutput(data));
                } else if (resultCode == UCrop.RESULT_ERROR) {
                    Toast.makeText(getActivity(), getString(R.string.error_cropping_image), Toast.LENGTH_SHORT).show();

                    final Throwable cropError = UCrop.getError(data);
                    AppLog.e(AppLog.T.MAIN, "Image cropping failed!", cropError);
                }
                break;
        }
    }

    private void showPhotoPickerForGravatar() {
        ActivityLauncher.showPhotoPickerForResult(getActivity());
    }

    private void startCropActivity(Uri uri) {
        final Context context = getActivity();

        if (context == null) {
            return;
        }

        UCrop.Options options = new UCrop.Options();
        options.setShowCropGrid(false);
        options.setStatusBarColor(ContextCompat.getColor(context, R.color.status_bar_tint));
        options.setToolbarColor(ContextCompat.getColor(context, R.color.color_primary));
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.NONE, UCropActivity.NONE);
        options.setHideBottomControls(true);

        UCrop.of(uri, Uri.fromFile(new File(context.getCacheDir(), "cropped_for_gravatar.jpg")))
                .withAspectRatio(1, 1)
                .withOptions(options)
                .start(getActivity(), this);
    }

    private void fetchMedia(Uri mediaUri) {
        if (!MediaUtils.isInMediaStore(mediaUri)) {
            // Do not download the file in async task. See https://github.com/wordpress-mobile/WordPress-Android/issues/5818
            Uri downloadedUri = MediaUtils.downloadExternalMedia(getActivity(), mediaUri);
            if (downloadedUri != null) {
                startGravatarUpload(MediaUtils.getRealPathFromURI(getActivity(), downloadedUri));
            } else {
                Toast.makeText(getActivity(), getString(R.string.error_downloading_image), Toast.LENGTH_SHORT).show();
            }
        } else {
            // It is a regular local media file
            startGravatarUpload(MediaUtils.getRealPathFromURI(getActivity(), mediaUri));
        }
    }

    private void startGravatarUpload(final String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            Toast.makeText(getActivity(), getString(R.string.error_locating_image), Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            Toast.makeText(getActivity(), getString(R.string.error_locating_image), Toast.LENGTH_SHORT).show();
            return;
        }

        showGravatarProgressBar(true);

        GravatarApi.uploadGravatar(file, mAccountStore.getAccount().getEmail(), mAccountStore.getAccessToken(),
                new GravatarApi.GravatarUploadListener() {
                    @Override
                    public void onSuccess() {
                        EventBus.getDefault().post(new GravatarUploadFinished(filePath, true));
                    }

                    @Override
                    public void onError() {
                        EventBus.getDefault().post(new GravatarUploadFinished(filePath, false));
                    }
                });
    }

    static public class GravatarUploadFinished {
        public final String filePath;
        public final boolean success;

        public GravatarUploadFinished(String filePath, boolean success) {
            this.filePath = filePath;
            this.success = success;
        }
    }

    public void onEventMainThread(GravatarUploadFinished event) {
        if (event.success) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.ME_GRAVATAR_UPLOADED);
            final String avatarUrl = constructGravatarUrl(mAccountStore.getAccount());
            loadAvatar(avatarUrl, event.filePath);
        } else {
            showGravatarProgressBar(false);
            Toast.makeText(getActivity(), getString(R.string.error_updating_gravatar), Toast.LENGTH_SHORT).show();
        }
    }

    static public class GravatarLoadFinished {
        public final boolean success;

        public GravatarLoadFinished(boolean success) {
            this.success = success;
        }
    }

    public void onEventMainThread(GravatarLoadFinished event) {
        if (!event.success && mIsUpdatingGravatar) {
            Toast.makeText(getActivity(), getString(R.string.error_refreshing_gravatar), Toast.LENGTH_SHORT).show();
        }
        showGravatarProgressBar(false);
    }

    // injects a fabricated cache entry to the request cache
    private void injectCache(File file, String avatarUrl) throws IOException {
        final SimpleDateFormat sdf = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
        final long currentTimeMs = System.currentTimeMillis();
        final Date currentTime = new Date(currentTimeMs);
        final long fiveMinutesLaterMs = currentTimeMs + 5 * 60 * 1000;
        final Date fiveMinutesLater = new Date(fiveMinutesLaterMs);

        Cache.Entry entry = new Cache.Entry();

        entry.data = new byte[(int) file.length()];
        DataInputStream dis = new DataInputStream(new FileInputStream(file));
        dis.readFully(entry.data);
        dis.close();

        entry.etag = null;
        entry.softTtl = fiveMinutesLaterMs;
        entry.ttl = fiveMinutesLaterMs;
        entry.serverDate = currentTimeMs;
        entry.lastModified = currentTimeMs;

        entry.responseHeaders = new TreeMap<>();
        entry.responseHeaders.put("Accept-Ranges", "bytes");
        entry.responseHeaders.put("Access-Control-Allow-Origin", "*");
        entry.responseHeaders.put("Cache-Control", "max-age=300");
        entry.responseHeaders.put("Content-Disposition", "inline; filename=\""
                + mAccountStore.getAccount().getAvatarUrl() + ".jpeg\"");
        entry.responseHeaders.put("Content-Length", String.valueOf(file.length()));
        entry.responseHeaders.put("Content-Type", "image/jpeg");
        entry.responseHeaders.put("Date", sdf.format(currentTime));
        entry.responseHeaders.put("Expires", sdf.format(fiveMinutesLater));
        entry.responseHeaders.put("Last-Modified", sdf.format(currentTime));
        entry.responseHeaders.put("Link", "<" + avatarUrl + ">; rel=\"canonical\"");
        entry.responseHeaders.put("Server", "injected cache");
        entry.responseHeaders.put("Source-Age", "0");
        entry.responseHeaders.put("X-Android-Received-Millis", String.valueOf(currentTimeMs));
        entry.responseHeaders.put("X-Android-Response-Source", "NETWORK 200");
        entry.responseHeaders.put("X-Android-Selected-Protocol", "http/1.1");
        entry.responseHeaders.put("X-Android-Sent-Millis", String.valueOf(currentTimeMs));

        WordPress.sRequestQueue.getCache().put(Request.Method.GET + ":" + avatarUrl, entry);
    }

    private class SignOutWordPressComAsync extends AsyncTask<Void, Void, Void> {
        WeakReference<Context> mWeakContext;

        public SignOutWordPressComAsync(Context context) {
            mWeakContext = new WeakReference<Context>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Context context = mWeakContext.get();
            if (context != null) {
                showDisconnectDialog(context);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            Context context = mWeakContext.get();
            if (context != null) {
                ((WordPress) getActivity().getApplication()).wordPressComSignOut();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (mDisconnectProgressDialog != null && mDisconnectProgressDialog.isShowing()) {
                mDisconnectProgressDialog.dismiss();
            }
            mDisconnectProgressDialog = null;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        refreshAccountDetails();
    }
}
