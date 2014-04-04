package org.xmlrpc.android;

import java.io.File;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

public interface XMLRPCClientInterface {
    public void addQuickPostHeader(String type);
    public void setAuthorizationHeader(String authToken);
    public Object call(String method, Object[] params) throws XMLRPCException, IOException, XmlPullParserException;
    public Object call(String method) throws XMLRPCException, IOException, XmlPullParserException;
    public Object call(String method, Object[] params, File tempFile) throws XMLRPCException, IOException, XmlPullParserException;
    public long callAsync(XMLRPCCallback listener, String methodName, Object[] params);
    public long callAsync(XMLRPCCallback listener, String methodName, Object[] params, File tempFile);
}
