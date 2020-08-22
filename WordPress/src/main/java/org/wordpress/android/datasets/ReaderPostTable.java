package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.ReaderCardType;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostIdList;
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent.ReaderPostTableActionEnded;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SqlUtils;

import java.util.Locale;

/**
 * tbl_posts contains all reader posts - the primary key is pseudo_id + tag_name + tag_type,
 * which allows the same post to appear in multiple streams (ex: it can exist in followed
 * sites, liked posts, and tag streams). note that posts in a specific blog or feed are
 * stored here with an empty tag_name.
 */
public class ReaderPostTable {
    private static final String COLUMN_NAMES =
            "post_id," // 1
            + "blog_id," // 2
            + "feed_id," // 3
            + "feed_item_id," // 4
            + "pseudo_id," // 5
            + "author_name," // 6
            + "author_first_name," // 7
            + "author_id," // 8
            + "title," // 9
            + "text," // 10
            + "excerpt," // 11
            + "format," // 12
            + "url," // 13
            + "short_url," // 14
            + "blog_name," // 15
            + "blog_url," // 16
            + "blog_image_url," // 17
            + "featured_image," // 18
            + "featured_video," // 19
            + "post_avatar," // 20
            + "score," // 21
            + "date_published," // 22
            + "date_liked," // 23
            + "date_tagged," // 24
            + "num_replies," // 25
            + "num_likes," // 26
            + "is_liked," // 27
            + "is_followed," // 28
            + "is_comments_open," // 29
            + "is_external," // 30
            + "is_private," // 31
            + "is_videopress," // 32
            + "is_jetpack," // 33
            + "primary_tag," // 34
            + "secondary_tag," // 35
            + "attachments_json," // 36
            + "discover_json," // 37
            + "xpost_post_id," // 38
            + "xpost_blog_id," // 39
            + "railcar_json," // 40
            + "tag_name," // 41
            + "tag_type," // 42
            + "has_gap_marker," // 43
            + "card_type," // 44
            + "use_excerpt," // 45
            + "is_bookmarked," // 46
            + "is_private_atomic," // 47
            + "tags"; // 48

