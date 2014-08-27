package org.wordpress.android.datasets;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import org.wordpress.android.models.ReaderAttachment;
import org.wordpress.android.models.ReaderAttachmentList;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.util.SqlUtils;

/**
 * stores attachments for reader posts
 */
public class ReaderAttachmentTable {
    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_attachments ("
                + "	post_id		        INTEGER,"
                + " blog_id             INTEGER,"
                + " attachment_id       INTEGER,"
                + " url                 TEXT NOT NULL,"
                + " mime_type           TEXT NOT NULL,"
                + " width               INTEGER DEFAULT 0,"
                + " height              INTEGER DEFAULT 0,"
                + " PRIMARY KEY (post_id, blog_id, attachment_id)"
                + ")");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_attachments");
    }

    /*
     * purge table of attachments to posts that no longer exist
     */
    protected static int purge(SQLiteDatabase db) {
        return db.delete("tbl_attachments", "post_id NOT IN (SELECT DISTINCT post_id FROM tbl_posts)", null);
    }

    public static ReaderAttachmentList getAttachmentsForPost(long blogId, long postId) {
        String[] args = {Long.toString(blogId), Long.toString(postId)};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_attachments WHERE blog_id=? AND post_id=?", args);
        try {
            ReaderAttachmentList attachments = new ReaderAttachmentList();
            if (c.moveToFirst()) {
                do {
                    attachments.add(getAttachmentFromCursor(c));
                } while (c.moveToNext());
            }
            return attachments;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static void saveAttachmentsForPosts(ReaderPostList posts) {
        if (posts == null || posts.size() == 0) {
            return;
        }

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        SQLiteStatement stmt = db.compileStatement(
                "INSERT INTO tbl_attachments"
              + " (blog_id, post_id, attachment_id, url, mime_type, width, height)"
              + " VALUES (?1,?2,?3,?4,?5,?6,?7)");
        try {
            for (ReaderPost post : posts) {
                if (post.hasAttachments()) {
                    // first delete all attachments for this post
                    String[] args = {Long.toString(post.blogId), Long.toString(post.postId)};
                    db.delete("tbl_attachments", "blog_id=? AND post_id=?", args);

                    // now insert the passed ones
                    stmt.bindLong(1, post.blogId);
                    stmt.bindLong(2, post.postId);
                    for (ReaderAttachment attach : post.getAttachments()) {
                        stmt.bindLong  (3, attach.attachmentId);
                        stmt.bindString(4, attach.getUrl());
                        stmt.bindString(5, attach.getMimeType());
                        stmt.bindLong  (6, attach.width);
                        stmt.bindLong  (7, attach.height);
                        stmt.execute();
                    }
                }
            }
        } finally {
            SqlUtils.closeStatement(stmt);
        }
    }

    private static ReaderAttachment getAttachmentFromCursor(Cursor c) {
        ReaderAttachment attach = new ReaderAttachment();

        attach.blogId = c.getLong(c.getColumnIndex("blog_id"));
        attach.postId = c.getLong(c.getColumnIndex("post_id"));
        attach.attachmentId = c.getLong(c.getColumnIndex("attachment_id"));

        attach.setUrl(c.getString(c.getColumnIndex("url")));
        attach.setMimeType(c.getString(c.getColumnIndex("mime_type")));

        attach.height = c.getInt(c.getColumnIndex("height"));
        attach.width = c.getInt(c.getColumnIndex("width"));

        return attach;
    }
}
