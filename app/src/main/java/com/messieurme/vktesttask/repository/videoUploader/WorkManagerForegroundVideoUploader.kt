package com.messieurme.vktesttask.repository.videoUploader

import android.util.Log
import androidx.work.*
import com.messieurme.vktesttask.classes.AccessTokenClass
import com.messieurme.vktesttask.classes.CoroutineScopes
import com.messieurme.vktesttask.repository.keyValueRepository.KeyValueRepository
import com.messieurme.vktesttask.repository.videoUploader.helperClasses.HttpsUploadWorker
import com.messieurme.vktesttask.room.UploadingQueue
import com.messieurme.vktesttask.service.UploadWorker
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerForegroundVideoUploader @Inject constructor(
    private val workManagerInstance: WorkManager,
) : AbstractVideoUploader() {

    override fun onResume() = coroutine.io().launch {
        val workInfo = workManagerInstance.getWorkInfosByTag("Work").await()
        if (workInfo.size != 0) {
            workManagerInstance.cancelAllWorkByTag("Work").result.await()
        }
        super.onResume().join()
    }


    private fun startWorkManager() = coroutine.default().launch {
        val constraint = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val myWork = OneTimeWorkRequest.Builder(UploadWorker::class.java)
            .setConstraints(constraint)
            .addTag("Work")
            .build()
        workManagerInstance.enqueueUniqueWork("Work", ExistingWorkPolicy.APPEND_OR_REPLACE, myWork)
    }

    override fun onSystemPause() = coroutine.io().launch {
        val sizeOfQueue = super.getQueueSize()
        super.onSystemPause()
        if (continueInBackground.value && !userPause.value && sizeOfQueue > 0) {
            startWorkManager()
        }
    }
}