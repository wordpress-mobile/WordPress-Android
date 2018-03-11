package org.wordpress.android.fluxc.model;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.util.StringUtils;

public class StockMediaUploadModel extends Payload<BaseNetworkError> {
    private String mUrl;
    private String mName;

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof StockMediaUploadModel)) return false;

        StockMediaUploadModel otherMedia = (StockMediaUploadModel) other;

        return StringUtils.equals(this.getUrl(), otherMedia.getUrl())
                && StringUtils.equals(this.getName(), otherMedia.getName());
    }
}
