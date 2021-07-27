package com.messieurme.vktesttask.modules

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class WorkManagerModule {
    @Provides
    @Singleton
    fun workManagerService(context: Context): WorkManager = WorkManager.getInstance(context)
}