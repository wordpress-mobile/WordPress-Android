package org.wordpress.android.fluxc.network.rest.wpcom.comment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

@SuppressWarnings("NotNullFieldNotInitialized")
public class CommentWPComRestResponse {
    public static class CommentsWPComRestResponse {
        @Nullable public List<CommentWPComRestResponse> comments;
    }

    public static class Post {
        public long ID;
        @NonNull public String title;
        @NonNull public String type;
        @NonNull public String link;
    }

    public static class Author {
        public long ID;
        @NonNull public String email; // can be boolean "false" if not set
        @NonNull public String name;
        @NonNull public String URL;
        @NonNull public String avatar_URL;
    }

    public long ID;
    @Nullable public CommentParent parent;
    @Nullable public Post post;
    @Nullable public Author author;
    @NonNull public String date;
    @NonNull public String status;
    @NonNull public String content;
    public boolean i_like;
    @NonNull public String URL;
}
