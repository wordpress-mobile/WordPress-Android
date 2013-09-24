
package org.wordpress.android.ui.accounts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.wordpress.android.R;
import org.wordpress.android.widgets.WPTextView;

public class TutorialFragmentStats extends NewAccountAbstractPageFragment {

    public TutorialFragmentStats() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.nux_fragment, container, false);

        ImageView statsIcon = (ImageView)rootView.findViewById(R.id.nux_fragment_icon);
        statsIcon.setImageResource(R.drawable.nux_icon_stats);

        WPTextView statsTitle = (WPTextView)rootView.findViewById(R.id.nux_fragment_title);
        statsTitle.setText(R.string.nux_tutorial_stats_title);

        WPTextView statsDescription = (WPTextView)rootView.findViewById(R.id.nux_fragment_description);
        statsDescription.setText(R.string.nux_tutorial_stats_description);
        
        return rootView;
    }
    
}