package com.messieurme.vktesttask.modules

import android.content.Context
import androidx.work.*
import com.messieurme.vktesttask.classes.AccessTokenClass
import com.messieurme.vktesttask.classes.CoroutineScopes
import com.messieurme.vktesttask.repository.keyValueRepository.KeyValueRepository
import com.messieurme.vktesttask.repository.videoUploader.ForegroundVideoUploader
import com.messieurme.vktesttask.service.UploadWorker
import dagger.Module
import dagger.Provides
import javax.inject.Inject


class WorkManagerFactory @Inject constructor(
    private val foregroundVideoUploader: ForegroundVideoUploader,
    private val coroutine: CoroutineScopes
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        val workerKlass = Class.forName(workerClassName).asSubclass(CoroutineWorker::class.java)
        val constructor =
            workerKlass.getDeclaredConstructor(Context::class.java, WorkerParameters::class.java)
        val instance = constructor.newInstance(appContext, workerParameters)

        when (instance) {
            is UploadWorker -> {
                instance.foregroundVideoUploader = foregroundVideoUploader
                instance.coroutine = coroutine
            }
        }
        return instance
    }
}