package org.wordpress.android.fluxc.network.wporg.plugin;

import java.util.List;

public class FetchPluginDirectoryResponse {
    public class FetchPluginDirectoryResponseInfo {
        public int page;
        public int pages;
        public int results;
    }

    public FetchPluginDirectoryResponseInfo info;
    public List<WPOrgPluginResponse> plugins;
}
