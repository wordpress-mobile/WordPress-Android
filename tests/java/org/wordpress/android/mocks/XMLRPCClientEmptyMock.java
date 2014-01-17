package org.wordpress.android.mocks;

import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;

import java.io.File;
import java.net.URI;

import hugo.weaving.DebugLog;

public class XMLRPCClientEmptyMock implements XMLRPCClientInterface {
    public XMLRPCClientEmptyMock(URI uri, String httpUser, String httpPassword) {

    }

    @DebugLog
    public void addQuickPostHeader(String type) {

    }

    @DebugLog
    public void setAuthorizationHeader(String authToken) {

    }

    @DebugLog
    public Object call(String method, Object[] params) throws XMLRPCException {
        return null;
    }

    @DebugLog
    public Object call(String method) throws XMLRPCException {
        return null;
    }

    @DebugLog
    public Object call(String method, Object[] params, File tempFile) throws XMLRPCException {
        return null;
    }

    @DebugLog
    public long callAsync(XMLRPCCallback listener, String methodName, Object[] params) {
        return 0;
    }

    @DebugLog
    public long callAsync(XMLRPCCallback listener, String methodName, Object[] params, File tempFile) {
        return 0;
    }
}
