package org.wordpress.android.mocks;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class XMLRPCClientCustomizableXMLMock extends XMLRPCClientCustomizableMockAbstract {
    XMLRPCClient mXmlRpcClient;

    public XMLRPCClientCustomizableXMLMock(URI uri, String httpUser, String httpPassword) {
        // Used to test ctor and preparePostMethod
        mXmlRpcClient = new XMLRPCClient("", "", "");
    }

    public void addQuickPostHeader(String type) {
    }

    public void setAuthorizationHeader(String authToken) {
    }

    private Object readFile(String method, String prefix) {
        // method example: wp.getUsersBlogs
        // Filename: default-wp.getUsersBlogs.xml
        String filename = prefix + "-" + method + ".xml";
        try {
            InputStream is = mContext.getAssets().open(filename);
            return XMLRPCClient.parseXMLRPCResponse(is);
        } catch (FileNotFoundException e) {
            AppLog.e(T.TESTS, "file not found: " + filename);
        } catch (Exception e) {
            AppLog.e(T.TESTS, "can't read file: " + filename, e);
        }
        return null;
    }

    public Object call(String method, Object[] params) throws XMLRPCException {
        try {
            mXmlRpcClient.preparePostMethod(method, params, null);
        } catch (IOException e) {
            // unexpected error, test must fail
            throw new XMLRPCException("preparePostMethod failed");
        }
        AppLog.v(T.TESTS, "XMLRPCClientCustomizableXMLMock call: " + method);
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
}
