package org.wordpress.android.ui.reader.actions;

import org.wordpress.android.models.ReaderBlogInfo;
import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderPost;

/**
 * classes in this package serve as a middleman between local data and server data - used by
 * reader activities/fragments/adapters that wish to perform actions on posts, blogs & topics,
 * or wish to get the latest data from the server.
 *
 * methods in this package which change state (like, follow, etc.) are generally optimistic
 * and work like this:
 *
 *  1. caller asks method to send a network request which changes state
 *  2. method changes state in local data and returns to caller *before* network request completes
 *  3. caller can access local state change without waiting for the network request
 *  4. if the network request fails, the method restores the previous state of the local data
 *  5. if caller passes a listener, it can be alerted to the actual success/failure of the request
 *
 *  note that all methods MUST be called from the UI thread in order to guarantee that listeners
 *  are alerted on the UI thread
 */
public class ReaderActions {

    private ReaderActions() {
        throw new AssertionError();
    }

    /*
     * result when a specific action is performed (liking a post, etc.)
     */
    public interface ActionListener {
        public void onActionResult(boolean succeeded);
    }

    /*
     * result when submitting a comment
     */
    public interface CommentActionListener {
        public void onActionResult(boolean succeeded, ReaderComment newComment);
    }

    /*
     * result when updating data (getting latest comments for a post, etc.)
     */
    public enum UpdateResult {CHANGED, UNCHANGED, FAILED}


    public interface UpdateResultListener {
        public void onUpdateResult(UpdateResult result);
    }

    /*
     * same as UpdateResultListener but includes count
     */
    public interface UpdateResultAndCountListener {
        public void onUpdateResult(UpdateResult result, int numNew);
    }

    /*
     * used by adapters to notify when data has been loaded
     */
    public interface DataLoadedListener {
        public void onDataLoaded(boolean isEmpty);
    }

    /*
     * used by adapters to notify when more data should be loaded
     */
    public static enum RequestDataAction {LOAD_NEWER, LOAD_OLDER}
    public interface DataRequestedListener {
        public void onRequestData(RequestDataAction action);
    }

    /*
     * used by post list & post list adapter when user asks to reblog a post
     */
    public interface RequestReblogListener {
        public void onRequestReblog(ReaderPost post);
    }

    /*
     * used by blog detail when requesting latest info about a blog
     */
    public interface UpdateBlogInfoListener {
        public void onResult(ReaderBlogInfo blogInfo);
    }

    /*
     * listener when updating posts and then backfilling them
     */
    public interface PostBackfillListener {
        public void onPostsBackfilled(int numNewPosts);
    }
}
