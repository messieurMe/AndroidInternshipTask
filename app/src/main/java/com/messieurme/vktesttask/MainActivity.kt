package com.messieurme.vktesttask

import android.os.Bundle
import com.vk.api.sdk.VK
import android.app.Activity
import android.widget.Toast
import android.content.Intent
import androidx.core.content.edit
import com.vk.api.sdk.auth.VKScope
import com.vk.api.sdk.auth.VKAccessToken
import com.vk.api.sdk.auth.VKAuthCallback
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        prepareForWorking()

        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)



    }


    private fun prepareForWorking(forceLogin: Boolean = false) {
        val preferences = getPreferences(Activity.MODE_PRIVATE)

        mainViewModel.pause.value = preferences.getBoolean("pause", false)

        if (!preferences.contains("access_token") || forceLogin) {
            VK.login(this, arrayListOf(VKScope.VIDEO))
        } else {
            mainViewModel.accessToken.value = preferences.getString("access_token", "")!!
        }
    }

    override fun onStop() {
        super.onStop()
        getPreferences(Activity.MODE_PRIVATE).edit {
            putBoolean("pause", mainViewModel.pause.value ?: false)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val callback = object : VKAuthCallback {
            override fun onLogin(token: VKAccessToken) {
                getPreferences(Activity.MODE_PRIVATE).edit {
                    putString(
                        "access_token",
                        token.accessToken
                    )
                }
                mainViewModel.accessToken.value = token.accessToken
            }

            override fun onLoginFailed(errorCode: Int) {
                Toast.makeText(applicationContext, "Failed to login", Toast.LENGTH_SHORT).show()
            }
        }
        if (data == null || !VK.onActivityResult(requestCode, resultCode, data, callback)) {
            super.onActivityResult(requestCode, resultCode, data) //No info about what to use for deprecated method
        }
    }

}