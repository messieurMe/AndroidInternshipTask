package com.messieurme.vktesttask

import android.os.Bundle
import com.vk.api.sdk.VK
import android.app.Activity
import android.widget.Toast
import android.content.Intent
import android.os.Handler
import android.os.Looper
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
import androidx.room.Room
import androidx.work.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.messieurme.vktesttask.classes.SharedFunctions
import com.messieurme.vktesttask.classes.UploadingProgress
import com.messieurme.vktesttask.room.UploadsDao
import com.messieurme.vktesttask.room.UploadsDatabase
import com.messieurme.vktesttask.service.UploadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.await
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.IllegalStateException
import java.lang.NullPointerException
import java.util.concurrent.atomic.AtomicInteger

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



        initializeDB()

        mainViewModel.enqueueUpload.observe(this, {
            if (it != null) {
                prepareForUploading(it)
            }
        })
        mainViewModel.userPause.observe(this, { uploadPause(it!!) })

        CoroutineScope(Dispatchers.IO).launch {
            if (mainViewModel.userPause.value != null) {
                while (uploads!!.getSize() > 0) {
                    mainViewModel.queue.value!!.add(
                        uploads!!.getFirst().also { uploads!!.remove(it.sessionID) })
                }
                mainViewModel.notifyItemRangeChanged.postValue(
                    mainViewModel.queue.value!!.size
                )
                resumeUploading()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        db?.close()
        db = null
    }

    private fun prepareForWorking(forceLogin: Boolean = false) {
        val preferences = getPreferences(Activity.MODE_PRIVATE)

        mainViewModel.userPause.value = preferences.getBoolean("pause", false)

        if (!preferences.contains("access_token") || forceLogin) {
            VK.login(this, arrayListOf(VKScope.VIDEO))
        } else {
            mainViewModel.accessToken.value = preferences.getString("access_token", "")!!
        }
    }

    var pauseUploads = false

    override fun onPause() {
        super.onPause()
        pauseUploads = true
        CoroutineScope(Dispatchers.Default).launch {
            updateRoom()
            while (numberOfThreads.get() != 0) {
                continue
            }
        }
        if (mainViewModel.isChecked && mainViewModel.queue.value!!.size > 0 && !mainViewModel.userPause.value!!) {
            startWorkManager()
        }
    }

    override fun onResume() {
        super.onResume()
        pauseUploads = false

        if (db == null || !db!!.isOpen) {
            initializeDB()
        }
        CoroutineScope(Dispatchers.IO).launch {
            val workInfo =
                WorkManager.getInstance(this@MainActivity).getWorkInfosByTag("Work").await()
            if (workInfo.size != 0) {
                WorkManager.getInstance(this@MainActivity).cancelAllWorkByTag("Work").result.await()
            }

            while (uploads!!.getSize() > 0) {
                mainViewModel.queue.value!!.add(
                    uploads!!.getFirst().also { uploads!!.remove(it.sessionID) })
            }
            resumeUploading()
        }
    }


    override fun onStop() {
        super.onStop()
        getPreferences(Activity.MODE_PRIVATE).edit {
            putBoolean("pause", mainViewModel.userPause.value ?: false)
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
            super.onActivityResult(
                requestCode,
                resultCode,
                data
            ) //No info about what to use for deprecated method
        }
    }

    private fun uploadPause(newValue: Boolean) {
        if (!newValue) {
            CoroutineScope(Dispatchers.IO).launch {
                resumeUploading()
            }
        }
    }

    var numberOfThreads = AtomicInteger(0)

    var db: UploadsDatabase? = null
    var uploads: UploadsDao? = null


    private fun prepareForUploading(uri: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val name = mainViewModel.newFileName
            val fileSize: Long = File(uri).length()
            SharedFunctions.retrofit
                .runCatching { this.save(name, mainViewModel.accessToken.value!!).await() }
                .onFailure {
                    //Could catch different exceptions but here might be only network or server problems
                    //In my case messages will be same for both exceptions
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            this@MainActivity, getString(R.string.connection_problem), Toast.LENGTH_SHORT
                        ).show()
                    }
                }.onSuccess { save ->
                    val uploadInfo = UploadingProgress(
                        url = save.response.upload_url,
                        fileSize = fileSize,
                        uri = uri,
                        name = name
                    )
                    mainViewModel.queue.value!!.add(uploadInfo)
                    mainViewModel.recyclerViewItemChanged.postValue(mainViewModel.queue.value!!.size - 1)
                    resumeUploading()
                }
        }
    }


    private fun resumeUploading() {
        if (numberOfThreads.compareAndSet(0, 1)) {
            upload()
            numberOfThreads.addAndGet(-1)
            //  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
            //  possibly can occur problem when one thread finished uploading, but didn't decreased value
            //  In this case new uploading will not start,
            //  BUT
            //  good practice is to call CAS and decrease value each time we going inside global `while`
            //  inside `upload()`.
            //  BUT X2
            //  it's atomic variable. Operations with it are very heavy. Problem when uploading didn't start
            //  will occur rare (if will occur). So to increase performance I call operations with atomic
            //  variable here. There is no chance to run into deadlock. Simply uploading will not start if
            //  one thread (now coroutine, but earlier it was thread) didn't decreased value and new thread
            //  ran into CAS
        }
    }


    private fun updateRoom() {
        try {
            uploads?.removeAll()
            mainViewModel.queue.value!!.forEach { i -> uploads?.insert(i) }
            mainViewModel.queue.value!!.clear()
        } catch (e: java.lang.Exception) {
            initializeDB()
            mainViewModel.queue.value!!.forEach { i -> uploads?.insert(i) }
            mainViewModel.queue.value!!.clear()
            db?.close()
        }
    }


    private fun startWorkManager() {
        val constraint =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        val myWork = OneTimeWorkRequest.Builder(UploadWorker::class.java)
            .setConstraints(constraint)
            .addTag("Work")
            .build()

        WorkManager
            .getInstance(this)
            .enqueueUniqueWork("Work", ExistingWorkPolicy.APPEND_OR_REPLACE, myWork)
    }


    private fun initializeDB() {
        db = Room.databaseBuilder(
            this.applicationContext,
            UploadsDatabase::class.java,
            "database"
        ).build()
        uploads = db!!.UploadsDao()
    }


    private fun upload() {
        while (mainViewModel.queue.value!!.size > 0 && !pauseUploads && numberOfThreads.get() == 1 && !mainViewModel.userPause.value!!) {
            val uploadIngFile = mainViewModel.queue.value!!.first()
            try {
                FileInputStream(uploadIngFile.uri).use { inputStream ->
                    if (uploadIngFile.uploaded != 0L) {
                        inputStream.skip(uploadIngFile.uploaded)
                    }
                    var badResponseCounter = 0
                    while (uploadIngFile.fileSize != uploadIngFile.uploaded && !pauseUploads && !mainViewModel.userPause.value!! && badResponseCounter < 3) {
                        SharedFunctions.uploadFunction(uploadIngFile, inputStream)
                        if (!uploadIngFile.lastSuccess) {
                            badResponseCounter++
                        }
                        val progress = SharedFunctions.getProgressInPercents(
                            uploadIngFile.uploaded,
                            uploadIngFile.fileSize
                        )
                        mainViewModel.progress.postValue(progress)

                        if (mainViewModel.cancelUploadForFirst) {
                            mainViewModel.cancelUploadForFirst = false
                            mainViewModel.queue.value!!.removeAt(0)
                            mainViewModel.recyclerViewItemRemoved.postValue(0)
                            break
                        }
                    }
                    if (badResponseCounter == 3) {
                        //Maybe I can store response code, kinda
                        //If it's 4** error, then connection problems, 5** - server
                        showToast("Bad response. Check internet connection or vk servers")
                        mainViewModel.userPause.value = true
                    }
                }
            } catch (e: IOException) {
                showToast("Problems with reading file for upload ${uploadIngFile.name}")
                mainViewModel.userPause.value = true
                break
            } catch (e: SecurityException) {
                showToast("Cannot access file for upload ${uploadIngFile.name}")
                mainViewModel.userPause.value = true
                break
            } catch (e: FileNotFoundException) {
                showToast("Cannot find file for upload  ${uploadIngFile.name}. May be it were deleted or replaced")
                mainViewModel.userPause.value = true
                break
            } catch (e: Exception) {
                //I don't know which exceptions can occur, but I don't want app crash too
                showToast("Error while uploading")
                mainViewModel.userPause.value = true
                break
            }
            if (uploadIngFile.fileSize == uploadIngFile.uploaded) {
                mainViewModel.queue.value!!.removeAt(0)
                mainViewModel.recyclerViewItemRemoved.postValue(0)
            }
        }
    }

    private fun showToast(message: String) {
        try {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: IllegalStateException) {
            //If not attached to context
        }
    }


}