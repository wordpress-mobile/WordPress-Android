package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.model.AuthorModel;
import org.wordpress.android.ui.stats.model.CommentFollowersModel;
import org.wordpress.android.ui.stats.model.CommentsModel;
import org.wordpress.android.ui.stats.model.SingleItemModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.widgets.TypefaceCache;

import java.util.List;


public class StatsCommentsFragment extends StatsAbstractListFragment implements RadioGroup.OnCheckedChangeListener {
    public static final String TAG = StatsCommentsFragment.class.getSimpleName();

    private static String mTotalLabel = "Total comment followers: ";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        int dp8 = DisplayUtils.dpToPx(view.getContext(), 8);
        int dp80 = DisplayUtils.dpToPx(view.getContext(), 80);

        //String[] titles = getTabTitles();

        String[] titles = {"By Authors", "By Posts & Pages"};

        for (int i = 0; i < titles.length; i++) {
            RadioButton rb = (RadioButton) inflater.inflate(R.layout.stats_radio_button, null, false);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT);
            rb.setTypeface((TypefaceCache.getTypeface(view.getContext())));

            params.setMargins(0, 0, dp8, 0);
            rb.setMinimumWidth(dp80);
            rb.setGravity(Gravity.CENTER);
            rb.setLayoutParams(params);
            rb.setText(titles[i]);
            mTopPagerRadioGroup.addView(rb);

