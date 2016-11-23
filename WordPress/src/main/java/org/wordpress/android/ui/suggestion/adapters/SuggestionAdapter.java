package org.wordpress.android.ui.suggestion.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.Suggestion;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.List;

public class SuggestionAdapter extends BaseAdapter implements Filterable {
    private final LayoutInflater mInflater;
    private Filter mSuggestionFilter;
    private List<Suggestion> mSuggestionList;
    private List<Suggestion> mOrigSuggestionList;
    private int mAvatarSz;

    public SuggestionAdapter(Context context) {
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_small);
        mInflater = LayoutInflater.from(context);
    }

    public void setSuggestionList(List<Suggestion> suggestionList) {
        mOrigSuggestionList = suggestionList;
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
            holder.imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
            holder.txtUserLogin.setText("@" + suggestion.getUserLogin());
            holder.txtDisplayName.setText(suggestion.getDisplayName());
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
        private final WPNetworkImageView imgAvatar;
        private final TextView txtUserLogin;
        private final TextView txtDisplayName;

        SuggestionViewHolder(View row) {
            imgAvatar = (WPNetworkImageView) row.findViewById(R.id.suggest_list_row_avatar);
            txtUserLogin = (TextView) row.findViewById(R.id.suggestion_list_row_user_login_label);
            txtDisplayName = (TextView) row.findViewById(R.id.suggestion_list_row_display_name_label);
        }
    }

    private class SuggestionFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();

            if (mOrigSuggestionList == null) {
                results.values = null;
                results.count = 0;
            }
            else if (constraint == null || constraint.length() == 0) {
                results.values = mOrigSuggestionList;
                results.count = mOrigSuggestionList.size();
            }
            else {
                List<Suggestion> nSuggestionList = new ArrayList<Suggestion>();

                for (Suggestion suggestion : mOrigSuggestionList) {
                    String lowerCaseConstraint = constraint.toString().toLowerCase();
                    if (suggestion.getUserLogin().toLowerCase().startsWith(lowerCaseConstraint)
                            || suggestion.getDisplayName().toLowerCase().startsWith(lowerCaseConstraint)
                            || suggestion.getDisplayName().toLowerCase().contains(" " + lowerCaseConstraint))
                        nSuggestionList.add(suggestion);
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
            if (results.count == 0)
                notifyDataSetInvalidated();
            else {
                mSuggestionList = (List<Suggestion>) results.values;
                notifyDataSetChanged();
            }
        }

        @Override
        public CharSequence convertResultToString (Object resultValue) {
            Suggestion suggestion = (Suggestion) resultValue;
            return suggestion.getUserLogin();
        }
    }
}
