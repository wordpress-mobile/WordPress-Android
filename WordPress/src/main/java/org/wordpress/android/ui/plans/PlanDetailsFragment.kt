package org.wordpress.android.ui.plans

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
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
    private var plan: PlanOffersModel? = null
    @Inject lateinit var imageManager: ImageManager
    private lateinit var dialogController: FullScreenDialogController

    companion object {
        const val EXTRA_PLAN = "EXTRA_PLAN"
        const val KEY_PLAN = "KEY_PLAN"

        fun newBundle(planOffersModel: PlanOffersModel): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(EXTRA_PLAN, planOffersModel)
            return bundle
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (requireActivity().application as WordPress).component().inject(this)

        plan = if (savedInstanceState != null) {
            savedInstanceState.getParcelable(KEY_PLAN)
        } else {
            arguments?.getParcelable(EXTRA_PLAN)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_PLAN, plan)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.plan_details_fragment, container, false) as ViewGroup

        val planIcon = rootView.findViewById<ImageView>(R.id.image_plan_icon)
        val planName = rootView.findViewById<TextView>(R.id.plan_name)
        val planTagline = rootView.findViewById<TextView>(R.id.plan_tagline)
        val featuresContainer = rootView.findViewById<ViewGroup>(R.id.plan_features_container)

        if (!TextUtils.isEmpty(plan!!.iconUrl)) {
            imageManager.loadIntoCircle(
                    planIcon, ImageType.PLAN,
                    StringUtils.notNullStr(plan!!.iconUrl)
            )
        }

        planName.text = plan!!.name
        planTagline.text = plan!!.tagline

        plan!!.features?.forEach { feature ->
            val view = inflater.inflate(R.layout.plan_feature_item, featuresContainer, false) as ViewGroup

            val featureTitle = view.findViewById<TextView>(R.id.feature_title)
            val featureDescription = view.findViewById<TextView>(R.id.feature_description)

            featureTitle.text = feature.name
            featureDescription.text = feature.description

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

    override fun setController(controller: FullScreenDialogController) {
        dialogController = controller
    }
}
