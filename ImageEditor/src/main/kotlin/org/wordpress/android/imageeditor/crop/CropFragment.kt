package org.wordpress.android.imageeditor.crop

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.yalantis.ucrop.UCropFragment
import org.wordpress.android.imageeditor.R

class CropFragment : UCropFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.menu_crop_fragment, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = if (item.itemId == R.id.menu_done) {
        cropAndSaveImage()
        true
    } else {
        super.onOptionsItemSelected(item)
    }
}
