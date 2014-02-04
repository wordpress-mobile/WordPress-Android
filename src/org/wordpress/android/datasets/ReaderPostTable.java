package org.wordpress.android.datasets;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.util.SqlUtils;

/**
 * Created by nbradbury on 6/27/13.
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
          + "title,"                // 5
          + "text,"                 // 6
          + "excerpt,"              // 7
          + "url,"                  // 8
          + "blog_url,"             // 9
          + "blog_name,"            // 10
          + "featured_image,"       // 11
          + "featured_video,"       // 12
          + "post_avatar,"          // 13
          + "timestamp,"            // 14
          + "published,"            // 15
          + "num_replies,"          // 16
          + "num_likes,"            // 17
          + "is_liked,"             // 18
          + "is_followed,"          // 19
          + "is_comments_open,"     // 20
          + "is_reblogged,"         // 21
          + "is_external,"          // 22
          + "is_private,"           // 23
          + "is_videopress,"        // 24
          + "tag_list";             // 25


    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_posts ("
                + "	post_id		        INTEGER,"       // post_id for WP blogs, feed_item_id for non-WP blogs
                + " blog_id             INTEGER,"       // blog_id for WP blogs, feed_id for non-WP blogs
                + " pseudo_id           TEXT NOT NULL,"
                + "	author_name	        TEXT,"
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
                + " PRIMARY KEY (post_id, blog_id)"
                + ")");

        db.execSQL("CREATE TABLE tbl_post_tags ("
                + "   post_id     INTEGER NOT NULL,"
                + "   blog_id     INTEGER NOT NULL,"
                + "   pseudo_id   TEXT NOT NULL,"
                + "   tag_name  TEXT NOT NULL COLLATE NOCASE,"
                + "   PRIMARY KEY (post_id, blog_id, tag_name)"
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
        return (getNumPosts()==0);
    }

    public static int getNumPosts() {
        long count = SqlUtils.getRowCount(ReaderDatabase.getReadableDb(), "tbl_posts");
        return (int)count;
    }

    public static int getNumPostsWithTag(String tagName) {
        if (TextUtils.isEmpty(tagName))
            return 0;
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(), "SELECT count(*) FROM tbl_post_tags WHERE tag_name=?", new String[]{tagName});
    }

    public static boolean hasPostsWithTag(String tagName) {
        if (TextUtils.isEmpty(tagName))
            return false;
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), "SELECT 1 FROM tbl_post_tags WHERE tag_name=? LIMIT 1", new String[]{tagName});
    }

    public static void addOrUpdatePost(ReaderPost post) {
        if (post==null)
            return;
        ReaderPostList posts = new ReaderPostList();
        posts.add(post);
        addOrUpdatePosts(null, posts);
    }

    public static ReaderPost getPost(long blogId, long postId) {
        String[] args = new String[] {Long.toString(blogId), Long.toString(postId)};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_posts WHERE blog_id=? AND post_id=? LIMIT 1", args);
        try {
            if (!c.moveToFirst())
                return null;
            return getPostFromCursor(c);
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
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), "SELECT title FROM tbl_posts WHERE blog_id=? AND post_id=?", args);
    }

    public static boolean postExists(long blogId, long postId) {
        String[] args = {Long.toString(blogId), Long.toString(postId)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), "SELECT 1 FROM tbl_posts WHERE blog_id=? AND post_id=?", args);
    }

    /*
     * returns a count of which posts in the passed list don't already exist in the db for the passed tag
     */
    public static int getNumNewPostsWithTag(String tagName, ReaderPostList posts) {
        if (posts==null || posts.size()==0)
            return 0;
        if (TextUtils.isEmpty(tagName))
            return 0;

        // if there aren't any posts in this tag, then all passed posts are new
        if (getNumPostsWithTag(tagName)==0)
            return posts.size();

        // build sql that tells us which posts *do* exist in the database
        // TODO: may be able to simplify by using pseudo_id here
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT COUNT(*) FROM tbl_post_tags")
          .append(" WHERE tag_name=?")
          .append(" AND (CAST(post_id AS TEXT) || '-' || CAST(blog_id AS TEXT))") // concatenated string, post_id-blog_id
          .append(" IN (");

        boolean isFirst = true;
        for (ReaderPost post: posts) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(",");
            }
            sb.append("'").append(post.postId).append("-").append(post.blogId).append("'");
        }
        sb.append(")");

        int numExisting = SqlUtils.intForQuery(ReaderDatabase.getReadableDb(), sb.toString(), new String[]{tagName});
        return posts.size() - numExisting;
    }

    /*
     * returns the #comments known to exist for this post (ie: #comments the server says this post has), which
     * may differ from ReaderCommentTable.getNumCommentsForPost (which returns # local comments for this post)
     */
    public static int getNumCommentsForPost(ReaderPost post) {
        if (post==null)
            return 0;
        String[] args = new String[] {Long.toString(post.blogId), Long.toString(post.postId)};
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(), "SELECT num_replies FROM tbl_posts WHERE blog_id=? AND post_id=?", args);
    }

    /*
     * returns the #likes known to exist for this post (ie: #likes the server says this post has), which
     * may differ from ReaderPostTable.getNumLikesForPost (which returns # local likes for this post)
     */
    public static int getNumLikesForPost(ReaderPost post) {
        if (post==null)
            return 0;
        String[] args = {Long.toString(post.blogId), Long.toString(post.postId)};
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(), "SELECT num_likes FROM tbl_posts WHERE blog_id=? AND post_id=?", args);
    }

    public static boolean isPostLikedByCurrentUser(ReaderPost post) {
        if (post==null)
            return false;
        String[] args = new String[] {Long.toString(post.blogId), Long.toString(post.postId)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), "SELECT is_liked FROM tbl_posts WHERE blog_id=? AND post_id=?", args);
    }

    public static boolean isPostFollowed(ReaderPost post) {
        if (post==null)
            return false;
        String[] args = new String[] {Long.toString(post.blogId), Long.toString(post.postId)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), "SELECT is_followed FROM tbl_posts WHERE blog_id=? AND post_id=?", args);
    }

    public static int deletePostsWithTag(String tagName) {
        if (TextUtils.isEmpty(tagName))
            return 0;

        // first delete posts from tbl_post_tags, and if any were deleted next delete posts in tbl_posts that no longer exist in tbl_post_tags
        int numDeleted = ReaderDatabase.getWritableDb().delete("tbl_post_tags", "tag_name=?", new String[]{tagName});
        if (numDeleted > 0)
            ReaderDatabase.getWritableDb().delete("tbl_posts", "post_id NOT IN (SELECT DISTINCT post_id FROM tbl_post_tags)", null);

        return numDeleted;
    }

    /*
     * returns the iso8601 published date of the oldest post
     */
    public static String getOldestPubDateWithTag(final String tagName) {
        if (TextUtils.isEmpty(tagName))
            return "";

        String sql = "SELECT tbl_posts.published FROM tbl_posts, tbl_post_tags"
                   + " WHERE tbl_posts.post_id = tbl_post_tags.post_id AND tbl_posts.blog_id = tbl_post_tags.blog_id"
                   + " AND tbl_post_tags.tag_name=? ORDER BY published LIMIT 1";
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), sql, new String[]{tagName});
    }

    public static void setBlogPostsFollowStatus(long blogId, boolean isFollowed) {
        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        try {
            // change is_followed in tbl_posts for this blog
            String sql = "UPDATE tbl_posts SET is_followed=" + SqlUtils.boolToSql(isFollowed)
                       + " WHERE blog_id=?";
            db.execSQL(sql, new String[]{Long.toString(blogId)});

            // if blog is no longer followed, remove its posts tagged with "Blogs I Follow" in tbl_post_tags
            if (!isFollowed)
                db.delete("tbl_post_tags", "blog_id=? AND tag_name=?",
                        new String[]{Long.toString(blogId), ReaderTag.TAG_NAME_FOLLOWING});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
    
    public static void addOrUpdatePosts(final String tagName, ReaderPostList posts) {
        if (posts==null || posts.size()==0)
            return;

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();

        SQLiteStatement stmtPosts = db.compileStatement("INSERT OR REPLACE INTO tbl_posts ("
                                                        + COLUMN_NAMES
                                                        + ") VALUES (?1,?2,?3,?4,?5,?6,?7,?8,?9,?10,?11,?12,?13,?14,?15,?16,?17,?18,?19,?20,?21,?22,?23,?24,?25)");

        SQLiteStatement stmtTags = db.compileStatement("INSERT OR REPLACE INTO tbl_post_tags (post_id, blog_id, pseudo_id, tag_name) VALUES (?1,?2,?3,?4)");

        try {
            // first insert into tbl_posts
            for (ReaderPost post: posts) {
                stmtPosts.bindLong  (1,  post.postId);
                stmtPosts.bindLong  (2,  post.blogId);
                stmtPosts.bindString(3, post.getPseudoId());
                stmtPosts.bindString(4,  post.getAuthorName());
                stmtPosts.bindString(5,  post.getTitle());
                stmtPosts.bindString(6,  post.getText());
                stmtPosts.bindString(7,  post.getExcerpt());
                stmtPosts.bindString(8,  post.getUrl());
                stmtPosts.bindString(9,  post.getBlogUrl());
                stmtPosts.bindString(10, post.getBlogName());
                stmtPosts.bindString(11, post.getFeaturedImage());
                stmtPosts.bindString(12, post.getFeaturedVideo());
                stmtPosts.bindString(13, post.getPostAvatar());
                stmtPosts.bindLong(14, post.timestamp);
                stmtPosts.bindString(15, post.getPublished());
                stmtPosts.bindLong(16, post.numReplies);
                stmtPosts.bindLong  (17, post.numLikes);
                stmtPosts.bindLong  (18, SqlUtils.boolToSql(post.isLikedByCurrentUser));
                stmtPosts.bindLong  (19, SqlUtils.boolToSql(post.isFollowedByCurrentUser));
                stmtPosts.bindLong  (20, SqlUtils.boolToSql(post.isCommentsOpen));
                stmtPosts.bindLong  (21, SqlUtils.boolToSql(post.isRebloggedByCurrentUser));
                stmtPosts.bindLong  (22, SqlUtils.boolToSql(post.isExternal));
                stmtPosts.bindLong  (23, SqlUtils.boolToSql(post.isPrivate));
                stmtPosts.bindLong  (24, SqlUtils.boolToSql(post.isVideoPress));
                stmtPosts.bindString(25, post.getTags());
                stmtPosts.execute();
                stmtPosts.clearBindings();
            }

            // now add to tbl_post_tags - note that tagName will be null when updating a single
            // post, in which case we skip it here
            if (!TextUtils.isEmpty(tagName)) {
                for (ReaderPost post: posts) {
                    stmtTags.bindLong  (1, post.postId);
                    stmtTags.bindLong  (2, post.blogId);
                    stmtTags.bindString(3, post.getPseudoId());
                    stmtTags.bindString(4, tagName);
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

    public static ReaderPostList getPostsWithTag(String tagName, int maxPosts) {
        if (TextUtils.isEmpty(tagName))
            return new ReaderPostList();

        String sql = "SELECT tbl_posts.* FROM tbl_posts, tbl_post_tags"
                   + " WHERE tbl_posts.post_id = tbl_post_tags.post_id"
                   + " AND tbl_posts.blog_id = tbl_post_tags.blog_id"
                   + " AND tbl_post_tags.tag_name=?";

        if (tagName.equals(ReaderTag.TAG_NAME_LIKED)) {
            // skip posts that are no longer liked if this is "Posts I Like"
            sql += " AND tbl_posts.is_liked != 0";
        } else if (tagName.equals(ReaderTag.TAG_NAME_FOLLOWING)) {
            // skip posts that are no longer followed if this is "Blogs I Follow"
            sql += " AND tbl_posts.is_followed != 0";
        }

        sql += " ORDER BY tbl_posts.timestamp DESC";

        if (maxPosts > 0)
            sql += " LIMIT " + Integer.toString(maxPosts);

        Cursor cursor = ReaderDatabase.getReadableDb().rawQuery(sql, new String[]{tagName});
        try {
            ReaderPostList posts = new ReaderPostList();
            if (cursor==null || !cursor.moveToFirst())
                return posts;

            do {
                posts.add(getPostFromCursor(cursor));
            } while (cursor.moveToNext());

            return posts;
        } finally {
            SqlUtils.closeCursor(cursor);
        }
    }
    
    public static void setPostReblogged(ReaderPost post, boolean isReblogged) {
        if (post==null)
            return;

        String sql = "UPDATE tbl_posts SET is_reblogged=" + SqlUtils.boolToSql(isReblogged)
                  + " WHERE blog_id=? AND post_id=?";
        String[] args = {Long.toString(post.blogId), Long.toString(post.postId)};
        ReaderDatabase.getWritableDb().execSQL(sql, args);
    }

    private static ReaderPost getPostFromCursor(Cursor c) {
        if (c==null)
            throw new IllegalArgumentException("null post cursor");

        ReaderPost post = new ReaderPost();

        post.postId = c.getLong(c.getColumnIndex("post_id"));
        post.blogId = c.getLong(c.getColumnIndex("blog_id"));
        post.setPseudoId(c.getString(c.getColumnIndex("pseudo_id")));

        post.setAuthorName(c.getString(c.getColumnIndex("author_name")));
        post.setBlogName(c.getString(c.getColumnIndex("blog_name")));
        post.setBlogUrl(c.getString(c.getColumnIndex("blog_url")));
        post.setExcerpt(c.getString(c.getColumnIndex("excerpt")));
        post.setFeaturedImage(c.getString(c.getColumnIndex("featured_image")));
        post.setFeaturedVideo(c.getString(c.getColumnIndex("featured_video")));

        post.setTitle(c.getString(c.getColumnIndex("title")));
        post.setText(c.getString(c.getColumnIndex("text")));
        post.setUrl(c.getString(c.getColumnIndex("url")));
        post.setPostAvatar(c.getString(c.getColumnIndex("post_avatar")));

        post.timestamp = c.getLong(c.getColumnIndex("timestamp"));
        post.setPublished(c.getString(c.getColumnIndex("published")));

        post.numReplies = c.getInt(c.getColumnIndex("num_replies"));
        post.numLikes = c.getInt(c.getColumnIndex("num_likes"));

        post.isLikedByCurrentUser = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_liked")));
        post.isFollowedByCurrentUser = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_followed")));
        post.isCommentsOpen = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_comments_open")));
        post.isRebloggedByCurrentUser = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_reblogged")));
        post.isExternal = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_external")));
        post.isPrivate = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_private")));
        post.isVideoPress = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_videopress")));

        post.setTags(c.getString(c.getColumnIndex("tag_list")));

        return post;
    }
}
