package org.wordpress.android.ui.stats;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.stats.StatsClicksFragment.CustomCursorAdapter;

public class StatsActivityTablet extends WPActionBarActivity {

    private LinearLayout mFragmentContainer;
    private LinearLayout mColumnLeft;
    private LinearLayout mColumnRight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        

        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        createMenuDrawer(R.layout.stats_activity_tablet);
        
        mFragmentContainer = (LinearLayout) findViewById(R.id.stats_fragment_container);
        mColumnLeft = (LinearLayout) findViewById(R.id.stats_tablet_col_left); 
        mColumnRight = (LinearLayout) findViewById(R.id.stats_tablet_col_right); 
        
        
        loadStatsFragments();
    }

    private void loadStatsFragments() {

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        
        StatsAbsViewFragment fragment = StatsAbsViewFragment.newInstance(StatsViewType.CLICKS);
        ft.add(R.id.stats_clicks_container, fragment, StatsClicksFragment.TAG);
        
        fragment = StatsAbsViewFragment.newInstance(StatsViewType.COMMENTS);
        ft.add(R.id.stats_comments_container, fragment); 
        
        fragment = StatsAbsViewFragment.newInstance(StatsViewType.VIEWS_BY_COUNTRY);
        ft.add(R.id.stats_geoviews_container, fragment);
        
        fragment = StatsAbsViewFragment.newInstance(StatsViewType.SEARCH_ENGINE_TERMS);
        ft.add(R.id.stats_searchengine_container, fragment);

        fragment = StatsAbsViewFragment.newInstance(StatsViewType.TAGS_AND_CATEGORIES);
        ft.add(R.id.stats_tags_and_categories_container, fragment);
        
        fragment = StatsAbsViewFragment.newInstance(StatsViewType.TOP_AUTHORS);
        ft.add(R.id.stats_top_authors_container, fragment);
        
        fragment = StatsAbsViewFragment.newInstance(StatsViewType.TOTALS_FOLLOWERS_AND_SHARES);
        ft.add(R.id.stats_totals_followers_shares_container, fragment);
        
        fragment = StatsAbsViewFragment.newInstance(StatsViewType.TOP_POSTS_AND_PAGES);
        ft.add(R.id.stats_top_posts_container, fragment);
        
        fragment = StatsAbsViewFragment.newInstance(StatsViewType.VIDEO_PLAYS);
        ft.add(R.id.stats_video_container, fragment);
        
        ft.commit();

        FrameLayout frameView = (FrameLayout) findViewById(R.id.stats_clicks_container);
        mFragmentContainer.removeView(frameView);
        mColumnRight.addView(frameView);

//        frameView = (FrameLayout) findViewById(R.id.stats_tags_and_categories_container);
//        mFragmentContainer.removeView(frameView);
//        mColumnRight.addView(frameView);
//
//        frameView = (FrameLayout) findViewById(R.id.stats_top_authors_container);
//        mFragmentContainer.removeView(frameView);
//        mColumnRight.addView(frameView);
//        
//        frameView = (FrameLayout) findViewById(R.id.stats_top_posts_container);
//        mFragmentContainer.removeView(frameView);
//        mColumnRight.addView(frameView);
//        
//        frameView = (FrameLayout) findViewById(R.id.stats_comments_container);
//        mFragmentContainer.removeView(frameView);
//        mColumnLeft.addView(frameView);
//        
//        frameView = (FrameLayout) findViewById(R.id.stats_geoviews_container);
//        mFragmentContainer.removeView(frameView);
//        mColumnLeft.addView(frameView);
//        
//        frameView = (FrameLayout) findViewById(R.id.stats_searchengine_container);
//        mFragmentContainer.removeView(frameView);
//        mColumnLeft.addView(frameView);
//        
//        
//        frameView = (FrameLayout) findViewById(R.id.stats_totals_followers_shares_container);
//        mFragmentContainer.removeView(frameView);
//        mColumnLeft.addView(frameView);
        
        frameView = (FrameLayout) findViewById(R.id.stats_video_container);
        mFragmentContainer.removeView(frameView);
        mColumnLeft.addView(frameView);
        
        
    }
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.stats, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_stats_load_sample_data).setVisible(!WordPress.wpStatsDB.hasSampleData());
        return super.onPrepareOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.menu_stats_load_sample_data) {
            
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    WordPress.wpStatsDB.generateSampleData();
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
    
}
