package org.wordpress.android.fluxc.network.rest.wpcom.site;

import org.wordpress.android.fluxc.network.Response;

public class ConnectSiteInfoResponse implements Response {
    public boolean exists = false;
    public boolean isWordPress = false;
    public boolean hasJetpack = false;
    public boolean isJetpackActive = false;
    public boolean isJetpackConnected = false;
    // CHECKSTYLE IGNORE RegexpSingleline
    public boolean isWordPressDotCom = false;
    // CHECKSTYLE END IGNORE RegexpSingleline
}
