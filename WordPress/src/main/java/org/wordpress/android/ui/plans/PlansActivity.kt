package org.wordpress.android.ui.plans

import android.os.Bundle
import kotlinx.android.synthetic.main.toolbar_main.*
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.plans.PlanOffersModel
import org.wordpress.android.ui.FullScreenDialogFragment
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.plans.PlansListFragment.PlansListInterface
import org.wordpress.android.util.StringUtils

class PlansActivity : LocaleAwareActivity(), PlansListInterface {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.plans_activity)

        setSupportActionBar(toolbar_main)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onPlanItemClicked(plan: PlanOffersModel) {
        val bundle = PlanDetailsFragment.newBundle(plan)
        val planDetailsDialog = FullScreenDialogFragment.Builder(this)
                .setTitle(StringUtils.notNullStr(plan.name))
                .setContent(PlanDetailsFragment::class.java, bundle)
                .build()
        planDetailsDialog!!.show(this.supportFragmentManager, FullScreenDialogFragment.TAG)
    }

    override fun onPlansUpdating() {
        val planDetailsDialogFragment = supportFragmentManager.findFragmentByTag(
                FullScreenDialogFragment.TAG
        )
        if (planDetailsDialogFragment != null && planDetailsDialogFragment is FullScreenDialogFragment) {
            planDetailsDialogFragment.dismissAllowingStateLoss()
        }
    }
}
