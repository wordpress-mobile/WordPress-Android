package org.wordpress.android.ui.suggestion.adapters;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.suggestion.Suggestion;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

public class SuggestionAdapter extends BaseAdapter implements Filterable {
    private final LayoutInflater mInflater;
    private Filter mSuggestionFilter;
    private List<Suggestion> mSuggestionList;
    private List<Suggestion> mOrigSuggestionList;
    private final int mAvatarSz;
    private @Nullable @AttrRes Integer mBackgroundColor;

    @Inject protected ImageManager mImageManager;

    private final char mPrefix;

    public SuggestionAdapter(Context context, char prefix) {
        this.mPrefix = prefix;
        ((WordPress) context.getApplicationContext()).component().inject(this);
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_small);
        mInflater = LayoutInflater.from(context);
    }

    public void setBackgroundColorAttr(int backgroundColor) {
        mBackgroundColor = backgroundColor;
    }

    public void setSuggestionList(List<Suggestion> suggestionList) {
        mOrigSuggestionList = suggestionList;
    }

    public List<Suggestion> getSuggestionList() {
        return mOrigSuggestionList;
    }

    public List<Suggestion> getFilteredSuggestions() {
        return mSuggestionList;
    }

    @Override
    public int getCount() {
        if (mSuggestionList == null) {
            return 0;
        }
        return mSuggestionList.size();
    }

    @Override
    public Suggestion getItem(int position) {
        if (mSuggestionList == null) {
            return null;
        }
        return mSuggestionList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final SuggestionViewHolder holder;

        if (convertView == null || convertView.getTag() == null) {
            convertView = mInflater.inflate(R.layout.suggestion_list_row, parent, false);
            if (mBackgroundColor != null) {
                TypedValue typedValue = new TypedValue();
                convertView.getContext().getTheme().resolveAttribute(mBackgroundColor, typedValue, true);
                View view = convertView.findViewById(R.id.suggestion_list_root_view);
                view.setBackgroundResource(typedValue.resourceId);
            }
            holder = new SuggestionViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (SuggestionViewHolder) convertView.getTag();
        }

        Suggestion suggestion = getItem(position);

        if (suggestion != null) {
            String avatarUrl = GravatarUtils.fixGravatarUrl(suggestion.getAvatarUrl(), mAvatarSz);
            mImageManager.loadIntoCircle(holder.mImgAvatar, ImageType.AVATAR_WITH_BACKGROUND, avatarUrl);
            String value = mPrefix + suggestion.getValue();
            holder.mValue.setText(value);
            holder.mDisplayValue.setText(suggestion.getDisplayValue());
        }

        return convertView;
    }

    @Override
    public Filter getFilter() {
        if (mSuggestionFilter == null) {
            mSuggestionFilter = new SuggestionFilter();
        }

        return mSuggestionFilter;
    }

    private static class SuggestionViewHolder {
        private final ImageView mImgAvatar;
        private final TextView mValue;
        private final TextView mDisplayValue;

        SuggestionViewHolder(View row) {
            mImgAvatar = row.findViewById(R.id.suggest_list_row_avatar);
            mValue = row.findViewById(R.id.suggestion_list_row_raw_value_label);
            mDisplayValue = row.findViewById(R.id.suggestion_list_row_display_value_label);
        }
    }

    private class SuggestionFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Suggestion> filteredSuggestions = getFilteredSuggestions(constraint);
            FilterResults results = new FilterResults();
            results.values = filteredSuggestions;
            results.count = filteredSuggestions.size();
            return results;
        }

        @NonNull
        private List<Suggestion> getFilteredSuggestions(CharSequence constraint) {
            if (mOrigSuggestionList == null) {
                return Collections.emptyList();
            } else if (constraint == null || constraint.length() == 0) {
                return mOrigSuggestionList;
            } else {
                List<Suggestion> filteredSuggestions = new ArrayList<>();
                for (Suggestion suggestion : mOrigSuggestionList) {
                    String lowerCaseConstraint = constraint.toString().toLowerCase(Locale.getDefault());
                    boolean suggestionMatchesConstraint =
                            suggestion.getValue().toLowerCase(Locale.ROOT).startsWith(lowerCaseConstraint)
                            || suggestion.getDisplayValue().toLowerCase(Locale.getDefault())
                                         .startsWith(lowerCaseConstraint)
                            || suggestion.getDisplayValue().toLowerCase(Locale.getDefault())
                                         .contains(" " + lowerCaseConstraint);
                    if (suggestionMatchesConstraint) {
                        filteredSuggestions.add(suggestion);
                    }
                }
                return filteredSuggestions;
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            mSuggestionList = (List<Suggestion>) results.values;
            if (results.count == 0) {
                notifyDataSetInvalidated();
            } else {
                notifyDataSetChanged();
            }
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            Suggestion suggestion = (Suggestion) resultValue;
            return suggestion.getValue();
        }
    }
}
