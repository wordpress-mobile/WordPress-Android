package org.wordpress.android.ui.comments;

import androidx.annotation.NonNull;

import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.util.AppLog;

import java.util.List;

public class CommentLeveler {
    private final List<CommentModel> mComments;

    public CommentLeveler(@NonNull List<CommentModel> comments) {
        mComments = comments;
    }

    public CommentList createLevelList() {
        CommentList result = new CommentList();

        // reset all levels, and add root comments to result
        for (CommentModel comment : mComments) {
            comment.level = 0;
            if (comment.getParentId() == 0) {
                result.add(comment);
            }
        }

        // add children at each level
        int level = 0;
        while (walkCommentsAtLevel(result, level)) {
            level++;
        }

        // check for orphans (child comments whose parents weren't found above) and give them
        // a non-zero level to distinguish them from top level comments
        for (CommentModel comment : result) {
            if (comment.level == 0 && comment.getParentId() != 0) {
                comment.level = 1;
                result.add(comment);
                AppLog.d(AppLog.T.READER, "Orphan comment encountered");
            }
        }

        return result;
    }

    /*
     * walk comments in the passed list that have the passed level and add their children
     * beneath them
     */
    private boolean walkCommentsAtLevel(@NonNull CommentList comments, int level) {
        boolean hasChanges = false;
        for (int index = 0; index < comments.size(); index++) {
            CommentModel parent = comments.get(index);
            if (parent.level == level && hasChildren(parent.getRemoteCommentId())) {
                // get children for this comment, set their level, then add them below the parent
                CommentList children = getChildren(parent.getRemoteCommentId());
                setLevel(children, level + 1);
                comments.addAll(index + 1, children);
                hasChanges = true;
                // skip past the children we just added
                index += children.size();
            }
        }
        return hasChanges;
    }

    public boolean hasChildren(long commentId) {
        for (CommentModel comment : mComments) {
            if (comment.getParentId() == commentId) {
                return true;
            }
        }
        return false;
    }

    public CommentList getChildren(long commentId) {
        CommentList children = new CommentList();
        for (CommentModel comment : mComments) {
            if (comment.getParentId() == commentId) {
                children.add(comment);
            }
        }
        return children;
    }

    private void setLevel(@NonNull CommentList comments, int level) {
        for (CommentModel comment : comments) {
            comment.level = level;
        }
    }
}
