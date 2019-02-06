package org.wordpress.android.ui.plans

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.plans.PlanOffersModel
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogContent
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogController
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import javax.inject.Inject

class PlanDetailsFragment : Fragment(), FullScreenDialogContent {
    private var planOffers: PlanOffersModel? = null
    @Inject lateinit var imageManager: ImageManager
    private lateinit var dialogController: FullScreenDialogController

    companion object {
        const val EXTRA_PLAN_OFFERS = "EXTRA_PLAN_OFFERS"
        const val KEY_PLAN_OFFERS = "KEY_PLAN_OFFERS"

        fun newBundle(planOffersModel: PlanOffersModel): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(EXTRA_PLAN_OFFERS, planOffersModel)
            return bundle
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (requireActivity().application as WordPress).component().inject(this)

        planOffers = if (savedInstanceState != null) {
            savedInstanceState.getParcelable(KEY_PLAN_OFFERS)
        } else {
            arguments?.getParcelable(EXTRA_PLAN_OFFERS)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.plan_fragment, container, false) as ViewGroup

        val planIcon = rootView.findViewById<ImageView>(R.id.image_plan_icon)
        val planName = rootView.findViewById<TextView>(R.id.text_product_name)
        val planTagline = rootView.findViewById<TextView>(R.id.text_tagline)

        if (!TextUtils.isEmpty(planOffers!!.iconUrl)) {
            imageManager.loadIntoCircle(
                    planIcon, ImageType.PLAN,
                    StringUtils.notNullStr(planOffers!!.iconUrl)
            )
        }

        planName.text = planOffers!!.name
        planTagline.text = planOffers!!.tagline

        val featuresContainer = rootView.findViewById<ViewGroup>(R.id.plan_container)

        planOffers!!.features?.forEach {
            val view = inflater.inflate(R.layout.plan_feature_item, featuresContainer, false) as ViewGroup

            val featureTitle = view.findViewById<TextView>(R.id.item_title)
            val featureDescription = view.findViewById<TextView>(R.id.item_subtitle)

            featureTitle.text = it.name
            featureDescription.text = it.description

            featuresContainer.addView(view)
        }

        return rootView
    }

    override fun onConfirmClicked(controller: FullScreenDialogController): Boolean {
        return true
    }

    override fun onDismissClicked(controller: FullScreenDialogController): Boolean {
        dialogController.dismiss()
        return true
    }

    override fun onViewCreated(controller: FullScreenDialogController) {
        dialogController = controller
    }
}
