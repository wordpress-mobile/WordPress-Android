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
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostIdList;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.CrashlyticsUtils;
import org.wordpress.android.util.SqlUtils;

/**
 * tbl_posts contains all reader posts - note that the same post can occur multiple times in this
 * table if it exists in multiple tag streams (ex: it exists in both followed sites and liked posts)
 */
public class ReaderPostTable {
    private static final String COLUMN_NAMES =
            "post_id,"              // 1
          + "blog_id,"              // 2
          + "feed_id,"              // 3
          + "feed_item_id,"         // 4
          + "pseudo_id,"            // 5
          + "author_name,"          // 6
          + "author_first_name,"    // 7
          + "author_id,"            // 8
          + "title,"                // 9
          + "text,"                 // 10
          + "excerpt,"              // 11
          + "format,"               // 12
          + "url,"                  // 13
          + "short_url,"            // 14
          + "blog_url,"             // 15
          + "blog_name,"            // 16
          + "featured_image,"       // 17
          + "featured_video,"       // 18
          + "post_avatar,"          // 19
          + "score,"                // 20
          + "date_published,"       // 21
          + "date_liked,"           // 22
          + "date_tagged,"          // 23
          + "num_replies,"          // 24
          + "num_likes,"            // 25
          + "is_liked,"             // 26
          + "is_followed,"          // 27
          + "is_comments_open,"     // 28
          + "is_external,"          // 29
          + "is_private,"           // 30
          + "is_videopress,"        // 31
          + "is_jetpack,"           // 32
          + "primary_tag,"          // 33
          + "secondary_tag,"        // 34
          + "attachments_json,"     // 35
          + "discover_json,"        // 36
          + "xpost_post_id,"        // 37
          + "xpost_blog_id,"        // 38
          + "railcar_json,"         // 39
          + "tag_name,"             // 40
          + "tag_type,"             // 41
          + "has_gap_marker";       // 42

    // used when querying multiple rows and skipping text column
    private static final String COLUMN_NAMES_NO_TEXT =
            "post_id,"              // 1
          + "blog_id,"              // 2
          + "feed_id,"              // 3
          + "feed_item_id,"         // 4
          + "author_id,"            // 5
          + "pseudo_id,"            // 6
          + "author_name,"          // 7
          + "author_first_name,"    // 8
          + "blog_name,"            // 9
          + "blog_url,"             // 10
          + "excerpt,"              // 11
          + "format,"               // 12
          + "featured_image,"       // 13
          + "featured_video,"       // 14
          + "title,"                // 15
          + "url,"                  // 16
          + "short_url,"            // 17
          + "post_avatar,"          // 18
          + "score,"                // 19
          + "date_published,"       // 20
          + "date_liked,"           // 21
          + "date_tagged,"          // 22
          + "num_replies,"          // 23
          + "num_likes,"            // 24
          + "is_liked,"             // 25
          + "is_followed,"          // 26
          + "is_comments_open,"     // 27
          + "is_external,"          // 28
          + "is_private,"           // 29
          + "is_videopress,"        // 30
          + "is_jetpack,"           // 31
          + "primary_tag,"          // 32
          + "secondary_tag,"        // 33
          + "attachments_json,"     // 34
          + "discover_json,"        // 35
          + "xpost_post_id,"        // 36
          + "xpost_blog_id,"        // 37
          + "railcar_json,"         // 38
          + "tag_name,"             // 39
          + "tag_type,"             // 40
          + "has_gap_marker";       // 41

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_posts ("
                + "	post_id		        INTEGER DEFAULT 0,"
                + " blog_id             INTEGER DEFAULT 0,"
                + " feed_id             INTEGER DEFAULT 0,"
                + " feed_item_id        INTEGER DEFAULT 0,"
                + " pseudo_id           TEXT NOT NULL,"
                + "	author_name	        TEXT,"
                + "	author_first_name	TEXT,"
                + " author_id           INTEGER DEFAULT 0,"
                + "	title	            TEXT,"
                + "	text                TEXT,"
                + "	excerpt             TEXT,"
                + "	format              TEXT,"
                + " url                 TEXT,"
                + " short_url           TEXT,"
                + " blog_url            TEXT,"
                + " blog_name           TEXT,"
                + " featured_image      TEXT,"
                + " featured_video      TEXT,"
                + " post_avatar         TEXT,"
                + " score               REAL DEFAULT 0,"
                + " date_published      TEXT,"
                + " date_liked          TEXT,"
                + " date_tagged         TEXT,"
                + " num_replies         INTEGER DEFAULT 0,"
                + " num_likes           INTEGER DEFAULT 0,"
                + " is_liked            INTEGER DEFAULT 0,"
                + " is_followed         INTEGER DEFAULT 0,"
                + " is_comments_open    INTEGER DEFAULT 0,"
                + " is_external         INTEGER DEFAULT 0,"
                + " is_private          INTEGER DEFAULT 0,"
                + " is_videopress       INTEGER DEFAULT 0,"
                + " is_jetpack          INTEGER DEFAULT 0,"
                + " primary_tag         TEXT,"
                + " secondary_tag       TEXT,"
                + " attachments_json    TEXT,"
                + " discover_json       TEXT,"
                + "	xpost_post_id		INTEGER DEFAULT 0,"
                + " xpost_blog_id       INTEGER DEFAULT 0,"
                + " railcar_json        TEXT,"
                + " tag_name            TEXT NOT NULL COLLATE NOCASE,"
                + " tag_type            INTEGER DEFAULT 0,"
                + " has_gap_marker      INTEGER DEFAULT 0,"
                + " PRIMARY KEY (pseudo_id, tag_name, tag_type)"
                + ")");

