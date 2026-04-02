package org.wordpress.android.models;

import java.util.ArrayList;

public class ReaderCommentList extends ArrayList<ReaderComment> {
    public int indexOfCommentId(long commentId) {
        for (int i = 0; i < this.size(); i++) {
            if (commentId == this.get(i).commentId) {
                return i;
            }
        }
        return -1;
    }

    /*
     * does passed list contain the same comments as this list?
     */
    public boolean isSameList(ReaderCommentList comments) {
        if (comments == null || comments.size() != this.size()) {
            return false;
        }

        for (ReaderComment comment : comments) {
            if (indexOf(comment) == -1) {
                return false;
            }
        }

        return true;
    }

    public boolean replaceComment(long commentId, ReaderComment newComment) {
        if (newComment == null) {
            return false;
        }

        int index = indexOfCommentId(commentId);
        if (index == -1) {
            return false;
        }

        // make sure the new comment has the same level as the old one
        newComment.level = this.get(index).level;

        this.set(index, newComment);
        return true;
    }
}
