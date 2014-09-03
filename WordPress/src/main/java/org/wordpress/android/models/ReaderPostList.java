package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostIdList;

import java.util.ArrayList;
import java.util.Date;

public class ReaderPostList extends ArrayList<ReaderPost> {
    public static ReaderPostList fromJson(JSONObject json) {
        if (json==null)
            throw new IllegalArgumentException("null json post list");

        ReaderPostList posts = new ReaderPostList();
        JSONArray jsonPosts = json.optJSONArray("posts");
        if (jsonPosts!=null) {
            for (int i=0; i < jsonPosts.length(); i++)
                posts.add(ReaderPost.fromJson(jsonPosts.optJSONObject(i)));
        }

        return posts;
    }

    private int indexOfPost(long blogId, long postId) {
        for (int i=0; i < size(); i++) {
            if (this.get(i).blogId==blogId && this.get(i).postId==postId)
                return i;
        }
        return -1;
    }

    public int indexOfPost(ReaderPost post) {
        if (post==null)
            return -1;
        return indexOfPost(post.blogId, post.postId);
    }

    /*
     * does passed list contain the same posts as this list?
     */
    public boolean isSameList(ReaderPostList posts) {
        if (posts == null || posts.size() != this.size()) {
            return false;
        }

        for (ReaderPost post: posts) {
            int index = indexOfPost(post.blogId, post.postId);
            if (index == -1) {
                return false;
            }
            ReaderPost thisPost = this.get(index);
            if (thisPost.numLikes != post.numLikes
                    || thisPost.numReplies != post.numReplies
                    || thisPost.isFollowedByCurrentUser != post.isFollowedByCurrentUser
                    || thisPost.isLikedByCurrentUser != post.isLikedByCurrentUser
                    || thisPost.isRebloggedByCurrentUser != post.isRebloggedByCurrentUser) {
                return  false;
            }
        }

        return true;
    }

    /*
     * returns the oldest pubDate of posts in this list
     */
    public Date getOldestPubDate() {
        Date oldestDate = null;
        for (ReaderPost post: this) {
            Date dtPublished = post.getDatePublished();
            if (dtPublished != null) {
                if (oldestDate == null) {
                    oldestDate = dtPublished;
                } else if (oldestDate.after(dtPublished)) {
                    oldestDate = dtPublished;
                }
            }
        }

        return oldestDate;
    }

    /*
     * return a list of blogId/postId pairs from this list of posts
     */
    public ReaderBlogIdPostIdList getBlogIdPostIdList() {
        ReaderBlogIdPostIdList ids = new ReaderBlogIdPostIdList();
        for (ReaderPost post: this) {
            ids.add(new ReaderBlogIdPostId(post.blogId, post.postId));
        }
        return ids;
    }
}
