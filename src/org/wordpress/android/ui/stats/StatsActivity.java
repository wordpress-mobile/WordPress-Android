package org.wordpress.android.ui.stats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
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
import org.wordpress.android.ui.OldStatsActivity;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.util.StatsRestHelper;

public class StatsActivity extends WPActionBarActivity implements StatsNavDialogFragment.NavigationListener {

    private static final String SAVED_NAV_POSITION = "SAVED_NAV_POSITION";
    
    private StatsAbsViewFragment mStatsViewFragment;
    private View mActionbarNav;
    private TextView mActionbarNavText;
    private DialogFragment mNavFragment;
    private int mNavPosition = 0;

    private MenuItem mRefreshMenuItem;
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(StatsRestHelper.REFRESH_VIEW_TYPE)) {
                
                if (mRefreshMenuItem == null)
                    return;
                
                boolean started = intent.getBooleanExtra(StatsRestHelper.REFRESH_VIEW_TYPE_STARTED, false);
                int ordinal = intent.getIntExtra(StatsRestHelper.REFRESH_VIEW_TYPE_ORDINAL, -1);
                if (ordinal == -1 && !started) {
                    stopAnimatingRefreshButton(mRefreshMenuItem);
                } else if (mStatsViewFragment != null && mStatsViewFragment.getViewType().ordinal() == ordinal) {
                    if (started)
                        startAnimatingRefreshButton(mRefreshMenuItem);
                    else
                        stopAnimatingRefreshButton(mRefreshMenuItem);
                            
                }
            }
        }
    };
    
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
        mActionbarNavText.setText(StatsViewType.getImplemented()[mNavPosition].getLabel());
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
            mStatsViewFragment = StatsAbsViewFragment.newInstance(StatsViewType.getImplemented()[0]);
            fm.beginTransaction().add(R.id.stats_container, mStatsViewFragment, StatsAbsViewFragment.TAG).commit();
        }
        
        mNavFragment = (DialogFragment) fm.findFragmentByTag(StatsNavDialogFragment.TAG);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(mReceiver, new IntentFilter(StatsRestHelper.REFRESH_VIEW_TYPE));
        
        refreshStats();
    }
    
    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(mReceiver);
        
        stopAnimatingRefreshButton(mRefreshMenuItem);
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
        else if (mNavFragment.getDialog().isShowing())
            return;
            
        if (!mNavFragment.isVisible())
            mNavFragment.show(getSupportFragmentManager(), StatsNavDialogFragment.TAG);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.stats, menu);
        mRefreshMenuItem = menu.findItem(R.id.menu_refresh);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            refreshStats();
            return true;
        } else if (item.getItemId() == R.id.menu_view_stats_full_site) {
            Intent intent = new Intent(this, OldStatsActivity.class);
            intent.putExtra("id", WordPress.currentBlog.getId());
            intent.putExtra("isNew", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityWithDelay(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onItemClick(int position) {
        mNavPosition = position;
        StatsViewType viewType = StatsViewType.getImplemented()[mNavPosition];
        mActionbarNavText.setText(viewType.getLabel());

        FragmentManager fm = getSupportFragmentManager();
        StatsNavDialogFragment navFragment = (StatsNavDialogFragment) fm.findFragmentByTag(StatsNavDialogFragment.TAG);
        if (navFragment != null)
            navFragment.dismissAllowingStateLoss();
        
        mStatsViewFragment = StatsAbsViewFragment.newInstance(viewType);
        fm.beginTransaction().replace(R.id.stats_container, mStatsViewFragment, StatsAbsViewFragment.TAG).commit();
        refreshStats();
    }

    @Override
    public void onBlogChanged() {
        super.onBlogChanged();
        refreshStats();
    }

    private void refreshStats() {
        if (WordPress.getCurrentBlog() == null)
            return;
        
        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        
        StatsRestHelper.getStatsSummary(blogId);
        
        if (mStatsViewFragment != null) {
            StatsViewType viewType = mStatsViewFragment.getViewType();
            StatsRestHelper.getStats(viewType, blogId);
        }
    }
}
