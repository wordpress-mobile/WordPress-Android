package org.wordpress.android.ui.activitylog

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.activity.ActivityLogModel

class ActivityLogDetailView(parentView: View) {
    private val actorIcon: ImageView by lazy {
        parentView.findViewById<ImageView>(R.id.activity_actor_icon)
    }
    private val actorName: TextView by lazy {
        parentView.findViewById<TextView>(R.id.activity_actor_name)
    }
    private val actorRole: TextView by lazy {
        parentView.findViewById<TextView>(R.id.activity_actor_role)
    }
    private val activityDate: TextView by lazy {
        parentView.findViewById<TextView>(R.id.activity_created_date)
    }
    private val activityTime: TextView by lazy {
        parentView.findViewById<TextView>(R.id.activity_created_time)
    }
    private val activityMessage: TextView by lazy {
        parentView.findViewById<TextView>(R.id.activity_message)
    }
    private val activityType: TextView by lazy {
        parentView.findViewById<TextView>(R.id.activity_type)
    }
    private val rewindButton: TextView by lazy {
        parentView.findViewById<Button>(R.id.activity_rewind_button)
    }

    fun show(activityLogModel: ActivityLogModel) {
    }
}
