package org.wordpress.android.fluxc.network.rest.wpcom.site;

import com.google.gson.annotations.SerializedName;

public class AutomatedTransferStatusResponse {
    public String status;
    @SerializedName("transfer_id")
    public String transferId;
}
