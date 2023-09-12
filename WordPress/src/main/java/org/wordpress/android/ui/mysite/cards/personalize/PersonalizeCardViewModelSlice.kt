package org.wordpress.android.ui.mysite.cards.personalize

import android.util.Log
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonalizeCardViewModelSlice@Inject constructor(
)  {
    fun getBuilderParams() = MySiteCardAndItemBuilderParams.PersonalizeCardBuilderParams(
        onClick = this::onCardClick)

    fun onCardClick() {
        Log.i(javaClass.simpleName, "***=> onCardClick")
    }
}
