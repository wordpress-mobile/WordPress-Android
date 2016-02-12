package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class CommentList extends ArrayList<Comment> {
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
     * returns true if any comments in this list do NOT have the passed status
     */
    public boolean hasAnyWithoutStatus(CommentStatus status) {
        for (Comment comment: this) {
            if (!comment.getStatusEnum().equals(status))
                return true;
        }
        return false;
    }

    /*
     * does passed list contain the same comments as this list?
     */
    public boolean isSameList(CommentList comments) {
        if (comments == null || comments.size() != this.size())
            return false;

        for (final Comment comment: comments) {
            int index = this.indexOfCommentId(comment.commentID);
            if (index == -1)
                return false;
            final Comment thisComment = this.get(index);
            if (!thisComment.getStatus().equals(comment.getStatus()))
                return false;
            if (!thisComment.getCommentText().equals(comment.getCommentText()))
                return false;
            if (!thisComment.getAuthorName().equals(comment.getAuthorName()))
                return false;
            if (!thisComment.getAuthorEmail().equals(comment.getAuthorEmail()))
                return false;
            if (!thisComment.getAuthorUrl().equals(comment.getAuthorUrl()))
                return false;

        }

        return true;
    }

    public static CommentList fromJSONV1_1(JSONObject object) throws JSONException {
        CommentList commentList = new CommentList();
        if (object == null) {
            return null;
        } else {
            JSONArray comments = object.getJSONArray("comments");
            for (int i=0; i < comments.length(); i++){
                JSONObject commentJson = comments.getJSONObject(i);
                commentList.add(Comment.fromJSON(commentJson));
            }
            return commentList;
        }
    }
}
