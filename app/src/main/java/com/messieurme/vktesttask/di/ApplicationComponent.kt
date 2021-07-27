package com.messieurme.vktesttask.di

import dagger.Component
import dagger.BindsInstance
import javax.inject.Singleton
import android.content.Context
import dagger.android.AndroidInjector
import dagger.android.AndroidInjectionModule
import com.messieurme.vktesttask.MyApplication
import com.messieurme.vktesttask.modules.*


@Singleton
@Component(
    modules = [
        NetworkModule::class,
        AndroidInjectionModule::class,
        HomeFragmentModule::class,
        MainActivityModule::class,
        DashboardFragmentModule::class,
        LocalRepositoryModule::class,
        SharedPreferencesModule::class,
        WorkManagerModule::class,
        WorkManagerFactoryModule::class
    ]
)
interface ApplicationComponent : AndroidInjector<MyApplication> {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance applicationContext: Context): ApplicationComponent
    }
}