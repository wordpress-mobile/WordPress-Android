package org.wordpress.android.fluxc.model.dashboard

import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.CardsResponse
import javax.inject.Inject

class CardsMapper @Inject constructor() {
    fun map(response: CardsResponse): CardsModel = CardsModel(response.todo)
}
