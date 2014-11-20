package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.model.SingleItemModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.StringUtils;

import java.util.List;


public class StatsPublicizeFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsPublicizeFragment.class.getSimpleName();

    @Override
    protected void updateUI() {
        if (mDatamodel != null && ((List<SingleItemModel>) mDatamodel).size() > 0) {
            List<SingleItemModel> publicizeItems = ((List<SingleItemModel>) mDatamodel);
            ArrayAdapter adapter = new PublicizeAdapter(getActivity(), publicizeItems);
            StatsUIHelper.reloadLinearLayout(getActivity(), adapter, mList, getMaxNumberOfItemsToShowInList());
            showEmptyUI(false);
        } else {
            showEmptyUI(true);
        }
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        return false;
    }

    @Override
    protected boolean isExpandableList() {
        return false;
    }

    private class PublicizeAdapter extends ArrayAdapter<SingleItemModel> {

        private final List<SingleItemModel> list;
        private final Activity context;
        private final LayoutInflater inflater;

        public PublicizeAdapter(Activity context, List<SingleItemModel> list) {
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

            String serviceName = currentRowData.getTitle();

            // entries
            holder.entryTextView.setText(getServiceName(serviceName));

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(currentRowData.getTotals()));

            // image
            holder.showNetworkImage(getServiceImage(serviceName));
            holder.networkImageView.setVisibility(View.VISIBLE);

            return rowView;
        }
    }

    private String getServiceImage(String service) {
        if (service.equals("facebook")) {
            return "https://secure.gravatar.com/blavatar/2343ec78a04c6ea9d80806345d31fd78?s=128";
        }

        if (service.equals("twitter")) {
            return "https://secure.gravatar.com/blavatar/7905d1c4e12c54933a44d19fcd5f9356?s=128";
        }

        if (service.equals("tumblr")) {
            return "https://secure.gravatar.com/blavatar/84314f01e87cb656ba5f382d22d85134?s=128";
        }

        if (service.equals("google_plus")) {
            return "https://secure.gravatar.com/blavatar/4a4788c1dfc396b1f86355b274cc26b3?s=128";
        }

        if (service.equals("linkedin")) {
            return "https://secure.gravatar.com/blavatar/f54db463750940e0e7f7630fe327845e?s=128";
        }

        if (service.equals("path")) {
            return "https://secure.gravatar.com/blavatar/3a03c8ce5bf1271fb3760bb6e79b02c1?s=128";
        }

        return null;
    }

    private String getServiceName(String service) {
        if (service.equals("facebook")) {
            return "Facebook";
        }

        if (service.equals("twitter")) {
            return "Twitter";
        }

        if (service.equals("tumblr")) {
            return "Tumblr";
        }

        if (service.equals("google_plus")) {
            return "Google+";
        }

        if (service.equals("linkedin")) {
            return "LinkedIn";
        }

        if (service.equals("path")) {
            return "Path";
        }

        return StringUtils.capitalize(service);
    }


    @Override
    protected int getEntryLabelResId() {
        return R.string.stats_entry_publicize;
    }

    @Override
    protected int getTotalsLabelResId() {
        return R.string.stats_totals_publicize;
    }

    @Override
    protected int getEmptyLabelTitleResId() {
        return R.string.stats_empty_publicize;
    }

    @Override
    protected int getEmptyLabelDescResId() {
        return R.string.stats_empty_publicize_desc;
    }

    @Override
    protected StatsService.StatsEndpointsEnum getSectionToUpdate() {
        return StatsService.StatsEndpointsEnum.PUBLICIZE;
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_publicize);
    }
}
