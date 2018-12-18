package org.wordpress.android.ui.quickstart;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
    private boolean mIsCompletedTaskListExpanded;
    private OnQuickStartAdapterActionListener mListener;

    private static final int VIEW_TYPE_TASK = 0;
    private static final int VIEW_TYPE_COMPLETED_TASKS_HEADER = 1;

    private static final float EXPANDED_CHEVRON_ROTATION = -180;
    private static final float COLLAPSED_CHEVRON_ROTATION = 0;

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
            case VIEW_TYPE_COMPLETED_TASKS_HEADER:
                return new CompletedHeaderViewHolder(
                        inflater.inflate(R.layout.quick_start_completed_tasks_list_header, viewGroup, false));
            default:
                throw new IllegalArgumentException("Unexpected view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        int viewType = getItemViewType(position);

        if (viewType == VIEW_TYPE_COMPLETED_TASKS_HEADER) {
            CompletedHeaderViewHolder headerViewHolder = (CompletedHeaderViewHolder) viewHolder;
            headerViewHolder.mTitle.setText(mContext.getString(R.string.quick_start_complete_tasks_header,
                    mTaskCompleted.size()));

            if (mIsCompletedTaskListExpanded) {
                headerViewHolder.mChevron.setRotation(EXPANDED_CHEVRON_ROTATION);
                headerViewHolder.mChevron.setContentDescription(
                        mContext.getString(R.string.quick_start_completed_tasks_header_chevron_collapse_desc));
            } else {
                headerViewHolder.mChevron.setRotation(COLLAPSED_CHEVRON_ROTATION);
                headerViewHolder.mChevron.setContentDescription(
                        mContext.getString(R.string.quick_start_completed_tasks_header_chevron_expand_desc));
            }

            headerViewHolder.mTopSpacing.setVisibility(mTasksUncompleted.isEmpty() ? View.GONE : View.VISIBLE);
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

         taskViewHolder.itemView.setLongClickable(isEnabled);

        // Hide divider for tasks before header and end of list.
        if (position == mTasksUncompleted.size() - 1 || position == mTasks.size() - 1) {
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

    void updateContent(QuickStartTask task, List<QuickStartTask> tasksUncompleted,
                       List<QuickStartTask> tasksCompleted) {
        List<QuickStartTask> newList = new ArrayList<>(tasksUncompleted);
        if (!tasksCompleted.isEmpty()) {
            newList.add(null);
        }
        if (mIsCompletedTaskListExpanded) {
            newList.addAll(tasksCompleted);
        }

        mTaskCompleted.clear();
        mTaskCompleted.addAll(tasksCompleted);

        mTasksUncompleted.clear();
        mTasksUncompleted.addAll(tasksUncompleted);

        DiffUtil.DiffResult result =
                DiffUtil.calculateDiff(new QuickStartTasksDiffCallback(mTasks, newList));

        mTasks.clear();
        mTasks.addAll(newList);

        result.dispatchUpdatesTo(this);
        notifyItemChanged(mTasks.indexOf(task));
        notifyItemChanged(mTasks.indexOf(null));

        notifyItemChanged(mTasks.indexOf(null));
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mTasksUncompleted.size()) {
            return VIEW_TYPE_COMPLETED_TASKS_HEADER;
        } else {
            return VIEW_TYPE_TASK;
        }
    }

    @Override
    public int getItemCount() {
        return mTasks.size();
    }

    public void setOnTaskTappedListener(OnQuickStartAdapterActionListener listener) {
        mListener = listener;
    }

    public class TaskViewHolder extends RecyclerView.ViewHolder {
        ImageView mIcon;
        TextView mSubtitle;
        TextView mTitle;
        View mDivider;
        View mPopupAnchor;

        TaskViewHolder(final View inflate) {
            super(inflate);
            mIcon = inflate.findViewById(R.id.icon);
            mTitle = inflate.findViewById(R.id.title);
            mSubtitle = inflate.findViewById(R.id.subtitle);
            mDivider = inflate.findViewById(R.id.divider);
            mPopupAnchor = inflate.findViewById(R.id.popup_anchor);

            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        mListener.onTaskTapped(mTasks.get(getAdapterPosition()));
                    }
                }
            };

            View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    PopupMenu popup = new PopupMenu(mContext, mPopupAnchor);

                    popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override public boolean onMenuItemClick(MenuItem item) {
                            if (item.getItemId() == R.id.quick_start_task_menu_skip) {
                                if (mListener != null) {
                                    mListener.onSkipTaskTapped(mTasks.get(getAdapterPosition()));
                                }
                                return true;
                            }
                            return false;
                        }
                    });

                    popup.inflate(R.menu.quick_start_task_menu);
                    popup.setGravity(Gravity.START);

                    popup.show();
                    return true;
                }
            };

            itemView.setOnClickListener(listener);
            itemView.setOnLongClickListener(longClickListener);
        }
    }

    public class CompletedHeaderViewHolder extends RecyclerView.ViewHolder {
        ImageView mChevron;
        TextView mTitle;
        View mTopSpacing;

        CompletedHeaderViewHolder(final View inflate) {
            super(inflate);
            mChevron = inflate.findViewById(R.id.completed_tasks_list_chevron);
            mTitle = inflate.findViewById(R.id.complete_tasks_header_label);
            mTopSpacing = inflate.findViewById(R.id.top_spacing);

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
                            .rotation(mIsCompletedTaskListExpanded ? COLLAPSED_CHEVRON_ROTATION
                                    : EXPANDED_CHEVRON_ROTATION)
                            .setInterpolator(new LinearInterpolator())
                            .setDuration(Duration.SHORT.toMillis(mContext));

            viewPropertyAnimator.setListener(new AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    itemView.setClickable(false);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
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

                @Override
                public void onAnimationCancel(Animator animation) {
                    itemView.setClickable(true);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
        }
    }

    interface OnQuickStartAdapterActionListener {
        void onTaskTapped(QuickStartTask task);

        void onSkipTaskTapped(QuickStartTask task);
    }

    boolean isCompletedTasksListExpanded() {
        return mIsCompletedTaskListExpanded;
    }
}
