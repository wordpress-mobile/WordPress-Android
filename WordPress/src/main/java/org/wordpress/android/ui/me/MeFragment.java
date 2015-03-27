package org.wordpress.android.ui.me;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPTextView;

public class MeFragment extends Fragment {

    private WPNetworkImageView mAvatarImageView;
    private WPTextView mFullNameTextView;
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
        mFullNameTextView = (WPTextView) rootView.findViewById(R.id.me_full_name);
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

        refreshAccountDetails();

        return rootView;
    }

    private void refreshAccountDetails() {
        mAvatarImageView.setImageUrl("http://lorempixum.com/128/128", WPNetworkImageView.ImageType.AVATAR);
        mFullNameTextView.setText("Full Name");
        mUsernameTextView.setText("@username");

        mLoginLogoutTextView.setText(R.string.me_disconnect_from_wordpress_com);
    }
}
