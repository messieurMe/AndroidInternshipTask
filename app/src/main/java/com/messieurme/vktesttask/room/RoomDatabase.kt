package com.messieurme.vktesttask.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.messieurme.vktesttask.classes.UploadingItem

@Database(entities = [UploadingItem::class], version = 1,exportSchema = false)
abstract class RoomDatabase : RoomDatabase() {
    abstract fun uploadingQueue(): UploadingQueue
}