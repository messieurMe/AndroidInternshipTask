package com.messieurme.vktesttask.ui.dashboard

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import androidx.work.*
import com.messieurme.vktesttask.MainViewModel
import com.messieurme.vktesttask.R
import com.messieurme.vktesttask.classes.SharedFunctions.Companion.getProgressInPercents
import com.messieurme.vktesttask.classes.SharedFunctions.Companion.retrofit
import com.messieurme.vktesttask.classes.SharedFunctions.Companion.uploadFunction
import com.messieurme.vktesttask.classes.UploadingProgress
import com.messieurme.vktesttask.classes.UrlFromUri
import com.messieurme.vktesttask.databinding.FragmentDashboardBinding
import com.messieurme.vktesttask.databinding.ItemUploadingBinding
import com.messieurme.vktesttask.room.UploadsDao
import com.messieurme.vktesttask.room.UploadsDatabase
import com.messieurme.vktesttask.service.UploadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.ByteString.Companion.encodeUtf8
import retrofit2.await
import java.io.*
import java.lang.IllegalStateException
import java.lang.NullPointerException
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


class DashboardFragment : Fragment() {

    private var accessToken = ""
    private var pauseUploads = false

    private lateinit var binding: FragmentDashboardBinding
    private lateinit var mainViewModel: MainViewModel

    private var numberOfThreads = AtomicInteger(0)

    private var db: UploadsDatabase? = null
    private var uploads: UploadsDao? = null


    private val chooseFile: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { result ->
            if (result != null) {
                val fileUrl = UrlFromUri.getFilePath(requireContext(), result)
                prepareForUploading(fileUrl!!)
            }
        }

