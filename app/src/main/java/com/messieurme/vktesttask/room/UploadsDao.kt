package com.messieurme.vktesttask.room

import androidx.room.*
import com.messieurme.vktesttask.classes.UploadingProgress
import java.util.*

@Dao
interface UploadsDao {
    @Query("SELECT * FROM UploadingProgress")
    fun getAll(): List<UploadingProgress>

    @Update
    suspend fun update(uploadingProgress: UploadingProgress)

    @Query("SELECT * FROM UploadingProgress LIMIT 1")
    suspend fun getFirst(): UploadingProgress

    @Query("SELECT COUNT(*) FROM UploadingProgress")
    suspend fun getSize(): Int

    @Insert
    suspend fun insert(uploadingProgress: UploadingProgress)

    @Delete
    suspend fun remove(uploadingProgress: UploadingProgress)

    @Query("DELETE FROM UploadingProgress WHERE sessionID = :id")
    suspend fun remove(id: Long)

    @Query("DELETE FROM UploadingProgress WHERE sessionID > 0")
    suspend fun removeAll()

    @Query("SELECT sessionID FROM UploadingProgress ")
    suspend fun getIds(): List<Long>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    suspend fun insertAll(list: List<UploadingProgress>)


    @Query("SELECT * FROM UploadingProgress WHERE sessionID = :id")
    suspend fun getById(id: Long): UploadingProgress
}