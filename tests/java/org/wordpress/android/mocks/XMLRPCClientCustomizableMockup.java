package org.wordpress.android.mocks;

import android.content.Context;

import com.google.gson.Gson;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

public class XMLRPCClientCustomizableMockup implements XMLRPCClientInterface {
    private Context mContext;
    private String mPrefix;

    public void setContext(Context context, String prefix) {
        mContext = context;
        mPrefix = prefix;
    }

    public XMLRPCClientCustomizableMockup(URI uri, String httpUser, String httpPassword) {
    }

    public void addQuickPostHeader(String type) {
    }

    public void setAuthorizationHeader(String authToken) {
    }

    public Object call(String method, Object[] params) throws XMLRPCException {
        AppLog.v(T.TESTS, "XMLRPCClientCustomizableMockup: <call(" + method + ", ...)>");
        Gson gson = new Gson();
        if ("wp.getUsersBlogs".equals(method)) {
            String filename = mPrefix + "-wp.getUsersBlogs.json";
            try {
                InputStream is = mContext.getAssets().open(filename);
                InputStreamReader inputStreamReader = new InputStreamReader(is);
                BufferedReader f = new BufferedReader(inputStreamReader);
                return gson.fromJson(inputStreamReader, Object[].class);
            } catch (IOException e) {
                AppLog.e(T.TESTS, "can't read file: " + filename);
            }
        }
        return null;
    }

    public Object call(String method) throws XMLRPCException {
        return null;
    }

    public Object call(String method, Object[] params, File tempFile) throws XMLRPCException {
        return null;
    }

    public long callAsync(XMLRPCCallback listener, String methodName, Object[] params) {
        return 0;
    }

    public long callAsync(XMLRPCCallback listener, String methodName, Object[] params, File tempFile) {
        return 0;
    }
}
