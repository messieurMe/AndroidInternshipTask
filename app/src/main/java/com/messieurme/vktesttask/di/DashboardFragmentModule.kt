package com.messieurme.vktesttask.di

import androidx.lifecycle.ViewModel
import com.messieurme.vktesttask.ui.dashboard.DashboardFragment
import com.messieurme.vktesttask.ui.dashboard.DashboardViewModel
import com.messieurme.vktesttask.ui.home.HomeFragment
import com.messieurme.vktesttask.ui.home.HomeViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

@Module
abstract class DashboardFragmentModule {
    @ContributesAndroidInjector(modules = [ViewModelBuilder::class])
    internal abstract fun dashboardFragment(): DashboardFragment

    @Binds
    @IntoMap
    @ViewModelKey(DashboardViewModel::class)
    abstract fun bindViewModel(viewModel: DashboardViewModel): ViewModel
}