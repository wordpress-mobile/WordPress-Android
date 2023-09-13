package org.wordpress.android.ui.mysite.cards.personalize

import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PersonalizeCardBuilderParams
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonalizeCardViewModelSlice@Inject constructor(
)  {
    fun getBuilderParams() = PersonalizeCardBuilderParams(onClick = this::onCardClick)

    fun onCardClick() {
        // todo: implement
    }
}
