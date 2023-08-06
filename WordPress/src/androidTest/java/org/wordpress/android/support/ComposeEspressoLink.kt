package org.wordpress.android.support

import androidx.test.espresso.IdlingRegistry

class ComposeEspressoLink {
    fun unregister() {
        IdlingRegistry.getInstance().resources.forEach {
            if (it.name == "Compose-Espresso link") {
                IdlingRegistry.getInstance().unregister(it)
            }
        }
    }
}