    private fun prepareForUploading(uri: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val name = newFileName
            val fileSize: Long = File(uri).length()
            retrofit
                .runCatching { this.save(name, accessToken).await() }
                .onFailure {
                    //Could catch different exceptions but here might be only network or server problems
                    //In my case messages will be same for both exceptions
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            requireContext(), getString(R.string.connection_problem), Toast.LENGTH_SHORT
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
                    activity?.runOnUiThread {
                        binding.recyclerView.adapter!!.notifyItemChanged(mainViewModel.queue.value!!.size - 1)
                    }
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


    override fun onPause() {
        super.onPause()
        if (db == null || !db!!.isOpen) {
            initializeDB()
        }
        pauseUploads = true
        CoroutineScope(Dispatchers.Default).launch {
            updateRoom()
            while (numberOfThreads.get() != 0) {
                continue
            }
        }
        if (binding.switchUploadMode.isChecked && mainViewModel.queue.value!!.size > 0 && !mainViewModel.pause.value!!) {
            startWorkManager()
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
            .getInstance(requireContext())
            .enqueueUniqueWork("Work", ExistingWorkPolicy.APPEND_OR_REPLACE, myWork)
    }

    private fun initializeDB() {
        db = Room.databaseBuilder(
            requireContext().applicationContext,
            UploadsDatabase::class.java,
            "database"
        ).build()
        uploads = db!!.UploadsDao()
    }


    override fun onResume() {
        super.onResume()
        if (db == null || !db!!.isOpen) {
            initializeDB()
        }
        pauseUploads = false
        CoroutineScope(Dispatchers.IO).launch {
            val workInfo =
                WorkManager.getInstance(requireContext()).getWorkInfosByTag("Work").await()
            if (workInfo.size != 0) {
                WorkManager.getInstance(requireContext()).cancelAllWorkByTag("Work").result.await()
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
        db?.close()
        db = null
    }


    private fun upload() {
        while (mainViewModel.queue.value!!.size > 0 && !pauseUploads && numberOfThreads.get() == 1 && !mainViewModel.pause.value!!) {
            val uploadIngFile = mainViewModel.queue.value!!.first()
            try {
                FileInputStream(uploadIngFile.uri).use { inputStream ->
                    if (uploadIngFile.uploaded != 0L) {
                        inputStream.skip(uploadIngFile.uploaded)
                    }
                    var badResponseCounter = 0
                    while (uploadIngFile.fileSize != uploadIngFile.uploaded && !pauseUploads && !mainViewModel.pause.value!! && badResponseCounter < 3) {
                        uploadFunction(uploadIngFile, inputStream)
                        if (!uploadIngFile.lastSuccess) {
                            badResponseCounter++
                        }
                        val progress = getProgressInPercents(uploadIngFile.uploaded, uploadIngFile.fileSize)
                        Handler(Looper.getMainLooper()).post {
                            try {
                                val viewHolder = binding.recyclerView.findViewHolderForAdapterPosition(0)
                                (viewHolder as UploadingLisAdapter.CustomViewHolder).binding.progressBar.progress =
                                    progress
                            } catch (ignore: NullPointerException) {
                            }
                        }
                        if (cancelUploadForFirst) {
                            cancelUploadForFirst = false
                            mainViewModel.queue.value!!.removeAt(0)
                            Handler(Looper.getMainLooper()).post {
                                binding.recyclerView.adapter?.notifyItemRemoved(0)
                            }
                            break
                        }
                    }
                    if (badResponseCounter == 3) {
                        //Maybe I can store response code, kinda
                        //If it's 4** error, then connection problems, 5** - server
                        showToast("Bad response. Check internet connection or vk servers")
                        setButtonToPaused()
                    }
                }
            } catch (e: IOException) {
                showToast("Problems with reading file for upload ${uploadIngFile.name}")
                setButtonToPaused()
                break
            } catch (e: SecurityException) {
                showToast("Cannot access file for upload ${uploadIngFile.name}")
                setButtonToPaused()
                break
            } catch (e: FileNotFoundException) {
                showToast("Cannot find file for upload  ${uploadIngFile.name}. May be it were deleted or replaced")
                setButtonToPaused()
                break
            } catch (e: Exception) {
                //I don't know which exceptions can occur, but I don't want app crash too
                showToast("Error while uploading")
                setButtonToPaused()
                break
            }
            if (uploadIngFile.fileSize == uploadIngFile.uploaded) {
                mainViewModel.queue.value!!.removeAt(0)
                activity?.runOnUiThread {
                    binding.recyclerView.adapter?.notifyItemRemoved(0)
                }
            }
        }
    }

    private fun setButtonToPaused() {
        Handler(Looper.getMainLooper()).post {
            mainViewModel.pause.value = true
        }
    }

    private fun showToast(message: String) {
        try {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: IllegalStateException) {
            //If not attached to context
        }
    }

    var cancelUploadForFirst = false
    private var newFileName = "VideoName"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        mainViewModel.accessToken.observe(viewLifecycleOwner, {
            CoroutineScope(Dispatchers.Default).launch { if (it.isNotEmpty()) accessToken = it }
        })
        initializeDB()
        binding.apply {
            recyclerView.apply {
                adapter = UploadingLisAdapter(mainViewModel.queue.value!!)
                layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            }

            addToUpload.setOnClickListener {
                askPermission()
                if (permission) {
                    chooseFile.launch("video/*")
                }
                newFileName = binding.searchQuery.text.toString()
                    .let { name -> if (name.isNotEmpty()) name.encodeUtf8().utf8() else "VideoName" }
                //I can add textField for description, post on wall and all this, but...
                //It's few minutes in code, but with this I must change layout,
                //there will be too many fields, I'll convert it to MotionLayout to make
                //appearance of them. MotionLayout is cool, but Android Studio is slows down.
                //And I'll spend  too much time on waiting. Just... let's imagine I did it
            }
            pause.setOnClickListener { uploadPause() }
        }
        mainViewModel.pause.observe(viewLifecycleOwner) {
            binding.isPause = it
        }
        CoroutineScope(Dispatchers.IO).launch {
            if (mainViewModel.pause.value != null) {
                while (uploads!!.getSize() > 0) {
                    mainViewModel.queue.value!!.add(
                        uploads!!.getFirst().also { uploads!!.remove(it.sessionID) })
                }
                Handler(Looper.getMainLooper()).post {
                    binding.isPause =
                        mainViewModel.pause.value!! // <- I know what DataBinding is. Just saying
                    binding.recyclerView.adapter!!.notifyItemRangeChanged(
                        0,
                        mainViewModel.queue.value!!.size
                    )
                }
                resumeUploading()
            }
        }
        return binding.root
    }

    private fun uploadPause() {
        val current = mainViewModel.pause
        mainViewModel.pause.value = !(mainViewModel.pause.value ?: false)
        binding.isPause = mainViewModel.pause.value!!

        if (!current.value!!) {
            CoroutineScope(Dispatchers.IO).launch {
                resumeUploading()
            }
        }
    }

    inner class UploadingLisAdapter(
        private val titles: ArrayList<UploadingProgress>
    ) :
        RecyclerView.Adapter<UploadingLisAdapter.CustomViewHolder>() {

        inner class CustomViewHolder(item: ItemUploadingBinding) :
            RecyclerView.ViewHolder(item.root) {
            var binding: ItemUploadingBinding = item

            fun bind(info: UploadingProgress) {
                binding.name.text = info.name
                binding.cancel.setOnClickListener {
                    val position = adapterPosition
                    if (position != 0) {
                        mainViewModel.queue.value!!.removeAt(position)
                        notifyItemRemoved(position)
                    } else {
                        cancelUploadForFirst = true
                    }
                }

            }
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = CustomViewHolder(
            ItemUploadingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
            holder.bind(titles[position])
        }

        override fun getItemCount() = titles.size
    }


    private var permission = false
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            permission = isGranted
        }

    private fun askPermission(): Boolean {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                permission = true
            }
            shouldShowRequestPermissionRationale("READ F...N STORAGE") -> {
                println("WHAT")
            }
            else -> {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        return true
    }

}