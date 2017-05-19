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

    public ReaderCommentList getLevelList() {
        ReaderCommentList result = new ReaderCommentList();

        // reset all levels, and add root comments to result
        for (ReaderComment comment : mComments) {
            comment.level = 0;
            if (comment.parentId == 0) {
                result.add(comment);
            }
        }

        boolean done = false;
        while (!done) {
            done = true;
            for (ReaderComment parent: result) {
                if (parent.level == 0 && hasChildren(parent.commentId)) {
                    ReaderCommentList children = getChildren(parent.commentId);
                    setLevel(children, parent.level + 1);
                    int index = result.indexOfCommentId(parent.commentId);
                    result.addAll(index + 1, children);
                    done = false;
                }
            }
        }

        return result;
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
