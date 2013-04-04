package com.wordpress.rest;

import org.json.JSONObject;
import org.json.JSONException;

public class OauthToken {
    
    private static final String TOKEN_TYPE_FIELD_NAME="token_type";
    private static final String ACCESS_TOKEN_FIELD_NAME="access_token";
    private static final String BLOG_URL_FIELD_NAME="blog_url";
    private static final String SCOPE_FIELD_NAME="scope";
    private static final String BLOG_ID_FIELD_NAME="blog_id";
    
    private String mTokenType;
    private String mScope;
    private String mAccessToken;
    private String mBlogUrl;
    private String mBlogId;
    
    public OauthToken(String accessToken, String blogUrl, String blogId, String scope, String tokenType){
        mAccessToken = accessToken;
        mBlogUrl = blogUrl;
        mBlogId = blogId;
        mScope = scope;
        mTokenType = tokenType;
    }
    
    public String getAccessToken(){
        return mAccessToken;
    }
    
    public String toString(){
        return getAccessToken();
    }
    
    public static OauthToken fromJSONObject(JSONObject tokenJSON)
    throws JSONException {
        return new OauthToken(
            tokenJSON.getString(ACCESS_TOKEN_FIELD_NAME),
            tokenJSON.getString(BLOG_URL_FIELD_NAME),
            tokenJSON.getString(BLOG_ID_FIELD_NAME),
            tokenJSON.getString(SCOPE_FIELD_NAME),
            tokenJSON.getString(TOKEN_TYPE_FIELD_NAME)
        );
    }
}
