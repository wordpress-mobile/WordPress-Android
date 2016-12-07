package org.wordpress.android.fluxc.model;

import android.support.annotation.NonNull;

import org.wordpress.android.fluxc.Payload;

import java.util.ArrayList;
import java.util.List;

public class TermsModel extends Payload {
    private List<TermModel> mTerms;

    public TermsModel() {
        mTerms = new ArrayList<>();
    }

    public TermsModel(@NonNull List<TermModel> terms) {
        mTerms = terms;
    }

    public List<TermModel> getTerms() {
        return mTerms;
    }

    public void setTerms(List<TermModel> terms) {
        this.mTerms = terms;
    }
}
