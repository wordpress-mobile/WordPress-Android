package org.wordpress.android.ui.reader_native.actions;

import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderPost;

/**
 * Created by nbradbury on 7/17/13.
 * classes in this package serve as a middleman between local data and server data - used by
 * reader activities/fragments/adapters that wish to perform actions on posts & topics,
 * or wish to get the latest data from the server
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
}
