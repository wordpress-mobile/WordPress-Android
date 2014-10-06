package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostIdList;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.CrashlyticsUtils;
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
          + "primary_tag,"          // 26
          + "secondary_tag,"        // 27
          + "is_likes_enabled,"     // 28
          + "is_sharing_enabled,"   // 29
          + "attachments_json";     // 30

    // used when querying multiple rows and skipping tbl_posts.text
    private static final String COLUMN_NAMES_NO_TEXT =
            "tbl_posts.post_id,"              // 1
          + "tbl_posts.blog_id,"              // 2
          + "tbl_posts.author_id,"            // 3
          + "tbl_posts.pseudo_id,"            // 4
          + "tbl_posts.author_name,"          // 5
          + "tbl_posts.blog_name,"            // 6
          + "tbl_posts.blog_url,"             // 7
          + "tbl_posts.excerpt,"              // 8
          + "tbl_posts.featured_image,"       // 9
          + "tbl_posts.featured_video,"       // 10
          + "tbl_posts.title,"                // 11
          + "tbl_posts.url,"                  // 12
          + "tbl_posts.post_avatar,"          // 13
          + "tbl_posts.timestamp,"            // 14
          + "tbl_posts.published,"            // 15
          + "tbl_posts.num_replies,"          // 16
          + "tbl_posts.num_likes,"            // 17
          + "tbl_posts.is_liked,"             // 18
          + "tbl_posts.is_followed,"          // 19
          + "tbl_posts.is_comments_open,"     // 20
          + "tbl_posts.is_reblogged,"         // 21
          + "tbl_posts.is_external,"          // 22
          + "tbl_posts.is_private,"           // 23
          + "tbl_posts.is_videopress,"        // 24
          + "tbl_posts.primary_tag,"          // 25
          + "tbl_posts.secondary_tag,"        // 26
          + "tbl_posts.is_likes_enabled,"     // 27
          + "tbl_posts.is_sharing_enabled,"   // 28
          + "tbl_posts.attachments_json";     // 29

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_posts ("
                + "	post_id		        INTEGER DEFAULT 0,"       // post_id for WP blogs, feed_item_id for non-WP blogs
                + " blog_id             INTEGER DEFAULT 0,"       // blog_id for WP blogs, feed_id for non-WP blogs
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
                + " primary_tag         TEXT,"
                + " secondary_tag       TEXT,"
                + " is_likes_enabled    INTEGER DEFAULT 0,"
                + " is_sharing_enabled  INTEGER DEFAULT 0,"
                + " attachments_json    TEXT,"
                + " PRIMARY KEY (post_id, blog_id)"
                + ")");
        db.execSQL("CREATE INDEX idx_posts_timestamp ON tbl_posts(timestamp)");

        db.execSQL("CREATE TABLE tbl_post_tags ("
                + "   post_id     INTEGER DEFAULT 0,"
                + "   blog_id     INTEGER DEFAULT 0,"
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

    /*
     * remove posts in "Blogs I Follow" that are no longer attached to followed blogs
     */
    public static int purgeUnfollowedPosts() {
        String[] args = {ReaderTag.TAG_NAME_FOLLOWING};
        int numPurged = ReaderDatabase.getWritableDb().delete(
                "tbl_post_tags",
                "tag_name=? AND blog_id NOT IN (SELECT DISTINCT blog_id FROM tbl_blog_info WHERE is_following!=0)",
                args);
        if (numPurged > 0) {
            AppLog.d(AppLog.T.READER, String.format("purged %d unfollowed posts", numPurged));
        }
        return numPurged;
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

    public static boolean isPostLikedByCurrentUser(ReaderPost post) {
        if (post == null) {
            return false;
        }
        return isPostLikedByCurrentUser(post.blogId, post.postId);
    }
    public static boolean isPostLikedByCurrentUser(long blogId, long postId) {
        String[] args = new String[] {Long.toString(blogId), Long.toString(postId)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(),
                "SELECT is_liked FROM tbl_posts WHERE blog_id=? AND post_id=?",
                args);
    }

    /*
     * updates both the like count for a post and whether it's liked by the current user
     */
    public static void setLikesForPost(ReaderPost post, int numLikes, boolean isLikedByCurrentUser) {
        if (post == null) {
            return;
        }

        String[] args = {Long.toString(post.blogId), Long.toString(post.postId)};

        ContentValues values = new ContentValues();
        values.put("num_likes", numLikes);
        values.put("is_liked", SqlUtils.boolToSql(isLikedByCurrentUser));

        ReaderDatabase.getWritableDb().update(
                "tbl_posts",
                values,
                "blog_id=? AND post_id=?",
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

    /*
     * Android's CursorWindow has a max size of 2MB per row which can be exceeded
     * with a very large text column, causing an IllegalStateException when the
     * row is read - prevent this by limiting the amount of text that's stored in
     * the text column - note that this situation very rarely occurs
     * https://github.com/android/platform_frameworks_base/blob/master/core/res/res/values/config.xml#L946
     * https://github.com/android/platform_frameworks_base/blob/3bdbf644d61f46b531838558fabbd5b990fc4913/core/java/android/database/CursorWindow.java#L103
     */
    private static final int MAX_TEXT_LEN = (1024 * 1024) / 2;
    private static String maxText(final ReaderPost post) {
        if (post.getText().length() <= MAX_TEXT_LEN) {
            return post.getText();
        }
        // if the post has an excerpt (which should always be the case), store it as the full text
        // with a link to the full article
        if (post.hasExcerpt()) {
            AppLog.w(AppLog.T.READER, "reader post table > max text exceeded, storing excerpt");
            return "<p>" + post.getExcerpt() + "</p>"
                  + String.format("<p style='text-align:center'><a href='%s'>%s</a></p>",
                    post.getUrl(), WordPress.getContext().getString(R.string.reader_label_view_original));
        } else {
            AppLog.w(AppLog.T.READER, "reader post table > max text exceeded, storing truncated text");
            return post.getText().substring(0, MAX_TEXT_LEN);
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
                + ") VALUES (?1,?2,?3,?4,?5,?6,?7,?8,?9,?10,?11,?12,?13,?14,?15,?16,?17,?18,?19,?20,?21,?22,?23,?24,?25,?26,?27,?28,?29,?30)");
        SQLiteStatement stmtTags = db.compileStatement(
                "INSERT OR REPLACE INTO tbl_post_tags (post_id, blog_id, pseudo_id, tag_name, tag_type) VALUES (?1,?2,?3,?4,?5)");

        db.beginTransaction();
        try {
            // first insert into tbl_posts
            for (ReaderPost post: posts) {
                stmtPosts.bindLong  (1,  post.postId);
                stmtPosts.bindLong(2, post.blogId);
                stmtPosts.bindString(3,  post.getPseudoId());
                stmtPosts.bindString(4, post.getAuthorName());
                stmtPosts.bindLong(5, post.authorId);
                stmtPosts.bindString(6,  post.getTitle());
                stmtPosts.bindString(7,  maxText(post));
                stmtPosts.bindString(8,  post.getExcerpt());
                stmtPosts.bindString(9,  post.getUrl());
                stmtPosts.bindString(10, post.getBlogUrl());
                stmtPosts.bindString(11, post.getBlogName());
                stmtPosts.bindString(12, post.getFeaturedImage());
                stmtPosts.bindString(13, post.getFeaturedVideo());
                stmtPosts.bindString(14, post.getPostAvatar());
                stmtPosts.bindLong(15, post.timestamp);
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
                stmtPosts.bindString(26, post.getPrimaryTag());
                stmtPosts.bindString(27, post.getSecondaryTag());
                stmtPosts.bindLong  (28, SqlUtils.boolToSql(post.isLikesEnabled));
                stmtPosts.bindLong  (29, SqlUtils.boolToSql(post.isSharingEnabled));
                stmtPosts.bindString(30, post.getAttachmentsJson());
                stmtPosts.execute();
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
                }
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmtPosts);
            SqlUtils.closeStatement(stmtTags);
        }
    }

    public static ReaderPostList getPostsWithTag(ReaderTag tag, int maxPosts, boolean excludeTextColumn) {
        if (tag == null) {
            return new ReaderPostList();
        }

        String columns = (excludeTextColumn ? COLUMN_NAMES_NO_TEXT : "tbl_posts.*");
        String sql = "SELECT " + columns + " FROM tbl_posts, tbl_post_tags"
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
            return getPostListFromCursor(cursor);
        } finally {
            SqlUtils.closeCursor(cursor);
        }
    }

    public static ReaderPostList getPostsInBlog(long blogId, int maxPosts, boolean excludeTextColumn) {
        String columns = (excludeTextColumn ? COLUMN_NAMES_NO_TEXT : "tbl_posts.*");
        String sql = "SELECT " + columns + " FROM tbl_posts WHERE blog_id = ? ORDER BY tbl_posts.timestamp DESC";

        if (maxPosts > 0) {
            sql += " LIMIT " + Integer.toString(maxPosts);
        }

        Cursor cursor = ReaderDatabase.getReadableDb().rawQuery(sql, new String[]{Long.toString(blogId)});
        try {
            return getPostListFromCursor(cursor);
        } finally {
            SqlUtils.closeCursor(cursor);
        }
    }

    /*
     * same as getPostsWithTag() but only returns the blogId/postId pairs
     */
    public static ReaderBlogIdPostIdList getBlogIdPostIdsWithTag(ReaderTag tag, int maxPosts) {
        ReaderBlogIdPostIdList idList = new ReaderBlogIdPostIdList();
        if (tag == null) {
            return idList;
        }

        String sql = "SELECT tbl_posts.blog_id, tbl_posts.post_id FROM tbl_posts, tbl_post_tags"
                + " WHERE tbl_posts.post_id = tbl_post_tags.post_id"
                + " AND tbl_posts.blog_id = tbl_post_tags.blog_id"
                + " AND tbl_post_tags.tag_name=?"
                + " AND tbl_post_tags.tag_type=?";

        if (tag.tagType == ReaderTagType.DEFAULT) {
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
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    idList.add(new ReaderBlogIdPostId(cursor.getLong(0), cursor.getLong(1)));
                } while (cursor.moveToNext());
            }
            return idList;
        } finally {
            SqlUtils.closeCursor(cursor);
        }
    }

    /*
     * same as getPostsInBlog() but only returns the blogId/postId pairs
     */
    public static ReaderBlogIdPostIdList getBlogIdPostIdsInBlog(long blogId, int maxPosts) {
        String sql = "SELECT post_id FROM tbl_posts WHERE blog_id = ? ORDER BY tbl_posts.timestamp DESC";

        if (maxPosts > 0) {
            sql += " LIMIT " + Integer.toString(maxPosts);
        }

        Cursor cursor = ReaderDatabase.getReadableDb().rawQuery(sql, new String[]{Long.toString(blogId)});
        try {
            ReaderBlogIdPostIdList idList = new ReaderBlogIdPostIdList();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    idList.add(new ReaderBlogIdPostId(blogId, cursor.getLong(0)));
                } while (cursor.moveToNext());
            }

            return idList;
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

    private static ReaderPost getPostFromCursor(Cursor c) {
        if (c == null) {
            throw new IllegalArgumentException("getPostFromCursor > null cursor");
        }

        ReaderPost post = new ReaderPost();

        // text column is skipped when retrieving multiple rows
        int idxText = c.getColumnIndex("text");
        if (idxText > -1) {
            post.setText(c.getString(idxText));
        }

        post.postId = c.getLong(c.getColumnIndex("post_id"));
        post.blogId = c.getLong(c.getColumnIndex("blog_id"));
        post.authorId = c.getLong(c.getColumnIndex("author_id"));
        post.setPseudoId(c.getString(c.getColumnIndex("pseudo_id")));

        post.setAuthorName(c.getString(c.getColumnIndex("author_name")));
        post.setBlogName(c.getString(c.getColumnIndex("blog_name")));
        post.setBlogUrl(c.getString(c.getColumnIndex("blog_url")));
        post.setExcerpt(c.getString(c.getColumnIndex("excerpt")));
        post.setFeaturedImage(c.getString(c.getColumnIndex("featured_image")));
        post.setFeaturedVideo(c.getString(c.getColumnIndex("featured_video")));

        post.setTitle(c.getString(c.getColumnIndex("title")));
        post.setUrl(c.getString(c.getColumnIndex("url")));
        post.setPostAvatar(c.getString(c.getColumnIndex("post_avatar")));

        post.timestamp = c.getLong(c.getColumnIndex("timestamp"));
        post.setPublished(c.getString(c.getColumnIndex("published")));

        post.numReplies = c.getInt(c.getColumnIndex("num_replies"));
        post.numLikes = c.getInt(c.getColumnIndex("num_likes"));

        post.isLikedByCurrentUser = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_liked")));
        post.isFollowedByCurrentUser = SqlUtils.sqlToBool(c.getInt( c.getColumnIndex("is_followed")));
        post.isCommentsOpen = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_comments_open")));
        post.isRebloggedByCurrentUser = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_reblogged")));
        post.isExternal = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_external")));
        post.isPrivate = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_private")));
        post.isVideoPress = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_videopress")));

        post.setPrimaryTag(c.getString(c.getColumnIndex("primary_tag")));
        post.setSecondaryTag(c.getString(c.getColumnIndex("secondary_tag")));

        post.isLikesEnabled = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_likes_enabled")));
        post.isSharingEnabled = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_sharing_enabled")));

        post.setAttachmentsJson(c.getString(c.getColumnIndex("attachments_json")));

        return post;
    }

    private static ReaderPostList getPostListFromCursor(Cursor cursor) {
        ReaderPostList posts = new ReaderPostList();
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    posts.add(getPostFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        } catch (IllegalStateException e) {
            CrashlyticsUtils.logException(e, CrashlyticsUtils.ExceptionType.SPECIFIC);
            AppLog.e(AppLog.T.READER, e);
        }
        return posts;
    }
}
