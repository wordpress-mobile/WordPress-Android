
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
    
    public static int QUICK_POST_PHOTO_CAMERA = 0;
    public static int QUICK_POST_PHOTO_LIBRARY = 1;
    public static int QUICK_POST_VIDEO_CAMERA = 2;
    public static int QUICK_POST_VIDEO_LIBRARY = 3;

    /**
     * User-Agent string used when making HTTP connections. This is used both for API traffic as
     * well as embedded WebViews.
     */
    public static final String USER_AGENT = "wp-android";
}
