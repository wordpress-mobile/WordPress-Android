package org.wordpress.android.ui.mysite.cards.personalize

import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PersonalizeCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PersonalizeCardBuilderParams
import org.wordpress.android.util.config.DashboardPersonalizationFeatureConfig
import javax.inject.Inject

class PersonalizeCardBuilder @Inject constructor(
    private val dashboardPersonalizationFeatureConfig: DashboardPersonalizationFeatureConfig
) {
    fun build(params: PersonalizeCardBuilderParams) : PersonalizeCardModel? {
        return if (shouldBuildCard()) {
            getPersonalizeCardModel(params)
        } else {
            null
        }
    }

    private fun getPersonalizeCardModel(params: PersonalizeCardBuilderParams): PersonalizeCardModel {
        return PersonalizeCardModel(onClick = params.onClick)
    }

    private fun shouldBuildCard() = dashboardPersonalizationFeatureConfig.isEnabled()
}
