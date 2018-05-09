package org.wordpress.android.fluxc.network.rest.wpcom.site;

import org.wordpress.android.fluxc.network.Response;

public class ConnectSiteInfoResponse implements Response {
    public boolean exists;
    public boolean isWordPress;
    public boolean hasJetpack;
    public boolean isJetpackActive;
    public boolean isJetpackConnected;
    public boolean isWordPressDotCom; // CHECKSTYLE IGNORE
    public String urlAfterRedirects;
}
