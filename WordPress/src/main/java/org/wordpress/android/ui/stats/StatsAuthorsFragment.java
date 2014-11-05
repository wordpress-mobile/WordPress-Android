package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.model.AuthorModel;
import org.wordpress.android.ui.stats.model.AuthorsModel;
import org.wordpress.android.ui.stats.model.TopPostModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;

import java.util.List;

public class StatsAuthorsFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsAuthorsFragment.class.getSimpleName();

    @Override
    protected void updateUI() {
        if (mDatamodel != null &&  ((AuthorsModel)mDatamodel).getAuthors() != null) {
            BaseExpandableListAdapter adapter = new MyExpandableListAdapter(getActivity(), ((AuthorsModel)mDatamodel).getAuthors());
            StatsUIHelper.reloadGroupViews(getActivity(), adapter, mGroupIdToExpandedMap, mList);
            showEmptyUI(false);
        } else {
            showEmptyUI(true);
        }
    }

    @Override
    protected boolean isExpandableList() {
        return true;
    }

    @Override
    protected StatsService.StatsSectionEnum getSectionToUpdate() {
        return StatsService.StatsSectionEnum.AUTHORS;
    }

    @Override
    protected int getEntryLabelResId() {
        return R.string.stats_entry_authors;
    }
    @Override
    protected int getTotalsLabelResId() {
        return R.string.stats_totals_views;
    }
    @Override
    protected int getEmptyLabelTitleResId() {
        return R.string.stats_empty_top_authors;
    }
    @Override
    protected int getEmptyLabelDescResId() {
        return R.string.stats_empty_top_authors_desc;
    }

    private class MyExpandableListAdapter extends BaseExpandableListAdapter {
        public LayoutInflater inflater;
        public Activity activity;
        private List<AuthorModel> authors;

        public MyExpandableListAdapter(Activity act, List<AuthorModel> authors) {
            this.activity = act;
            this.authors = authors;
            this.inflater = act.getLayoutInflater();
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            AuthorModel currentGroup = authors.get(groupPosition);
            List<TopPostModel> posts = currentGroup.getPosts();
            TopPostModel currentRes = posts.get(childPosition);
            return currentRes;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {

            final TopPostModel children = (TopPostModel) getChild(groupPosition, childPosition);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.stats_list_cell, parent, false);
                // configure view holder
                StatsViewHolder viewHolder = new StatsViewHolder(convertView);
                convertView.setTag(viewHolder);
            }

            final StatsViewHolder holder = (StatsViewHolder) convertView.getTag();

            String name = children.getTitle();
            int total = children.getViews();
            String url = children.getUrl();

            // name, url
            holder.setEntryTextOrLink(url, name);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // no icon, make it invisible so children are indented
            holder.networkImageView.setVisibility(View.INVISIBLE);

            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            AuthorModel currentGroup = authors.get(groupPosition);
            List<TopPostModel> posts = currentGroup.getPosts();
            if (posts == null) {
                return 0;
            } else {
                return posts.size();
            }
        }

        @Override
        public Object getGroup(int groupPosition) {
            return authors.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return authors.size();
        }


        @Override
        public long getGroupId(int groupPosition) {
            return 0;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.stats_list_cell, parent, false);
                convertView.setTag(new StatsViewHolder(convertView));
            }

            final StatsViewHolder holder = (StatsViewHolder) convertView.getTag();

            AuthorModel group = (AuthorModel) getGroup(groupPosition);

            String name = group.getName();
            int total = group.getViews();
            String icon = group.getAvatar();
            int children = getChildrenCount(groupPosition);

            holder.entryTextView.setText(name);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // icon
            holder.showNetworkImage(icon);

            // expand/collapse chevron
            holder.chevronImageView.setVisibility(children > 0 ? View.VISIBLE : View.GONE);
            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }

    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_top_authors);
    }
}
