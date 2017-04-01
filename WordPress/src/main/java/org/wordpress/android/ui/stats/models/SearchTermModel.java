package org.wordpress.android.ui.stats.models;

import java.io.Serializable;

public class SearchTermModel extends SingleItemModel implements Serializable {

    private final boolean mEncriptedTermsEh;

    public SearchTermModel(long blogId, String date, String title, int totals, boolean encriptedTermsEh) {
        super(blogId, date, null, title, totals, null, null);
        this.mEncriptedTermsEh = encriptedTermsEh;
    }

    public boolean encriptedTermsEh() {
        return mEncriptedTermsEh;
    }

}
