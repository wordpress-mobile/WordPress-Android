package org.wordpress.android.ui.stats;

import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest.ErrorListener;
import com.wordpress.rest.RestRequest.Listener;

import org.json.JSONObject;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.StatsSummary;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.util.StatUtils;

public class StatsActivity extends WPActionBarActivity implements StatsNavDialogFragment.NavigationListener {

    private static final String SAVED_NAV_POSITION = "SAVED_NAV_POSITION";
    
    private StatsAbsViewFragment mStatsViewFragment;
    private View mActionbarNav;
    private TextView mActionbarNavText;
    private DialogFragment mNavFragment;
    private int mNavPosition = 0;

    private TextView mViewsTotalText;
    private TextView mCommentsTotalText;
    private TextView mFavsTotalText;
    private TextView mReblogTotalText;

    private View mStatsHeader;

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
        
        restoreState(savedInstanceState);
        
        mActionbarNav = getLayoutInflater().inflate(R.layout.stats_ab_navigation, null, false);
        actionBar.setCustomView(mActionbarNav);
        
        mActionbarNavText = (TextView) mActionbarNav.findViewById(R.id.stats_ab_nav_text);
        mActionbarNavText.setText(StatsViewType.values()[mNavPosition].getLabel());
        mActionbarNavText.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!isFinishing())
                    showViews();
            }

        });
        
        FragmentManager fm = getSupportFragmentManager();
        mStatsViewFragment = (StatsAbsViewFragment) fm.findFragmentByTag(StatsAbsViewFragment.TAG);
        if (mStatsViewFragment == null) { 
            mStatsViewFragment = StatsAbsViewFragment.newInstance(StatsViewType.values()[0]);
            fm.beginTransaction().add(R.id.stats_container, mStatsViewFragment, StatsAbsViewFragment.TAG).commit();
        }
        
        mNavFragment = (DialogFragment) fm.findFragmentByTag(StatsNavDialogFragment.TAG);

        mStatsHeader = findViewById(R.id.stats_header);
        hideHeaderIfLandscape();
        
        mViewsTotalText = (TextView) findViewById(R.id.stats_header_views_total);
        mCommentsTotalText = (TextView) findViewById(R.id.stats_header_comments_total);
        mFavsTotalText = (TextView) findViewById(R.id.stats_header_favs_total);
        mReblogTotalText = (TextView) findViewById(R.id.stats_header_reblog_total);
        
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        refreshStats();
        refreshStatsFromServer();
    }

    private void hideHeaderIfLandscape() {
        Display display = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        if (point.y < point.x) {
            mStatsHeader.setVisibility(View.GONE);
        } else {
            mStatsHeader.setVisibility(View.VISIBLE);
        }
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null)
            return;
            
        mNavPosition = savedInstanceState.getInt(SAVED_NAV_POSITION);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        outState.putInt(SAVED_NAV_POSITION, mNavPosition);
    }
    
    protected void showViews() {
        FragmentManager fm = getSupportFragmentManager();
        mNavFragment = (DialogFragment) fm.findFragmentByTag(StatsNavDialogFragment.TAG);
        if (mNavFragment == null)
            mNavFragment = StatsNavDialogFragment.newInstance(mNavPosition);
        if (!mNavFragment.isVisible())
            mNavFragment.show(getSupportFragmentManager(), StatsNavDialogFragment.TAG);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.stats, menu);
        return true;
    }

    @Override
    public void onItemClick(int position) {
        mNavPosition = position;
        StatsViewType viewType = StatsViewType.values()[mNavPosition];
        mActionbarNavText.setText(viewType.getLabel());

        FragmentManager fm = getSupportFragmentManager();
        StatsNavDialogFragment navFragment = (StatsNavDialogFragment) fm.findFragmentByTag(StatsNavDialogFragment.TAG);
        if (navFragment != null)
            navFragment.dismissAllowingStateLoss();
        
        mStatsViewFragment = StatsAbsViewFragment.newInstance(viewType);
        fm.beginTransaction().replace(R.id.stats_container, mStatsViewFragment, StatsAbsViewFragment.TAG).commit();
    }
    
    private void refreshStats() {
        if (WordPress.getCurrentBlog() == null)
            return; 
        
        final String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        
        new AsyncTask<String, Void, StatsSummary>() {

            @Override
            protected StatsSummary doInBackground(String... params) {
                final String blogId = params[0];
                
                StatsSummary stats = StatUtils.getSummary(blogId);
                
                return stats;
            }
            
            protected void onPostExecute(StatsSummary result) {
                refreshViews(result);
            };
        }.execute(blogId);
    }


    private void refreshStatsFromServer() {
        if (WordPress.getCurrentBlog() == null)
            return; 
        
        final String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        
        WordPress.restClient.getStatsSummary(blogId, 
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject response) {
                        StatUtils.saveSummary(blogId, response);
                        runOnUiThread(new Runnable() {
                            
                            @Override
                            public void run() {
                                refreshStats();
                                
                            }
                        });
                    }
                }, 
                new ErrorListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        
                    }
                });
    }
    
    protected void refreshViews(StatsSummary result) {
        int views = 0;
        int comments = 0;
        int favs = 0;
        int reblogs = 0;
        
        if (result != null) {
            views = result.getViews();
            comments = result.getComments();
            favs = result.getFavorites();
            reblogs = result.getReblogs();
        }
        
        mViewsTotalText.setText(views + "");
        mCommentsTotalText.setText(comments + "");
        mFavsTotalText.setText(favs + "");
        mReblogTotalText.setText(reblogs + "");
        
    }

}
