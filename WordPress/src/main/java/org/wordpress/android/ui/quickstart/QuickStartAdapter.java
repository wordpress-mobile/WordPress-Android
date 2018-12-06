package org.wordpress.android.ui.quickstart;

import android.content.Context;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask;

import java.util.ArrayList;
import java.util.List;

public class QuickStartAdapter extends RecyclerView.Adapter<QuickStartAdapter.ViewHolder> {
    private Context mContext;
    private List<QuickStartTask> mTasks;
    private List<QuickStartTask> mTasksUncompleted;
    private OnTaskTappedListener mListener;

    QuickStartAdapter(Context context, List<QuickStartTask> tasksUncompleted, List<QuickStartTask> tasksCompleted) {
        mContext = context;
        mTasks = new ArrayList<>();
        mTasks.addAll(tasksUncompleted);
        mTasks.addAll(tasksCompleted);
        mTasksUncompleted = new ArrayList<>();
        mTasksUncompleted = tasksUncompleted;
    }

    @Override
    public void onBindViewHolder(@NonNull QuickStartAdapter.ViewHolder viewHolder, int position) {
        QuickStartTask task = mTasks.get(position);
        boolean isEnabled = mTasksUncompleted.contains(task);
        viewHolder.mIcon.setEnabled(isEnabled);
        viewHolder.mTitle.setEnabled(isEnabled);

        if (!isEnabled) {
            viewHolder.mTitle.setPaintFlags(viewHolder.mTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        switch (task) {
            case CREATE_SITE:
                viewHolder.mIcon.setImageResource(R.drawable.ic_plus_white_24dp);
                viewHolder.mTitle.setText(R.string.quick_start_list_create_site_title);
                viewHolder.mSubtitle.setText(R.string.quick_start_list_create_site_subtitle);
                break;
            case UPLOAD_SITE_ICON:
                viewHolder.mIcon.setImageResource(R.drawable.ic_globe_white_24dp);
                viewHolder.mTitle.setText(R.string.quick_start_list_upload_icon_title);
                viewHolder.mSubtitle.setText(R.string.quick_start_list_upload_icon_subtitle);
                break;
            case CHOOSE_THEME:
                viewHolder.mIcon.setImageResource(R.drawable.ic_themes_white_24dp);
                viewHolder.mTitle.setText(R.string.quick_start_list_browse_themes_title);
                viewHolder.mSubtitle.setText(R.string.quick_start_list_browse_themes_subtitle);
                break;
            case CUSTOMIZE_SITE:
                viewHolder.mIcon.setImageResource(R.drawable.ic_customize_white_24dp);
                viewHolder.mTitle.setText(R.string.quick_start_list_customize_site_title);
                viewHolder.mSubtitle.setText(R.string.quick_start_list_customize_site_subtitle);
                break;
            case CREATE_NEW_PAGE:
                viewHolder.mIcon.setImageResource(R.drawable.ic_pages_white_24dp);
                viewHolder.mTitle.setText(R.string.quick_start_list_create_page_title);
                viewHolder.mSubtitle.setText(R.string.quick_start_list_create_page_subtitle);
                break;
            case VIEW_SITE:
                viewHolder.mIcon.setImageResource(R.drawable.ic_external_white_24dp);
                viewHolder.mTitle.setText(R.string.quick_start_list_view_site_title);
                viewHolder.mSubtitle.setText(R.string.quick_start_list_view_site_subtitle);
                break;
            case ENABLE_POST_SHARING:
                viewHolder.mIcon.setImageResource(R.drawable.ic_share_white_24dp);
                viewHolder.mTitle.setText(R.string.quick_start_list_enable_sharing_title);
                viewHolder.mSubtitle.setText(R.string.quick_start_list_enable_sharing_subtitle);
                break;
            case PUBLISH_POST:
                viewHolder.mIcon.setImageResource(R.drawable.ic_create_white_24dp);
                viewHolder.mTitle.setText(R.string.quick_start_list_publish_post_title);
                viewHolder.mSubtitle.setText(R.string.quick_start_list_publish_post_subtitle);
                break;
            case FOLLOW_SITE:
                viewHolder.mIcon.setImageResource(R.drawable.ic_reader_follow_white_24dp);
                viewHolder.mTitle.setText(R.string.quick_start_list_follow_site_title);
                viewHolder.mSubtitle.setText(R.string.quick_start_list_follow_site_subtitle);
                break;
            case CHECK_STATS:
                viewHolder.mIcon.setImageResource(R.drawable.ic_stats_alt_white_24dp);
                viewHolder.mTitle.setText(R.string.quick_start_list_check_stats_title);
                viewHolder.mSubtitle.setText(R.string.quick_start_list_check_stats_subtitle);
                break;
            case EXPLORE_PLANS:
                viewHolder.mIcon.setImageResource(R.drawable.ic_plans_white_24dp);
                viewHolder.mTitle.setText(R.string.quick_start_list_explore_plans_title);
                viewHolder.mSubtitle.setText(R.string.quick_start_list_explore_plans_subtitle);
                break;
        }
    }

    @NonNull
    @Override
    public QuickStartAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        final View view = LayoutInflater.from(mContext).inflate(R.layout.quick_start_list_item, viewGroup, false);
        return new QuickStartAdapter.ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return mTasks.size();
    }

    public void setOnTaskTappedListener(OnTaskTappedListener listener) {
        mListener = listener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mIcon;
        TextView mSubtitle;
        TextView mTitle;

        public ViewHolder(final View inflate) {
            super(inflate);
            mIcon = inflate.findViewById(R.id.icon);
            mTitle = inflate.findViewById(R.id.title);
            mSubtitle = inflate.findViewById(R.id.subtitle);

            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        mListener.onTaskTapped(mTasks.get(getAdapterPosition()));
                    }
                }
            };

            itemView.setOnClickListener(listener);
        }
    }

    interface OnTaskTappedListener {
        void onTaskTapped(QuickStartTask task);
    }
}
