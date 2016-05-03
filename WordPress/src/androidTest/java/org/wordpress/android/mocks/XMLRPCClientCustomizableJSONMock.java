package org.wordpress.android.mocks;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.wordpress.android.TestUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.xmlrpc.android.LoggedInputStream;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;

public class XMLRPCClientCustomizableJSONMock extends XMLRPCClientCustomizableMockAbstract {
    private LoggedInputStream mLoggedInputStream;

    public XMLRPCClientCustomizableJSONMock(URI uri, String httpUser, String httpPassword) {
    }

    public void addQuickPostHeader(String type) {
    }

    public void setAuthorizationHeader(String authToken) {
    }

    private Object readFile(String method, String prefix) {
        // method example: wp.getUsersBlogs
        // Filename: default-wp.getUsersBlogs.json
        String filename = prefix + "-" + method + ".json";
        try {
            Gson gson = new Gson();
            mLoggedInputStream = new LoggedInputStream(mContext.getAssets().open(filename));
            String jsonString = TestUtils.convertStreamToString(mLoggedInputStream);
            AppLog.i(T.TESTS, "loading: " + filename);
            try {
                // Try to load a JSONArray
                return TestUtils.injectDateInArray(gson.fromJson(jsonString, Object[].class));
            } catch (Exception e) {
                // If that fails, try to load a JSONObject
                Type type = new TypeToken<HashMap<String, Object>>(){}.getType();
                HashMap<String, Object> map = gson.fromJson(jsonString, type);
                return TestUtils.injectDateInMap(map);
            }
        } catch (IOException e) {
            AppLog.e(T.TESTS, "can't read file: " + filename);
        }
        return null;
    }

    public Object call(String method, Object[] params) throws XMLRPCException {
        mLoggedInputStream = null;
        AppLog.v(T.TESTS, "XMLRPCClientCustomizableJSONMock: call: " + method);
        if ("login-failure".equals(mPrefix)) {
            // Wrong login
            throw new XMLRPCException("code 403");
        }

        Object retValue = readFile(method, mPrefix);
        if (retValue == null) {
            // failback to default
            AppLog.w(T.TESTS, "failback to default");
            retValue = readFile(method, "default");
        }
        return retValue;
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

    public String getResponse() {
        if (mLoggedInputStream == null) {
            return "";
        }
        return mLoggedInputStream.getResponseDocument();
    }
}
