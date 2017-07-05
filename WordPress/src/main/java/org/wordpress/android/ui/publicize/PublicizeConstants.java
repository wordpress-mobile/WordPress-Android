package org.wordpress.android.ui.publicize;

public class PublicizeConstants {
    public static final String ARG_SERVICE_ID     = "service_id";
    public static final String ARG_CONNECTION_ID  = "connection_id";
    public static final String ARG_CONNECTION_ARRAY_JSON = "connection_array_json";

    // G+ no longers supports authentication via a WebView, so we hide it here unless the
    // user already has a connection - and if they do, we hide the ability to connect
    // another G+ account
    public static final String GOOGLE_PLUS_ID = "google_plus";

    public enum ConnectAction {
        CONNECT,
        DISCONNECT,
        RECONNECT,
        CONNECT_ANOTHER_ACCOUNT
    }
}
