package org.wordpress.android.fluxc.network.xmlrpc;

public enum XMLRPC {
    GET_OPTIONS("wp.getOptions"),
    GET_POST_FORMATS("wp.getPostFormats"),
    GET_USERS_BLOGS("wp.getUsersBlogs"),
    LIST_METHODS("system.listMethods");

    private final String mEndpoint;

    XMLRPC(String endpoint) {
        mEndpoint = endpoint;
    }

    @Override
    public String toString() {
        return mEndpoint;
    }
}
