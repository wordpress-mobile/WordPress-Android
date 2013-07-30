package org.wordpress.android.ui.stats;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.stats.Stats.ViewType;

public class StatsActivity extends WPActionBarActivity implements StatsNavDialogFragment.NavigationListener {

    private StatsPhoneFragment mStatsPhoneFragment;
    private View mActionbarNav;
    private TextView mActionbarNavText;
    private DialogFragment mNavFragment;
    private int mNavPosition = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setTitle("");

        createMenuDrawer(R.layout.stats_activity);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        
        mActionbarNav = getLayoutInflater().inflate(R.layout.stats_ab_navigation, null, false);
        actionBar.setCustomView(mActionbarNav);
        
        mActionbarNavText = (TextView) mActionbarNav.findViewById(R.id.stats_ab_nav_text);
        mActionbarNavText.setText(ViewType.values()[mNavPosition].getLabel());
        mActionbarNavText.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!isFinishing())
                    showViews();
            }

        });
        
        FragmentManager fm = getSupportFragmentManager();
        mStatsPhoneFragment = (StatsPhoneFragment) fm.findFragmentByTag(StatsPhoneFragment.TAG);
        if (mStatsPhoneFragment == null) { 
            mStatsPhoneFragment = StatsPhoneFragment.newInstance(ViewType.TOP_POSTS_AND_PAGES);
            fm.beginTransaction().add(R.id.stats_container, mStatsPhoneFragment).commit();
        }
        
        mNavFragment = (DialogFragment) fm.findFragmentByTag(StatsNavDialogFragment.TAG);
        
    }
    
    protected void showViews() {
        FragmentManager fm = getSupportFragmentManager();
        mNavFragment = (DialogFragment) fm.findFragmentByTag(StatsNavDialogFragment.TAG);
        if (mNavFragment == null)
            mNavFragment = StatsNavDialogFragment.newInstance(mNavPosition);
        mNavFragment.show(getSupportFragmentManager(), StatsNavDialogFragment.TAG);
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

    @Override
    public void onItemClick(int position) {
        mNavPosition = position;
        ViewType viewType = ViewType.values()[mNavPosition];
        mActionbarNavText.setText(viewType.getLabel());

        FragmentManager fm = getSupportFragmentManager();
        StatsNavDialogFragment navFragment = (StatsNavDialogFragment) fm.findFragmentByTag(StatsNavDialogFragment.TAG);
        if (navFragment != null)
            navFragment.dismissAllowingStateLoss();
        
        mStatsPhoneFragment = StatsPhoneFragment.newInstance(viewType);
        fm.beginTransaction().replace(R.id.stats_container, mStatsPhoneFragment, StatsPhoneFragment.TAG).commit();
    }
}
