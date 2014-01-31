package org.wordpress.android.models;

import java.util.ArrayList;

/**
 * Created by nbradbury on 29-Jan-2014
 */
public class CommentList extends ArrayList<Comment> {

    private boolean commentIdExists(long commentId) {
        return (indexOfCommentId(commentId) > -1);
    }

    public int indexOfCommentId(long commentId) {
        for (int i=0; i < this.size(); i++) {
            if (commentId==this.get(i).commentID)
                return i;
        }
        return -1;
    }

    /*
     * replace comments in this list that match the passed list
     */
    public void replaceComments(final CommentList comments) {
        if (comments == null || comments.size() == 0)
            return;
        for (Comment comment: comments) {
            int index = indexOfCommentId(comment.commentID);
            if (index > -1)
                set(index, comment);
        }
    }

    /*
     * delete comments in this list that match the passed list
     */
    public void deleteComments(final CommentList comments) {
        if (comments == null || comments.size() == 0)
            return;
        for (Comment comment: comments) {
            int index = indexOfCommentId(comment.commentID);
            if (index > -1)
                remove(index);
        }
    }

    /*
     * returns true if any comments in this list have the passed status
     */
    public boolean hasAnyWithStatus(CommentStatus status) {
        for (Comment comment: this) {
            if (comment.getStatusEnum().equals(status))
                return true;
        }
        return false;
    }

    /*
     * does passed list contain the same comments as this list? doesn't do an EXACT match - only
     * checks whether commentIDs & statuses match
     */
    public boolean isSameList(CommentList comments) {
        if (comments == null || comments.size() != this.size())
            return false;

        for (Comment comment: comments) {
            int index = this.indexOfCommentId(comment.commentID);
            if (index == -1)
                return false;
            if (! this.get(index).getStatusEnum().equals(comment.getStatusEnum()))
                return false;
        }

        return true;
    }
}
