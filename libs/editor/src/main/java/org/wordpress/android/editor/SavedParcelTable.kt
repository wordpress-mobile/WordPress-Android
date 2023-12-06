package org.wordpress.android.editor

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Parcel
import android.os.Parcelable
import org.wordpress.android.util.SqlUtils

object SavedParcelTable {
    private const val SAVED_PARCEL_TABLE = "tbl_saved_parcel"
    private const val PARCEL_ID = "parcel_id"
    private const val PARCEL_DATA = "parcel_data"
    fun createTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE " + SAVED_PARCEL_TABLE + " ("
                    + PARCEL_ID + " TEXT,"
                    + PARCEL_DATA + " BLOB,"
                    + " PRIMARY KEY (" + PARCEL_ID + ")"
                    + ")"
        )
    }

    fun dropTable(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS $SAVED_PARCEL_TABLE")
    }

    fun reset(db: SQLiteDatabase) {
        dropTable(db)
        createTable(db)
    }

    fun addParcel(writableDb: SQLiteDatabase?, parcelId: String, parcel: Parcelable) {
        val parcelable = ParcelableObject(parcel)
        val values = ContentValues()
        values.put(PARCEL_ID, parcelId)
        values.put(PARCEL_DATA, parcelable.toBytes())
        writableDb?.insertWithOnConflict(SAVED_PARCEL_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        parcelable.recycle()
    }

    fun <T> getParcel(readableDb: SQLiteDatabase?, parcelId: String, creator: Parcelable.Creator<T>): T? {
        val db = readableDb ?: return null
        val c = db.rawQuery("SELECT * FROM $SAVED_PARCEL_TABLE WHERE $PARCEL_ID ='$parcelId'", null)
        return try {
            if (c.moveToFirst()) {
                val parcelableObject = ParcelableObject(c.getBlob(c.getColumnIndexOrThrow(PARCEL_DATA)))
                val parcelable = creator.createFromParcel(parcelableObject.getParcel())
                parcelableObject.recycle()
                parcelable
            } else {
                null
            }
        } finally {
            SqlUtils.closeCursor(c)
        }
    }

    private class ParcelableObject {
        private val parcel = Parcel.obtain()

        constructor(parcelable: Parcelable) {
            parcelable.writeToParcel(parcel, 0)
        }

        constructor(data: ByteArray) {
            parcel.unmarshall(data, 0, data.size)
            parcel.setDataPosition(0)
        }

        fun toBytes(): ByteArray {
            return parcel.marshall()
        }

        fun getParcel(): Parcel {
            parcel.setDataPosition(0)
            return parcel
        }

        fun recycle() {
            parcel.recycle()
        }
    }
}
