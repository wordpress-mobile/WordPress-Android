package org.wordpress.android.ui.sitecreation

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import org.wordpress.android.R
import org.wordpress.android.databinding.SiteCreationFormScreenBinding

abstract class SiteCreationBaseFormFragment : Fragment(R.layout.site_creation_form_screen), MenuProvider {
    @LayoutRes protected abstract fun getContentLayout(): Int

    protected abstract val screenTitle: String

    protected abstract fun setupContent()

    protected abstract fun onHelp()

    protected abstract fun setBindingViewStubListener(parentBinding: SiteCreationFormScreenBinding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)

        with(SiteCreationFormScreenBinding.bind(view)) {
            siteCreationFormContentStub.layoutResource = getContentLayout()
            setBindingViewStubListener(this)
            siteCreationFormContentStub.inflate()

            setupContent()

            (requireActivity() as AppCompatActivity).setSupportActionBar(toolbarMain)
            val actionBar = (activity as AppCompatActivity?)!!.supportActionBar
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true)
                actionBar.title = screenTitle
                // important for accessibility
                requireActivity().title = screenTitle
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_site_creation, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem) = when (menuItem.itemId) {
        R.id.help -> {
            onHelp()
            true
        }
        else -> false
    }

    companion object {
        const val EXTRA_SCREEN_TITLE = "extra_screen_title"
    }
}
