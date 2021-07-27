package com.messieurme.vktesttask.ui.dashboard

import android.widget.Toast
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.messieurme.vktesttask.repository.keyValueRepository.KeyValueRepository
import com.messieurme.vktesttask.repository.videoUploader.WorkManagerForegroundVideoUploader
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

class DashboardViewModel @Inject constructor(
    private val foregroundVideoUploader: WorkManagerForegroundVideoUploader,
) : ViewModel() {

    var backgroundModeLiveData = foregroundVideoUploader.continueInBackground.asLiveData()

    fun inBackground(): Boolean = continueInBackground.value
    val progress = foregroundVideoUploader.progress

    val errorHandler = foregroundVideoUploader.errorHandler.receiveAsFlow()

    val pausedByUser: StateFlow<Boolean> = foregroundVideoUploader.userPause

    val queueChangedListener = foregroundVideoUploader.notifyQueueChanged.receiveAsFlow()

    val continueInBackground: StateFlow<Boolean> = foregroundVideoUploader.continueInBackground


    fun queueCopy() = foregroundVideoUploader.getQueue()

    fun refreshAll() = foregroundVideoUploader.refresh().let { this }

    fun removeAt(i: Int) = foregroundVideoUploader.onRemoveItem(i)

    fun enqueueVideo(name: String, description: String, uri: String) =
        foregroundVideoUploader.onNewItem(name, description, uri)

    fun userPause() = foregroundVideoUploader.onUserPause(!pausedByUser.value)

    fun backgroundModeChanged() = foregroundVideoUploader.onBackgroundUploading(!continueInBackground.value)

}