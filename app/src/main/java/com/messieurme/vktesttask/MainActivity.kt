package com.messieurme.vktesttask

import java.io.File
import androidx.work.*
import retrofit2.await
import android.os.Looper
import android.os.Bundle
import com.vk.api.sdk.VK
import android.os.Handler
import androidx.room.Room
import java.io.IOException
import kotlinx.coroutines.*
import android.app.Activity
import android.widget.Toast
import android.content.Intent
import androidx.core.content.edit
import com.vk.api.sdk.auth.VKScope
import java.io.FileNotFoundException
import com.vk.api.sdk.auth.VKAccessToken
import com.vk.api.sdk.auth.VKAuthCallback
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.messieurme.vktesttask.room.UploadsDao
import java.util.concurrent.atomic.AtomicInteger
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.messieurme.vktesttask.room.UploadsDatabase
import com.messieurme.vktesttask.service.UploadWorker
import com.messieurme.vktesttask.classes.SharedFunctions
import com.messieurme.vktesttask.classes.UploadingProgress
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.messieurme.vktesttask.classes.SharedFunctions.Companion.getProgressInPercents
import kotlinx.coroutines.flow.*
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.inputStream

class MainActivity : AppCompatActivity() {

    lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        prepareForWorking()

        setContentView(R.layout.activity_main)

