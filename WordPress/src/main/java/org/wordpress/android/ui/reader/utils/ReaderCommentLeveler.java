package org.wordpress.android.ui.reader.utils;

import androidx.annotation.NonNull;

import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderCommentList;

/*
 * utility class which accepts a list of comments and then creates a "level list" from it
 * which places child comments below their parents with indentation levels applied
 */

public class ReaderCommentLeveler {
    private final ReaderCommentList mComments;

    public ReaderCommentLeveler(@NonNull ReaderCommentList comments) {
        mComments = comments;
    }

    public ReaderCommentList createLevelList() {
        ReaderCommentList result = new ReaderCommentList();

        // reset all levels, and add root comments to result
        for (ReaderComment comment : mComments) {
            comment.level = 0;
            if (comment.parentId == 0) {
                result.add(comment);
            }
        }

        // add children at each level
        int level = 0;
        while (walkCommentsAtLevel(result, level)) {
            level++;
        }

        return result;
    }

    /*
     * walk comments in the passed list that have the passed level and add their children
     * beneath them
     */
    private boolean walkCommentsAtLevel(@NonNull ReaderCommentList comments, int level) {
        boolean hasChanges = false;
        for (int index = 0; index < comments.size(); index++) {
            ReaderComment parent = comments.get(index);
            if (parent.level == level && hasChildren(parent.commentId)) {
                // get children for this comment, set their level, then add them below the parent
                ReaderCommentList children = getChildren(parent.commentId);
                setLevel(children, level + 1);
                comments.addAll(index + 1, children);
                hasChanges = true;
                // skip past the children we just added
                index += children.size();
            }
        }
        return hasChanges;
    }

    private boolean hasChildren(long commentId) {
        for (ReaderComment comment : mComments) {
            if (comment.parentId == commentId) {
                return true;
            }
        }
        return false;
    }

    private ReaderCommentList getChildren(long commentId) {
        ReaderCommentList children = new ReaderCommentList();
        for (ReaderComment comment : mComments) {
            if (comment.parentId == commentId) {
                children.add(comment);
            }
        }
        return children;
    }

    private void setLevel(@NonNull ReaderCommentList comments, int level) {
        for (ReaderComment comment : comments) {
            comment.level = level;
        }
    }
}
