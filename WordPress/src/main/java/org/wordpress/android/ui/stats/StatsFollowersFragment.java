package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.FollowDataModel;
import org.wordpress.android.ui.stats.models.FollowerModel;
import org.wordpress.android.ui.stats.models.FollowersModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.widgets.TypefaceCache;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class StatsFollowersFragment extends StatsAbstractListFragment implements RadioGroup.OnCheckedChangeListener {
    public static final String TAG = StatsFollowersFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        int dp4 = DisplayUtils.dpToPx(view.getContext(), 4);
        int dp80 = DisplayUtils.dpToPx(view.getContext(), 80);

        //String[] titles = getTabTitles();

        String[] titles = {
                getResources().getString(R.string.stats_followers_wpcom_selector),
                getResources().getString(R.string.stats_followers_email_selector),
        };

        for (int i = 0; i < titles.length; i++) {
            RadioButton rb = (RadioButton) inflater.inflate(R.layout.stats_radio_button, null, false);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.MATCH_PARENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT);
            params.weight = 1;
            rb.setTypeface((TypefaceCache.getTypeface(view.getContext())));
            if (i == 0) {
                params.setMargins(0, 0, dp4, 0);
            } else {
                params.setMargins(dp4, 0, 0, 0);
            }
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

        mTotalsLabel.setVisibility(View.VISIBLE);
        mTotalsLabel.setText("");

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
        mTotalsLabel.setVisibility(View.VISIBLE);

        if (mDatamodels == null) {
            showEmptyUI(true);
            mTotalsLabel.setText(getTotalFollowersLabel(0));
            return;
        }

        if (isErrorResponse(mTopPagerSelectedButtonIndex)) {
            showErrorUI(mDatamodels[mTopPagerSelectedButtonIndex]);
            return;
        }

        final FollowersModel followersModel = (FollowersModel) mDatamodels[mTopPagerSelectedButtonIndex];
        if (followersModel != null && followersModel.getFollowers() != null &&
                followersModel.getFollowers().size() > 0) {
            ArrayAdapter adapter = new DotComFollowerAdapter(getActivity(), followersModel.getFollowers());
            StatsUIHelper.reloadLinearLayout(getActivity(), adapter, mList, getMaxNumberOfItemsToShowInList());
            showEmptyUI(false);
            if (isSingleView()) {
                if (followersModel.getPages() > 1) {
                    mPaginationContainer.setVisibility(View.VISIBLE);
                    mPaginationText.setText("Page " + followersModel.getPage() + " of " + followersModel.getPages());
                    mPaginationGoBackButton.setEnabled(true);
                    mPaginationGoForwardButton.setEnabled(true);

                    if (followersModel.getPage() == 1) {
                        mPaginationGoBackButton.setVisibility(View.GONE);
                    } else {
                        mPaginationGoBackButton.setVisibility(View.VISIBLE);
                        mPaginationGoBackButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mPaginationGoBackButton.setEnabled(false);
                                mPaginationGoForwardButton.setEnabled(false);
                                mMoreDataListener.onMoreDataRequested(
                                        getSectionToUpdate()[mTopPagerSelectedButtonIndex],
                                        followersModel.getPage() - 1
                                );
                            }
                        });
                    }

                    if (followersModel.getPage() == followersModel.getPages()) {
                        mPaginationGoForwardButton.setVisibility(View.GONE);
                    } else {
                        mPaginationGoForwardButton.setVisibility(View.VISIBLE);
                        mPaginationGoForwardButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mPaginationGoBackButton.setEnabled(false);
                                mPaginationGoForwardButton.setEnabled(false);
                                mMoreDataListener.onMoreDataRequested(
                                        getSectionToUpdate()[mTopPagerSelectedButtonIndex],
                                        followersModel.getPage() + 1
                                );
                            }
                        });
                    }
                } else {
                    mPaginationContainer.setVisibility(View.GONE);
                }
            }

            if (mTopPagerSelectedButtonIndex == 0) {
                mTotalsLabel.setText(getTotalFollowersLabel(followersModel.getTotalWPCom()));
            } else {
                mTotalsLabel.setText(getTotalFollowersLabel(followersModel.getTotalEmail()));
            }
        } else {
            showEmptyUI(true);
            mPaginationContainer.setVisibility(View.GONE);
            mTotalsLabel.setText(getTotalFollowersLabel(0));
        }
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        if (mDatamodels == null) {
            return false;
        }
        FollowersModel followersModel = (FollowersModel) mDatamodels[mTopPagerSelectedButtonIndex];
        if (followersModel == null || followersModel.getFollowers() == null
                || followersModel.getFollowers().size() < 10) {
            return false;
        }

        return true;
    }

    private String getTotalFollowersLabel(int total) {
        if ( mTopPagerSelectedButtonIndex == 0 ) {
            return getString(R.string.stats_followers_total_wpcom) + " " + total;
        }

        return  getString(R.string.stats_followers_total_email) + " "  + total;
    }


    @Override
    protected boolean isExpandableList() {
        return false;
    }

    private class DotComFollowerAdapter extends ArrayAdapter<FollowerModel> {

        private final List<FollowerModel> list;
        private final Activity context;
        private final LayoutInflater inflater;

        public DotComFollowerAdapter(Activity context, List<FollowerModel> list) {
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

            final FollowerModel currentRowData = list.get(position);
            final StatsViewHolder holder = (StatsViewHolder) rowView.getTag();

            // entries
            if (mTopPagerSelectedButtonIndex == 0) {
                holder.setEntryTextOrLink(currentRowData.getURL(), currentRowData.getLabel());
            } else {
                holder.entryTextView.setText(currentRowData.getLabel());
            }

            // since date

            holder.totalsTextView.setText(getSinceLabel(currentRowData.getDateSubscribed()));

            // Avatar
            holder.showNetworkImage(currentRowData.getAvatar());

            final FollowDataModel followData = currentRowData.getFollowData();
            if (followData == null) {
                holder.imgMore.setVisibility(View.GONE);
                holder.imgMore.setOnClickListener(null);
            } else {
                holder.imgMore.setVisibility(View.VISIBLE);
                holder.imgMore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FollowHelper fh = new FollowHelper(context);
                        fh.showPopup(holder.imgMore, followData);
                    }
                });
            }

            // no icon
            holder.networkImageView.setVisibility(View.VISIBLE);

            return rowView;
        }

        private int roundUp(double num, double divisor) {
            double unrounded = num / divisor;
            return (int) (unrounded + 0.5);
        }

        private String getSinceLabel(String dataSubscribed) {

            Date currentDateTime = new Date();

            try {
                SimpleDateFormat from = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                Date date = from.parse(dataSubscribed);

                // See http://momentjs.com/docs/#/displaying/fromnow/
                long currentDifference = Math.abs(
                        StatsUtils.getDateDiff(date, currentDateTime, TimeUnit.SECONDS)
                );

                if (currentDifference <= 45 ) {
                    return "seconds ago";
                }
                if (currentDifference < 90 ) {
                    return "a minute ago";
                }

                // 90 seconds to 45 minutes
                if (currentDifference <= 2700 ) {
                    long minutes = this.roundUp(currentDifference, 60);
                    return minutes + " minutes";
                }

                // 45 to 90 minutes
                if (currentDifference <= 5400 ) {
                    return "an hour ago";
                }

                // 90 minutes to 22 hours
                if (currentDifference <= 79200 ) {
                    long hours = this.roundUp(currentDifference, 60*60);
                    return hours + " hours";
                }

                // 22 to 36 hours
                if (currentDifference <= 129600 ) {
                    return "A day";
                }

                // 36 hours to 25 days
                // 86400 secs in a day -  2160000 secs in 25 days
                if (currentDifference <= 2160000 ) {
                    long days = this.roundUp(currentDifference, 86400);
                    return days + " days";
                }

                // 25 to 45 days
                // 3888000 secs in 45 days
                if (currentDifference <= 3888000 ) {
                    return "A month";
                }

                // 45 to 345 days
                // 2678400 secs in a month - 29808000 secs in 345 days
                if (currentDifference <= 29808000 ) {
                    long months = this.roundUp(currentDifference, 2678400);
                    return months + " months";
                }

                // 345 to 547 days (1.5 years)
                if (currentDifference <= 47260800 ) {
                    return  "A year";
                }

                // 548 days+
                // 31536000 secs in a year
                long years = this.roundUp(currentDifference, 31536000);
                return years + " years";

            } catch (ParseException e) {
                AppLog.e(AppLog.T.STATS, e);
            }

            return "";
        }
    }

    @Override
    protected int getEntryLabelResId() {
        return R.string.stats_entry_followers;
    }

    @Override
    protected int getTotalsLabelResId() {
        return R.string.stats_totals_followers;
    }

    @Override
    protected int getEmptyLabelTitleResId() {
        return R.string.stats_empty_followers;
    }

    @Override
    protected int getEmptyLabelDescResId() {
        return R.string.stats_empty_followers_desc;
    }

    @Override
    protected StatsService.StatsEndpointsEnum[] getSectionToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.FOLLOWERS_WPCOM, StatsService.StatsEndpointsEnum.FOLLOWERS_EMAIL
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_followers);
    }
}
