package org.wordpress.android.ui.posts;

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
                    android.R.id.content, postFragment).commitAllowingStateLoss();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }
}
