package org.wordpress.android.ui.quickstart;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask;
import org.wordpress.android.util.AniUtils.Duration;

import java.util.ArrayList;
import java.util.List;

public class QuickStartAdapter extends RecyclerView.Adapter<ViewHolder> {
    private Context mContext;
    private List<QuickStartTask> mTasks;
    private List<QuickStartTask> mTasksUncompleted;
    private List<QuickStartTask> mTaskCompleted;
    private OnTaskTappedListener mListener;
    private boolean mIsCompletedTaskListExpanded;

    private static final int VIEW_TYPE_TASK = 0;
    private static final int VIEW_TYPE_COMPLETE_TASKS_HEADER = 1;

    QuickStartAdapter(Context context, List<QuickStartTask> tasksUncompleted, List<QuickStartTask> tasksCompleted,
                      boolean isCompletedTasksListExpanded) {
        mContext = context;
        mTasks = new ArrayList<>();
        mTasks.addAll(tasksUncompleted);
        if (!tasksCompleted.isEmpty()) {
            mTasks.add(null); // adding null where the complete tasks header simplifies a lot of logic for us
        }
        mIsCompletedTaskListExpanded = isCompletedTasksListExpanded;
        if (mIsCompletedTaskListExpanded) {
            mTasks.addAll(tasksCompleted);
        }
        mTasksUncompleted = tasksUncompleted;
        mTaskCompleted = tasksCompleted;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);