        setupNavController()
        initializeDB()
        initializeListeners()
    }

    private fun initializeListeners() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.enqueueUpload.receiveAsFlow().collect { url ->
                    url?.let { prepareForUploading(it) }
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.userPause.collect { uploadPause() }
            }
        }

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

    override fun onDestroy() {
        super.onDestroy()
        db?.close()
        db = null
    }

    private fun prepareForWorking(forceLogin: Boolean = false) {
        val preferences = getPreferences(Activity.MODE_PRIVATE)

        mainViewModel.userPause.value = preferences.getBoolean("pause", false)
        mainViewModel.isChecked.value = preferences.getBoolean("background_mode", true)

        CoroutineScope(Dispatchers.IO).launch { mainViewModel.kostylForUI.emit(true) }

        if (!preferences.contains("access_token") || forceLogin) {
            VK.login(this, arrayListOf(VKScope.VIDEO))
        } else {
            mainViewModel.accessToken.value = preferences.getString("access_token", "")!!
        }
    }

    override fun onStop() {
        super.onStop()
        numberOfThreads.set(0)
        uploadSessionIds.clear()
        runBlocking {
            CoroutineScope(Dispatchers.IO).launch {
                getPreferences(Activity.MODE_PRIVATE).edit {
                    putBoolean("pause", mainViewModel.userPause.value ?: false)
                    putBoolean("background_mode", mainViewModel.isChecked.value ?: true)
                }
                updateRoom()
                if ((mainViewModel.isChecked.value == true) && uploads!!.getSize() > 0 && !mainViewModel.userPause.value!!) {
                    startWorkManager()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val callback = object : VKAuthCallback {
            override fun onLogin(token: VKAccessToken) {
                getPreferences(Activity.MODE_PRIVATE).edit { putString("access_token", token.accessToken) }
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

    private fun uploadPause() {
        CoroutineScope(Dispatchers.IO).launch { resumeUploading() }
    }

    var numberOfThreads = AtomicInteger(0)

    var db: UploadsDatabase? = null
    var uploads: UploadsDao? = null


    private fun prepareForUploading(uri: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val name = mainViewModel.newFileName
            val description = mainViewModel.description
            val fileSize: Long = File(uri).length()
            val uploadInfo = UploadingProgress(
                fileSize = fileSize,
                description = description,
                uri = uri,
                name = name
            )

            mainViewModel.queue.value.addLast(uploadInfo)
            mainViewModel.notifyQueueChanged.send(-1)

            resumeUploading()
        }
    }


    private suspend fun resumeUploading() {
        if (numberOfThreads.compareAndSet(0, 1)) {
            val id = System.currentTimeMillis()
            uploadSessionIds.add(id)
            upload(id)
            uploadSessionIds.remove(id)
            numberOfThreads.compareAndSet(1, 0)
        }
    }

    override fun onStart() {
        super.onStart()
        CoroutineScope(Dispatchers.IO).launch {
            val workInfo = WorkManager.getInstance(this@MainActivity).getWorkInfosByTag("Work").await()
            if (workInfo.size != 0) {
                WorkManager.getInstance(this@MainActivity).cancelAllWorkByTag("Work").result.await()
            }

            mainViewModel.queue.value.clear()
            if (uploads!!.getSize() > 0) {
//                uploads!!.getIds().forEach { id ->
//                    mainViewModel.queue.value.addPreLast(uploads!!.getById(id))
//                    uploads!!.remove(id)
//                }

                mainViewModel.queue.value.addAllPreLast(uploads!!.getAll())
                uploads!!.removeAll()

                mainViewModel.notifyQueueChanged.send(3)

                mainViewModel.queue.value.first().also {
                    mainViewModel.progress.emit(getProgressInPercents(it.uploaded, it.fileSize))
                }
            }
            resumeUploading()
        }
    }

    private suspend fun updateRoom() {
        uploads!!.removeAll()
        uploads!!.insertAll(mainViewModel.queue.value)
        mainViewModel.queue.value.clear()
    }


    private fun startWorkManager() {
        val accessToken = workDataOf("access_token" to mainViewModel.accessToken.value)
        val constraint = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val myWork = OneTimeWorkRequest.Builder(UploadWorker::class.java)
            .setConstraints(constraint)
            .setInputData(accessToken)
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

    @Volatile
    var uploadSessionIds = HashSet<Long>(0)


    @OptIn(ExperimentalPathApi::class)
    private suspend fun upload(id: Long) {
        fun stopUploadingChecker(): Boolean {
            return mainViewModel.queue.value.size > 0 && uploadSessionIds.contains(id) && !mainViewModel.userPause.value!!
        }

        suspend fun badResponse(n: Int) =
            when (n) {
                0 -> false
                1, 2 -> {
                    val timeout = n * 2000L
                    showToast("Bad response. Check internet connection or vk servers. Retrying after $timeout")
                    delay(timeout)
                    false
                }
                3 -> {
                    showToast("Cannot upload")
                    mainViewModel.userPause.emit(true)
                    true
                }
                else -> true
            }

        withContext(Dispatchers.IO) {
            var badResponseCounter = 0

            while (stopUploadingChecker()) {
                val uploadIngFile = mainViewModel.queue.value.first()
                if (uploadIngFile.url == "-") {
                    if (getUrlForFile(uploadIngFile)) {
                        mainViewModel.queue.value.first()
                    } else {
                        badResponseCounter++
                        badResponse(badResponseCounter)
                        continue
                    }
                }
                try {
                    Paths.get(uploadIngFile.uri).inputStream().use { inputStream ->
                        if (uploadIngFile.uploaded != 0L) {
                            inputStream.skip(uploadIngFile.uploaded)
                        }
                        while (uploadIngFile.fileSize != uploadIngFile.uploaded && stopUploadingChecker() && mainViewModel.queue.value.first().sessionID == uploadIngFile.sessionID) {
                            SharedFunctions.uploadFunction(uploadIngFile, inputStream)

                            if (!uploadIngFile.lastSuccess) {
                                badResponse(++badResponseCounter)
                            }
                            mainViewModel.progress.emit(uploadIngFile.progress)
                        }
                    }
                } catch (e: IOException) {
                    showToast("Problems with reading file for upload ${uploadIngFile.name}")
                    mainViewModel.userPause.emit(true)
                    break
                } catch (e: SecurityException) {
                    showToast("Cannot access file for upload ${uploadIngFile.name}")
                    mainViewModel.userPause.emit(true)
                    break
                } catch (e: FileNotFoundException) {
                    showToast("Cannot find file for upload  ${uploadIngFile.name}. May be it were deleted or replaced")
                    mainViewModel.userPause.emit(true)
                    break
                } catch (e: Exception) {
                    //I don't know which exceptions can occur, but I don't want app crash too
                    showToast("Error while uploading")
                    mainViewModel.userPause.emit(true)
                    break
                }

                if (uploadIngFile.fileSize == uploadIngFile.uploaded) {
                    mainViewModel.queue.value.removeAt(0)
                    mainViewModel.notifyQueueChanged.send(1)
                }
            }
        }
    }

    private suspend fun getUrlForFile(uploadIngFile: UploadingProgress) = SharedFunctions.retrofit
        .runCatching {
            this.save(
                uploadIngFile.name,
                mainViewModel.accessToken.value,
                uploadIngFile.description
            ).await()
        }
        .onFailure {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    this@MainActivity, getString(R.string.connection_problem), Toast.LENGTH_SHORT
                ).show()
            }
        }.onSuccess { save ->
            uploadIngFile.url = save.response.upload_url
        }.isSuccess

    private fun showToast(message: String) {
        try {
            Handler(Looper.getMainLooper()).post { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        } catch (e: IllegalStateException) {
            //If not attached to context
        }
    }
}