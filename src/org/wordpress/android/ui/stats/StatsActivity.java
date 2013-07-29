package org.wordpress.android.ui.stats;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.stats.Stats.ViewType;

public class StatsActivity extends WPActionBarActivity {

    private StatsListFragment mStatsListFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setTitle(R.string.stats);

        createMenuDrawer(R.layout.stats_activity);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        FragmentManager fm = getSupportFragmentManager();
        mStatsListFragment = (StatsListFragment) fm.findFragmentByTag(StatsListFragment.TAG);
        
        if (mStatsListFragment == null) {
            mStatsListFragment = StatsListFragment.newInstance(ViewType.CLICKS);
            fm.beginTransaction().add(R.id.stats_container, mStatsListFragment).commit();
        }
        
    }
    
}
