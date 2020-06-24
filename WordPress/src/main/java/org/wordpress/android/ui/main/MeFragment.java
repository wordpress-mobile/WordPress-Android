package org.wordpress.android.ui.main;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.networking.GravatarApi;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.HelpActivity.Origin;
import org.wordpress.android.ui.main.utils.MeGravatarLoader;
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource;
import org.wordpress.android.ui.prefs.AppPrefsWrapper;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageManager.RequestListener;
import org.wordpress.android.util.image.ImageType;

import java.io.File;
import java.lang.ref.WeakReference;

import javax.inject.Inject;

public class MeFragment extends Fragment implements MainToolbarFragment, WPMainActivity.OnScrollToTopListener {
    private static final String IS_DISCONNECTING = "IS_DISCONNECTING";
    private static final String IS_UPDATING_GRAVATAR = "IS_UPDATING_GRAVATAR";

    private ViewGroup mAvatarCard;
    private View mProgressBar;
    private ImageView mAvatarImageView;
    private TextView mDisplayNameTextView;
    private TextView mUsernameTextView;
    private TextView mLoginLogoutTextView;
    private View mMyProfileView;
    private View mAccountSettingsView;
    private ProgressDialog mDisconnectProgressDialog;
    private ScrollView mScrollView;

    @Nullable
    private Toolbar mToolbar = null;
    private String mToolbarTitle;

