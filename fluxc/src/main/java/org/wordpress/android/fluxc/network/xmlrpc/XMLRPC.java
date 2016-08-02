package org.wordpress.android.fluxc.network.xmlrpc;

public enum XMLRPC {
    GET_OPTIONS("wp.getOptions"),

    GET_USERS_BLOGS("wp.getUsersBlogs"),
    LIST_METHODS("system.listMethods"),

    // TODO: Switch from metaWeblog to wp where feasible
    DELETE_PAGE("wp.deletePage"),
    DELETE_POST("wp.deletePost"),
    EDIT_POST("metaWeblog.editPost"),
    GET_PAGE("wp.getPage"),
    GET_PAGES("wp.getPages"),
    GET_POST("metaWeblog.getPost"),
    GET_POSTS("metaWeblog.getRecentPosts"),
    NEW_POST("metaWeblog.newPost");

    private final String mEndpoint;

    XMLRPC(String endpoint) {
        mEndpoint = endpoint;
    }

    @Override
    public String toString() {
        return mEndpoint;
    }
}
