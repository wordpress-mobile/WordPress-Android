package org.wordpress.android.models;

import java.net.URI;

public class Comment {
    public String postID = "";
    public int commentID;
    public int position;
    public String name = "";
    public String emailURL = "";
    public String status = "";
    public String comment = "";
    public String postTitle = "";
    public String authorURL = "";
    public String authorEmail = "";
    public String dateCreatedFormatted = "";
    public URI profileImageUrl = null;

    public Comment(String postID, int commentID, int position, String name,
            String dateCreatedFormatted, String comment, String status,
            String postTitle, String authorURL, String authorEmail,
            URI profileImageUrl) {
        this.postID = postID;
        this.commentID = commentID;
        this.position = position;
        this.name = name;
        this.emailURL = authorEmail;
        this.status = status;
        this.comment = comment;
        this.postTitle = postTitle;
        this.authorURL = authorURL;
        this.authorEmail = authorEmail;
        this.profileImageUrl = profileImageUrl;
        this.dateCreatedFormatted = dateCreatedFormatted;
    }

    public CommentStatus getStatusEnum() {
        return CommentStatus.fromString(status);
    }

    public static enum CommentStatus {
        UNKNOWN,
        PENDING,
        APPROVED,
        SPAM;

        public String toString() {
            switch (this) {
                case PENDING:
                    return "hold";
                case APPROVED:
                    return "approve";
                case SPAM :
                    return "spam";
                default :
                    return "";
            }
        }

        public static CommentStatus fromString(String value) {
            if (value==null)
                return CommentStatus.UNKNOWN;
            if (value.equals("approve"))
                return CommentStatus.APPROVED;
            if (value.equals("hold"))
                return CommentStatus.PENDING;
            if (value.equals("spam"))
                return SPAM;
            return CommentStatus.UNKNOWN;
        }
    }
}
