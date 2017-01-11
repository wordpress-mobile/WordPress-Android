package org.wordpress.android.fluxc.network.discovery;

import org.wordpress.android.fluxc.network.Response;
import org.wordpress.android.fluxc.network.rest.JsonObjectOrEmptyArray;

import java.util.ArrayList;

public class RootWPAPIRestResponse implements Response {
    public class Authentication extends JsonObjectOrEmptyArray {
        public class Oauth1 {
            public String request;
            public String authorize;
            public String access;
            public String version;
        }

        public Oauth1 oauth1;
    }

    public ArrayList<String> namespaces;
    public Authentication authentication;
}
