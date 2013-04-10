package org.wordpress.android.ui.notifications;

import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.ui.WPActionBarActivity;

public class NotificationsActivity extends WPActionBarActivity {
    public final String TAG="WPNotifications";
    @Override
    public void onCreate(Bundle savedInstanceState){
        Log.d(TAG, "Launching notifications activity");
        super.onCreate(savedInstanceState);
        createMenuDrawer(R.layout.notifications);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        setTitle(getString(R.string.notifications));
        
    }
}