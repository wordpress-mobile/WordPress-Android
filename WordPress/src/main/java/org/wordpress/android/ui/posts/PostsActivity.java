package org.wordpress.android.ui.posts;

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
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.posts.PostsListFragment.OnPostActionListener;
import org.wordpress.android.ui.posts.PostsListFragment.OnPostSelectedListener;
import org.wordpress.android.ui.posts.ViewPostFragment.OnDetailPostActionListener;
import org.wordpress.android.util.AlertUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPMeShortlinks;
import org.wordpress.android.widgets.WPAlertDialogFragment;
import org.wordpress.passcodelock.AppLockManager;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFactory;

import java.io.IOException;

public class PostsActivity extends ActionBarActivity
        implements OnPostSelectedListener, PostsListFragment.OnSinglePostLoadedListener, OnPostActionListener,
                   OnDetailPostActionListener, WPAlertDialogFragment.OnDialogConfirmListener {
    public static final String EXTRA_VIEW_PAGES = "viewPages";
    public static final String EXTRA_ERROR_MSG = "errorMessage";
    public static final String EXTRA_ERROR_INFO_TITLE = "errorInfoTitle";
    public static final String EXTRA_ERROR_INFO_LINK = "errorInfoLink";

    public static final int POST_DELETE = 0;
    public static final int POST_SHARE = 1;
    public static final int POST_EDIT = 2;
    private static final int POST_CLEAR = 3;
    public static final int POST_VIEW = 5;
    private static final int ID_DIALOG_DELETING = 1, ID_DIALOG_SHARE = 2;
    private ProgressDialog mLoadingDialog;
    private boolean mIsPage = false;
    private String mErrorMsg = "";
    private PostsListFragment mPostList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This should be removed when #2734 is fixed
        if (WordPress.getCurrentBlog() == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }
        ProfilingUtils.split("PostsActivity.onCreate");
        ProfilingUtils.dump();

        setContentView(R.layout.posts);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FragmentManager fm = getFragmentManager();
        mPostList = (PostsListFragment) fm.findFragmentById(R.id.postList);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mIsPage = extras.getBoolean(EXTRA_VIEW_PAGES);
            showErrorDialogIfNeeded(extras);
        }

        if (mIsPage) {
            getSupportActionBar().setTitle(getString(R.string.pages));
        } else {
            getSupportActionBar().setTitle(getString(R.string.posts));
        }

        WordPress.currentPost = null;

        if (savedInstanceState != null) {
            popPostDetail();
        }

        attemptToSelectPost();
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
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

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            popPostDetail();
        } else {
            super.onBackPressed();
        }
    }

    private void popPostDetail() {
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

    public void newPost() {
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
        startActivityForResult(i, RequestCodes.EDIT_POST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            if (requestCode == RequestCodes.EDIT_POST && resultCode == RESULT_OK) {
                if (data.getBooleanExtra(EditPostActivity.EXTRA_SHOULD_REFRESH, false)) {
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
        if (isFinishing()) {
            return;
        }
        FragmentManager fm = getFragmentManager();
        ViewPostFragment viewPostFragment = (ViewPostFragment) fm.findFragmentById(R.id.postDetail);

        if (post != null) {
            if (post.isUploading()){
                ToastUtils.showToast(this, R.string.toast_err_post_uploading, ToastUtils.Duration.SHORT);
                return;
            }
            WordPress.currentPost = post;
            if (viewPostFragment == null || !viewPostFragment.isInLayout()) {
                FragmentTransaction ft = fm.beginTransaction();
                ft.hide(mPostList);
                viewPostFragment = new ViewPostFragment();
                ft.add(R.id.postDetailFragmentContainer, viewPostFragment);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.addToBackStack(null);
                ft.commitAllowingStateLoss();
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
            if (mLoadingDialog == null || isFinishing()) {
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

    private class refreshCommentsTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Object[] commentParams = { WordPress.currentBlog.getRemoteBlogId(),
                    WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword() };

            try {
                ApiHelper.refreshComments(WordPress.currentBlog, commentParams);
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
                String deletePostMessage = getResources().getText(
                        (post.isPage()) ? R.string.delete_sure_page
                                : R.string.delete_sure_post).toString();
                if (!post.getTitle().isEmpty()) {
                    String postTitleEnclosedByQuotes = "'" + post.getTitle() + "'";
                    deletePostMessage += " " + postTitleEnclosedByQuotes;
                }

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                        PostsActivity.this);
                dialogBuilder.setTitle(getResources().getText(
                        (post.isPage()) ? R.string.delete_page
                                : R.string.delete_post));
                dialogBuilder.setMessage(deletePostMessage + "?");
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
                AlertUtils.showAlert(this, R.string.error,
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

    public void setRefreshing(boolean refreshing) {
        mPostList.setRefreshing(refreshing);
    }


    public boolean isDualPane() {
        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.postDetail);
        return fragment != null && fragment.isVisible();
    }
}
