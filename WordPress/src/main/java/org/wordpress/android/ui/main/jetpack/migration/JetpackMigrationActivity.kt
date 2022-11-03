package org.wordpress.android.ui.main.jetpack.migration

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.databinding.ActivityJetpackMigrationBinding

@AndroidEntryPoint
class JetpackMigrationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(ActivityJetpackMigrationBinding.inflate(layoutInflater)) {
            setContentView(root)
        }
    }
}
