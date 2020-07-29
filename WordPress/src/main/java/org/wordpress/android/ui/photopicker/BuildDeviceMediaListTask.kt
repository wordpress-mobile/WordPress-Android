package org.wordpress.android.ui.photopicker

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.provider.MediaStore.Images.Media
import android.provider.MediaStore.Video
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MEDIA
import org.wordpress.android.util.SqlUtils
import java.util.ArrayList
import java.util.Collections

/*
 * builds the list of media items from the device
 */
internal class BuildDeviceMediaListTask(
    private val mReload: Boolean, private val mBrowserType: MediaBrowserType, private val mContext: Context,
    private val mListener: BuildDeviceMediaListListener
) : AsyncTask<Void?, Void?, Boolean>() {
    private val mTmpList = ArrayList<PhotoPickerItem>()
    override fun doInBackground(vararg params: Void?): Boolean {
        // images
        if (mBrowserType.isImagePicker) {
            addMedia(Media.EXTERNAL_CONTENT_URI, false)
        }

        // videos
        if (mBrowserType.isVideoPicker) {
            addMedia(Video.Media.EXTERNAL_CONTENT_URI, true)
        }

        // sort by id in reverse (newest first)
        mTmpList.sortWith(Comparator { item1, item2 -> item2.id.compareTo(item1.id) })

        // if we're reloading then return true so the adapter is updated, otherwise only
        // return true if changes are detected
        return mReload
    }

    private fun addMedia(baseUri: Uri, isVideo: Boolean) {
        val projection = arrayOf(ID_COL)
        var cursor: Cursor? = null
        try {
            cursor = mContext.contentResolver.query(
                    baseUri,
                    projection,
                    null,
                    null,
                    null
            )
        } catch (e: SecurityException) {
            AppLog.e(MEDIA, e)
        }
        if (cursor == null) {
            return
        }
        try {
            val idIndex = cursor.getColumnIndexOrThrow(ID_COL)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val item = PhotoPickerItem(
                        id,
                        Uri.withAppendedPath(baseUri, "" + id),
                        isVideo
                )
                mTmpList.add(item)
            }
        } finally {
            SqlUtils.closeCursor(cursor)
        }
    }

    override fun onCancelled() {
        super.onCancelled()
        mListener.onCancelled()
    }

    override fun onPostExecute(result: Boolean) {
        mListener.onSuccess(mTmpList)
    }

    internal interface BuildDeviceMediaListListener {
        fun onCancelled()
        fun onSuccess(result: List<PhotoPickerItem>?)
    }

    companion object {
        private const val ID_COL = Media._ID
    }
}
