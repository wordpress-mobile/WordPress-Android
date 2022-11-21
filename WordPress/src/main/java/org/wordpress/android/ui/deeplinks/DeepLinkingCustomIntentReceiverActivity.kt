package org.wordpress.android.ui.deeplinks

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * An activity to handle custom uri intent deep linking schemes like:
 * <p>
 * wordpress://feature
 * <p>
 */
@AndroidEntryPoint
class DeepLinkingCustomIntentReceiverActivity : AppCompatActivity() {
    private val viewModel: DeepLinkingCustomIntentReceiverViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.forwardDeepLink(intent)
        finish()
    }
}
