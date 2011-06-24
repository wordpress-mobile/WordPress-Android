package org.wordpress.android;

import org.wordpress.android.util.AlertUtil;

import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TabHost;
import android.widget.Toast;

public class TabView extends TabActivity {
    private String id = "";
    private String accountName = "";
    private String activateTab = "", action = "";
    boolean fromNotification = false;
    int uploadID = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            id = extras.getString("id");
            accountName = extras.getString("accountName");
            activateTab = extras.getString("activateTab");
            fromNotification = extras.getBoolean("fromNotification", false);
            action = extras.getString("action");
            uploadID = extras.getInt("uploadID");
        }

        setTitle(accountName);

        Intent tab1 = new Intent(this, ViewComments.class);
        Intent tab2 = new Intent(this, ViewPosts.class);
        Intent tab3 = new Intent(this, ViewPosts.class);
        Intent tab4 = new Intent(this, ViewStats.class);

        Bundle bundle = new Bundle();
        bundle.putString("accountName", accountName);
        bundle.putString("id", id);

        if (fromNotification) {
            bundle.putBoolean("fromNotification", true);
        }

        tab1.putExtras(bundle);
        tab4.putExtras(bundle);

        if (action != null) {
            bundle.putString("action", action);
            bundle.putInt("uploadID", uploadID);
        }

        tab2.putExtras(bundle);
        bundle.putBoolean("viewPages", true);
        tab3.putExtras(bundle);

        final TabHost host = getTabHost();
        host.addTab(host.newTabSpec("one").setIndicator(
                getResources().getText(R.string.tab_comments),
                getResources().getDrawable(R.layout.comment_tab_selector))
                .setContent(tab1));
        host.addTab(host.newTabSpec("two").setIndicator(
                getResources().getText(R.string.tab_posts),
                getResources().getDrawable(R.layout.posts_tab_selector))
                .setContent(tab2));
        host.addTab(host.newTabSpec("three").setIndicator(
                getResources().getText(R.string.tab_pages),
                getResources().getDrawable(R.layout.pages_tab_selector))
                .setContent(tab3));
        host.addTab(host.newTabSpec("four").setIndicator(
                getResources().getText(R.string.tab_stats),
                getResources().getDrawable(R.layout.stats_tab_selector))
                .setContent(tab4));

        if (activateTab != null) {
            if (activateTab.equals("posts")) {
                host.setCurrentTab(1);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // ignore orientation change
        super.onConfigurationChanged(newConfig);
    }

    // Add settings to menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 0, 0, getResources().getText(R.string.blog_settings));
        MenuItem menuItem1 = menu.findItem(0);
        menuItem1.setIcon(R.drawable.ic_menu_prefs);
        menu.add(0, 1, 0, getResources().getText(R.string.remove_account));
        MenuItem menuItem2 = menu.findItem(1);
        menuItem2.setIcon(R.drawable.ic_menu_close_clear_cancel);
        return true;
    }

    // Menu actions
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case 0:

            Bundle bundle = new Bundle();
            bundle.putString("id", id);
            bundle.putString("accountName", accountName);
            Intent i = new Intent(this, Settings.class);
            i.putExtras(bundle);
            startActivity(i);
            return true;

        case 1:
            DialogInterface.OnClickListener positiveListener = 
                new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    WordPressDB settingsDB = new WordPressDB(
                            TabView.this);
                    boolean deleteSuccess = settingsDB.deleteAccount(
                            TabView.this, id);
                    if (deleteSuccess) {
                        Toast.makeText(TabView.this, getResources().getText(
                                        R.string.blog_removed_successfully),
                                Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        AlertUtil.showAlert(TabView.this, R.string.error,
                                R.string.could_not_remove_account);
                    }
                }
            };
            
            AlertUtil.showAlert(TabView.this, R.string.remove_account,
                    R.string.sure_to_remove_account,
                    getString(R.string.yes), positiveListener,
                    getString(R.string.no), null);
            
            return true;
        }
        return false;
    }
}
