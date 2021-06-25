package org.wordpress.android.ui.comments.unified

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.comments.unified.CommentListLoadingStateAdapter.LoadStateViewHolder

class CommentListLoadingStateAdapter(private val retry: () -> Unit) : LoadStateAdapter<LoadStateViewHolder>() {
    override fun onBindViewHolder(holder: LoadStateViewHolder, loadState: LoadState) = holder.bind(loadState, retry)

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState) = LoadStateViewHolder(parent)

    class LoadStateViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.load_or_action_item, parent, false)
    ) {
        private val progress = itemView.findViewById<View>(R.id.progress)
        private val actionButton = itemView.findViewById<Button>(R.id.action_button)

        fun bind(loadState: LoadState, retry: () -> Unit) {
            if (loadState is LoadState.Error) {
                progress.visibility = View.GONE
                actionButton.visibility = View.VISIBLE
                actionButton.setOnClickListener {
                    retry()
                }
            } else {
                progress.visibility = View.VISIBLE
                actionButton.visibility = View.GONE
            }
        }
    }
}
