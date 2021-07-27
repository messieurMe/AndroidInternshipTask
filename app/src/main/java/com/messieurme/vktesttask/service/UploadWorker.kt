package com.messieurme.vktesttask.service

import android.os.Build
import android.content.Context
import android.app.Notification
import com.messieurme.vktesttask.R
import androidx.work.ForegroundInfo
import android.annotation.TargetApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.app.NotificationManager
import android.app.NotificationChannel
import androidx.core.app.NotificationCompat
import com.messieurme.vktesttask.classes.CoroutineScopes
import com.messieurme.vktesttask.repository.videoUploader.ForegroundVideoUploader
import com.messieurme.vktesttask.retrofit.Video
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
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

    @Inject
    lateinit var retrofitVideoClient : Video

    private var notificationId = 1

    private val myNotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


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