        switch (viewType) {
            case VIEW_TYPE_TASK:
                return new TaskViewHolder(
                        inflater.inflate(R.layout.quick_start_list_item, viewGroup, false));
            case VIEW_TYPE_COMPLETE_TASKS_HEADER:
                return new CompletedHeaderViewHolder(
                        inflater.inflate(R.layout.quick_start_completed_tasks_list_header, viewGroup, false));
            default:
                throw new IllegalArgumentException("Unexpected view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        int viewType = getItemViewType(position);

        if (viewType == VIEW_TYPE_COMPLETE_TASKS_HEADER) {
            CompletedHeaderViewHolder headerViewHolder = (CompletedHeaderViewHolder) viewHolder;
            headerViewHolder.mTitle.setText(mContext.getString(R.string.quick_start_complete_tasks_header,
                    mTaskCompleted.size()));

            if (mIsCompletedTaskListExpanded) {
                headerViewHolder.mChevron.setImageResource(R.drawable.ic_chevron_down_blue_wordpress_24dp);
                headerViewHolder.mChevron.setContentDescription(
                        mContext.getString(R.string.quick_start_completed_tasks_header_chevron_collapse_desc));
            } else {
                headerViewHolder.mChevron.setImageResource(R.drawable.ic_chevron_up_blue_wordpress_24dp);
                headerViewHolder.mChevron.setContentDescription(
                        mContext.getString(R.string.quick_start_completed_tasks_header_chevron_expand_desc));
            }
            return;
        }

        TaskViewHolder taskViewHolder = (TaskViewHolder) viewHolder;

        QuickStartTask task = mTasks.get(position);
        boolean isEnabled = mTasksUncompleted.contains(task);
        taskViewHolder.mIcon.setEnabled(isEnabled);
        taskViewHolder.mTitle.setEnabled(isEnabled);

        if (!isEnabled) {
            taskViewHolder.mTitle.setPaintFlags(taskViewHolder.mTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        // hide divider for the task before completed header
        if (position == mTasksUncompleted.size() - 1) {
            taskViewHolder.mDivider.setVisibility(View.INVISIBLE);
        } else {
            taskViewHolder.mDivider.setVisibility(View.VISIBLE);
        }

        switch (task) {
            case CREATE_SITE:
                taskViewHolder.mIcon.setImageResource(R.drawable.ic_plus_white_24dp);
                taskViewHolder.mTitle.setText(R.string.quick_start_list_create_site_title);
                taskViewHolder.mSubtitle.setText(R.string.quick_start_list_create_site_subtitle);
                break;
            case UPLOAD_SITE_ICON:
                taskViewHolder.mIcon.setImageResource(R.drawable.ic_globe_white_24dp);
                taskViewHolder.mTitle.setText(R.string.quick_start_list_upload_icon_title);
                taskViewHolder.mSubtitle.setText(R.string.quick_start_list_upload_icon_subtitle);
                break;
            case CHOOSE_THEME:
                taskViewHolder.mIcon.setImageResource(R.drawable.ic_themes_white_24dp);
                taskViewHolder.mTitle.setText(R.string.quick_start_list_browse_themes_title);
                taskViewHolder.mSubtitle.setText(R.string.quick_start_list_browse_themes_subtitle);
                break;
            case CUSTOMIZE_SITE:
                taskViewHolder.mIcon.setImageResource(R.drawable.ic_customize_white_24dp);
                taskViewHolder.mTitle.setText(R.string.quick_start_list_customize_site_title);
                taskViewHolder.mSubtitle.setText(R.string.quick_start_list_customize_site_subtitle);
                break;
            case CREATE_NEW_PAGE:
                taskViewHolder.mIcon.setImageResource(R.drawable.ic_pages_white_24dp);
                taskViewHolder.mTitle.setText(R.string.quick_start_list_create_page_title);
                taskViewHolder.mSubtitle.setText(R.string.quick_start_list_create_page_subtitle);
                break;
            case VIEW_SITE:
                taskViewHolder.mIcon.setImageResource(R.drawable.ic_external_white_24dp);
                taskViewHolder.mTitle.setText(R.string.quick_start_list_view_site_title);
                taskViewHolder.mSubtitle.setText(R.string.quick_start_list_view_site_subtitle);
                break;
            case ENABLE_POST_SHARING:
                taskViewHolder.mIcon.setImageResource(R.drawable.ic_share_white_24dp);
                taskViewHolder.mTitle.setText(R.string.quick_start_list_enable_sharing_title);
                taskViewHolder.mSubtitle.setText(R.string.quick_start_list_enable_sharing_subtitle);
                break;
            case PUBLISH_POST:
                taskViewHolder.mIcon.setImageResource(R.drawable.ic_create_white_24dp);
                taskViewHolder.mTitle.setText(R.string.quick_start_list_publish_post_title);
                taskViewHolder.mSubtitle.setText(R.string.quick_start_list_publish_post_subtitle);
                break;
            case FOLLOW_SITE:
                taskViewHolder.mIcon.setImageResource(R.drawable.ic_reader_follow_white_24dp);
                taskViewHolder.mTitle.setText(R.string.quick_start_list_follow_site_title);
                taskViewHolder.mSubtitle.setText(R.string.quick_start_list_follow_site_subtitle);
                break;
            case CHECK_STATS:
                taskViewHolder.mIcon.setImageResource(R.drawable.ic_stats_alt_white_24dp);
                taskViewHolder.mTitle.setText(R.string.quick_start_list_check_stats_title);
                taskViewHolder.mSubtitle.setText(R.string.quick_start_list_check_stats_subtitle);
                break;
            case EXPLORE_PLANS:
                taskViewHolder.mIcon.setImageResource(R.drawable.ic_plans_white_24dp);
                taskViewHolder.mTitle.setText(R.string.quick_start_list_explore_plans_title);
                taskViewHolder.mSubtitle.setText(R.string.quick_start_list_explore_plans_subtitle);
                break;
        }
    }

    @Override public int getItemViewType(int position) {
        if (position == mTasksUncompleted.size()) {
            return VIEW_TYPE_COMPLETE_TASKS_HEADER;
        } else {
            return VIEW_TYPE_TASK;
        }
    }

    @Override
    public int getItemCount() {
        return mTasks.size();
    }

    public void setOnTaskTappedListener(OnTaskTappedListener listener) {
        mListener = listener;
    }

    public class TaskViewHolder extends RecyclerView.ViewHolder {
        ImageView mIcon;
        TextView mSubtitle;
        TextView mTitle;
        View mDivider;

        TaskViewHolder(final View inflate) {
            super(inflate);
            mIcon = inflate.findViewById(R.id.icon);
            mTitle = inflate.findViewById(R.id.title);
            mSubtitle = inflate.findViewById(R.id.subtitle);
            mDivider = inflate.findViewById(R.id.divider);

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

    public class CompletedHeaderViewHolder extends RecyclerView.ViewHolder {
        ImageView mChevron;
        TextView mTitle;

        CompletedHeaderViewHolder(final View inflate) {
            super(inflate);
            mChevron = inflate.findViewById(R.id.completed_tasks_list_chevron);
            mTitle = inflate.findViewById(R.id.complete_tasks_header_label);

            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    toggleCompletedTasksList();
                }
            };

            itemView.setOnClickListener(listener);
        }

        private void toggleCompletedTasksList() {
            ViewPropertyAnimator viewPropertyAnimator =
                    mChevron.animate()
                            .rotation(mIsCompletedTaskListExpanded ? 0f : 180f)
                            .setInterpolator(new LinearInterpolator())
                            .setDuration(Duration.SHORT.toMillis(mContext));

            viewPropertyAnimator.setListener(new AnimatorListener() {
                @Override public void onAnimationStart(Animator animation) {
                    itemView.setClickable(false);
                }

                @Override public void onAnimationEnd(Animator animation) {
                    if (mIsCompletedTaskListExpanded) {
                        mTasks.removeAll(mTaskCompleted);
                        notifyItemRangeRemoved(getAdapterPosition() + 1, mTaskCompleted.size());
                    } else {
                        mTasks.addAll(mTaskCompleted);
                        notifyItemRangeInserted(getAdapterPosition() + 1, mTaskCompleted.size());
                    }
                    mIsCompletedTaskListExpanded = !mIsCompletedTaskListExpanded;
                    itemView.setClickable(true);
                }

                @Override public void onAnimationCancel(Animator animation) {
                    itemView.setClickable(true);
                }

                @Override public void onAnimationRepeat(Animator animation) {
                }
            });
        }
    }

    interface OnTaskTappedListener {
        void onTaskTapped(QuickStartTask task);
    }

    public boolean isCompletedTasksListExpanded() {
        return mIsCompletedTaskListExpanded;
    }
}