    // used when querying multiple rows and skipping text column
    private static final String COLUMN_NAMES_NO_TEXT =
            "post_id," // 1
            + "blog_id," // 2
            + "feed_id," // 3
            + "feed_item_id," // 4
            + "author_id," // 5
            + "pseudo_id," // 6
            + "author_name," // 7
            + "author_first_name," // 8
            + "blog_name," // 9
            + "blog_url," // 10
            + "blog_image_url," // 11
            + "excerpt," // 12
            + "format," // 13
            + "featured_image," // 14
            + "featured_video," // 15
            + "title," // 16
            + "url," // 17
            + "short_url," // 18
            + "post_avatar," // 19
            + "score," // 20
            + "date_published," // 21
            + "date_liked," // 22
            + "date_tagged," // 23
            + "num_replies," // 24
            + "num_likes," // 25
            + "is_liked," // 26
            + "is_followed," // 27
            + "is_comments_open," // 28
            + "is_external," // 29
            + "is_private," // 30
            + "is_videopress," // 31
            + "is_jetpack," // 32
            + "primary_tag," // 33
            + "secondary_tag," // 34
            + "attachments_json," // 35
            + "discover_json," // 36
            + "xpost_post_id," // 37
            + "xpost_blog_id," // 38
            + "railcar_json," // 39
            + "tag_name," // 40
            + "tag_type," // 41
            + "has_gap_marker," // 42
            + "card_type," // 43
            + "use_excerpt," // 44
            + "is_bookmarked," // 45
            + "is_private_atomic," // 46
            + "tags"; // 47

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_posts ("
                   + " post_id INTEGER DEFAULT 0,"
                   + " blog_id INTEGER DEFAULT 0,"
                   + " feed_id INTEGER DEFAULT 0,"
                   + " feed_item_id INTEGER DEFAULT 0,"
                   + " pseudo_id TEXT NOT NULL,"
                   + " author_name TEXT,"
                   + " author_first_name TEXT,"
                   + " author_id INTEGER DEFAULT 0,"
                   + " title  TEXT,"
                   + " text TEXT,"
                   + " excerpt TEXT,"
                   + " format TEXT,"
                   + " url TEXT,"
                   + " short_url TEXT,"
                   + " blog_name TEXT,"
                   + " blog_url TEXT,"
                   + " blog_image_url TEXT,"
                   + " featured_image TEXT,"
                   + " featured_video TEXT,"
                   + " post_avatar TEXT,"
                   + " score REAL DEFAULT 0,"
                   + " date_published TEXT,"
                   + " date_liked TEXT,"
                   + " date_tagged TEXT,"
                   + " num_replies INTEGER DEFAULT 0,"
                   + " num_likes INTEGER DEFAULT 0,"
                   + " is_liked INTEGER DEFAULT 0,"
                   + " is_followed INTEGER DEFAULT 0,"
                   + " is_comments_open INTEGER DEFAULT 0,"
                   + " is_external INTEGER DEFAULT 0,"
                   + " is_private INTEGER DEFAULT 0,"
                   + " is_videopress INTEGER DEFAULT 0,"
                   + " is_jetpack INTEGER DEFAULT 0,"
                   + " primary_tag TEXT,"
                   + " secondary_tag TEXT,"
                   + " attachments_json TEXT,"
                   + " discover_json TEXT,"
                   + " xpost_post_id INTEGER DEFAULT 0,"
                   + " xpost_blog_id INTEGER DEFAULT 0,"
                   + " railcar_json TEXT,"
                   + " tag_name TEXT NOT NULL COLLATE NOCASE,"
                   + " tag_type INTEGER DEFAULT 0,"
                   + " has_gap_marker INTEGER DEFAULT 0,"
                   + " card_type TEXT,"
                   + " use_excerpt INTEGER DEFAULT 0,"
                   + " is_bookmarked INTEGER DEFAULT 0,"
                   + " is_private_atomic INTEGER DEFAULT 0,"
                   + " tags TEXT,"
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
        for (ReaderTag tag : tags) {
            numDeleted += purgePostsForTag(db, tag);
        }

        numDeleted += purgeUnbookmarkedPostsWithBookmarkTag();

        // delete search results
        numDeleted += purgeSearchResults(db);
        return numDeleted;
    }

