package org.wordpress.android.ui.me;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPTextView;

public class MeFragment extends Fragment {

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
        WPNetworkImageView imgAvatar = (WPNetworkImageView) rootView.findViewById(R.id.me_avatar);
        WPTextView fullNameTextView = (WPTextView) rootView.findViewById(R.id.me_full_name);
        WPTextView usernameTextView = (WPTextView) rootView.findViewById(R.id.me_username);

        imgAvatar.setImageUrl("http://lorempixum.com/128/128", WPNetworkImageView.ImageType.AVATAR);
        fullNameTextView.setText("Full Name");
        usernameTextView.setText("@username");

        return rootView;
    }
}
