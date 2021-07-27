package com.messieurme.vktesttask.repository.videoUploader

import android.app.Activity
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.work.*
import com.messieurme.vktesttask.R
import com.messieurme.vktesttask.classes.*
import com.messieurme.vktesttask.exceptions.NoInternetException
import com.messieurme.vktesttask.repository.keyValueRepository.KeyValueRepository
import com.messieurme.vktesttask.repository.videoUploader.helperClasses.HttpsUploadWorker
import com.messieurme.vktesttask.room.RoomDatabase
import com.messieurme.vktesttask.room.UploadingQueue
import com.messieurme.vktesttask.service.UploadWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.await
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.inputStream

@Singleton
class ForegroundVideoUploader @Inject constructor() : AbstractVideoUploader() {
}