package org.xmlrpc.android;

import java.io.File;

public interface XMLRPCClientInterface {
    public void addQuickPostHeader(String type);
    public void setAuthorizationHeader(String authToken);
    public Object call(String method, Object[] params) throws XMLRPCException;
    public Object call(String method) throws XMLRPCException;
    public Object call(String method, Object[] params, File tempFile) throws XMLRPCException;
    public long callAsync(XMLRPCCallback listener, String methodName, Object[] params);
    public long callAsync(XMLRPCCallback listener, String methodName, Object[] params, File tempFile);
}
