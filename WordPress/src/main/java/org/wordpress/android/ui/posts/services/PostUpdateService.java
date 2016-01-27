package org.wordpress.android.ui.posts.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.AppLog;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.Method;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFactory;
import org.xmlrpc.android.XMLRPCFault;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * service which retrieves posts for the post list
 */

public class PostUpdateService extends Service {

    private static final String ARG_BLOG_ID = "blog_id";
    private static final String ARG_LOAD_MORE = "load_more";
    private static final String ARG_IS_PAGE = "is_page";

    private static final int NUM_POSTS_TO_REQUEST = 20;

    /*
     * fetch posts/pages in a specific blog
     */
    public static void startServiceForBlog(Context context, int blogId, boolean isPage, boolean loadMore) {
        Intent intent = new Intent(context, PostUpdateService.class);
        intent.putExtra(ARG_BLOG_ID, blogId);
        intent.putExtra(ARG_IS_PAGE, isPage);
        intent.putExtra(ARG_LOAD_MORE, loadMore);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.POSTS, "PostUpdateService > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.POSTS, "PostUpdateService > destroyed");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        new Thread() {
            @Override
            public void run() {
                int blogId = intent.getIntExtra(ARG_BLOG_ID, 0);
                boolean isPage = intent.getBooleanExtra(ARG_IS_PAGE, false);
                boolean loadMore = intent.getBooleanExtra(ARG_LOAD_MORE, false);
                fetchPostsInBlog(blogId, isPage, loadMore);
            }
        }.start();

        return START_NOT_STICKY;
    }

    private void fetchPostsInBlog(int blogId, boolean isPage, boolean loadMore) {
        Blog blog = WordPress.getBlog(blogId);
        if (blog == null) {
            return;
        }

        XMLRPCClientInterface client = XMLRPCFactory.instantiate(
                blog.getUri(),
                blog.getHttpuser(),
                blog.getHttppassword());

        int numPostsToRequest;
        if (loadMore) {
            int numExisting = WordPress.wpDB.getUploadedCountInBlog(blogId, isPage);
            numPostsToRequest = numExisting + NUM_POSTS_TO_REQUEST;
        } else {
            numPostsToRequest = NUM_POSTS_TO_REQUEST;
        }

        Object[] result;
        Object[] xmlrpcParams = {
                blog.getRemoteBlogId(),
                blog.getUsername(),
                blog.getPassword(),
                numPostsToRequest};

        PostEvents.RequestPosts event = new PostEvents.RequestPosts(blogId, isPage);
        try {
            boolean canLoadMore;

            result = (Object[]) client.call(isPage ? Method.GET_PAGES : "metaWeblog.getRecentPosts", xmlrpcParams);
            if (result != null && result.length > 0) {
                canLoadMore = true;

                // If we're loading more posts, only save the posts at the end of the array.
                // NOTE: Switching to wp.getPosts wouldn't require janky solutions like this
                // since it allows for an offset parameter.
                int startPosition = 0;
                if (loadMore && result.length > NUM_POSTS_TO_REQUEST) {
                    startPosition = result.length - NUM_POSTS_TO_REQUEST;
                }

                List<Map<?, ?>> postsList = new ArrayList<>();
                for (int ctr = startPosition; ctr < result.length; ctr++) {
                    Map<?, ?> postMap = (Map<?, ?>) result[ctr];
                    postsList.add(postMap);
                }

                if (!loadMore) {
                    WordPress.wpDB.deleteUploadedPosts(blogId, isPage);
                }
                WordPress.wpDB.savePosts(postsList, blogId, isPage, false);
            } else {
                canLoadMore = false;
            }

            event.setCanLoadMore(canLoadMore);

        } catch (XMLRPCException | IOException | XmlPullParserException e){
            AppLog.e(AppLog.T.POSTS, e);
            ApiHelper.ErrorType errorType;
            if (e instanceof XMLRPCFault) {
                if (((XMLRPCFault)(e)).getFaultCode() == 401) {
                    errorType = ApiHelper.ErrorType.UNAUTHORIZED;
                } else {
                    errorType = ApiHelper.ErrorType.NETWORK_XMLRPC;
                }
            } else {
                errorType = ApiHelper.ErrorType.INVALID_RESULT;
            }

            event.setErrorType(errorType);
        }

        EventBus.getDefault().post(event);
    }
}