        db.execSQL("CREATE INDEX idx_posts_post_id_blog_id ON tbl_posts(post_id, blog_id)");
        db.execSQL("CREATE INDEX idx_posts_date_published ON tbl_posts(date_published)");
        db.execSQL("CREATE INDEX idx_posts_date_tagged ON tbl_posts(date_tagged)");
        db.execSQL("CREATE INDEX idx_posts_tag_name ON tbl_posts(tag_name)");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_posts");
    }

    protected static void reset(SQLiteDatabase db) {
        dropTables(db);
        createTables(db);
    }

    /*
     * purge table of unattached/older posts - no need to wrap this in a transaction since it's
     * only called from ReaderDatabase.purge() which already creates a transaction
     */
    protected static int purge(SQLiteDatabase db) {
        // delete posts attached to tags that no longer exist
        int numDeleted = db.delete("tbl_posts", "tag_name NOT IN (SELECT DISTINCT tag_name FROM tbl_tags)", null);

        // delete excess posts on a per-tag basis
        ReaderTagList tags = ReaderTagTable.getAllTags();
        for (ReaderTag tag: tags) {
            numDeleted += purgePostsForTag(db, tag);
        }

        // delete search results
        numDeleted += purgeSearchResults(db);

        return numDeleted;
    }

    /*
     * purge excess posts in the passed tag
     */
    private static final int MAX_POSTS_PER_TAG = ReaderConstants.READER_MAX_POSTS_TO_DISPLAY;
    private static int purgePostsForTag(SQLiteDatabase db, ReaderTag tag) {
        int numPosts = getNumPostsWithTag(tag);
        if (numPosts <= MAX_POSTS_PER_TAG) {
            return 0;
        }

        int numToPurge = numPosts - MAX_POSTS_PER_TAG;
        String[] args = {tag.getTagSlug(), Integer.toString(tag.tagType.toInt()), Integer.toString(numToPurge)};
        String where = "tag_name=? AND tag_type=? ORDER BY " + getSortColumnForTag(tag) + " LIMIT ?";
        int numDeleted = db.delete("tbl_posts", where, args);
        AppLog.d(AppLog.T.READER, String.format("reader post table > purged %d posts in tag %s", numDeleted, tag.getTagNameForLog()));
        return numDeleted;
    }

    /*
     * purge all posts that were retained from previous searches
     */
    private static int purgeSearchResults(SQLiteDatabase db) {
        String[] args = {Integer.toString(ReaderTagType.SEARCH.toInt())};
        return db.delete("tbl_posts", "tag_type=?", args);
    }

    public static int getNumPostsInBlog(long blogId) {
        if (blogId == 0) {
            return 0;
        }
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                "SELECT count(*) FROM tbl_posts WHERE blog_id=?",
                new String[]{Long.toString(blogId)});
    }

    public static int getNumPostsInFeed(long feedId) {
        if (feedId == 0) {
            return 0;
        }
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                "SELECT count(*) FROM tbl_posts WHERE feed_id=?",
                new String[]{Long.toString(feedId)});
    }

    public static int getNumPostsWithTag(ReaderTag tag) {
        if (tag == null) {
            return 0;
        }
        String[] args = {tag.getTagSlug(), Integer.toString(tag.tagType.toInt())};
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                    "SELECT count(*) FROM tbl_posts WHERE tag_name=? AND tag_type=?",
                    args);
    }

    public static void addOrUpdatePost(ReaderPost post) {
        if (post == null) {
            return;
        }
        ReaderPostList posts = new ReaderPostList();
        posts.add(post);
        addOrUpdatePosts(null, posts);
    }

    public static ReaderPost getBlogPost(long blogId, long postId, boolean excludeTextColumn) {
        return getPost(false, blogId, postId, excludeTextColumn);
    }

    public static ReaderPost getFeedPost(long feedId, long feedItemId, boolean excludeTextColumn) {
        return getPost(true, feedId, feedItemId, excludeTextColumn);
    }

    private static ReaderPost getPost(boolean isFeed, long blogOrFeedId, long postOrItemId, boolean excludeTextColumn) {
        String columns = (excludeTextColumn ? COLUMN_NAMES_NO_TEXT : "*");
        String sql = "SELECT " + columns + " FROM tbl_posts WHERE "
                + (isFeed ? "feed_id" : "blog_id") + "=? AND " + (isFeed ? "feed_item_id" : "post_id") + "=? LIMIT 1";

        String[] args = new String[] {Long.toString(blogOrFeedId), Long.toString(postOrItemId)};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery(sql, args);
        try {
            if (!c.moveToFirst()) {
                return null;
            }
            return getPostFromCursor(c);
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static String getPostTitle(long blogId, long postId) {
        String[] args = {Long.toString(blogId), Long.toString(postId)};
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(),
                "SELECT title FROM tbl_posts WHERE blog_id=? AND post_id=?",
                args);
    }

    public static String getPostText(long blogId, long postId) {
        String[] args = {Long.toString(blogId), Long.toString(postId)};
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(),
                "SELECT text FROM tbl_posts WHERE blog_id=? AND post_id=?",
                args);
    }

    public static boolean postExists(long blogId, long postId) {
        String[] args = {Long.toString(blogId), Long.toString(postId)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(),
                "SELECT 1 FROM tbl_posts WHERE blog_id=? AND post_id=?",
                args);
    }

    /*
     * returns whether any of the passed posts are new or changed - used after posts are retrieved
     */
    public static ReaderActions.UpdateResult comparePosts(ReaderPostList posts) {
        if (posts == null || posts.size() == 0) {
            return ReaderActions.UpdateResult.UNCHANGED;
        }

        boolean hasChanges = false;
        for (ReaderPost post: posts) {
            ReaderPost existingPost = getBlogPost(post.blogId, post.postId, true);
            if (existingPost == null) {
                return ReaderActions.UpdateResult.HAS_NEW;
            } else if (!hasChanges && !post.isSamePost(existingPost)) {
                hasChanges = true;
            }
        }

        return (hasChanges ? ReaderActions.UpdateResult.CHANGED : ReaderActions.UpdateResult.UNCHANGED);
    }

    /*
     * returns true if any posts in the passed list exist in this list
     */
    public static boolean hasOverlap(ReaderPostList posts) {
        for (ReaderPost post: posts) {
            if (postExists(post.blogId, post.postId)) {
                return true;
            }
        }
        return false;
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
        return post != null && isPostLikedByCurrentUser(post.blogId, post.postId);
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

        String[] args = {tag.getTagSlug(), Integer.toString(tag.tagType.toInt())};
        return ReaderDatabase.getWritableDb().delete(
                "tbl_posts",
                "tag_name=? AND tag_type=?",
                args);
    }

    public static int deletePostsInBlog(long blogId) {
        String[] args = {Long.toString(blogId)};
        return ReaderDatabase.getWritableDb().delete("tbl_posts", "blog_id = ?", args);
    }

    /*
     * ensure that posts in blogs that are no longer followed don't have their followed status
     * set to true
     */
    public static void updateFollowedStatus() {
        SQLiteStatement statement = ReaderDatabase.getWritableDb().compileStatement(
                  "UPDATE tbl_posts SET is_followed = 0"
                + " WHERE is_followed != 0"
                + " AND blog_id NOT IN (SELECT DISTINCT blog_id FROM tbl_blog_info WHERE is_followed != 0)");
        try {
            int count = statement.executeUpdateDelete();
            if (count > 0) {
                AppLog.d(AppLog.T.READER, String.format("reader post table > marked %d posts unfollowed", count));
            }
        } finally {
            statement.close();
        }
    }

    /*
     * returns the iso8601 date of the oldest post with the passed tag
     */
    public static String getOldestDateWithTag(final ReaderTag tag) {
        if (tag == null) {
            return "";
        }

        // date field depends on the tag
        String dateColumn = getSortColumnForTag(tag);
        String sql = "SELECT " + dateColumn + " FROM tbl_posts"
                   + " WHERE tag_name=? AND tag_type=?"
                   + " ORDER BY " + dateColumn + " LIMIT 1";
        String[] args = {tag.getTagSlug(), Integer.toString(tag.tagType.toInt())};
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), sql, args);
    }

    /*
     * returns the iso8601 pub date of the oldest post in the passed blog
     */
    public static String getOldestPubDateInBlog(long blogId) {
        String sql = "SELECT date_published FROM tbl_posts"
                  + " WHERE blog_id = ?"
                  + " ORDER BY date_published LIMIT 1";
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), sql, new String[]{Long.toString(blogId)});
    }

    public static String getOldestPubDateInFeed(long feedId) {
        String sql = "SELECT date_published FROM tbl_posts"
                  + " WHERE feed_id = ?"
                  + " ORDER BY date_published LIMIT 1";
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), sql, new String[]{Long.toString(feedId)});
    }

    public static void removeGapMarkerForTag(final ReaderTag tag) {
        if (tag == null) return;

        String[] args = {tag.getTagSlug(), Integer.toString(tag.tagType.toInt())};
        String sql = "UPDATE tbl_posts SET has_gap_marker=0 WHERE has_gap_marker!=0 AND tag_name=? AND tag_type=?";
        ReaderDatabase.getWritableDb().execSQL(sql, args);
    }

    /*
     * returns the blogId/postId of the post with the passed tag that has a gap marker, or null if none exists
     */
    public static ReaderBlogIdPostId getGapMarkerIdsForTag(final ReaderTag tag) {
        if (tag == null) {
            return null;
        }

        String[] args = {tag.getTagSlug(), Integer.toString(tag.tagType.toInt())};
        String sql = "SELECT blog_id, post_id FROM tbl_posts WHERE has_gap_marker!=0 AND tag_name=? AND tag_type=?";
        Cursor cursor = ReaderDatabase.getReadableDb().rawQuery(sql, args);
        try {
            if (cursor.moveToFirst()) {
                long blogId = cursor.getLong(0);
                long postId = cursor.getLong(1);
                return new ReaderBlogIdPostId(blogId, postId);
            } else {
                return null;
            }
        } finally {
            SqlUtils.closeCursor(cursor);
        }
    }

    public static void setGapMarkerForTag(long blogId, long postId, ReaderTag tag) {
        if (tag == null) return;

        String[] args = {
                Long.toString(blogId),
                Long.toString(postId),
                tag.getTagSlug(),
                Integer.toString(tag.tagType.toInt())
        };
        String sql = "UPDATE tbl_posts SET has_gap_marker=1 WHERE blog_id=? AND post_id=? AND tag_name=? AND tag_type=?";
        ReaderDatabase.getWritableDb().execSQL(sql, args);
    }

    public static String getGapMarkerDateForTag(ReaderTag tag) {
        ReaderBlogIdPostId ids = getGapMarkerIdsForTag(tag);
        if (ids == null) {
            return null;
        }

        String dateColumn = getSortColumnForTag(tag);
        String[] args = {Long.toString(ids.getBlogId()), Long.toString(ids.getPostId())};
        String sql = "SELECT " + dateColumn + " FROM tbl_posts WHERE blog_id=? AND post_id=?";
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), sql, args);
    }

    /*
     * the column posts are sorted by depends on the type of tag stream being displayed:
     *
     *      liked posts      sort by the date the post was liked
     *      followed posts   sort by the date the post was published
     *      search results   sort by score
     *      tagged posts     sort by the date the post was tagged
     */
    private static String getSortColumnForTag(ReaderTag tag) {
        if (tag.isPostsILike()) {
            return "date_liked";
        } else if (tag.isFollowedSites()) {
            return "date_published";
        } else if (tag.tagType == ReaderTagType.SEARCH) {
            return "score";
        } else if (tag.isTagTopic()) {
            return "date_tagged";
        } else {
            return "date_published";
        }
    }

    /*
     * delete posts with the passed tag that come before the one with the gap marker for
     * this tag - note this may leave some stray posts in tbl_posts, but these will
     * be cleaned up by the next purge
     */
    public static void deletePostsBeforeGapMarkerForTag(ReaderTag tag) {
        String gapMarkerDate = getGapMarkerDateForTag(tag);
        if (TextUtils.isEmpty(gapMarkerDate)) return;

        String dateColumn = getSortColumnForTag(tag);
        String[] args = {gapMarkerDate, tag.getTagSlug(), Integer.toString(tag.tagType.toInt())};
        String where = "tag_name=? AND tag_type=? AND " + dateColumn + " < ?";
        int numDeleted = ReaderDatabase.getWritableDb().delete("tbl_posts", where, args);
        if (numDeleted > 0) {
            AppLog.d(AppLog.T.READER, "removed " + numDeleted + " posts older than gap marker");
        }
    }

    public static void setFollowStatusForPostsInBlog(long blogId, boolean isFollowed) {
        setFollowStatusForPosts(blogId, 0, isFollowed);
    }
    public static void setFollowStatusForPostsInFeed(long feedId, boolean isFollowed) {
        setFollowStatusForPosts(0, feedId, isFollowed);
    }
    private static void setFollowStatusForPosts(long blogId, long feedId, boolean isFollowed) {
        if (blogId == 0 && feedId == 0) {
            return;
        }

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        try {
            if (blogId != 0) {
                String sql = "UPDATE tbl_posts SET is_followed=" + SqlUtils.boolToSql(isFollowed)
                          + " WHERE blog_id=?";
                db.execSQL(sql, new String[]{Long.toString(blogId)});
            } else {
                String sql = "UPDATE tbl_posts SET is_followed=" + SqlUtils.boolToSql(isFollowed)
                          + " WHERE feed_id=?";
                db.execSQL(sql, new String[]{Long.toString(feedId)});
            }


            // if blog/feed is no longer followed, remove its posts tagged with "Followed Sites"
            if (!isFollowed) {
                if (blogId != 0) {
                    db.delete("tbl_posts", "blog_id=? AND tag_name=?",
                            new String[]{Long.toString(blogId), ReaderTag.TAG_TITLE_FOLLOWED_SITES});
                } else {
                    db.delete("tbl_posts", "feed_id=? AND tag_name=?",
                            new String[]{Long.toString(feedId), ReaderTag.TAG_TITLE_FOLLOWED_SITES});
                }
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
     * https://github.com/android/platform_frameworks_base/blob/b77bc869241644a662f7e615b0b00ecb5aee373d/core/res/res/values/config.xml#L1268
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
                        + ") VALUES (?1,?2,?3,?4,?5,?6,?7,?8,?9,?10,?11,?12,?13,?14,?15,?16,?17,?18,?19,?20,?21,?22,?23,?24,?25,?26,?27,?28,?29,?30,?31,?32,?33,?34,?35,?36,?37,?38,?39,?40,?41,?42)");

        db.beginTransaction();
        try {
            String tagName = (tag != null ? tag.getTagSlug() : "");
            int tagType = (tag != null ? tag.tagType.toInt() : 0);

            // we can safely assume there's no gap marker because any existing gap marker is
            // already removed before posts are updated
            boolean hasGapMarker = false;

            for (ReaderPost post: posts) {
                stmtPosts.bindLong  (1,  post.postId);
                stmtPosts.bindLong  (2,  post.blogId);
                stmtPosts.bindLong  (3,  post.feedId);
                stmtPosts.bindLong  (4,  post.feedItemId);
                stmtPosts.bindString(5,  post.getPseudoId());
                stmtPosts.bindString(6,  post.getAuthorName());
                stmtPosts.bindString(7,  post.getAuthorFirstName());
                stmtPosts.bindLong  (8,  post.authorId);
                stmtPosts.bindString(9,  post.getTitle());
                stmtPosts.bindString(10, maxText(post));
                stmtPosts.bindString(11, post.getExcerpt());
                stmtPosts.bindString(12, post.getFormat());
                stmtPosts.bindString(13, post.getUrl());
                stmtPosts.bindString(14, post.getShortUrl());
                stmtPosts.bindString(15, post.getBlogUrl());
                stmtPosts.bindString(16, post.getBlogName());
                stmtPosts.bindString(17, post.getFeaturedImage());
                stmtPosts.bindString(18, post.getFeaturedVideo());
                stmtPosts.bindString(19, post.getPostAvatar());
                stmtPosts.bindDouble(20, post.score);
                stmtPosts.bindString(21, post.getDatePublished());
                stmtPosts.bindString(22, post.getDateLiked());
                stmtPosts.bindString(23, post.getDateTagged());
                stmtPosts.bindLong  (24, post.numReplies);
                stmtPosts.bindLong  (25, post.numLikes);
                stmtPosts.bindLong  (26, SqlUtils.boolToSql(post.isLikedByCurrentUser));
                stmtPosts.bindLong  (27, SqlUtils.boolToSql(post.isFollowedByCurrentUser));
                stmtPosts.bindLong  (28, SqlUtils.boolToSql(post.isCommentsOpen));
                stmtPosts.bindLong  (29, SqlUtils.boolToSql(post.isExternal));
                stmtPosts.bindLong  (30, SqlUtils.boolToSql(post.isPrivate));
                stmtPosts.bindLong  (31, SqlUtils.boolToSql(post.isVideoPress));
                stmtPosts.bindLong  (32, SqlUtils.boolToSql(post.isJetpack));
                stmtPosts.bindString(33, post.getPrimaryTag());
                stmtPosts.bindString(34, post.getSecondaryTag());
                stmtPosts.bindString(35, post.getAttachmentsJson());
                stmtPosts.bindString(36, post.getDiscoverJson());
                stmtPosts.bindLong  (37, post.xpostPostId);
                stmtPosts.bindLong  (38, post.xpostBlogId);
                stmtPosts.bindString(39, post.getRailcarJson());
                stmtPosts.bindString(40, tagName);
                stmtPosts.bindLong  (41, tagType);
                stmtPosts.bindLong  (42, SqlUtils.boolToSql(hasGapMarker));
                stmtPosts.execute();
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmtPosts);
        }
    }

    public static ReaderPostList getPostsWithTag(ReaderTag tag, int maxPosts, boolean excludeTextColumn) {
        if (tag == null) {
            return new ReaderPostList();
        }

        String columns = (excludeTextColumn ? COLUMN_NAMES_NO_TEXT : "*");
        String sql = "SELECT " + columns + " FROM tbl_posts WHERE tag_name=? AND tag_type=?";

        if (tag.tagType == ReaderTagType.DEFAULT) {
            // skip posts that are no longer liked if this is "Posts I Like", skip posts that are no
            // longer followed if this is "Followed Sites"
            if (tag.isPostsILike()) {
                sql += " AND is_liked != 0";
            } else if (tag.isFollowedSites()) {
                sql += " AND is_followed != 0";
            }
        }

        sql += " ORDER BY " + getSortColumnForTag(tag) + " DESC";

        if (maxPosts > 0) {
            sql += " LIMIT " + Integer.toString(maxPosts);
        }

        String[] args = {tag.getTagSlug(), Integer.toString(tag.tagType.toInt())};
        Cursor cursor = ReaderDatabase.getReadableDb().rawQuery(sql, args);
        try {
            return getPostListFromCursor(cursor);
        } finally {
            SqlUtils.closeCursor(cursor);
        }
    }

    public static ReaderPostList getPostsInBlog(long blogId, int maxPosts, boolean excludeTextColumn) {
        String columns = (excludeTextColumn ? COLUMN_NAMES_NO_TEXT : "*");
        String sql = "SELECT " + columns + " FROM tbl_posts WHERE blog_id = ? ORDER BY date_published DESC";

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

    public static ReaderPostList getPostsInFeed(long feedId, int maxPosts, boolean excludeTextColumn) {
        String columns = (excludeTextColumn ? COLUMN_NAMES_NO_TEXT : "*");
        String sql = "SELECT " + columns + " FROM tbl_posts WHERE feed_id = ? ORDER BY date_published DESC";

        if (maxPosts > 0) {
            sql += " LIMIT " + Integer.toString(maxPosts);
        }

        Cursor cursor = ReaderDatabase.getReadableDb().rawQuery(sql, new String[]{Long.toString(feedId)});
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

        String sql = "SELECT blog_id, post_id FROM tbl_posts WHERE tag_name=? AND tag_type=?";

        if (tag.tagType == ReaderTagType.DEFAULT) {
            if (tag.isPostsILike()) {
                sql += " AND is_liked != 0";
            } else if (tag.isFollowedSites()) {
                sql += " AND is_followed != 0";
            }
        }

        sql += " ORDER BY " + getSortColumnForTag(tag) + " DESC";

        if (maxPosts > 0) {
            sql += " LIMIT " + Integer.toString(maxPosts);
        }

        String[] args = {tag.getTagSlug(), Integer.toString(tag.tagType.toInt())};
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
        String sql = "SELECT post_id FROM tbl_posts WHERE blog_id = ? ORDER BY date_published DESC";

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
        post.feedId = c.getLong(c.getColumnIndex("feed_id"));
        post.feedItemId = c.getLong(c.getColumnIndex("feed_item_id"));
        post.authorId = c.getLong(c.getColumnIndex("author_id"));
        post.setPseudoId(c.getString(c.getColumnIndex("pseudo_id")));

        post.setAuthorName(c.getString(c.getColumnIndex("author_name")));
        post.setAuthorFirstName(c.getString(c.getColumnIndex("author_first_name")));
        post.setBlogName(c.getString(c.getColumnIndex("blog_name")));
        post.setBlogUrl(c.getString(c.getColumnIndex("blog_url")));
        post.setExcerpt(c.getString(c.getColumnIndex("excerpt")));
        post.setFormat(c.getString(c.getColumnIndex("format")));
        post.setFeaturedImage(c.getString(c.getColumnIndex("featured_image")));
        post.setFeaturedVideo(c.getString(c.getColumnIndex("featured_video")));

        post.setTitle(c.getString(c.getColumnIndex("title")));
        post.setUrl(c.getString(c.getColumnIndex("url")));
        post.setShortUrl(c.getString(c.getColumnIndex("short_url")));
        post.setPostAvatar(c.getString(c.getColumnIndex("post_avatar")));

        post.setDatePublished(c.getString(c.getColumnIndex("date_published")));
        post.setDateLiked(c.getString(c.getColumnIndex("date_liked")));
        post.setDateTagged(c.getString(c.getColumnIndex("date_tagged")));

        post.score = c.getDouble(c.getColumnIndex("score"));
        post.numReplies = c.getInt(c.getColumnIndex("num_replies"));
        post.numLikes = c.getInt(c.getColumnIndex("num_likes"));

        post.isLikedByCurrentUser = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_liked")));
        post.isFollowedByCurrentUser = SqlUtils.sqlToBool(c.getInt( c.getColumnIndex("is_followed")));
        post.isCommentsOpen = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_comments_open")));
        post.isExternal = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_external")));
        post.isPrivate = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_private")));
        post.isVideoPress = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_videopress")));
        post.isJetpack = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_jetpack")));

        post.setPrimaryTag(c.getString(c.getColumnIndex("primary_tag")));
        post.setSecondaryTag(c.getString(c.getColumnIndex("secondary_tag")));

        post.setAttachmentsJson(c.getString(c.getColumnIndex("attachments_json")));
        post.setDiscoverJson(c.getString(c.getColumnIndex("discover_json")));

        post.xpostPostId = c.getLong(c.getColumnIndex("xpost_post_id"));
        post.xpostBlogId = c.getLong(c.getColumnIndex("xpost_blog_id"));

        post.setRailcarJson(c.getString(c.getColumnIndex("railcar_json")));

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
