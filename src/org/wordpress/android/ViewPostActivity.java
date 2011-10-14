package org.wordpress.android;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class ViewPostActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // During initial setup, plug in the details fragment.
        	ViewPostFragment postFragment = new ViewPostFragment();
            getSupportFragmentManager().beginTransaction().add(
            		android.R.id.content, postFragment).commit();
        }
    }
}
