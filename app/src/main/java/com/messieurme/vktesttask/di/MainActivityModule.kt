package com.messieurme.vktesttask.di

import androidx.lifecycle.ViewModel
import com.messieurme.vktesttask.ui.main.MainActivity
import com.messieurme.vktesttask.ui.main.MainViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

@Module
abstract class MainActivityModule {

    @ContributesAndroidInjector(modules = [ViewModelBuilder::class])
    internal abstract fun mainActivity(): MainActivity

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun bindViewModel(viewModel: MainViewModel): ViewModel
}