package org.wordpress.android.models;

import androidx.annotation.StringRes;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

public enum PeopleListFilter implements FilterCriteria {
    TEAM(R.string.people_dropdown_item_team),
    SUBSCRIBERS(R.string.people_dropdown_item_subscribers),
    EMAIL_SUBSCRIBERS(R.string.people_dropdown_item_email_subscribers),
    VIEWERS(R.string.people_dropdown_item_viewers);

    private final int mLabelResId;

    PeopleListFilter(@StringRes int labelResId) {
        mLabelResId = labelResId;
    }

    @Override
    public String getLabel() {
        return WordPress.getContext().getString(mLabelResId);
    }
}
