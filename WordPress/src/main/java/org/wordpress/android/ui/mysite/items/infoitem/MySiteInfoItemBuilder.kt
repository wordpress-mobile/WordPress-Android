package org.wordpress.android.ui.mysite.items.infoitem

import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.utils.UiString
import javax.inject.Inject

class MySiteInfoItemBuilder @Inject constructor() {
    fun build(params: MySiteCardAndItemBuilderParams.InfoItemBuilderParams) =
        params.isStaleMessagePresent.takeIf { it }
            ?.let {
                MySiteCardAndItem.Item.InfoItem(title = UiString.UiStringRes(R.string.my_site_dashboard_stale_message))
            }
}
