package org.wordpress.android.ui.quickstart;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask;
import org.wordpress.android.util.AniUtils.Duration;
import org.wordpress.android.util.ViewUtilsKt;

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

            int topMargin = mTasksUncompleted.size() > 0
                    ? mContext.getResources().getDimensionPixelSize(R.dimen.margin_extra_large)
                    : 0;
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            params.setMargins(0, topMargin, 0, 0);
            headerViewHolder.itemView.setLayoutParams(params);
            return;
        }

        TaskViewHolder taskViewHolder = (TaskViewHolder) viewHolder;

        QuickStartTask task = mTasks.get(position);
        boolean isEnabled = mTasksUncompleted.contains(task);
        taskViewHolder.mIcon.setEnabled(isEnabled);
        taskViewHolder.mTitle.setEnabled(isEnabled);
        taskViewHolder.itemView.setLongClickable(isEnabled);

        if (!isEnabled) {
            taskViewHolder.mTitle.setPaintFlags(taskViewHolder.mTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        // Hide divider for tasks before header and end of list.
        if (position == mTasksUncompleted.size() - 1 || position == mTasks.size() - 1) {
            taskViewHolder.mDivider.setVisibility(View.INVISIBLE);
        } else {
            taskViewHolder.mDivider.setVisibility(View.VISIBLE);
        }

        QuickStartTaskDetails quickStartTaskDetails = QuickStartTaskDetails.getDetailsForTask(task);

        if (quickStartTaskDetails == null) {
            throw new IllegalStateException(task.toString() + " task is not recognized in adapter.");
        }

        taskViewHolder.mIcon.setImageResource(quickStartTaskDetails.getIconResId());
        taskViewHolder.mTitle.setText(quickStartTaskDetails.getTitleResId());
        taskViewHolder.mSubtitle.setText(quickStartTaskDetails.getSubtitleResId());
    }

    void updateContent(List<QuickStartTask> tasksUncompleted, List<QuickStartTask> tasksCompleted) {
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

        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(new QuickStartTasksDiffCallback(mTasks, newList));

        mTasks.clear();
        mTasks.addAll(newList);

        diffResult.dispatchUpdatesTo(this);

        // Notify adapter of each task change individually.  Using notifyDataSetChanged() kills list changing animation.
        for (QuickStartTask task : mTasks) {
            notifyItemChanged(mTasks.indexOf(task));
        }
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

    void setOnTaskTappedListener(OnQuickStartAdapterActionListener listener) {
        mListener = listener;
    }

    boolean isCompletedTasksListExpanded() {
        return mIsCompletedTaskListExpanded;
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

            View.OnClickListener clickListener = view -> {
                if (mListener != null) {
                    mListener.onTaskTapped(mTasks.get(getAdapterPosition()));
                }
            };

            View.OnLongClickListener longClickListener = v -> {
                PopupMenu popup = new PopupMenu(mContext, mPopupAnchor);
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.quick_start_task_menu_skip) {
                        if (mListener != null) {
                            mListener.onSkipTaskTapped(mTasks.get(getAdapterPosition()));
                        }
                        return true;
                    }
                    return false;
                });
                popup.inflate(R.menu.quick_start_task_menu);
                popup.show();
                return true;
            };

            itemView.setOnClickListener(clickListener);
            itemView.setOnLongClickListener(longClickListener);
            ViewUtilsKt.redirectContextClickToLongPressListener(itemView);
        }
    }

    public class CompletedHeaderViewHolder extends RecyclerView.ViewHolder {
        ImageView mChevron;
        TextView mTitle;

        CompletedHeaderViewHolder(final View inflate) {
            super(inflate);
            mChevron = inflate.findViewById(R.id.completed_tasks_header_chevron);
            mTitle = inflate.findViewById(R.id.completed_tasks_header_title);

            View.OnClickListener clickListener = view -> toggleCompletedTasksList();

            itemView.setOnClickListener(clickListener);
        }

        private void toggleCompletedTasksList() {
            ViewPropertyAnimator viewPropertyAnimator = mChevron
                    .animate()
                    .rotation(mIsCompletedTaskListExpanded ? COLLAPSED_CHEVRON_ROTATION : EXPANDED_CHEVRON_ROTATION)
                    .setInterpolator(new LinearInterpolator())
                    .setDuration(Duration.SHORT.toMillis(mContext));

            viewPropertyAnimator.setListener(new AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    itemView.setEnabled(false);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    int positionOfHeader = getAdapterPosition();
                    int positionAfterHeader = positionOfHeader + 1;

                    if (mIsCompletedTaskListExpanded) {
                        mTasks.removeAll(mTaskCompleted);
                        notifyItemRangeRemoved(positionAfterHeader, mTaskCompleted.size());
                    } else {
                        mTasks.addAll(mTaskCompleted);
                        notifyItemRangeInserted(positionAfterHeader, mTaskCompleted.size());
                    }

                    // Update header background based after collapsed and expanded.
                    notifyItemChanged(positionOfHeader);
                    mIsCompletedTaskListExpanded = !mIsCompletedTaskListExpanded;
                    itemView.setEnabled(true);

                    if (mListener != null) {
                        mListener.onCompletedTasksListToggled(mIsCompletedTaskListExpanded);
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    itemView.setEnabled(true);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
        }
    }

    interface OnQuickStartAdapterActionListener {
        void onSkipTaskTapped(QuickStartTask task);

        void onTaskTapped(QuickStartTask task);

        void onCompletedTasksListToggled(boolean isExpanded);
    }
}
