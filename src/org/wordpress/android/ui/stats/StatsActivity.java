package org.wordpress.android.ui.stats;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.stats.Stats.ViewType;

public class StatsActivity extends WPActionBarActivity {

    private StatsPhoneFragment mStatsPhoneFragment;

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
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(), R.layout.sherlock_spinner_item, ViewType.toStringArray());
        ActionBar.OnNavigationListener navigationListener = new OnNavigationListener() {

            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {

                FragmentManager fm = getSupportFragmentManager();
                mStatsPhoneFragment = (StatsPhoneFragment) fm.findFragmentByTag(StatsPhoneFragment.TAG);
                
                mStatsPhoneFragment = StatsPhoneFragment.newInstance(ViewType.values()[itemPosition]);
                fm.beginTransaction().replace(R.id.stats_container, mStatsPhoneFragment).commit();
                return false;
            }
        };
        getSupportActionBar().setListNavigationCallbacks(adapter, navigationListener);


        FragmentManager fm = getSupportFragmentManager();
        mStatsPhoneFragment = StatsPhoneFragment.newInstance(ViewType.TOP_POSTS_AND_PAGES);
        fm.beginTransaction().add(R.id.stats_container, mStatsPhoneFragment).commit();
        
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.stats, menu);
        return true;
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.menu_stats_load_sample_data) {
            
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    WordPress.wpDB.loadSampleStats();
                    return null;
                }
                
                @Override
                protected void onPostExecute(Void result) {
                    invalidateOptionsMenu();
                }
                
            };
            task.execute();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (WordPress.getCurrentBlog() != null) {
        
            if (WordPress.wpDB.getStatsCount(WordPress.getCurrentBlog().getBlogId()) > 0)
                menu.findItem(R.id.menu_stats_load_sample_data).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }
}