            if (i == mTopPagerSelectedButtonIndex)
                rb.setChecked(true);
        }

        mTopPagerRadioGroup.setVisibility(View.VISIBLE);
        mTopPagerRadioGroup.setOnCheckedChangeListener(this);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            AppLog.d(AppLog.T.STATS, this.getTag() + " > restoring instance state");
            if (savedInstanceState.containsKey(ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX)) {
                mTopPagerSelectedButtonIndex = savedInstanceState.getInt(ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX);
            }
        } else {
            // first time it's created
            mTopPagerSelectedButtonIndex = getArguments().getInt(ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX, 0);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        AppLog.d(AppLog.T.STATS, this.getTag() + " > saving instance state");
        outState.putInt(ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX, mTopPagerSelectedButtonIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        // checkedId will be -1 when the selection is cleared
        if (checkedId == -1)
            return;

        int index  = group.indexOfChild(group.findViewById(checkedId));
        if (index == -1)
            return;

        mTopPagerSelectedButtonIndex = index;

        View view = this.getView();
        TextView entryLabel = (TextView) view.findViewById(R.id.stats_list_entry_label);
        entryLabel.setText(getEntryLabelResId());

        updateUI();
    }

    @Override
    protected void updateUI() {
        mTopPagerRadioGroup.setVisibility(View.VISIBLE);

        if (mDatamodels == null) {
            showEmptyUI(true);
            mTotalsLabel.setVisibility(View.GONE);
            return;
        }

        if (isErrorResponse(mTopPagerSelectedButtonIndex)) {
            showErrorUI(mDatamodels[mTopPagerSelectedButtonIndex]);
            return;
        }

        if (mDatamodels[1] != null) { // check if comment-followers is already here
            mTotalsLabel.setVisibility(View.VISIBLE);
            mTotalsLabel.setText(mTotalLabel + ((CommentFollowersModel)mDatamodels[1]).getTotal());
        } else {
            mTotalsLabel.setVisibility(View.GONE);
        }

        ArrayAdapter adapter = null;

        if (mTopPagerSelectedButtonIndex == 0 && hasAuthors()) {
            adapter = new AuthorsAdapter(getActivity(), getAuthors());
        } else if (mTopPagerSelectedButtonIndex == 1 && hasPosts()) {
            adapter = new TopPostsAndPagesAdapter(getActivity(), getPosts());
        }

        if (adapter != null) {
            StatsUIHelper.reloadLinearLayout(getActivity(), adapter, mList, getMaxNumberOfItemsToShowInList());
            showEmptyUI(false);
        } else {
            showEmptyUI(true);
        }
    }

    private boolean hasAuthors() {
        return mDatamodels != null && mDatamodels[0] != null
                && ((CommentsModel) mDatamodels[0]).getAuthors() != null
                && ((CommentsModel) mDatamodels[0]).getAuthors().size() > 0;
    }

    private List<AuthorModel> getAuthors() {
        if (!hasAuthors()) {
            return null;
        }
        return ((CommentsModel) mDatamodels[0]).getAuthors();
    }

    private boolean hasPosts() {
        return mDatamodels != null && mDatamodels[0] != null
                && ((CommentsModel) mDatamodels[0]).getPosts() != null
                && ((CommentsModel) mDatamodels[0]).getPosts().size() > 0;
    }

    private List<SingleItemModel> getPosts() {
        if (!hasPosts()) {
            return null;
        }
        return ((CommentsModel) mDatamodels[0]).getPosts();
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        if (mTopPagerSelectedButtonIndex == 0 && hasAuthors() && getAuthors().size() > 10) {
            return true;
        } else if (mTopPagerSelectedButtonIndex == 1 && hasPosts() && getPosts().size() > 10) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean isExpandableList() {
        return false;
    }

    private class TopPostsAndPagesAdapter extends ArrayAdapter<SingleItemModel> {

        private final List<SingleItemModel> list;
        private final Activity context;
        private final LayoutInflater inflater;

        public TopPostsAndPagesAdapter(Activity context, List<SingleItemModel> list) {
            super(context, R.layout.stats_list_cell, list);
            this.context = context;
            this.list = list;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            // reuse views
            if (rowView == null) {
                rowView = inflater.inflate(R.layout.stats_list_cell, null);
                // configure view holder
                StatsViewHolder viewHolder = new StatsViewHolder(rowView);
                rowView.setTag(viewHolder);
            }

            final SingleItemModel currentRowData = list.get(position);
            StatsViewHolder holder = (StatsViewHolder) rowView.getTag();
            // fill data
            // entries
            holder.setEntryTextOrLink(currentRowData.getUrl(), currentRowData.getTitle());
            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(currentRowData.getTotals()));

            // no icon
            holder.networkImageView.setVisibility(View.GONE);

            return rowView;
        }
    }

    private class AuthorsAdapter extends ArrayAdapter<AuthorModel> {

        private final List<AuthorModel> list;
        private final Activity context;
        private final LayoutInflater inflater;

        public AuthorsAdapter(Activity context, List<AuthorModel> list) {
            super(context, R.layout.stats_list_cell, list);
            this.context = context;
            this.list = list;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            // reuse views
            if (rowView == null) {
                rowView = inflater.inflate(R.layout.stats_list_cell, null);
                // configure view holder
                StatsViewHolder viewHolder = new StatsViewHolder(rowView);
                rowView.setTag(viewHolder);
            }

            final AuthorModel currentRowData = list.get(position);
            StatsViewHolder holder = (StatsViewHolder) rowView.getTag();
            // fill data
            // entries
            holder.entryTextView.setText(currentRowData.getName());
            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(currentRowData.getViews()));

            holder.showNetworkImage(currentRowData.getAvatar());

            // no icon
            holder.networkImageView.setVisibility(View.VISIBLE);

            return rowView;
        }
    }

    @Override
    protected int getEntryLabelResId() {
        if (mTopPagerSelectedButtonIndex == 0) {
            return R.string.stats_entry_top_commenter;
        } else {
            return R.string.stats_entry_posts_and_pages;
        }
    }

    @Override
    protected int getTotalsLabelResId() {
        return R.string.stats_totals_comments;
    }

    @Override
    protected int getEmptyLabelTitleResId() {
        return R.string.stats_empty_comments;
    }

    @Override
    protected int getEmptyLabelDescResId() {
        return R.string.stats_empty_comments_desc;
    }

    @Override
    protected StatsService.StatsEndpointsEnum[] getSectionToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.COMMENTS, StatsService.StatsEndpointsEnum.COMMENT_FOLLOWERS
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_comments);
    }
}