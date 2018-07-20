package org.wordpress.android.fluxc.model

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.network.BaseRequest
import java.util.ArrayList

class PlansModel : Payload<BaseRequest.BaseNetworkError> {
    var sites: List<PlanModel>? = null

    constructor() {
        sites = ArrayList()
    }

    constructor(sites: List<PlanModel>) {
        this.sites = sites
    }
}
