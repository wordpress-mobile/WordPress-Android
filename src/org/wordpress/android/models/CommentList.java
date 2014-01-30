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
     * use this to convert the results of WordPress.wpDB.loadComments() to a CommentList
     */
    /*public static CommentList fromMap(List<Map<String, Object>> commentMap) {
        CommentList comments = new CommentList();
        if (commentMap == null || commentMap.size()==0)
            return comments;

        String author, postID, commentContent, dateCreatedFormatted, status, authorEmail, authorURL, postTitle;
        int commentID;
        for (int i = 0; i < commentMap.size(); i++) {
            Map<String, Object> contentHash = commentMap.get(i);
            commentID = (Integer) contentHash.get("commentID");
            postID = contentHash.get("postID").toString();
            commentContent = contentHash.get("comment").toString();
            dateCreatedFormatted = contentHash.get("commentDateFormatted").toString();
            status = contentHash.get("status").toString();
            author = StringUtils.unescapeHTML(contentHash.get("author").toString());
            authorEmail = StringUtils.unescapeHTML(contentHash.get("email").toString());
            authorURL = StringUtils.unescapeHTML(contentHash.get("url").toString());
            postTitle = StringUtils.unescapeHTML(contentHash.get("postTitle").toString());

            comments.add(new Comment(postID,
                    commentID,
                    i,
                    author,
                    dateCreatedFormatted,
                    commentContent,
                    status,
                    postTitle,
                    authorURL,
                    authorEmail,
                    GravatarUtils.gravatarUrlFromEmail(authorEmail, 140)));
        }

        return comments;
    }*/

}
