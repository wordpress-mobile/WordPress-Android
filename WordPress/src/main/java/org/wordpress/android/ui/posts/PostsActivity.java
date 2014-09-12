package org.wordpress.android.ui.posts;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.MenuDrawerItem;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.notifications.NotificationsActivity;
import org.wordpress.android.ui.posts.PostsListFragment.OnPostActionListener;
import org.wordpress.android.ui.posts.PostsListFragment.OnPostSelectedListener;
import org.wordpress.android.ui.posts.ViewPostFragment.OnDetailPostActionListener;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AlertUtil;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.WPMeShortlinks;
import org.wordpress.android.widgets.WPAlertDialogFragment;
import org.wordpress.passcodelock.AppLockManager;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFactory;

import java.io.IOException;

public class PostsActivity extends WPActionBarActivity
        implements OnPostSelectedListener, PostsListFragment.OnSinglePostLoadedListener, OnPostActionListener,
                   OnDetailPostActionListener, WPAlertDialogFragment.OnDialogConfirmListener {
    public static final String EXTRA_VIEW_PAGES = "viewPages";
    public static final String EXTRA_ERROR_MSG = "errorMessage";
    public static final String EXTRA_ERROR_INFO_TITLE = "errorInfoTitle";
    public static final String EXTRA_ERROR_INFO_LINK = "errorInfoLink";

    public static final int POST_DELETE = 0, POST_SHARE = 1, POST_EDIT = 2, POST_CLEAR = 3, POST_VIEW = 5;
    public static final int ACTIVITY_EDIT_POST = 0;
    private static final int ID_DIALOG_DELETING = 1, ID_DIALOG_SHARE = 2;
    public ProgressDialog mLoadingDialog;
    public boolean mIsPage = false;
    public String mErrorMsg = "";
    private PostsListFragment mPostList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ProfilingUtils.split("PostsActivity.onCreate");
        ProfilingUtils.dump();
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
        if (WordPress.shouldRestoreSelectedActivity && WordPress.getCurrentBlog() != null &&
                !(this instanceof PagesActivity)) {
            WordPress.shouldRestoreSelectedActivity = false;
            ActivityId lastActivity = ActivityId.getActivityIdFromName(AppPrefs.getLastActivityStr());
            if (lastActivity.autoRestoreMapper() != ActivityId.UNKNOWN) {
                for (MenuDrawerItem item : mMenuItems) {
                    // if we have a matching item id, and it's not selected and it's visible, call it
                    if (item.hasItemId() && item.getItemId() == lastActivity.autoRestoreMapper() && !item.isSelected()
                            && item.isVisible()) {
                        mFirstLaunch = true;
                        item.selectItem();
                        finish();
                        return;
                    }
                }
            }
        }

        createMenuDrawer(R.layout.posts);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
        }

        FragmentManager fm = getFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);
        mPostList = (PostsListFragment) fm.findFragmentById(R.id.postList);

        if (extras != null) {
            mIsPage = extras.getBoolean(EXTRA_VIEW_PAGES);
            showErrorDialogIfNeeded(extras);
        }

        if (mIsPage) {
            setTitle(getString(R.string.pages));
        } else {
            setTitle(getString(R.string.posts));
        }

        WordPress.currentPost = null;

        if (savedInstanceState != null) {
            popPostDetail();
        }

        attemptToSelectPost();
    }

    private void showPostUploadErrorAlert(String errorMessage, String infoTitle,
                                          final String infoURL) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(PostsActivity.this);
        dialogBuilder.setTitle(getResources().getText(R.string.error));
        dialogBuilder.setMessage(errorMessage);
        dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Just close the window.
                    }
                }
        );
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
            return;
        }
        String errorMessage = extras.getString(EXTRA_ERROR_MSG);
        if (!TextUtils.isEmpty(errorMessage)) {
            String errorInfoTitle = extras.getString(EXTRA_ERROR_INFO_TITLE);
            String errorInfoLink = extras.getString(EXTRA_ERROR_INFO_LINK);
            showPostUploadErrorAlert(errorMessage, errorInfoTitle, errorInfoLink);
        }
    }

    private void startNotificationsActivity(Bundle extras) {
        // Manually set last selection to notifications
        AppPrefs.setLastActivityStr(ActivityId.NOTIFICATIONS.name());

        Intent i = new Intent(this, NotificationsActivity.class);
        i.putExtras(extras);
        startActivity(i);
        finish();
    }

    private FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            if (getFragmentManager().getBackStackEntryCount() == 0)
                mMenuDrawer.setDrawerIndicatorEnabled(true);
        }
    };

    public boolean isRefreshing() {
        return mPostList.isRefreshing();
    }

    public void requestPosts() {
        if (WordPress.getCurrentBlog() == null) {
            return;
        }
        // If user has local changes, don't refresh
        if (!WordPress.wpDB.findLocalChanges(WordPress.getCurrentBlog().getLocalTableBlogId(), mIsPage)) {
            popPostDetail();
            mPostList.requestPosts(false);
            mPostList.setRefreshing(true);
        }
    }

    protected void popPostDetail() {
        if (isFinishing()) {
            return;
        }

        FragmentManager fm = getFragmentManager();
        ViewPostFragment f = (ViewPostFragment) fm.findFragmentById(R.id.postDetail);
        if (f == null) {
            try {
                fm.popBackStack();
            } catch (RuntimeException e) {
                AppLog.e(T.POSTS, e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // posts can't be shown if there aren't any visible blogs, so redirect to the reader and
        // exit the post list in this situation
        if (WordPress.isSignedIn(PostsActivity.this)) {
            if (showReaderIfNoBlog()) {
                finish();
            }
        }

        if (WordPress.postsShouldRefresh) {
            requestPosts();
            mPostList.setRefreshing(true);
            WordPress.postsShouldRefresh = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.posts, menu);
        if (mIsPage) {
            menu.findItem(R.id.menu_new_post).setTitle(R.string.new_page);
        }
        return true;
    }

    public void newPost() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_CREATED_POST);
        if (WordPress.getCurrentBlog() == null) {
            if (!isFinishing())
                Toast.makeText(this, R.string.blog_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        // Create a new post object
        Post newPost = new Post(WordPress.getCurrentBlog().getLocalTableBlogId(), mIsPage);
        WordPress.wpDB.savePost(newPost);
        Intent i = new Intent(this, EditPostActivity.class);
        i.putExtra(EditPostActivity.EXTRA_POSTID, newPost.getLocalTablePostId());
        i.putExtra(EditPostActivity.EXTRA_IS_PAGE, mIsPage);
        i.putExtra(EditPostActivity.EXTRA_IS_NEW_POST, true);
        startActivityForResult(i, ACTIVITY_EDIT_POST);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_new_post) {
            newPost();
            return true;
        } else if (itemId == android.R.id.home) {
            FragmentManager fm = getFragmentManager();
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
                    mPostList.getPostListAdapter().loadPosts();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void attemptToSelectPost() {
        FragmentManager fm = getFragmentManager();
        ViewPostFragment f = (ViewPostFragment) fm.findFragmentById(R.id.postDetail);
        if (f != null && f.isInLayout()) {
            mPostList.setShouldSelectFirstPost(true);
        }
    }

    @Override
    public void onPostSelected(Post post) {
        if (isFinishing() || isActivityDestroyed()) {
            return;
        }
        FragmentManager fm = getFragmentManager();
        ViewPostFragment viewPostFragment = (ViewPostFragment) fm.findFragmentById(R.id.postDetail);

        if (post != null) {
            WordPress.currentPost = post;
            if (viewPostFragment == null || !viewPostFragment.isInLayout()) {
                FragmentTransaction ft = fm.beginTransaction();
                ft.hide(mPostList);
                viewPostFragment = new ViewPostFragment();
                ft.add(R.id.postDetailFragmentContainer, viewPostFragment);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.addToBackStack(null);
                ft.commitAllowingStateLoss();
                mMenuDrawer.setDrawerIndicatorEnabled(false);
            } else {
                viewPostFragment.loadPost(post);
            }
        }
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
            if (result) {
                WordPress.wpDB.deletePost(post);
            }
            if (mLoadingDialog == null || isActivityDestroyed() || isFinishing()) {
                return;
            }
            dismissDialog(ID_DIALOG_DELETING);
            attemptToSelectPost();
            if (result) {
                Toast.makeText(PostsActivity.this, getResources().getText((mIsPage) ?
                        R.string.page_deleted : R.string.post_deleted),
                        Toast.LENGTH_SHORT).show();
                requestPosts();
                mPostList.requestPosts(false);
                mPostList.setRefreshing(true);
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
            Blog blog = WordPress.currentBlog;
            XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                    blog.getHttppassword());

            Object[] postParams = { "", post.getRemotePostId(),
                    WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword() };
            Object[] pageParams = { WordPress.currentBlog.getRemoteBlogId(),
                    WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword(), post.getRemotePostId() };

            try {
                client.call((mIsPage) ? "wp.deletePage" : "blogger.deletePost", (mIsPage) ? pageParams : postParams);
                result = true;
            } catch (final XMLRPCException e) {
                mErrorMsg = prepareErrorMessage(e);
            } catch (IOException e) {
                mErrorMsg = prepareErrorMessage(e);
            } catch (XmlPullParserException e) {
                mErrorMsg = prepareErrorMessage(e);
            }
            return result;
        }

        private String prepareErrorMessage(Exception e) {
            AppLog.e(AppLog.T.POSTS, "Error while deleting post or page", e);
            return String.format(getResources().getString(R.string.error_delete_post),
                    (mIsPage) ? getResources().getText(R.string.page)
                              : getResources().getText(R.string.post));
        }
    }

    public class refreshCommentsTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Object[] commentParams = { WordPress.currentBlog.getRemoteBlogId(),
                    WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword() };

            try {
                ApiHelper.refreshComments(PostsActivity.this, WordPress.currentBlog, commentParams);
            } catch (final Exception e) {
                mErrorMsg = getResources().getText(R.string.error_generic).toString();
            }
            return null;
        }
    }

    protected void refreshComments() {
        new refreshCommentsTask().execute();
    }

    @Override
    public void onPostAction(int action, final Post post) {
        // No post? No service.
        if (post == null) {
            Toast.makeText(PostsActivity.this, R.string.post_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        if (action == POST_DELETE) {
            if (post.isLocalDraft()) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                        PostsActivity.this);
                dialogBuilder.setTitle(getResources().getText(
                        R.string.delete_draft));

                String deleteDraftMessage = getResources().getText(R.string.delete_sure).toString();
                if (!post.getTitle().isEmpty()) {
                    String postTitleEnclosedByQuotes = "'" + post.getTitle() + "'";
                    deleteDraftMessage += " " + postTitleEnclosedByQuotes;
                }

                dialogBuilder.setMessage(deleteDraftMessage + "?");
                dialogBuilder.setPositiveButton(
                        getResources().getText(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                WordPress.wpDB.deletePost(post);
                                popPostDetail();
                                attemptToSelectPost();
                                mPostList.getPostListAdapter().loadPosts();
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
            // Only share published posts
            if (post.getStatusEnum() != PostStatus.PUBLISHED && post.getStatusEnum() != PostStatus.SCHEDULED) {
                AlertUtil.showAlert(this, R.string.error,
                        post.isPage() ? R.string.page_not_published : R.string.post_not_published);
                return;
            }

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, post.getTitle());
            String shortlink = WPMeShortlinks.getPostShortlink(WordPress.getCurrentBlog(), post);
            share.putExtra(Intent.EXTRA_TEXT, shortlink != null ? shortlink : post.getPermaLink());
            startActivity(Intent.createChooser(share, getResources()
                    .getText(R.string.share_url)));
            AppLockManager.getInstance().setExtendedTimeout();
        } else if (action == POST_CLEAR) {
            FragmentManager fm = getFragmentManager();
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
        mPostList.requestPosts(true);
        mPostList.setRefreshing(true);
    }

    @Override
    public void onSinglePostLoaded() {
        popPostDetail();
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
        mPostList.clear();
        mPostList.getPostListAdapter().loadPosts();
        mPostList.onBlogChanged();
    }

    public void setRefreshing(boolean refreshing) {
        mPostList.setRefreshing(refreshing);
    }


    public boolean isDualPane() {
        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.postDetail);
        return fragment != null && fragment.isVisible();
    }
}
