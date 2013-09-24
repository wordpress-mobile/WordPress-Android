
package org.wordpress.android.ui.accounts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.wordpress.android.R;

public class TutorialFragmentGetStarted extends NewAccountAbstractPageFragment {

    public TutorialFragmentGetStarted() {

    }
   
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.nux_fragment_get_started, container, false);

        LinearLayout rootLayout = (LinearLayout)rootView.findViewById(R.id.root);
        rootLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null)
                    getActivity().finish();
            }
        });

        return rootView;
    }
    
}