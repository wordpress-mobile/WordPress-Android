package org.wordpress.android.mocks;

import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCFactoryAbstract;

import java.net.URI;

public class XMLRPCFactoryTest extends XMLRPCFactoryAbstract  {
    public XMLRPCClientInterface make(URI uri, String httpUser, String httpPassword) {
        return new XMLRPCClientEmptyMock(uri, httpUser, httpPassword);
    }
}
