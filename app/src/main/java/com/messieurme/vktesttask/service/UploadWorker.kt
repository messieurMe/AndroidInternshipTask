package com.messieurme.vktesttask.service

import android.os.Build
import androidx.room.Room
import android.content.Context
import java.io.FileInputStream
import android.app.Notification
import com.messieurme.vktesttask.R
import androidx.work.ForegroundInfo
import android.annotation.TargetApi
import android.app.Activity
import androidx.work.CoroutineWorker
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.work.WorkerParameters
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import androidx.core.app.NotificationCompat
import com.messieurme.vktesttask.classes.SharedFunctions
import com.messieurme.vktesttask.room.UploadsDao
import com.messieurme.vktesttask.room.UploadsDatabase
import com.messieurme.vktesttask.classes.SharedFunctions.Companion.uploadFunction
import com.messieurme.vktesttask.classes.SharedFunctions.Companion.getProgressInPercents
import com.messieurme.vktesttask.classes.UploadingProgress
import kotlinx.coroutines.delay
import retrofit2.await

class UploadWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    private var notificationId = 1
    private lateinit var db: UploadsDatabase
    private lateinit var uploads: UploadsDao
    private val myNotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


    private suspend fun getUrlForFile(uploadIngFile: UploadingProgress, accessToken: String) =
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

        var accessToken = inputData.getString("access_token")!!


        try {
            // Dispatchers IO, as I read in documentation, creates thread for blocking operations if need.
            // I didn't find out how to remove warning (except the way to create your own thread, but
            // Dispatchers.IO must do it for me), so that's why Suppress here
            @Suppress("BlockingMethodInNonBlockingContext")
            withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
                getUploadingInfo()
                var toContinue = 0
                while (uploads.getSize() > 0) {
                    val uploadIngFile = uploads.getFirst()

                    if (uploadIngFile.url == "-" && !getUrlForFile(uploadIngFile, accessToken)) {
                        toContinue++
                        delay(toContinue * 2000L)
                        continue
                    }


                    FileInputStream(uploadIngFile.uri).use { inputStream ->
                        if (uploadIngFile.uploaded != 0L) inputStream.skip(uploadIngFile.uploaded)
                        var badResponse = 0
                        while (uploadIngFile.fileSize != uploadIngFile.uploaded && !isStopped && badResponse < 3) {
                            uploadFunction(uploadIngFile, inputStream)
                            updateNotification(
                                res,
                                getProgressInPercents(uploadIngFile.uploaded, uploadIngFile.fileSize),
                                false,
                                uploadIngFile.name
                            )
                            if (!uploadIngFile.lastSuccess) {
                                badResponse++
                            } else {
                                uploads.update(uploadIngFile)
                            }
                        }
                    }
                    if (uploadIngFile.fileSize == uploadIngFile.uploaded) {
                        uploads.remove(uploadIngFile)
                        updateNotification(res, 100, true, uploadIngFile.name)
                    } else { //something cancelled us
                        uploads.update(uploadIngFile)
                        myNotificationManager.cancelAll()
                        break
                    }
                }
            }
        } catch (ignore: Exception) { //it throws "kotlinx.coroutines.JobCancellationException" which I can't catch
        } finally {
            db.close()
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
        fileName: String
    ) {
        myNotificationManager.notify(1,
            res
                .setContentTitle("Uploading file: $fileName")
                .setProgress(100, progress, indeterminate)
                .setContentText("$progress%")
                .build().also { it.flags = it.flags or 2 }
        )
    }

    private suspend fun getUploadingInfo() =
        withContext(CoroutineScope(Dispatchers.Default).coroutineContext) {
            db = Room.databaseBuilder(applicationContext, UploadsDatabase::class.java, "database").build()
            uploads = db.UploadsDao()
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
