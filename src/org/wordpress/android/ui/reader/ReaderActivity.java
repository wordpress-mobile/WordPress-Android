package org.wordpress.android.ui.reader;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.posts.PostsActivity;
import org.wordpress.android.ui.reader.ReaderBaseFragment.ChangeTopicListener;
import org.wordpress.android.ui.reader.ReaderBaseFragment.GetLastSelectedItemListener;
import org.wordpress.android.ui.reader.ReaderBaseFragment.GetLoadedItemsListener;
import org.wordpress.android.ui.reader.ReaderBaseFragment.GetPermalinkListener;
import org.wordpress.android.ui.reader.ReaderBaseFragment.UpdateButtonStatusListener;
import org.wordpress.android.ui.reader.ReaderBaseFragment.UpdateTopicIDListener;
import org.wordpress.android.ui.reader.ReaderBaseFragment.UpdateTopicTitleListener;
import org.wordpress.android.ui.reader.ReaderDetailPageFragment.LoadExternalURLListener;
import org.wordpress.android.ui.reader.ReaderImplFragment.LoadDetailListener;
import org.wordpress.android.ui.reader.ReaderImplFragment.PostSelectedListener;
import org.wordpress.android.ui.reader.ReaderImplFragment.ShowTopicsListener;
import org.wordpress.android.util.WPViewPager;

