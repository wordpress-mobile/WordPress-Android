package org.wordpress.android.ui.main;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.support.v4.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Outline;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Account;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.prefs.PrefsEvents;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.HelpshiftHelper.Tag;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.lang.ref.WeakReference;

import de.greenrobot.event.EventBus;

public class MeFragment extends Fragment {
    private static final String IS_DISCONNECTING = "IS_DISCONNECTING";

    private ViewGroup mAvatarFrame;
    private WPNetworkImageView mAvatarImageView;
    private TextView mDisplayNameTextView;
    private TextView mUsernameTextView;
    private TextView mLoginLogoutTextView;
    private View mMyProfileView;
    private View mNotificationsView;
    private View mNotificationsDividerView;
    private ProgressDialog mDisconnectProgressDialog;

    public static MeFragment newInstance() {
        return new MeFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.me_fragment, container, false);

        mAvatarFrame = (ViewGroup) rootView.findViewById(R.id.frame_avatar);
        mAvatarImageView = (WPNetworkImageView) rootView.findViewById(R.id.me_avatar);
        mDisplayNameTextView = (TextView) rootView.findViewById(R.id.me_display_name);
        mUsernameTextView = (TextView) rootView.findViewById(R.id.me_username);
        mLoginLogoutTextView = (TextView) rootView.findViewById(R.id.me_login_logout_text_view);
        mMyProfileView = rootView.findViewById(R.id.row_my_profile);
        mNotificationsView = rootView.findViewById(R.id.row_notifications);
        mNotificationsDividerView = rootView.findViewById(R.id.me_notifications_divider);

        addDropShadowToAvatar();
        refreshAccountDetails();

        mMyProfileView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewMyProfile(getActivity());
            }
        });

        rootView.findViewById(R.id.row_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewAccountSettings(getActivity());
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
                if (AccountHelper.isSignedInWordPressDotCom()) {
                    signOutWordPressComWithConfirmation();
                } else {
                    ActivityLauncher.showSignInForResult(getActivity());
                }
            }
        });

        if (savedInstanceState != null && savedInstanceState.getBoolean(IS_DISCONNECTING, false)) {
            showDisconnectDialog(getActivity());
        }

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mDisconnectProgressDialog != null) {
            outState.putBoolean(IS_DISCONNECTING, true);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
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

    /**
     * adds a circular drop shadow to the avatar's parent view (Lollipop+ only)
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void addDropShadowToAvatar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mAvatarFrame.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
            mAvatarFrame.setElevation(mAvatarFrame.getResources().getDimensionPixelSize(R.dimen.card_elevation));
        }
    }

    private void refreshAccountDetails() {
        // we only want to show user details for WordPress.com users
        if (AccountHelper.isSignedInWordPressDotCom()) {
            Account defaultAccount = AccountHelper.getDefaultAccount();

            mDisplayNameTextView.setVisibility(View.VISIBLE);
            mUsernameTextView.setVisibility(View.VISIBLE);
            mAvatarFrame.setVisibility(View.VISIBLE);
            mMyProfileView.setVisibility(View.VISIBLE);
            mNotificationsView.setVisibility(View.VISIBLE);
            mNotificationsDividerView.setVisibility(View.VISIBLE);

            int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
            String avatarUrl = GravatarUtils.fixGravatarUrl(defaultAccount.getAvatarUrl(), avatarSz);
            mAvatarImageView.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);

            mUsernameTextView.setText("@" + defaultAccount.getUserName());
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
            mAvatarFrame.setVisibility(View.GONE);
            mMyProfileView.setVisibility(View.GONE);
            mNotificationsView.setVisibility(View.GONE);
            mNotificationsDividerView.setVisibility(View.GONE);
            mLoginLogoutTextView.setText(R.string.me_connect_to_wordpress_com);
        }
    }

    private void signOutWordPressComWithConfirmation() {
        String message = String.format(getString(R.string.sign_out_wpcom_confirm), AccountHelper.getDefaultAccount()
                .getUserName());

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
                WordPress.WordPressComSignOut(context);
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

    public void onEventMainThread(PrefsEvents.MyProfileDetailsChanged event) {
        refreshAccountDetails();
    }
}
