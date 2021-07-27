package com.messieurme.vktesttask.repository.uploadedRepository

import com.messieurme.vktesttask.classes.*
import com.messieurme.vktesttask.retrofit.Get
import com.messieurme.vktesttask.retrofit.Video
import retrofit2.awaitResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadedAlbumRepository @Inject constructor(private val videoRetrofitClient: Video) {
    suspend fun getVideos(accessToken: String): ResponseOrError {
        return try {
            val uploadedId = getAlbumId(accessToken)  //Id for uploaded is -1, but i'll download it
            val uploadedVideos = getVideosByAlbumId(accessToken, uploadedId)
            ResponseOrError.IsSuccsess(uploadedVideos!!.response.items)
        } catch (e: Exception) {
            ResponseOrError.IsError(e)
        }
    }

    private suspend fun getAlbumId(accessToken: String): Int? {
        val response = videoRetrofitClient.getAlbums(accessToken, 1).awaitResponse()
        return response.body()?.response?.items?.find { it.title == "Загруженные" }?.id
    }

    private suspend fun getVideosByAlbumId(accessToken: String, uploadedId: Int?): Get? {
        val response = videoRetrofitClient.get(uploadedId!!, accessToken).awaitResponse()
        return response.body()
    }

}