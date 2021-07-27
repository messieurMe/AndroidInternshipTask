package com.messieurme.vktesttask.modules

import android.content.Context
import androidx.room.Room
import com.messieurme.vktesttask.room.UploadingQueue
import com.messieurme.vktesttask.room.RoomDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class LocalRepositoryModule {

    @Singleton
    @Provides
    fun provideDatabase(context: Context): RoomDatabase = Room.databaseBuilder(
        context,
        RoomDatabase::class.java,
        "database"
    ).build()

    @Singleton
    @Provides
    fun provideUploadingQueue(database: RoomDatabase): UploadingQueue = database.uploadingQueue()
}