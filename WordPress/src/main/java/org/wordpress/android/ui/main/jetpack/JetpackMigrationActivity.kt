package org.wordpress.android.ui.main.jetpack

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.databinding.ActivityJetpackWelcomeBinding

@AndroidEntryPoint
class JetpackMigrationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(ActivityJetpackWelcomeBinding.inflate(layoutInflater)) {
            setContentView(root)
        }
    }
}
