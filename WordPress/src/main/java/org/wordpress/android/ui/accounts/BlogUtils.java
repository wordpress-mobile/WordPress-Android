package org.wordpress.android.ui.accounts;

import android.content.Context;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlogUtils {
    private static final String DEFAULT_IMAGE_SIZE = "2000";

    /**
     * Remove blogs that are not in the list and add others
     * TODO: it's horribly slow due to datastructures used (List of Map), We should replace
     * that by a HashSet of a specialized Blog class (that supports comparison)
     *
     * @return true if a change has been made (new blog added, old blog updated, blog deleted).
     */
    public static boolean syncBlogs(Context context, List<Map<String, Object>> newBlogList, String username) {
        boolean retValue;

        // Add all blogs from blogList
        retValue = addBlogs(newBlogList, username, null, null, null);

        // Delete blogs if not in bloegList
        List<Map<String, Object>> allBlogs = WordPress.wpDB.getAccountsBy("dotcomFlag=1", null);
        Set<String> newBlogURLs = new HashSet<String>();
        for (Map<String, Object> blog : newBlogList) {
            newBlogURLs.add(blog.get("xmlrpc").toString() + blog.get("blogid").toString());
        }
        for (Map<String, Object> blog : allBlogs) {
            if (!newBlogURLs.contains(blog.get("url").toString() + blog.get("blogId"))) {
                WordPress.wpDB.deleteAccount(context, Integer.parseInt(blog.get("id").toString()));
                retValue = true;
            }
        }
        return retValue;
    }

    /**
     * Add selected blog(s) to the database.
     *
     * @return true if a change has been made (new blog added or old blog updated).
     */
    public static boolean addBlogs(List<Map<String, Object>> blogList, String username, String password,
                                   String httpUsername, String httpPassword) {
        boolean retValue = false;
        for (Map<String, Object> blogMap : blogList) {
            String blogName = StringUtils.unescapeHTML(blogMap.get("blogName").toString());
            String xmlrpc = blogMap.get("xmlrpc").toString();
            String homeUrl = blogMap.get("url").toString();
            String blogId = blogMap.get("blogid").toString();
            boolean isVisible = true;
            if (blogMap.containsKey("isVisible")) {
                isVisible = MapUtils.getMapBool(blogMap, "isVisible");
            }
            boolean isAdmin = MapUtils.getMapBool(blogMap, "isAdmin");
            retValue |= addOrUpdateBlog(blogName, xmlrpc, homeUrl, blogId, username, password, httpUsername,
                    httpPassword, isAdmin, isVisible);
        }
        return retValue;
    }

    /**
     * Check xmlrpc urls validity
     *
     * @param blogList blog list
     * @param xmlrpcUrl forced xmlrpc url
     * @return true if there is at least one invalid xmlrpc url
     */
    public static boolean isAnyInvalidXmlrpcUrl(List<Map<String, Object>> blogList) {
        for (Map<String, Object> blogMap : blogList) {
            String xmlrpc = blogMap.get("xmlrpc").toString();
            if (!UrlUtils.isValidUrlAndHostNotNull(xmlrpc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a new blog or update a blog name in local DB.
     *
     * @return true if a new blog has been added or an old blog has been updated.
     * Return false if no change has been made.
     */
    public static boolean addOrUpdateBlog(String blogName, String xmlRpcUrl, String homeUrl, String blogId,
                                           String username, String password, String httpUsername, String httpPassword,
                                           boolean isAdmin, boolean isVisible) {
        Blog blog;
        if (!WordPress.wpDB.isBlogInDatabase(Integer.parseInt(blogId), xmlRpcUrl)) {
            // The blog isn't in the app, so let's create it
            blog = new Blog(xmlRpcUrl, username, password);
            blog.setHomeURL(homeUrl);
            blog.setHttpuser(httpUsername);
            blog.setHttppassword(httpPassword);
            blog.setBlogName(blogName);
            // deprecated
            blog.setImagePlacement("");
            blog.setFullSizeImage(false);
            blog.setMaxImageWidth(DEFAULT_IMAGE_SIZE);
            // deprecated
            blog.setMaxImageWidthId(0);
            blog.setRemoteBlogId(Integer.parseInt(blogId));
            blog.setDotcomFlag(xmlRpcUrl.contains("wordpress.com"));
            // assigned later in getOptions call
            blog.setWpVersion("");
            blog.setAdmin(isAdmin);
            blog.setHidden(!isVisible);
            WordPress.wpDB.saveBlog(blog);
            return true;
        } else {
            // Update blog name
            int localTableBlogId = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogIdAndXmlRpcUrl(
                    Integer.parseInt(blogId), xmlRpcUrl);
            try {
                blog = WordPress.wpDB.instantiateBlogByLocalId(localTableBlogId);
                if (!blogName.equals(blog.getBlogName())) {
                    blog.setBlogName(blogName);
                    WordPress.wpDB.saveBlog(blog);
                    return true;
                }
            } catch (Exception e) {
                AppLog.e(T.NUX, "localTableBlogId: " + localTableBlogId + " not found");
            }
            return false;
        }
    }

    public static void addBlogs(List<Map<String, Object>> userBlogList, String username) {
        addBlogs(userBlogList, username, null, null, null);
    }
}
