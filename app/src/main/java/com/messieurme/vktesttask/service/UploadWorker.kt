package com.messieurme.vktesttask.service

import android.os.Build
import androidx.room.Room
import android.content.Context
import java.io.FileInputStream
import android.app.Notification
import com.messieurme.vktesttask.R
import androidx.work.ForegroundInfo
import android.annotation.TargetApi
import androidx.work.CoroutineWorker
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.work.WorkerParameters
import android.app.NotificationManager
import android.app.NotificationChannel
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import androidx.core.app.NotificationCompat
import com.messieurme.vktesttask.classes.AccessTokenClass
import com.messieurme.vktesttask.classes.CoroutineScopes
import com.messieurme.vktesttask.classes.SharedFunctions
import com.messieurme.vktesttask.room.UploadingQueue
import com.messieurme.vktesttask.room.RoomDatabase
import com.messieurme.vktesttask.classes.SharedFunctions.Companion.uploadFunction
import com.messieurme.vktesttask.classes.SharedFunctions.Companion.getProgressInPercents
import com.messieurme.vktesttask.classes.UploadingItem
import com.messieurme.vktesttask.modules.CoroutineScopesModule
import com.messieurme.vktesttask.repository.keyValueRepository.KeyValueRepository
import com.messieurme.vktesttask.repository.videoUploader.ForegroundVideoUploader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import retrofit2.await
import javax.inject.Inject

class UploadWorker(
    context: Context,
    parameters: WorkerParameters,
) :
    CoroutineWorker(context, parameters) {

    @Inject
    lateinit var foregroundVideoUploader: ForegroundVideoUploader

    @Inject
    lateinit var coroutine: CoroutineScopes

    private var notificationId = 1

    private val myNotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


    private suspend fun getUrlForFile(uploadIngFile: UploadingItem, accessToken: String) =
        SharedFunctions.retrofit
            .runCatching {
                this.save(
                    uploadIngFile.name,
                    accessToken,
                    uploadIngFile.description
                ).await()
            }
            .onFailure {
            }.onSuccess { save ->
                uploadIngFile.apply { url = save.response.upload_url }
            }.isSuccess


    override suspend fun doWork(): Result {
        val res = createNotification()
        updateNotification(res, 100, true, "Prepare for uploading...")

        try {
            foregroundVideoUploader.onResume().join()

            var currentUploadingName = ""
            foregroundVideoUploader.notifyQueueChanged.receiveAsFlow().onEach {
                currentUploadingName = foregroundVideoUploader.getCurrentUploadingName()
            }.launchIn(coroutine.default())

            while (!isStopped && foregroundVideoUploader.getQueueSize() > 0) {
                updateNotification(res,
                    foregroundVideoUploader.progress.value,
                    false,
                    currentUploadingName
                )
                delay(1500)
            }
            foregroundVideoUploader.onSystemPause()
            myNotificationManager.cancelAll()
        } catch (ignore: Exception) {
        }
        return Result.success()
    }


    private suspend fun createNotification() = createForegroundInfo().also {
        setForeground(
            ForegroundInfo(
                notificationId,
                it.build().also { inner -> inner.flags = inner.flags or Notification.FLAG_ONGOING_EVENT }
            )
        )
    }


    private fun updateNotification(
        res: NotificationCompat.Builder,
        progress: Int,
        indeterminate: Boolean,
        fileName: String,
    ) {
        myNotificationManager.notify(1,
            res
                .setContentTitle("Uploading file: $fileName")
                .setProgress(100, progress, indeterminate)
                .setContentText("$progress%")
                .build().also { it.flags = it.flags or 2 }
        )
    }

    private fun createForegroundInfo(): NotificationCompat.Builder {
        val notification = NotificationCompat.Builder(applicationContext, "Work")
            .setContentTitle("Uploading file")
            .setContentText("0%")
            .setTicker("%video_name%")
            .setOngoing(true)
            .setProgress(100, 0, true)
            .setSmallIcon(R.drawable.ic_home_black_24dp)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
        return notification
    }


    @TargetApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val channel = NotificationChannel(
            "Work",
            "Work",
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Work Notifications"
        myNotificationManager.createNotificationChannel(channel)
    }
}
