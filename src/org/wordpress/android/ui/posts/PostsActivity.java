package org.wordpress.android.ui.posts;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.ui.MenuDrawerItem;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.notifications.NotificationsActivity;
import org.wordpress.android.ui.posts.PostsListFragment.OnPostActionListener;
import org.wordpress.android.ui.posts.PostsListFragment.OnPostSelectedListener;
import org.wordpress.android.ui.posts.PostsListFragment.OnRefreshListener;
import org.wordpress.android.ui.posts.ViewPostFragment.OnDetailPostActionListener;
import org.wordpress.android.util.WPAlertDialogFragment.OnDialogConfirmListener;
import org.wordpress.android.util.WPMobileStatsUtil;
import org.wordpress.passcodelock.AppLockManager;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

public class PostsActivity extends WPActionBarActivity implements OnPostSelectedListener,
        OnRefreshListener, OnPostActionListener, OnDetailPostActionListener, OnDialogConfirmListener {
    private static final int ID_DIALOG_DELETING = 1, ID_DIALOG_SHARE = 2;
    public static final int POST_DELETE = 0, POST_SHARE = 1, POST_EDIT = 2, POST_CLEAR = 3,
            POST_VIEW = 5;
    public static final int ACTIVITY_EDIT_POST = 0;

    private PostsListFragment mPostList;
    private MenuItem mRefreshMenuItem;

    public ProgressDialog mLoadingDialog;
    public boolean mIsPage = false;
    public String mErrorMsg = "";
    public boolean mIsRefreshing = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Special check for a null database (see #507)
        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Check if we came from a notification, if so let's launch NotificationsActivity
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean(NotificationsActivity.FROM_NOTIFICATION_EXTRA)) {
            startNotificationsActivity(extras);
            return;
        }

        // Restore last selection on app creation
        if (WordPress.shouldRestoreSelectedActivity && WordPress.getCurrentBlog() != null
                && !(this instanceof PagesActivity)) {
            // Refresh blog content when returning to the app
            new ApiHelper.RefreshBlogContentTask(this, WordPress.getCurrentBlog(),
                    new ApiHelper.GenericCallback() {
                @Override
                public void onSuccess() {
                    if (!isFinishing())
                        updateMenuDrawer();
                }

                @Override
                public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                }
            }).execute(false);

            WordPress.shouldRestoreSelectedActivity = false;
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            int lastActivitySelection = settings.getInt(LAST_ACTIVITY_PREFERENCE, -1);
            if (lastActivitySelection > MenuDrawerItem.NO_ITEM_ID && lastActivitySelection != WPActionBarActivity.DASHBOARD_ACTIVITY) {
                Iterator<MenuDrawerItem> itemIterator = mMenuItems.iterator();
                while(itemIterator.hasNext()){
                    MenuDrawerItem item = itemIterator.next();
                    // if we have a matching item id, and it's not selected and it's visible, call it
                    if (item.hasItemId() && item.getItemId() == lastActivitySelection && !item.isSelected() && item.isVisible()) {
                        mFirstLaunch = true;
                        item.selectItem();
                        finish();
                        return;
                    }
                }
            }
        }

        createMenuDrawer(R.layout.posts);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);

        FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);
        mPostList = (PostsListFragment) fm.findFragmentById(R.id.postList);

        if (extras != null) {
            mIsPage = extras.getBoolean("viewPages");
            showErrorDialogIfNeeded(extras);
        }

        if (mIsPage)
            setTitle(getString(R.string.pages));
        else
            setTitle(getString(R.string.posts));

        WordPress.currentPost = null;

         WordPress.setOnPostUploadedListener(new WordPress.OnPostUploadedListener(){

            @Override
            public void OnPostUploaded() {
                if (isFinishing())
                    return;

                checkForLocalChanges(false);
            }

         });

        if (savedInstanceState != null)
            popPostDetail();

        WPMobileStatsUtil.trackEventForWPCom(statEventForViewOpening());
    }

    private void showPostUploadErrorAlert(String errorMessage, String infoTitle,
                                          final String infoURL) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(PostsActivity.this);
        dialogBuilder.setTitle(getResources().getText(R.string.error));
        dialogBuilder.setMessage(errorMessage);
        dialogBuilder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Just close the window.
                    }
                });
        if (infoTitle != null && infoURL != null) {
            dialogBuilder.setNeutralButton(infoTitle,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(infoURL)));
                    }
                });
        }
        dialogBuilder.setCancelable(true);
        if (!isFinishing())
            dialogBuilder.create().show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Bundle extras = intent.getExtras();
        if (extras != null) {
            // Check if we came from a notification, if so let's launch NotificationsActivity
            if (extras.getBoolean(NotificationsActivity.FROM_NOTIFICATION_EXTRA)) {
                startNotificationsActivity(extras);
                return;
            }
        }
    }

    private void showErrorDialogIfNeeded(Bundle extras) {
        if (extras == null) {
            return ;
        }
        String errorMessage = extras.getString("errorMessage");
        String errorInfoTitle = extras.getString("errorInfoTitle");
        String errorInfoLink = extras.getString("errorInfoLink");
        if (errorMessage != null) {
            showPostUploadErrorAlert(errorMessage, errorInfoTitle, errorInfoLink);
        }
    }

    private void startNotificationsActivity(Bundle extras) {
        // Manually set last selection to notifications
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(LAST_ACTIVITY_PREFERENCE, NOTIFICATIONS_ACTIVITY);
        editor.commit();

        Intent i = new Intent(this, NotificationsActivity.class);
        i.putExtras(extras);
        startActivity(i);
        finish();
    }

    private FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0)
                mMenuDrawer.setDrawerIndicatorEnabled(true);
        }
    };

    protected void checkForLocalChanges(boolean shouldPrompt) {
        if (WordPress.getCurrentBlog() == null)
            return;
        boolean hasLocalChanges = WordPress.wpDB.findLocalChanges(WordPress.getCurrentBlog().getLocalTableBlogId(), mIsPage);
        if (hasLocalChanges) {
            if (!shouldPrompt)
                return;
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                    PostsActivity.this);
            dialogBuilder.setTitle(getResources().getText(
                    R.string.local_changes));
            dialogBuilder.setMessage(getResources().getText(R.string.remote_changes));
            dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            popPostDetail();
                            attemptToSelectPost();
                            mPostList.refreshPosts(false);
                        }
                    });
            dialogBuilder.setNegativeButton(getResources().getText(R.string.no),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            //just close the window
                        }
                    });
            dialogBuilder.setCancelable(true);
            if (!isFinishing()) {
                dialogBuilder.create().show();
            }
        } else {
            popPostDetail();
            attemptToSelectPost();
            mShouldAnimateRefreshButton = true;
            mPostList.refreshPosts(false);
        }
    }

    protected void popPostDetail() {
        FragmentManager fm = getSupportFragmentManager();
        ViewPostFragment f = (ViewPostFragment) fm
                .findFragmentById(R.id.postDetail);
        if (f == null) {
            try {
                fm.popBackStack();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (WordPress.isSignedIn(PostsActivity.this)) {
            showReaderIfNoBlog();
        }
        if (mPostList.getListView().getCount() == 0)
            mPostList.loadPosts(false);
        if (WordPress.postsShouldRefresh) {
            checkForLocalChanges(false);
            WordPress.postsShouldRefresh = false;
        }
        attemptToSelectPost();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mIsRefreshing)
            stopAnimatingRefreshButton(mRefreshMenuItem);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mPostList.getPostsTask != null)
            mPostList.getPostsTask.cancel(true);
    }

    @Override
    protected void onDestroy() {
        WPMobileStatsUtil.trackEventForWPComWithSavedProperties(statEventForViewClosing());
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.posts, menu);
        mRefreshMenuItem = menu.findItem(R.id.menu_refresh);

        if (mIsPage) {
            menu.findItem(R.id.menu_new_post).setTitle(R.string.new_page);
        }

        if (mShouldAnimateRefreshButton) {
            mShouldAnimateRefreshButton = false;
            onRefresh(true);
        }
        return true;
    }

    public void newPost() {
        WPMobileStatsUtil.trackEventForWPCom(statEventForNewPost());
        if (WordPress.getCurrentBlog() == null) {
            if (!isFinishing())
                Toast.makeText(this, R.string.blog_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        // Create a new post object
        Post newPost = new Post(WordPress.getCurrentBlog().getLocalTableBlogId(), mIsPage);
        Intent i = new Intent(this, EditPostActivity.class);
        i.putExtra(EditPostActivity.EXTRA_POSTID, newPost.getId());
        i.putExtra(EditPostActivity.EXTRA_IS_PAGE, mIsPage);
        i.putExtra(EditPostActivity.EXTRA_IS_NEW_POST, true);
        startActivityForResult(i, ACTIVITY_EDIT_POST);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            checkForLocalChanges(true);
            new ApiHelper.RefreshBlogContentTask(this, WordPress.getCurrentBlog(), null).execute(false);
            return true;
        } else if (itemId == R.id.menu_new_post) {
            newPost();
            return true;
        } else if (itemId == android.R.id.home) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                popPostDetail();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            if (requestCode == ACTIVITY_EDIT_POST && resultCode == RESULT_OK) {
                if (data.getBooleanExtra("shouldRefresh", false)) {
                    mPostList.loadPosts(false);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void attemptToSelectPost() {
        FragmentManager fm = getSupportFragmentManager();
        ViewPostFragment f = (ViewPostFragment) fm.findFragmentById(R.id.postDetail);
        if (f != null && f.isInLayout()) {
            mPostList.shouldSelectAfterLoad = true;
        }
    }

    @Override
    public void onPostSelected(Post post) {
        FragmentManager fm = getSupportFragmentManager();
        ViewPostFragment f = (ViewPostFragment) fm
                .findFragmentById(R.id.postDetail);

        if (post != null) {

            WordPress.currentPost = post;
            if (f == null || !f.isInLayout()) {
                FragmentTransaction ft = fm.beginTransaction();
                ft.hide(mPostList);
                f = new ViewPostFragment();
                ft.add(R.id.postDetailFragmentContainer, f);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.addToBackStack(null);
                ft.commitAllowingStateLoss();
                mMenuDrawer.setDrawerIndicatorEnabled(false);
            } else {
                f.loadPost(post);
            }
        }
    }

    @Override
    public void onRefresh(final boolean start) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (start) {
                    attemptToSelectPost();
                    mShouldAnimateRefreshButton = true;
                    startAnimatingRefreshButton(mRefreshMenuItem);
                    mIsRefreshing = true;
                } else {
                    mShouldAnimateRefreshButton = false;
                    stopAnimatingRefreshButton(mRefreshMenuItem);
                    mIsRefreshing = false;
                }
            }
        });
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        mLoadingDialog = new ProgressDialog(this);
        if (id == ID_DIALOG_DELETING) {
            mLoadingDialog.setMessage(getResources().getText(
                    mIsPage ? R.string.deleting_page : R.string.deleting_post));
            mLoadingDialog.setCancelable(false);
            return mLoadingDialog;
        } else if (id == ID_DIALOG_SHARE) {
            mLoadingDialog.setMessage(mIsPage ? getString(R.string.share_url_page) : getString(
                    R.string.share_url_post));
            mLoadingDialog.setCancelable(false);
            return mLoadingDialog;
        }

        return super.onCreateDialog(id);
    }

    protected String statEventForViewOpening() {
        return WPMobileStatsUtil.StatsEventPostsOpened;
    }

    protected String statEventForViewClosing() {
        return WPMobileStatsUtil.StatsEventPostsClosed;
    }

    protected String statEventForNewPost() {
        return WPMobileStatsUtil.StatsEventPostsClickedNewPost;
    }

    public class deletePostTask extends AsyncTask<Post, Void, Boolean> {

        Post post;

        @Override
        protected void onPreExecute() {
            // pop out of the detail view if on a smaller screen
            popPostDetail();
            showDialog(ID_DIALOG_DELETING);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            dismissDialog(ID_DIALOG_DELETING);
            attemptToSelectPost();
            if (result) {
                Toast.makeText(PostsActivity.this, getResources().getText((mIsPage) ?
                        R.string.page_deleted : R.string.post_deleted),
                        Toast.LENGTH_SHORT).show();
                checkForLocalChanges(false);
                post.delete();
                attemptToSelectPost();
                mPostList.loadPosts(false);
            } else {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(PostsActivity.this);
                dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
                dialogBuilder.setMessage(mErrorMsg);
                dialogBuilder.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Just close the window.
                            }
                        });
                dialogBuilder.setCancelable(true);
                if (!isFinishing()) {
                    dialogBuilder.create().show();
                }
            }
        }

        @Override
        protected Boolean doInBackground(Post... params) {
            boolean result = false;
            post = params[0];
            XMLRPCClient client = new XMLRPCClient(
                    WordPress.currentBlog.getUrl(),
                    WordPress.currentBlog.getHttpuser(),
                    WordPress.currentBlog.getHttppassword());

            Object[] postParams = { "", post.getPostid(),
                    WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword() };
            Object[] pageParams = { WordPress.currentBlog.getRemoteBlogId(),
                    WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword(), post.getPostid() };

            try {
                client.call((mIsPage) ? "wp.deletePage" : "blogger.deletePost",
                        (mIsPage) ? pageParams : postParams);
                result = true;
            } catch (final XMLRPCException e) {
                mErrorMsg = String.format(getResources().getString(R.string.error_delete_post),
                        (mIsPage) ? getResources().getText(R.string.page)
                                  : getResources().getText(R.string.post));
                result = false;
            }
            return result;
        }
    }

    public class refreshCommentsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            Object[] commentParams = { WordPress.currentBlog.getRemoteBlogId(),
                    WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword() };

            try {
                ApiHelper.refreshComments(PostsActivity.this, commentParams);
            } catch (final XMLRPCException e) {
                mErrorMsg = getResources().getText(R.string.error_generic).toString();
            }
            return null;
        }

    }

    public class shareURLTask extends AsyncTask<Post, Void, String> {

        Post post;

        @Override
        protected void onPreExecute() {
            showDialog(ID_DIALOG_SHARE);
        }

        @Override
        protected void onPostExecute(String shareURL) {
            dismissDialog(ID_DIALOG_SHARE);
            if (shareURL == null) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                        PostsActivity.this);
                dialogBuilder.setTitle(getResources().getText(
                        R.string.connection_error));
                dialogBuilder.setMessage(mErrorMsg);
                dialogBuilder.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                // Just close the window.
                            }
                        });
                dialogBuilder.setCancelable(true);
                if (!isFinishing()) {
                    dialogBuilder.create().show();
                }
            } else {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_SUBJECT, post.getTitle());
                share.putExtra(Intent.EXTRA_TEXT, shareURL);
                startActivity(Intent.createChooser(share, getResources()
                        .getText(R.string.share_url)));
                AppLockManager.getInstance().setExtendedTimeout();
            }

        }

        @Override
        protected String doInBackground(Post... params) {
            String result = null;
            post = params[0];
            if (post == null)
                return null;
            XMLRPCClient client = new XMLRPCClient(
                    WordPress.currentBlog.getUrl(),
                    WordPress.currentBlog.getHttpuser(),
                    WordPress.currentBlog.getHttppassword());

            Object versionResult = new Object();
            try {
                if (mIsPage) {
                    Object[] vParams = { WordPress.currentBlog.getRemoteBlogId(),
                            post.getPostid(),
                            WordPress.currentBlog.getUsername(),
                            WordPress.currentBlog.getPassword() };
                    versionResult = (Object) client.call("wp.getPage", vParams);
                } else {
                    Object[] vParams = { post.getPostid(),
                            WordPress.currentBlog.getUsername(),
                            WordPress.currentBlog.getPassword() };
                    versionResult = (Object) client.call("metaWeblog.getPost",
                            vParams);
                }
            } catch (XMLRPCException e) {
                mErrorMsg = getResources().getText(R.string.error_generic).toString();
                return null;
            }

            if (versionResult != null) {
                try {
                    Map<?, ?> contentHash = (Map<?, ?>) versionResult;
                    if ((mIsPage && !"publish".equals(contentHash.get("page_status").toString()))
                            || (!mIsPage && !"publish".equals(
                            contentHash.get("post_status").toString()))) {
                        if (mIsPage) {
                            mErrorMsg = getString(R.string.page_not_published);
                        } else {
                            mErrorMsg = getString(R.string.post_not_published);
                        }
                        return null;
                    } else {
                        String postURL = contentHash.get("permaLink").toString();
                        String shortlink = getShortlinkTagHref(postURL);
                        if (shortlink == null) {
                            result = postURL;
                        } else {
                            result = shortlink;
                        }
                    }
                } catch (Exception e) {
                    mErrorMsg = getResources().getText(R.string.error_generic).toString();
                    return null;
                }
            }

            return result;
        }
    }

    protected void refreshComments() {
        new refreshCommentsTask().execute();
    }

    private String getShortlinkTagHref(String urlString) {
        String html = getHTML(urlString);

        if (html != "") {
            try {
                int location = html.indexOf("http://wp.me");
                String shortlink = html.substring(location, location + 30);
                shortlink = shortlink.substring(0, shortlink.indexOf("'"));
                return shortlink;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null; // never found the shortlink tag
    }

    public String getHTML(String urlSource) {
          URL url;
          HttpURLConnection conn;
          BufferedReader rd;
          String line;
          String result = "";
          try {
             url = new URL(urlSource);
             conn = (HttpURLConnection) url.openConnection();
             conn.setRequestMethod("GET");
             rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
             while ((line = rd.readLine()) != null) {
                result += line;
             }
             rd.close();
          } catch (Exception e) {
             e.printStackTrace();
          }
          return result;
       }

    @Override
    public void onPostAction(int action, final Post post) {
        if (mPostList.getPostsTask != null) {
            mPostList.getPostsTask.cancel(true);
            //titleBar.stopRotatingRefreshIcon();
            mIsRefreshing = false;
        }

        // No post? No service.
        if (post == null) {
            Toast.makeText(PostsActivity.this, R.string.post_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        if (action == POST_DELETE) {
            WPMobileStatsUtil.flagProperty(statEventForViewClosing(), WPMobileStatsUtil.StatsPropertyPostDetailClickedDelete);
            if (post.isLocalDraft()) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                        PostsActivity.this);
                dialogBuilder.setTitle(getResources().getText(
                        R.string.delete_draft));
                dialogBuilder.setMessage(getResources().getText(
                        R.string.delete_sure)
                        + " '" + post.getTitle() + "'?");
                dialogBuilder.setPositiveButton(
                        getResources().getText(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                post.delete();
                                popPostDetail();
                                attemptToSelectPost();
                                mPostList.loadPosts(false);
                            }
                        });
                dialogBuilder.setNegativeButton(
                        getResources().getText(R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                // Just close the window.

                            }
                        });
                dialogBuilder.setCancelable(true);
                if (!isFinishing()) {
                    dialogBuilder.create().show();
                }

            } else {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                        PostsActivity.this);
                dialogBuilder.setTitle(getResources().getText(
                        (post.isPage()) ? R.string.delete_page
                                : R.string.delete_post));
                dialogBuilder.setMessage(getResources().getText(
                        (post.isPage()) ? R.string.delete_sure_page
                                : R.string.delete_sure_post)
                        + " '" + post.getTitle() + "'?");
                dialogBuilder.setPositiveButton(
                        getResources().getText(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                new deletePostTask().execute(post);
                            }
                        });
                dialogBuilder.setNegativeButton(
                        getResources().getText(R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                // Just close the window.

                            }
                        });
                dialogBuilder.setCancelable(true);
                if (!isFinishing()) {
                    dialogBuilder.create().show();
                }

            }
        } else if (action == POST_SHARE) {
            new shareURLTask().execute(post);
            WPMobileStatsUtil.flagProperty(statEventForViewClosing(), WPMobileStatsUtil.StatsPropertyPostDetailClickedShare);
        } else if (action == POST_CLEAR) {
            FragmentManager fm = getSupportFragmentManager();
            ViewPostFragment f = (ViewPostFragment) fm
                    .findFragmentById(R.id.postDetail);
            if (f != null) {
                f.clearContent();
            }
        } else if (action == POST_EDIT) {
            WPMobileStatsUtil.flagProperty(statEventForViewClosing(), WPMobileStatsUtil.StatsPropertyPostDetailClickedEdit);
        } else if (action == POST_VIEW) {
            WPMobileStatsUtil.flagProperty(statEventForViewClosing(), WPMobileStatsUtil.StatsPropertyPostDetailClickedPreview);
        }
    }

    @Override
    public void onDetailPostAction(int action, Post post) {

        onPostAction(action, post);

    }

    @Override
    public void onDialogConfirm() {
        mPostList.switcher.showNext();
        mPostList.numRecords += 30;
        mPostList.refreshPosts(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBlogChanged() {
        super.onBlogChanged();
        popPostDetail();
        attemptToSelectPost();
        mPostList.loadPosts(false);
        new ApiHelper.RefreshBlogContentTask(this, WordPress.currentBlog, null).execute(false);
    }
}
