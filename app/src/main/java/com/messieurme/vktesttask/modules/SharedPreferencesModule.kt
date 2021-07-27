package com.messieurme.vktesttask.modules

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import javax.inject.Singleton


@Module
class SharedPreferencesModule {

    @Singleton
    @Provides
    fun getSharedPreferencesService(context: Context): SharedPreferences =
        context.getSharedPreferences("messieurMe.VideoUploader", Activity.MODE_PRIVATE)
}