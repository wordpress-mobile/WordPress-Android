package org.wordpress.android.ui.plans.models;

import org.wordpress.android.ui.plans.PlansUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SitePlanList extends ArrayList<SitePlan> {

    /*
     * sort the list by price
     */
    public void sortPlans() {
        Collections.sort(this, new Comparator<SitePlan>() {
            @Override
            public int compare(SitePlan lhs, SitePlan rhs) {
                return PlansUtils.compareProducts(lhs.getProductID(), rhs.getProductID());
            }
        });
    }
}
