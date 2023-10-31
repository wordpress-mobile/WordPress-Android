package org.wordpress.android.fluxc.model;

import androidx.annotation.NonNull;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;

import java.util.ArrayList;
import java.util.List;

public class TermsModel extends Payload<BaseNetworkError> {
    @NonNull private List<TermModel> mTerms;

    public TermsModel() {
        mTerms = new ArrayList<>();
    }

    public TermsModel(@NonNull List<TermModel> terms) {
        mTerms = terms;
    }

    @NonNull
    public List<TermModel> getTerms() {
        return mTerms;
    }

    public void setTerms(@NonNull List<TermModel> terms) {
        this.mTerms = terms;
    }
}
