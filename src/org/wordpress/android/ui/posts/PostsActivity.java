package org.wordpress.android.ui.posts;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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

import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.posts.ViewPostFragment.OnDetailPostActionListener;
import org.wordpress.android.ui.posts.ViewPostsFragment.OnPostActionListener;
import org.wordpress.android.ui.posts.ViewPostsFragment.OnPostSelectedListener;
import org.wordpress.android.ui.posts.ViewPostsFragment.OnRefreshListener;
import org.wordpress.android.util.WPAlertDialogFragment.OnDialogConfirmListener;

public class PostsActivity extends WPActionBarActivity implements OnPostSelectedListener,
        OnRefreshListener, OnPostActionListener, OnDetailPostActionListener, OnDialogConfirmListener {

    private ViewPostsFragment postList;
    private int ID_DIALOG_DELETING = 1, ID_DIALOG_SHARE = 2;
    public static int POST_DELETE = 0, POST_SHARE = 1, POST_EDIT = 2, POST_CLEAR = 3;
    public ProgressDialog loadingDialog;
    public boolean isPage = false;
    public String errorMsg = "";
    public boolean isRefreshing = false;
    private MenuItem refreshMenuItem;
    private int ACTIVITY_EDIT_POST = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        createMenuDrawer(R.layout.posts);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);

        FragmentManager fm = getSupportFragmentManager();
        postList = (ViewPostsFragment) fm.findFragmentById(R.id.postList);
        postList.setListShown(true);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            isPage = extras.getBoolean("viewPages");
            String errorMessage = extras.getString("errorMessage");
            if (errorMessage != null) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                        PostsActivity.this);
                dialogBuilder.setTitle(getResources().getText(
                        R.string.error));
                dialogBuilder.setMessage(errorMessage);
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
            }
        }
        
        if (isPage)
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

        attemptToSelectPost();
    }

    protected void checkForLocalChanges(boolean shouldPrompt) {
        boolean hasLocalChanges = WordPress.wpDB.findLocalChanges();
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
                            postList.refreshPosts(false);
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
            shouldAnimateRefreshButton = true;
            postList.refreshPosts(false);
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
        if (postList.getListView().getCount() == 0)
            postList.loadPosts(false);
        if (WordPress.postsShouldRefresh) {
            checkForLocalChanges(false);
            WordPress.postsShouldRefresh = false;
        }
        attemptToSelectPost();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRefreshing)
            stopAnimatingRefreshButton(refreshMenuItem);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (postList.getPostsTask != null)
            postList.getPostsTask.cancel(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.posts, menu);
        refreshMenuItem = menu.findItem(R.id.menu_refresh);
        if (shouldAnimateRefreshButton) {
            shouldAnimateRefreshButton = false;
            startAnimatingRefreshButton(refreshMenuItem);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            postList.refreshPosts(false);
            new ApiHelper.RefreshBlogContentTask(this, WordPress.currentBlog).execute(true);
            return true;
        } else if (itemId == R.id.menu_new_post) {
            Intent i = new Intent(this, EditPostActivity.class);
            i.putExtra("id", WordPress.currentBlog.getId());
            i.putExtra("isNew", true);
            if (isPage)
                i.putExtra("isPage", true);
            startActivityForResult(i, ACTIVITY_EDIT_POST);
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
        if (requestCode == ACTIVITY_EDIT_POST && resultCode == RESULT_OK && data != null) {
            if (data.getBooleanExtra("shouldRefresh", false))
                postList.loadPosts(false);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void attemptToSelectPost() {

        FragmentManager fm = getSupportFragmentManager();
        ViewPostFragment f = (ViewPostFragment) fm
                .findFragmentById(R.id.postDetail);

        if (f != null && f.isInLayout()) {
            postList.shouldSelectAfterLoad = true;
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
                ft.hide(postList);
                f = new ViewPostFragment();
                ft.add(R.id.postDetailFragmentContainer, f);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.addToBackStack(null);
                ft.commit();
            } else {
                f.loadPost(post);
            }
        }
    }

    @Override
    public void onRefresh(boolean start) {
        if (start) {
            attemptToSelectPost();
            shouldAnimateRefreshButton = true;
            startAnimatingRefreshButton(refreshMenuItem);
            isRefreshing = true;
        } else {
            stopAnimatingRefreshButton(refreshMenuItem);
            isRefreshing = false;
        }

    }

    @Override
    protected Dialog onCreateDialog(int id) {
        loadingDialog = new ProgressDialog(this);
        if (id == ID_DIALOG_DELETING) {
            loadingDialog.setTitle(getResources().getText(
                    (isPage) ? R.string.delete_page : R.string.delete_post));
            loadingDialog.setMessage(getResources().getText(
                    (isPage) ? R.string.attempt_delete_page
                            : R.string.attempt_delete_post));
            loadingDialog.setCancelable(false);
            return loadingDialog;
        } else if (id == ID_DIALOG_SHARE) {
            loadingDialog.setTitle(isPage ? getString(R.string.share_url_page) : getString(R.string.share_url));
            loadingDialog.setMessage(getResources().getText(
                    R.string.attempting_fetch_url));
            loadingDialog.setCancelable(false);
            return loadingDialog;
        }

        return super.onCreateDialog(id);
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
                Toast.makeText(
                        PostsActivity.this,
                        getResources().getText(
                                (isPage) ? R.string.page_deleted
                                        : R.string.post_deleted),
                        Toast.LENGTH_SHORT).show();
                checkForLocalChanges(false);
            } else {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                        PostsActivity.this);
                dialogBuilder.setTitle(getResources().getText(
                        R.string.connection_error));
                dialogBuilder.setMessage(errorMsg);
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
            Object[] pageParams = { WordPress.currentBlog.getBlogId(),
                    WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword(), post.getPostid() };

            try {
                client.call((isPage) ? "wp.deletePage" : "blogger.deletePost",
                        (isPage) ? pageParams : postParams);

                result = true;
            } catch (final XMLRPCException e) {
                errorMsg = String.format(getResources().getString(R.string.error_delete_post), (isPage) ? getResources().getText(R.string.page) : getResources().getText(R.string.post));
                result = false;
            }
            return result;
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
                dialogBuilder.setMessage(errorMsg);
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
                if (isPage) {
                    Object[] vParams = { WordPress.currentBlog.getBlogId(),
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
                errorMsg = getResources().getText(R.string.error_generic).toString();
                return null;
            }

            if (versionResult != null) {
                try {
                    Map<?, ?> contentHash = (Map<?, ?>) versionResult;

                    if ((isPage && !"publish".equals(contentHash.get(
                            "page_status").toString()))
                            || (!isPage && !"publish".equals(contentHash.get(
                                    "post_status").toString()))) {
                        if (isPage) {
                            errorMsg = getResources().getText(
                                    R.string.page_not_published).toString();
                        } else {
                            errorMsg = getResources().getText(
                                    R.string.post_not_published).toString();
                        }
                        return null;
                    } else {
                        String postURL = contentHash.get("permaLink")
                                .toString();
                        String shortlink = getShortlinkTagHref(postURL);
                        if (shortlink == null) {
                            result = postURL;
                        } else {
                            result = shortlink;
                        }
                    }
                } catch (Exception e) {
                    errorMsg = getResources().getText(R.string.error_generic).toString();
                    return null;
                }
            }

            return result;
        }
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
        if (postList.getPostsTask != null) {
            postList.getPostsTask.cancel(true);
            //titleBar.stopRotatingRefreshIcon();
            isRefreshing = false;
        }
        if (action == POST_DELETE) {
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
                                attemptToSelectPost();
                                postList.loadPosts(false);
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
                                new PostsActivity.deletePostTask().execute(post);
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
            new PostsActivity.shareURLTask().execute(post);
        } else if (action == POST_CLEAR) {
            FragmentManager fm = getSupportFragmentManager();
            ViewPostFragment f = (ViewPostFragment) fm
                    .findFragmentById(R.id.postDetail);
            if (f != null) {
                f.clearContent();
            }
        }
    }

    @Override
    public void onDetailPostAction(int action, Post post) {

        onPostAction(action, post);

    }

    @Override
    public void onDialogConfirm() {
        postList.switcher.showNext();
        postList.numRecords += 30;
        postList.refreshPosts(true);
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
        postList.loadPosts(false);
        new ApiHelper.RefreshBlogContentTask(this, WordPress.currentBlog).execute(false);
    }
}
