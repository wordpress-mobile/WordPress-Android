package com.wordpress.rest;

import org.json.JSONObject;

public class OauthTokenResponseHandler {
    public void onStart(){}
    public void onSuccess(OauthToken token){}
    public void onFailure(Throwable e, JSONObject respose){}
    public void onFinish(){}
}
