package com.messieurme.vktesttask.ui.home

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.*
import com.messieurme.vktesttask.classes.AccessTokenClass
import com.messieurme.vktesttask.classes.ResponseOrError
import com.messieurme.vktesttask.repository.uploadedRepository.UploadedAlbumRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


class HomeViewModel @Inject constructor(
    private val uploadedAlbumRepository: UploadedAlbumRepository,
    private val accessToken: AccessTokenClass,
) : ViewModel() {

    val refreshing = ObservableBoolean(false)
    private var _uploadedResponse = MutableStateFlow<ResponseOrError>(ResponseOrError.Nothing)
    var uploaded = _uploadedResponse.asStateFlow()

    private suspend fun requestUploadedAlbumList() {
        refreshing.set(true)
        _uploadedResponse.value = ResponseOrError.Loading

        val uploadedAlbumList = uploadedAlbumRepository.getVideos(accessToken.getAccessToken())

        _uploadedResponse.value = uploadedAlbumList
        refreshing.set(false)
    }

    fun refreshData() = CoroutineScope(Dispatchers.IO).launch {
        requestUploadedAlbumList()
    }
}