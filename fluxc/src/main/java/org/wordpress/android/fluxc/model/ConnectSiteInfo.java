package org.wordpress.android.fluxc.model;

import org.wordpress.android.fluxc.Payload;

public class ConnectSiteInfo extends Payload {
    public String url = "";
    public boolean exists = false;
    public boolean isWordPress = false;
    public boolean hasJetpack = false;
    public boolean isJetpackActive = false;
    public boolean isJetpackConnected = false;
    public boolean isWordPressDotCom = false;

}
