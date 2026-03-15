package org.wordpress.android.models;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import org.wordpress.android.R;

public enum PeopleListFilter implements FilterCriteria {
    TEAM(R.string.people_dropdown_item_team),
    SUBSCRIBERS(R.string.people_dropdown_item_subscribers),
    EMAIL_SUBSCRIBERS(R.string.people_dropdown_item_email_subscribers),
    VIEWERS(R.string.people_dropdown_item_viewers);

    private final int mLabelResId;

    PeopleListFilter(@StringRes int labelResId) {
        mLabelResId = labelResId;
    }

    @NonNull @Override
    public String getLabel(@NonNull Context context) {
        return context.getString(mLabelResId);
    }
}
