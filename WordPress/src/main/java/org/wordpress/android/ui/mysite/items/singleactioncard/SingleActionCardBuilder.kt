package org.wordpress.android.ui.mysite.items.singleactioncard

import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.SingleActionCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SingleActionCardParams
import javax.inject.Inject

class SingleActionCardBuilder @Inject constructor() {
    fun build(params: SingleActionCardParams): SingleActionCard = with(params) {
        SingleActionCard(
                textResource = textResource,
                onActionClick = onActionClick
        )
    }
}
