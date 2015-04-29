package org.wordpress.android.ui.main;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Outline;
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
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.AccountHelper;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

public class MeFragment extends Fragment {

    private WPNetworkImageView mAvatarImageView;
    private TextView mDisplayNameTextView;
    private TextView mUsernameTextView;
    private TextView mLoginLogoutTextView;

    public static MeFragment newInstance() {
        return new MeFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_me, container, false);
        mAvatarImageView = (WPNetworkImageView) rootView.findViewById(R.id.me_avatar);
        mDisplayNameTextView = (TextView) rootView.findViewById(R.id.me_display_name);
        mUsernameTextView = (TextView) rootView.findViewById(R.id.me_username);

        TextView settingsTextView = (TextView) rootView.findViewById(R.id.me_settings_text_view);
        settingsTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewAccountSettings(getActivity());
            }
        });

        TextView supportTextView = (TextView) rootView.findViewById(R.id.me_support_text_view);
        supportTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewHelpAndSupport(getActivity());
            }
        });

        mLoginLogoutTextView = (TextView) rootView.findViewById(R.id.me_login_logout_text_view);

        addDropShadow(rootView.findViewById(R.id.frame_avatar));
        refreshAccountDetails();

        return rootView;
    }

    /*
     * adds a circular drop shadow to the avatar's parent view (Lollipop+ only)
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void addDropShadow(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
            view.setElevation(view.getResources().getDimensionPixelSize(R.dimen.card_elevation));
        }
    }

    private void refreshAccountDetails() {
        Account defaultAccount = AccountHelper.getDefaultAccount();
        // we only want to show user details for WordPress.com users
        if (defaultAccount.isWordPressComUser()) {
            mAvatarImageView.setVisibility(View.VISIBLE);
            mDisplayNameTextView.setVisibility(View.VISIBLE);
            mUsernameTextView.setVisibility(View.VISIBLE);

            int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
            String avatarUrl = GravatarUtils.fixGravatarUrl(defaultAccount.getAvatarUrl(), avatarSz);
            mAvatarImageView.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);

            mUsernameTextView.setText("@" + defaultAccount.getUserName());

            String displayName = defaultAccount.getDisplayName();
            if (!TextUtils.isEmpty(displayName)) {
                mDisplayNameTextView.setText(displayName);
            } else {
                mDisplayNameTextView.setText(defaultAccount.getUserName());
            }

            mLoginLogoutTextView.setText(R.string.me_disconnect_from_wordpress_com);
            mLoginLogoutTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    signoutWithConfirmation();
                }
            });
        } else {
            mAvatarImageView.setVisibility(View.GONE);
            mDisplayNameTextView.setVisibility(View.GONE);
            mUsernameTextView.setVisibility(View.GONE);

            mLoginLogoutTextView.setText(R.string.me_connect_to_wordpress_com);
            mLoginLogoutTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActivityLauncher.showSignInForResult(getActivity());
                }
            });
        }
    }

    private void signoutWithConfirmation() {
        new AlertDialog.Builder(getActivity())
            .setMessage(getString(R.string.sign_out_confirm))
            .setPositiveButton(R.string.signout, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    signout();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .setCancelable(true)
            .create().show();
    }

    private void signout() {
        WordPress.signOutAsyncWithProgressBar(getActivity(), new WordPress.SignOutAsync.SignOutCallback() {
            @Override
            public void onSignOut() {
                // note that signing out sends a CoreEvents.UserSignedOut() EventBus event,
                // which will cause the main activity to show the sign in screen
                if (isAdded()) {
                    refreshAccountDetails();
                }
            }
        });
    }
}
