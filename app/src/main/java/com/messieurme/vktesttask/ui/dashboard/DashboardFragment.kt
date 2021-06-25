package com.messieurme.vktesttask.ui.dashboard

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.messieurme.vktesttask.MainViewModel
import com.messieurme.vktesttask.classes.*
import com.messieurme.vktesttask.databinding.FragmentDashboardBinding
import com.messieurme.vktesttask.databinding.ItemQueuedBinding
import com.messieurme.vktesttask.databinding.ItemUploadingBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*


class DashboardFragment : Fragment() {
    private lateinit var binding: FragmentDashboardBinding
    private lateinit var mainViewModel: MainViewModel

    private val chooseFile: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { result ->
            CoroutineScope(Dispatchers.Default).launch {
                result?.let {
                    mainViewModel.enqueueUpload.send(UrlFromUri.getFilePath(requireContext(), it)!!)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)


        binding.apply {
            switchUploadMode.setOnCheckedChangeListener { _, isChecked ->
                mainViewModel.isChecked.value = isChecked
            }

            recyclerView.apply {
                adapter = UploadingLisAdapter(PriorityArrayList(mainViewModel.queue.value.map { it.copy() }))
                layoutManager = RecyclerViewBugWithInserting(context, RecyclerView.VERTICAL, false)
            }

            addToUpload.setOnClickListener {
                askPermission()
                if (permission) {
                    mainViewModel.newFileName = binding.searchQuery.text.toString().let { name ->
                        if (name.isNotEmpty()) name else "VideoName"
                    }
                    mainViewModel.description = binding.description.text.toString().let { description ->
                        if (description.isNotEmpty()) description else ""
                    }
                    chooseFile.launch("video/*")
                }
            }

            pause.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    val currentValue = !(mainViewModel.userPause.value ?: false)
                    mainViewModel.userPause.emit(currentValue)
                }
            }
        }


        CoroutineScope(Dispatchers.Main).launch {
            mainViewModel.progress.collect { update ->
                kotlin.runCatching {
                    (binding.recyclerView.findViewHolderForAdapterPosition(0) as UploadingLisAdapter.CustomViewHolderUploading).binding.apply {
                        progressBar.isIndeterminate = false
                        progressBar.setProgress(update, true)
                        progressInDigits.text = "$update%"
                    }
                }
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.userPause.collect { pause -> pause?.let { binding.isPause = it } }

                mainViewModel.apply {
                    kostylForUI.collect {
                        binding.isPause = mainViewModel.userPause.value
                        binding.background = mainViewModel.isChecked.value
                    }
                }
            }
        }
    }


    inner class UploadingLisAdapter(var items: PriorityArrayList<UploadingProgress>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        init {
            CoroutineScope(Dispatchers.Main).launch {
                mainViewModel.notifyQueueChanged.receiveAsFlow().collect { updateList(it) }
            }
        }

        private fun updateList(it: Int) = kotlin.runCatching {
            val diffResult =
                DiffUtil.calculateDiff(DiffUtilCallbackUploading(this.items, mainViewModel.queue.value), true)
            diffResult.dispatchUpdatesTo(this)
            items = PriorityArrayList(mainViewModel.queue.value.map { it.copy() })
        }.isSuccess

        inner class CustomViewHolderUploading(item: ItemUploadingBinding) :
            RecyclerView.ViewHolder(item.root) {
            var binding: ItemUploadingBinding = item

            fun bind(info: UploadingProgress) {
                binding.name.text = info.name
                binding.progressBar.isIndeterminate = false
                binding.cancel.setOnClickListener {
                    CoroutineScope(Dispatchers.Main).launch {
                        mainViewModel.queue.value.removeAt(absoluteAdapterPosition)
                        mainViewModel.notifyQueueChanged.send(2)
                        mainViewModel.progress.emit(0)
                    }
                }
                mainViewModel.progress.value.also {
                    val ourProgress = if (it == 100) 0 else it
                    binding.progressBar.setProgress(ourProgress, true)
                    binding.progressInDigits.text = "$ourProgress%"
                }
            }
        }

        inner class CustomViewHolderQueued(item: ItemQueuedBinding) : RecyclerView.ViewHolder(item.root) {
            var binding: ItemQueuedBinding = item

            fun bind(info: UploadingProgress) {
                binding.name.text = info.name
                binding.cancel.setOnClickListener {
                    CoroutineScope(Dispatchers.Main).launch {
                        mainViewModel.queue.value.removeAt(absoluteAdapterPosition)
                        mainViewModel.notifyQueueChanged.send(2)
                    }
                }
            }
        }

        override fun getItemViewType(position: Int) = if (position == 0) 1 else 2

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
            1 -> CustomViewHolderUploading(
                ItemUploadingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> CustomViewHolderQueued(
                ItemQueuedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder.itemViewType) {
                1 -> (holder as CustomViewHolderUploading).bind(mainViewModel.queue.value[position])
                else -> (holder as CustomViewHolderQueued).bind(mainViewModel.queue.value[position])
            }
        }

        override fun getItemCount() = mainViewModel.queue.value.size
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
