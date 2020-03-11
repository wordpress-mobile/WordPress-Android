package org.wordpress.android.ui.publicize;

public class PublicizeConstants {
    public static final String ARG_SERVICE_ID = "service_id";
    public static final String ARG_CONNECTION_ID = "connection_id";
    public static final String ARG_CONNECTION_ARRAY_JSON = "connection_array_json";

    public static final String GOOGLE_PLUS_ID = "google_plus";
    public static final String FACEBOOK_ID = "facebook";
    public static final String TWITTER_ID = "twitter";
    public static final String LINKEDIN_ID = "linkedin";

    public static final String FACEBOOK_SHARING_CHANGE_BLOG_POST =
            "https://en.blog.wordpress.com/2018/07/23/sharing-options-from-wordpress-com-to-facebook-are-changing/";

    public enum ConnectAction {
        CONNECT,
        DISCONNECT,
        RECONNECT,
        CONNECT_ANOTHER_ACCOUNT
    }
}
