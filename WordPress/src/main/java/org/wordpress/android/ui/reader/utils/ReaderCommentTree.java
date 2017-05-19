package org.wordpress.android.ui.reader.utils;

import android.support.annotation.NonNull;

import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderCommentList;

/**
 *
 */

public class ReaderCommentTree {
    private final ReaderCommentList mComments;

    public ReaderCommentTree(@NonNull ReaderCommentList comments) {
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

        // add children
        int level = 0;
        while (walkCommentsAtLevel(level, result)) {
            level++;
        }

        return result;
    }

    private boolean walkCommentsAtLevel(int level, @NonNull ReaderCommentList comments) {
        boolean hasChanges = false;
        for (int index = 0; index < comments.size(); index++) {
            ReaderComment parent = comments.get(index);
            if (parent.level == level && hasChildren(parent.commentId)) {
                ReaderCommentList children = getChildren(parent.commentId);
                setLevel(children, level + 1);
                comments.addAll(index + 1, children);
                hasChanges = true;
            }
        }
        return hasChanges;
    }

    private boolean hasChildren(long commentId) {
        for (ReaderComment comment: mComments) {
            if (comment.parentId == commentId) {
                return true;
            }
        }
        return false;
    }

    private ReaderCommentList getChildren(long commentId) {
        ReaderCommentList children = new ReaderCommentList();
        for (ReaderComment comment: mComments) {
            if (comment.parentId == commentId) {
                children.add(comment);
            }
        }
        return children;
    }

    private void setLevel(@NonNull ReaderCommentList comments, int level) {
        for (ReaderComment comment: comments) {
            comment.level = level;
        }
    }
}
