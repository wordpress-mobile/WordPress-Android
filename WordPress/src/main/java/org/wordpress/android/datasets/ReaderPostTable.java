package org.wordpress.android.datasets;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.util.SqlUtils;

/**
 * tbl_posts contains all reader posts
 * tbl_post_tags stores the association between posts and tags (posts can exist in more than one tag)
 *
 */
public class ReaderPostTable {
    private static final String COLUMN_NAMES =
            "post_id,"              // 1
          + "blog_id,"              // 2
          + "pseudo_id,"            // 3
          + "author_name,"          // 4
          + "author_id,"            // 5
          + "title,"                // 6
          + "text,"                 // 7
          + "excerpt,"              // 8
          + "url,"                  // 9
          + "blog_url,"             // 10
          + "blog_name,"            // 11
          + "featured_image,"       // 12
          + "featured_video,"       // 13
          + "post_avatar,"          // 14
          + "timestamp,"            // 15
          + "published,"            // 16
          + "num_replies,"          // 17
          + "num_likes,"            // 18
          + "is_liked,"             // 19
          + "is_followed,"          // 20
          + "is_comments_open,"     // 21
          + "is_reblogged,"         // 22
          + "is_external,"          // 23
          + "is_private,"           // 24
          + "is_videopress,"        // 25
          + "tag_list,"             // 26
          + "primary_tag,"          // 27
          + "secondary_tag";        // 28


    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_posts ("
                + "	post_id		        INTEGER,"       // post_id for WP blogs, feed_item_id for non-WP blogs
                + " blog_id             INTEGER,"       // blog_id for WP blogs, feed_id for non-WP blogs
                + " pseudo_id           TEXT NOT NULL,"
                + "	author_name	        TEXT,"
                + " author_id           INTEGER DEFAULT 0,"
                + "	title	            TEXT,"
                + "	text                TEXT,"
                + "	excerpt             TEXT,"
                + " url                 TEXT,"
                + " blog_url            TEXT,"
                + " blog_name           TEXT,"
                + " featured_image      TEXT,"
                + " featured_video      TEXT,"
                + " post_avatar         TEXT,"
                + " timestamp           INTEGER DEFAULT 0,"
                + " published           TEXT,"
                + " num_replies         INTEGER DEFAULT 0,"
                + " num_likes           INTEGER DEFAULT 0,"
                + " is_liked            INTEGER DEFAULT 0,"
                + " is_followed         INTEGER DEFAULT 0,"
                + " is_comments_open    INTEGER DEFAULT 0,"
                + " is_reblogged        INTEGER DEFAULT 0,"
                + " is_external         INTEGER DEFAULT 0,"
                + " is_private          INTEGER DEFAULT 0,"
                + " is_videopress       INTEGER DEFAULT 0,"
                + " tag_list            TEXT,"
                + " primary_tag         TEXT,"
                + " secondary_tag       TEXT,"
                + " PRIMARY KEY (post_id, blog_id)"
                + ")");

