package org.wordpress.android.ui.stats.models;

import java.io.Serializable;

public class SearchTermModel extends SingleItemModel implements Serializable {

    private final boolean mIsEncriptedTerms;

    public SearchTermModel(String blogId, String date, String title, int totals, boolean isEncriptedTerms) {
        super(blogId, date, null, title, totals, null, null);
        this.mIsEncriptedTerms = isEncriptedTerms;
    }

    public boolean isEncriptedTerms() {
        return mIsEncriptedTerms;
    }

}
