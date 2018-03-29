package org.wordpress.android.ui.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.Cache;
import com.android.volley.Request;
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
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.HelpshiftHelper.Tag;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class MeFragment extends Fragment {
    private static final String IS_DISCONNECTING = "IS_DISCONNECTING";
    private static final String IS_UPDATING_GRAVATAR = "IS_UPDATING_GRAVATAR";

    private ViewGroup mAvatarCard;
    private View mProgressBar;
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.me_fragment, container, false);

        mAvatarCard = rootView.findViewById(R.id.card_avatar);
        mAvatarContainer = rootView.findViewById(R.id.avatar_container);
        mAvatarImageView = rootView.findViewById(R.id.me_avatar);
        mProgressBar = rootView.findViewById(R.id.avatar_progress);
        mDisplayNameTextView = rootView.findViewById(R.id.me_display_name);
        mUsernameTextView = rootView.findViewById(R.id.me_username);
        mLoginLogoutTextView = rootView.findViewById(R.id.me_login_logout_text_view);
        mMyProfileView = rootView.findViewById(R.id.row_my_profile);
        mAccountSettingsView = rootView.findViewById(R.id.row_account_settings);
        mNotificationsView = rootView.findViewById(R.id.row_notifications);
        mNotificationsDividerView = rootView.findViewById(R.id.me_notifications_divider);

        OnClickListener showPickerListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnalyticsTracker.track(AnalyticsTracker.Stat.ME_GRAVATAR_TAPPED);
                showPhotoPickerForGravatar();
            }
        };

        mAvatarContainer.setOnClickListener(showPickerListener);
        rootView.findViewById(R.id.change_photo).setOnClickListener(showPickerListener);

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
    }

    @Override
    public void onDestroy() {
        if (mDisconnectProgressDialog != null) {
            mDisconnectProgressDialog.dismiss();
            mDisconnectProgressDialog = null;
        }
        super.onDestroy();
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
            mAvatarCard.setVisibility(View.VISIBLE);
            mMyProfileView.setVisibility(View.VISIBLE);
            mNotificationsView.setVisibility(View.VISIBLE);
            mNotificationsDividerView.setVisibility(View.VISIBLE);

            final String avatarUrl = constructGravatarUrl(mAccountStore.getAccount());
            loadAvatar(avatarUrl, null);

            mUsernameTextView.setText(getString(R.string.at_username, defaultAccount.getUserName()));
            mLoginLogoutTextView.setText(R.string.me_disconnect_from_wordpress_com);

            String displayName = defaultAccount.getDisplayName();
            if (!TextUtils.isEmpty(displayName)) {
                mDisplayNameTextView.setText(displayName);
            } else {
                mDisplayNameTextView.setText(defaultAccount.getUserName());
            }
        } else {
            mDisplayNameTextView.setVisibility(View.GONE);
            mUsernameTextView.setVisibility(View.GONE);
            mAvatarCard.setVisibility(View.GONE);
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

        new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.Calypso_Dialog))
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // If the fragment is not attached to the activity, we can't start the crop activity or upload the
        // cropped image.
        if (!isAdded()) {
            return;
        }

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
                    if (imageUri != null) {
                        boolean didGoWell = WPMediaUtils.fetchMediaAndDoNext(getActivity(), imageUri,
                                                                             new WPMediaUtils.MediaFetchDoNext() {
                                                                                 @Override
                                                                                 public void doNext(Uri uri) {
                                                                                     startCropActivity(uri);
                                                                                 }
                                                                             });

                        if (!didGoWell) {
                            AppLog.e(AppLog.T.UTILS, "Can't download picked or captured image");
                        }
                    }
                }
                break;
            case UCrop.REQUEST_CROP:
                AnalyticsTracker.track(AnalyticsTracker.Stat.ME_GRAVATAR_CROPPED);

                if (resultCode == Activity.RESULT_OK) {
                    WPMediaUtils.fetchMediaAndDoNext(getActivity(), UCrop.getOutput(data),
                                                     new WPMediaUtils.MediaFetchDoNext() {
                                                         @Override
                                                         public void doNext(Uri uri) {
                                                             startGravatarUpload(
                                                                     MediaUtils.getRealPathFromURI(getActivity(), uri));
                                                         }
                                                     });
                } else if (resultCode == UCrop.RESULT_ERROR) {
                    AppLog.e(AppLog.T.MAIN, "Image cropping failed!", UCrop.getError(data));
                    ToastUtils.showToast(getActivity(), R.string.error_cropping_image, Duration.SHORT);
                }
                break;
        }
    }

    private void showPhotoPickerForGravatar() {
        ActivityLauncher.showPhotoPickerForResult(getActivity(), MediaBrowserType.GRAVATAR_IMAGE_PICKER, null);
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

    private void startGravatarUpload(final String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            ToastUtils.showToast(getActivity(), R.string.error_locating_image, ToastUtils.Duration.SHORT);
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            ToastUtils.showToast(getActivity(), R.string.error_locating_image, ToastUtils.Duration.SHORT);
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

    public static class GravatarUploadFinished {
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
            ToastUtils.showToast(getActivity(), R.string.error_updating_gravatar, ToastUtils.Duration.SHORT);
        }
    }

    public static class GravatarLoadFinished {
        public final boolean success;

        public GravatarLoadFinished(boolean success) {
            this.success = success;
        }
    }

    public void onEventMainThread(GravatarLoadFinished event) {
        if (!event.success && mIsUpdatingGravatar) {
            ToastUtils.showToast(getActivity(), R.string.error_refreshing_gravatar, ToastUtils.Duration.SHORT);
        }
        showGravatarProgressBar(false);
    }

    // injects a fabricated cache entry to the request cache
    private void injectCache(File file, String avatarUrl) throws IOException {
        final SimpleDateFormat sdf = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.ROOT);
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

        SignOutWordPressComAsync(Context context) {
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
