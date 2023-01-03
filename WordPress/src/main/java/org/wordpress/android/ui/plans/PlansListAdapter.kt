package org.wordpress.android.ui.plans

import android.app.Activity
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.plans.PlanOffersModel
import org.wordpress.android.ui.plans.PlansListAdapter.PlansItemViewHolder
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import javax.inject.Inject

class PlansListAdapter(
    val activity: Activity,
    private val itemClickListener: (PlanOffersModel) -> Unit
) : Adapter<PlansItemViewHolder>() {
    private val list = mutableListOf<PlanOffersModel>()

    @Inject
    lateinit var imageManager: ImageManager

    init {
        (activity.applicationContext as WordPress).component().inject(this)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: PlansItemViewHolder, position: Int, payloads: List<Any>) {
        onBindViewHolder(holder, position)
    }

    override fun onBindViewHolder(holder: PlansItemViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlansItemViewHolder {
        return PlansItemViewHolder(parent, itemClickListener, imageManager)
    }

    internal fun updateList(items: List<PlanOffersModel>) {
        list.clear()
        list.addAll(items)
        notifyDataSetChanged()
    }

    class PlansItemViewHolder(
        parent: ViewGroup,
        private val itemClickListener: (PlanOffersModel) -> Unit,
        val imageManager: ImageManager
    ) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.plans_list_item, parent, false)
    ) {
        private val planImage: ImageView = itemView.findViewById(R.id.plan_image)
        private val title: TextView = itemView.findViewById(R.id.plan_title)
        private val subtitle: TextView = itemView.findViewById(R.id.plan_subtitle)

        fun bind(planOffersModel: PlanOffersModel) {
            itemView.setOnClickListener {
                itemView.performAccessibilityAction(
                    AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null
                )
                itemClickListener(planOffersModel)
            }
            title.text = planOffersModel.name
            subtitle.text = planOffersModel.tagline

            if (!TextUtils.isEmpty(planOffersModel.iconUrl)) {
                imageManager.loadIntoCircle(
                    planImage, ImageType.PLAN,
                    StringUtils.notNullStr(planOffersModel.iconUrl)
                )
            }
        }
    }
}
