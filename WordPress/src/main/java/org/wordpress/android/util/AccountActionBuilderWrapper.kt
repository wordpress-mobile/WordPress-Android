package org.wordpress.android.util

import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload
import javax.inject.Inject

class AccountActionBuilderWrapper @Inject constructor() {
    fun newUpdateAccessTokenAction(accessToken: String): Action<UpdateTokenPayload> =
            AccountActionBuilder.newUpdateAccessTokenAction(UpdateTokenPayload(accessToken))
}