    /**
     * When the user unbookmarks a post, we keep the row in the database, but we just change the is_bookmarked flag
     * to false, so we can show "undo" items in the saved posts list. This method purges database from such rows.
     */
    public static int purgeUnbookmarkedPostsWithBookmarkTag() {
        int numDeleted = 0;
        ReaderTagList tags = ReaderTagTable.getAllTags();
        for (ReaderTag tag : tags) {
            if (tag.isBookmarked()) {
                // delete posts which has a bookmark tag but is_bookmarked flag is false
                String[] args = {tag.getTagSlug(), Integer.toString(tag.tagType.toInt())};
                numDeleted += ReaderDatabase.getWritableDb()
                                            .delete("tbl_posts", "tag_name=? AND tag_type=? AND is_bookmarked=0", args);
            }
        }
        if (numDeleted > 0) {
            EventBus.getDefault().post(ReaderPostTableActionEnded.INSTANCE);
        }
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
        String tagName = tag.getTagSlug();
        String tagType = Integer.toString(tag.tagType.toInt());
        String[] args = {tagName, tagType, tagName, tagType, Integer.toString(MAX_POSTS_PER_TAG)};
        String where = "tag_name=? AND tag_type=? AND pseudo_id NOT IN (SELECT DISTINCT pseudo_id FROM tbl_posts WHERE "
                       + "tag_name=? AND tag_type=? ORDER BY " + getSortColumnForTag(tag) + " DESC LIMIT ?)";
        int numDeleted = db.delete("tbl_posts", where, args);
        AppLog.d(AppLog.T.READER,
                String.format(Locale.ENGLISH, "reader post table > purged %d posts in tag %s", numDeleted,
                        tag.getTagNameForLog()));
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
                                    "SELECT count(*) FROM tbl_posts WHERE blog_id=? AND tag_name=''",
                                    new String[]{Long.toString(blogId)});
    }

    public static int getNumPostsInFeed(long feedId) {
        if (feedId == 0) {
            return 0;
        }
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                                    "SELECT count(*) FROM tbl_posts WHERE feed_id=? AND tag_name=''",
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

    public static void updatePost(@NonNull ReaderPost post) {
        // we need to update a few important fields across all instances of this post - this is
        // necessary because a post can exist multiple times in the table with different tags
        ContentValues values = new ContentValues();
        values.put("title", post.getTitle());
        values.put("text", post.getText());
        values.put("excerpt", post.getExcerpt());
        values.put("num_replies", post.numReplies);
        values.put("num_likes", post.numLikes);
        values.put("is_liked", post.isLikedByCurrentUser);
        values.put("is_followed", post.isFollowedByCurrentUser);
        values.put("is_comments_open", post.isCommentsOpen);
        values.put("use_excerpt", post.useExcerpt);
        ReaderDatabase.getWritableDb().update(
                "tbl_posts", values, "pseudo_id=?", new String[]{post.getPseudoId()});

        ReaderPostList posts = new ReaderPostList();
        posts.add(post);
        addOrUpdatePosts(null, posts);
    }

    public static void addPost(@NonNull ReaderPost post) {
        ReaderPostList posts = new ReaderPostList();
        posts.add(post);
        addOrUpdatePosts(null, posts);
    }

    public static ReaderPost getBlogPost(long blogId, long postId, boolean excludeTextColumn) {
        return getPost("blog_id=? AND post_id=?", new String[]{Long.toString(blogId), Long.toString(postId)},
                       excludeTextColumn);
    }

    public static ReaderPost getBlogPost(String blogSlug, String postSlug, boolean excludeTextColumn) {
        return getPost("blog_url LIKE ? AND url LIKE ?", new String[]{"%//" + blogSlug, "%/" + postSlug + "/"},
                       excludeTextColumn);
    }

    public static ReaderPost getFeedPost(long feedId, long feedItemId, boolean excludeTextColumn) {
        return getPost("feed_id=? AND feed_item_id=?", new String[]{Long.toString(feedId), Long.toString(feedItemId)},
                       excludeTextColumn);
    }

    private static ReaderPost getPost(String where, String[] args, boolean excludeTextColumn) {
        String columns = (excludeTextColumn ? COLUMN_NAMES_NO_TEXT : "*");
        String sql = "SELECT " + columns + " FROM tbl_posts WHERE " + where + " LIMIT 1";

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

    public static String getPostBlogName(long blogId, long postId) {
        String[] args = {Long.toString(blogId), Long.toString(postId)};
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(),
                                       "SELECT blog_name FROM tbl_posts WHERE blog_id=? AND post_id=?",
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

    private static boolean postExistsForReaderTag(long blogId, long postId, ReaderTag readerTag) {
        String[] args = {Long.toString(blogId), Long.toString(postId), readerTag.getTagSlug(),
                Integer.toString(readerTag.tagType.toInt())};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(),
                "SELECT 1 FROM tbl_posts WHERE blog_id=? AND post_id=? AND tag_name=? AND tag_type=?",
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
        for (ReaderPost post : posts) {
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
     * returns true if any posts in the passed list exist in this list for the given tag
     */
    public static boolean hasOverlap(ReaderPostList posts, ReaderTag tag) {
        for (ReaderPost post : posts) {
            if (postExistsForReaderTag(post.blogId, post.postId, tag)) {
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
        return getNumCommentsForPost(post.blogId, post.postId);
    }

    public static int getNumCommentsForPost(long blogId, long postId) {
        String[] args = new String[]{Long.toString(blogId), Long.toString(postId)};
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                                    "SELECT num_replies FROM tbl_posts WHERE blog_id=? AND post_id=?",
                                    args);
    }

    public static void setNumCommentsForPost(long blogId, long postId, int numComments) {
        ContentValues values = new ContentValues();
        values.put("num_replies", numComments);

        update(blogId, postId, values);
    }

    public static void incNumCommentsForPost(long blogId, long postId) {
        int numComments = getNumCommentsForPost(blogId, postId);
        numComments++;
        setNumCommentsForPost(blogId, postId, numComments);
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
        String[] args = new String[]{Long.toString(blogId), Long.toString(postId)};
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
        ContentValues values = new ContentValues();
        values.put("num_likes", numLikes);
        values.put("is_liked", SqlUtils.boolToSql(isLikedByCurrentUser));

        update(post.blogId, post.postId, values);
    }


    public static void setBookmarkFlag(long blogId, long postId, boolean bookmark) {
        ContentValues values = new ContentValues();
        values.put("is_bookmarked", SqlUtils.boolToSql(bookmark));

        update(blogId, postId, values);
    }

    public static boolean hasBookmarkedPosts() {
        String sql = "SELECT 1 FROM tbl_posts WHERE is_bookmarked != 0 LIMIT 1";
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), sql, null);
    }

    private static void update(long blogId, long postId, ContentValues values) {
        String[] args = {Long.toString(blogId), Long.toString(postId)};
        ReaderDatabase.getWritableDb().update(
                "tbl_posts",
                values,
                "blog_id=? AND post_id=?",
                args);
        EventBus.getDefault().post(ReaderPostTableActionEnded.INSTANCE);
    }


    public static boolean isPostFollowed(ReaderPost post) {
        if (post == null) {
            return false;
        }
        String[] args = new String[]{Long.toString(post.blogId), Long.toString(post.postId)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(),
                                     "SELECT is_followed FROM tbl_posts WHERE blog_id=? AND post_id=?",
                                     args);
    }

    public static int deletePostsWithTag(final ReaderTag tag) {
        if (tag == null) {
            return 0;
        }

        String[] args = {tag.getTagSlug(), Integer.toString(tag.tagType.toInt())};
        int rowsDeleted = ReaderDatabase.getWritableDb().delete(
                "tbl_posts",
                "tag_name=? AND tag_type=?",
                args);

        if (rowsDeleted > 0) {
            EventBus.getDefault().post(ReaderPostTableActionEnded.INSTANCE);
        }
        return rowsDeleted;
    }

    public static int removeTagsFromPost(long blogId, long postId, final ReaderTagType tagType) {
        if (tagType == null) {
            return 0;
        }

        String[] args = {Integer.toString(tagType.toInt()), Long.toString(blogId), Long.toString(postId)};
        int rowsDeleted = ReaderDatabase.getWritableDb().delete(
                "tbl_posts",
                "tag_type=? AND blog_id=? AND post_id=?",
                args);

        if (rowsDeleted > 0) {
            EventBus.getDefault().post(ReaderPostTableActionEnded.INSTANCE);
        }
        return rowsDeleted;
    }

    public static int deletePostsInBlog(long blogId) {
        String[] args = {Long.toString(blogId)};
        int rowsDeleted = ReaderDatabase.getWritableDb().delete("tbl_posts", "blog_id = ?", args);
        if (rowsDeleted > 0) {
            EventBus.getDefault().post(ReaderPostTableActionEnded.INSTANCE);
        }
        return rowsDeleted;
    }

    public static void deletePost(long blogId, long postId) {
        String[] args = new String[]{Long.toString(blogId), Long.toString(postId)};
        ReaderDatabase.getWritableDb().delete("tbl_posts", "blog_id=? AND post_id=?", args);
        EventBus.getDefault().post(ReaderPostTableActionEnded.INSTANCE);
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
                AppLog.d(AppLog.T.READER, String.format(Locale.ENGLISH,
                        "reader post table > marked %d posts unfollowed", count));
                EventBus.getDefault().post(ReaderPostTableActionEnded.INSTANCE);
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
                     + " WHERE blog_id=? AND tag_name=''"
                     + " ORDER BY date_published LIMIT 1";
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), sql, new String[]{Long.toString(blogId)});
    }

    public static String getOldestPubDateInFeed(long feedId) {
        String sql = "SELECT date_published FROM tbl_posts"
                     + " WHERE feed_id=? AND tag_name=''"
                     + " ORDER BY date_published LIMIT 1";
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), sql, new String[]{Long.toString(feedId)});
    }

    public static void removeGapMarkerForTag(final ReaderTag tag) {
        if (tag == null) {
            return;
        }

        String[] args = {tag.getTagSlug(), Integer.toString(tag.tagType.toInt())};
        String sql = "UPDATE tbl_posts SET has_gap_marker=0 WHERE has_gap_marker!=0 AND tag_name=? AND tag_type=?";
        ReaderDatabase.getWritableDb().execSQL(sql, args);
        EventBus.getDefault().post(ReaderPostTableActionEnded.INSTANCE);
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
        if (tag == null) {
            return;
        }

        String[] args = {
                Long.toString(blogId),
                Long.toString(postId),
                tag.getTagSlug(),
                Integer.toString(tag.tagType.toInt())
        };
        String sql =
                "UPDATE tbl_posts SET has_gap_marker=1 WHERE blog_id=? AND post_id=? AND tag_name=? AND tag_type=?";
        ReaderDatabase.getWritableDb().execSQL(sql, args);
        EventBus.getDefault().post(ReaderPostTableActionEnded.INSTANCE);
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
     * liked posts sort by the date the post was liked
     * followed posts sort by the date the post was published
     * search results sort by score
     * tagged posts sort by the date the post was tagged
     */
    private static String getSortColumnForTag(ReaderTag tag) {
        if (tag.isPostsILike()) {
            return "date_liked";
        } else if (tag.isFollowedSites()) {
            return "date_published";
        } else if (tag.tagType == ReaderTagType.SEARCH) {
            return "score";
        } else if (tag.isTagTopic() || tag.isBookmarked()) {
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
        if (TextUtils.isEmpty(gapMarkerDate)) {
            return;
        }

        String dateColumn = getSortColumnForTag(tag);
        String[] args = {tag.getTagSlug(), Integer.toString(tag.tagType.toInt()), gapMarkerDate};
        String where = "tag_name=? AND tag_type=? AND " + dateColumn + " < ?";
        int numDeleted = ReaderDatabase.getWritableDb().delete("tbl_posts", where, args);
        if (numDeleted > 0) {
            AppLog.d(AppLog.T.READER, "removed " + numDeleted + " posts older than gap marker");
            EventBus.getDefault().post(ReaderPostTableActionEnded.INSTANCE);
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
            EventBus.getDefault().post(ReaderPostTableActionEnded.INSTANCE);
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Android's CursorWindow has a max size of 2MB per row which can be exceeded
     * with a very large text column, causing an IllegalStateException when the
     * row is read - prevent this by limiting the amount of text that's stored in
     * the text column - note that this situation very rarely occurs
     * http://bit.ly/2Fs7B78
     * http://bit.ly/2oOKCJc
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
                                   post.getUrl(),
                                   WordPress.getContext().getString(R.string.reader_label_view_original));
        } else {
            AppLog.w(AppLog.T.READER, "reader post table > max text exceeded, storing truncated text");
            return post.getText().substring(0, MAX_TEXT_LEN);
        }
    }

    public static void addOrUpdatePosts(final ReaderTag tag, ReaderPostList posts) {
        if (posts == null || posts.size() == 0) {
            return;
        }

        updateIsBookmarkedField(posts);

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        SQLiteStatement stmtPosts = db.compileStatement(
                "INSERT OR REPLACE INTO tbl_posts ("
                + COLUMN_NAMES
                + ") VALUES (?1,?2,?3,?4,?5,?6,?7,?8,?9,?10,?11,?12,?13,?14,?15,?16,?17,?18,?19,?20,?21,?22,?23,?24,"
                + "?25,?26,?27,?28,?29,?30,?31,?32,?33,?34,?35,?36,?37,?38,?39,?40,?41,?42,?43,?44, ?45, ?46, ?47,"
                + "?48)");

        db.beginTransaction();
        try {
            String tagName = (tag != null ? tag.getTagSlug() : "");
            int tagType = (tag != null ? tag.tagType.toInt() : 0);

            ReaderBlogIdPostId postWithGapMarker = getGapMarkerIdsForTag(tag);

            for (ReaderPost post : posts) {
                // keep the gapMarker flag
                boolean hasGapMarker = postWithGapMarker != null && postWithGapMarker.getPostId() == post.postId
                                       && postWithGapMarker.getBlogId() == post.blogId;
                stmtPosts.bindLong(1, post.postId);
                stmtPosts.bindLong(2, post.blogId);
                stmtPosts.bindLong(3, post.feedId);
                stmtPosts.bindLong(4, post.feedItemId);
                stmtPosts.bindString(5, post.getPseudoId());
                stmtPosts.bindString(6, post.getAuthorName());
                stmtPosts.bindString(7, post.getAuthorFirstName());
                stmtPosts.bindLong(8, post.authorId);
                stmtPosts.bindString(9, post.getTitle());
                stmtPosts.bindString(10, maxText(post));
                stmtPosts.bindString(11, post.getExcerpt());
                stmtPosts.bindString(12, post.getFormat());
                stmtPosts.bindString(13, post.getUrl());
                stmtPosts.bindString(14, post.getShortUrl());
                stmtPosts.bindString(15, post.getBlogName());
                stmtPosts.bindString(16, post.getBlogUrl());
                stmtPosts.bindString(17, post.getBlogImageUrl());
                stmtPosts.bindString(18, post.getFeaturedImage());
                stmtPosts.bindString(19, post.getFeaturedVideo());
                stmtPosts.bindString(20, post.getPostAvatar());
                stmtPosts.bindDouble(21, post.score);
                stmtPosts.bindString(22, post.getDatePublished());
                stmtPosts.bindString(23, post.getDateLiked());
                stmtPosts.bindString(24, post.getDateTagged());
                stmtPosts.bindLong(25, post.numReplies);
                stmtPosts.bindLong(26, post.numLikes);
                stmtPosts.bindLong(27, SqlUtils.boolToSql(post.isLikedByCurrentUser));
                stmtPosts.bindLong(28, SqlUtils.boolToSql(post.isFollowedByCurrentUser));
                stmtPosts.bindLong(29, SqlUtils.boolToSql(post.isCommentsOpen));
                stmtPosts.bindLong(30, SqlUtils.boolToSql(post.isExternal));
                stmtPosts.bindLong(31, SqlUtils.boolToSql(post.isPrivate));
                stmtPosts.bindLong(32, SqlUtils.boolToSql(post.isVideoPress));
                stmtPosts.bindLong(33, SqlUtils.boolToSql(post.isJetpack));
                stmtPosts.bindString(34, post.getPrimaryTag());
                stmtPosts.bindString(35, post.getSecondaryTag());
                stmtPosts.bindString(36, post.getAttachmentsJson());
                stmtPosts.bindString(37, post.getDiscoverJson());
                stmtPosts.bindLong(38, post.xpostPostId);
                stmtPosts.bindLong(39, post.xpostBlogId);
                stmtPosts.bindString(40, post.getRailcarJson());
                stmtPosts.bindString(41, tagName);
                stmtPosts.bindLong(42, tagType);
                stmtPosts.bindLong(43, SqlUtils.boolToSql(hasGapMarker));
                stmtPosts.bindString(44, ReaderCardType.toString(post.getCardType()));
                stmtPosts.bindLong(45, SqlUtils.boolToSql(post.useExcerpt));
                stmtPosts.bindLong(46, SqlUtils.boolToSql(post.isBookmarked));
                stmtPosts.bindLong(47, SqlUtils.boolToSql(post.isPrivateAtomic));
                stmtPosts.bindString(48, ReaderUtils.getCommaSeparatedTagSlugs(post.getTags()));
                stmtPosts.execute();
            }

            db.setTransactionSuccessful();
            EventBus.getDefault().post(ReaderPostTableActionEnded.INSTANCE);
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
        String sql =
                "SELECT " + columns + " FROM tbl_posts WHERE blog_id=? AND tag_name='' ORDER BY date_published DESC";

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
        String sql =
                "SELECT " + columns + " FROM tbl_posts WHERE feed_id=? AND tag_name='' ORDER BY date_published DESC";

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
        if (tag == null) {
            return new ReaderBlogIdPostIdList();
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
        return getBlogIdPostIds(sql, args);
    }

    private static ReaderBlogIdPostIdList getBlogIdPostIdsWithTagType(ReaderTagType tagType, int maxPosts) {
        if (tagType == null) {
            return new ReaderBlogIdPostIdList();
        }

        String sql = "SELECT blog_id, post_id FROM tbl_posts WHERE tag_type=?";

        if (maxPosts > 0) {
            sql += " LIMIT " + Integer.toString(maxPosts);
        }

        String[] args = {Integer.toString(tagType.toInt())};
        return getBlogIdPostIds(sql, args);
    }

    private static ReaderBlogIdPostIdList getBlogIdPostIds(@NonNull String sql, @NonNull String[] args) {
        ReaderBlogIdPostIdList idList = new ReaderBlogIdPostIdList();
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
        String sql = "SELECT post_id FROM tbl_posts WHERE blog_id=? AND tag_name='' ORDER BY date_published DESC";

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
        post.setBlogImageUrl(c.getString(c.getColumnIndex("blog_image_url")));
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
        post.isFollowedByCurrentUser = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_followed")));
        post.isCommentsOpen = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_comments_open")));
        post.isExternal = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_external")));
        post.isPrivate = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_private")));
        post.isPrivateAtomic = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_private_atomic")));
        post.isVideoPress = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_videopress")));
        post.isJetpack = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_jetpack")));
        post.isBookmarked = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_bookmarked")));

        post.setPrimaryTag(c.getString(c.getColumnIndex("primary_tag")));
        post.setSecondaryTag(c.getString(c.getColumnIndex("secondary_tag")));

        post.setAttachmentsJson(c.getString(c.getColumnIndex("attachments_json")));
        post.setDiscoverJson(c.getString(c.getColumnIndex("discover_json")));

        post.xpostPostId = c.getLong(c.getColumnIndex("xpost_post_id"));
        post.xpostBlogId = c.getLong(c.getColumnIndex("xpost_blog_id"));

        post.setRailcarJson(c.getString(c.getColumnIndex("railcar_json")));
        post.setCardType(ReaderCardType.fromString(c.getString(c.getColumnIndex("card_type"))));

        post.useExcerpt = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("use_excerpt")));

        String commaSeparatedTags = (c.getString(c.getColumnIndex("tags")));
        if (commaSeparatedTags != null) {
            post.setTags(ReaderUtils.getTagsFromCommaSeparatedSlugs(commaSeparatedTags));
        }

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
            AppLog.e(AppLog.T.READER, e);
        }
        return posts;
    }

    /**
     * Currently "is_bookmarked" field is not supported by the server, therefore posts from the server have always
     * is_bookmarked set to false. This method is a workaround which makes sure, that the field is always up to date
     * and synced across all instances(rows) of each post.
     */
    private static void updateIsBookmarkedField(final ReaderPostList posts) {
        ReaderBlogIdPostIdList bookmarkedPosts = getBookmarkedPostIds();
        for (ReaderPost post : posts) {
            for (ReaderBlogIdPostId bookmarkedPostId : bookmarkedPosts) {
                if (post.blogId == bookmarkedPostId.getBlogId() && post.postId == bookmarkedPostId.getPostId()) {
                    post.isBookmarked = true;
                }
            }
        }
    }

    private static ReaderBlogIdPostIdList getBookmarkedPostIds() {
        return getBlogIdPostIdsWithTagType(ReaderTagType.BOOKMARKED, 99999);
    }
}
