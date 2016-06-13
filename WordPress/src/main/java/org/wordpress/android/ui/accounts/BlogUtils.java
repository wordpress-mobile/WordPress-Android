package org.wordpress.android.ui.accounts;

import android.content.Context;
import android.text.Editable;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.stats.datasets.StatsTable;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPUrlUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlogUtils {
    private static final String DEFAULT_IMAGE_SIZE = "2000";

    public static final int BLOG_ID_INVALID = 0;

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
        retValue = addBlogs(newBlogList, username);

        // Delete blogs if not in blogList
        List<Map<String, Object>> allBlogs = WordPress.wpDB.getBlogsBy("dotcomFlag=1", null);
        Set<String> newBlogURLs = new HashSet<String>();
        for (Map<String, Object> blog : newBlogList) {
            newBlogURLs.add(blog.get("xmlrpc").toString() + blog.get("blogid").toString());
        }
        for (Map<String, Object> blog : allBlogs) {
            if (!newBlogURLs.contains(blog.get("url").toString() + blog.get("blogId"))) {
                WordPress.wpDB.deleteBlog(context, Integer.parseInt(blog.get("id").toString()));
                StatsTable.deleteStatsForBlog(context, Integer.parseInt(blog.get("id").toString())); // Remove stats data
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
            long planID = 0;
            if (blogMap.containsKey("planID")) {
                planID = MapUtils.getMapLong(blogMap, "planID");
            }
            String planShortName = MapUtils.getMapStr(blogMap, "plan_product_name_short");
            String capabilities = MapUtils.getMapStr(blogMap, "capabilities");

            retValue |= addOrUpdateBlog(blogName, xmlrpc, homeUrl, blogId, username, password, httpUsername,
                    httpPassword, isAdmin, isVisible, planID, planShortName, capabilities);
        }
        return retValue;
    }

    /**
     * Check xmlrpc urls validity
     *
     * @param blogList blog list
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
                                          boolean isAdmin, boolean isVisible,
                                          long planID, String planShortName, String capabilities) {
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
            blog.setDotcomFlag(WPUrlUtils.isWordPressCom(xmlRpcUrl));
            // assigned later in getOptions call
            blog.setWpVersion("");
            blog.setAdmin(isAdmin);
            blog.setHidden(!isVisible);
            blog.setPlanID(planID);
            blog.setPlanShortName(planShortName);
            blog.setCapabilities(capabilities);
            WordPress.wpDB.saveBlog(blog);
            return true;
        } else {
            // Update blog name and/or PlanID/PlanShortName
            int localTableBlogId = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogIdAndXmlRpcUrl(
                    Integer.parseInt(blogId), xmlRpcUrl);
            try {
                boolean blogUpdated = false;
                blog = WordPress.wpDB.instantiateBlogByLocalId(localTableBlogId);
                if (!blogName.equals(blog.getBlogName())) {
                    blog.setBlogName(blogName);
                    blogUpdated = true;
                }
                if (planID != blog.getPlanID()) {
                    blog.setPlanID(planID);
                    blogUpdated = true;
                }
                if (!blog.getPlanShortName().equals(planShortName)) {
                    blog.setPlanShortName(planShortName);
                    blogUpdated = true;
                }
                if (blog.getCapabilities() == null || !blog.getCapabilities().equals(capabilities)) {
                    blog.setCapabilities(capabilities);
                    blogUpdated = true;
                }
                if (blogUpdated) {
                    WordPress.wpDB.saveBlog(blog);
                    return true;
                }
            } catch (Exception e) {
                AppLog.e(T.NUX, "localTableBlogId: " + localTableBlogId + " not found");
            }
            return false;
        }
    }

    public static boolean addBlogs(List<Map<String, Object>> userBlogList, String username) {
        return addBlogs(userBlogList, username, null, null, null);
    }

    /**
     * Get a Blog's local Id.
     *
     * @param blog The Blog to get its local ID
     * @return Blog's local id or {@value BlogUtils#BLOG_ID_INVALID} if null
     */
    public static int getBlogLocalId(final Blog blog) {
        return (blog != null ? blog.getLocalTableBlogId() : BLOG_ID_INVALID);
    }

    public static void convertToLowercase(Editable s) {
        String lowerCase = s.toString().toLowerCase();
        if (!lowerCase.equals(s.toString())) {
            s.replace(0, s.length(), lowerCase);
        }
    }
}
