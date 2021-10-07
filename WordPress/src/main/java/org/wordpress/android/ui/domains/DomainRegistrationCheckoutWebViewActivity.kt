package org.wordpress.android.ui.domains

import android.os.Bundle
import org.wordpress.android.ui.WPWebViewActivity

class DomainRegistrationCheckoutWebViewActivity : WPWebViewActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toggleNavbarVisibility(false)
    }
}
