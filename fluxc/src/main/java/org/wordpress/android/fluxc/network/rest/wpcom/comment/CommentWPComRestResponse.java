package org.wordpress.android.fluxc.network.rest.wpcom.comment;

import java.util.List;

public class CommentWPComRestResponse {
    public class CommentsWPComRestResponse {
        public List<CommentWPComRestResponse> comments;
    }

    public class Post {
        public long ID;
        public String title;
        public String type;
        public String link;
    }

    public class Author {
        public long ID;
        public String email; // can be boolean "false" if not set
        public String name;
        public String URL;
        public String avatar_URL;
    }

    public long ID;
    public CommentParent parent;
    public Post post;
    public Author author;
    public String date;
    public String status;
    public String content;
    public boolean i_like;
    public String URL;
}