    private boolean mIsUpdatingGravatar;

    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject ImageManager mImageManager;
    @Inject AppPrefsWrapper mAppPrefsWrapper;
    @Inject PostStore mPostStore;
    @Inject MeGravatarLoader mMeGravatarLoader;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.me_fragment, container, false);

        mAvatarCard = rootView.findViewById(R.id.card_avatar);
        ViewGroup avatarContainer = rootView.findViewById(R.id.avatar_container);
        mAvatarImageView = rootView.findViewById(R.id.me_avatar);
        mProgressBar = rootView.findViewById(R.id.avatar_progress);
        mDisplayNameTextView = rootView.findViewById(R.id.me_display_name);
        mUsernameTextView = rootView.findViewById(R.id.me_username);
        mLoginLogoutTextView = rootView.findViewById(R.id.me_login_logout_text_view);
        mMyProfileView = rootView.findViewById(R.id.row_my_profile);
        mAccountSettingsView = rootView.findViewById(R.id.row_account_settings);
        mScrollView = rootView.findViewById(R.id.scroll_view);

        OnClickListener showPickerListener = v -> {
            AnalyticsTracker.track(AnalyticsTracker.Stat.ME_GRAVATAR_TAPPED);
            showPhotoPickerForGravatar();
        };

        avatarContainer.setOnClickListener(showPickerListener);
        rootView.findViewById(R.id.change_photo).setOnClickListener(showPickerListener);

        mMyProfileView.setOnClickListener(v -> ActivityLauncher.viewMyProfile(getActivity()));

        mAccountSettingsView.setOnClickListener(v -> ActivityLauncher.viewAccountSettings(getActivity()));

        rootView.findViewById(R.id.row_app_settings).setOnClickListener(
                v -> ActivityLauncher.viewAppSettingsForResult(getActivity()));

        rootView.findViewById(R.id.row_support).setOnClickListener(
                v -> ActivityLauncher
                        .viewHelpAndSupport(getActivity(), Origin.ME_SCREEN_HELP, getSelectedSite(), null));

        rootView.findViewById(R.id.row_logout).setOnClickListener(v -> {
            if (mAccountStore.hasAccessToken()) {
                signOutWordPressComWithConfirmation();
            } else {
                ActivityLauncher.showSignInForResult(getActivity(), true);
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

        mToolbar = rootView.findViewById(R.id.toolbar_main);
        mToolbar.setTitle(mToolbarTitle);

        return rootView;
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        if (mDisconnectProgressDialog != null) {
            outState.putBoolean(IS_DISCONNECTING, true);
        }
        outState.putBoolean(IS_UPDATING_GRAVATAR, mIsUpdatingGravatar);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onScrollToTop() {
        if (isAdded()) {
            mScrollView.smoothScrollTo(0, 0);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        mDispatcher.register(this);
    }

    @Override
    public void setTitle(@NotNull String title) {
        mToolbarTitle = title;
        if (mToolbar != null) {
            mToolbar.setTitle(title);
        }
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

            loadAvatar(null);

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
            mLoginLogoutTextView.setText(R.string.me_connect_to_wordpress_com);
        }
    }

    private void showGravatarProgressBar(boolean isUpdating) {
        mProgressBar.setVisibility(isUpdating ? View.VISIBLE : View.GONE);
        mIsUpdatingGravatar = isUpdating;
    }

    private void loadAvatar(String injectFilePath) {
        final boolean newAvatarUploaded = injectFilePath != null && !injectFilePath.isEmpty();
        final String avatarUrl = mMeGravatarLoader.constructGravatarUrl(mAccountStore.getAccount().getAvatarUrl());

        mMeGravatarLoader.load(
                newAvatarUploaded,
                avatarUrl,
                injectFilePath,
                mAvatarImageView,
                ImageType.AVATAR_WITHOUT_BACKGROUND,
                new RequestListener<Drawable>() {
                    @Override
                    public void onLoadFailed(@Nullable Exception e, @Nullable Object model) {
                        final String appLogMessage = "onLoadFailed while loading Gravatar image!";
                        if (e == null) {
                            AppLog.e(T.MAIN, appLogMessage + " e == null");
                        } else {
                            AppLog.e(T.MAIN, appLogMessage, e);
                        }

                        // For some reason, the Activity can be null so, guard for it. See #8590.
                        if (getActivity() != null) {
                            ToastUtils.showToast(getActivity(), R.string.error_refreshing_gravatar,
                                    ToastUtils.Duration.SHORT);
                        }
                    }

                    @Override
                    public void onResourceReady(@NotNull Drawable resource, @Nullable Object model) {
                        if (newAvatarUploaded && resource instanceof BitmapDrawable) {
                            Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
                            // create a copy since the original bitmap may by automatically recycled
                            bitmap = bitmap.copy(bitmap.getConfig(), true);
                            WordPress.getBitmapCache().put(
                                    avatarUrl,
                                    bitmap
                            );
                        }
                    }
                });
    }

    private void signOutWordPressComWithConfirmation() {
        // if there are local changes we need to let the user know they'll be lost if they logout, otherwise
        // we use a simpler (less scary!) confirmation
        String message;
        if (mPostStore.getNumLocalChanges() > 0) {
            message = getString(R.string.sign_out_wpcom_confirm_with_changes);
        } else {
            message = getString(R.string.sign_out_wpcom_confirm_with_no_changes);
        }

        new MaterialAlertDialogBuilder(getActivity())
                .setMessage(message)
                .setPositiveButton(R.string.signout, (dialog, whichButton) -> signOutWordPressCom())
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
                    String[] mediaUriStringsArray = data.getStringArrayExtra(PhotoPickerActivity.EXTRA_MEDIA_URIS);
                    if (mediaUriStringsArray == null || mediaUriStringsArray.length == 0) {
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
                    Uri imageUri = Uri.parse(mediaUriStringsArray[0]);
                    if (imageUri != null) {
                        boolean didGoWell = WPMediaUtils.fetchMediaAndDoNext(getActivity(), imageUri,
                                this::startCropActivity);

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
                            uri -> startGravatarUpload(
                                    MediaUtils.getRealPathFromURI(getActivity(), uri)));
                } else if (resultCode == UCrop.RESULT_ERROR) {
                    AppLog.e(AppLog.T.MAIN, "Image cropping failed!", UCrop.getError(data));
                    ToastUtils.showToast(getActivity(), R.string.error_cropping_image, Duration.SHORT);
                }
                break;
        }
    }

    private void showPhotoPickerForGravatar() {
        ActivityLauncher.showPhotoPickerForResult(this, MediaBrowserType.GRAVATAR_IMAGE_PICKER, null, null);
    }

    private void startCropActivity(Uri uri) {
        final Context context = getActivity();

        if (context == null) {
            return;
        }

        UCrop.Options options = new UCrop.Options();
        options.setShowCropGrid(false);
        options.setStatusBarColor(ContextCompat.getColor(context, R.color.status_bar));
        options.setToolbarColor(ContextCompat.getColor(context, R.color.primary));
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

        GravatarUploadFinished(String filePath, boolean success) {
            this.filePath = filePath;
            this.success = success;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(GravatarUploadFinished event) {
        showGravatarProgressBar(false);
        if (event.success) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.ME_GRAVATAR_UPLOADED);
            loadAvatar(event.filePath);
        } else {
            ToastUtils.showToast(getActivity(), R.string.error_updating_gravatar, ToastUtils.Duration.SHORT);
        }
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

    private @Nullable SiteModel getSelectedSite() {
        if (getActivity() instanceof WPMainActivity) {
            WPMainActivity mainActivity = (WPMainActivity) getActivity();
            return mainActivity.getSelectedSite();
        }
        return null;
    }
}
