package org.wordpress.android.mocks;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlrpc.android.LoggedInputStream;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

public class XMLRPCClientCustomizableXMLMock extends XMLRPCClientCustomizableMockAbstract {
    XMLRPCClient mXmlRpcClient;
    private LoggedInputStream mLoggedInputStream;

    public XMLRPCClientCustomizableXMLMock(URI uri, String httpUser, String httpPassword) {
        // Used to test ctor and preparePostMethod
        mXmlRpcClient = new XMLRPCClient("", "", "");
    }

    public void addQuickPostHeader(String type) {
    }

    public void setAuthorizationHeader(String authToken) {
    }

    private Object readFile(String method, String prefix) throws IOException, XMLRPCException, XmlPullParserException {
        // method example: wp.getUsersBlogs
        // Filename: default-wp.getUsersBlogs.xml
        String filename = prefix + "-" + method + ".xml";
        try {
            mLoggedInputStream = new LoggedInputStream(mContext.getAssets().open(filename));
            return XMLRPCClient.parseXMLRPCResponse(mLoggedInputStream, null);
        } catch (FileNotFoundException e) {
            AppLog.e(T.TESTS, "file not found: " + filename);
        }
        return null;
    }

    public Object call(String method, Object[] params) throws XMLRPCException, IOException, XmlPullParserException {
        mLoggedInputStream = null;
        try {
            mXmlRpcClient.preparePostMethod(method, params, null);
        } catch (IOException e) {
            // unexpected error, test must fail
            throw new XMLRPCException("preparePostMethod failed");
        }
        AppLog.v(T.TESTS, "XMLRPCClientCustomizableXMLMock call: " + method);
        if ("login-failure".equals(mPrefix)) {
            // Wrong login
            throw new XMLRPCFault("code 403", 403);
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
