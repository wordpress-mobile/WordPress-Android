package org.wordpress.android.ui.me;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.graphics.Outline;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import org.wordpress.android.R;
import org.wordpress.android.models.Account;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.AccountHelper;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPTextView;

public class MeFragment extends Fragment {

    private WPNetworkImageView mAvatarImageView;
    private WPTextView mDisplayNameTextView;
    private WPTextView mUsernameTextView;
    private WPTextView mLoginLogoutTextView;

    public static MeFragment newInstance() {
        return new MeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_me, container, false);
        mAvatarImageView = (WPNetworkImageView) rootView.findViewById(R.id.me_avatar);
        mDisplayNameTextView = (WPTextView) rootView.findViewById(R.id.me_display_name);
        mUsernameTextView = (WPTextView) rootView.findViewById(R.id.me_username);

        WPTextView settingsTextView = (WPTextView) rootView.findViewById(R.id.me_settings_text_view);
        settingsTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewAccountSettings(getActivity());
            }
        });

        WPTextView supportTextView = (WPTextView) rootView.findViewById(R.id.me_support_text_view);
        supportTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewHelpAndSupport(getActivity());
            }
        });

        mLoginLogoutTextView = (WPTextView) rootView.findViewById(R.id.me_login_logout_text_view);

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

            mAvatarImageView.setImageUrl(defaultAccount.getAvatarUrl(), WPNetworkImageView.ImageType.AVATAR);
            mUsernameTextView.setText("@" + defaultAccount.getUserName());

            String displayName = defaultAccount.getDisplayName();
            if (!TextUtils.isEmpty(displayName)) {
                mDisplayNameTextView.setText(displayName);
            } else {
                mDisplayNameTextView.setText(defaultAccount.getUserName());
            }

            mLoginLogoutTextView.setText(R.string.me_disconnect_from_wordpress_com);
        }
        else {
            mAvatarImageView.setVisibility(View.GONE);
            mDisplayNameTextView.setVisibility(View.GONE);
            mUsernameTextView.setVisibility(View.GONE);

            mLoginLogoutTextView.setText(R.string.me_connect_to_wordpress_com);
        }
    }
}
