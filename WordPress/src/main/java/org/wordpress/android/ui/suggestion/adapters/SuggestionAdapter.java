package org.wordpress.android.ui.suggestion.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Suggestion;
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
    private int mAvatarSz;

    @Inject protected ImageManager mImageManager;

    public SuggestionAdapter(Context context) {
        ((WordPress) context.getApplicationContext()).component().inject(this);
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_small);
        mInflater = LayoutInflater.from(context);
    }

    public void setSuggestionList(List<Suggestion> suggestionList) {
        mOrigSuggestionList = suggestionList;
    }

    @NonNull
    public List<Suggestion> getFilteredSuggestions() {
        return mSuggestionList != null ? mSuggestionList : Collections.emptyList();
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
            holder = new SuggestionViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (SuggestionViewHolder) convertView.getTag();
        }

        Suggestion suggestion = getItem(position);

        if (suggestion != null) {
            String avatarUrl = GravatarUtils.fixGravatarUrl(suggestion.getImageUrl(), mAvatarSz);
            mImageManager.loadIntoCircle(holder.mImgAvatar, ImageType.AVATAR_WITH_BACKGROUND, avatarUrl);
            holder.mTxtUserLogin
                    .setText(convertView.getResources().getString(R.string.at_username, suggestion.getUserLogin()));
            holder.mTxtDisplayName.setText(suggestion.getDisplayName());
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

    private class SuggestionViewHolder {
        private final ImageView mImgAvatar;
        private final TextView mTxtUserLogin;
        private final TextView mTxtDisplayName;

        SuggestionViewHolder(View row) {
            mImgAvatar = row.findViewById(R.id.suggest_list_row_avatar);
            mTxtUserLogin = row.findViewById(R.id.suggestion_list_row_user_login_label);
            mTxtDisplayName = row.findViewById(R.id.suggestion_list_row_display_name_label);
        }
    }

    private class SuggestionFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();

            if (mOrigSuggestionList == null) {
                results.values = null;
                results.count = 0;
            } else if (constraint == null || constraint.length() == 0) {
                results.values = mOrigSuggestionList;
                results.count = mOrigSuggestionList.size();
            } else {
                List<Suggestion> nSuggestionList = new ArrayList<>();

                for (Suggestion suggestion : mOrigSuggestionList) {
                    String lowerCaseConstraint = constraint.toString().toLowerCase(Locale.getDefault());
                    if (suggestion.getUserLogin().toLowerCase(Locale.ROOT).startsWith(lowerCaseConstraint)
                        || suggestion.getDisplayName().toLowerCase(Locale.getDefault()).startsWith(lowerCaseConstraint)
                        || suggestion.getDisplayName().toLowerCase(Locale.getDefault())
                                     .contains(" " + lowerCaseConstraint)) {
                        nSuggestionList.add(suggestion);
                    }
                }

                results.values = nSuggestionList;
                results.count = nSuggestionList.size();
            }
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            if (results.count == 0) {
                notifyDataSetInvalidated();
            } else {
                mSuggestionList = (List<Suggestion>) results.values;
                notifyDataSetChanged();
            }
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            Suggestion suggestion = (Suggestion) resultValue;
            return suggestion.getUserLogin();
        }
    }
}
