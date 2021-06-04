package com.messieurme.vktesttask.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.messieurme.vktesttask.classes.UploadingProgress

@Database(entities = [UploadingProgress::class], version = 1)
abstract class UploadsDatabase : RoomDatabase() {
    abstract fun UploadsDao(): UploadsDao
}