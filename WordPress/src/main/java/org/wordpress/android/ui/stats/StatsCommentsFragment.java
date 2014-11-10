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

    private RadioGroup mRadioGroup;

    private static final String SELECTED_BUTTON_INDEX = "SELECTED_BUTTON_INDEX";
    private int mSelectedButtonIndex = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mRadioGroup = (RadioGroup) view.findViewById(R.id.stats_pager_tabs);


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
            mRadioGroup.addView(rb);

            if (i == mSelectedButtonIndex)
                rb.setChecked(true);
        }

        mRadioGroup.setVisibility(View.VISIBLE);
        mRadioGroup.setOnCheckedChangeListener(this);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            AppLog.d(AppLog.T.STATS, this.getTag() + " > restoring instance state");
            if (savedInstanceState.containsKey(SELECTED_BUTTON_INDEX)) {
                mSelectedButtonIndex = savedInstanceState.getInt(SELECTED_BUTTON_INDEX);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        AppLog.d(AppLog.T.STATS, this.getTag() + " > saving instance state");
        outState.putInt(SELECTED_BUTTON_INDEX, mSelectedButtonIndex);
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

        mSelectedButtonIndex = index;

        View view = this.getView();
        TextView entryLabel = (TextView) view.findViewById(R.id.stats_list_entry_label);
        entryLabel.setText(getEntryLabelResId());

        updateUI();
    }


    @Override
    protected void updateUI() {
        if (mDatamodel == null) {
            showEmptyUI(true);
            return;
        }

        CommentsModel commentsModel = (CommentsModel) mDatamodel;
        ArrayAdapter adapter = null;
        if (mSelectedButtonIndex == 0) {
            List<AuthorModel> authors = commentsModel.getAuthors();
            if (authors != null && authors.size() > 0) {
                adapter = new AuthorsAdapter(getActivity(), authors);
            }
        } else {
            List<SingleItemModel> posts = commentsModel.getPosts();
            if (posts != null && posts.size() > 0) {
                adapter = new TopPostsAndPagesAdapter(getActivity(), posts);
            }
        }

        if (adapter != null) {
            StatsUIHelper.reloadLinearLayout(getActivity(), adapter, mList);
            showEmptyUI(false);
        } else {
            showEmptyUI(true);
        }
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
        if (mSelectedButtonIndex == 0) {
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
    protected StatsService.StatsEndpointsEnum getSectionToUpdate() {
        return StatsService.StatsEndpointsEnum.COMMENTS;
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_comments);
    }
}
