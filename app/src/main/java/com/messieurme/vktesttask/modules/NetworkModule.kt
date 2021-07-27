package com.messieurme.vktesttask.modules

import com.google.gson.GsonBuilder
import com.messieurme.vktesttask.retrofit.Video
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
class NetworkModule {

    @Singleton
    @Provides
    fun provideVideoRetrofitService(gsonConverterFactory: GsonConverterFactory): Video = Retrofit.Builder()
        .baseUrl("https://api.vk.com/method/")
        .addConverterFactory(gsonConverterFactory)
        .build()
        .create(Video::class.java)

    @Singleton
    @Provides
    fun provideGsonConverterFactoryService(): GsonConverterFactory =
        GsonConverterFactory.create(GsonBuilder().setLenient().create())
}