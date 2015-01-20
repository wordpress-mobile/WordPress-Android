package org.xmlrpc.android;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;

public interface XMLRPCClientInterface {
    public void addQuickPostHeader(String type);
    public void setAuthorizationHeader(String authToken);
    public Object call(String method, Object[] params) throws XMLRPCException, IOException, XmlPullParserException;
    public Object call(String method) throws XMLRPCException, IOException, XmlPullParserException;
    public Object call(String method, Object[] params, File tempFile) throws XMLRPCException, IOException, XmlPullParserException;
    public long callAsync(XMLRPCCallback listener, String methodName, Object[] params);
    public long callAsync(XMLRPCCallback listener, String methodName, Object[] params, File tempFile);
    public String getResponse();
}
