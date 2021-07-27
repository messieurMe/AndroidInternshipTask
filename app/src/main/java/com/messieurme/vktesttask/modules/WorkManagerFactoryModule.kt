package com.messieurme.vktesttask.modules

import androidx.work.WorkerFactory
import com.messieurme.vktesttask.classes.AccessTokenClass
import com.messieurme.vktesttask.classes.CoroutineScopes
import com.messieurme.vktesttask.repository.keyValueRepository.KeyValueRepository
import com.messieurme.vktesttask.repository.videoUploader.ForegroundVideoUploader
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class WorkManagerFactoryModule {

    @Singleton
    @Provides
    fun provideWorkManagerFactoryService(
        foregroundVideoUploader: ForegroundVideoUploader,
        coroutine: CoroutineScopes,
    ): WorkerFactory =
        WorkManagerFactory(foregroundVideoUploader, coroutine)
}
