
package org.wordpress.android.ui.accounts;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.wordpress.android.R;
import org.wordpress.android.util.WPViewPager;
import org.wordpress.android.widgets.WPTextView;

public class WelcomeFragmentHome extends NewAccountAbstractPageFragment {

    private WPTextView mSignInButton;
    private WPTextView mCreateAccountButton;
    private WPViewPager mPager;

    public WelcomeFragmentHome(WPViewPager pager) {
        mPager = pager;
    }
   
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.nux_fragment_welcome, container, false);

        ImageView statsIcon = (ImageView)rootView.findViewById(R.id.nux_fragment_icon);
        statsIcon.setImageResource(R.drawable.nux_icon_wp);

        WPTextView statsTitle = (WPTextView)rootView.findViewById(R.id.nux_fragment_title);
        statsTitle.setText(R.string.nux_welcome);

        WPTextView statsDescription = (WPTextView)rootView.findViewById(R.id.nux_fragment_description);
        statsDescription.setText(R.string.nux_welcome_description);

        mSignInButton = (WPTextView) rootView.findViewById(R.id.nux_sign_in_button);
        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPager.setCurrentItem(1);
            }
        });
        mCreateAccountButton = (WPTextView) (WPTextView)rootView.findViewById(R.id.nux_create_account_button);
        mCreateAccountButton.setOnClickListener(mCreateAccountListener);

        return rootView;
    }

    private View.OnClickListener mCreateAccountListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent newAccountIntent = new Intent(getActivity(), NewAccountActivity.class);
            startActivityForResult(newAccountIntent, WelcomeActivity.CREATE_ACCOUNT_REQUEST);
        }
    };
    
}