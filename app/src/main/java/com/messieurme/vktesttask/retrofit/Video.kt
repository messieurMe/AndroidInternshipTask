package com.messieurme.vktesttask.retrofit

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface Video {
    @GET("video.getAlbums")
    fun getAlbums(
        @Query("access_token") accessToken: String,
        @Query("need_system") needSystem: Int,
        @Query("v") v: String = "5.131"
    ): Call<GetAlbum>

    @GET("video.get")
    fun get(
        @Query("album_id") albumID: Int,
        @Query("access_token") accessToken: String,
        @Query("v") v: String = "5.131"
    ): Call<Get>

    @GET("video.save")
    fun save(
        @Query("name") name: String,
        @Query("access_token") accessToken: String,
        @Query("description") description: String,
        @Query("v") v: String = "5.131"
    ): Call<Save>

}

class Save(var response: Response) {
    class Response(
        var access_key: String,
        var description: String,
        var owner_id: String,
        var title: String,
        var upload_url: String,
        var video_id: String
    )
}

class Get(var response: Response) {
    class Response(var count: Int, var items: List<Items>) {
        class Items(
            var id: Int?,
            var views: Int?,
            var width: Int?,
            var height: Int?,
            var title: String?,
            var duration: Int?,
            var player: String?,
            var addingDate: Int?,
            var image: List<Image>?,
            var is_favorite: Boolean?,
        ) {
            class Image(
                var url: String?,
                var height: Int?,
                var width: Int?,
                var with_padding: Int,
            )
        }
    }
}

class GetAlbum(var response: Response?) {
    class Response(var count: Int, var items: List<VideoGetAlbumsResponse>?) {
        class VideoGetAlbumsResponse(var id: Int, var owner_id: Int, val title: String) {
        }
    }
}

