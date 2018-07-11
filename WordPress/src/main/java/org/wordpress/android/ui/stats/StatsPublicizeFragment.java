package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.PublicizeModel;
import org.wordpress.android.ui.stats.models.SingleItemModel;
import org.wordpress.android.ui.stats.service.StatsServiceLogic;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.image.ImageType;

import java.util.List;


public class StatsPublicizeFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsPublicizeFragment.class.getSimpleName();

    private PublicizeModel mPublicizeData;

    @Override
    protected boolean hasDataAvailable() {
        return mPublicizeData != null;
    }

    @Override
    protected void saveStatsData(Bundle outState) {
        if (mPublicizeData != null) {
            outState.putSerializable(ARG_REST_RESPONSE, mPublicizeData);
        }
    }

    @Override
    protected void restoreStatsData(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
            mPublicizeData = (PublicizeModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.PublicizeUpdated event) {
        if (!shouldUpdateFragmentOnUpdateEvent(event)) {
            return;
        }

        mPublicizeData = event.mPublicizeModel;
        updateUI();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.SectionUpdateError event) {
        if (!shouldUpdateFragmentOnErrorEvent(event)) {
            return;
        }

        mPublicizeData = null;
        showErrorUI(event.mError);
    }

    @Override
    protected void updateUI() {
        if (!isAdded()) {
            return;
        }

        if (hasPublicize()) {
            ArrayAdapter adapter = new PublicizeAdapter(getActivity(), getPublicize());
            StatsUIHelper.reloadLinearLayout(getActivity(), adapter, mList, getMaxNumberOfItemsToShowInList());
            showHideNoResultsUI(false);
        } else {
            showHideNoResultsUI(true);
        }
    }

    private boolean hasPublicize() {
        return mPublicizeData != null
               && mPublicizeData.getServices() != null
               && mPublicizeData.getServices().size() > 0;
    }

    private List<SingleItemModel> getPublicize() {
        if (!hasPublicize()) {
            return null;
        }
        return mPublicizeData.getServices();
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
        private final List<SingleItemModel> mList;
        private final LayoutInflater mInflater;

        PublicizeAdapter(Activity context, List<SingleItemModel> list) {
            super(context, R.layout.stats_list_cell, list);
            mList = list;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            // reuse views
            final StatsViewHolder holder;
            if (rowView == null) {
                rowView = mInflater.inflate(R.layout.stats_list_cell, parent, false);
                // configure view holder
                holder = new StatsViewHolder(rowView);
                rowView.setTag(holder);
            } else {
                holder = (StatsViewHolder) rowView.getTag();
            }

            final SingleItemModel currentRowData = mList.get(position);

            String serviceName = currentRowData.getTitle();

            // entries
            holder.setEntryText(getServiceName(serviceName));

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(currentRowData.getTotals()));
            holder.totalsTextView.setContentDescription(
                    org.wordpress.android.util.StringUtils.getQuantityString(
                            holder.totalsTextView.getContext(),
                            R.string.stats_followers_zero_desc,
                            R.string.stats_followers_one_desc,
                            R.string.stats_followers_many_desc,
                            currentRowData.getTotals()));

            // image
            mImageManager.load(holder.networkImageView, ImageType.BLAVATAR,
                    GravatarUtils.fixGravatarUrl(getServiceImage(serviceName), mResourceVars.mHeaderAvatarSizePx));
            holder.networkImageView.setVisibility(View.VISIBLE);

            return rowView;
        }
    }

    private String getServiceImage(String service) {
        String serviceIconURL;

        switch (service) {
            case "facebook":
                serviceIconURL = "https://secure.gravatar.com/blavatar/2343ec78a04c6ea9d80806345d31fd78?s=";
                break;
            case "twitter":
                serviceIconURL = "https://secure.gravatar.com/blavatar/7905d1c4e12c54933a44d19fcd5f9356?s=";
                break;
            case "tumblr":
                serviceIconURL = "https://secure.gravatar.com/blavatar/84314f01e87cb656ba5f382d22d85134?s=";
                break;
            case "google_plus":
                serviceIconURL = "https://secure.gravatar.com/blavatar/4a4788c1dfc396b1f86355b274cc26b3?s=";
                break;
            case "linkedin":
                serviceIconURL = "https://secure.gravatar.com/blavatar/f54db463750940e0e7f7630fe327845e?s=";
                break;
            case "path":
                serviceIconURL = "https://secure.gravatar.com/blavatar/3a03c8ce5bf1271fb3760bb6e79b02c1?s=";
                break;
            default:
                return null;
        }

        return serviceIconURL + mResourceVars.mHeaderAvatarSizePx;
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
    protected StatsServiceLogic.StatsEndpointsEnum[] sectionsToUpdate() {
        return new StatsServiceLogic.StatsEndpointsEnum[]{
                StatsServiceLogic.StatsEndpointsEnum.PUBLICIZE
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_publicize);
    }
}
