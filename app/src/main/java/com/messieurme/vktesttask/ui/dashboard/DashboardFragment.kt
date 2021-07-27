package com.messieurme.vktesttask.ui.dashboard

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.messieurme.vktesttask.ui.main.MainViewModel
import com.messieurme.vktesttask.classes.*
import com.messieurme.vktesttask.databinding.FragmentDashboardBinding
import com.messieurme.vktesttask.databinding.ItemQueuedBinding
import com.messieurme.vktesttask.databinding.ItemUploadingBinding
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.URL
import javax.inject.Inject


class DashboardFragment : DaggerFragment() {
    private lateinit var binding: FragmentDashboardBinding

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val dashboardViewModel: DashboardViewModel by viewModels { viewModelFactory }

    private val chooseFile: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { result ->
            CoroutineScope(Dispatchers.Default).launch {
                result?.let {
                    dashboardViewModel.enqueueVideo(
                        binding.videoName.text.toString()
                            .let { name -> if (name.isEmpty()) "VideoName" else name },
                        binding.description.text.toString(),
                        UrlFromUri.getFilePath(requireContext(), it)!!
                    )
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = dashboardViewModel.refreshAll()

        dashboardViewModel.continueInBackground.onEach {
            binding.switchUploadMode.isChecked = it
        }.launchIn(lifecycleScope)

        binding.switchUploadMode.setOnCheckedChangeListener { _, isChecked ->
            dashboardViewModel.backgroundModeChanged()
        }

        dashboardViewModel.errorHandler.onEach {
            Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
        }.launchIn(lifecycleScope)

        binding.apply {
            recyclerView.apply {
                adapter = UploadingLisAdapter(dashboardViewModel.queueCopy(), dashboardViewModel)
                layoutManager = RecyclerViewBugWithInserting(context, RecyclerView.VERTICAL, false)
            }

            addToUpload.setOnClickListener {
                askPermission()
                if (permission) {
                    chooseFile.launch("video/*")
                }
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dashboardViewModel.refreshAll()
    }


    private var permission = false
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

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            permission = isGranted
        }
}
