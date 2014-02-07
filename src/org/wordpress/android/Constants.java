
package org.wordpress.android;

public class Constants {

    public static String readerURL = "https://en.wordpress.com/reader/mobile/v2";
    public static String readerLoginURL = "https://wordpress.com/wp-login.php";

    public static String readerURL_v3 = "https://en.wordpress.com/reader/mobile/v2/?chrome=no";
    public static String authorizedHybridHost = "en.wordpress.com";
    public static String readerTopicsURL = "http://en.wordpress.com/reader/mobile/v2/?template=topics";
    public static String readerDetailURL = "https://en.wordpress.com/wp-admin/admin-ajax.php?action=wpcom_load_mobile&template=details&v=2";
    
    public static String wpcomXMLRPCURL = "https://wordpress.com/xmlrpc.php";
    public static String wpcomLoginURL = "https://wordpress.com/wp-login.php";

    public static final String URL_TOS = "http://en.wordpress.com/tos";
    public static String videoPressURL = "http://videopress.com";

    public static int QUICK_POST_PHOTO_CAMERA = 0;
    public static int QUICK_POST_PHOTO_LIBRARY = 1;
    public static int QUICK_POST_VIDEO_CAMERA = 2;
    public static int QUICK_POST_VIDEO_LIBRARY = 3;

    /**
     * User-Agent string used when making HTTP connections. This is used both for API traffic as
     * well as embedded WebViews.
     */
    public static final String USER_AGENT = "wp-android";

    /*
     * Reader constants
     */
    public static final int READER_MAX_POSTS_TO_REQUEST    = 20;                          // max #posts to request when updating posts (should be an even # to avoid "hanging post" in 2-column grid mode)
    public static final int READER_MAX_POSTS_TO_DISPLAY    = 200;                         // max #posts to display in ReaderPostListFragment
    public static final int READER_MAX_COMMENTS_TO_REQUEST = READER_MAX_POSTS_TO_REQUEST; // max #comments to request when updating comments
    public static final int READER_MAX_USERS_TO_DISPLAY    = 500;                         // max #users to show in ReaderUserListActivity
    public static final long READER_AUTO_UPDATE_DELAY_MINUTES = 15;                       // 15 minute delay between automatic updates

    // intent IDs
    public static final int INTENT_READER_POST_DETAIL = 1000;
    public static final int INTENT_READER_TAGS        = 1001;
    public static final int INTENT_READER_REBLOG      = 1002;
    public static final int INTENT_COMMENT_EDITOR     = 1010;

}
