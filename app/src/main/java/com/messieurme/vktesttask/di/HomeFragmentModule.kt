package com.messieurme.vktesttask.di

import androidx.lifecycle.ViewModel
import com.messieurme.vktesttask.ui.home.HomeFragment
import com.messieurme.vktesttask.ui.home.HomeViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

@Module
abstract class HomeFragmentModule {
    @ContributesAndroidInjector(modules = [ViewModelBuilder::class])
    internal abstract fun homeFragment():HomeFragment

    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    abstract fun bindViewModel(viewModel: HomeViewModel): ViewModel

}