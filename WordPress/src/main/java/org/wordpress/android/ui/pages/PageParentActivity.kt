package org.wordpress.android.ui.pages

import android.os.Bundle
import org.wordpress.android.databinding.PagesParentActivityBinding
import org.wordpress.android.ui.main.BaseAppCompatActivity

class PageParentActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = PagesParentActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar.toolbarMain)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }
}
