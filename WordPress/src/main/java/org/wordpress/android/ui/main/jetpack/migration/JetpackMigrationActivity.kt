package org.wordpress.android.ui.main.jetpack.migration

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.ActivityJetpackMigrationBinding
import org.wordpress.android.localcontentmigration.LocalMigrationState.SingleStep

@AndroidEntryPoint
class JetpackMigrationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(ActivityJetpackMigrationBinding.inflate(layoutInflater)) {
            setContentView(root)
            val singleStepState = intent.getParcelableExtra<SingleStep?>(KEY_SINGLE_STEP_STATE)
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, JetpackMigrationFragment.newInstance(singleStepState))
                    .commit()
        }
    }

    companion object {
        private const val KEY_SINGLE_STEP_STATE = "KEY_SINGLE_STEP_STATE"
        fun createIntent(context: Context, singleStepState: SingleStep? = null): Intent =
                Intent(context, JetpackMigrationActivity::class.java).apply {
                    putExtra(KEY_SINGLE_STEP_STATE, singleStepState)
                }
    }
}
