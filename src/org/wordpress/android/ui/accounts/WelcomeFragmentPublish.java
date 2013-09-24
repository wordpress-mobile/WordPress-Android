
package org.wordpress.android.ui.accounts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.wordpress.android.R;
import org.wordpress.android.widgets.WPTextView;

public class WelcomeFragmentPublish extends NewAccountAbstractPageFragment {

    public WelcomeFragmentPublish() {

    }
   
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.nux_fragment, container, false);

        ImageView statsIcon = (ImageView)rootView.findViewById(R.id.nux_fragment_icon);
        statsIcon.setImageResource(R.drawable.nux_icon_post);

        WPTextView statsTitle = (WPTextView)rootView.findViewById(R.id.nux_fragment_title);
        statsTitle.setText(R.string.nux_welcome_publish_title);

        WPTextView statsDescription = (WPTextView)rootView.findViewById(R.id.nux_fragment_description);
        statsDescription.setText(R.string.nux_welcome_publish_description);

        return rootView;
    }
    
}