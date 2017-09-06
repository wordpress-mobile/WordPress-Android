package org.wordpress.android.fluxc.network.rest.wpcom.plugin;

import java.util.List;

public class PluginWPComRestResponse {
    public class FetchPluginsResponse {
        public List<PluginWPComRestResponse> plugins;
    }

    public String id;
    public boolean active;
    public String author;
    public String author_url;
    public boolean autoupdate;
    public String description;
    public String name;
    public String plugin_url;
    public String slug;
    public String version;
}
