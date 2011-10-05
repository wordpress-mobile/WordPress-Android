package org.wordpress.android;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class ViewCommentActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            // If the screen is now in landscape mode, we can show the
            // dialog in-line so we don't need this activity.
            finish();
            return;
        }

        if (savedInstanceState == null) {
            // During initial setup, plug in the details fragment.
        	ViewCommentFragment commentFragment = new ViewCommentFragment();
            getSupportFragmentManager().beginTransaction().add(
            		android.R.id.content, commentFragment).commit();
        	//commentFragment.loadComment(WordPress.currentComment);
        }
    }
}
