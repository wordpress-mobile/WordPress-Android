package org.wordpress.android.ui.reader.actions;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.VolleyUtils;

public class ReaderTagActions {
    public enum TagAction {ADD, DELETE}

    private ReaderTagActions() {
        throw new AssertionError();
    }

    /**
     * perform the passed action on the passed tag - this is optimistic (returns before API call completes)
     **/
    public static boolean performTagAction(final ReaderTag tag,
                                           final TagAction action,
                                           final ReaderActions.ActionListener actionListener) {
        if (tag == null) {
            if (actionListener != null) {
                actionListener.onActionResult(false);
            }
            return false;
        }

        // don't allow actions on default tags
        if (tag.tagType == ReaderTagType.DEFAULT) {
            AppLog.w(T.READER, "cannot add or delete default tag");
            if (actionListener != null) {
                actionListener.onActionResult(false);
            }
            return false;
        }

        final String path;
        final String tagNameForApi = ReaderUtils.sanitizeWithDashes(tag.getTagName());

        switch (action) {
            case DELETE:
                // delete tag & all related posts
                ReaderTagTable.deleteTag(tag);
                ReaderPostTable.deletePostsWithTag(tag);
                path = "/read/tags/" + tagNameForApi + "/mine/delete";
                break;

            case ADD :
                String endpoint = "/read/tags/" + tagNameForApi + "/posts";
                ReaderTag newTopic = new ReaderTag(tag.getTagName(), endpoint, ReaderTagType.FOLLOWED);
                ReaderTagTable.addOrUpdateTag(newTopic);
                path = "/read/tags/" + tagNameForApi + "/mine/new";
                break;

            default :
                return false;
        }

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                AppLog.i(T.READER, "tag action " + action.name() + " succeeded");
                if (actionListener != null) {
                    actionListener.onActionResult(true);
                }
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                // if we're adding a topic and the error says the user is already following
                // this topic, or we're removing a topic and the error says the user isn't
                // following it, treat it as a success - this can happen if the user edits
                // topics in the web reader while this app is running
                String error = VolleyUtils.errStringFromVolleyError(volleyError);
                boolean isSuccess = (action== TagAction.ADD    && error.equals("already_subscribed"))
                                 || (action== TagAction.DELETE && error.equals("not_subscribed"));
                if (isSuccess) {
                    AppLog.w(T.READER, "tag action " + action.name() + " succeeded with error " + error);
                    if (actionListener != null) {
                        actionListener.onActionResult(true);
                    }
                    return;
                }

                AppLog.w(T.READER, "tag action " + action.name() + " failed");
                AppLog.e(T.READER, volleyError);

                // revert on failure
                switch (action) {
                    case DELETE:
                        // add back original tag
                        ReaderTagTable.addOrUpdateTag(tag);
                        break;
                    case ADD:
                        // remove new topic
                        ReaderTagTable.deleteTag(tag);
                        break;
                }

                if (actionListener != null) {
                    actionListener.onActionResult(false);
                }
            }
        };
        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);

        return true;
    }
}
