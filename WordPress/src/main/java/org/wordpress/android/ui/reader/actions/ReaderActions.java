package org.wordpress.android.ui.reader.actions;

import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.models.ReaderComment;

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
     * listener when a specific action is performed (liking a post, etc.)
     */
    public interface ActionListener {
        void onActionResult(boolean succeeded);
    }

    /*
     * helper routine for telling an action listener the call succeeded or failed w/o having to null check
     */
    public static void callActionListener(ActionListener actionListener, boolean succeeded) {
        if (actionListener != null) {
            actionListener.onActionResult(succeeded);
        }
    }

    /*
     * listener when the failure status code is required
     */
    public interface OnRequestListener {
        void onSuccess();
        void onFailure(int statusCode);
    }

    /*
     * listener when submitting a comment
     */
    public interface CommentActionListener {
        void onActionResult(boolean succeeded, ReaderComment newComment);
    }

    /*
     * result when updating data (getting latest posts or comments for a post, etc.)
     */
    public enum UpdateResult {
        HAS_NEW,    // new posts/comments/etc. have been retrieved
        CHANGED,    // no new posts/comments, but existing ones have changed
        UNCHANGED,  // no new or changed posts/comments
        FAILED;     // request failed
        public boolean isNewOrChanged() {
            return (this == HAS_NEW || this == CHANGED);
        }
    }
    public interface UpdateResultListener {
        void onUpdateResult(UpdateResult result);
    }

    /*
     * used by adapters to notify when more data should be loaded
     */
    public interface DataRequestedListener {
        void onRequestData();
    }

    /*
     * used by blog preview when requesting latest info about a blog
     */
    public interface UpdateBlogInfoListener {
        void onResult(ReaderBlog blogInfo);
    }

}
