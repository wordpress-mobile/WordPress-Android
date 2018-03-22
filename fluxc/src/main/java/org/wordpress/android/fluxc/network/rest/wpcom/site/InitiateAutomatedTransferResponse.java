package org.wordpress.android.fluxc.network.rest.wpcom.site;

import com.google.gson.annotations.SerializedName;

public class InitiateAutomatedTransferResponse {
    public String status;
    public boolean success;
    @SerializedName("transfer_id")
    public int transferId;
}
