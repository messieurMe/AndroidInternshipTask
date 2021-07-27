package com.messieurme.vktesttask.ui.main

import android.os.Bundle
import com.vk.api.sdk.VK
import kotlinx.coroutines.*
import android.app.Activity
import android.widget.Toast
import android.content.Intent
import androidx.activity.viewModels
import androidx.core.content.edit
import com.vk.api.sdk.auth.VKScope
import com.vk.api.sdk.auth.VKAccessToken
import com.vk.api.sdk.auth.VKAuthCallback
import androidx.navigation.findNavController
import androidx.lifecycle.*
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.messieurme.vktesttask.R
import com.messieurme.vktesttask.classes.AccessTokenClass
import com.messieurme.vktesttask.repository.videoUploader.WorkManagerForegroundVideoUploader
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject


class MainActivity : DaggerAppCompatActivity() {
    @Inject
    lateinit var foregroundVideoUploader: WorkManagerForegroundVideoUploader

    @Inject
    lateinit var accessTokenClass: AccessTokenClass

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val mainViewModel by viewModels<MainViewModel> { viewModelFactory }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
            prepareForWorking()
        setContentView(R.layout.activity_main)
        setupNavController()
    }

    private fun setupNavController() {
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStart() {
        super.onStart()
        foregroundVideoUploader.onResume()
    }

    override fun onStop() {
        super.onStop()
        foregroundVideoUploader.onSystemPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        foregroundVideoUploader.closeAll()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val callback = object : VKAuthCallback {
            override fun onLogin(token: VKAccessToken) {
                getPreferences(Activity.MODE_PRIVATE).edit { putString("access_token", token.accessToken) }
                accessTokenClass.submitAccessToken(token.accessToken)
            }

            override fun onLoginFailed(errorCode: Int) {
                Toast.makeText(applicationContext, "Failed to login", Toast.LENGTH_SHORT).show()
            }
        }
        if (data == null || !VK.onActivityResult(requestCode, resultCode, data, callback)) {
            super.onActivityResult(
                requestCode,
                resultCode,
                data
            ) //No info about what to use for deprecated method
        }
    }


    private fun prepareForWorking(forceLogin: Boolean = false) {
        val preferences = getPreferences(Activity.MODE_PRIVATE)
        if (!preferences.contains("access_token") || forceLogin) {
            VK.login(this, arrayListOf(VKScope.VIDEO))
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                accessTokenClass.submitAccessToken(preferences.getString("access_token", "")!!)
            }
        }
    }
}
