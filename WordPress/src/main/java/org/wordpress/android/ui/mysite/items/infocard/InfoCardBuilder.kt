package org.wordpress.android.ui.mysite.items.infocard

import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.InfoCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.InfoCardParams
import javax.inject.Inject

class InfoCardBuilder @Inject constructor() {
    fun build(params: InfoCardParams): InfoCard = with(params) {
        InfoCard(
                infoResource = infoResource,
                onInfoClick = onInfoClick
        )
    }
}
