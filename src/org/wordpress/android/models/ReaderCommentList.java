package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by nbradbury on 7/8/13.
 */
public class ReaderCommentList extends ArrayList<ReaderComment> {

    public static ReaderCommentList fromJson(JSONObject json, long blogId) {
        if (json==null)
            throw new IllegalArgumentException("null json comment list");

        ReaderCommentList comments = new ReaderCommentList();

        JSONArray jsonComments = json.optJSONArray("comments");
        if (jsonComments!=null) {
            for (int i=0; i < jsonComments.length(); i++)
                comments.add(ReaderComment.fromJson(jsonComments.optJSONObject(i), blogId));
        }

        return comments;
    }

    private boolean commentIdExists(long commentId) {
        return (indexOfCommentId(commentId) > -1);
    }

    public int indexOfCommentId(long commentId) {
        for (int i=0; i < this.size(); i++) {
            if (commentId==this.get(i).commentId)
                return i;
        }
        return -1;
    }

    /*
     * does passed list contain the same comments as this list?
     */
    public boolean isSameList(ReaderCommentList comments) {
        if (comments==null || comments.size()!=this.size())
            return false;

        for (ReaderComment comment: comments) {
            if (!commentIdExists(comment.commentId))
                return false;
        }

        return true;
    }

    public boolean replaceComment(long commentId, ReaderComment comment) {
        if (comment==null)
            return false;
        int index = indexOfCommentId(commentId);
        if (index == -1)
            return false;
        this.set(index, comment);
        return true;
    }

    /*
     * builds a new list from the passed one with child comments placed under their parents and indent levels applied
     */
    public static ReaderCommentList getLevelList(ReaderCommentList thisList) {
        if (thisList==null)
            return new ReaderCommentList();

        // first check if there are any child comments - if not, just return the passed list
        boolean hasChildComments = false;
        for (ReaderComment comment: thisList) {
            if (comment.parentId!=0) {
                hasChildComments = true;
                break;
            }
        }
        if (!hasChildComments)
            return thisList;

        ReaderCommentList result = new ReaderCommentList();

        // reset all levels, and add root comments to result
        for (ReaderComment comment: thisList) {
            comment.level = 0;
            if (comment.parentId==0)
                result.add(comment);
        }

        // add child comments under their parents
        boolean done;
        do {
            done = true;
            for (ReaderComment comment: thisList) {
                // only look at comments that have a parentId but no level assigned yet
                if (comment.parentId!=0 && comment.level==0) {
                    int parentIndex = result.indexOfCommentId(comment.parentId);
                    if (parentIndex > -1) {
                        comment.level = result.get(parentIndex).level + 1;

                        // insert this comment after the last comment of this level that has this parent
                        int commentIndex=parentIndex+1;
                        while (commentIndex < result.size()) {
                            if (result.get(commentIndex).level!=comment.level || result.get(commentIndex).parentId!=comment.parentId)
                                break;
                            commentIndex++;
                        }
                        result.add(commentIndex, comment);


                        done = false;
                    }
                }
            }
        } while (!done);

        // handle orphans (child comments whose parents weren't found above)
        for (ReaderComment comment: thisList) {
            if (comment.level==0 && comment.parentId!=0) {
                comment.level = 1; // give it a non-zero level so it's indented by ReaderCommentAdapter
                result.add(comment);
            }
        }

        return result;
    }
}
