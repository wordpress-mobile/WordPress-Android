package org.wordpress.android.fluxc.network.rest.wpcom.site;

import com.google.gson.annotations.SerializedName;

public class AutomatedTransferEligibilityCheckResponse {
    @SerializedName("is_eligible")
    public boolean isEligible;
    public EligibilityError[] errors;

    class EligibilityError {
        public String code;
        public String message;
    }
}
