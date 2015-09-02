package org.wordpress.android.ui.posts;

public class PostUtils {

    public static final String SHORTCODE_GALLERY = "gallery";

    /*
     * collapses the passed shortcode in the passed post content, stripping anything between the
     * shortcode name and the closing brace.
     * ex: collapseShortcode("gallery", "[gallery ids="1206,1205,1191"]") -> "[gallery]"
     */
    public static String collapseShortcode(String shortCode, String postContent) {
        // speed things up by skipping regex if content doesn't contain a brace
        if (postContent == null || !postContent.contains("[")) {
            return postContent;
        }

        String regex = "\\[" + shortCode + "(.+?)\\]";
        String replacement = "\\[" + shortCode + "\\]";
        return postContent.replaceAll(regex, replacement);
    }

}