public class ReaderActivity extends WPActionBarActivity implements ChangeTopicListener, PostSelectedListener, UpdateTopicIDListener,
        UpdateTopicTitleListener, GetLoadedItemsListener, UpdateButtonStatusListener, ShowTopicsListener, LoadExternalURLListener,
        GetPermalinkListener, GetLastSelectedItemListener, LoadDetailListener {

    private WPViewPager readerPager;
    private ReaderPagerAdapter readerAdapter;
    private Fragment readerPage;
    private Fragment detailPage;
    private Fragment topicPage;
    private Fragment webPage;
    private Dialog topicsDialog;
    private boolean isShare;
    private RelativeLayout topicSelector;
    private TextView topicText;
    private MenuItem refreshMenuItem;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_PROGRESS);
        
        super.onCreate(savedInstanceState);
        
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        setSupportProgressBarVisibility(false);

        if (savedInstanceState != null) {
            // Restore fragments
            readerPage = getSupportFragmentManager().getFragment(
                    savedInstanceState, ReaderImplFragment.class.getName());
            topicPage = getSupportFragmentManager().getFragment(
                    savedInstanceState, ReaderTopicsSelectorFragment.class.getName());
            detailPage = getSupportFragmentManager().getFragment(
                    savedInstanceState, ReaderDetailPageFragment.class.getName());
            webPage = getSupportFragmentManager().getFragment(
                    savedInstanceState, ReaderWebPageFragment.class.getName());
        }
        
        createMenuDrawer(R.layout.reader_wpcom_pager);
        readerPager = (WPViewPager) findViewById(R.id.pager);
        readerPager.setOffscreenPageLimit(3);

        readerAdapter = new ReaderPagerAdapter(super.getSupportFragmentManager());

        readerPager.setAdapter(readerAdapter);
        readerPager.setCurrentItem(1, true);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        // No blog selector for the Reader
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        topicSelector = (RelativeLayout) inflator.inflate(R.layout.reader_topics, null);
        topicText = (TextView) topicSelector.findViewById(R.id.topic_title);

        topicSelector.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!isFinishing())
                    showTopics();
            }

        });

        actionBar.setCustomView(topicSelector);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (refreshMenuItem != null && isAnimatingRefreshButton)
            this.stopAnimatingButton();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        getSupportFragmentManager().putFragment(outState, ReaderImplFragment.class.getName(), readerPage);
        getSupportFragmentManager().putFragment(outState, ReaderTopicsSelectorFragment.class.getName(), topicPage);
        getSupportFragmentManager().putFragment(outState, ReaderDetailPageFragment.class.getName(), detailPage);
        getSupportFragmentManager().putFragment(outState, ReaderWebPageFragment.class.getName(), webPage);
        super.onSaveInstanceState(outState);
    }

    private class ReaderPagerAdapter extends FragmentStatePagerAdapter {
        public ReaderPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public Fragment getItem(int location) {
            Fragment f = null;

            switch (location) {
            case 0:
                f = ReaderTopicsSelectorFragment.newInstance();
                topicPage = f;
                break;
            case 1:
                f = ReaderImplFragment.newInstance();
                readerPage = f;
                break;
            case 2:
                f = ReaderDetailPageFragment.newInstance();
                detailPage = f;
                break;
            case 3:
                f = ReaderWebPageFragment.newInstance();
                webPage = f;
                break;
            }

            return f;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.reader, menu);
        refreshMenuItem = menu.findItem(R.id.menu_refresh);
        if (shouldAnimateRefreshButton) {
            shouldAnimateRefreshButton = false;
            startAnimatingRefreshButton(refreshMenuItem);
        }

        if (readerPager.getCurrentItem() > 1) {
            menu.findItem(R.id.menu_refresh).setVisible(false);
            menu.findItem(R.id.menu_browser).setVisible(true);
            menu.findItem(R.id.menu_share_link).setVisible(true);
        } else {
            menu.findItem(R.id.menu_refresh).setVisible(true);
            menu.findItem(R.id.menu_browser).setVisible(false);
            menu.findItem(R.id.menu_share_link).setVisible(false);
        }
        return true;
    }

    public boolean onOptionsItemSelected(final MenuItem item) {
        final ReaderWebPageFragment readerWebPageFragment = (ReaderWebPageFragment) webPage;
        final ReaderDetailPageFragment readerPageDetailFragment = (ReaderDetailPageFragment) detailPage;

        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            ReaderImplFragment readerPageFragment = (ReaderImplFragment) readerPage;
            readerPageFragment.refreshReader();
            return true;
        } else if (itemId == R.id.menu_browser) {
            if (readerPageDetailFragment != null && readerPageDetailFragment != null) {
                if (readerPager.getCurrentItem() == 2) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            readerPageDetailFragment.wv.loadUrl("javascript:Reader2.get_article_permalink();");
                        }
                    });

                } else {
                    String url = readerWebPageFragment.wv.getUrl();
                    if (url != null) {
                        Uri uri = Uri.parse(url);
                        if (uri != null) {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(uri);
                            startActivity(i);
                        }
                    }
                }
            }
            return true;
        } else if (itemId == R.id.menu_share_link) {
            if (readerWebPageFragment != null && readerPageDetailFragment != null) {
                if (readerPager.getCurrentItem() == 2) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            isShare = true;
                            readerPageDetailFragment.wv.loadUrl("javascript:Reader2.get_article_permalink();");
                        }
                    });

                } else {
                    String url = readerWebPageFragment.wv.getUrl();
                    if (url != null) {
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("text/plain");
                        share.putExtra(Intent.EXTRA_TEXT, url);
                        startActivity(Intent.createChooser(share, getResources().getText(R.string.share_url)));
                    }
                }
            }
            return true;
        } else if (itemId == android.R.id.home) {
            if (readerPager.getCurrentItem() > 1) {
                readerPager.setCurrentItem(readerPager.getCurrentItem() - 1);
                supportInvalidateOptionsMenu();
                if (readerPager.getCurrentItem() == 1) {
                    try {
                        mMenuDrawer.setDrawerIndicatorEnabled(true);
                    } catch (Exception e) {
                    }
                }
                return true;
            } 
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // ignore orientation change
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onChangeTopic(final String topicID, final String topicName) {

        try {
            final ReaderImplFragment readerPageFragment = (ReaderImplFragment) readerPage;
            readerPageFragment.topicsID = topicID;
            runOnUiThread(new Runnable() {
                public void run() {
                    String methodCall = "Reader2.load_topic('" + topicID + "')";
                    readerPageFragment.wv.loadUrl("javascript:" + methodCall);
                    if (topicName != null) {
                        topicText.setText(topicName);
                    }
                    if (readerPager.getCurrentItem() != 1) {
                        readerPager.setCurrentItem(1, true);
                        supportInvalidateOptionsMenu();
                    }
                }
            });
        } catch (Exception e) {
        }
    }

    @Override
    public void onPostSelected(String requestedURL) {
        readerPager.setCurrentItem(2, true);
        try {
            mMenuDrawer.setDrawerIndicatorEnabled(false);
        } catch (Exception e) {
        }
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onBackPressed() {
        if (readerPager.getCurrentItem() > 1) {
            if (readerPager.getCurrentItem() == 2) {
                ReaderDetailPageFragment readerPageDetailFragment = (ReaderDetailPageFragment) detailPage;
                readerPageDetailFragment.wv.loadUrl("javascript:Reader2.clear_article_details();");
            }
            readerPager.setCurrentItem(readerPager.getCurrentItem() - 1, true);
            if (readerPager.getCurrentItem() == 1) {
                try {
                    mMenuDrawer.setDrawerIndicatorEnabled(true);
                } catch (Exception e) {
                }
            }
            supportInvalidateOptionsMenu();
        } else
            super.onBackPressed();
    }

    @Override
    public void updateTopicTitle(final String topicTitle) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (topicsDialog != null) {
                    if (topicsDialog.isShowing())
                        topicsDialog.cancel();
                }
                if (topicTitle != null) {
                    topicText.setText(topicTitle);
                }
                // readerPager.setCurrentItem(1, true);
            }
        });

    }

    @Override
    public void onUpdateTopicID(String topicID) {
        if (topicPage == null)
            topicPage = readerAdapter.getItem(0);
        final ReaderTopicsSelectorFragment topicsFragment = (ReaderTopicsSelectorFragment) topicPage;
        final String methodCall = "document.setSelectedTopic('" + topicID + "')";
        runOnUiThread(new Runnable() {
            public void run() {
                if (topicsFragment.wv != null)
                    topicsFragment.wv.loadUrl("javascript:" + methodCall);
            }
        });
    }

    @Override
    public void getLoadedItems(String items) {
        if (items == null)
            return;
        if (!items.equals("[]")) {
            final ReaderDetailPageFragment readerPageDetailFragment = (ReaderDetailPageFragment) detailPage;
            readerPageDetailFragment.readerItems = items;
            final String method = "Reader2.set_loaded_items(" + readerPageDetailFragment.readerItems + ")";
            runOnUiThread(new Runnable() {
                public void run() {
                    readerPageDetailFragment.wv.loadUrl("javascript:" + method);
                }
            });
        }
    }

    @Override
    public void updateButtonStatus(final int button, final boolean enabled) {
        final ReaderDetailPageFragment readerPageDetailFragment = (ReaderDetailPageFragment) detailPage;
        runOnUiThread(new Runnable() {
            public void run() {
                readerPageDetailFragment.updateButtonStatus(button, enabled);
            }
        });

    }

    @Override
    public void showTopics() {
        ReaderTopicsSelectorFragment topicsFragment = (ReaderTopicsSelectorFragment) topicPage;
        ((ViewGroup) topicsFragment.getView().getParent()).removeView(topicsFragment.getView());
        topicsDialog = new Dialog(this);
        topicsDialog.setContentView(topicsFragment.getView());
        topicsDialog.setTitle(getResources().getText(R.string.topics));
        topicsDialog.setCancelable(true);
        if (!isFinishing()) {
            try {
                topicsDialog.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void loadExternalURL(String url) {
        ReaderWebPageFragment readerWebPageFragment = (ReaderWebPageFragment) webPage;
        readerWebPageFragment.wv.clearView();
        readerWebPageFragment.wv.loadUrl(url);
        readerPager.setCurrentItem(3, true);
    }

    @Override
    public void getPermalink(String permalink) {
        if (!permalink.equals("")) {
            if (isShare) {
                isShare = false;
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, permalink);
                startActivity(Intent.createChooser(share, getResources().getText(R.string.share_link)));
            } else {
                Uri uri = Uri.parse(permalink);
                if (uri != null) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(uri);
                    startActivity(i);
                }
            }
        }

    }

    @Override
    public void getLastSelectedItem(final String lastSelectedItem) {
        final ReaderDetailPageFragment readerPageDetailFragment = (ReaderDetailPageFragment) detailPage;
        runOnUiThread(new Runnable() {
            public void run() {
                String methodCall = "Reader2.show_article_details(" + lastSelectedItem + ")";
                if (readerPageDetailFragment.wv != null) {
                    readerPageDetailFragment.wv.loadUrl("javascript:" + methodCall);
                    readerPageDetailFragment.wv.loadUrl("javascript:Reader2.is_next_item();");
                    readerPageDetailFragment.wv.loadUrl("javascript:Reader2.is_prev_item();");
                }
            }
        });
    }

    @Override
    public void onLoadDetail() {
        final ReaderDetailPageFragment readerPageDetailFragment = (ReaderDetailPageFragment) detailPage;
        runOnUiThread(new Runnable() {
            public void run() {
                readerPageDetailFragment.wv.loadUrl(Constants.readerDetailURL);
            }
        });
    }

    public void startAnimatingButton() {
        shouldAnimateRefreshButton = true;
        startAnimatingRefreshButton(refreshMenuItem);
    }

    public void stopAnimatingButton() {
        stopAnimatingRefreshButton(refreshMenuItem);
    }

    @Override
    public void onBlogChanged() {
        super.onBlogChanged();
        
        if (WordPress.currentBlog.isDotcomFlag()) {
            ReaderImplFragment readerPageFragment = (ReaderImplFragment) readerPage;
            readerPageFragment.reloadReader();
        }
        
    }
    
    
    
}