        db.execSQL("CREATE TABLE tbl_post_tags ("
                + "   post_id     INTEGER NOT NULL,"
                + "   blog_id     INTEGER NOT NULL,"
                + "   pseudo_id   TEXT NOT NULL,"
                + "   tag_name    TEXT NOT NULL COLLATE NOCASE,"
                + "   tag_type    INTEGER DEFAULT 0,"
                + "   PRIMARY KEY (post_id, blog_id, tag_name, tag_type)"
                + ")");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_posts");
        db.execSQL("DROP TABLE IF EXISTS tbl_post_tags");
    }

    protected static void reset(SQLiteDatabase db) {
        dropTables(db);
        createTables(db);
    }

    /*
     * purge table of unattached posts - no need to wrap this in a transaction since this
     * is only called from ReaderDatabase.purge() which already creates a transaction
     */
    protected static int purge(SQLiteDatabase db) {
        // delete posts in tbl_post_tags attached to tags that no longer exist
        int numDeleted = db.delete("tbl_post_tags", "tag_name NOT IN (SELECT DISTINCT tag_name FROM tbl_tags)", null);

        // delete posts in tbl_posts that no longer exist in tbl_post_tags
        numDeleted += db.delete("tbl_posts", "pseudo_id NOT IN (SELECT DISTINCT pseudo_id FROM tbl_post_tags)", null);

        return numDeleted;
    }

    public static boolean isEmpty() {
        return (getNumPosts() == 0);
    }

    private static int getNumPosts() {
        long count = SqlUtils.getRowCount(ReaderDatabase.getReadableDb(), "tbl_posts");
        return (int)count;
    }

    public static int getNumPostsInBlog(long blogId) {
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                "SELECT count(*) FROM tbl_posts WHERE blog_id=?",
                new String[]{Long.toString(blogId)});
    }

    public static int getNumPostsWithTag(ReaderTag tag) {
        if (tag == null) {
            return 0;
        }
        String[] args = {tag.getTagName(), Integer.toString(tag.tagType.toInt())};
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                    "SELECT count(*) FROM tbl_post_tags WHERE tag_name=? AND tag_type=?",
                    args);
    }

    public static boolean hasPostsWithTag(ReaderTag tag) {
        return (getNumPostsWithTag(tag) > 0);
    }

    public static void addOrUpdatePost(ReaderPost post) {
        if (post == null) {
            return;
        }
        ReaderPostList posts = new ReaderPostList();
        posts.add(post);
        addOrUpdatePosts(null, posts);
    }

    public static ReaderPost getPost(long blogId, long postId) {
        String[] args = new String[] {Long.toString(blogId), Long.toString(postId)};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_posts WHERE blog_id=? AND post_id=? LIMIT 1", args);
        try {
            if (!c.moveToFirst()) {
                return null;
            }
            return getPostFromCursor(c, null);
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static void deletePost(long blogId, long postId) {
        String[] args = {Long.toString(blogId), Long.toString(postId)};
        ReaderDatabase.getWritableDb().delete("tbl_posts", "blog_id=? AND post_id=?", args);
    }

    public static String getPostTitle(long blogId, long postId) {
        String[] args = {Long.toString(blogId), Long.toString(postId)};
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(),
                "SELECT title FROM tbl_posts WHERE blog_id=? AND post_id=?",
                args);
    }

    public static boolean postExists(long blogId, long postId) {
        String[] args = {Long.toString(blogId), Long.toString(postId)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(),
                "SELECT 1 FROM tbl_posts WHERE blog_id=? AND post_id=?",
                args);
    }

    /*
     * returns a count of which posts in the passed list don't already exist in the db for the passed tag
     */
    public static int getNumNewPostsWithTag(ReaderTag tag, ReaderPostList posts) {
        if (posts == null || posts.size() == 0 || tag == null) {
            return 0;
        }

        // if there aren't any posts in this tag, then all passed posts are new
        if (getNumPostsWithTag(tag) == 0) {
            return posts.size();
        }

        StringBuilder sb = new StringBuilder(
                "SELECT COUNT(*) FROM tbl_post_tags WHERE tag_name=? AND tag_type=? AND pseudo_id IN (");
        boolean isFirst = true;
        for (ReaderPost post: posts) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(",");
            }
            sb.append("'").append(post.getPseudoId()).append("'");
        }
        sb.append(")");

        String[] args = {tag.getTagName(), Integer.toString(tag.tagType.toInt())};
        int numExisting = SqlUtils.intForQuery(ReaderDatabase.getReadableDb(), sb.toString(), args);
        return posts.size() - numExisting;
    }

    /*
     * returns the #comments known to exist for this post (ie: #comments the server says this post has), which
     * may differ from ReaderCommentTable.getNumCommentsForPost (which returns # local comments for this post)
     */
    public static int getNumCommentsForPost(ReaderPost post) {
        if (post == null) {
            return 0;
        }
        String[] args = new String[] {Long.toString(post.blogId), Long.toString(post.postId)};
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                "SELECT num_replies FROM tbl_posts WHERE blog_id=? AND post_id=?",
                args);
    }

    /*
     * returns the #likes known to exist for this post (ie: #likes the server says this post has), which
     * may differ from ReaderPostTable.getNumLikesForPost (which returns # local likes for this post)
     */
    public static int getNumLikesForPost(long blogId, long postId) {
        String[] args = {Long.toString(blogId), Long.toString(postId)};
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                "SELECT num_likes FROM tbl_posts WHERE blog_id=? AND post_id=?",
                args);
    }

    public static boolean isPostLikedByCurrentUser(long blogId, long postId) {
        String[] args = new String[] {Long.toString(blogId), Long.toString(postId)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(),
                "SELECT is_liked FROM tbl_posts WHERE blog_id=? AND post_id=?",
                args);
    }

    public static boolean isPostFollowed(ReaderPost post) {
        if (post == null) {
            return false;
        }
        String[] args = new String[] {Long.toString(post.blogId), Long.toString(post.postId)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(),
                "SELECT is_followed FROM tbl_posts WHERE blog_id=? AND post_id=?",
                args);
    }

    /*
     * updates the follow status of all posts in the passed list, returns true if any changed
     */
    public static boolean checkFollowStatusOnPosts(ReaderPostList posts) {
        if (posts == null || posts.size() == 0) {
            return false;
        }

        boolean isChanged = false;
        for (ReaderPost post: posts) {
            boolean isFollowed = isPostFollowed(post);
            if (isFollowed != post.isFollowedByCurrentUser) {
                post.isFollowedByCurrentUser = isFollowed;
                isChanged = true;
            }
        }
        return isChanged;
    }

    public static int deletePostsWithTag(final ReaderTag tag) {
        if (tag == null) {
            return 0;
        }

        // first delete posts from tbl_post_tags, and if any were deleted next delete posts in tbl_posts that no longer exist in tbl_post_tags
        String[] args = {tag.getTagName(), Integer.toString(tag.tagType.toInt())};
        int numDeleted = ReaderDatabase.getWritableDb().delete("tbl_post_tags",
                "tag_name=? AND tag_type=?",
                args);

        if (numDeleted > 0)
            ReaderDatabase.getWritableDb().delete("tbl_posts",
                    "post_id NOT IN (SELECT DISTINCT post_id FROM tbl_post_tags)",
                    null);

        return numDeleted;
    }

    public static int deletePostsInBlog(long blogId) {
        String[] args = {Long.toString(blogId)};
        return ReaderDatabase.getWritableDb().delete("tbl_posts", "blog_id = ?", args);
    }

    /*
     * returns the iso8601 published date of the oldest post with the passed tag
     */
    public static String getOldestPubDateWithTag(final ReaderTag tag) {
        if (tag == null) {
            return "";
        }

        String sql = "SELECT tbl_posts.published FROM tbl_posts, tbl_post_tags"
                   + " WHERE tbl_posts.post_id = tbl_post_tags.post_id AND tbl_posts.blog_id = tbl_post_tags.blog_id"
                   + " AND tbl_post_tags.tag_name=? AND tbl_post_tags.tag_type=?"
                   + " ORDER BY published LIMIT 1";
        String[] args = {tag.getTagName(), Integer.toString(tag.tagType.toInt())};
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), sql, args);
    }

    /*
     * returns the iso8601 published date of the oldest post in the passed blog
     */
    public static String getOldestPubDateInBlog(long blogId) {
        String sql = "SELECT published FROM tbl_posts"
                  + " WHERE blog_id = ?"
                  + " ORDER BY published LIMIT 1";
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), sql, new String[]{Long.toString(blogId)});
    }

    /*
     * sets the following status for all posts in the passed blog
     */
    public static void setFollowStatusForPostsInBlog(long blogId, String blogUrl, boolean isFollowed) {
        if (blogId == 0 && TextUtils.isEmpty(blogUrl)) {
            return;
        }

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        try {
            // change is_followed in tbl_posts for this blog - use blogId if we have it,
            // otherwise use url
            if (blogId != 0) {
                String sql = "UPDATE tbl_posts SET is_followed=" + SqlUtils.boolToSql(isFollowed)
                          + " WHERE blog_id=?";
                db.execSQL(sql, new String[]{Long.toString(blogId)});
            } else {
                String sql = "UPDATE tbl_posts SET is_followed=" + SqlUtils.boolToSql(isFollowed)
                          + " WHERE blog_url=?";
                db.execSQL(sql, new String[]{blogUrl});
            }

            // if blog is no longer followed, remove its posts tagged with "Blogs I Follow" in
            // tbl_post_tags - note that this requires the blogId
            if (!isFollowed && blogId != 0) {
                db.delete("tbl_post_tags", "blog_id=? AND tag_name=?",
                        new String[]{Long.toString(blogId), ReaderTag.TAG_NAME_FOLLOWING});
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
    
    public static void addOrUpdatePosts(final ReaderTag tag, ReaderPostList posts) {
        if (posts == null || posts.size() == 0) {
            return;
        }

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        SQLiteStatement stmtPosts = db.compileStatement(
                "INSERT OR REPLACE INTO tbl_posts ("
                + COLUMN_NAMES
                + ") VALUES (?1,?2,?3,?4,?5,?6,?7,?8,?9,?10,?11,?12,?13,?14,?15,?16,?17,?18,?19,?20,?21,?22,?23,?24,?25,?26,?27,?28)");
        SQLiteStatement stmtTags = db.compileStatement(
                "INSERT OR REPLACE INTO tbl_post_tags (post_id, blog_id, pseudo_id, tag_name, tag_type) VALUES (?1,?2,?3,?4,?5)");

        db.beginTransaction();
        try {
            // first insert into tbl_posts
            for (ReaderPost post: posts) {
                stmtPosts.bindLong  (1,  post.postId);
                stmtPosts.bindLong  (2,  post.blogId);
                stmtPosts.bindString(3,  post.getPseudoId());
                stmtPosts.bindString(4,  post.getAuthorName());
                stmtPosts.bindLong  (5,  post.authorId);
                stmtPosts.bindString(6,  post.getTitle());
                stmtPosts.bindString(7,  post.getText());
                stmtPosts.bindString(8,  post.getExcerpt());
                stmtPosts.bindString(9,  post.getUrl());
                stmtPosts.bindString(10, post.getBlogUrl());
                stmtPosts.bindString(11, post.getBlogName());
                stmtPosts.bindString(12, post.getFeaturedImage());
                stmtPosts.bindString(13, post.getFeaturedVideo());
                stmtPosts.bindString(14, post.getPostAvatar());
                stmtPosts.bindLong  (15, post.timestamp);
                stmtPosts.bindString(16, post.getPublished());
                stmtPosts.bindLong  (17, post.numReplies);
                stmtPosts.bindLong  (18, post.numLikes);
                stmtPosts.bindLong  (19, SqlUtils.boolToSql(post.isLikedByCurrentUser));
                stmtPosts.bindLong  (20, SqlUtils.boolToSql(post.isFollowedByCurrentUser));
                stmtPosts.bindLong  (21, SqlUtils.boolToSql(post.isCommentsOpen));
                stmtPosts.bindLong  (22, SqlUtils.boolToSql(post.isRebloggedByCurrentUser));
                stmtPosts.bindLong  (23, SqlUtils.boolToSql(post.isExternal));
                stmtPosts.bindLong  (24, SqlUtils.boolToSql(post.isPrivate));
                stmtPosts.bindLong  (25, SqlUtils.boolToSql(post.isVideoPress));
                stmtPosts.bindString(26, post.getTags());
                stmtPosts.bindString(27, post.getPrimaryTag());
                stmtPosts.bindString(28, post.getSecondaryTag());
                stmtPosts.execute();
                stmtPosts.clearBindings();
            }

            // now add to tbl_post_tags if a tag was passed
            if (tag != null) {
                String tagName = tag.getTagName();
                int tagType = tag.tagType.toInt();
                for (ReaderPost post: posts) {
                    stmtTags.bindLong  (1, post.postId);
                    stmtTags.bindLong  (2, post.blogId);
                    stmtTags.bindString(3, post.getPseudoId());
                    stmtTags.bindString(4, tagName);
                    stmtTags.bindLong  (5, tagType);
                    stmtTags.execute();
                    stmtTags.clearBindings();
                }
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmtPosts);
            SqlUtils.closeStatement(stmtTags);
        }
    }

    public static ReaderPostList getPostsWithTag(ReaderTag tag, int maxPosts) {
        if (tag == null) {
            return new ReaderPostList();
        }

        String sql = "SELECT tbl_posts.* FROM tbl_posts, tbl_post_tags"
                   + " WHERE tbl_posts.post_id = tbl_post_tags.post_id"
                   + " AND tbl_posts.blog_id = tbl_post_tags.blog_id"
                   + " AND tbl_post_tags.tag_name=?"
                   + " AND tbl_post_tags.tag_type=?";

        if (tag.tagType == ReaderTagType.DEFAULT) {
            // skip posts that are no longer liked if this is "Posts I Like", skip posts that are no
            // longer followed if this is "Blogs I Follow"
            if (tag.getTagName().equals(ReaderTag.TAG_NAME_LIKED)) {
                sql += " AND tbl_posts.is_liked != 0";
            } else if (tag.getTagName().equals(ReaderTag.TAG_NAME_FOLLOWING)) {
                sql += " AND tbl_posts.is_followed != 0";
            }
        }

        sql += " ORDER BY tbl_posts.timestamp DESC";

        if (maxPosts > 0) {
            sql += " LIMIT " + Integer.toString(maxPosts);
        }

        String[] args = {tag.getTagName(), Integer.toString(tag.tagType.toInt())};
        Cursor cursor = ReaderDatabase.getReadableDb().rawQuery(sql, args);
        try {
            ReaderPostList posts = new ReaderPostList();
            if (cursor != null && cursor.moveToFirst()) {
                // create column indexes object that can be used for every post in this cursor so
                // getPostFromCursor() doesn't need to call "getColumnIndex()" for every row
                final PostColumnIndexes cols = new PostColumnIndexes(cursor);
                do {
                    posts.add(getPostFromCursor(cursor, cols));
                } while (cursor.moveToNext());
            }
            return posts;
        } finally {
            SqlUtils.closeCursor(cursor);
        }
    }

    public static ReaderPostList getPostsInBlog(long blogId, int maxPosts) {
        String sql = "SELECT * FROM tbl_posts WHERE blog_id = ? ORDER BY tbl_posts.timestamp DESC";

        if (maxPosts > 0) {
            sql += " LIMIT " + Integer.toString(maxPosts);
        }

        Cursor cursor = ReaderDatabase.getReadableDb().rawQuery(sql, new String[]{Long.toString(blogId)});
        try {
            ReaderPostList posts = new ReaderPostList();
            if (cursor == null || !cursor.moveToFirst()) {
                return posts;
            }

            final PostColumnIndexes cols = new PostColumnIndexes(cursor);
            do {
                posts.add(getPostFromCursor(cursor, cols));
            } while (cursor.moveToNext());

            return posts;
        } finally {
            SqlUtils.closeCursor(cursor);
        }
    }


    public static void setPostReblogged(ReaderPost post, boolean isReblogged) {
        if (post == null) {
            return;
        }

        String sql = "UPDATE tbl_posts SET is_reblogged=" + SqlUtils.boolToSql(isReblogged)
                  + " WHERE blog_id=? AND post_id=?";
        String[] args = {Long.toString(post.blogId), Long.toString(post.postId)};
        ReaderDatabase.getWritableDb().execSQL(sql, args);
    }

    /*
     * stores column indexes for a specific cursor - used when loading multiple posts from
     * a cursor to avoid having to call getColumnIndex() for every row
     */
    private static class PostColumnIndexes {
        private final int idx_post_id;
        private final int idx_blog_id;
        private final int idx_pseudo_id;

        private final int idx_author_name;
        private final int idx_author_id;
        private final int idx_blog_name;
        private final int idx_blog_url;
        private final int idx_excerpt;
        private final int idx_featured_image;
        private final int idx_featured_video;

        private final int idx_title;
        private final int idx_text;
        private final int idx_url;
        private final int idx_post_avatar;

        private final int idx_timestamp;
        private final int idx_published;

        private final int idx_num_replies;
        private final int idx_num_likes;

        private final int idx_is_liked;
        private final int idx_is_followed;
        private final int idx_is_comments_open;
        private final int idx_is_reblogged;
        private final int idx_is_external;
        private final int idx_is_private;
        private final int idx_is_videopress;

        private final int idx_tag_list;
        private final int idx_primary_tag;
        private final int idx_secondary_tag;

        private PostColumnIndexes(Cursor c) {
            if (c == null)
                throw new IllegalArgumentException("PostColumnIndexes > null cursor");

            idx_post_id = c.getColumnIndex("post_id");
            idx_blog_id = c.getColumnIndex("blog_id");
            idx_pseudo_id = c.getColumnIndex("pseudo_id");

            idx_author_name = c.getColumnIndex("author_name");
            idx_author_id = c.getColumnIndex("author_id");
            idx_blog_name = c.getColumnIndex("blog_name");
            idx_blog_url = c.getColumnIndex("blog_url");
            idx_excerpt = c.getColumnIndex("excerpt");
            idx_featured_image = c.getColumnIndex("featured_image");
            idx_featured_video = c.getColumnIndex("featured_video");

            idx_title = c.getColumnIndex("title");
            idx_text = c.getColumnIndex("text");
            idx_url = c.getColumnIndex("url");
            idx_post_avatar = c.getColumnIndex("post_avatar");

            idx_timestamp = c.getColumnIndex("timestamp");
            idx_published = c.getColumnIndex("published");

            idx_num_replies = c.getColumnIndex("num_replies");
            idx_num_likes = c.getColumnIndex("num_likes");

            idx_is_liked = c.getColumnIndex("is_liked");
            idx_is_followed = c.getColumnIndex("is_followed");
            idx_is_comments_open = c.getColumnIndex("is_comments_open");
            idx_is_reblogged = c.getColumnIndex("is_reblogged");
            idx_is_external = c.getColumnIndex("is_external");
            idx_is_private = c.getColumnIndex("is_private");
            idx_is_videopress = c.getColumnIndex("is_videopress");

            idx_tag_list = c.getColumnIndex("tag_list");
            idx_primary_tag = c.getColumnIndex("primary_tag");
            idx_secondary_tag = c.getColumnIndex("secondary_tag");
        }
    }

    private static ReaderPost getPostFromCursor(Cursor c, PostColumnIndexes cols) {
        if (c == null) {
            throw new IllegalArgumentException("getPostFromCursor > null cursor");
        }

        ReaderPost post = new ReaderPost();

        // if column index object wasn't passed, create it now
        if (cols == null) {
            cols = new PostColumnIndexes(c);
        }

        post.postId = c.getLong(cols.idx_post_id);
        post.blogId = c.getLong(cols.idx_blog_id);
        post.authorId = c.getLong(cols.idx_author_id);
        post.setPseudoId(c.getString(cols.idx_pseudo_id));

        post.setAuthorName(c.getString(cols.idx_author_name));
        post.setBlogName(c.getString(cols.idx_blog_name));
        post.setBlogUrl(c.getString(cols.idx_blog_url));
        post.setExcerpt(c.getString(cols.idx_excerpt));
        post.setFeaturedImage(c.getString(cols.idx_featured_image));
        post.setFeaturedVideo(c.getString(cols.idx_featured_video));

        post.setTitle(c.getString(cols.idx_title));
        post.setText(c.getString(cols.idx_text));
        post.setUrl(c.getString(cols.idx_url));
        post.setPostAvatar(c.getString(cols.idx_post_avatar));

        post.timestamp = c.getLong(cols.idx_timestamp);
        post.setPublished(c.getString(cols.idx_published));

        post.numReplies = c.getInt(cols.idx_num_replies);
        post.numLikes = c.getInt(cols.idx_num_likes);

        post.isLikedByCurrentUser = SqlUtils.sqlToBool(c.getInt(cols.idx_is_liked));
        post.isFollowedByCurrentUser = SqlUtils.sqlToBool(c.getInt(cols.idx_is_followed));
        post.isCommentsOpen = SqlUtils.sqlToBool(c.getInt(cols.idx_is_comments_open));
        post.isRebloggedByCurrentUser = SqlUtils.sqlToBool(c.getInt(cols.idx_is_reblogged));
        post.isExternal = SqlUtils.sqlToBool(c.getInt(cols.idx_is_external));
        post.isPrivate = SqlUtils.sqlToBool(c.getInt(cols.idx_is_private));
        post.isVideoPress = SqlUtils.sqlToBool(c.getInt(cols.idx_is_videopress));

        post.setTags(c.getString(cols.idx_tag_list));
        post.setPrimaryTag(c.getString(cols.idx_primary_tag));
        post.setSecondaryTag(c.getString(cols.idx_secondary_tag));

        return post;
    }
}
