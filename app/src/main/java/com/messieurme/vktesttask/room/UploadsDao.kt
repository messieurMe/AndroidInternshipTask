package com.messieurme.vktesttask.room

import androidx.room.*
import com.messieurme.vktesttask.classes.UploadingProgress
import java.util.*

@Dao
interface UploadsDao {
    @Query("SELECT * FROM UploadingProgress")
    fun getAll(): List<UploadingProgress>

    @Update
    fun update(uploadingProgress: UploadingProgress)

    @Query("SELECT * FROM UploadingProgress LIMIT 1")
    fun getFirst(): UploadingProgress

    @Query("SELECT COUNT(*) FROM UploadingProgress")
    fun getSize(): Int

    @Insert
    fun insert(uploadingProgress: UploadingProgress)

    @Delete
    fun remove(uploadingProgress: UploadingProgress)

    @Query("DELETE FROM UploadingProgress WHERE sessionID = :id")
    fun remove(id: Long)

    @Query("DELETE FROM UploadingProgress WHERE sessionID > 0")
    fun removeAll()

}