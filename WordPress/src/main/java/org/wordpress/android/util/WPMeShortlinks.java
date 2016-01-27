package org.wordpress.android.util;

/**
 * Enable WP.me-powered shortlinks for Posts, Pages, and Blogs on WordPress.com or Jetpack powered sites.
 * <p/>
 * Shortlinks are a quick way to get short and simple links to your posts, pages, and blogs.
 * They use the wp.me domain so you can have more space to write on social media sites.
 * <p/>
 * See: https://github.com/Automattic/jetpack/blob/master/modules/shortlinks.php
 */

import android.text.TextUtils;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.util.AppLog.T;

public class WPMeShortlinks {
    /**
     * Converts a base-10 number to base-62
     *
     * @param num base-10 number
     * @return String base-62 number
     */
    public static String wpme_dec2sixtwo(double num) {
        if (num == 0) {
            return "0";
        }

        StringBuilder out;
        try {
            String index = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
            out = new StringBuilder();

            if (num < 0) {
                out.append('-');
                num = Math.abs(num);
            }

            double t = Math.floor(Math.log10(num) / Math.log10(62));
            for (; t >= 0; t--) {
                int a = (int) Math.floor(num / Math.pow(62, t));
                out.append(index.substring(a, a + 1));
                num = num - (a * Math.pow(62, t));
            }
            return out.toString();
        } catch (IndexOutOfBoundsException e) {
            AppLog.e(T.UTILS, "Connot convert number " + num + " to base 62", e);
        }
        return null;
    }

    /**
     * Returns The post shortlink
     *
     * @param blog Blog that contains the post or the page
     * @param post Post or page we want calculate the shortlink
     * @return String The blog shortlink or null (null is returned if the blog object is empty, or it's not a wpcom/jetpack blog, or in case of errors).
     */
    public static String getPostShortlink(Blog blog, Post post) {
        if (post == null || blog == null) {
            return null;
        }

        if (!blog.isDotcomFlag() && !blog.isJetpackPowered()) {
            return null;
        }

        String postID = post.getRemotePostId();
        if (postID == null) {
            return null;
        }

        String id = null;
        String type = null;

        String postName = StringUtils.notNullStr(post.getSlug());
        if (post.getStatusEnum() == PostStatus.PUBLISHED && postName.length() > 0 && postName.length() <= 8 &&
                !postName.contains("%") && !postName.contains("-")) {
            id = postName;
            type = "s";
        } else {
            try {
                id = wpme_dec2sixtwo(Double.parseDouble(postID));
            } catch (NumberFormatException e) {
                AppLog.e(T.UTILS, "Remote postID cannot be converted to double" + postID, e);
                return null;
            }

            if (post.isPage()) {
                type = "P";
            } else {
                type = "p";
            }
        }

        // Calculate the blog shortlink
        String blogShortlink = null;
        try {
            double blogID = blog.isDotcomFlag() ? blog.getRemoteBlogId() : Double.parseDouble(blog.getApi_blogid());
            blogShortlink = wpme_dec2sixtwo(blogID);
        } catch (NumberFormatException e) {
            AppLog.e(T.UTILS, "Remote Blog ID cannot be converted to double", e);
            return null;
        }

        if (TextUtils.isEmpty(type) || TextUtils.isEmpty(id) || TextUtils.isEmpty(blogShortlink)) {
            return null;
        }

        return "http://wp.me/" + type + blogShortlink + "-" + id;
    }

    public static String getPostShortlink(Post post) {
        Blog blog = WordPress.wpDB.instantiateBlogByLocalId(post.getLocalTableBlogId());
        return getPostShortlink(blog, post);
    }

    /**
     * Returns The blog shortlink
     *
     * @param blog Blog we want calculate the shortlink
     * @return String The blog shortlink or null (null is returned if the blog object is empty, or it's not a wpcom/jetpack blog, or in case of errors).
     */
    public static String getBlogShortlink(Blog blog) {
        if (blog == null) {
            return null;
        }

        if (!blog.isDotcomFlag() && !blog.isJetpackPowered()) {
            return null;
        }

        try {
            double blogID = blog.isDotcomFlag() ? blog.getRemoteBlogId() : Double.parseDouble(blog.getApi_blogid());
            String shortlink = wpme_dec2sixtwo(blogID);
            String shortlinkWithProtocol = (shortlink == null) ? blog.getHomeURL() : "http://wp.me/" + shortlink;
            return shortlinkWithProtocol;
        } catch (NumberFormatException e) {
            AppLog.e(T.UTILS, "Remote Blog ID cannot be converted to double ", e);
            return blog.getHomeURL();
        }
    }
}
