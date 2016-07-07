package org.wordpress.android.models;

import android.support.annotation.StringRes;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

public enum PeopleListFilter implements FilterCriteria {
    TEAM(R.string.people_dropdown_item_team),
    FOLLOWERS(R.string.people_dropdown_item_followers),
    EMAIL_FOLLOWERS(R.string.people_dropdown_item_email_followers);

    private final int mLabelResId;

    PeopleListFilter(@StringRes int labelResId) {
        mLabelResId = labelResId;
    }

    @Override
    public String getLabel() {
        return WordPress.getContext().getString(mLabelResId);
    }
}
