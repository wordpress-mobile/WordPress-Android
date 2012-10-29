package org.wordpress.android;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class ViewCommentActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // During initial setup, plug in the details fragment.
        	ViewCommentFragment commentFragment = new ViewCommentFragment();
            getSupportFragmentManager().beginTransaction().add(
            		android.R.id.content, commentFragment).commit();
        	//commentFragment.loadComment(WordPress.currentComment);
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
