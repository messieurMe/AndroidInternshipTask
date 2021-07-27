package com.messieurme.vktesttask.room

import androidx.room.*
import com.messieurme.vktesttask.classes.UploadingItem

@Dao
interface UploadingQueue {
    @Query("SELECT * FROM UploadingItem")
    fun getAll(): List<UploadingItem>

    @Update
    suspend fun update(uploadingItem: UploadingItem)

    @Update
    suspend fun updateAll(uploadingItems: List<UploadingItem>)

    @Query("SELECT * FROM UploadingItem LIMIT 1")
    suspend fun getFirst(): UploadingItem

    @Query("SELECT COUNT(*) FROM UploadingItem")
    suspend fun getSize(): Int

    @Insert
    suspend fun insert(uploadingItem: UploadingItem)

    @Delete
    suspend fun remove(uploadingItem: UploadingItem)

    @Query("DELETE FROM UploadingItem WHERE sessionID = :id")
    suspend fun remove(id: Long)

    @Query("DELETE FROM UploadingItem WHERE sessionID > 0")
    suspend fun removeAll()

    @Query("SELECT sessionID FROM UploadingItem ")
    suspend fun getIds(): List<Long>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    suspend fun insertAll(list: List<UploadingItem>)

    @Query("SELECT * FROM UploadingItem WHERE sessionID = :id")
    suspend fun getById(id: Long): UploadingItem
}