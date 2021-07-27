package com.messieurme.vktesttask

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import com.messieurme.vktesttask.di.ApplicationComponent
import com.messieurme.vktesttask.di.DaggerApplicationComponent
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import javax.inject.Inject


open class MyApplication : DaggerApplication(), Configuration.Provider {
    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerApplicationComponent.factory().create(this.applicationContext)
    }

    @Inject
    lateinit var workerFactory: WorkerFactory


    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setWorkerFactory(workerFactory)
            .build()
    }

}
