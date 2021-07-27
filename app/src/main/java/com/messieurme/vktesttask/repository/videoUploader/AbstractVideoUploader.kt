package com.messieurme.vktesttask.repository.videoUploader

import android.util.Log
import com.messieurme.vktesttask.classes.*
import com.messieurme.vktesttask.exceptions.NoInternetException
import com.messieurme.vktesttask.repository.keyValueRepository.KeyValueRepository
import com.messieurme.vktesttask.repository.videoUploader.helperClasses.HttpsUploadWorker
import com.messieurme.vktesttask.room.UploadingQueue
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.await
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.inputStream

abstract class AbstractVideoUploader {
    @Inject lateinit var accessToken: AccessTokenClass
    @Inject lateinit var localQueueDatabase: UploadingQueue
    @Inject lateinit var httpsUploadWorker: HttpsUploadWorker
    @Inject lateinit var keyValueRepository: KeyValueRepository
    @Inject lateinit var coroutine: CoroutineScopes

    private var _progress = MutableStateFlow<Int>(0)
    val progress = _progress.asStateFlow()

    private val _userPause = MutableStateFlow(true)
    val userPause = _userPause.asStateFlow()

    private val _continueInBackground = MutableStateFlow(false)
    val continueInBackground = _continueInBackground.asStateFlow()

    private val queue = PriorityArrayList<UploadingItem>()
    var notifyQueueChanged = Channel<Int>(5)

    val errorHandler = Channel<Throwable>(1)

    @Volatile
    var uploadSessionIds = HashSet<Long>(0)

    fun getQueue() = ArrayList(queue.map { it.copy() })

    fun getQueueSize() = queue.size

    fun getCurrentUploadingName() = if (getQueueSize() > 0) queue.first().name else ""

    open fun onResume() = coroutine.io().launch {
        refreshSuspend()
        queue.clear()
        if (localQueueDatabase.getSize() > 0) {
            queue.addAllPreLast(localQueueDatabase.getAll())
            notifyQueueChanged.send((Math.random() * (Int.MAX_VALUE - 10)).toInt())
            queue.first().also { item -> _progress.emit(item.progress) }
        }
        coroutine.io().launch { resumeUploading() }
    }

    open fun onSystemPause() = coroutine.io().launch {
        numberOfThreads.set(0)
        uploadSessionIds.clear()
        keyValueRepository.put("pause", userPause.value)
        keyValueRepository.put("background_mode", continueInBackground.value)
        queue.clear()
    }

    open fun onUserPause(isPaused: Boolean = true) = coroutine.io().launch {
        _userPause.emit(isPaused)
        if (!isPaused) {
            resumeUploading()
        }
    }

    open fun onBackgroundUploading(enabled: Boolean) =
        coroutine.io().launch { _continueInBackground.emit(enabled) }


    open fun onNewItem(name: String, description: String, uri: String) = coroutine.io().launch {
        val fileSize: Long = File(uri).length()
        val uploadInfo = UploadingItem(
            fileSize = fileSize,
            description = description,
            uri = uri,
            name = name
        )

        queue.addLast(uploadInfo)
        localQueueDatabase.insert(uploadInfo)
        notifyQueueChanged.send((Math.random() * (Int.MAX_VALUE - 10)).toInt())
        resumeUploading()
    }

    open fun onRemoveItem(i: Int) = coroutine.io().launch {
        localQueueDatabase.remove(queue[i])
        queue.removeAt(i)
        if (i == 0) {
            _progress.emit(0)
        }
        notifyQueueChanged.send((Math.random() * 10000).toInt())
    }

    fun closeAll() {

    }

    private var numberOfThreads = AtomicInteger(0)
    private suspend fun resumeUploading() {
        if (numberOfThreads.compareAndSet(0, 1)) {
            val id = System.currentTimeMillis()
            uploadSessionIds.add(id)
            upload(id)
            uploadSessionIds.remove(id)
            numberOfThreads.compareAndSet(1, 0)
        }
    }


    private fun stopUploadingChecker(id: Long): Boolean {
        return queue.size > 0 && uploadSessionIds.contains(id) && !userPause.value
    }


    private suspend fun badResponse(n: Int) = when (n) {
        0 -> false
        1, 2 -> delay(n * 2000L).let { false }
        3 -> {
            _userPause.emit(true)
            errorHandler.send(NoInternetException())
            true
        }
        else -> true
    }


    @Suppress("BlockingMethodInNonBlockingContext")
    @OptIn(ExperimentalPathApi::class)
    private suspend fun upload(id: Long) {
        withContext(Dispatchers.IO) {
            var badResponseCounter = 0

            while (stopUploadingChecker(id)) {
                val uploadIngFile = queue.first()
                if (uploadIngFile.url == "-" && !getUrlForFile(uploadIngFile)) {
                    badResponse(++badResponseCounter)
                    continue
                }
                val buffer = ByteArray(1024 * 1024 * 1)
                try {
                    Paths.get(uploadIngFile.uri).inputStream().use { inputStream ->
                        if (uploadIngFile.uploaded != 0L) {
                            inputStream.skip(uploadIngFile.uploaded)
                        }
                        while (uploadIngFile.fileSize != uploadIngFile.uploaded && stopUploadingChecker(id) && queue.first().sessionID == uploadIngFile.sessionID) {
                            if (uploadIngFile.lastSuccess) {
                                inputStream.read(buffer).also { uploadIngFile.lastBytesRead = it }
                            } else {
                                badResponse(++badResponseCounter)
                            }
                            httpsUploadWorker.uploadFunction(uploadIngFile, buffer)
                            localQueueDatabase.update(uploadIngFile)
                            _progress.emit(uploadIngFile.progress)
                        }
                    }
                } catch (e: Exception) {
                    _userPause.emit(true)
                    errorHandler.send(e)
                    break
                }

                if (uploadIngFile.fileSize == uploadIngFile.uploaded) {
                    localQueueDatabase.remove(queue.first())
                    queue.removeAt(0)
                    notifyQueueChanged.send((Math.random() * (Int.MAX_VALUE - 10)).toInt())
                    _progress.emit(0)
                }
            }
        }
    }


    private suspend fun getUrlForFile(uploadIngFile: UploadingItem) = SharedFunctions.retrofit
        .runCatching {
            this.save(
                uploadIngFile.name,
                accessToken.getAccessToken(),
                uploadIngFile.description
            ).await()
        }
        .onFailure {}
        .onSuccess { save ->
            uploadIngFile.url = save.response.upload_url
        }.isSuccess


    fun refresh() = coroutine.default().launch { refreshSuspend() }

    private suspend fun refreshSuspend() {
        notifyQueueChanged.send(0)
        _userPause.emit(keyValueRepository.getBoolean("pause", false))
        _continueInBackground.emit(keyValueRepository.getBoolean("background_mode", true))
    }

}